# Monitoring, tracing and error investigation

Compose provisions Prometheus, Grafana, OpenTelemetry Collector and Tempo. The AOBP dashboard JSON and data-source UIDs are versioned under infra/observability. Application management ports are private to the container network. Default local sampling is 100% for demonstrations; tune sampling/capacity and retention before production.

## Verify collection

1. Start Compose stack and run ATC simulation, browser login and flight reads.
2. Open localhost:9090/targets; gateway, bff, flight-service and workflow-service should be UP.
3. Open Grafana localhost:3000 → AOBP folder → AOBP requests/security/performance.
4. If p95 is empty, wait for requests and histogram buckets. Check target /actuator/prometheus. Metric labels depend on selected Spring/Micrometer version; validate the supplied queries against actual exports.
5. Explore → Tempo → search by service name. A BFF RestClient created through the observed builder propagates context; gateway instrumentation must be verified in the selected runtime.
6. Copy a trace ID and locate matching structured application logs. Never log token values to improve traceability.

## Boundary and gaps

This implementation exports server-side traces and metrics, and emits JSON logs to container/native stdout. It does not deploy a central Loki/OpenSearch collector, browser telemetry ingestion, durable audit store or production alert routing. Use docker logs/kubectl logs/native log files for diagnostics. Do not present the Grafana dashboard as a full API management console or sampled traces as complete audit evidence.

## Other execution modes

Docker-only: run official Prometheus/Grafana/Tempo/Collector containers on aobp-demo using the same read-only configuration mounts shown in infra/docker/compose.yml. Native: run their official binaries; replace service DNS targets with localhost:9091/9092/9093/9094 and tracing endpoints with localhost. Plain Kubernetes/Helm include the same ephemeral monitoring stack. Port-forward Grafana 3000 and optionally Prometheus 9090; scrapes use each application Service management port 9090. Do not expose management endpoints via application ingress.

Production monitoring must add alert owners/runbooks, failure/latency SLOs, backend retention/HA, sensitive-data redaction, protected Grafana OIDC, private data sources, collector authentication/TLS, central logs and sampling policy. The dashboard includes request rate, errors, denied operations, latency and saturation, so these controls have a clear extension point.
