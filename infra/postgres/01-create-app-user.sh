#!/usr/bin/env bash
set -Eeuo pipefail

: "${APP_DB_USER:?APP_DB_USER is required}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD is required}"

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=app_user="$APP_DB_USER" \
  --set=app_password="$APP_DB_PASSWORD" \
  --set=app_database="$POSTGRES_DB" <<-'EOSQL'
    CREATE EXTENSION IF NOT EXISTS vector;
    SELECT format(
      'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT',
      :'app_user', :'app_password'
    ) \gexec
    SELECT format('ALTER DATABASE %I OWNER TO %I', :'app_database', :'app_user') \gexec
    SELECT format('GRANT ALL ON SCHEMA public TO %I', :'app_user') \gexec
EOSQL
