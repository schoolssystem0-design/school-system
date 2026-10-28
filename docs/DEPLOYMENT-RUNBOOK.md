# Deployment Runbook

Locked hosting: **Vercel** (frontend), **Railway paid plan** (Spring Boot + PostgreSQL, same region, private network),
**Cloudflare R2** (files, PDFs, backups).

## 1. Environments

| | Staging | Production |
|---|---|---|
| Frontend | Vercel project `sms-web`, preview deploy aliased to `staging-app.<platform>.com` | `vercel deploy --prod` → `app.<platform>.com` + each school's website domain (e.g. `www.nns.edu.pk`) |
| Backend | Railway project `sms-staging` | Railway project `sms-prod` |
| Database | Railway Postgres in `sms-staging` (one DB, all schools) | Railway Postgres in `sms-prod` (one DB, all schools) |
| API domain | `staging-api.<platform>.com` | `api.<platform>.com` |
| School websites | `<code>.staging-sites.<platform>.com` (wildcard) | School's own domain, or fallback `<code>.sites.<platform>.com` |
| R2 | `sms-files-staging` | `sms-files-prod`, `sms-public-prod`, `sms-backups` |
| SMS | Fake/allow-list | Real provider |

Region: the Railway region closest to Pakistan (Singapore listing; confirm on the plan). Backend and DB **must** share the region.

## 2. Source control, checks and deploys (no GitHub: DECISIONS D-22)
The code lives in a **local git repository only**. There is no GitHub (or other hosted git), no remote and no hosted CI.
Deploys are pushed from the developer machine with the Railway and Vercel CLIs.

Branches (local):
- `main` = what is in production. `develop` = what is on staging. Work on `feature/<fr-id>-name` branches.
- Merge a feature into `develop` only after `scripts/verify.sh` passes, then deploy to staging.
- After staging QA sign-off, merge `develop` into `main`, tag it (`git tag v0.x.y`), and deploy to production.
- Hotfix: branch from `main`, fix, verify, deploy, then merge back into `develop`.

Quality gate, run locally before every merge and deploy:
```
scripts/verify.sh            # secrets check, backend mvn verify (unit + integration on PostgreSQL),
                             # API type drift, frontend lint/typecheck/tests, production build, npm audit
cd frontend && npm run e2e   # browser tests (API must be running; E2E_BASE_URL=https://staging-app... for staging)
```

Deploy commands (one-time: `npm i -g @railway/cli vercel`, then `railway login`, `vercel login`, and link each folder):
```
# Backend → Railway (uploads backend/ and builds backend/Dockerfile; Flyway migrates on start)
cd backend && railway up --service sms-backend --environment staging       # or: --environment production

# Frontend → Vercel (one project; "Preview" env vars hold staging values, "Production" env vars hold production values)
cd frontend && url=$(vercel deploy) && vercel alias set "$url" staging-app.<platform>.com   # staging
cd frontend && vercel deploy --prod                                                          # production
```

Dependency updates (no Dependabot): monthly, run `mvn versions:display-dependency-updates` and `npm outdated`.
Update, run `scripts/verify.sh`, and deploy to staging first.

Backup of the code itself: the repository is the only copy, so back it up. Each release, make a
`git bundle create sms-<tag>.bundle --all` and store it off the machine (e.g. in the private R2 bucket `sms-backups/code/`).

Railway builds the backend from `backend/Dockerfile` (multi-stage: Maven build → `eclipse-temurin:21-jre`) on `railway up`. Vercel builds `frontend/` on `vercel deploy`.

## 3. Backend container
```dockerfile
# backend/Dockerfile (outline)
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd -r app
USER app
COPY --from=build /app/target/sms-backend.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```
- Set the Railway memory limit to match the plan (start at 1 GB; `MaxRAMPercentage=75` keeps the heap below it).
- Health checks: Railway healthcheck path `/actuator/health/readiness`.
- Flyway runs on startup. If a migration fails, the container fails and Railway keeps the previous deployment serving.

## 4. Environment variables

Backend (Railway):
| Variable | Example / notes |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `staging` / `prod` |
| `DB_APP_USER`, `DB_APP_PASSWORD` | Non-owner role used at runtime (RLS enforced) |
| `DB_MIGRATION_USER`, `DB_MIGRATION_PASSWORD` | Owner role used only by Flyway |
| `PUBLIC_SITE_SIGNING_SECRET` | Verifies `X-School-Host` from the Next.js server |
| `BOOTSTRAP_SUPERADMIN_EMAIL`, `BOOTSTRAP_SUPERADMIN_PASSWORD` | Creates the first super admin if none exists (TOTP enrolled at first sign-in); remove after |
| `DATABASE_URL` / `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` | Railway reference variables (private host) |
| `APP_CORS_ALLOWED_ORIGINS` | `https://app.<platform>.com` |
| `APP_COOKIE_DOMAIN` | `.<platform>.com` |
| `APP_SESSION_IDLE_MINUTES` | `30` |
| `R2_ENDPOINT`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_FILES`, `R2_BUCKET_PUBLIC` | Cloudflare R2 S3 API credentials |
| `SMS_PROVIDER`, `SMS_API_KEY`, `SMS_ALLOWLIST` | Platform-level gateway account (DECISIONS D-06); the sender ID/mask is set per school in its settings |
| `MAIL_HOST`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM` | Password reset emails |
| `SENTRY_DSN` | Error tracking |
| `REVALIDATE_WEBHOOK_URL`, `REVALIDATE_SECRET` | Notify Vercel when website content is published |

Frontend (Vercel):
| Variable | Notes |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `https://api.<platform>.com/api/v1` |
| `PUBLIC_SITE_SIGNING_SECRET` | Must match the backend; server-only |
| `PLATFORM_APP_HOST` | `app.<platform>.com`; any other host is treated as a school website |
| `REVALIDATE_SECRET` | Must match the backend |
| `NEXT_PUBLIC_TURNSTILE_SITE_KEY`, `TURNSTILE_SECRET` | Public form protection |
| `SENTRY_DSN` | |

## 5. Backups and restore
1. **Railway Postgres backups** enabled on the production DB (schedule per plan).
2. **Daily off-site dump:** a Railway cron service (or the Railway "postgres daily backups" template) runs at 02:00 PKT:
   `pg_dump -Fc` → `sms-backups/daily/YYYY-MM-DD.dump`. Monthly copies go to `monthly/`. R2 lifecycle keeps 30 daily + 12 monthly.
3. Backup job success/failure posts to the alert channel. A missing backup triggers an alert by 06:00.
4. **Monthly restore drill** (also before go-live):
   ```
   pg_restore --no-owner -d <scratch-db-url> YYYY-MM-DD.dump
   ```
   Boot a staging backend against the scratch DB and run the checksum query (student count, family count, Σ outstanding,
   Σ journal debits/credits). Record the result in `docs/restore-log.md`.
5. R2 files: enable object versioning on `sms-files-prod` (or a weekly rclone copy to a second bucket).

### Per-school restore
The database is shared, so a full restore affects every school. To recover **one** school's data (e.g. after an accidental bulk change),
restore the dump into a scratch DB and copy that school's rows back with the per-school export/import tool (P-8), under super admin control.

## 6. Onboarding a new school
1. Super admin → `/platform` → Create school (code, name). Defaults are seeded automatically.
2. Create the school's main administrator. The setup link is sent to their email/phone.
3. Website: add the school's domain in the platform console and in Vercel (`vercel domains add`). The school sets DNS (CNAME → `cname.vercel-dns.com`).
4. School admin completes the setup wizard. Optional data import from their old system.
5. Run the smoke checklist for that school; set status ACTIVE.

## 7. Rollback
- **App:** Railway/Vercel "redeploy previous deployment" (instant).
- **DB:** Flyway migrations are forward-only. Every migration must stay backward compatible with the previous app version
  (expand → migrate → contract across two releases). For destructive changes, take a manual backup right before deploying.

## 8. Monitoring
- Uptime monitor (e.g. Better Stack / UptimeRobot) on `https://api.<platform>.com/actuator/health`, `https://app.<platform>.com` and each school website, every 1 min.
- Logs and Sentry events carry `schoolId` (never PII) so issues can be traced per school.
- Sentry for backend and frontend errors. Alert on new issues and spikes.
- Railway metrics: CPU, memory and restarts. Alert above 85% memory.
- **Costs:** set a Railway spending limit and review usage monthly (Stack doc §4).

## 9. Go-live checklist
- [ ] Custom domains + TLS on platform and school website hostnames; HSTS enabled.
- [ ] CORS and cookie domain verified in production.
- [ ] Two super admins with TOTP created; `BOOTSTRAP_*` vars removed.
- [ ] RLS verified in production (QA-TEN-003 run as the app role).
- [ ] Backups: Railway + R2 dump verified; restore drill done.
- [ ] SMS provider live; sender ID approved; allow-list removed.
- [ ] Monitoring + alerts tested (simulate a failure).
- [ ] Data migration reconciled and signed off by the accountant and principal.
- [ ] Spending limit set.
