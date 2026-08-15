#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || ! "$1" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Usage: $0 <40-character commit SHA> [post-deploy-check-command...]" >&2
  exit 64
fi

deploy_sha="$1"
shift
post_deploy_check=("$@")
repo_root="$(git rev-parse --show-toplevel)"
compose_file="$repo_root/platform/compose/compose.yml"
env_file="/opt/streamora/platform/compose/.env"
state_dir="${HOME}/.local/state/streamora"
state_file="$state_dir/last-healthy.json"

if [[ ! -r "$env_file" ]]; then
  echo "The VM-local Compose environment file is missing or unreadable." >&2
  exit 78
fi

compose() {
  STREAMORA_IMAGE_TAG="$1" docker compose --env-file "$env_file" -f "$compose_file" --profile core "${@:2}"
}

has_migration_change() {
  local previous_sha="$1"
  git diff --name-only "$previous_sha" "$deploy_sha" | grep -Eq '(^|/)db/migration/'
}

verify_core() {
  local web_address admin_address container_id status health
  web_address="$(compose "$deploy_sha" port web 80)"
  admin_address="$(compose "$deploy_sha" port admin-web 80)"
  curl --fail --silent --show-error --max-time 10 "http://${web_address}/healthz" >/dev/null
  curl --fail --silent --show-error --max-time 10 "http://${admin_address}/healthz" >/dev/null
  compose "$deploy_sha" exec -T gateway-service wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness
  compose "$deploy_sha" exec -T identity-service wget -q -O /dev/null http://127.0.0.1:8082/actuator/health/readiness

  while IFS= read -r container_id; do
    [[ -n "$container_id" ]] || continue
    status="$(docker inspect --format '{{.State.Status}}' "$container_id")"
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$container_id")"
    if [[ "$status" != "running" || "$health" == "unhealthy" ]]; then
      echo "Container $container_id is status=$status health=${health:-none}" >&2
      return 1
    fi
  done < <(compose "$deploy_sha" ps -q)
}

run_post_deploy_check() {
  if (( ${#post_deploy_check[@]} == 0 )); then
    return 0
  fi

  echo "Running post-deployment browser integration checks."
  "${post_deploy_check[@]}"
}

rollback() {
  local previous_sha="$1"
  echo "Deployment failed; restoring last healthy images $previous_sha." >&2
  compose "$previous_sha" up -d --no-build --wait --wait-timeout 300 --remove-orphans
  deploy_sha="$previous_sha"
  verify_core
}

previous_sha=""
if [[ -r "$state_file" ]]; then
  previous_sha="$(sed -nE 's/.*"sha"[[:space:]]*:[[:space:]]*"([0-9a-f]{40})".*/\1/p' "$state_file" | head -n 1)"
fi

if ! compose "$deploy_sha" config --quiet; then
  exit 1
fi

if compose "$deploy_sha" up -d --build --wait --wait-timeout 900 --remove-orphans && verify_core && run_post_deploy_check; then
  install -d -m 700 "$state_dir"
  umask 077
  printf '{"sha":"%s","deployedAt":"%s"}\n' "$deploy_sha" "$(date --iso-8601=seconds)" > "$state_file"
  echo "Core deployment is healthy at $deploy_sha."
  exit 0
fi

if [[ "$previous_sha" =~ ^[0-9a-f]{40}$ ]] && ! has_migration_change "$previous_sha"; then
  rollback "$previous_sha"
else
  if [[ "$previous_sha" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Deployment changed Flyway migrations; automatic database rollback is disabled." >&2
  else
    echo "No recorded healthy revision exists; automatic rollback is unavailable." >&2
  fi
fi

exit 1
