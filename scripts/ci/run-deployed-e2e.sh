#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
compose_file="$repo_root/platform/compose/compose.yml"
env_file="/opt/streamora/platform/compose/.env"

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
  docker compose --env-file "$env_file" -f "$compose_file" --profile core "$@"
}

web_address="$(compose port web 80)"
admin_address="$(compose port admin-web 80)"
admin_login="$(read_env_value STREAMORA_BOOTSTRAP_ADMIN_LOGIN)"
admin_password="$(read_env_value STREAMORA_BOOTSTRAP_ADMIN_PASSWORD)"

if [[ -z "$web_address" || -z "$admin_address" || -z "$admin_login" || -z "$admin_password" ]]; then
  echo "The deployed addresses or browser-test administrator credentials are unavailable." >&2
  exit 78
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
  -v "$repo_root:/workspace" \
  -w /workspace \
  mcr.microsoft.com/playwright:v1.62.0-noble \
  /bin/bash -lc '/workspace/node_modules/.bin/playwright test --config playwright.config.ts'
