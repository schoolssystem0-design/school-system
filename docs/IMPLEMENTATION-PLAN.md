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

## Phase 3: Fees
FR 4.10, 10.1–10.18 (10.19 deferred)

- Fee heads, class-wise structure per year (missing-amount flags), student overrides.
- Discounts (amount/percent) with an approval workflow above a threshold, sibling discount rules, "should have a discount but doesn't" report.
- Bill generation (single/family/class/multi-class/selected, exclusions) as an idempotent Spring Batch job.
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

## Phase 5: Attendance
FR 13.1–13.12

- Student register per section/date, default present, exceptions only, save marks complete. Holidays and OFF days are blocked.
- Missing-register list. Corrections with reason (single, class or school for a date range).
- Monthly register PDF, attendance % (working days only), repeated-absence detection (consecutive + below threshold), class summaries, full-attendance list.
- Leave applications (students & staff) → approved leave shows as Leave.
- Staff attendance with arrival time and category-specific late thresholds (from settings). Corrections. Monthly summary + approval → payroll.
- Optional SMS to parents for absences (template + guardian message permissions).

Exit criteria: `QA-ATT-*` pass. Percentages match a hand-calculated fixture month.

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

## Phase 7: Dashboard, office tools, communication
FR 3.1–3.13, 4.3, 4.12, 8.6

- Office module: tasks (share, urgent, overdue), complaints (status, assign, remarks), circulars, events calendar, visitor count entry, birthday lists + wishes.
- Communication: templates, outbox worker, provider adapter(s), delivery log, bulk send with recipient preview.
- Dashboard API: one aggregated endpoint per panel, role-filtered, cached 60 s. Panels as in FR 3. Urgent-matters
  strip (max 5 lines, priority order). Figure masking toggle. Quick bar favourites.

Exit criteria: `QA-DSH-*`, `QA-COM-*` pass. Dashboard loads in under 1.5 s with production-sized seed data.

## Phase 8: Reports
FR 15.1–15.7

- Common filter bar (week/month/custom, year, class, section, person) printed in every report header.
- Student, teacher and class weekly/monthly reports, including comparison with the previous period.
- **Super admin cross-school reports (P-9):** school-by-school comparison of enrolment, attendance %, fee collection vs. billed,
  outstanding/defaulters, income vs. expense and exam averages, for any period. Drill down into a school, export to Excel/PDF.
- Consolidated report catalogue linking the detailed reports from earlier phases.
- Print / PDF / Excel for every report. Role-based content (fee data removed for unauthorised viewers).

## Phase 9: Public website
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
