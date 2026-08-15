#!/usr/bin/env bash
set -euo pipefail

changed_files="$(git diff --name-only -- .)"
if [[ -z "$changed_files" ]]; then
  echo "Repair produced no changes." >&2
  exit 1
fi

while IFS= read -r file; do
  [[ -n "$file" ]] || continue
  case "$file" in
    .env|.env.*|*/.env|*/.env.*|*application-secret*.yml|*application-secret*.yaml|*.pem|*.key|*.p12|*.pfx|*.jks|*.keystore|*/db/migration/*|pnpm-lock.yaml|package-lock.json|yarn.lock|npm-shrinkwrap.json|composer.lock|Gemfile.lock)
      echo "Repair changed a prohibited file: $file" >&2
      exit 1
      ;;
  esac
done <<< "$changed_files"

if git diff -U0 -- .github/workflows | grep -E '^[+-].*(permissions:|secrets\.|OPENAI_API_KEY)' >/dev/null; then
  echo "Repair changed workflow permissions or secret handling." >&2
  exit 1
fi

if git diff -U0 | grep -E '^-.*(docker compose .*down.*-v|docker volume rm|rm -rf .*\b(data|volume|volumes)\b)' >/dev/null; then
  echo "Repair introduced persistent-volume deletion logic." >&2
  exit 1
fi
