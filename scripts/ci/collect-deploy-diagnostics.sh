#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 || ! "$1" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Usage: $0 <40-character commit SHA> <output directory>" >&2
  exit 64
fi

deploy_sha="$1"
output_dir="$2"
repo_root="$(git rev-parse --show-toplevel)"
compose_file="$repo_root/platform/compose/compose.yml"
env_file="/opt/streamora/platform/compose/.env"

mkdir -p "$output_dir"

sanitize() {
  sed -E \
    -e 's#(://)[^:/@[:space:]]+:[^@[:space:]]+@#\1***:***@#g' \
    -e 's#([Pp][Aa][Ss][Ss][Ww][Oo][Rr][Dd]|[Ss][Ee][Cc][Rr][Ee][Tt]|[Tt][Oo][Kk][Ee][Nn]|[Aa][Pp][Ii][_-]?[Kk][Ee][Yy]|[Aa]uthorization|[Cc]ookie)([=:[:space:]]+)[^[:space:]]+#\1\2***#g'
}

compose() {
  STREAMORA_IMAGE_TAG="$deploy_sha" docker compose --env-file "$env_file" -f "$compose_file" --profile core "$@"
}

printf '{"sha":"%s","collectedAt":"%s"}\n' "$deploy_sha" "$(date --iso-8601=seconds)" > "$output_dir/deployment.json"

if [[ -r "$env_file" ]]; then
  compose ps --all > "$output_dir/compose-ps.txt" 2>&1 || true
  compose logs --no-color --tail 300 > "$output_dir/compose-logs.txt" 2>&1 || true
  sanitize < "$output_dir/compose-logs.txt" > "$output_dir/compose-logs.sanitized.txt"
  mv "$output_dir/compose-logs.sanitized.txt" "$output_dir/compose-logs.txt"

  {
    echo "web healthz"
    web_address="$(compose port web 80 2>&1 || true)"
    curl --fail --silent --show-error --max-time 10 "http://${web_address}/healthz" 2>&1 || true
    echo
    echo "admin-web healthz"
    admin_address="$(compose port admin-web 80 2>&1 || true)"
    curl --fail --silent --show-error --max-time 10 "http://${admin_address}/healthz" 2>&1 || true
    echo
    echo "gateway readiness"
    compose exec -T gateway-service wget -q -O - http://127.0.0.1:8080/actuator/health/readiness 2>&1 || true
    echo
    echo "identity readiness"
    compose exec -T identity-service wget -q -O - http://127.0.0.1:8082/actuator/health/readiness 2>&1 || true
  } | sanitize > "$output_dir/health-checks.txt"
else
  echo "VM-local environment file is missing or unreadable." > "$output_dir/diagnostic-error.txt"
fi
