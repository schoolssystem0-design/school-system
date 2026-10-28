# CLAUDE.md

Multi-school School Management Platform (SelectCode; first school NNS Secondary School). Read `README.md` first, then the doc relevant to the task in `docs/`.

## Source of truth
- Requirements: `School_Management_System_Functional_Requirements.pdf`. Feature IDs like `FR 10.7` refer to its sections; platform features are `P-x`.
  `docs/REQUIREMENTS-TRACEABILITY.md` lists every feature with phase, priority and status. Update the status when you finish a feature.
- Stack is **locked**: Next.js + TypeScript (Vercel), Java 21 + Spring Boot + PostgreSQL (Railway), Cloudflare R2. Do not propose alternatives.
- All decisions: `docs/DECISIONS.md`. Follow them. Do not re-open a decision without the user asking.

## Non-negotiable rules
- **Multi-school:** every school-owned table has `school_id` + RLS + composite keys. The tenant comes only from the session.
  School users never see another school; cross-school leakage is always S1.
- **Super admin = the system owner (the product owner).** Full access to all schools' data: all-schools overview, switch into any school
  with every permission, manage schools and users. Everything audited. Do not restrict this.
- **No parent or student portal/logins.** Removed by the owner. Do not add them back.
- **No GitHub.** Never create a GitHub repo, add a git remote, push, or use `gh`. Local git only; run `scripts/verify.sh` instead of CI.
- **English only.** Never add Urdu, RTL or i18n. The user removed it permanently.
- Money: `BigDecimal` / `NUMERIC(14,2)`, sent as strings in JSON. Never `double`/`float`.
- Financial and historical records are append-only. Correct them with reversal entries that carry a reason, never update/delete.
- A payment, its receipt, allocations, journal entry and audit row commit in one transaction. Money-creating endpoints take an `Idempotency-Key`.
- Every endpoint declares a permission and enforces data scope on the server. Hiding UI elements is not security.
- Year-bound data carries `academic_year_id`. Closed years are read-only.
- Never log PII (CNIC, phones, passwords, OTPs).
- Integration tests (`*IT`) run on a real embedded PostgreSQL (zonky, no Docker), never H2, with at least two seeded schools.
- Frontend: Next.js 16. Read `frontend/AGENTS.md` and `frontend/node_modules/next/dist/docs/` before writing Next code (e.g. `proxy.ts`, not `middleware.ts`; Cache Components on).
- Local run: `cd backend && mvn spring-boot:test-run -Dspring-boot.run.main-class=com.selectcode.sms.LocalDevApplication`, then `cd frontend && npm run dev`.

## Layout
- `backend/`: Maven, package `com.selectcode.sms.<module>` with `api/ app/ domain/ infra/` sub-packages; `platform/` module for super admin.
- `frontend/`: Next.js App Router. Routes: `(auth)/sign-in`, `app/` (school portal), `platform/` (super admin, Phase 1), `site/[host]/...` (per-school public website, reached only via the `proxy.ts` host rewrite).
- `docs/`: architecture, data model, API conventions, plan, QA, security, runbook, standards, decisions.

## Workflow
- Work phase by phase per `docs/IMPLEMENTATION-PLAN.md`. Meet the Definition of Done in `docs/CODING-STANDARDS.md`.
- Add test cases to `docs/QA-TEST-PLAN.md` when a feature introduces a new rule.
