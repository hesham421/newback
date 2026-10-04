#!/usr/bin/env bash
# Starts a throw-away erp-app-reference instance for one core_api_verify.py run profile (Dev/Test only).
#
#   docs/test-api/start_profile_instance.sh <jar> <port> <db-name> <log-file> [extra --spring args...]
#
# Required env: ERP_BOOTSTRAP_ADMIN_PASSWORD. Optional: JAVA_HOME (JDK 21+), PGUSER/PGPASSWORD
# (default postgres/postgres), PGHOST (localhost). The database must exist (createdb <db-name>).
# JWT_SECRET / FILE_TOKEN_SECRET are generated per start (tokens do not survive a restart anyway).
#
# Profile recipes (plan §2.1), all on the variant jar built with spring-boot-starter-mail added to
# erp-app-reference/pom.xml (see core-verify-report.md "How the profile builds were made"):
#   P-MAIL      : --spring.mail.host=localhost --spring.mail.port=1025   (core_api_verify.py runs the sink)
#   P-MAIL-DOWN : --spring.mail.host=localhost --spring.mail.port=2525   (nothing listens there)
#   P-CAP       : --erp.core.report.max-export-rows=2   (then restart with =3)
#   P-LOCAL     : --erp.core.files.storage=LOCAL --erp.core.files.local.root=<existing dir>
set -euo pipefail
JAR="$1"; PORT="$2"; DB="$3"; LOG="$4"; shift 4
: "${ERP_BOOTSTRAP_ADMIN_PASSWORD:?set ERP_BOOTSTRAP_ADMIN_PASSWORD}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
export DB_URL="jdbc:postgresql://${PGHOST:-localhost}:5432/${DB}"
export DB_USER="${PGUSER:-postgres}" DB_PASSWORD="${PGPASSWORD:-postgres}"
export JWT_SECRET="verify-$(od -An -tx1 -N32 /dev/urandom | tr -d ' \n')"
export FILE_TOKEN_SECRET="verify-$(od -An -tx1 -N32 /dev/urandom | tr -d ' \n')"
export ERP_BOOTSTRAP_ADMIN_PASSWORD
nohup "$JAVA" -jar "$JAR" --spring.profiles.active=dev --spring.cache.type=simple --server.port="$PORT" "$@" \
  > "$LOG" 2>&1 &
echo "pid=$!"
for _ in $(seq 1 120); do
  # /actuator/health is not used: with spring-boot-starter-mail and no SMTP server listening yet, the
  # mail health indicator keeps it at 503 DOWN although the app serves requests
  code=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${PORT}/v3/api-docs" || true)
  if [ "$code" = "200" ]; then echo "serving on ${PORT}"; exit 0; fi
  sleep 1
done
echo "instance did not come up; see ${LOG}" >&2
exit 1
