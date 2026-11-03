# Requirements Traceability Matrix

Source labels (from the FR document): **OBS** = observed in the current system · **SCR** = shown in the dashboard screenshot ·
**NC** = in the current system but NOT confirmed · **REC** = recommended for the new system · **DEC** = added by a project decision.
Language: the FR mentions Urdu messages/letters (4.3, 10.16). These are **removed permanently**: the system is English only (DECISIONS D-18).
Priority: **M** must · **S** should · **C** could/later. Phase numbers refer to [IMPLEMENTATION-PLAN.md](IMPLEMENTATION-PLAN.md).
The QA column gives the test suite prefix in [QA-TEST-PLAN.md](QA-TEST-PLAN.md).

Total: 158 FR features (FR 8.7 parent portal is out of scope, see DECISIONS D-19) + 9 platform features (P-x, from DECISIONS D-01). Status starts as `Planned` and is updated as work completes (`In progress` → `Done` → `Verified`).

## 0. Platform (multi-school + super admin), from DECISIONS D-01, not in the FR
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| P-1 | Super admin account (TOTP mandatory), platform sign-in | DEC | M | 1 | platform | QA-PLT | Done |
| P-2 | Create / edit school (code, name, status) with default seeding | DEC | M | 1 | platform | QA-PLT | Done |
| P-3 | Create / reset each school's main administrator | DEC | M | 1 | platform | QA-PLT | Done |
| P-4 | Suspend / reactivate school | DEC | M | 1 | platform | QA-PLT | Done |
| P-5 | Tenant isolation (session tenant, Hibernate tenant filter, PostgreSQL RLS, tenant-prefixed files) | DEC | M | 0–1 | shared | QA-TEN | Done (DB, JPA, files, sessions) |
| P-6 | School website domain mapping (host → school) | DEC | M | 9 | platform, website | QA-PLT | Partial (mapping built, unused; website dropped, D-63) |
| P-7 | Switch into any school with full access (view + edit, all modules), audited | DEC | M | 1 | platform | QA-PLT | Done |
| P-8 | Usage overview, platform audit log, school export/offboarding | DEC | S | 1/10 | platform | QA-PLT | Partial (usage + platform audit; export Phase 10) |
| P-9 | All-schools overview: key figures per school, drill-down, cross-school reports and Excel export | DEC | M | 1/8 | platform, reports | QA-PLT | Done |
## 1. Administrator & School Setup
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 1.1 | School profile and identity (+ document identity) | OBS | M | 1 | school | QA-SET | Done |
| 1.2 | Academic year set-up and switching | OBS | M | 1 | school | QA-SET | Done |
| 1.3 | Opening a new academic year (rollover wizard) | REC | M | 2 | school, students | QA-SET | Done |
| 1.4 | Working days and holidays | REC | M | 1 | school | QA-SET, QA-ATT | Done |
| 1.5 | Class and section structure | OBS | M | 1 | academics | QA-ACA | Done |
| 1.6 | Main administrator account | OBS | M | 1 | iam | QA-SEC | Done |
| 1.7 | User roles and permissions set-up | OBS | M | 1 | iam | QA-SEC | Done |
| 1.8 | General preferences | OBS | M | 1 | settings | QA-SET | Done |

## 2. Login System and User Permissions
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 2.1 | Sign in / sign out, inactivity timeout | OBS | M | 1 | iam | QA-SEC | Done |
| 2.2 | Change password | OBS | M | 1 | iam | QA-SEC | Done |
| 2.3 | Secure password reset (no password display) | REC | M | 1 | iam | QA-SEC | Done |
| 2.4 | Account activation/deactivation | REC | M | 1 | iam | QA-SEC | Done |
| 2.5 | Role-based access + data scope | OBS | M | 1 | iam | QA-SEC | Done |
| 2.6 | Protection of sensitive information (per-user step-up) | OBS | M | 1 | iam | QA-SEC | Done (email code) |
| 2.7 | Login history and failed attempts, lockout | OBS | M | 1 | iam | QA-SEC | Done |
| 2.8 | Activity history (audit trail with old/new values) | OBS | M | 1 | shared/audit | QA-SEC | Done |

## 3. Main Dashboard
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 3.1 | Key totals: students, parents, staff (+ gender split) | SCR | M | 7 | dashboard | QA-DSH | Done |
| 3.2 | Online website forms counters | SCR | M | 9 | dashboard, website | QA-DSH | Dropped (D-63) |
| 3.3 | Parent complaints summary | SCR | M | 7 | office | QA-DSH | Done |
| 3.4 | Student and staff birthdays | SCR | S | 7 | office | QA-DSH | Done |
| 3.5 | Task list | SCR | S | 7 | office | QA-DSH | Done |
| 3.6 | Attendance status and charts | SCR | M | 7 | dashboard, attendance | QA-DSH | Done |
| 3.7 | Recent admissions and withdrawals | SCR | M | 7 | dashboard | QA-DSH | Done |
| 3.8 | Logins panel + new location alert | SCR | S | 7 | iam | QA-DSH | Done |
| 3.9 | Visitors summary (count only) | SCR | C | 7 | office | QA-DSH | Done |
| 3.10 | Campus events calendar | SCR | M | 7 | office | QA-DSH | Done |
| 3.11 | Latest circulars | SCR | M | 7 | office | QA-DSH | Done |
| 3.12 | Quick links, favourites, alerts bar | SCR | S | 7 | dashboard | QA-DSH | Done (no tutorial videos) |
| 3.13 | Urgent-matters highlight (≤ 5 lines) | REC | M | 7 | dashboard | QA-DSH | Done |

## 4. New Student Admission
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 4.1 | Record a new enquiry | OBS | M | 2 | admissions | QA-ADM | Done |
| 4.2 | Enquiry statuses (custom, coloured) | OBS | M | 2 | admissions | QA-ADM | Done |
| 4.3 | Follow-up remarks and messages (English only) | OBS | M | 2/7 | admissions, communication | QA-ADM | Planned |
| 4.4 | Entry test / interview outcome | OBS | S | 2 | admissions | QA-ADM | Done |
| 4.5 | Search and avoid duplicates | REC | M | 2 | admissions | QA-ADM | Done |
| 4.6 | Convert approved enquiry to student | REC | M | 2 | admissions, students | QA-ADM | Done |
| 4.7 | Admission form (GR suggestion) | OBS | M | 2 | students | QA-ADM | Done |
| 4.8 | Link parent/family record | OBS | M | 2 | families | QA-ADM | Done |
| 4.9 | Documents and photo | OBS | M | 2 | students, shared/files | QA-ADM | Done |
| 4.10 | Admission fee and initial charges | REC | M | 3 | fees | QA-FEE | Done |
| 4.11 | Admission terms, syllabus, printable form | OBS | S | 2 | admissions | QA-ADM | Done |
| 4.12 | Welcome message | OBS | S | 7 | communication | QA-COM | Done |

## 5. Student Management
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 5.1 | Student profile (tabs) | OBS | M | 2 | students | QA-STU | Done |
| 5.2 | Find a student quickly + class strength | OBS | M | 2 | students | QA-STU | Done |
| 5.3 | Update details + bulk field update + history | OBS | M | 2 | students | QA-STU | Done |
| 5.4 | Move to another class/section (effective date) | REC | M | 2 | students | QA-STU | Done |
| 5.5 | Promotion to next academic year | REC | M | 2 | students | QA-STU | Done |
| 5.6 | Withdrawal / leaving (balance check, bad debt) | OBS | M | 2/3 | students, fees | QA-STU | Planned |
| 5.7 | Leavers and historical records | OBS | M | 2 | students | QA-STU | Done |
| 5.8 | ID cards and certificates | OBS | M | 2 | students, shared/pdf | QA-STU | Done |
| 5.9 | Printable records (bio-data, registers) | OBS | S | 2 | students | QA-STU | Done |
| 5.10 | Student activities (houses, clubs, shining stars) | OBS | C | 2 | students | QA-STU | Done |
| 5.11 | Privacy of student information | REC | M | 2 | iam, students | QA-SEC | Done |

## 6. Teacher Management
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 6.1 | Teacher record | OBS | M | 2 | staff | QA-STF | Done |
| 6.2 | Login account for teachers | OBS | M | 2 | iam | QA-STF | Done |
| 6.3 | Assign classes and subjects | OBS | M | 2 | academics | QA-STF | Done |
| 6.4 | Teacher timetable | OBS | S | 2 | academics | QA-STF | Done |
| 6.5 | Attendance and leave view | OBS | M | 5 | attendance | QA-ATT | Done |
| 6.6 | Assignment changes (bulk, effective date) | OBS | M | 2 | academics | QA-STF | Done |
| 6.7 | Teacher leaving + experience certificate | OBS | M | 2 | staff | QA-STF | Done |
| 6.8 | Connection to salary | OBS | M | 4 | payroll | QA-PAY | Done |

## 7. Other Staff Management
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 7.1 | Non-teaching staff record | OBS | M | 2 | staff | QA-STF | Done |
| 7.2 | Login and permissions for staff | OBS | M | 2 | iam | QA-STF | Done |
| 7.3 | Attendance and leave (own start time) | OBS | M | 5 | attendance | QA-ATT | Done |
| 7.4 | Inactive staff and history | OBS | M | 2 | staff | QA-STF | Done |

## 8. Parent & Family Management
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 8.1 | Family record | OBS | M | 2 | families | QA-FAM | Done |
| 8.2 | Find all siblings | OBS | M | 2 | families | QA-FAM | Done |
| 8.3 | Separate guardians + message routing | REC | M | 2 | families | QA-FAM | Done |
| 8.4 | Safe updates (confirm relink, history) | REC | M | 2 | families | QA-FAM | Done |
| 8.5 | Bulk family updates and data checks | OBS | S | 2 | families | QA-FAM | Done |
| 8.6 | Communication with families (SMS/WhatsApp) | OBS | M | 7 | communication | QA-COM | Done |
| 8.7 | Parent portal / app access | NC | — | — | — | — | **Out of scope** (D-19) |

## 9. Classes, Sections & Subjects
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 9.1 | Academic years | OBS | M | 1 | school | QA-SET | Done |
| 9.2 | Classes (grading mode, order) | OBS | M | 1 | academics | QA-ACA | Done |
| 9.3 | Sections (+ section class teacher) | OBS | M | 1 | academics | QA-ACA | Done |
| 9.4 | Subjects and maximum/passing marks | OBS | M | 1 | academics | QA-ACA | Done |
| 9.5 | Assign class teacher and subject teachers | OBS | M | 2 | academics | QA-ACA | Done |
| 9.6 | Effect of class/section changes (history) | REC | M | 1/2 | academics | QA-ACA | Partial (rename/deactivate done; dated section history with enrolments in Phase 2) |
| 9.7 | Deletion safeguards ("where used") | OBS | M | 1 | academics | QA-ACA | Done |

## 10. Fee System
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 10.1 | Fee head set-up | OBS | M | 3 | fees | QA-FEE | Done |
| 10.2 | Fee amounts by class/student/family | OBS | M | 3 | fees | QA-FEE | Done |
| 10.3 | Discounts, scholarships, concessions (+ approval) | OBS | M | 3 | fees | QA-FEE | Done |
| 10.4 | Sibling discount | REC | S | 3 | fees | QA-FEE | Done |
| 10.5 | Generate fee bills | OBS | M | 3 | fees | QA-FEE | Done |
| 10.6 | Fee vouchers | OBS | M | 3 | fees, shared/pdf | QA-FEE | Done |
| 10.7 | Record full or partial payments | OBS | M | 3 | fees | QA-FEE | Done |
| 10.8 | Receipts (print + SMS) | OBS | M | 3 | fees | QA-FEE | Done |
| 10.9 | Family payments | OBS | M | 3 | fees | QA-FEE | Done |
| 10.10 | Advance payments and carry-forward | OBS | M | 3 | fees | QA-FEE | Done |
| 10.11 | Late fee | OBS | M | 3 | fees | QA-FEE | Done |
| 10.12 | Absent fine (if policy allows) | OBS | C | 5 | fees | QA-FEE | Done |
| 10.13 | Adjustments and corrections | OBS | M | 3 | fees | QA-FEE | Done |
| 10.14 | Student and family ledgers | OBS | M | 3 | fees | QA-FEE | Done |
| 10.15 | Fee status views | OBS | M | 3 | fees | QA-FEE | Done |
| 10.16 | Fee reminders and defaulters | OBS | M | 3 | fees, communication | QA-FEE | Done |
| 10.17 | Fee reports | OBS | M | 3 | fees | QA-FEE | Done |
| 10.18 | Traceability of every payment | REC | M | 3 | fees, shared/audit | QA-FEE | Done |
| 10.19 | Online fee payment (future) | REC | C | later | fees | — | Deferred |

## 11. School Accounts
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 11.1 | Cash and bank accounts | OBS | M | 4 | accounts | QA-ACC | Done |
| 11.2 | Transfers between accounts | OBS | M | 4 | accounts | QA-ACC | Done |
| 11.3 | Account ledger | OBS | M | 4 | accounts | QA-ACC | Done |
| 11.4 | Income entries (non-fee) | NC | M | 4 | accounts | QA-ACC | Done |
| 11.5 | Expense entries (+ approval) | NC | M | 4 | accounts | QA-ACC | Done |
| 11.6 | Account categories (heads) | NC | M | 4 | accounts | QA-ACC | Done |
| 11.7 | Supplier/vendor payments | OBS | M | 4 | accounts | QA-ACC | Done |
| 11.8 | Fee collections feed accounts once | REC | M | 3/4 | fees, accounts | QA-ACC | Done |
| 11.9 | Daily cash summary | REC | M | 4 | accounts | QA-ACC | Done |
| 11.10 | Bank reconciliation | REC | S | 4 | accounts | QA-ACC | Done |
| 11.11 | Corrections, reversals, approvals | REC | M | 4 | accounts | QA-ACC | Done |
| 11.12 | Financial activity history | OBS | M | 4 | shared/audit | QA-ACC | Done |
| 11.13 | Financial reports (balance sheet = S) | NC | M | 4 | accounts | QA-ACC | Done |

## 12. Rules and Settings
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 12.1 | Central settings area (all groups) | OBS | M | 1 | settings | QA-SET | Done |
| 12.2 | Restricted change and history | REC | M | 1 | settings | QA-SET | Done |
| 12.3 | Consistent application of rules | REC | M | all | settings | QA-SET | Done (consumers added per phase) |

## 13. Attendance
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 13.1 | Mark daily class attendance (default present) | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.2 | Missing register alert | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.3 | Correct attendance with permission (bulk/holiday) | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.4 | Daily and monthly registers | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.5 | Attendance percentage | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.6 | Repeated absences | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.7 | Student attendance reports | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.8 | Leave applications (entered by office; no portal) | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.9 | Daily staff attendance | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.10 | Staff attendance corrections | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.11 | Monthly staff summaries | OBS | M | 5 | attendance | QA-ATT | Done |
| 13.12 | Link to salary | OBS | M | 4/5 | payroll | QA-PAY | Done |

## 14. Examinations and Results
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 14.1 | Create exams or terms | OBS | M | 6 | exams | QA-EXM | Hidden (D-51) |
| 14.2 | Subjects, maximum and passing marks | OBS | M | 6 | exams | QA-EXM | Hidden (D-51) |
| 14.3 | Exam datesheet | OBS | M | 6 | exams | QA-EXM | Hidden (D-51) |
| 14.4 | Admit cards and roll number slips | OBS | S | 6 | exams | QA-EXM | Hidden (D-51) |
| 14.5 | Enter and correct marks | OBS | M | 6 | exams | QA-EXM | Done |
| 14.6 | Absent students and missing marks | REC | M | 6 | exams | QA-EXM | Done |
| 14.7 | Totals, percentages and grades | OBS | M | 6 | exams | QA-EXM | Done |
| 14.8 | Class positions (optional) | OBS | S | 6 | exams | QA-EXM | Done |
| 14.9 | Teacher and overall remarks | OBS | M | 6 | exams | QA-EXM | Done |
| 14.10 | Review before publishing | REC | M | 6 | exams | QA-EXM | Hidden (D-51) |
| 14.11 | Controlled correction after publishing | REC | M | 6 | exams | QA-EXM | Hidden (D-51) |
| 14.12 | Result sheets and analysis | OBS | M | 6 | exams | QA-EXM | Partial: tables, no graphs; hidden (D-51) |
| 14.13 | Report cards | OBS | M | 6 | exams, shared/pdf | QA-EXM | Hidden (D-51) |
| 14.14 | Sharing results with parents (SMS + printed report card; no portal) | OBS | M | 6 | exams, shared/messaging | QA-EXM | Hidden (D-51) |
| 14.15 | Results kept by academic year | OBS | M | 6 | exams | QA-EXM | Done |

## 15. Reports
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 15.1 | Report filters (in heading) | REC | M | 8 | reports | QA-RPT | Done |
| 15.2 | Student weekly/monthly report | REC | M | 8 | reports | QA-RPT | Done |
| 15.3 | Teacher weekly/monthly report | REC | S | 8 | reports | QA-RPT | Done |
| 15.4 | Class weekly/monthly report | REC | M | 8 | reports | QA-RPT | Done |
| 15.5 | Existing detailed reports | OBS | M | 3–6 | various | QA-RPT | Done |
| 15.6 | Print, PDF and Excel export | OBS | M | all | shared | QA-RPT | Done |
| 15.7 | Access restrictions in reports | REC | M | 8 | reports, iam | QA-RPT | Done |

## 16. School Website
| ID | Feature | Src | Pri | Ph | Module | QA | Status |
|---|---|---|---|---|---|---|---|
| 16.1 | Identity and contact information | OBS | M | 9 | website | QA-WEB | Dropped (D-63) |
| 16.2 | Home page content | OBS | M | 9 | website | QA-WEB | Dropped (D-63) |
| 16.3 | About, message, mission, team pages | OBS | M | 9 | website | QA-WEB | Dropped (D-63) |
| 16.4 | Announcements, news and events | OBS | M | 9 | website | QA-WEB | Dropped (D-63) |
| 16.5 | Photo galleries | OBS | S | 9 | website | QA-WEB | Dropped (D-63) |
| 16.6 | Important dates, admission info, downloads | REC | S | 9 | website | QA-WEB | Dropped (D-63) |
| 16.7 | Online admission enquiry form | OBS | M | 9 | website, admissions | QA-WEB | Dropped (D-63) |
| 16.8 | Job application form | OBS | S | 9 | website | QA-WEB | Dropped (D-63) |
| 16.9 | Feedback / contact form | OBS | M | 9 | website | QA-WEB | Dropped (D-63) |
| 16.10 | Edit, preview, publish, remove | REC | M | 9 | website | QA-WEB | Dropped (D-63) |
| 16.11 | Responsibility and private-data separation | REC | M | 9 | website | QA-WEB | Dropped (D-63) |

## Supporting capabilities implied by the FR but not defined as features
| Capability | Implied by | Ph | Module |
|---|---|---|---|
| Tasks, complaints, circulars, events, visitor count | FR 3.3, 3.5, 3.9–3.11 | 7 | office |
| Messaging templates, outbox, delivery log | FR 4.3, 8.6, 10.8, 10.16, 14.14 | 7 (stub from 2) | communication |
| Payroll / salary sheets | FR 6.8, 12 (Salary), 13.12 | 4 | payroll |
| Legacy data migration | Audit of current system (data back to 2016-2017) | 11 | migration tooling |
| Background jobs UI | FR 10.5, 10.6 batch work | 3 | fees (bill runs list with "not finished — run again"; DECISIONS D-31) |
