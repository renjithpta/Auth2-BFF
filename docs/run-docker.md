# Run with Docker CLI (without Compose)

1. Install Docker Engine/Desktop and Python 3. Verify `docker version`.
2. Enter repository root; run `python3 scripts/configure.py` if .env does not exist.
3. Run `bash scripts/docker-run.sh up`. It builds application/UI images and creates the aobp-demo network, named containers and required state volumes.
4. Wait for Keycloak: `docker logs aobp-keycloak` should show startup and realm import. Do not expose these ports beyond localhost.
5. If BFF started before identity was ready, run `docker restart aobp-bff`. Inspect `docker logs aobp-bff`.
6. Run `python3 scripts/atc-simulate.py`; expect five PASS lines.
7. Open http://localhost:8088 and execute docs/demo.md.
8. Inspect a request using `docker logs aobp-gateway`, `docker logs aobp-bff`, `docker logs aobp-flight-service` and request/trace IDs.
9. Stop only these containers: `bash scripts/docker-run.sh down`. Named state volumes are retained.

The Docker-only runner also starts the provisioned Prometheus/Grafana/Tempo/Collector services. Open Grafana at localhost:3000 and follow monitoring.md. The runner refuses duplicate names through normal Docker errors; run down before recreating containers. It does not silently delete existing state or unrelated containers.
