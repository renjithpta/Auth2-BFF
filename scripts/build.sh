#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
"${MVN:-mvn}" -B -ntp verify
for app in ui ui-flight ui-workflow; do (cd "$app"; npm ci; npm run build); done
