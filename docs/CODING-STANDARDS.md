# Coding Standards and Workflow

## General
- English only: code, identifiers, commits, UI, PDFs and messages. **No Urdu, no RTL, no i18n framework** (DECISIONS D-18). Do not add it back.
- Every school-owned entity extends `TenantScopedEntity`. Never accept `schoolId` from request input on authenticated APIs. Never use
  `ThreadLocal` tenant state outside `TenantContext`. Cache keys always include `schoolId`.
- Domain terms follow the FR glossary: `AcademicYear`, `GrNumber`, `Family`, `FeeHead`, `Bill`, `Voucher`, `Receipt`, `Defaulter`, `Register`, `ReportCard`, `Datesheet`.

## Backend (Java 21, Spring Boot)
- Package by feature (see ARCHITECTURE §3). A module may use another module only through its public facade or events.
- DTOs are Java `record`s in `api/`. Entities never leave the application layer.
- `@Transactional` lives on application-service methods only (not on controllers or repositories).
- Money: `Money`/`BigDecimal` only. `double`/`float` in money code fails the ArchUnit build.
- Time: `java.time` only. Inject a `Clock` so tests can control "today".
- Validation: Bean Validation on DTOs; business rules in the domain/service layer raise `BusinessRuleException(code)`.
- Mapping: MapStruct or hand-written mappers. No reflection-based "magic" copying for money fields.
- Migrations: `V<NNN>__<snake_description>.sql`. Never edit an applied migration. Each must be backward compatible with the previous release.
- Logging: SLF4J, structured. No PII.
- Formatting: Spotless (google-java-format). Static analysis: Error Prone or SpotBugs at warning level, failing on new issues.

## Frontend (Next.js, TypeScript)
- `strict: true`; no `any` without a justification comment.
- Server state via TanStack Query; forms via react-hook-form + zod; no global state library unless justified.
- Money is handled as strings from the API and formatted via one `formatMoney()` helper. Arithmetic uses `decimal.js` only when unavoidable;
  the server is the source of truth for totals.
- Accessibility: semantic elements, labelled inputs, keyboard support for grids (attendance, marks entry), focus management in dialogs.
- Components: shadcn/ui primitives in `components/ui`; feature components in `features/<module>`.
- Formatting: ESLint + Prettier. Imports sorted.

## Git
- Branches: `feature/<fr-id>-short-name` (e.g. `feature/10.7-partial-payments`), `fix/...`, `hotfix/...`.
- Conventional Commits: `feat(fees): record partial payments (FR 10.7)`.
- Local repository only: no GitHub or other remote (DECISIONS D-22). Keep merges small (< 500 changed lines where possible).
- Required before merging into `develop`/`main`: `scripts/verify.sh` passes, the diff is reviewed (`git diff develop...feature/x`),
  and the security checklist (SECURITY.md §7) is ticked.

## Definition of Done
A feature is done when:
1. Acceptance criteria are met, and the FR "School example" is covered by an automated test.
2. Unit + integration tests are written. Coverage thresholds hold. The permission matrix is updated for new endpoints.
3. Tenant isolation (school_id, RLS, QA-TEN sweep), audit, year scoping and permission checks are implemented where applicable.
4. UI passes axe with no serious/critical issues, is keyboard operable, and works on desktop and tablet widths.
5. Print/PDF outputs (if any) are verified with the school's document identity.
6. OpenAPI is updated, and frontend types are regenerated.
7. It is deployed to staging and QA has verified it. The REQUIREMENTS-TRACEABILITY status is updated.
8. User-facing help text or a release note has been added.
