#!/usr/bin/env bash
set -euo pipefail

pnpm contract:lint
pnpm contract:generate
git diff --exit-code -- packages/openapi/generated
