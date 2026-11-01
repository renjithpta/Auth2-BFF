# Verification record

Date: 31 October 2026. Baseline and commands are recorded here so the team can reproduce them.

## Commands

```bash
mvn -B -ntp verify
for app in ui ui-flight ui-workflow; do (cd "$app" && npm ci && npm run build); done
python3 -m pip install -r scripts/requirements.txt
python3 scripts/validate.py
python3 scripts/atc-simulate.py
helm lint infra/k8s/helm/aobp
helm template aobp infra/k8s/helm/aobp --namespace aobp-demo
```

## Executed evidence

Implementation checks performed 31 October 2026, with final source review on 1 November 2026.

| Check | Result | Evidence / coverage |
|---|---|---|
| Java reactor compilation and verification | PASS | 23 tests across shared support, gateway, BFF and Flight API; no failures/errors |
| JWT verification | PASS | Real RSA signature/JWKS; wrong issuer, audience and expired token rejected |
| Browser security boundaries | PASS | JSON 401, PKCE S256, CSRF, administrator denial, server-side encrypted client state and stale-owner rejection |
| Flight authorization/concurrency | PASS | T1/T2 list/object scope, forbidden writes, ETag stale-write checks, null/unknown mutation fields, revocation outage |
| Native integration with Keycloak 26.3.5 and Redis-compatible store | PASS | Generated realm and client credentials; ATC anonymous401, T1 read200, T2 denial403, concealed object404, write denial403 |
| Real OIDC browser-cookie protocol | PASS | Both sample users; login callback, token relay, CSRF mutation, workflow permission denial, Keycloak logout |
| WebSocket protocol | PASS | Valid session connection, rejected foreign origin at gateway, session logout closes connection |
| Chromium UI end-to-end | PASS | Two Playwright tests: remote rendering, scoped flight views, mutation, read-only UI, workflow denial and logout; no page errors |
| Frontend builds | PASS | Shell, Flight remote and Workflow remote built independently; browser-incompatible process globals removed |
| npm audit | PASS | Zero reported vulnerabilities for the pinned frontend packages after Vite upgrade to 7.3.7 |
| Helm lint and rendering | PASS | Chart lint/render; no live cluster applied |
| Static contracts | PASS | XML/JSON/YAML parsing, shell syntax, build layout and git whitespace checks |

## Runtime boundaries not certified here

- Docker Engine and a Kubernetes cluster were unavailable in the implementation environment. Docker/Compose images, plain manifests and Helm resources were authored and statically checked; local engine/cluster execution is not claimed. The included CI workflow runs Compose integration when enabled in GitHub.
- The native monitoring binaries were not installed; dashboard/data-source/Collector/Tempo/Prometheus configurations are provided. Production-scale trace completeness, monitoring availability and retention are not certified.
- No Java/container CVE certification, penetration test, load test, two-BFF crash/failover exercise or DR certification is claimed.
- Flight state remains an intentional in-memory fixture. Durable business/audit persistence and multi-writer behaviour remain production gates.

## Reproduce

Use `bash scripts/build.sh`, install `scripts/requirements.txt` and run `python3 scripts/validate.py`. With local Keycloak and session-store binaries configured, run `bash scripts/verify-native.sh`. Install Chromium with `(cd ui && npx playwright install chromium --only-shell)`, then run `E2E_BROWSER=1 bash scripts/verify-native.sh` for the UI tests. Full instructions and generated sample credentials are in the separate infrastructure and Keycloak guides.
