#!/bin/bash
# =====================================================================
# Creates one database + one user per service (ADR 0004).
#
# WHEN does this run?
#   The official postgres image runs every *.sh / *.sql file in
#   /docker-entrypoint-initdb.d ONLY when the data volume is empty,
#   i.e. on the very first start. On later starts it's skipped.
#   To re-run it: `make reset` (deletes the volume and all data).
#
# WHY a .sh and not a .sql file?
#   Plain SQL can't read environment variables. A shell script can,
#   so the password comes from SERVICE_DB_PASSWORD (set in .env).
# =====================================================================
set -euo pipefail   # stop on the first error, unset variable, or failed pipe

for svc in event booking payment ticket; do
  echo "Creating database ${svc}_db owned by user ${svc}_user"

  # psql runs as the superuser created from POSTGRES_USER.
  # ON_ERROR_STOP=1 makes psql exit non-zero if any statement fails.
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-SQL
    CREATE USER ${svc}_user WITH PASSWORD '${SERVICE_DB_PASSWORD}';
    CREATE DATABASE ${svc}_db OWNER ${svc}_user;

    -- By default every user may CONNECT to every database (via PUBLIC).
    -- Revoke that so a service can only reach its own database.
    REVOKE CONNECT ON DATABASE ${svc}_db FROM PUBLIC;
    GRANT  CONNECT ON DATABASE ${svc}_db TO ${svc}_user;
SQL
done

echo "Service databases created."
