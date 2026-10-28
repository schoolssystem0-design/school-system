# Restore drill (monthly, and before go-live)

1. Download the latest dump:
   ```
   aws s3 cp s3://$R2_BUCKET_BACKUPS/prod/daily/<YYYY-MM-DD>.dump ./latest.dump --endpoint-url $R2_ENDPOINT
   ```
2. Create a scratch database (a temporary Railway Postgres, or local) and restore:
   ```
   pg_restore --no-owner --dbname "<scratch-db-url>" latest.dump
   ```
3. Point a staging backend at the scratch DB and confirm it starts (Flyway reports "Schema is up to date").
4. Run the checksum queries as the owner and compare with production at dump time:
   ```sql
   select count(*) from school;
   select school_id, count(*) from app_user group by school_id order by 1;
   -- From Phase 3: outstanding fees per school; from Phase 4: sum(debit) = sum(credit)
   ```
5. Record the result in `docs/restore-log.md` (date, dump file, duration, checksums match yes/no, who ran it).
6. Delete the scratch database.
