#!/usr/bin/env bash
set -euo pipefail

# 在虚拟机中以 ub001 身份执行；注册令牌从 GitHub Runner 页面临时获取，绝不提交到仓库。
if [[ "$(id -un)" != "ub001" ]]; then
  echo "Run this installer as ub001, not root." >&2
  exit 1
fi

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <repository-url> <temporary-registration-token>" >&2
  exit 64
fi

repo_url="$1"
registration_token="$2"
runner_dir="$HOME/actions-runner"

if ! command -v docker >/dev/null; then
  echo "Docker must be installed before registering the runner." >&2
  exit 1
fi
if ! id -nG | tr ' ' '\n' | grep -qx docker; then
  echo "ub001 must belong to the docker group; log out and back in after adding it." >&2
  exit 1
fi

mkdir -p "$runner_dir"
cd "$runner_dir"
if [[ ! -x ./config.sh ]]; then
  runner_version="2.329.0"
  archive="actions-runner-linux-x64-${runner_version}.tar.gz"
  curl --fail --location --output "$archive" "https://github.com/actions/runner/releases/download/v${runner_version}/${archive}"
  tar xzf "$archive"
  rm "$archive"
fi

./config.sh --unattended --url "$repo_url" --token "$registration_token" --name streamora-core --labels streamora-core --work _work --replace
sudo ./svc.sh install ub001
sudo ./svc.sh start
