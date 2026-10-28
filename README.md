# School Management System

Multi-school, ERP-style school management platform built by SelectCode (first school: NNS Secondary School).
About 158 features across 16 functional modules: setup, security, dashboard, admissions, students, staff,
families, classes, fees, accounts, settings, attendance, exams, reports and the public website.

- **Multiple schools** on one platform. Each school has its own users, data, settings and website, fully isolated.
- **Super admin = the system owner.** Full access to every school's details and data, an all-schools overview,
  one-click switching into any school, plus creating and managing schools and their administrators.
- **Staff-only system.** No parent or student portal; parents are reached by SMS and printed documents.
- **English only.** Urdu/RTL support has been removed permanently.

## Locked stack

| Layer | Choice | Hosted on |
|---|---|---|
| Frontend | Next.js + React + TypeScript, shadcn/ui | Vercel |
| Backend | Java 21 + Spring Boot (Security, Batch, Data JPA, Flyway) | Railway (paid) |
| Database | PostgreSQL | Railway (same region as backend, private network) |
| Files and backups | Cloudflare R2 (S3-compatible) | Cloudflare |
| PDFs / Excel | OpenPDF / Apache POI | Inside backend |

## Source documents

- `School_Management_System_Functional_Requirements.pdf`: what the system must do (v1.0, Oct 2026)
- `School_Management_System_Stack_and_Deployment_Final.pdf`: locked technology and hosting decision

## Project documentation (`docs/`)

| File | Purpose |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture, repo layout, backend modules, cross-cutting design |
| [docs/DATA-MODEL.md](docs/DATA-MODEL.md) | Core entities, relationships and database rules per module |
| [docs/API-CONVENTIONS.md](docs/API-CONVENTIONS.md) | REST conventions, auth, errors, pagination, year scoping |
| [docs/IMPLEMENTATION-PLAN.md](docs/IMPLEMENTATION-PLAN.md) | Phased delivery plan with tasks and exit criteria |
| [docs/REQUIREMENTS-TRACEABILITY.md](docs/REQUIREMENTS-TRACEABILITY.md) | Every FR feature → phase, priority, backend module, test IDs |
| [docs/QA-TEST-PLAN.md](docs/QA-TEST-PLAN.md) | Test strategy, tools, critical test cases, UAT, release gates |
| [docs/SECURITY.md](docs/SECURITY.md) | Security and privacy requirements |
| [docs/DEPLOYMENT-RUNBOOK.md](docs/DEPLOYMENT-RUNBOOK.md) | Environments, local quality gate, CLI deploys, env vars, backups, restore, monitoring |
| [docs/CODING-STANDARDS.md](docs/CODING-STANDARDS.md) | Conventions, Git workflow, Definition of Done |
| [docs/DECISIONS.md](docs/DECISIONS.md) | All project decisions (multi-school, English-only, payroll, accounting …) with rationale |

## Run locally

Needs Java 21, Maven and Node 24+. No Docker or installed database is required.

```bash
# 1. API on http://localhost:8080 with an embedded PostgreSQL and two demo schools
cd backend
mvn spring-boot:test-run -Dspring-boot.run.main-class=com.selectcode.sms.LocalDevApplication

# 2. App on http://localhost:3000 (second terminal)
cd frontend
npm install
npm run dev
```

| Who | Where | Sign in with |
|---|---|---|
| School administrator | http://localhost:3000/sign-in | school `DEMO` (or `DEMO2`), username `admin`, password `Admin@12345` |
| Super admin (system owner) | http://localhost:3000/platform/sign-in | `owner@local.test` / `Owner@123456` + the code from an authenticator app set up with key `JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP` |

Emails the app would send locally (setup links, password resets, verification codes) are shown at
http://localhost:8080/api/v1/dev/outbox. Demo data is discarded when the API stops.

Before merging or deploying, run `scripts/verify.sh` (all checks). Browser tests: `cd frontend && npm run e2e`.
The code is kept in a local git repository only; there is no GitHub.

## Repository layout

```
School-System/
├── ops/backup/       Daily pg_dump → Cloudflare R2 job + restore drill
├── backend/          Spring Boot API (Maven)
├── frontend/         Next.js app: school portal, super admin console, school public websites
├── docs/             This documentation
├── scripts/verify.sh Local quality gate (run before every merge and deploy)
└── CLAUDE.md         Guidance for AI-assisted development
```
