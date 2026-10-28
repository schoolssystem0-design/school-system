#!/bin/sh
# pg_dump (custom format) -> R2 daily/YYYY-MM-DD.dump; on the 1st of the month also monthly/YYYY-MM.dump.
# Retention (30 daily / 12 monthly) is an R2 lifecycle rule on the bucket, not done here.
#
# Required env: PGHOST PGPORT PGUSER PGPASSWORD PGDATABASE
#               R2_ENDPOINT R2_ACCESS_KEY_ID R2_SECRET_ACCESS_KEY R2_BUCKET_BACKUPS
# Optional:     BACKUP_PREFIX (default "prod"), ALERT_WEBHOOK_URL (POSTed on failure)
set -eu

PREFIX="${BACKUP_PREFIX:-prod}"
DAY="$(date -u +%Y-%m-%d)"
FILE="/tmp/sms-${DAY}.dump"

fail() {
  echo "Backup FAILED: $1" >&2
  if [ -n "${ALERT_WEBHOOK_URL:-}" ]; then
    wget -q -O /dev/null --header="Content-Type: application/json" \
      --post-data="{\"text\":\"SMS database backup failed (${PREFIX}, ${DAY}): $1\"}" "$ALERT_WEBHOOK_URL" || true
  fi
  exit 1
}

export AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION="auto"

pg_dump --format=custom --no-owner --compress=9 --file="$FILE" || fail "pg_dump error"
# A dump that cannot be listed is unusable; check it before upload.
pg_restore --list "$FILE" > /dev/null || fail "dump is not readable"

aws s3 cp "$FILE" "s3://${R2_BUCKET_BACKUPS}/${PREFIX}/daily/${DAY}.dump" \
  --endpoint-url "$R2_ENDPOINT" --only-show-errors || fail "upload to R2"

if [ "$(date -u +%d)" = "01" ]; then
  aws s3 cp "$FILE" "s3://${R2_BUCKET_BACKUPS}/${PREFIX}/monthly/$(date -u +%Y-%m).dump" \
    --endpoint-url "$R2_ENDPOINT" --only-show-errors || fail "monthly upload to R2"
fi

echo "Backup OK: ${PREFIX}/daily/${DAY}.dump ($(du -h "$FILE" | cut -f1))"
rm -f "$FILE"
