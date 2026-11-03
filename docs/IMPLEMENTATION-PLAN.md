# Implementation Plan

> Delivery runs in phases. Each phase ends with a **staging demo + QA sign-off** before the next starts.
> Feature IDs (`FR x.y`) map to [REQUIREMENTS-TRACEABILITY.md](REQUIREMENTS-TRACEABILITY.md).
> Priorities: **M** = must for go-live, **S** = should (go-live if time allows), **C** = could/later.
> Durations assume 2–3 developers plus 1 QA. They are a planning baseline, not a commitment.

## Phase overview

| # | Phase | Main FR sections | Est. |
|---|---|---|---|
| 0 | Foundation, environments, spikes | Stack doc | 2 wks |
| 1 | Platform (multi-school + super admin), school setup, security, settings, academic structure | 1, 2, 12, 9 | 4 wks |
| 2 | People: admissions, students, families, staff | 4, 5, 6, 7, 8 | 4 wks |
| 3 | Fees | 10 | 4 wks |
| 4 | School accounts + payroll | 11, 6.8, 13.12 | 3 wks |
| 5 | Attendance | 13 | 2 wks |
| 6 | Examinations and results | 14 | 3 wks |
| 7 | Dashboard, office tools, communication | 3, 4.3, 8.6 | 2 wks |
| 8 | Reports | 15 | 2 wks |
| 9 | Public website and online forms | 16 | 2 wks |
| 10 | Data migration, hardening, UAT, go-live | 19 | 3 wks |

Total ≈ 31 weeks. Parent and student portals are out of scope (DECISIONS D-19). Phases 5 and 9 can run in parallel with 3–4 if a third developer is available.

---

## Phase 0: Foundation and spikes
**Goal:** an empty but production-shaped system that deploys from the local repository to staging and production (no GitHub, DECISIONS D-22).

Tasks
1. Monorepo `backend/`, `frontend/`, `docs/`, `ops/`, `scripts/` in a local git repository (no remote).
2. Backend skeleton: Spring Boot (Java 21, Maven), Web, Security, Data JPA, Validation, Flyway, Actuator, springdoc,
   Spring Session JDBC, Spring Batch, Spring Modulith. Pin versions in `pom.xml`.
3. `shared/`: `Money` value object, Problem Details error handler, **`TenantContext` + Hibernate `@TenantId` base entity +
   per-transaction `SET LOCAL app.school_id`**, `YearContext`, audit aspect, request-ID filter, JSON logging.
4. Flyway `V001__baseline.sql` (school, school_domain, platform_user, school_profile, academic_year, app_user, role, permission,
   audit_log, login_event, setting_value) **with RLS policies and a non-owner app DB role**.
5. Frontend skeleton: Next.js (App Router, TS strict), Tailwind, shadcn/ui, TanStack Query, API client generated from OpenAPI,
   layout shell (sidebar, header with year switcher, user menu), host-resolution middleware. English only.
6. Local quality gate `scripts/verify.sh`: backend `mvn verify` (unit + integration on embedded PostgreSQL), frontend
   lint/typecheck/test/build, OpenAPI drift check, secrets check, npm audit.
7. Environments: Railway projects **staging** and **production** (backend + Postgres, Singapore region if available);
   Vercel projects with custom domains; R2 buckets (`sms-files-staging`, `sms-files-prod`, `sms-backups`).
8. **Spike A, tenant isolation:** two seeded schools; prove that Hibernate filtering + RLS block cross-school reads and writes, including
   native SQL and batch jobs. The QA-TEN baseline tests must pass.
9. **Spike B, cookie/session/CORS:** sign-in from `staging-app` to `staging-api` with an HTTP-only cookie and CSRF working in all target browsers.
10. Nightly `pg_dump` → R2 job and a documented restore (see DEPLOYMENT-RUNBOOK).

Exit criteria
- `scripts/verify.sh` passes; `railway up` / `vercel deploy` put the build on staging and production. Flyway runs on startup.
- The health check is green and the uptime monitor is configured.
- Spike results are recorded in DECISIONS. A restore from an R2 dump has been tested once.

### Phase 0 status (2026-10-27)
| # | Task | Status |
|---|---|---|
| 1 | Monorepo layout, git | ✅ Done (local repository only; no GitHub by owner decision D-22) |
| 2 | Backend skeleton: Spring Boot **4.1.2**, Java 21, Maven, Security, Session JDBC, JPA (Hibernate 7.4), Flyway, Actuator, springdoc 3.2, Modulith 2.1 | ✅ Done. Spring Batch moves to Phase 3 (first batch job) |
| 3 | `shared/`: Money, Problem Details errors, TenantContext + `@TenantId` + RLS connection hook, request-ID filter, JSON logs (staging/prod) | ✅ Done. `YearContext` and the audit aspect move to Phase 1 (they need `academic_year` and real actions) |
| 4 | Flyway `V001__baseline.sql` with RLS policies and restricted runtime role | ✅ Done (school, school_domain, platform_user, platform_audit_log, app_user, login_event, audit_log, Spring Session). Remaining Phase 1 tables come in V002+ |
| 5 | Frontend skeleton: Next.js **16.4.3**, React 19.3, Tailwind 4, shadcn/ui, TanStack Query, generated API types, sign-in page, portal shell, `proxy.ts` host routing | ✅ Done |
| 6 | Quality gate `scripts/verify.sh` (replaces hosted CI): secrets check, backend verify, API type drift, frontend lint/typecheck/test/build, npm audit | ✅ Done |
| 7 | Railway (staging/prod), Vercel, R2 buckets, domains | ⏳ Blocked: needs your accounts and the platform domain (DECISIONS "Inputs still needed") |
| 8 | Spike A: tenant isolation | ✅ Passed. 13 tests: Hibernate filter, RLS on raw SQL, no-tenant = no rows, cross-school write blocked, composite FK, all-schools mode, thread reuse, insert-only audit, RLS present on every tenant table |
| 9 | Spike B: cookie / CSRF / CORS | ✅ Passed locally (11 API tests + 4 Playwright browser tests, localhost:3000 ↔ :8080). ⏳ Re-run on the real staging domains |
| 10 | Nightly `pg_dump` → R2 + restore drill | ✅ Job written (`ops/backup`). ⏳ Deploy + first restore drill once Railway/R2 exist |

## Phase 1: Platform, school setup, security, settings, academic structure
FR 1.1–1.8, 2.1–2.8, 12.1–12.3, 9.1–9.7, plus platform features P-1…P-9 (REQUIREMENTS-TRACEABILITY §0)

Backend
- `platform` (super admin = system owner, full access): sign-in with mandatory TOTP; create school (code, name, status) → seeds roles,
  statuses, grade scale, inactive fee heads and settings defaults → creates the school's **main administrator** with a one-time setup link;
  edit/suspend/reactivate school; school domains; manage/reset any school user; **switch into any school with full permissions**
  (audited in both logs); **all-schools overview** endpoint (figures grow as later phases add fees, attendance etc.; cross-school
  reports finish in Phase 8); usage (students, users, storage, SMS per school); platform audit log; view any school's login/audit history.
  The first super admin is bootstrapped from env vars.
- `school`: profile, document identity (uploads), academic years (create, set current, close), working week, holidays.
- `iam`: sign-in/out, session timeout, change password, admin-assisted reset with forced change, OTP reset via SMS/email
  (provider stub), activate/deactivate (revokes sessions), roles and permissions CRUD, data scopes, lock after N failed
  attempts, login history search, new-location flag (IP-to-country/city lookup), audit log search.
- Step-up OTP for sensitive modules (FR 2.6).
- `settings`: typed registry for all groups in FR 12 (keys defined up front, even where consumers arrive later);
  edit with confirmation and reason; history.
- `academics`: classes (reorder, grading mode), sections, subjects, class subjects (max/passing marks per year),
  "where used" check + deactivate instead of delete.
- Seed per school (on creation): roles, default enquiry statuses, attendance statuses, grade scale, fee heads (inactive), settings defaults.

Frontend
- `/platform` console: all-schools overview dashboard, schools list, create/edit/suspend school, domains, school users,
  "Open school" switcher (school portal with a "Super admin viewing <School>" banner), usage, audit logs.
- Sign-in (school code + username, remembered; `/s/<code>` shortcut), forgot/reset password, change password, profile.
- Setup wizard (first run): school profile → year → working days → classes/sections → subjects.
- Admin screens: users, roles/permission matrix, login history, activity history, settings by group, academic structure.
- Role-filtered navigation and permission-aware buttons.

### Phase 1 status (2026-10-28)
| Area | Status |
|---|---|
| Platform: super admin TOTP sign-in + first-time enrolment (QR), create school (profile + 9 built-in roles + main admin + setup link, one transaction), rename, suspend/reactivate (sessions ended), domains, school users + new setup link, open any school with full access (audited in school + platform logs), all-schools overview, platform audit | ✅ Done |
| School: profile + document identity (logo, letterhead, stamp, signatures) with uploads checked by real file type; academic years (one current, opening a year closes the previous; closed years read-only except `school.year.correct_closed`); working week; holidays/vacations; working-day API for attendance | ✅ Done |
| IAM: sign-in/out, permissions re-read on every request, users (temporary password shown once, forced change), roles + permission matrix (Administrator locked), deactivate/activate with reason (sessions ended), unlock, admin reset, email reset link, setup links, step-up email code for user/role/rule changes, login history with new-network flag, activity history with old/new values, rate limits | ✅ Done |
| Settings: 31 typed rules in 12 groups, reason required, history, one service read by all modules, per-school cache | ✅ Done |
| Academics: classes (order, grading mode, deactivate, delete only if unused), sections, subjects, per-year subjects with max/passing marks | ✅ Done |
| Frontend: platform console, school portal shell (permission menu, year switcher, super-admin banner), all Phase 1 screens, setup checklist, forgot/reset/setup-account pages | ✅ Done |
| Tests: 70 backend (17 unit/architecture + 53 integration on PostgreSQL), 16 frontend unit, 7 browser end-to-end | ✅ Passing |
| Moved: year rollover wizard (FR 1.3) and class-teacher assignment (FR 9.5) to Phase 2 (need students/staff); optional sign-in TOTP for school users (D-10) to Phase 10 hardening; per-user permission overrides not built (custom roles cover FR 1.7) | ℹ️ |

Exit criteria: QA suites `QA-PLT-*`, `QA-TEN-*`, `QA-SEC-*`, `QA-SET-*`, `QA-ACA-*` pass. A teacher account cannot reach any admin API
(verified by an API test, not just the UI). A School A user cannot read or write any School B record.

## Phase 2: People
FR 4.1–4.12, 5.1–5.11, 6.1–6.7, 7.1–7.4, 8.1–8.6

- Admissions: enquiry CRUD, custom statuses with colours, remarks timeline, entry test result, duplicate detection
  (name/DOB/phone/CNIC fuzzy match), approve/decline, **Enrol** (prefilled admission form), terms/syllabus content,
  printable admission form PDF.
- Students: profile with tabs (fees, attendance and results tabs filled by later phases), fast search (GR, name, parent name, phone),
  class strength (total/boys/girls), edit + bulk field update, move section/class with effective date,
  withdrawal (shows outstanding balance once fees exist), leavers list, documents/photo upload,
  ID cards + certificates (leaving, DOB, character, appreciation, provisional, hope), bio-data/registers PDF, houses/clubs/achievements.
- Families: family record, siblings view, guardians with message/collection permissions, safe update with confirmation,
  bulk update, data-quality reports (invalid phones, missing CNIC, empty families).
- Staff: teacher and other-staff records, documents, login creation, teaching assignments, timetable (optional),
  bulk reassignment with effective date, leaving + experience certificate, salary profile (fields only).
- Student promotion wizard (FR 5.5) + academic-year rollover wizard (FR 1.3).
- Privacy: field-level masking (fees, documents, contact numbers) by permission (FR 5.11).

Exit criteria: suites `QA-ADM-*`, `QA-STU-*`, `QA-FAM-*`, `QA-STF-*` pass. Enrolling an approved enquiry creates the student,
enrolment and family link in one transaction.

### Phase 2 status (2026-10-28)
| Area | Status |
|---|---|
| Admissions: enquiries (ENQ- numbers), coloured custom statuses, remarks timeline, admission test, duplicate warnings (name/DOB/phone/CNIC, fuzzy), approval before enrolment, enrol in one transaction (student + enrolment + family + status), terms/syllabus, printable admission form PDF | ✅ Done |
| Students: list with search/filters, class strength, admission/leaving register, admit (GR suggestion), profile tabs (details, class history, family & contacts, documents, houses/clubs/achievements, certificates), move section with effective date, withdraw/re-admit, promotion wizard, bulk field update, ID cards, bio-data, six certificate types with per-school serial numbers, private-field masking | ✅ Done |
| Families: family record shared by siblings, extra contacts with collection/message flags, change-family with confirmation, bulk update, data-issue report | ✅ Done |
| Staff: teachers and other staff (EMP- numbers), documents, sign-in creation (verification code), salary history (append-only, verification code), leaving (sign-in closed, classes ended) / rejoin, experience certificate | ✅ Done |
| Teaching: class and subject teachers with effective dates, hand-over, section timetable with double-booking check, teacher's timetable | ✅ Done |
| Year rollover: copy working week, subjects and teachers from an earlier year (earlier year untouched) | ✅ Done |
| Fixes found in Phase 2: unique API schema names (guarded by a test); concurrent sign-ins of one user no longer conflict (QA-SEC-021); clear message when sign-in is rate limited | ✅ Done |
| Tests: 93 backend (18 unit/architecture + 75 integration on PostgreSQL), 18 frontend unit, 11 browser end-to-end | ✅ Passing |
| Not yet: withdrawal balance and fee/attendance/result tabs arrive with Phases 3, 5 and 6 (FR 5.6 balance, QA-STU-005) | ℹ️ |

## Phase 3: Fees
FR 4.10, 10.1–10.18 (10.19 deferred)

- Fee heads, class-wise structure per year (missing-amount flags), student overrides.
- Discounts (amount/percent) with an approval workflow above a threshold, sibling discount rules, "should have a discount but doesn't" report.
- Bill generation (single/family/class/multi-class/selected, exclusions) as an idempotent, chunked, restartable run (D-31).
  Admission charges are proposed automatically at enrolment.
- Vouchers: settings-driven layout, arrears show/hide, late-fee note, bank details, batch PDF.
- Payments: student or family, partial, multiple bills, oldest-first allocation with manual override, methods,
  account selection, advance credit and carry-forward, gapless receipt numbers, receipt PDF + SMS (queued).
- Late fee run and absent fine run (the absent fine reads attendance once Phase 5 lands, so build the interface now).
- Adjustments/reversals with reason + approval. Negative-balance report.
- Ledgers (student, family), fee status views, defaulters (filters, reminders, final reminder letter, follow-up remarks).
- Fee reports: head-wise charged/paid/recoverable, family month-wise receivables, recent payments, bad debts.
- Every payment posts a journal entry. In this phase, write it to a minimal `journal_entry` table that Phase 4 completes.

Exit criteria: all `QA-FEE-*` cases pass, including concurrency, idempotency and rollback tests. Reconciliation test:
Σ payments = Σ allocations + Σ advance credit = Σ fee-income journal lines for any date range.

### Phase 3 status (2026-10-28)
| Area | Status |
|---|---|
| Set-up: fee heads (any frequency, maximum, refundable, switch off), class amounts per year with missing-amount flags, student's own amounts, sibling rules | ✅ Done |
| Discounts: amount or percentage per head or all heads, dates, approval above the school's limit, end; sibling discount automatic; "missing sibling discount" report | ✅ Done |
| Bill runs: whole school / classes / section / student, exclusions, monthly / yearly / one-time / term heads, Idempotency-Key replay, never the same head and period twice (database rule), chunked and restartable, missing-amount report, advance credit applied automatically; admission charges proposed on the student's Fees tab | ✅ Done |
| Vouchers: three copies per page (bank, school, parent), arrears shown or hidden per setting, late-fee note, bank details and note settings, batch PDF per run or section | ✅ Done |
| Payments: family or one child, partial, several bills, oldest-first or chosen lines, cash/bank/cheque/online, account, advance credit, gapless receipt number per year, receipt PDF (A4/A5) and SMS through a retrying outbox, one transaction with allocations, journal and audit | ✅ Done |
| Corrections: correction / waiver / bad debt with reason and approval; a reduced paid charge becomes advance credit; payment reversal with reason and verification code; money tables insert-only for the app | ✅ Done |
| Late fee run (once per bill, class exclusions); ledgers (student, family); fee status; defaulters with SMS reminders, final reminder letter and follow-ups; reports by head, money received, family month-wise receivables, advances, bad debts; message log | ✅ Done |
| Journal: every payment, credit use and reversal posts a balanced entry (database refuses unbalanced ones) to cash/bank, fee income and fees received in advance | ✅ Done |
| Tests: 111 backend (18 unit/architecture + 93 integration incl. 18 fee cases: idempotency, two cashiers, atomic failure, 40-step random reconciliation), 23 frontend unit, 14 browser end-to-end | ✅ Passing |
| Not in this phase: absent fine (FR 10.12, needs attendance, moved to Phase 5); online payment (FR 10.19, deferred) | ℹ️ |

## Phase 4: School accounts and payroll
FR 11.1–11.13, 6.8, 13.12, 12 (salary rules)

- Cash/bank accounts, transfers, account ledger (detailed / daily / monthly), income and expense entries with categories,
  attachments and approval for large amounts, vendors (purchases, payments, balance), daily cash summary + day close,
  bank reconciliation (mark cleared), reversals with approval, financial activity history,
  reports: daily collection, monthly income/expense, account balances, income-expense summary. Balance sheet = S.
- Step-up OTP enforced on all accounts and salary endpoints.
- Payroll (scope per DECISIONS D-04): monthly salary sheet from salary profile + approved staff attendance summary +
  deduction rules (absent, half-day, late-as-absent, extra leave, tax); approve → pay → journal entry; salary slip PDF.

Exit criteria: `QA-ACC-*`, `QA-PAY-*` pass. The trial balance always nets to zero.

### Phase 4 status (2026-10-31)
| Area | Status |
|---|---|
| Cash and bank accounts with opening balances, close (only at zero); income and expense categories (each with its own ledger account); suppliers with what is owed | ✅ Done |
| Entries: income, expense, transfer, supplier bill, supplier payment; attachment; Idempotency-Key; expenses and supplier payments above the limit wait for approval; reject; reversal with reason (both kept); insert-only | ✅ Done |
| Books: account ledger (each entry / per day / per month), daily cash summary by source (fee receipts appear once, from the journal), day close with counted amount (difference needs a note; closed days lock the account, fee payments included), bank reconciliation | ✅ Done |
| Reports: income and expense per month, by category, daily collection, trial balance, balance sheet; financial activity history | ✅ Done |
| Payroll: sheet per month from the salary record in effect on the 1st, deductions by the salary rules (absent, half day, late days as absences, leave beyond the paid allowance), lectures, allowance, tax and other deductions; approve (accrual journal) → pay (Idempotency-Key, payment journal) → locked; reversal; salary slips | ✅ Done |
| Every change in accounts and payroll asks for a verification code; reads and writes need the "all classes" data scope | ✅ Done |
| Fee journal posting moved to the shared journal writer (balance and day-close checks for every module) | ✅ Done |
| Tests: 122 backend (18 unit/architecture + 104 integration incl. 7 accounts and 4 payroll cases), 23 frontend unit, 16 browser end-to-end | ✅ Passing |
| Waiting for Phase 5: staff attendance fills the salary sheet automatically (until then the days are entered on the draft sheet; D-36) | ℹ️ |

## Phase 5: Attendance
FR 13.1–13.12

- Student register per section/date, default present, exceptions only, save marks complete. Holidays and OFF days are blocked.
- Missing-register list. Corrections with reason (single, class or school for a date range).
- Monthly register PDF, attendance % (working days only), repeated-absence detection (consecutive + below threshold), class summaries, full-attendance list.
- Leave applications (students & staff) → approved leave shows as Leave.
- Staff attendance with arrival time and category-specific late thresholds (from settings). Corrections. Monthly summary + approval → payroll.
- Optional SMS to parents for absences (template + guardian message permissions).

Exit criteria: `QA-ATT-*` pass. Percentages match a hand-calculated fixture month.

### Phase 5 status (2026-10-31)
| Area | Status |
|---|---|
| Class register per section and date: everyone present by default, exceptions marked, saving completes it; closed days blocked unless corrected with a reason; teachers take only their class-teacher sections; arrival after the student late time becomes late | ✅ Done |
| Missing-register list; corrections of earlier days need the correction permission and a reason (old/new kept); bulk change for a class or the school over a date range; change history | ✅ Done |
| Attendance % per D-09 on marked working days; low attendance, full attendance, absent days in a row (days off skipped), class summaries, student month-by-month, monthly register PDF (moved students shown only for their dates) | ✅ Done |
| Leave for students and staff entered by the office; approval turns those days into Leave, including days already marked (with a correction record) | ✅ Done |
| Staff attendance with arrival times and category late times; corrections; monthly summary → approval (locks the month, verification code) → payroll fills the salary sheet from it; reopen only before salaries are approved | ✅ Done |
| Optional absence SMS (setting, off by default) through the outbox; absent fine run in Fees (FR 10.12) | ✅ Done |
| Tests: 132 backend (18 unit/architecture + 114 integration incl. 10 attendance cases), 23 frontend unit; browser tests for Phase 5 written, run pending (see below) | ✅ Backend and unit passing |

## Phase 6: Examinations and results
FR 14.1–14.15

- Exams per year with classes, cumulative flag/weights, visibility. Exam subjects copied from class subjects.
- Datesheet (PDF + shareable PNG). Admit cards / roll slips (optional defaulter check).
- Marks entry grid per section/subject (teacher scope enforced), Absent/Exempt flags, missing-marks report, max-marks validation.
- Grading engine: totals, %, grade band, remarks; grade-only scheme for early years; positions per settings (method + ties).
- Draft → Reviewed → Published per class. Publishing freezes a result snapshot. Correction after publish requires unlock + reason and creates a new version.
- Result sheets, failed list, multi-exam summary, graphs. Report cards (layouts, attendance % optional, signatures), batch PDF.
- Result SMS to parents (FR 14.14). Printed report cards.

Exit criteria: `QA-EXM-*` pass. Grade-boundary and tie-ranking fixtures pass. A published result cannot change without an audit record.

### Phase 6 status (2026-10-31)
**Changed by the owner (D-51):** staff record simple test marks instead of creating exams.

| Area | Status |
|---|---|
| Test marks: section, subject, name, date and "out of"; marks and Absent per student, stored on the student's record (profile tab); positions per test and overall over a period; teachers limited to their subjects and sections; changing a saved mark needs a reason and is logged; closed years read-only | ✅ Done |
| Full exam system (exams, papers, datesheet, admit cards, review and publish with versions, report cards, result SMS, cumulative results): built and tested at API level, not in the menu | ⏸️ Hidden (D-51) |
| Graphs of results | ⏳ Not built |

## Phase 7: Dashboard, office tools, communication
FR 3.1–3.13, 4.3, 4.12, 8.6

- Office module: tasks (share, urgent, overdue), complaints (status, assign, remarks), circulars, events calendar, visitor count entry, birthday lists + wishes.
- Communication: templates, outbox worker, provider adapter(s), delivery log, bulk send with recipient preview.
- Dashboard API: one aggregated endpoint per panel, role-filtered, cached 60 s. Panels as in FR 3. Urgent-matters
  strip (max 5 lines, priority order). Figure masking toggle. Quick bar favourites.

Exit criteria: `QA-DSH-*`, `QA-COM-*` pass. Dashboard loads in under 1.5 s with production-sized seed data.

### Phase 7 status (2026-10-31)
| Area | Status |
|---|---|
| Office tools: shared tasks, parent complaints with history and assignment, circulars, month calendar, visitor log, birthdays with wishes | ✅ Done |
| Messages: send to all families, a class, a section, open enquiries or staff with a recipient, part and cost check first; automatic message templates; delivery log with send again; welcome message | ✅ Done |
| Dashboard: urgent lines (at most five), totals, fee figures, attendance with a 7-day chart, complaints, tasks, recent admissions and withdrawals, birthdays, events, circulars, logins with new-location alerts, visitors; panels by role; masking; quick bar per user | ✅ Done |
| SMS gateway adapter and staging allow-list: built and tested against a stand-in | ✅ Done |
| Real SMS sending: needs a gateway account and its URL (`app.sms.url-template`) | ⏳ Waiting for the account |
| Not built: FR 3.2 website form counters (Phase 9), tutorial videos on the quick bar, printed birthday and welcome cards, WhatsApp (after go-live, D-06) | ℹ️ |

## Phase 8: Reports
FR 15.1–15.7

- Common filter bar (week/month/custom, year, class, section, person) printed in every report header.
- Student, teacher and class weekly/monthly reports, including comparison with the previous period.
- **Super admin cross-school reports (P-9):** school-by-school comparison of enrolment, attendance %, fee collection vs. billed,
  outstanding/defaulters, income vs. expense and exam averages, for any period. Drill down into a school, export to Excel/PDF.
- Consolidated report catalogue linking the detailed reports from earlier phases.
- Print / PDF / Excel for every report. Role-based content (fee data removed for unauthorised viewers).

### Phase 8 status (2026-10-31)
| Area | Status |
|---|---|
| Student, teacher and class reports for a week, month or custom dates, with the filters in the heading and a comparison with the previous period | ✅ Done |
| PDF, Excel (numeric cells) and print for these reports, from the same data as the screen | ✅ Done |
| Fee figures only for authorised users; teachers limited to their sections; teacher reports for the principal | ✅ Done |
| All-schools report for the super admin: comparison, totals, drill-down, Excel with a sheet per school, PDF, audited | ✅ Done |
| List of detailed reports; Excel, PDF and print on attendance, fee, financial, payroll and test marks pages | ✅ Done |
| Not built: charts in reports, conduct or remarks in the student report (none are recorded) | ℹ️ |

## Phase 9: Public website
**Dropped by the owner (2026-10-31, D-63). Not built.** The plan below is kept for reference only.

FR 16.1–16.11

- CMS: identity/contact/social, home slider (≤ 4) and key figures, about/message/mission/team, news/events posts, galleries,
  important dates, admission info, downloads. Draft → preview → publish → unpublish, with owner and last-edited-by.
- Public Next.js pages per school, resolved by host (SSR/ISR, SEO metadata, per-school sitemap, structured data).
- Super admin maps each school's domain. DNS instructions are shown to the school, and Vercel verifies the domain.
- Forms: admission enquiry (creates `enquiry` with source ONLINE), job application (CV upload), feedback.
  Rate limiting, CAPTCHA (Cloudflare Turnstile), honeypot. Dashboard counters for unread submissions.

## Phase 10: Migration, hardening, UAT, go-live
- **Data migration** per school from its current system (history back to 2016-2017, see DECISIONS D-03), via a reusable importer: export → staging tables →
  validated transforms → reconciliation reports (student counts, family counts, outstanding balances per family must match).
- Performance test with 3× production volume. Security test (OWASP ASVS L2 checklist + external scan). Accessibility audit.
- Backup/restore drill on production-like data. Runbook rehearsal.
- UAT with principal, accountant, office and two teachers using the UAT scripts in QA-TEST-PLAN §9.
- Training sessions + short user guides per role.
- School onboarding playbook: super admin creates the school → main admin completes the setup wizard → data import → go-live.
  Repeated for each new school.
- Go-live: freeze old system → final migration → reconcile → switch → 2-week hypercare.

---

## Definition of Ready (per feature)
- FR reference and acceptance criteria written (derived from the FR card: "what it produces", plus the school example as a test).
- Permissions identified (who can view/add/edit/approve).
- Year scoping and audit needs identified.

## Definition of Done (per feature)
See [CODING-STANDARDS.md](CODING-STANDARDS.md#definition-of-done).
