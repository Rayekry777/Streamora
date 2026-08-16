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
compose_file="${STREAMORA_COMPOSE_FILE:-$repo_root/platform/compose/compose.yml}"
compose_override="${STREAMORA_COMPOSE_OVERRIDE:-}"
compose_profile="${STREAMORA_COMPOSE_PROFILE:-core}"
compose_project="${STREAMORA_COMPOSE_PROJECT:-streamora}"
env_file="${STREAMORA_ENV_FILE:-/opt/streamora/platform/compose/.env}"
state_dir="${STREAMORA_STATE_DIR:-${HOME}/.local/state/streamora}"
state_file="$state_dir/last-healthy.json"
IFS=' ' read -r -a stage_services <<< "${STREAMORA_COMPOSE_SERVICES:-}"

if [[ ! -r "$env_file" ]]; then
  echo "The VM-local Compose environment file is missing or unreadable." >&2
  exit 78
fi

compose() {
  local image_tag="$1"
  shift
  local args=(docker compose --project-name "$compose_project" --env-file "$env_file" -f "$compose_file")
  if [[ -n "$compose_override" ]]; then
    args+=(-f "$compose_override")
  fi
  STREAMORA_COMPOSE_PROJECT="$compose_project" STREAMORA_IMAGE_TAG="$image_tag" "${args[@]}" --profile "$compose_profile" "$@"
}

has_migration_change() {
  local previous_sha="$1"
  local previous_source_sha="$previous_sha"
  if [[ -r "$state_file" ]]; then
    previous_source_sha="$(node -p "const s=JSON.parse(require('fs').readFileSync(process.argv[1], 'utf8')); s.sourceSha || s.masterSha || s.sha" "$state_file")"
  fi
  git diff --name-only "$previous_source_sha" "$deploy_sha" | grep -Eq '(^|/)db/migration/'
}

verify_core() {
  local web_address admin_address container_id status health
  local ps_args=(ps -q)
  web_address="$(compose "$deploy_sha" port web 80)"
  admin_address="$(compose "$deploy_sha" port admin-web 80)"
  curl --fail --silent --show-error --max-time 10 "http://${web_address}/healthz" >/dev/null
  curl --fail --silent --show-error --max-time 10 "http://${admin_address}/healthz" >/dev/null
  compose "$deploy_sha" exec -T gateway-service wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness
  compose "$deploy_sha" exec -T identity-service wget -q -O /dev/null http://127.0.0.1:8082/actuator/health/readiness

  if (( ${#stage_services[@]} > 0 )); then
    ps_args+=("${stage_services[@]}")
  fi
  while IFS= read -r container_id; do
    [[ -n "$container_id" ]] || continue
    status="$(docker inspect --format '{{.State.Status}}' "$container_id")"
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$container_id")"
    if [[ "$status" != "running" || "$health" == "unhealthy" ]]; then
      echo "Container $container_id is status=$status health=${health:-none}" >&2
      return 1
    fi
  done < <(compose "$deploy_sha" "${ps_args[@]}")
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
  local previous_source_sha previous_phase previous_profile previous_compose previous_services_value
  echo "Deployment failed; restoring last healthy images $previous_sha." >&2
  previous_source_sha="$(node -p "const s=JSON.parse(require('fs').readFileSync(process.argv[1], 'utf8')); s.sourceSha || s.masterSha || s.sha" "$state_file")"
  previous_phase="$(node -p "JSON.parse(require('fs').readFileSync(process.argv[1], 'utf8')).phase || ''" "$state_file")"
  previous_profile="core"
  if [[ "$previous_phase" == "8" ]]; then previous_profile="full"; fi
  previous_services_value="$(node -e '
    const state = JSON.parse(require("fs").readFileSync(process.argv[1], "utf8"));
    process.stdout.write(Array.isArray(state.services) ? state.services.join(" ") : "");
  ' "$state_file")"
  IFS=' ' read -r -a previous_services <<< "$previous_services_value"
  if ! git cat-file -e "$previous_source_sha^{commit}"; then
    echo "Previous accepted source revision $previous_source_sha is unavailable in the checkout." >&2
    return 1
  fi
  previous_compose="${RUNNER_TEMP:-/tmp}/streamora-previous-compose-${previous_source_sha}.yml"
  git show "$previous_source_sha:platform/compose/compose.yml" > "$previous_compose"
  rollback_args=(up -d --no-build --wait --wait-timeout 300 --remove-orphans)
  if (( ${#previous_services[@]} > 0 )); then rollback_args+=("${previous_services[@]}"); fi
  STREAMORA_COMPOSE_PROJECT="$compose_project" STREAMORA_IMAGE_TAG="$previous_sha" docker compose \
    --project-name "$compose_project" \
    --project-directory "$repo_root/platform/compose" \
    --env-file "$env_file" \
    -f "$previous_compose" \
    --profile "$previous_profile" \
    "${rollback_args[@]}"
}

previous_sha=""
if [[ -r "$state_file" ]]; then
  previous_sha="$(sed -nE 's/.*"sha"[[:space:]]*:[[:space:]]*"([0-9a-f]{40})".*/\1/p' "$state_file" | head -n 1)"
fi

if ! compose "$deploy_sha" config --quiet; then
  exit 1
fi

up_args=(up -d --build --wait --wait-timeout 900 --remove-orphans)
if (( ${#stage_services[@]} > 0 )); then
  up_args+=("${stage_services[@]}")
fi

if compose "$deploy_sha" "${up_args[@]}" && verify_core && run_post_deploy_check; then
  if [[ "${STREAMORA_DEFER_HEALTH_RECORD:-false}" != "true" ]]; then
    install -d -m 700 "$state_dir"
    umask 077
    printf '{"sha":"%s","phase":"%s","project":"%s","deployedAt":"%s"}\n' \
      "$deploy_sha" "${STREAMORA_STAGE_PHASE:-}" "$compose_project" "$(date --iso-8601=seconds)" > "$state_file"
    echo "Core deployment is healthy at $deploy_sha."
  else
    echo "Core deployment checks passed at $deploy_sha; health registration is deferred until stage acceptance completes."
  fi
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
