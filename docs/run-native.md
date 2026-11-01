# Run without Docker or Kubernetes

## Prerequisites

Java 17+, Maven 3.9+, Node 22.12+ (or another Vite-supported release), npm, Python 3, Valkey/Redis-compatible server and Keycloak 26.3.5 distribution. Verify `java -version`, `mvn -v`, `node -v`, `valkey-server --version`. Configure KEYCLOAK_HOME to an extracted official Keycloak distribution; do not point at a production installation. If using Redis locally set REDIS_BIN=redis-server. Use a distribution permitted by your licence policy.

Ports: UI 8088, gateway 8080, BFF 8081, Flight 8091, Workflow 8092, Keycloak 8085, session store 6379, remotes 8090/8093, management 9091-9094. Do not run another deployment mode concurrently.

## Build and start

1. Enter repository root. Run `python3 scripts/configure.py` once.
2. Run `bash scripts/build.sh`. Expected: Java tests pass and all three Vite builds complete. If Maven is at a non-default path, export MVN=/path/to/mvn.
3. Export `KEYCLOAK_HOME=/absolute/path/keycloak-26.3.5` and optionally `REDIS_BIN=redis-server`.
4. Run `bash scripts/native-up.sh`. It imports the realm only when absent and starts processes. Logs/PIDs are under ignored .runtime.
5. Wait for identity startup, then run `python3 scripts/atc-simulate.py` until all PASS checks succeed. A failed assertion is a failure to investigate, not a step to ignore.
6. Open http://localhost:8088; follow demo.md with generated DEMO_PASSWORD.
7. Check `curl -fsS http://localhost:9093/actuator/health/readiness`; expected status UP. Health aggregate can be DOWN if required state is unavailable.
8. Inspect `.runtime/logs/gateway.log`, `bff.log`, `flight-service.log` and `workflow-service.log` for request/trace correlation.
9. Run `bash scripts/native-down.sh`. It terminates PIDs recorded by this runner; state is preserved.

If an application log shows an unavailable identity/store, confirm dependency readiness and restart using native-down/up. Do not regenerate credentials to solve a transient startup issue. For a native monitoring backend, install/run the same official Prometheus, Grafana, Tempo and Collector binaries using the supplied configs, replacing container DNS with localhost and service metrics ports with 9091-9094. See monitoring.md. The native runner itself starts application dependencies, not these optional operator binaries.

## Automated local acceptance
After a successful build, run `bash scripts/verify-native.sh` with KEYCLOAK_HOME/REDIS_BIN configured. It starts the stack, waits for readiness, exercises real ATC and browser-cookie/OIDC/CSRF/WebSocket flows, then stops its own process groups even when a test fails. This is HTTP/protocol verification; it does not execute React JavaScript.

For real browser execution, run `(cd ui && npx playwright install chromium --only-shell)` once (Linux may also require `--with-deps`), then `E2E_BROWSER=1 bash scripts/verify-native.sh`. This verifies rendered remotes, mutations, read-only behaviour and logout in Chromium. Screenshots remain in ignored .runtime for local review.
