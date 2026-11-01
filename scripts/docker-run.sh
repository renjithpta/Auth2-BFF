#!/usr/bin/env bash
# Explicit Docker lifecycle for the full stack; Compose is optional here.
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/docker-run.py "${1:-up}"
