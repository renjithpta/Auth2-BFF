# Team presentation: verified scenarios and expected results

## Prepare

1. Choose one infrastructure guide and complete it; keep UI 8088, gateway 8080 and Keycloak 8085.
2. Keep the architecture diagram, two browser profiles/incognito windows, a terminal and Grafana (if started) available.
3. Read generated DEMO_PASSWORD locally. Never display .env, token JSON or client secret to the audience.
4. Run `python3 scripts/atc-simulate.py`; show only its PASS output.

## A. Browser login and token boundary

1. Open localhost:8088 signed out. Explain: shell/remotes are separate deployments; downloading them grants no data access.
2. In browser Network tab observe /bff/api/v1/session → 401 JSON.
3. Select Sign in with Keycloak. Show authorize redirect includes code_challenge_method=S256, without revealing credentials.
4. Sign in as ops-t1 using generated password. Callback returns to UI.
5. Expect one flight SN1234 / T1. Browser business requests carry only an HttpOnly session cookie; do not export/copy it. There is no access/refresh token in localStorage.
6. Show request response X-Request-ID; use it to locate backend logs. Tracing joins gateway → BFF → Flight API when telemetry backend is running.

## B. Authorization and mutation

1. Select Set boarding. Expected flight status BOARDING and version increases.
2. Observe GET detail with ETag, CSRF token acquisition and PATCH with If-Match/CSRF header.
3. Sign into a second browser as ops-t2. Expect only BA456 / T2, no update action.
4. Select Workflow. Expect a readable 403, not a login loop. UI hiding is convenience; backend enforces the permission.
5. Demonstrate cross-terminal/object denial with ATC simulation. Do not bypass UI to expose credentials.

## C. Independent microfrontends

1. Show separate shell, flight-mfe and workflow-mfe containers/deployments.
2. Show mfe-manifest.json and mount/unmount contract.
3. Rebuild/redeploy only flight-mfe using your infrastructure guide; shell/gateway/BFF remain unchanged.
4. Reload UI. In production, immutable asset versions and compatibility gates are required; demo stable paths simplify local execution.

## D. Machine ATC integration

1. Run `python3 scripts/atc-simulate.py`.
2. Explain token acquisition occurs at Keycloak, business request at gateway, backend scope at Flight API.
3. Show PASS for anonymous401, T1 read200, foreign terminal403, concealed object404 and forbidden mutation403.
4. Explain there is no browser, BFF cookie or redirect in this flow.

## E. Realtime and logout

1. Network → WebSocket /bff/ws/v1 should show 101 and CONNECTED/REFRESH_HINT messages.
2. Explain hints cause authorized REST refetch; no unrestricted flight payload is broadcast.
3. Sign out. Session invalidates and sockets close; requests require a new login. Check no credentials appear in logs.
4. Session/blocked-user checks run every five seconds. Distributed logout/refresh fault injection remains a separate production gate.

## F. Monitoring

1. Open Grafana localhost:3000 using admin / generated KC_ADMIN_PASSWORD.
2. Open AOBP dashboard, generate reads and an update, wait two scrape intervals.
3. Show throughput, errors, p95, heap and update counters.
4. Explore → Tempo → find traces by service name; select a Flight request and identify gateway/BFF/API spans. Inspect local structured logs by request/trace ID. Browser JS tracing and central log aggregation are not claimed as implemented. The Chromium acceptance tests do verify the rendered shell/remotes and capture uncaught page errors.
5. Present verification.md to distinguish actual tests from future HA/DR/load/security certification.
