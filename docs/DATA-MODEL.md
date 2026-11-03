# Data Model (logical)

> This is the logical model the implementation follows. Exact columns are finalised in Flyway migrations.
> Conventions: `id BIGINT identity` PK; `created_at`, `created_by`, `updated_at`, `updated_by`, `version` on mutable tables;
> money is `NUMERIC(14,2)`; dates are `DATE`, timestamps are `TIMESTAMPTZ` (stored UTC, displayed Asia/Karachi).
> Soft state uses `status` enums, not deletes. **Used master data can only be deactivated** (FR 9.7).
>
> **Multi-tenancy (DECISIONS D-01):** every table in sections 1–11 (except `school` itself) has `school_id BIGINT NOT NULL`
> referencing `school`, is covered by a PostgreSQL RLS policy, and includes `school_id` in its unique keys and foreign keys
> (e.g. `UNIQUE(school_id, gr_no)`, FK `(school_id, student_id) → student(school_id, id)`). Indexes lead with `school_id`.
> `school_id` is omitted from the tables below for brevity.

## 0. Platform (not tenant-scoped)

| Entity | Key fields | Notes |
|---|---|---|
| `school` | code (unique, short, used at sign-in), name, status `ACTIVE/SUSPENDED/OFFBOARDING`, plan, timezone (default Asia/Karachi), created_by, created_at | One row per tenant |
| `school_domain` | school_id, host (unique), verified, primary | Maps public-website hosts to a school |
| `platform_user` | email (unique), name, password_hash, totp_secret (required), status | Super admin = system owner; full access to all schools |
| `platform_login_event` | platform_user_id, at, result, ip | |
| `platform_audit_log` | actor, action (CREATE_SCHOOL, SUSPEND, CREATE_SCHOOL_ADMIN, RESET_USER, SWITCH_SCHOOL, CROSS_SCHOOL_EXPORT…), school_id, details jsonb, at | Append-only |

In a school's `audit_log`, actions by the super admin carry `actor_type = PLATFORM` and `actor_id = platform_user.id`.

## 1. School and setup (FR 1, 12)

| Entity | Key fields | Notes |
|---|---|---|
| `school_profile` | school_id (PK), name, campus_name, address, phones, email, head_name, head_title | One per school; edited by the school admin (FR 1.1) |
| `document_identity` | logo_file_id, letterhead_file_id, stamp_file_id, signatures (jsonb: role→file, name, title) | FR 1.1 recommendation |
| `academic_year` | name (`2026-2027`), start_date, end_date, status `UPCOMING/CURRENT/CLOSED`, opened_by, opened_at | Exactly one CURRENT per school (partial unique index on `school_id`) |
| `year_rollover` | from_year_id, to_year_id, items_copied (jsonb), performed_by | FR 1.3 |
| `working_week` | academic_year_id, weekday, type `FULL/HALF/OFF` | FR 1.4 |
| `calendar_day_off` | academic_year_id, date_from, date_to, title, type `HOLIDAY/VACATION` | Excluded from attendance |
| `setting_value` | key, value (jsonb), effective_from, changed_by, reason | History kept, latest effective wins (FR 12) |

## 2. Identity and access (FR 2)

| Entity | Key fields | Notes |
|---|---|---|
| `app_user` | username (unique per school, case-insensitive), password_hash (bcrypt, cost 12), status `ACTIVE/INACTIVE/LOCKED`, must_change_password, staff_id (nullable), phone, email, mfa_secret | School staff only; no parent/student accounts (DECISIONS D-19) |
| `role` | code, name, system flag | Seeded per school on creation: ADMIN, PRINCIPAL, SCHOOL_OWNER, AUDITOR, ACCOUNTANT, TEACHER, FRONT_OFFICE, EXAM_COORDINATOR, WEBSITE_EDITOR |
| `permission` | code `fees.payment.add` | Global catalogue defined in code (not tenant-scoped) |
| `role_permission`, `user_role` | | |
| `user_permission_override` | user_id, permission, grant/deny | Fine-tuning (FR 1.7) |
| `user_data_scope` | user_id, scope `ALL/ASSIGNED_CLASSES/SELF` | Teachers derive classes from `teaching_assignment` |
| `login_event` | user_id/username, at, result `SUCCESS/INVALID/BLOCKED`, ip, user_agent, approx_location, new_location flag | FR 2.7, 3.8 |
| `password_reset` | user_id, token_hash, channel, expires_at, used_at, approved_by | FR 2.3 |
| `account_status_change` | user_id, from, to, reason, by | FR 2.4 |
| `audit_log` | actor_id, action, entity_type, entity_id, before jsonb, after jsonb, reason, ip, at | Append-only (FR 2.8) |
| `user_favourite` | user_id, route, label, order | Quick bar (FR 3.12) |

## 3. Academic structure (FR 9, 6.3, 6.4)

| Entity | Key fields | Notes |
|---|---|---|
| `school_class` | name, short_name, display_order, grading_mode `MARKS/GRADES`, status | |
| `section` | class_id, name, class_teacher_staff_id, status | |
| `subject` | name, short_name | Master list |
| `class_subject` | academic_year_id, class_id, subject_id, max_marks, passing_marks, display_order, status | Per year, so changes don't rewrite history |
| `teaching_assignment` | academic_year_id, section_id, subject_id (nullable = class teacher), staff_id, effective_from, effective_to | Drives teacher data scope (FR 6.3, 6.6) |
| `timetable_period` | academic_year_id, section_id, weekday, period_no, start/end time, subject_id, staff_id | Optional (FR 6.4) |
| `structure_change_log` | entity, old, new, effective_date | FR 9.6 |

## 4. Admissions (FR 4)

| Entity | Key fields | Notes |
|---|---|---|
| `enquiry_status` | name, colour, order, system_code (NEW, FOLLOW_UP, APPROVED, NOT_PROCEEDING, ENROLLED) | School-defined (FR 4.2) |
| `enquiry` | registration_no, enquiry_date, source `WALK_IN/PHONE/ONLINE`, child_name, gender, dob, class_applied_id, parent_name, parent_phone, parent_cnic, address, previous_school, remarks, status_id, student_id (after enrolment) | |
| `enquiry_remark` | enquiry_id, text, by, at | FR 4.3 |
| `entry_test_result` | enquiry_id, test_date, obtained, total, outcome, remark | FR 4.4 |
| `admission_content` | class_id nullable, type `TERMS/SYLLABUS`, body | FR 4.11 |

Duplicate detection (FR 4.5) uses trigram indexes (`pg_trgm`) on names plus exact matching on phone, CNIC and B-Form.

## 5. Students and families (FR 5, 8)

| Entity | Key fields | Notes |
|---|---|---|
| `family` | family_no (unique), father_name, father_cnic, father_phone, profession, email, address, mother_name, mother_phone, emergency_contact, preferred_channel, remarks | |
| `guardian` | family_id nullable, name, relationship, cnic, phone | Separate guardians (FR 8.3) |
| `student_guardian` | student_id, guardian_id/family role, can_collect, receives_attendance_msgs, receives_fee_msgs, receives_result_msgs | Who gets which message |
| `student` | gr_no (unique), name, gender, dob, place_of_birth, b_form_no, religion, nationality, phone, address, admission_date, first_class_id, previous_school, study_group, seat_no, photo_file_id, family_id, enquiry_id, status `ACTIVE/LEFT`, notes | GR number suggested = max+1, editable, unique |
| `enrolment` | student_id, academic_year_id, section_id, roll_no, effective_from, effective_to, reason, type `ADMISSION/MOVE/PROMOTION` | History of class membership (FR 5.4, 5.5) |
| `promotion_decision` | rollover_id, student_id, decision `PROMOTE/REPEAT/LEAVE`, to_section_id | |
| `withdrawal` | student_id, date, reason, outstanding_balance, bad_debt_amount, approved_by | FR 5.6 |
| `student_document` | student_id, type, file_id | FR 4.9 |
| `certificate_issue` | student_id/staff_id, type, serial_no, issued_at, issued_by, file_id | Leaving, DOB, character, appreciation, provisional, hope, experience |
| `house`, `club`, `student_activity` | | FR 5.10 |
| `field_change_history` | entity, entity_id, field, old, new, by, at | Or reuse `audit_log` |

## 6. Staff and payroll (FR 6, 7, 6.8, 13.12)

| Entity | Key fields | Notes |
|---|---|---|
| `staff` | employee_no, category `TEACHER/OTHER`, name, father_or_husband_name, photo, phone, email, address, cnic, dob, qualification, experience, joining_date, designation, employment_type `PERMANENT/CONTRACT`, status, leaving_date, leaving_reason, shift | |
| `staff_document` | staff_id, type, file_id | |
| `salary_profile` | staff_id, basic_salary, per_lecture_rate, bank_name, bank_account, effective_from | Increments = new rows |
| `salary_sheet` | month, academic_year_id, status `DRAFT/APPROVED/PAID`, approved_by | |
| `salary_sheet_line` | sheet_id, staff_id, basic, absent_days, half_days, late_days, deductions (jsonb breakdown), tax, net, journal_entry_id | Rules come from settings |

## 7. Fees (FR 10)

| Entity | Key fields | Notes |
|---|---|---|
| `fee_head` | name, frequency `MONTHLY/YEARLY/ONE_TIME/PER_TERM/CUSTOM`, max_amount, display_order, refundable, status | No compulsory heads |
| `fee_structure` | academic_year_id, fee_head_id, class_id, amount | Missing amounts flagged (FR 10.2) |
| `student_fee_override` | student_id, fee_head_id, academic_year_id, amount | |
| `discount` | student_id, fee_head_id, type `AMOUNT/PERCENT`, value, reason, valid_from/to, status `PENDING/APPROVED/REJECTED`, approved_by | Approval above a settings threshold |
| `sibling_discount_rule` | child_order, percent, fee_head_ids | FR 10.4 |
| `bill` | bill_no, student_id, family_id, academic_year_id, period (`2026-10`), title, issue_date, due_date, validity_date, status `UNPAID/PARTIAL/PAID/CANCELLED`, job_run_id, version | |
| `bill_line` | bill_id, fee_head_id, period, gross, discount, net, paid, `UNIQUE(student_id, fee_head_id, period)` where not reversed | Prevents double billing |
| `adjustment` | bill_line_id, amount (±), reason, requested_by, approved_by | Reversals (FR 10.13) |
| `payment` | receipt_no (gapless per year), payer student_id or family_id, date, method `CASH/BANK/CHEQUE/ONLINE`, account_id, amount, reference, received_by, idempotency_key unique, reversed_by_payment_id | |
| `payment_allocation` | payment_id, bill_line_id, amount | Oldest-due-first default, manual override |
| `advance_credit` | student/family, amount, remaining | Applied to future bills (FR 10.10) |
| `late_fee_run`, `absent_fine_run` | parameters, applied_at, bills_affected | Idempotent per period |
| `fee_followup` | family_id, remark, next_date, by | Defaulters (FR 10.16) |
| `voucher_batch` | parameters, file_id, created_by | |

As built (V004): adjustments are `fee_adjustment` + `fee_adjustment_decision` (approval is a separate row); there is no
`advance_credit` table (credit = the unallocated part of a payment, view `payment_credit`; allocation kinds PAYMENT, CREDIT,
REVERSAL, RELEASE); bills have no stored status (computed); `fee_run` covers bill and late-fee runs; voucher batches are
generated on demand; `message_outbox` queues receipt/reminder SMS. See DECISIONS D-31…D-35.

**Balance** = Σ bill_line.net + Σ adjustments − Σ allocations. It is computed from these rows and never stored as an editable
number. A cached materialised view per student and family is allowed for list performance.

## 8. Accounts (FR 11)

| Entity | Key fields | Notes |
|---|---|---|
| `ledger_account` | code, name, type `ASSET/LIABILITY/INCOME/EXPENSE/EQUITY`, subtype `CASH/BANK/FEE_INCOME/...`, display_order, status | Cash/bank accounts are ASSET subtype |
| `account_category` | name, type `INCOME/EXPENSE`, ledger_account_id | User-facing "heads" (FR 11.6) |
| `journal_entry` | date, type `FEE_RECEIPT/INCOME/EXPENSE/TRANSFER/VENDOR/SALARY/REVERSAL/OPENING`, source_type, source_id, memo, reversed_entry_id, approved_by | |
| `journal_line` | entry_id, ledger_account_id, debit, credit, cleared_at (bank reconciliation) | DB check: entry sums balance |
| `income_entry`, `expense_entry` | date, category_id, amount, account_id, vendor_id, reference, attachment_file_id, approval_status | Create journal entries |
| `vendor` | name, phone, address | Vendor balance from journal lines (FR 11.7) |
| `day_close` | account_id, date, opening, in, out, closing, counted_amount, signed_by | FR 11.9 |

As built (V005): income, expense, transfer, supplier bill and supplier payment are one insert-only table `money_entry`
(kind, Idempotency-Key, reversal link) with approval in `money_entry_decision`; vendor balances are computed from approved
supplier entries; bank reconciliation marks are rows in `bank_clearance` (journal lines stay unchanged); payroll uses
`salary_sheet` + `salary_sheet_line` with a database trigger that locks lines once the sheet leaves DRAFT. See D-36…D-40.

## 9. Attendance (FR 13)

| Entity | Key fields | Notes |
|---|---|---|
| `attendance_status` | code (P, A, L, LV, HD…), name, counts_as `PRESENT/ABSENT/LATE/LEAVE/EXCLUDED` | School-approved list |
| `student_attendance` | date, section_id, student_id, status, marked_by, marked_at, `UNIQUE(student_id, date)` | Section stored as at that date |
| `register_completion` | section_id, date, completed_by, completed_at | Missing-register alert (FR 13.2) |
| `staff_attendance` | date, staff_id, status, arrival_time, marked_by, `UNIQUE(staff_id, date)` | Late threshold by category |
| `attendance_correction` | target, old_status, new_status, reason, by | Or `audit_log` |
| `leave_application` | applicant_type, applicant_id, leave_type, from, to, days, reason, status, decided_by | Approved leave → LEAVE status |
| `staff_month_summary` | staff_id, month, totals, approved_by | Feeds payroll |

## 10. Exams (FR 14)

| Entity | Key fields | Notes |
|---|---|---|
| `exam` | academic_year_id, name, start_date, end_date, comments, counts_in_cumulative, weight NUMERIC(5,2), visible, version | Name unique per year (case-insensitive) |
| `exam_class` | exam_id, class_id, grade_scale_id, status `DRAFT/REVIEWED/PUBLISHED`, reviewed_by/at, published_by/at, published_version, unlocked_by/at, unlock_reason | Workflow per class (D-49) |
| `exam_subject` | exam_class_id, subject_id, max_marks, passing_marks, display_order, paper_date, start_time, end_time, syllabus | A paper; copied from `class_subject`, editable while draft; also the datesheet |
| `mark` | exam_subject_id, student_id, obtained NUMERIC(6,2), grade, flag `ABSENT/EXEMPT`, entered_by, entered_at | Exactly one of obtained / grade / flag; no row = missing (D-47) |
| `mark_change` | exam_subject_id, student_id, old_value, new_value, reason, changed_by, changed_at | Insert-only log of changes after first publishing (FR 14.11) |
| `grade_scale`, `grade_band` | scale: name, kind `MARKS/GRADES`, is_default; band: grade, min_percent (inclusive), remark, passing, display_order | One default scale per kind per school, seeded on school creation (D-14) |
| `result_remark` | exam_class_id, student_id, teacher_remark, principal_remark, updated_by | One row per student per exam class |
| `result_snapshot` | exam_class_id, student_id, version, section_id, total_obtained, total_max, percent, grade, position, passed, data JSONB, published_by/at | Insert-only; frozen at publish; a correction adds a new version (D-49) |

Simple test marks (D-51), the tables in use on screen:

| Entity | Key fields | Notes |
|---|---|---|
| `class_test` | academic_year_id, section_id, subject_id, name, test_date, max_marks | Unique per section, subject, day and name; can be deleted only with no marks |
| `test_mark` | class_test_id, student_id, obtained, absent, entered_by/at | No row = not entered |
| `test_mark_change` | class_test_id, student_id, old_value, new_value, reason, changed_by/at | Insert-only log of changes to saved marks |

## 11. Office, communication, website (FR 3, 8.6, 16)

| Entity | Notes |
|---|---|
| `task`, `task_share` | description, deadline, priority NORMAL/URGENT, status, completed_at |
| `complaint`, `complaint_remark` | parent/family, subject, status OPEN/CLOSED, assigned_to, opened_at |
| `circular` | title, body, circular_date, created_by | For staff; families are told through Messages (D-56) |
| `school_event` | title, date_from, date_to, type EVENT/HOLIDAY/EXAM, show_on_website |
| `visitor_entry` | date, name, purpose, time_in | Count only on dashboard (visitor mgmt out of scope) |
| `message_template` | code, body with placeholders (English) | Only customised wording is stored |
| `user_favourite` | user_id, href, label, display_order | Quick bar per user (FR 3.12) |
| `message_outbox` | channel SMS/WHATSAPP/PUSH, recipient, body, status, attempts, provider_ref, related entity |
| `web_page`, `web_post`, `web_gallery`, `web_photo`, `web_download`, `web_setting` | status DRAFT/PUBLISHED/REMOVED, publish_at, owner_user_id, last_edited_by |
| `web_form_submission` | type ADMISSION/JOB/FEEDBACK, payload jsonb, file_id, read_at, linked_enquiry_id |
| `web_form_remark` | remark history |

## 12. Integrity rules checklist
- Every tenant table has `school_id NOT NULL`, an RLS policy, and `school_id` in its unique and foreign keys. A test (`TenantIsolationIT.everyTenantTableHasRowLevelSecurity`) verifies this from the catalogue.
- Exactly one CURRENT academic year per school.
- No hard deletes on: students, staff, families, bills, payments, journal, attendance, audit, result snapshots. A mark can be cleared (back to missing) only while its class has never been published; after that marks are corrected, never removed, and each change is logged.
- `journal_line` sums per entry: Σdebit = Σcredit (deferred constraint trigger).
- `mark.obtained` must be between 0 and `exam_subject.max_marks` (0 and 2 decimals in the DB; the maximum in the service, which also refuses lowering a maximum below marks already entered).
- A student has at most one open `enrolment` per academic year at any date (exclusion constraint on a daterange).
- A receipt number is unique per school and academic year, with no gaps.
- Attendance cannot be marked on OFF days or holidays unless the user overrides with a reason.
