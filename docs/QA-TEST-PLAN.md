# QA Test Plan

> Scope: all 16 modules in the FR v1.0, plus the non-functional requirements in ARCHITECTURE §5 and SECURITY.md.
> Each test case ID (`QA-<SUITE>-NNN`) traces to FR IDs. Every FR "School example" becomes at least one test.

## 1. Quality goals (highest priority first)
0. **Schools never see each other's data:** tenant isolation holds at the API, query, database (RLS) and file layers (DECISIONS D-01).
1. **Money is never wrong:** no lost, duplicated or mis-rounded rupee; every payment is traceable (FR 10.18, 11.8).
2. **Private data stays private:** role and data-scope rules are enforced on the API, not only in the UI (FR 2.5, 5.11, 15.7).
3. **History is preserved:** closed years, moved students, left staff and published results keep correct history (FR 1.3, 5.4, 9.6, 14.11).
4. **Rules apply consistently:** one setting changes every screen, printout and message (FR 12.3).
5. **Daily work is fast and usable:** attendance marking, fee collection and marks entry work quickly and accessibly.

## 1a. Automated coverage so far
| Suite | Where |
|---|---|
| QA-TEN | `backend/.../shared/tenant/TenantIsolationIT` (13 cases), `PlatformIT`, `SchoolSetupIT` (cross-school files), `SettingsIT` (per-school values) |
| QA-PLT | `backend/.../platform/PlatformIT` (8), `frontend/tests/e2e/phase1.spec.ts` |
| QA-SEC | `backend/.../iam/SignInFlowIT` (12), `IamIT` (8), `PasswordAndLocationTest`, `TotpTest`, `ArchitectureTest.everyEndpointDeclaresItsAccessRule`, `frontend/tests/e2e/sign-in.spec.ts` |
| QA-SET | `SchoolSetupIT` (7), `SettingsIT` (3), e2e rule change with step-up |
| QA-ACA | `AcademicsIT` (2), e2e class + section |
| QA-ADM | `AdmissionsIT` (6), `frontend/tests/e2e/phase2.spec.ts` (enquiry → approve → enrol) |
| QA-STU | `StudentsIT` (6), `RolloverIT` (2), e2e withdrawal + leaving certificate, e2e cross-school search |
| QA-FAM | `FamiliesIT` (4) |
| QA-STF | `StaffIT` (3), e2e add staff |
| QA-FEE | `backend/.../fees/FeesIT` (18: idempotent runs and payments, missing amounts, discounts and approval, rounding, siblings, partial/family/advance payments, two cashiers, atomic failure, late fee, corrections, reversal, insert-only grants, other school, receipt sequence, 40-step random reconciliation), `frontend/src/lib/money.test.ts`, `frontend/tests/e2e/phase3.spec.ts` |
| QA-ACC | `backend/.../accounts/AccountsIT` (7: transfer, approval limit, reversal, idempotency + insert-only, day close incl. fee receipts and lock, suppliers + bank reconciliation, reports vs journal, other school), `frontend/tests/e2e/phase4.spec.ts` |
| QA-PAY | `backend/.../payroll/PayrollIT` (4: increment month, late-as-absent formula, approve → pay once → locked → reverse, permissions and step-up), e2e prepare and discard a sheet |
| QA-ATT | `backend/.../attendance/AttendanceIT` (10: register default and completion, missing list, closed days and corrections, teacher scope, % formula, days in a row, leave over marked days, staff late by category → approved month → payroll, moved student and monthly PDF, bulk change + absent fine, other school), `frontend/tests/e2e/phase5.spec.ts` |
| API contract | `ArchitectureTest.apiTypesHaveUniqueNames` (no two API types share a schema name), `OpenApiExportIT` |

## 2. Test levels and tools

| Level | What | Tools | Owner | Runs |
|---|---|---|---|---|
| Unit (backend) | Domain rules: money, allocation, grading, positions, attendance %, deductions | JUnit 5, AssertJ, jqwik (property-based for money) | Dev | Every push |
| Unit (frontend) | Components, form schemas, formatters (money, dates) | Vitest, Testing Library | Dev | Every push |
| Integration | Repositories, transactions, Flyway, batch jobs, security config | Spring Boot Test + **real PostgreSQL** (embedded zonky binaries; never H2) | Dev | Every push |
| Architecture | Module boundaries, no `double` for money, no controller→repository | Spring Modulith / ArchUnit | Dev | Every push |
| API contract | OpenAPI drift; generated TS types compile | springdoc + openapi-typescript in `scripts/verify.sh` | Dev | Every merge |
| Tenant isolation | Every endpoint as School A against School B IDs; RLS checks with raw SQL | Integration tests (two seeded schools) | Dev + QA | Every push |
| API permission matrix | Every endpoint × every role × scope | Parameterised integration tests (REST Assured / MockMvc) | Dev + QA | Every push |
| E2E | Critical journeys in a browser against staging-like stack | Playwright (Chromium, WebKit, Firefox; desktop + tablet viewport) | QA | Nightly + pre-release |
| Accessibility | WCAG 2.2 AA | axe-core in Playwright, manual keyboard + NVDA spot checks | QA | Per phase |
| Visual / PDF | Vouchers, receipts, report cards, certificates | PDF text extraction + image snapshot diff | QA | Per phase |
| Performance | Bill run, voucher batch, dashboard, list pages | k6 (API), Lighthouse CI (website) | QA | Phase 3, 7, 11 |
| Security | OWASP ASVS L2 checklist, dependency/secret scanning, DAST | OWASP ZAP baseline, `npm audit` + monthly dependency review, secrets check in `scripts/verify.sh` | QA + Dev | Every merge + Phase 10 |
| Backup/restore | Restore dump to a fresh DB and verify | Scripted drill | DevOps | Monthly + Phase 10 |
| UAT | Real users following scripts | Staging with migrated anonymised data | School | Phase 10 |

Coverage targets: domain/service packages ≥ 85% line and 75% branch. `fees`, `accounts`, `payroll`, `exams/grading` ≥ 95% branch.
Coverage is a floor, not a goal. Tests must assert business outcomes.

## 3. Environments and test data
- **Local:** `LocalDevApplication` (embedded PostgreSQL + two demo schools, no Docker needed). File storage, email and SMS use fake adapters until their phases.
- **Quality gate:** `scripts/verify.sh` runs the same embedded PostgreSQL inside `mvn verify` before every merge and deploy.
- **Staging:** mirrors production. At least **two schools** are seeded: "NNS demo" (below) and a smaller "Second demo school"
  (3 classes, 60 students) so isolation is always exercised. The NNS demo dataset: 2 academic years, 12 classes (Play Group–Class 10), 807 students,
  585 families, 15 staff, fee structures, 3 months of bills/payments, attendance and one published exam.
  The seed generator is deterministic (fixed random seed) so tests can reference known records.
- **Production-sized load set:** 3× the above (≈2,400 students) for performance tests.
- **Rules:** never use real student personal data outside production. Migrated data used in UAT is anonymised
  (names, CNIC, phones scrambled; balances kept).
- The SMS/WhatsApp provider is a fake adapter everywhere except production. A staging allow-list sends only to QA phones.

## 4. Entry and exit criteria per phase
**Entry:** feature meets the Definition of Ready, deployed to staging, migrations applied cleanly, seed data loaded.
**Exit (phase sign-off):**
- 100% of the phase's **M** test cases pass. **S** cases ≥ 90% pass, with failures logged.
- No open **S1/S2** defects. S3 defects have an agreed fix plan.
- Permission matrix tests are green for all new endpoints.
- axe-core reports no serious or critical violations on new screens.
- Regression E2E suite is green.

### Defect severity
| Sev | Definition | Example |
|---|---|---|
| S1 Critical | Data loss/corruption, wrong money, privacy breach, security hole, system down | Payment saved without receipt; teacher sees fee balances |
| S2 Major | Core workflow blocked, no workaround | Cannot generate October bills for a class |
| S3 Minor | Workaround exists, or a non-core feature is wrong | Datesheet image misaligned |
| S4 Trivial | Cosmetic/text | Typo in a label |

Any cross-school data exposure is always **S1**.

## 5. Critical test cases by suite

Legend: **P** = priority (M/S). Unless stated, each case also checks that an audit record is written where applicable.

### QA-PLT: Platform and super admin (P-1…P-9)
| ID | Ref | P | Scenario | Expected |
|---|---|---|---|---|
| QA-PLT-001 | P-1 | M | Super admin sign-in without TOTP / wrong TOTP | Rejected; event logged |
| QA-PLT-002 | P-2 | M | Create school "ABC" | School row + seeded roles, statuses, grade scale, settings defaults; platform audit entry |
| QA-PLT-003 | P-2 | M | Create a school with a duplicate code | 409 |
| QA-PLT-004 | P-3 | M | Create the main admin for ABC | One-time setup link works once and expires in 48 h; admin can sign in only with school code ABC |
| QA-PLT-005 | P-4 | M | Suspend ABC while its users are signed in | Sessions revoked; new sign-ins get 423 SCHOOL_SUSPENDED; ABC website shows maintenance; other schools unaffected |
| QA-PLT-006 | P-7 | M | Super admin opens NNS, then ABC | Sees NNS data with every module (incl. accounts, salaries); after switching, sees only ABC data; banner shows the current school |
| QA-PLT-007 | P-7 | M | Super admin edits a student and records a payment in NNS | Saved; NNS audit log shows actor "Super admin <name>"; platform audit log has SWITCH_SCHOOL + actions |
| QA-PLT-011 | P-9 | M | All-schools overview with 2 seeded schools | Per-school figures match each school's own dashboard/report totals; grand total = sum |
| QA-PLT-012 | P-9 | M | School admin (not super admin) calls `/platform/overview/**` | 403; `app.all_schools` never set |
| QA-PLT-013 | P-9 | S | Cross-school Excel export | One sheet/section per school; totals correct |
| QA-PLT-008 | P-1 | M | School admin calls `/platform/**` | 403 |
| QA-PLT-009 | P-6 | M | Request `www.nns.edu.pk` and `www.abc.edu.pk` | Each shows only its own school's content; unknown host shows a 404 page |
| QA-PLT-010 | P-8 | S | Export ABC | ZIP contains only ABC rows and files |

### QA-TEN: Tenant isolation (P-5). **Always S1 if any case fails.**
| ID | P | Scenario | Expected |
|---|---|---|---|
| QA-TEN-001 | M | **Automated sweep:** for every GET/PATCH/POST action endpoint, a School A user uses School B's record IDs | 404 for all; nothing modified |
| QA-TEN-002 | M | Same username "admin" in two schools | Each signs in only with its own school code; sessions are separate |
| QA-TEN-003 | M | Raw SQL as the app DB role without `app.school_id` set | Returns zero rows (RLS); insert fails |
| QA-TEN-004 | M | Bug simulation: repository query missing the tenant filter | RLS still blocks other schools' rows |
| QA-TEN-005 | M | Foreign key to another school's row (e.g. bill for School B's student) | Rejected by the composite FK |
| QA-TEN-006 | M | Nightly batch (late fee) runs | Each school is processed with its own settings; no cross-school writes; job_run per school |
| QA-TEN-007 | M | Presigned URL request for School B's file key | 404 |
| QA-TEN-008 | M | Search, dashboard, reports, Excel export | Counts and rows only for the signed-in school |
| QA-TEN-009 | M | Receipt numbers / GR numbers | Independent sequences per school; same number allowed in different schools |
| QA-TEN-010 | M | Public website form posted with a forged `X-School-Host` from a browser | Rejected (signature check) |
| QA-TEN-011 | M | Thread reuse: request for School A, then School B on the same worker thread | Tenant context cleared; no leakage (concurrency test) |
| QA-TEN-012 | M | Caches (settings, dashboard) | Cache keys include school_id; School A's setting change doesn't affect School B |

### QA-SEC: Login, permissions, audit (FR 2, 1.6, 1.7, 5.11)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-SEC-001 | 2.1 | M | Valid sign-in, then sign-out | Session cookie set HttpOnly/Secure/SameSite; after sign-out, old cookie returns 401 |
| QA-SEC-002 | 2.1 | M | Idle beyond timeout | Next request returns 401 SESSION_EXPIRED; UI redirects to sign-in, unsaved-form warning shown |
| QA-SEC-003 | 2.2 | M | Change password with wrong current password | Rejected; no change; event logged |
| QA-SEC-004 | 2.3 | M | Reset via OTP: expired, reused, wrong code | All rejected; rate-limited after 5 tries |
| QA-SEC-005 | 2.3 | M | Admin assisted reset | Temporary password works once and forces a change; no screen anywhere shows an existing password |
| QA-SEC-006 | 2.4 | M | Deactivate a user who is signed in elsewhere | Their active sessions die immediately; history records still show their name |
| QA-SEC-007 | 2.7 | M | 5 failed attempts | Account locked; `BLOCKED` events logged; admin can unlock |
| QA-SEC-008 | 2.7/3.8 | S | Sign-in from a new country/IP range | `new_location` flagged; dashboard alert shown |
| QA-SEC-010 | 2.5 | M | **Permission matrix:** every endpoint × every seeded role | Only allowed combinations return 2xx; others 403/404 (automated) |
| QA-SEC-011 | 2.5 | M | Class 4 teacher calls `GET /attendance?sectionId=<6-B>` and marks API for another section | 404/403; no data leaked |
| QA-SEC-012 | 2.5 | M | IDOR: teacher requests a student/bill/report-card file outside their assigned sections | 404; presigned URL not issued |
| QA-SEC-020 | D-19 | M | Try to create a user linked to a family/student, or sign in as a parent | Not possible: no such role, scope or endpoint |
| QA-SEC-013 | 5.11 | M | Teacher opens student profile | Academic data visible; fee balance, documents and contact numbers masked; API responses omit them too |
| QA-SEC-014 | 2.6 | M | Accountant opens accounts without step-up | 403 STEP_UP_REQUIRED; after OTP, access for N minutes only |
| QA-SEC-015 | 2.8 | M | Change a result after publish | Audit shows user, time, old and new values, reason |
| QA-SEC-016 | 2.8 | M | Attempt to UPDATE/DELETE `audit_log` with the app DB role | Denied by DB grants |
| QA-SEC-017 | — | M | CSRF: POST without `X-XSRF-TOKEN` (token from `/auth/csrf`) | 403 |
| QA-SEC-018 | — | M | CORS: request from an origin other than the app domain | Blocked |
| QA-SEC-019 | — | M | Hidden menu items | Menu matches role. Direct URL navigation still hits API 403 and shows a friendly page |
| QA-SEC-021 | 2.1 | M | Same user signs in from several devices at the same moment | All succeed; sign-in does not change the user's version, so an admin editing that user gets no false conflict |
| QA-SEC-022 | 2.1 | S | 31st sign-in attempt from one network within 5 minutes | 429 `RATE_LIMITED`; sign-in page says to wait a few minutes |

### QA-SET: School setup, years, settings (FR 1, 9.1, 12)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-SET-001 | 1.1 | M | Change school phone | Next generated voucher/receipt/report card shows the new number; old stored PDFs unchanged |
| QA-SET-002 | 1.2 | M | Switch header year to 2025-2026 | Lists, dashboard and reports show only that year's data; new entries are blocked if the year is closed |
| QA-SET-003 | 1.2 | M | Try to set two current years | Rejected (DB constraint + UI message) |
| QA-SET-004 | 1.3 | M | Rollover: copy classes, subjects and fee structure with +10% edit | New year has copied structure; old year read-only; rollover record stored |
| QA-SET-005 | 1.3 | M | Edit a closed-year bill without `year.correct_closed` | 409 YEAR_CLOSED |
| QA-SET-006 | 1.4 | M | Enter summer vacation 1 Jun–14 Aug | No attendance expected; no missing-register alerts; % excludes those days |
| QA-SET-010 | 12.1/12.3 | M | Change student late time 8:00 → 8:15 | Arrival 8:10 after the change is not late; earlier records are not recalculated |
| QA-SET-011 | 12.3 | M | Hide class positions | Removed from report card, result sheet **and** result SMS together |
| QA-SET-012 | 12.2 | M | Accountant tries to change the late-fee rule | Can view, cannot edit (API 403) |
| QA-SET-013 | 12.2 | M | Principal changes a setting | Confirmation + reason required; history shows old/new/user/date |

### QA-ACA: Classes, sections, subjects (FR 9)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-ACA-001 | 9.2 | M | Reorder classes | Order reflected in every picker and report |
| QA-ACA-002 | 9.7 | M | Delete "Drawing" that has marks | 409 IN_USE with usage list; deactivate offered and works |
| QA-ACA-003 | 9.6 | M | Merge sections mid-year | Pre-merge attendance still reports under the old section |
| QA-ACA-004 | 9.4 | M | Passing marks > max marks | Validation error |

### QA-ADM: Admissions (FR 4)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-ADM-001 | 4.1 | M | Add phone enquiry with DOB | Age auto-calculated; status New Enquiry; registration no. assigned |
| QA-ADM-002 | 4.2 | M | Custom status with colour | Appears in filters and badges |
| QA-ADM-003 | 4.5 | M | New enquiry with a phone matching last year's enquiry / an existing family | Possible matches shown; user can link instead of duplicating |
| QA-ADM-004 | 4.6 | M | Enrol approved enquiry | Admission form prefilled; on save: student + enrolment + family link + status Enrolled in **one** transaction; enquiry links to student |
| QA-ADM-005 | 4.6 | M | Simulate DB failure mid-enrolment | Nothing partially created |
| QA-ADM-006 | 4.7 | M | GR number suggestion conflicts with a concurrent enrolment | Second save gets a uniqueness error and a fresh suggestion |
| QA-ADM-007 | 4.9 | M | Upload a 30 MB file / an `.exe` renamed `.pdf` | Rejected by size / magic-byte check |
| QA-ADM-008 | 4.10 | M | Enrol into Class 4 | Admission fee + first month proposed from the fee structure; adjustment needs permission |
| QA-ADM-010 | 4.6 | M | Enrol an enquiry that is not approved | Enrol button disabled with a hint; API returns 422 `NOT_APPROVED` |
| QA-ADM-011 | 4.6 | M | Enquiry contact details for a role without `students.private.view` | Phone/CNIC/address hidden; editing keeps the stored values |
| QA-ADM-009 | 4.3 | S | Bulk SMS to Class 1 enquiries | Recipient preview; outbox rows; delivery log |

### QA-STU / QA-FAM / QA-STF: People (FR 5–8)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-STU-001 | 5.2 | M | Search by father's name, partial GR, phone | Results in < 500 ms on the 2,400-student set |
| QA-STU-002 | 5.3 | M | Bulk set board seat numbers for Class 10 | All saved; per-field history recorded |
| QA-STU-003 | 5.4 | M | Move 7-A → 7-B effective 1 Nov | October attendance/marks remain under 7-A; November under 7-B; lists correct on both sides of the date |
| QA-STU-004 | 5.5 | M | Promote Class 5 except two (one repeats, one leaves) | New-year enrolments correct; old-year class/results unchanged |
| QA-STU-005 | 5.6 | M | Withdraw with an outstanding balance | Balance shown; must choose: settle, carry, or write off as bad debt (permission); student inactive; appears in leavers & dashboard |
| QA-STU-006 | 5.8 | M | Leaving certificate | School heading, signatures and serial no. correct; serial numbers per school |
| QA-STU-008 | 5.8 | M | Leaving certificate for a student who has not left | Not offered in the UI; API returns 422 `NOT_LEFT` |
| QA-STU-009 | 5.11 | M | Edit a student as a role without `students.private.view` | B-Form/phone/address hidden and kept unchanged on save |
| QA-STU-010 | 1.3 | M | Copy set-up from the previous year into a new year | Chosen items copied; the earlier year unchanged; copying into an earlier or closed year refused |
| QA-STU-007 | 5.7 | M | Character certificate for a student who left 3 years ago | Record found in leavers; certificate issued |
| QA-FAM-001 | 8.2 | M | Open family with 3 children | All children and balances visible to the accountant |
| QA-FAM-002 | 8.3 | M | Grandmother receives attendance messages, father receives fee bills | Outbox routes each message type to the right person |
| QA-FAM-003 | 8.4 | M | Change a child's family link | Confirmation required; old/new recorded; sibling discounts recalculated from the next bill |
| QA-FAM-004 | 8.5 | S | Data-quality report | Lists invalid phones, missing CNIC, families with no children |
| QA-STF-001 | 6.3 | M | Assign Maths 8-A/8-B + class teacher 8-A | Teacher scope = 8-A, 8-B; attendance only for 8-A |
| QA-STF-002 | 6.6 | M | Bulk move all periods to a replacement from 1 Dec | Past marks still attributed to the original teacher |
| QA-STF-004 | 6.4 | M | Same teacher in two sections in the same period | Saving the second timetable fails with `TEACHER_DOUBLE_BOOKED` |
| QA-STF-005 | 6.1 | M | Salary change | New dated row with reason; earlier rows unchanged; needs `staff.salary.manage` + verification code |
| QA-STF-003 | 6.7/7.4 | M | Staff leaves | Login deactivated; removed from active lists/timetable; history and experience certificate available |

### QA-FEE: Fees (FR 10, 4.10). **Highest-risk suite.**
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-FEE-001 | 10.2 | M | Class with a missing amount for an active head | Flagged; bill run warns and skips/blocks per option |
| QA-FEE-002 | 10.3 | M | 50% staff-child discount on tuition 3,500 | Bill line net 1,750.00 |
| QA-FEE-003 | 10.3 | M | Discount above the no-approval threshold | Status PENDING; not applied until the principal approves |
| QA-FEE-004 | 10.4 | S | Third child admitted, rule 10%/20% | Third child's tuition −20% automatically; first two unchanged |
| QA-FEE-005 | 10.5 | M | Generate October tuition for the whole school | One line per student/head/period; job report counts match the enrolled count |
| QA-FEE-006 | 10.5 | M | **Run the same bill generation twice** | Second run creates nothing (idempotent); report says "already billed" |
| QA-FEE-007 | 10.5 | M | Stop a bill run mid-way (chunks already committed), then run it again | Resumes; no duplicates; no gaps |
| QA-FEE-008 | 10.6 | M | Voucher with arrears hidden + late fee note | Totals correct; arrears hidden but included in the total per setting |
| QA-FEE-010 | 10.7 | M | Pay 5,000 against a 7,000 bill | Status PARTIAL; balance 2,000.00; cash account +5,000; receipt issued |
| QA-FEE-011 | 10.7 | M | **Atomicity:** force failure after receipt insert, before journal | Whole payment rolled back; no receipt number consumed (or the voided number is recorded) |
| QA-FEE-012 | 10.7 | M | **Double click / network retry** with the same Idempotency-Key | Exactly one payment and receipt |
| QA-FEE-013 | 10.7 | M | **Two cashiers** pay the same bill concurrently | One succeeds; the other gets a version conflict or allocates only the remaining balance; never overpaid without an advance record |
| QA-FEE-014 | 10.8 | M | Receipt numbers across 1,000 payments | Unique, sequential, gapless per year |
| QA-FEE-015 | 10.8 | M | SMS provider down when saving a payment | Payment succeeds; SMS queued and retried; failure visible in delivery log |
| QA-FEE-016 | 10.9 | M | Family pays for 3 children in one transaction | One receipt; allocation per child (oldest due first, or manual); each child's ledger correct |
| QA-FEE-017 | 10.10 | M | Pay 3 months in advance | Advance credit recorded; next 3 bills auto-settled from credit; balance zero |
| QA-FEE-018 | 10.10 | M | Unpaid amount carried forward | Arrears appear on the next voucher, counted once |
| QA-FEE-019 | 10.11 | M | Late fee Rs. 200 on the 11th for unpaid October bills, excluding Class 10 | Applied only to eligible unpaid bills; running it again adds nothing |
| QA-FEE-020 | 10.12 | C | Absent fine Rs. 50 per unexplained absence | Count excludes leave/holidays; added to the next bill |
| QA-FEE-021 | 10.13 | M | Reverse a mistaken transport charge | Reversal entry with reason + approver; original still visible; balance corrected |
| QA-FEE-022 | 10.13 | M | Try to edit or delete a payment directly | Not possible (no endpoint; DB grants) |
| QA-FEE-023 | 10.14 | M | Ledger for a father questioning Rs. 3,000 | Shows two unpaid months with dates/heads; running balance correct |
| QA-FEE-024 | 10.16 | M | Defaulters > Rs. 10,000, final reminder letter | Correct list; letter PDF generated; follow-up remark saved |
| QA-FEE-025 | 10.17 | M | Fee-head collection report for October | Matches Σ allocations by head; matches the accounts fee-income total |
| QA-FEE-026 | 10.18 | M | Auditor traces a receipt | Shows student/family, heads, account, method, user, timestamp |
| QA-FEE-029 | 10.13 | M | Reduce a charge that was already paid | Excess becomes advance credit (journal Dr fee income / Cr advances) and settles the next bill |
| QA-FEE-030 | 10.5 | M | Bill run without an Idempotency-Key / payment with a reused key and a different amount | 422 `IDEMPOTENCY_KEY_REQUIRED` / 422 `IDEMPOTENCY_KEY_REUSED` |
| QA-FEE-031 | 10.13 | M | Approve a correction or reverse a payment | Needs the approval/reversal permission and a fresh verification code |
| QA-FEE-032 | — | M | User limited to assigned classes holds fee permissions | Fee endpoints refuse (fees need the "all classes" data scope) |
| QA-FEE-027 | — | M | **Property test:** random sequences of bills, discounts, payments, reversals | Invariant: balance = Σnet + Σadj − Σalloc ≥ −advance; no float drift; sums always reconcile |
| QA-FEE-028 | — | M | Rounding: 33.33% discount on 3,000 | Deterministic HALF_UP to 2 dp; voucher, ledger and report show the same figure |

### QA-ACC / QA-PAY: Accounts and payroll (FR 11, 6.8, 13.12)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-ACC-001 | 11.2 | M | Transfer 200,000 cash → bank | Both balances updated; one journal entry balanced |
| QA-ACC-002 | 11.8 | M | Day with 120,000 fee collection | Appears once in the daily cash summary as fee income; never re-entered manually |
| QA-ACC-003 | 11.5 | M | Expense above the approval threshold | Pending until approved; balance changes only after approval |
| QA-ACC-004 | 11.9 | M | Day close: counted 92,500 vs system 92,500 | Closed and signed; a mismatch requires a note |
| QA-ACC-005 | 11.10 | S | Cheque deposited 30 Sep cleared 2 Oct | Reconciliation shows it uncleared until marked |
| QA-ACC-006 | 11.11 | M | Expense entered twice → reverse | Reversal entry, approver recorded, both visible |
| QA-ACC-007 | — | M | Trial balance at any date | Σdebits = Σcredits |
| QA-ACC-008 | 11.13 | M | October income/expense report | Matches journal totals by category |
| QA-ACC-009 | 11.9 | M | Record a fee payment or expense in cash on a closed day | Refused with `DAY_CLOSED`; another (open) account still works |
| QA-ACC-010 | 11.5 | M | Same Idempotency-Key for an entry, then with a different amount | Same entry returned / 422 `IDEMPOTENCY_KEY_REUSED` |
| QA-ACC-011 | 11.6 | M | Expense entry against the Salaries category | Refused: salaries go through payroll |
| QA-PAY-004 | 13.12 | M | Staff member without a salary record | Left off the sheet and listed as skipped |
| QA-PAY-005 | — | M | Change a line of an approved sheet directly in the database | Refused by the database trigger |
| QA-PAY-001 | 13.12 | M | Rule: 3 late days = 1 absence; staff has 4 late + 1 absent | Deduction = 2 days at basic/working days (formula documented); slip shows breakdown |
| QA-PAY-002 | 6.8 | M | Increment recorded in July | August salary uses the new basic, July the old |
| QA-PAY-003 | — | M | Approve and pay a salary sheet | Journal entry posted once; sheet locked; corrections only via reversal |

### QA-ATT: Attendance (FR 13)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-ATT-001 | 13.1 | M | Teacher opens 4-A register | All present by default; mark 2 absent and 1 late; save → register complete |
| QA-ATT-002 | 13.1 | M | Attempt to mark on a holiday / Sunday | Blocked unless overridden with reason (permission) |
| QA-ATT-003 | 13.2 | M | 9:00 am, 6-B not marked | Appears in missing-register list and dashboard |
| QA-ATT-004 | 13.3 | M | Correct Absent → Present (school trip) | Old/new/user/reason stored |
| QA-ATT-005 | 13.3 | M | Bulk mark the whole school holiday for a date range | All registers updated; excluded from % |
| QA-ATT-006 | 13.5 | M | Fixture month: 22 working days, 20 present, 1 late, 1 leave | Per DECISIONS D-09: (20 + 1) ÷ (22 − 1) = 100.0% |
| QA-ATT-007 | 13.6 | M | 3 consecutive absences | Flagged; badge on dashboard; skips weekends/holidays when counting "in a row" |
| QA-ATT-008 | 13.8 | M | Approve 3-day leave | Those days show Leave, not Absent, including already-marked days (recalculated with audit) |
| QA-ATT-009 | 13.9 | M | Teacher arrives 7:50 vs 7:30 start; support staff 7:20 vs 7:00 | Both marked late using category-specific thresholds |
| QA-ATT-010 | 13.11 | M | Monthly staff summary → approve | Locked; payroll uses the approved summary only |
| QA-ATT-012 | 13.1 | M | Subject teacher (not class teacher) opens another section's register | Can view, cannot save (403) |
| QA-ATT-013 | 13.11 | M | Approve a staff month with unmarked days | Refused unless "count unmarked as present"; reopening refused once salaries are approved |
| QA-ATT-014 | 13.8 | M | Second leave overlapping an existing one | Refused (409) |
| QA-ATT-011 | 13.1 | M | Student moved section mid-month | Monthly register for each section shows the student only for their dates |

### QA-MRK: Simple test marks (D-51)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-MRK-001 | 14.5 | M | Marks above the "out of" total, not a number, or for a student outside the section | Refused |
| QA-MRK-002 | 14.8 | M | Two students tie at 95 and one has 80 | Positions 1, 1, 3 (or 1, 1, 2 with the dense setting); Absent has no position |
| QA-MRK-003 | 14.8 | M | Overall positions over two tests with different totals | Ranked by percentage of the tests with marks; Absent counts as 0 |
| QA-MRK-004 | 14.5 | M | Teacher adds or edits marks outside their subject or section | 403; another section's test is 404 |
| QA-MRK-005 | 14.11 | M | Change a saved mark | Needs a reason; old and new values logged |
| QA-MRK-006 | 14.15 | M | Change marks in a closed year | Refused (YEAR_CLOSED); reading still works |
| QA-MRK-007 | P-1 | M | Another school opens a test or a student's marks | 404 or empty |

### QA-OFF: Office tools (FR 3)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-OFF-001 | 3.5 | M | A task shared with one colleague | The creator and the colleague see it and can complete it; anyone else gets 404 |
| QA-OFF-002 | 3.3 | M | Assign, remark on and close a complaint | History kept; closing needs a note; closed years are read-only |
| QA-OFF-003 | 3.10 | M | Event with the last day before the first; events overlapping a month | Refused; shown on each day they cover |
| QA-OFF-004 | 3.4 | S | Send a birthday wish twice | One message a year; the list shows it was sent |
| QA-OFF-005 | P-1 | M | Another school opens a complaint, task, circular or student | 404 or empty |

### QA-EXM: Exams and results (FR 14)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-EXM-001 | 14.1 | M | Hidden exam | Not visible to teachers until made visible |
| QA-EXM-002 | 14.5 | M | Enter 112 for a 100-mark paper | Rejected inline and by the API |
| QA-EXM-003 | 14.5 | M | Teacher enters marks for an unassigned section/subject | 403 |
| QA-EXM-004 | 14.6 | M | Mark Absent vs leave blank | Absent shows "Abs"; blank listed as missing; publish blocked while missing > 0 |
| QA-EXM-005 | 14.7 | M | Grade boundaries: 94.99, 95.00, 100, 0 | Correct band per scale (boundary inclusivity documented) |
| QA-EXM-006 | 14.7 | M | Grade-only class (Play Group) | No marks/percentages shown; grades only |
| QA-EXM-007 | 14.8 | S | Positions with ties (two at 87.5%) | Tie handled per setting (1,1,3 or 1,1,2) |
| QA-EXM-008 | 14.10 | M | Draft → Reviewed → Published | Nothing is sent to families or printed as final before Published; only users with the publish permission can publish |
| QA-EXM-009 | 14.11 | M | Correct after publish | Requires unlock permission + reason; new version; revised report card; audit entry; old snapshot kept |
| QA-EXM-010 | 14.13 | M | Batch report cards for Class 5 with attendance % on | Each card correct; signatures from document identity |
| QA-EXM-011 | 14.15 | M | Reprint a 2024 report card | Uses the frozen snapshot, not current grading rules |
| QA-EXM-012 | 14.1 | S | Cumulative result (first term + final, weights) | Totals match a hand-calculated fixture |
| QA-EXM-013 | 14.10 | M | Change a mark after the class was reviewed | Class goes back to draft and must be reviewed again |
| QA-EXM-014 | 14.6 | M | Absent in one paper, Exempt in another | Absent counts 0 of the maximum and fails; Exempt is left out of the total |
| QA-EXM-015 | 14.15 | M | Change the grade scale after publishing | Published results and reprints keep the old grades (D-49) |
| QA-EXM-016 | 14.14 | M | Send results twice with the same key; two children on one phone | Each child gets one message; the repeat queues nothing |
| QA-EXM-018 | 14.15 | M | Change marks, papers, remarks or the workflow in a closed year | Refused (YEAR_CLOSED) without the closed-year correction permission; reading and printing still work |
| QA-EXM-017 | P-1 | M | Another school opens an exam class, its results or report cards | 404; each school has its own grade scales |

### QA-DSH: Dashboard (FR 3)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-DSH-001 | 3.1 | M | Totals for selected year | Match list counts; clicking opens the filtered list |
| QA-DSH-002 | 3.13 | M | 7 pending conditions | Only the top 5 shown, in priority order, each linking to its screen |
| QA-DSH-003 | 3.x | M | Accountant vs teacher vs principal | Each sees only role-permitted panels; API does not return hidden panels |
| QA-DSH-004 | 3 | M | Mask-figures setting on | Sensitive totals masked |
| QA-DSH-005 | 3.6 | M | Holiday today | No "incomplete attendance" warning |
| QA-DSH-006 | — | S | Load time with 2,400 students | < 1.5 s |

### QA-COM: Communication
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-COM-001 | 8.6 | M | Receipt SMS template with placeholders | Rendered "Received Rs. 3,500 for October fee." |
| QA-COM-002 | 8.6 | M | Long template (> 160 chars) | GSM-7 segment count and cost estimate shown before send |
| QA-COM-003 | — | M | Provider failure / retry | Exponential backoff, max attempts, final FAILED state visible |
| QA-COM-004 | — | M | Staging environment | Messages only to the allow-list |
| QA-COM-005 | 8.6 | M | Send to a section with two children on one phone and a family with no phone | One message to that phone; the other counted as no phone; the same key queues nothing more |
| QA-COM-006 | 8.6 | M | Template with an unknown placeholder | Refused with the list of allowed placeholders |
| QA-COM-007 | 4.12 | S | Press Welcome twice | One message; the list shows Welcomed |

### QA-RPT: Reports (FR 15)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-RPT-001 | 15.1 | M | Any report | Heading shows filters + academic year |
| QA-RPT-002 | 15.2 | M | Student monthly report | Attendance %, exam results in period, comparison with the previous period |
| QA-RPT-003 | 15.7 | M | Subject teacher's class report | No fee data in screen, PDF or Excel |
| QA-RPT-004 | 15.6 | M | Export to Excel and PDF | Values identical to screen; money cells numeric in Excel |
| QA-RPT-005 | 15.1 | M | Dates outside the selected academic year | Refused, naming the year's dates |
| QA-RPT-006 | 15.3 | S | Tests entered on time, late and incomplete; an on-time mark corrected later | Each status correct; the correction does not make it late |
| QA-RPT-007 | 15.7 | M | Teacher opens a student or section they do not teach, or a teacher report | 404; 403 |
| QA-RPT-008 | 15.6 | M | Export a table with a value starting with = and a code 00123 | The value stays text and never runs as a formula; the code keeps its zeros |
| QA-RPT-009 | P-5 | M | Another school opens a report | 404; cross-school report 403 |

### QA-WEB: Website (FR 16)
| ID | FR | P | Scenario | Expected |
|---|---|---|---|---|
| QA-WEB-001 | 16.10 | M | Draft post | Not publicly reachable; preview only for editors |
| QA-WEB-002 | 16.10 | M | Publish, then unpublish | Live within 60 s (revalidation); after unpublish returns 404/410 |
| QA-WEB-003 | 16.7 | M | Submit admission form | Enquiry created with source ONLINE; dashboard counter +1 |
| QA-WEB-004 | 16.7 | M | Spam: 50 submissions/min from one IP, honeypot filled | Rate-limited / silently dropped |
| QA-WEB-005 | 16.8 | S | Job application with CV | CV stored privately; only authorised staff can download |
| QA-WEB-006 | 16.11 | M | No student data published by default | No public endpoint returns student records |
| QA-WEB-007 | — | S | Lighthouse | Performance, Accessibility, SEO ≥ 90 on home, about, news |


## 6. Non-functional testing
- **Performance (k6):** 50 virtual users mixing list/search/payment; p95 within ARCHITECTURE §5 targets; bill run for 1,000 students
  < 60 s; 1,000-voucher PDF < 3 min; JVM memory stays below the Railway limit.
- **Security:** ASVS L2 checklist (SECURITY.md), ZAP baseline on staging every release, dependency scan with no high/critical
  findings unresolved, secret scan, session fixation test, password hashing verified, rate limits verified.
- **Accessibility:** keyboard-only completion of attendance marking, payment entry and marks entry; visible focus;
  form errors announced; colour is never the only status indicator (enquiry status colours also carry a label); contrast AA.
- **Formats:** money shown as `Rs. 3,500` everywhere (screen, PDF, Excel, SMS); dates `DD-MM-YYYY` per default setting; times in the school's timezone.
- **Browser matrix:** Chrome, Edge, Firefox, Safari (latest two), Android Chrome.
- **Print:** A4 vouchers (school/bank/student copies), A5 receipts (DECISIONS D-08), report cards.
- **Resilience:** backend restart during a bill run; DB failover; R2 unavailable (uploads fail gracefully, core work continues).
- **Backup/restore:** restore the latest R2 dump to a scratch DB; app boots; row counts and a fee-balance checksum match production at dump time.

## 7. Regression strategy
- Every fixed S1/S2 defect gets an automated regression test before closing.
- **Smoke suite** (≤ 5 min, runs after every deploy to staging and production, read-only on production): sign-in, dashboard
  loads, student search, ledger view, report card PDF generation (staging), health endpoint.
- **Full E2E regression** nightly on staging, and before every production release.

## 8. Defect workflow
Defects are logged in [DEFECT-LOG.md](DEFECT-LOG.md) with severity, module and FR ID.
Report template: steps, expected, actual, environment, user/role, academic year, screenshots, request ID.
S1 must be triaged within 4 working hours and fixed or rolled back within 1 working day.

## 9. UAT scripts (Phase 10)
Each script is run by the named role on staging with migrated, anonymised data and signed off in writing.

| Script | Role | Covers |
|---|---|---|
| UAT-00 Onboard a new school | Super admin + new school's admin | P-2, P-3, setup wizard, FR 1 |
| UAT-01 Open new year & promote | Admin/Principal | FR 1.3, 5.5 |
| UAT-02 Enquiry to enrolment to first bill | Front office + Accountant | FR 4, 4.10, 10.5 |
| UAT-03 Monthly fee cycle | Accountant | Bills → vouchers → partial/family/advance payments → late fee → defaulters → reports |
| UAT-04 Day close & bank deposit | Accountant + Principal | FR 11.2, 11.9 |
| UAT-05 Daily attendance | 2 Teachers + Office | FR 13.1–13.3, dashboard alert |
| UAT-06 Term exam | Teachers + Exam coordinator + Principal | FR 14 end-to-end incl. correction after publish |
| UAT-07 Monthly payroll | Accountant + Principal | FR 13.11–13.12, payroll |
| UAT-08 Website update & online enquiry | Website editor + Front office | FR 16 |
| UAT-10 Withdrawal & certificates | Office + Accountant | FR 5.6, 5.8 |

## 10. Release gate checklist (production)
- [ ] All phase exit criteria met; no open S1/S2.
- [ ] Flyway migration tested on a copy of production data; rollback plan written.
- [ ] Smoke suite green on staging with the release candidate.
- [ ] Backup taken immediately before deploy and verified.
- [ ] Release notes + user-facing changes communicated.
- [ ] Post-deploy smoke on production green; error rate monitored for 1 hour.
