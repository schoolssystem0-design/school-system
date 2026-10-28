# Security and Privacy Requirements

Baseline: **OWASP ASVS 4.0 Level 2**. The system holds children's personal data (B-Form, CNIC, phones, photos) and
school finances. Pakistan's Personal Data Protection Bill is not yet law; the design follows its draft principles anyway (purpose limitation, consent for photos).

## 0. Multi-school isolation and super admin
- Tenant isolation is the top security requirement. The layers are listed in ARCHITECTURE §4.0: session-bound tenant, Hibernate `@TenantId`,
  PostgreSQL RLS with a non-owner app role, composite FKs, tenant-prefixed R2 keys, and the QA-TEN test suite in `scripts/verify.sh`.
- The tenant is **never** read from client input on authenticated APIs. Public website APIs trust only the signed `X-School-Host` from the Next.js server.
- Super admin (`platform_user`) = **the system owner**, with **full read and write access to every school's data**. Because this account
  can see everything, it is the most protected: mandatory TOTP, separate sign-in, separate session cookie (`SMS_PLATFORM_SESSION`),
  optional IP allow-list, 15-minute idle timeout, alert on every new-location sign-in.
- Cross-school queries (`app.all_schools = on`) are available only on `/platform/overview/**` endpoints, which check the super admin role first.
- A second, emergency super admin account (credentials stored offline) allows recovery. Super admin actions are in `platform_audit_log` (append-only)
  and, when inside a school, also in that school's audit log.
- A suspended school's sessions are revoked immediately.

## 1. Authentication
- Passwords hashed with **bcrypt, cost 12** (Spring Security; no extra crypto dependency). Minimum 10 characters, checked against a breached-password list; no forced composition rules.
- Passwords are **never** retrievable or displayed. The old "retrieve passwords" feature is removed (FR 2.3).
- Reset: single-use token/OTP, 15-minute expiry, stored hashed, rate-limited. Admin-issued temporary passwords force a change.
- Lockout: 5 failures in 15 minutes locks the account for 15 minutes, or until an admin unlocks it (FR 2.7). Rate limit per IP as well.
- Sessions: server-side (Spring Session JDBC), ID rotated on login, cookie `HttpOnly; Secure; SameSite=Lax; Domain=.<platform>.com`. The session stores user ID + school ID.
  Idle timeout is configurable (default 30 min); absolute timeout 12 h. All sessions are revoked on password change or deactivation.
- **Step-up verification** (FR 2.6): OTP by SMS/email, or a TOTP app, valid for 15 minutes. Required for accounts, salary,
  settings edits, publishing results, unlocking published results, reversals and user/role management. It replaces the shared super-admin passcode.
- Sign-in 2FA (TOTP): mandatory for super admins; optional per school user, and a school may make it mandatory for admin/principal/accountant via settings (DECISIONS D-10).

## 2. Authorisation
- Deny by default. Every endpoint declares a permission. A test in `mvn verify` fails if a controller method lacks one (added in Phase 1 with permissions).
- Data scope is enforced in queries (teacher → assigned sections). An out-of-scope ID returns 404.
- Sensitive fields (fee balances, documents, CNIC, phones) are serialised only when the viewer holds the field permission (FR 5.11).
- The main admin cannot be deactivated or demoted while they are the last admin.

## 3. Data protection
- TLS everywhere (Vercel/Railway managed certificates). HSTS on both domains.
- Database reachable only on Railway's private network. The application DB user has least privilege (no DDL in production; Flyway uses a separate migration role if supported).
- `audit_log`, `payment`, `journal_line` are insert-only for the app role.
- R2 buckets are private. Access only through presigned URLs (≤ 5 min). Website public media is in a separate bucket.
- Backups (R2 `sms-backups`) are encrypted at rest. Access is limited to two named people. Lifecycle: 30 daily + 12 monthly.
- Secrets live in Vercel/Railway environment variables only. `.env*` files are gitignored, and `scripts/verify.sh` fails if an `.env` file or private key is tracked.
- PII is never written to logs (CNIC, phones, passwords, OTPs, tokens). Log keys are redacted by a logging filter.

## 4. Web security
- CORS: allow only `https://app.<platform>.com` (and the staging equivalent), with credentials. School websites call the API only server-side from Next.js.
- CSRF token on all state-changing requests.
- Security headers via Next.js and Spring: CSP (no inline scripts except Next nonce), `X-Content-Type-Options`, `Referrer-Policy`,
  `Permissions-Policy`, `frame-ancestors 'none'`.
- Input validation in both layers (zod + Bean Validation). JPA parameter binding only, no string-built SQL.
- Output encoding: React escapes by default. CMS rich text is sanitised server-side (allow-list HTML).
- File uploads: size limits, magic-byte MIME check, random object keys, `Content-Disposition: attachment` for documents.
  No antivirus scan in v1 (DECISIONS D-11). Documents are never rendered inline, only downloaded.
- Public forms: Cloudflare Turnstile + honeypot + per-IP rate limit.

## 5. Privacy
- Student photos and names appear on the website only when consent is recorded (FR 16.11).
- Purpose-based access: teachers see academic data only (FR 5.11, 15.7).
- Data export (Excel) of personal data is a separate permission, and exports are audited.
- Retention: records are kept permanently for certificates (FR 5.7, 14.15). Deactivated accounts keep their history but cannot sign in.

## 6. Monitoring and response
- Alerts on: repeated failed logins, new-location admin logins, 5xx spikes, failed backup job, failed batch jobs.
- Incident response: revoke sessions, rotate secrets, restore from backup, notify every affected school's owner within 24 h of a suspected breach.

## 7. Security checklist per merge
- [ ] New tables have `school_id`, an RLS policy and composite keys. New endpoints are covered by the QA-TEN sweep.
- [ ] New endpoints declare a permission and data scope; permission matrix test updated.
- [ ] No PII in logs. No secrets in code.
- [ ] Money writes are transactional, idempotent and audited.
- [ ] User-controlled input is validated. File handling uses the shared `FileService`.
