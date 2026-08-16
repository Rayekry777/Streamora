#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
compose_file="$repo_root/platform/compose/compose.yml"
clean_override="$repo_root/platform/compose/compose.stage-clean.yml"
env_file="$repo_root/platform/compose/.env.example"
project_name="streamora-functional-${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-1}"
services_csv="${1:-}"

compose() {
  STREAMORA_COMPOSE_PROJECT="$project_name" \
    STREAMORA_IMAGE_TAG="functional-${GITHUB_SHA:-local}" \
    docker compose --project-name "$project_name" --env-file "$env_file" -f "$compose_file" -f "$clean_override" "$@"
}

cleanup() {
  compose --profile infra down --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

compose --profile core config --quiet

changed_services=()
if [[ -n "$services_csv" ]]; then
  IFS=',' read -r -a changed_services <<< "$services_csv"
fi

infra_services=(
  postgres
  postgres-privileges-init
  redis
  minio
  nacos
  rocketmq-namesrv
  rocketmq-volume-init
  rocketmq-broker
)

compose --profile infra up -d --wait --wait-timeout 300 "${infra_services[@]}"
compose --profile infra ps "${infra_services[@]}"

runtime_services=("${infra_services[@]}")
if (( ${#changed_services[@]} > 0 )); then
  echo "Booting affected services against real dependency containers: ${changed_services[*]}"
  compose --profile full up -d --build --wait --wait-timeout 600 "${changed_services[@]}"
  runtime_services+=("${changed_services[@]}")
fi

while IFS= read -r container_id; do
  [[ -n "$container_id" ]] || continue
  status="$(docker inspect --format '{{.State.Status}}' "$container_id")"
  health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$container_id")"
  exit_code="$(docker inspect --format '{{.State.ExitCode}}' "$container_id")"
  if [[ "$status" != "running" && "$status" != "exited" ]]; then
    echo "Container $container_id is status=$status health=${health:-none}" >&2
    exit 1
  fi
  if [[ -n "$health" && "$health" != "healthy" ]]; then
    echo "Container $container_id is status=$status health=$health" >&2
    exit 1
  fi
  if [[ "$status" == "exited" && "$exit_code" != "0" ]]; then
    echo "Container $container_id exited with code $exit_code" >&2
    exit 1
  fi
done < <(compose --profile infra --profile full ps -q "${runtime_services[@]}")

echo "Functional Docker verification passed for project $project_name."
