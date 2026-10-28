# Project Decisions

All former open questions are now decided. D-01 and D-18 were decided by the product owner; the rest were decided by the development team
under the owner's instruction ("use your capabilities to solve them"). A decision changes only if the owner asks.

| # | Topic | Decision | Rationale | Affects |
|---|---|---|---|---|
| D-01 | **Multiple schools** | One platform serves **many schools**. Each school has its own users, roles, data, settings, documents and website; school users never see other schools. Shared DB with `school_id` + PostgreSQL RLS (ARCHITECTURE §4.0). Sign-in = school code + username + password | **Owner decision.** Shared schema is cheapest on Railway and simplest to migrate; RLS gives database-level isolation | All |
| D-01a | **Super admin = system owner** | The super admin is **the product owner of the whole system**, not a school user. The super admin has **full access to all schools' details and data**: an all-schools overview with key figures and cross-school reports, one-click switching into any school with every module and permission (view and edit), plus school management (create/suspend schools, domains, school admins and users, audit logs, exports). All actions are audited. Mandatory TOTP | **Owner decision** | All |
| D-02 | UI library | **shadcn/ui** + Tailwind (Radix primitives) | Accessible, fully customisable, no runtime theme lock-in | Frontend |
| D-03 | Legacy data migration | Reusable per-school **CSV/Excel importer** with validation + reconciliation report. Imported: active students, families/guardians, staff, classes/sections, **opening fee balance per student** at cut-over, and the last 2 years of exam results. Older history is attached as archived PDF/Excel files on the school record | Full-history migration from an un-inspected system is high-risk; balances and current records are what daily work needs | Phase 2 (formats), 11 |
| D-04 | Payroll | **Basic payroll**: salary profile with increments; monthly sheet from approved staff attendance; deductions per settings (absent, half-day, late-as-absent, extra leave); tax as a manual per-staff amount; approve → pay → journal posting; salary slip PDF | FR references it in 6.8/12/13.12; full tax computation is out of scope | Phase 4 |
| D-05 | Accounting depth | Internal **double-entry journal**, invisible to users (simple income/expense/transfer screens). Balance sheet report = S priority | Guarantees fee income is counted once and the books always balance | Phase 3–4 |
| D-06 | SMS / WhatsApp | Provider-agnostic `MessageProvider` port. **SMS first** through one platform-level gateway account (a Pakistani bulk-SMS gateway with an HTTP API, selected when the account is opened); each school sets its own sender mask; messages metered per school for billing. **WhatsApp** (Meta Cloud API) after go-live. Email via SMTP for password resets | One integration serves all schools; costs traceable per school | Phase 1 (OTP), 7 |
| D-07 | PDF engine | **OpenPDF** only (JasperReports not used). English only, embedded fonts | Urdu removed, so no complex shaping needed; OpenPDF is lighter | Phase 0+ |
| D-08 | Printing | Fee voucher: A4 with 3 copies (bank / school / parent). Receipt: A5. Thermal receipts not in v1 | Matches common Pakistani school practice; works on any laser printer | Phase 3 |
| D-09 | Attendance % | `% = (Present + Late + 0.5 × HalfDay) ÷ (WorkingDays − ApprovedLeaveDays) × 100`, rounded to 1 dp. Holidays/OFF days are never working days. A per-school setting can make Late count as absent after N lates (default off) | Clear, common and testable (QA-ATT-006) | Phase 5 |
| D-10 | Two-factor auth | Super admin: TOTP **mandatory**. School users: step-up OTP required for sensitive screens; sign-in TOTP optional per user, and each school can make it mandatory for admin/principal/accountant | Balances security with ease for teachers | Phase 1 |
| D-11 | Antivirus on uploads | Not in v1. Strict size/magic-byte checks, private storage, download-only (never inline) | Cost vs. risk for school documents/photos | Phase 2 |
| D-12 | Timetable / daily diary | Timetable = S (FR 6.4). Teacher daily diary is **not in v1** (mentioned but never described in the FR) | Keeps scope to described features | Phase 2 |
| D-13 | Extra roles | Seed **SCHOOL_OWNER** (a school's proprietor/director, inside one school only, not the system owner: read-only finance + reports + dashboard) and **AUDITOR** (read-only finance + audit log) roles in every school | FR 10.17 "owner", FR 10.18 "auditors" | Phase 1 |
| D-14 | Grading details | Percentage computed to 2 dp; band lower bound inclusive (95.00 = A++). Positions: standard competition ranking (1, 1, 3). Cumulative result: per-exam weights, default equal. All configurable per school | Deterministic and testable (QA-EXM-005/007/012) | Phase 6 |
| D-15 | Document designs | 2 report-card layouts, 1 design per certificate type, 1 voucher, 1 receipt. Each school customises via its document identity (logo, letterhead, stamp, signatures) and layout options in settings | Serves many schools without bespoke work | Phase 2, 3, 6 |
| D-16 | Bank details / bank payments | Bank name, account title and number come from school settings and are printed on vouchers. Bank-paid vouchers are entered manually by the accountant (no bank file import in v1) | No bank integration specified | Phase 3 |
| D-17 | Accounts and domains | SelectCode owns the Vercel, Railway and Cloudflare accounts (it is the platform operator). Platform domain is written as `<platform>` in the docs until registered. Schools bring their own website domain (or use `<code>.sites.<platform>.com`) | Multi-school SaaS needs one operator | Phase 0 |
| D-18 | **Language** | **English only, permanently.** No Urdu, no RTL, no i18n framework, in UI, PDFs, SMS or website. FR references to Urdu (4.3, 10.16) are dropped | **Owner decision.** Avoids rendering/shaping issues | All |
| D-19 | **Parent and student portals** | **Removed.** No parent or student logins, portal or app. FR 8.7 is out of scope. Parents are informed by SMS (receipts, attendance, results, reminders) and printed vouchers/report cards. Leave applications are entered by office staff (FR 13.8) | **Owner decision** | All |
| D-20 | Mobile | Staff portal is responsive (usable on tablets); no mobile app | Follows D-19 | Frontend |
| D-22 | **Source control** | **No GitHub** (or any hosted git). Local git repository only, no remote. Quality checks run locally with `scripts/verify.sh`; deploys use the Railway and Vercel CLIs; dependency updates are reviewed monthly by hand; the repository is backed up as a `git bundle` to private R2 storage each release | **Owner decision** | All |
| D-23 | Step-up channel | Step-up codes are 6-digit, emailed, valid 5 minutes (5 tries); a verified step-up lasts 15 minutes in the session and is cleared when the super admin switches school. Users without an email cannot do step-up actions | Works without an SMS provider; SMS can be added in Phase 7 | Phase 1 |
| D-24 | Audit writing | Application services call `AuditService.record(...)` explicitly inside their transaction (instead of an automatic aspect), so each entry has a meaningful action name, before/after values and reason | Clearer, testable history | All |
| D-25 | Single backend instance in v1 | Rate limits and the settings cache are in memory per instance. Before running more than one backend instance, move them to the database or a shared cache | Simplicity; one instance is enough for v1 load | Phase 10 |
| D-26 | Super admin new-school flow | The new school's ID is reserved first, then the school row, profile, built-in roles and main administrator are created in one transaction already running as that school (RLS) | Atomic: no half-created schools | Phase 1 |
| D-21 | Online fee payment | Deferred (FR 10.19 = C). The payment-provider port is designed in Phase 3 so it can be added later (it would use a payment link sent by SMS, since there is no portal) | FR marks it as a future option | Later |

## Inputs still needed (not decisions; these don't block Phase 0)
| Input | Needed by | From |
|---|---|---|
| Platform domain name (registered) | Phase 0 end | SelectCode |
| SMS gateway business account + sender mask approval | Phase 7 (OTP can use email until then) | SelectCode / each school |
| Export files from the current system (students, families, balances, results) | Phase 2 for field mapping, Phase 10 for the run | Each school / old vendor |
| School logo, letterhead, stamp, signature images; sample printed vouchers/report cards | Phase 2–3 | Each school |
| Bank details for vouchers | Phase 3 | Each school |

## Change log
| Date | # | Change | By |
|---|---|---|---|
| 2026-10-27 | D-01 | Multi-school with super admin | Product owner |
| 2026-10-27 | D-18 | Urdu removed permanently; English only | Product owner |
| 2026-10-27 | D-02…D-21 | Defaults finalised as decisions | Dev team (owner's instruction) |
| 2026-10-27 | D-01a | Super admin is the system owner with full access to all schools' data (replaces the earlier "restricted support access" design) | Product owner |
| 2026-10-27 | D-19, D-20 | Parent and student portals removed | Product owner |
| 2026-10-27 | D-22 | No GitHub; local repository, local checks, CLI deploys | Product owner |
| 2026-10-28 | D-23…D-26 | Phase 1 implementation decisions | Dev team |
| 2026-10-28 | — | No git commits; changes stay in the working tree | Product owner |
