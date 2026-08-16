#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 || ! "$1" =~ ^[0-9]+$ || ! "$2" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Usage: $0 <phase> <40-character commit SHA>" >&2
  exit 64
fi

phase="$1"
candidate_sha="$2"
repo_root="$(git rev-parse --show-toplevel)"
manifest="$repo_root/scripts/ci/stage-manifest.mjs"
compose_file="$repo_root/platform/compose/compose.yml"
clean_override="$repo_root/platform/compose/compose.stage-clean.yml"
env_file="/opt/streamora/platform/compose/.env"
evidence_dir="$repo_root/platform/stage-evidence"
state_dir="${HOME}/.local/state/streamora"
state_file="$state_dir/last-healthy.json"
state_backup="${RUNNER_TEMP:-/tmp}/streamora-last-healthy-${candidate_sha}.json"

field() {
  node "$manifest" get "$phase" "$1"
}

ensure_current_pr_sha() {
  if [[ -z "${STREAMORA_REPOSITORY:-}" || -z "${STREAMORA_PR_NUMBER:-}" || -z "${GH_TOKEN:-}" ]]; then
    echo "PR freshness inputs are required for stage acceptance." >&2
    return 1
  fi
  EXPECTED_SHA="$candidate_sha" node -e '
    const { GH_TOKEN, STREAMORA_REPOSITORY, STREAMORA_PR_NUMBER, EXPECTED_SHA } = process.env;
    const headers = { Authorization: `Bearer ${GH_TOKEN}`, Accept: "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28" };
    const get = async (path) => {
      const response = await fetch(`https://api.github.com/repos/${STREAMORA_REPOSITORY}/${path}`, { headers });
      if (!response.ok) throw new Error(`GitHub returned ${response.status} for ${path}`);
      return response.json();
    };
    Promise.all([
      get(`pulls/${STREAMORA_PR_NUMBER}`),
      get(`compare/master...${EXPECTED_SHA}`),
      get(`commits/${EXPECTED_SHA}/check-runs`)
    ]).then(([pull, comparison, checks]) => {
      if (pull.head.sha !== EXPECTED_SHA) throw new Error(`Stage candidate is stale: ${EXPECTED_SHA} != ${pull.head.sha}`);
      if (pull.base.ref !== "master" || comparison.behind_by !== 0) throw new Error("Stage candidate is no longer based on the latest master.");
      const gate = checks.check_runs.find((check) => check.name === "功能验证门禁" && check.head_sha === EXPECTED_SHA);
      if (!gate || gate.status !== "completed" || gate.conclusion !== "success") throw new Error("Functional verification is no longer successful for the stage candidate.");
    }).catch((error) => { console.error(error.message); process.exitCode = 1; });
  '
}

node "$manifest" validate-tests "$phase"
ensure_current_pr_sha

minimum_compose_version="2.24.4"
compose_version="$(docker compose version --short | sed 's/^v//')"
if [[ "$(printf '%s\n%s\n' "$minimum_compose_version" "$compose_version" | sort -V | head -n 1)" != "$minimum_compose_version" ]]; then
  echo "Docker Compose $minimum_compose_version or newer is required for isolated clean-install overrides; found $compose_version." >&2
  exit 78
fi

compose_profile="core"
if (( phase == 8 )); then
  compose_profile="full"
fi
compose_services="$(field composeServices)"
IFS=' ' read -r -a compose_service_list <<< "$compose_services"
backend_tests="$(field backendTests)"
backend_test_modules="${backend_tests// /,}"
e2e_grep="@smoke|@phase-([0-$phase])"
minimum_resources="$(field minimumResources)"
minimum_cpu="$(node -e 'const value=JSON.parse(process.argv[1]); console.log(value.cpu)' "$minimum_resources")"
minimum_memory_gb="$(node -e 'const value=JSON.parse(process.argv[1]); console.log(value.memoryGb)' "$minimum_resources")"
minimum_disk_gb="$(node -e 'const value=JSON.parse(process.argv[1]); console.log(value.diskGb)' "$minimum_resources")"

actual_cpu="$(nproc)"
actual_memory_gb="$(awk '/MemTotal/ { print int($2 / 1024 / 1024) }' /proc/meminfo)"
actual_disk_gb="$(df -BG --output=avail "$repo_root" | tail -n 1 | tr -dc '0-9')"
if (( actual_cpu < minimum_cpu || actual_memory_gb < minimum_memory_gb || actual_disk_gb < minimum_disk_gb )); then
  echo "Stage VM resources are insufficient: cpu=$actual_cpu/$minimum_cpu memoryGb=$actual_memory_gb/$minimum_memory_gb diskGb=$actual_disk_gb/$minimum_disk_gb" >&2
  exit 78
fi

if [[ ! -r "$env_file" ]]; then
  echo "The VM-local Compose environment file is missing or unreadable." >&2
  exit 78
fi

mkdir -p "$evidence_dir"
if [[ -r "$state_file" ]]; then
  cp "$state_file" "$state_backup"
else
  rm -f "$state_backup"
fi
rm -f "$state_dir/stage-blocked.json"

mark_stage_blocked() {
  local reason="$1"
  install -d -m 700 "$state_dir"
  rm -f "$state_file"
  umask 077
  printf '{"phase":%s,"candidateSha":"%s","status":"blocked","reason":"%s","blockedAt":"%s"}\n' \
    "$phase" "$candidate_sha" "$reason" "$(date --iso-8601=seconds)" > "$state_dir/stage-blocked.json"
}

export STREAMORA_STAGE_PHASE="$phase"
export STREAMORA_COMPOSE_PROFILE="$compose_profile"
export STREAMORA_COMPOSE_SERVICES="$compose_services"
export STREAMORA_E2E_GREP="$e2e_grep"
export STREAMORA_ENV_FILE="$env_file"
export STREAMORA_COMPOSE_PROJECT="streamora"
export STREAMORA_STATE_DIR="$state_dir"
export STREAMORA_E2E_REPORT_SUFFIX="stage-upgrade-phase-$phase"
export STREAMORA_DEFER_HEALTH_RECORD="true"

echo "Running the phase $phase declared Maven integration-test set."
bash "$repo_root/mvnw" -B -ntp -pl "$backend_test_modules" -am verify

clean_project="streamora-stage-clean-${phase}-${candidate_sha:0:12}"
clean_state_dir="${RUNNER_TEMP:-/tmp}/$clean_project-state"

clean_compose() {
  STREAMORA_COMPOSE_PROJECT="$clean_project" STREAMORA_IMAGE_TAG="$candidate_sha" docker compose \
    --project-name "$clean_project" \
    --env-file "$env_file" \
    -f "$compose_file" \
    -f "$clean_override" \
    --profile "$compose_profile" "$@"
}

cleanup_clean_stack() {
  clean_compose down --remove-orphans >/dev/null 2>&1 || true
}
upgrade_succeeded=false
acceptance_complete=false
rollback_handled=false

cleanup_stage_run() {
  cleanup_clean_stack
  if [[ "$upgrade_succeeded" == "true" && "$acceptance_complete" != "true" && "$rollback_handled" != "true" ]]; then
    if ! restore_previous_stage; then
      mark_stage_blocked "stage-interrupted-without-safe-restoration"
    fi
  fi
}
trap cleanup_stage_run EXIT

collect_clean_diagnostics() {
  local diagnostics_dir="$repo_root/platform/stage-diagnostics"
  mkdir -p "$diagnostics_dir"
  clean_compose ps --all > "$diagnostics_dir/clean-compose-ps.txt" 2>&1 || true
  clean_compose logs --no-color --tail 300 2>&1 | sed -E \
    -e 's#(://)[^:/@[:space:]]+:[^@[:space:]]+@#\1***:***@#g' \
    -e 's#([Aa]uthorization[":=[:space:]]+)([Bb]earer[[:space:]]+)?[^[:space:]",}]+#\1***#g' \
    -e 's#("([Pp]assword|[Ss]ecret|[Tt]oken|[Aa]pi[_-]?[Kk]ey|[Aa]uthorization|[Cc]ookie)"[[:space:]]*:[[:space:]]*")[^"]*"#\1***"#g' \
    -e 's#([Pp][Aa][Ss][Ss][Ww][Oo][Rr][Dd]|[Ss][Ee][Cc][Rr][Ee][Tt]|[Tt][Oo][Kk][Ee][Nn]|[Aa][Pp][Ii][_-]?[Kk][Ee][Yy]|[Aa]uthorization|[Cc]ookie)([=:[:space:]]+)[^[:space:]]+#\1\2***#g' \
    > "$diagnostics_dir/clean-compose-logs.txt" || true
}

restore_previous_stage() {
  if [[ ! -r "$state_backup" ]]; then
    echo "No previous health record exists; automatic state restoration is unavailable." >&2
    return 1
  fi
  local previous_sha previous_source_sha previous_phase previous_profile previous_compose previous_services_value
  if ! previous_sha="$(node -p "JSON.parse(require('fs').readFileSync(process.argv[1], 'utf8')).sha" "$state_backup")"; then return 1; fi
  if ! previous_source_sha="$(node -e '
    const fs = require("fs");
    const state = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    let source = state.sourceSha || state.masterSha || state.sha;
    if (fs.existsSync(process.argv[2])) {
      const promoted = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
      if (promoted.candidateSha === state.sha) source = promoted.masterSha;
    }
    process.stdout.write(source);
  ' "$state_backup" "$state_dir/promoted-master.json")"; then return 1; fi
  if ! previous_phase="$(node -p "JSON.parse(require('fs').readFileSync(process.argv[1], 'utf8')).phase || ''" "$state_backup")"; then return 1; fi
  previous_profile="core"
  if [[ "$previous_phase" == "8" ]]; then previous_profile="full"; fi
  if ! previous_services_value="$(node -e '
    const state = JSON.parse(require("fs").readFileSync(process.argv[1], "utf8"));
    process.stdout.write(Array.isArray(state.services) ? state.services.join(" ") : "");
  ' "$state_backup")"; then return 1; fi
  IFS=' ' read -r -a previous_services <<< "$previous_services_value"
  if ! git cat-file -e "$previous_source_sha^{commit}"; then
    echo "Previous accepted source revision $previous_source_sha is unavailable in the checkout." >&2
    return 1
  fi
  if git diff --name-only "$previous_source_sha" "$candidate_sha" | grep -Eq '(^|/)db/migration/'; then
    echo "Automatic state restoration is disabled because Flyway migrations changed." >&2
    return 1
  fi
  previous_compose="${RUNNER_TEMP:-/tmp}/streamora-previous-compose-${previous_source_sha}.yml"
  if ! git show "$previous_source_sha:platform/compose/compose.yml" > "$previous_compose"; then
    echo "Unable to materialize the previous accepted Compose definition." >&2
    return 1
  fi
  if ! (
    unset WEB_PORT ADMIN_WEB_PORT NACOS_PORT MINIO_API_PORT MINIO_CONSOLE_PORT PROMETHEUS_PORT GRAFANA_PORT
    restore_args=(up -d --no-build --wait --wait-timeout 300 --remove-orphans)
    if (( ${#previous_services[@]} > 0 )); then restore_args+=("${previous_services[@]}"); fi
    STREAMORA_COMPOSE_PROJECT=streamora STREAMORA_IMAGE_TAG="$previous_sha" docker compose \
      --project-name streamora \
      --project-directory "$repo_root/platform/compose" \
      --env-file "$env_file" \
      -f "$previous_compose" \
      --profile "$previous_profile" \
      "${restore_args[@]}"
  ); then
    echo "The previous accepted Compose stack could not be restored." >&2
    return 1
  fi
  if ! cp "$state_backup" "$state_file"; then return 1; fi
  echo "The previous immutable stage image and health record were restored." >&2
}

echo "Running phase $phase upgrade-path deployment for $candidate_sha."
export STREAMORA_COMPOSE_PROJECT="streamora"
unset STREAMORA_COMPOSE_OVERRIDE
unset WEB_PORT ADMIN_WEB_PORT NACOS_PORT MINIO_API_PORT MINIO_CONSOLE_PORT PROMETHEUS_PORT GRAFANA_PORT
export STREAMORA_STATE_DIR="$state_dir"
export STREAMORA_E2E_REPORT_SUFFIX="stage-upgrade-phase-$phase"
if ! bash "$repo_root/scripts/ci/deploy-core.sh" "$candidate_sha" bash "$repo_root/scripts/ci/run-deployed-e2e.sh"; then
  if ! restore_previous_stage; then
    mark_stage_blocked "upgrade-failed-without-safe-restoration"
  fi
  rollback_handled=true
  exit 1
fi
upgrade_succeeded=true

export STREAMORA_COMPOSE_PROJECT="$clean_project"
export STREAMORA_COMPOSE_OVERRIDE="$clean_override"
export STREAMORA_STATE_DIR="$clean_state_dir"
export STREAMORA_E2E_REPORT_SUFFIX="stage-clean-phase-$phase"
export WEB_PORT="$((13000 + phase * 10))"
export ADMIN_WEB_PORT="$((13001 + phase * 10))"
export NACOS_PORT="$((13848 + phase * 10))"
export MINIO_API_PORT="$((13900 + phase * 10))"
export MINIO_CONSOLE_PORT="$((13901 + phase * 10))"
export PROMETHEUS_PORT="$((13909 + phase * 10))"
export GRAFANA_PORT="$((13002 + phase * 10))"
echo "Running phase $phase isolated clean-install deployment for $candidate_sha."
if ! bash "$repo_root/scripts/ci/deploy-core.sh" "$candidate_sha" bash "$repo_root/scripts/ci/run-deployed-e2e.sh"; then
  echo "Clean-install verification failed after the upgrade-path deployment." >&2
  collect_clean_diagnostics
  if ! restore_previous_stage; then
    mark_stage_blocked "clean-install-failed-without-safe-restoration"
  fi
  rollback_handled=true
  exit 1
fi

if ! ensure_current_pr_sha; then
  echo "Stage acceptance completed for a stale candidate; the result will not be registered." >&2
  if ! restore_previous_stage; then
    mark_stage_blocked "stale-candidate-without-safe-restoration"
  fi
  rollback_handled=true
  exit 1
fi

STREAMORA_COMPOSE_PROJECT=streamora STREAMORA_IMAGE_TAG="$candidate_sha" docker compose \
  --project-name streamora \
  --env-file "$env_file" \
  -f "$compose_file" \
  --profile "$compose_profile" \
  ps --format json "${compose_service_list[@]}" > "$evidence_dir/upgrade-health.json"
STREAMORA_COMPOSE_PROJECT=streamora STREAMORA_IMAGE_TAG="$candidate_sha" docker compose \
  --project-name streamora \
  --env-file "$env_file" \
  -f "$compose_file" \
  --profile "$compose_profile" \
  logs --no-color "${compose_service_list[@]}" 2>&1 | \
  sed -E \
    -e 's#(://)[^:/@[:space:]]+:[^@[:space:]]+@#\1***:***@#g' \
    -e 's#([Aa]uthorization[":=[:space:]]+)([Bb]earer[[:space:]]+)?[^[:space:]",}]+#\1***#g' \
    -e 's#("([Pp]assword|[Ss]ecret|[Tt]oken|[Aa]pi[_-]?[Kk]ey|[Aa]uthorization|[Cc]ookie)"[[:space:]]*:[[:space:]]*")[^"]*"#\1***"#g' | \
  grep -Ei 'flyway|migrat(ion|ing|ed)|schema history|schema version' > "$evidence_dir/flyway-results.txt" || true
if [[ ! -s "$evidence_dir/flyway-results.txt" ]]; then
  echo "No Flyway log lines were emitted; container health evidence remains authoritative." > "$evidence_dir/flyway-results.txt"
fi
clean_compose ps --format json "${compose_service_list[@]}" > "$evidence_dir/clean-health.json"
clean_compose images --format json > "$evidence_dir/images.json"
image_digest="sha256:$(sha256sum "$evidence_dir/images.json" | cut -d ' ' -f 1)"
printf '{"phase":%s,"candidateSha":"%s","upgrade":"success","cleanInstall":"success","healthArtifacts":["upgrade-health.json","clean-health.json","flyway-results.txt"],"imageDigest":"%s","testDataVersion":"%s","project":"%s","completedAt":"%s"}\n' \
  "$phase" "$candidate_sha" "$image_digest" "$(field testDataVersion)" "$clean_project" "$(date --iso-8601=seconds)" > "$evidence_dir/stage-acceptance.json"

install -d -m 700 "$state_dir"
umask 077
STATE_SHA="$candidate_sha" STATE_PHASE="$phase" STATE_IMAGE_DIGEST="$image_digest" STATE_SERVICES="$compose_services" node -e '
  const fs = require("fs");
  const state = {
    sha: process.env.STATE_SHA,
    sourceSha: process.env.STATE_SHA,
    phase: Number(process.env.STATE_PHASE),
    project: "streamora",
    services: process.env.STATE_SERVICES.split(/\s+/).filter(Boolean),
    imageDigest: process.env.STATE_IMAGE_DIGEST,
    deployedAt: new Date().toISOString()
  };
  fs.writeFileSync(process.argv[1], JSON.stringify(state) + "\n");
' "$state_file"
acceptance_complete=true

echo "Phase $phase stage acceptance passed for $candidate_sha."
