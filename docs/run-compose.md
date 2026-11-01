# Run using Docker Compose

## Prerequisites

Docker Engine/Desktop with Compose v2 (`docker version`, `docker compose version`), Python 3, Git, network access for approved images/Maven/npm. Reserve ports 8088, 8080, 8085, 3000 and 9090. No local Java/Node install is needed for image builds.

## Start

1. Clone repository and enter its root.
2. Run `python3 scripts/configure.py` (skip if .env already exists).
3. Run `bash scripts/compose.sh config --quiet`. Expected: exit 0.
4. Run `bash scripts/compose.sh up -d --build`. Java and three UI images build independently; first dependency downloads take time.
5. Run `bash scripts/compose.sh ps`. Identity startup can take a minute. Inspect `bash scripts/compose.sh logs --tail=100 keycloak bff gateway` on errors; never publish complete sensitive logs.
6. Run `python3 scripts/atc-simulate.py`. Expected: PASS anonymous 401, T1 200, T2 403, concealed object 404 and read-only mutation 403.
7. Open `http://localhost:8088` and complete docs/demo.md.
8. Open `http://localhost:3000`; Grafana user admin and KC_ADMIN_PASSWORD from .env. Open AOBP folder/dashboard. See monitoring.md.

## Rebuild one frontend

Run `bash scripts/compose.sh up -d --build --no-deps flight-mfe`. Shell, BFF and gateway are not rebuilt. Refresh the page to load the updated same-origin remote. This demo uses stable remote paths; production must publish immutable versioned URLs and update the manifest with cache/release compatibility controls.

## Stop

`bash scripts/compose.sh down` preserves named volumes. Do not add -v unless deliberately deleting demo identity/session data. If ports conflict, stop the other execution mode first rather than changing only one callback port.

## Troubleshooting

- 401 token acquisition: verify realm exists and ATC secret matches imported realm; old volume may contain older credentials.
- Login callback mismatch: origin must remain 8088, callback exactly /login/oauth2/code/aobp. Do not open the UI at gateway port 8080.
- BFF 503: check shared-store connectivity. Do not disable revocation checks to hide the failure.
- Microfrontend 404: verify flight-mfe/workflow-mfe images and entry proxy paths.
- Metrics empty: generate requests, wait 20 seconds, inspect Prometheus targets at localhost:9090/targets.

HTTP, development Keycloak and single-instance state are local-only. See production-checklist.md before any external deployment.
