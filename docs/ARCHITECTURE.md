# Architecture

> Status: v0.1, derived from the Functional Requirements (FR) v1.0 and the locked Stack & Deployment decision (Oct 2026).
> FR section references are written as `FR 10.7`.

## 1. System context

```
 Principal / staff / teachers / parents / public
              │ HTTPS
              ▼
 ┌─────────────────────────────┐        HTTPS (CORS: app domain only)
 │ Vercel: Next.js (TypeScript)│ ───────────────────────────────┐
 │  • School portal /app/...   │                                │
 │  • Super admin  /platform/..│                                ▼
 │  • Public website /         │               ┌────────────────────────────────┐
 └─────────────────────────────┘               │ Railway: Spring Boot REST API  │
                                               │  Java 21, modular monolith     │
                                               └──────┬──────────────┬──────────┘
                                         private net  │              │ S3 API
                                                      ▼              ▼
                                       ┌──────────────────┐   ┌────────────────────┐
                                       │ Railway Postgres │   │ Cloudflare R2      │
                                       └──────────────────┘   │ files, PDFs, dumps │
                                                              └────────────────────┘
                     External (via adapters): SMS / WhatsApp gateway, email (password reset)
```

- **Multi-school platform:** one deployment serves many schools (tenants). See §4.0.
- Platform domains: `app.<platform>.com` (school portal for all schools + super admin console, on Vercel) and `api.<platform>.com` (Railway).
  Staging uses `staging-app.<platform>.com` / `staging-api.<platform>.com`, so cookies stay **same-site** in both environments
  (default `*.vercel.app` / `*.up.railway.app` domains are cross-site and break `SameSite` session cookies).
- Each school's **public website** (FR 16) runs on that school's own domain (e.g. `www.nns.edu.pk`), added as a custom domain to
  the same Vercel project. Next.js middleware maps the request host to the school.
- The database is reachable only over Railway's private network.
- The system is **English only**. There is no Urdu/RTL support, now or later (see DECISIONS D-18).

## 2. Architectural style

**Modular monolith.** One deployable Spring Boot service, split into strict feature modules. Boundaries are
checked with Spring Modulith (or ArchUnit) tests. There are no microservices: the scale is one to a few
schools with around 1,000 students each.

**Why:** fee payments, ledgers, account postings and audit entries must commit in **one database transaction**
(Stack doc §2). Splitting into services would make that harder.

## 3. Repository layout

```
backend/
  pom.xml
  src/main/java/com/selectcode/sms/
    SmsApplication.java
    shared/            # cross-cutting: money, audit, security, errors, files, pdf, messaging, year context, tenant context
    platform/          # super admin (system owner): schools, domains, school admins, suspension, all-schools overview, school switching
    school/            # FR 1  – school profile, document identity, academic years, calendar
    iam/               # FR 2  – users, roles, permissions, sessions, login history, password reset
    settings/          # FR 12 – typed settings registry + change history
    academics/         # FR 9  – classes, sections, subjects, teaching assignments, timetable
    admissions/        # FR 4  – enquiries, statuses, remarks, entry tests, enrolment
    students/          # FR 5  – profile, enrolments, moves, promotion, withdrawal, certificates, activities
    staff/             # FR 6,7 – employees (teacher / other), salary profile, leaving
    families/          # FR 8  – families, guardians, contact permissions
    fees/              # FR 10 – fee heads, structures, discounts, bills, vouchers, payments, receipts
    accounts/          # FR 11 – cash/bank accounts, journal, income, expense, vendors, reconciliation
    payroll/           # FR 6.8, 12, 13.12 – monthly salary sheets (scope: DECISIONS D-04)
    attendance/        # FR 13 – student & staff registers, leave, summaries
    exams/             # FR 14 – exams, datesheets, marks, grading, results, report cards
    office/            # FR 3  – tasks, complaints, circulars, events, visitor count, birthdays
    communication/     # FR 4.3, 8.6, 14.14 – templates, outbox, delivery log, provider adapters
    reports/           # FR 15 – weekly/monthly reports, exports
    website/           # FR 16 – CMS content, galleries, public forms inbox
    dashboard/         # FR 3  – read-only aggregation over other modules
  src/main/resources/
    db/migration/      # Flyway: V001__baseline.sql ...
    reports/           # PDF templates (OpenPDF builders), fonts
    application.yml
  src/test/java/...    # unit, architecture (ArchUnit + Modulith), *IT integration tests on embedded PostgreSQL;
                       # LocalDevApplication runs the API locally without an installed database

frontend/
  package.json
  src/proxy.ts         # Next 16 "proxy" (formerly middleware): host → school website rewrite, /s/<code>, auth redirect
  src/app/
    site/[host]/...    # FR 16 public website per school; proxy.ts rewrites non-platform hosts here
    (auth)/            # sign-in (school code + username), reset password
    app/               # school portal (role-filtered menu; super admin sees everything)
    platform/          # super admin console (all-schools overview, schools, school admins, audit)
  src/components/ui/   # shadcn/ui primitives
  src/components/      # shared composites (DataTable, MoneyInput, ClassSectionPicker, YearSwitcher …)
  src/features/<module>/  # screens, hooks, forms per module
  src/lib/api/         # generated OpenAPI types + fetch client
  tests/e2e/           # Playwright
```

Each backend module follows the same internal layout:

```
fees/
  api/        # @RestController + request/response DTOs (records)
  app/        # application services: use cases, @Transactional boundaries, permission checks
  domain/     # entities, value objects, domain rules (pure, unit-testable)
  infra/      # repositories, batch jobs, adapters
  FeesModule.java  # public interface other modules may call (events / facade)
```

Modules talk to each other only through their public facade or through **domain events** (Spring application events,
published inside the transaction and handled `@TransactionalEventListener` where the side effect may be async, e.g. SMS).

## 4. Cross-cutting design decisions

### 4.0 Multi-tenancy: many schools, one platform (DECISIONS D-01)
**Model:** shared database, shared schema, with a `school_id` discriminator on every school-owned table.
This was chosen over one database or schema per school because it is cheapest on Railway, needs one Flyway run, and suits "a few to many" schools.

**Isolation (defence in depth). Any one layer failing must not leak data:**
1. **Tenant context.** After sign-in, the session holds `schoolId`. A request filter puts it into a `TenantContext`
   (request-scoped, cleared in `finally`). School users can never choose the school: there is no tenant header from the browser.
   Only the super admin can change the session's active school (via `POST /platform/active-school`, see Users below).
2. **Hibernate `@TenantId`** on a `TenantScopedEntity` base class. Every JPA query is automatically filtered and every insert stamped.
3. **PostgreSQL Row-Level Security** on every tenant table:
   `USING (school_id = current_setting('app.school_id', true)::bigint OR current_setting('app.all_schools', true) = 'on')`.
   The app connects as a non-owner role (RLS cannot be bypassed). `app.school_id` is set per transaction via `SET LOCAL`
   by a connection-preparation hook. Native SQL and reports are therefore protected too.
   `app.all_schools = on` is set **only** inside the super admin's cross-school endpoints (`/platform/overview/**`), and only after the caller is verified as a super admin.
4. **Composite constraints.** Unique keys include `school_id` (GR number, receipt number, family number, username). Foreign keys
   between tenant tables include `school_id`, so a row can never reference another school's row.
5. **Files:** R2 object keys are prefixed `schools/{schoolId}/...`. Presigned URLs are issued only after a tenant + permission check.
6. **Tests:** an automated cross-tenant suite (QA-TEN) runs every endpoint as School A against School B's IDs.

**Platform level (no tenant):** tables `school`, `school_domain`, `platform_user`, `platform_audit_log`, (no batch-framework tables: bill runs keep their own `fee_run` record, DECISIONS D-31).
Background jobs run **per school**: the scheduler iterates active schools and sets the tenant context for each run.

**Users:**
- **Super admin = the system owner** (the product owner, not a school user). It is a `platform_user`, not tied to any school.
  The super admin signs in at `/platform` with mandatory TOTP 2FA and has **full access to every school's details and data**:
  - **All-schools overview:** one console listing every school with key figures (students, staff, today's attendance completion,
    month's fee collection, outstanding balance, defaulters, open complaints, last activity, storage/SMS usage). Super admins can
    drill down and run cross-school reports and Excel exports.
  - **Open any school:** switching into a school sets the session's active school. The super admin then sees that school's full admin portal
    with every module and permission, including accounts and salaries, and can view, correct or act there like the school's main administrator.
    A banner shows "Super admin viewing <School>", and switching between schools takes one click.
  - **Manage schools:** create, edit, suspend and reactivate schools; map website domains; create and reset each school's **main administrator**
    and any school user; view login history and audit logs of all schools; export a school's data.
  - Every super admin action inside a school is recorded in that school's `audit_log`, with the actor shown as the super admin, and in the `platform_audit_log`.
- **School users** (admin, principal, accountant, teacher, office staff…) belong to exactly one school and can never see another school.
  Usernames are unique within a school. Sign-in asks for **School code + username + password**. The browser remembers the school code,
  and `app.<platform>.com/s/<code>` pre-fills it.
- Each school's main administrator manages that school's users and roles (FR 1.6, 1.7).
- There are **no parent or student logins** (DECISIONS D-19). Parents are reached by SMS and printed documents only.

**Per-school configuration:** profile, document identity, academic years, settings (FR 12), fee heads, roles, website content, message
sender ID: all tenant-scoped. Seeds (default roles, statuses, grade scale) are copied into a school when it is created.

**Suspension:** a suspended school's users cannot sign in, and its public website shows a maintenance page. Data is retained.
**Offboarding:** a per-school export (SQL + files ZIP) followed by a scheduled purge, run only by a super admin with confirmation.

### 4.1 Money (FR 10, 11)
- Java `BigDecimal` only. `double`/`float` are banned for money (enforced by an ArchUnit rule).
- DB type `NUMERIC(14,2)`. Currency PKR (single currency). Rounding `HALF_UP` to 0.01 at line level.
  Percentage discounts are computed per line and rounded once.
- A `Money` value object lives in `shared/money`.
- Financial records are **append-only**. Corrections are reversal entries with a reason and approver
  (FR 10.13, 11.11). Rows are never hard-deleted or updated in place.

### 4.2 Transactions and integrity
- One payment = one DB transaction covering the receipt, allocations to bill lines, ledger update, journal
  entry (cash/bank debit, fee income credit) and audit row (FR 10.7, 10.18, 11.8). It saves completely or not at all.
- Optimistic locking (`@Version`) on bills and accounts. Receipt numbers come from a per-year DB sequence
  table locked `FOR UPDATE`, so they are gapless and unique.
- Every write endpoint that creates money records accepts an `Idempotency-Key` header, so double-clicks and retries
  do not create duplicate receipts.

### 4.3 Accounting model (FR 11)
- Internally, a **lightweight double-entry journal** (`journal_entry` + `journal_line`, debits = credits enforced).
  Staff only see simple screens: cash/bank accounts, income, expense, transfer, vendor.
- Fee payments post automatically, exactly once, linked to the receipt (FR 11.8).
- This gives account ledgers, the daily cash summary, the income/expense summary and (later) a balance sheet from one source of truth.
  The FR only recommends this approach; decided in DECISIONS D-05.

### 4.4 Academic year scoping (FR 1.2, 15.1)
- Year-bound data (enrolments, bills, attendance, exams) carries `academic_year_id`.
- The frontend sends the selected year in the `X-Academic-Year` header. The backend resolves it into a request-scoped
  `YearContext`, and defaults to the current year.
- Writes into a **closed** year are rejected unless the user has `year.correct_closed` (FR 1.3).
- A student's class membership is stored as `enrolment` rows with `effective_from/effective_to`. Past
  attendance and results therefore keep reporting under the class/section the student was in at the time (FR 5.4, 9.6).

### 4.5 Security (details in SECURITY.md)
- Spring Security with **server-side sessions** (Spring Session JDBC), stored in an HTTP-only, Secure, SameSite=Lax cookie.
  Inactivity timeout is configurable (default 30 min). Sessions can be revoked when an account is deactivated (FR 2.4).
- CSRF protection: HttpOnly `XSRF-TOKEN` cookie + the raw token returned by `GET /api/v1/auth/csrf`, sent back as `X-XSRF-TOKEN`.
  (A JS-readable cookie would not work: the cookie belongs to `api.` and the app runs on `app.`.)
- Permission model: `module.action` permissions (view/add/edit/delete/print/approve) grouped into roles, plus a **data scope**
  (ALL, ASSIGNED_CLASSES, SELF). Checked in application services with `@PreAuthorize` and scope-aware queries.
  Hiding a menu item is cosmetic only and never counts as access control (FR 2.5).
- Step-up verification: per-user OTP for sensitive areas (accounts, salaries), replacing the old shared passcode (FR 2.6).

### 4.6 Audit trail (FR 2.8, 11.12, 12.2)
- `audit_log` table: actor, action, entity type/id, `before` and `after` as `jsonb`, reason, IP, user agent, timestamp.
- Written by an application-service aspect inside the same transaction. It is append-only, and the DB role has no UPDATE/DELETE on it.
- Login history and failed attempts are stored separately in `login_event` (FR 2.7).

### 4.7 Settings (FR 12)
- Typed settings registry in code: key, type, default, group, and the permission needed to edit it.
  Values live in a `setting_value` table with history.
- All modules read settings through `SettingsService`, so one rule is applied everywhere (FR 12.3). Values are cached and evicted when changed.

### 4.8 Files (FR 4.9, 6.1, 16)
- R2 private bucket via the AWS S3 SDK. The DB stores only metadata (`stored_file`: key, mime type, size, sha256, owner entity).
- Downloads use short-lived presigned URLs, issued only after a permission check. Public website media goes in a separate public bucket/prefix.
- Upload limits: images ≤ 5 MB, documents ≤ 10 MB. MIME type is checked by magic bytes, and filenames are never trusted.

### 4.9 Documents: PDF and Excel (FR 5.8, 10.6, 14.13, 15.6)
- `shared/pdf` renders vouchers, receipts, report cards, certificates, registers and admit cards. All of them use the
  school "document identity" (logo, letterhead, stamp, signatures; FR 1.1).
- **Library: OpenPDF** (LGPL/MPL, small footprint), using code-built layouts behind a `PdfTemplate` interface so layouts can be swapped per school.
  JasperReports is not used: its designer workflow adds weight we don't need. English only, embedded Inter/Noto Sans fonts (DECISIONS D-07).
- Excel exports use Apache POI SXSSF (streaming).
- Large batches (for example 800 vouchers) run as async jobs. The PDF is stored in R2 and the user gets a download link.

### 4.10 Batch and scheduling (FR 10.5, 10.11, 10.12, payroll)
- Background runs (chunked, restartable, idempotent; DECISIONS D-31): monthly bill generation, late-fee application, absent-fine application, salary sheet, nightly
  DB dump to R2. Jobs are restartable and idempotent: a unique key on `(student, fee_head, period)` prevents double billing.
- `job_run` records show the operator, parameters, counts and errors in the UI.

### 4.11 Messaging (FR 4.3, 4.12, 8.6, 10.16, 14.14)
- `communication` module: templates (English, placeholders), a transactional **outbox** table, a worker that sends
  through a `MessageProvider` port (SMS / WhatsApp adapters), and a delivery log.
- Messages are queued in the business transaction and sent after commit, so a failing gateway never blocks a payment.

### 4.12 Frontend
- Next.js App Router, TypeScript strict mode, shadcn/ui + Tailwind, TanStack Query (server state), react-hook-form + zod,
  TanStack Table (lists with filter/sort/export), Recharts (dashboard charts).
- API types are generated from the backend OpenAPI spec (springdoc → `openapi-typescript`). The build fails if they drift.
- English-only UI, left-to-right. No i18n framework. Font: Inter via `next/font`.
- Public website pages are SSR/ISR for SEO, resolved per school by host. They are revalidated (tag `school:{id}`) by a webhook when content is published (FR 16.10).
- Accessibility target: WCAG 2.2 AA.

### 4.13 Observability
- Spring Boot Actuator `/actuator/health` (liveness/readiness) for Railway health checks.
- Structured JSON logs with a request ID (`X-Request-Id`, echoed to the client).
- Error tracking (Sentry or similar) on both backend and frontend. External uptime monitor on `/actuator/health` and the website.

## 5. Non-functional targets

| Area | Target |
|---|---|
| Scale (v1) | Up to 20 schools, ≤ 2,000 students and ≤ 150 staff per school, ≤ 200 concurrent users platform-wide |
| API latency | p95 < 300 ms for reads, < 800 ms for payment save |
| Page load | LCP < 2.5 s on 4G for admin pages; website Lighthouse ≥ 90 performance/accessibility/SEO |
| Batch | Monthly bills for 1,000 students < 60 s; 1,000 vouchers PDF < 3 min |
| Availability | 99.5% monthly (no sleeping instances; paid Railway plan) |
| Backups | Railway backups + daily `pg_dump` to R2, 30 daily / 12 monthly retained, restore tested monthly |
| RPO / RTO | RPO ≤ 24 h (≤ 1 h with Railway PITR if available on plan), RTO ≤ 4 h |
| Browsers | Latest Chrome, Edge, Firefox, Safari (desktop/laptop; tablet usable) |

## 6. Explicitly out of scope (FR 19)
Digital/biometric/QR/GPS attendance, transport management (except the transport fee head), shop/POS/inventory/assets,
online tests, question papers, lesson planning, visitor management beyond the daily count, and predictive analytics.
