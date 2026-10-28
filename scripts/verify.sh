#!/usr/bin/env bash
# Local quality gate: run before every merge to main and before every deploy (no external CI is used).
#
#   scripts/verify.sh          full check
#   scripts/verify.sh --quick  skip the production build and npm audit
#
# Exits non-zero on the first failure.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
QUICK="${1:-}"

step() { printf '\n\033[1m== %s\033[0m\n' "$1"; }

step "Secrets: no .env files or private keys tracked by git"
cd "$ROOT"
if git ls-files | grep -E '(^|/)\.env($|\.)' | grep -v '\.env\.example$'; then
  echo "ERROR: .env files are tracked. Remove them from git." >&2; exit 1
fi
if git ls-files -z | xargs -0 grep -lIE -- '-----BEGIN ([A-Z]+ )?PRIVATE KEY-----' 2>/dev/null; then
  echo "ERROR: a private key is tracked by git." >&2; exit 1
fi

step "Backend: unit, architecture and integration tests (real PostgreSQL)"
cd "$ROOT/backend"
mvn -B -q verify

step "API contract: frontend types match the backend OpenAPI document"
cd "$ROOT/frontend"
cp src/lib/api/schema.d.ts /tmp/sms-schema.before.d.ts
npm run -s gen:api > /dev/null
if ! diff -q /tmp/sms-schema.before.d.ts src/lib/api/schema.d.ts > /dev/null; then
  echo "ERROR: API types were out of date and have been regenerated. Review and commit src/lib/api/schema.d.ts." >&2
  exit 1
fi

step "Frontend: lint, typecheck, unit tests"
npm run -s lint
npm run -s typecheck > /dev/null
npm test --silent

if [ "$QUICK" != "--quick" ]; then
  step "Frontend: production build"
  npm run -s build > /dev/null

  step "Dependencies: npm audit (production, high+)"
  npm audit --omit=dev --audit-level=high
fi

printf '\n\033[32mAll checks passed.\033[0m\n'
