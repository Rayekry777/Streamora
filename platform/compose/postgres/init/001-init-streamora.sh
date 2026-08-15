#!/bin/sh
set -eu

: "${STREAMORA_SERVICE_DB_PASSWORD:?STREAMORA_SERVICE_DB_PASSWORD is required}"

psql --set ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
CREATE EXTENSION IF NOT EXISTS vector;
EOSQL

for mapping in \
  admin:admin identity:identity user:user_profile video:video media:media \
  playback:playback danmaku:danmaku comment:comment engagement:engagement \
  feed:feed search:search pet:pet agent:agent moderation:moderation \
  notification:notification
do
  unit="${mapping%%:*}"
  schema_name="${mapping#*:}"
  role_name="streamora_${unit}"

  psql --set ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set service_role="$role_name" \
    --set service_schema="$schema_name" \
    --set service_password="$STREAMORA_SERVICE_DB_PASSWORD" <<'EOSQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'service_role', :'service_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'service_role') \gexec
SELECT format('CREATE SCHEMA IF NOT EXISTS %I AUTHORIZATION %I', :'service_schema', :'service_role') \gexec
SELECT format('GRANT CONNECT, CREATE ON DATABASE %I TO %I', current_database(), :'service_role') \gexec
SELECT format('ALTER ROLE %I SET search_path TO %I, public', :'service_role', :'service_schema') \gexec
EOSQL
done
