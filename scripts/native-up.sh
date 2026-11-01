#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test -f .env || python3 scripts/configure.py
python3 scripts/native-run.py up
