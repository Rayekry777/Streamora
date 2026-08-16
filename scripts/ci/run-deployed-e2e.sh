#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
compose_file="${STREAMORA_COMPOSE_FILE:-$repo_root/platform/compose/compose.yml}"
compose_override="${STREAMORA_COMPOSE_OVERRIDE:-}"
compose_profile="${STREAMORA_COMPOSE_PROFILE:-core}"
compose_project="${STREAMORA_COMPOSE_PROJECT:-streamora}"
env_file="${STREAMORA_ENV_FILE:-/opt/streamora/platform/compose/.env}"

if [[ ! -r "$env_file" ]]; then
  echo "The VM-local Compose environment file is missing or unreadable." >&2
  exit 78
fi

read_env_value() {
  local key="$1"
  awk -v key="$key" '
    $0 !~ /^[[:space:]]*#/ && index($0, key "=") == 1 {
      value = substr($0, length(key) + 2)
      sub(/\r$/, "", value)
      if (value ~ /^".*"$/ || value ~ /^'\''.*'\''$/) {
        value = substr(value, 2, length(value) - 2)
      }
      print value
      exit
    }
  ' "$env_file"
}

compose() {
  local args=(docker compose --project-name "$compose_project" --env-file "$env_file" -f "$compose_file")
  if [[ -n "$compose_override" ]]; then
    args+=(-f "$compose_override")
  fi
  STREAMORA_COMPOSE_PROJECT="$compose_project" "${args[@]}" --profile "$compose_profile" "$@"
}

web_address="$(compose port web 80)"
admin_address="$(compose port admin-web 80)"
admin_login="$(read_env_value STREAMORA_BOOTSTRAP_ADMIN_LOGIN)"
admin_password="$(read_env_value STREAMORA_BOOTSTRAP_ADMIN_PASSWORD)"

if [[ -z "$web_address" || -z "$admin_address" || -z "$admin_login" || -z "$admin_password" ]]; then
  echo "The deployed addresses or browser-test administrator credentials are unavailable." >&2
  exit 78
fi

playwright_args=(test --config playwright.config.ts)
if [[ -n "${STREAMORA_E2E_GREP:-}" ]]; then
  playwright_args+=(--grep "$STREAMORA_E2E_GREP")
fi

docker run --rm \
  --network host \
  --ipc=host \
  --user "$(id -u):$(id -g)" \
  -e PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
  -e E2E_WEB_BASE_URL="http://${web_address}" \
  -e E2E_ADMIN_BASE_URL="http://${admin_address}" \
  -e E2E_ADMIN_LOGIN="$admin_login" \
  -e E2E_ADMIN_PASSWORD="$admin_password" \
  -e STREAMORA_E2E_REPORT_SUFFIX="${STREAMORA_E2E_REPORT_SUFFIX:-deployed}" \
  -e CI=true \
  -v "$repo_root:/workspace" \
  -w /workspace \
  mcr.microsoft.com/playwright:v1.62.0-noble \
  /workspace/node_modules/.bin/playwright "${playwright_args[@]}"
