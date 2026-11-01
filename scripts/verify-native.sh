#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/native-up.sh
trap 'bash scripts/native-down.sh' EXIT
python3 - <<'PY'
import time
import urllib.request
urls = ['http://localhost:8085/realms/aobp', 'http://localhost:8088/']
urls += [f'http://localhost:{port}/actuator/health/readiness' for port in range(9091,9095)]
for attempt in range(90):
    try:
        for url in urls:
            with urllib.request.urlopen(url, timeout=2) as response:
                assert response.status == 200
        print('PASS native dependency/application readiness')
        break
    except Exception:
        if attempt == 89:
            raise
        time.sleep(1)
PY
python3 scripts/atc-simulate.py
python3 scripts/browser-flow-test.py

if [[ "${E2E_BROWSER:-0}" == 1 ]]; then (cd ui; npx playwright test); fi
