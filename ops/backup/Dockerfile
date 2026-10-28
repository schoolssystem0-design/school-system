# Daily off-site database dump to Cloudflare R2 (DEPLOYMENT-RUNBOOK §5).
# Deploy as a Railway cron service (schedule "0 21 * * *" = 02:00 PKT) in the same project as the database.
FROM postgres:18-alpine
RUN apk add --no-cache aws-cli
COPY backup.sh /usr/local/bin/backup.sh
RUN chmod +x /usr/local/bin/backup.sh
ENTRYPOINT ["/usr/local/bin/backup.sh"]
