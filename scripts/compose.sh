#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test -f .env || python3 scripts/configure.py
docker compose --env-file .env -f infra/docker/compose.yml "${@:-up}"
