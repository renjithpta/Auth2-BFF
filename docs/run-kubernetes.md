# Run on Kubernetes using plain YAML

## Local demo cluster

Use a local cluster such as kind/minikube or Docker Desktop Kubernetes, kubectl and Docker for image builds. These manifests use one namespace (aobp-demo), one replica and ephemeral identity/session state: restarting a stateful pod can lose demo state. They do not implement HA, production ingress, database persistence or DR.

1. Generate credentials once with `python3 scripts/configure.py`.
2. Build images using `bash scripts/compose.sh build`. This builds only; it does not require running the Compose services.
3. Load these images into your local cluster or push to an accessible registry: aobp/gateway:demo, aobp/bff:demo, aobp/flight-service:demo, aobp/workflow-service:demo, aobp/shell:demo, aobp/flight-mfe:demo, aobp/workflow-mfe:demo. For kind, run `kind load docker-image <image>` for each; for minikube use its image load command. Registry users must update image references before apply.
4. Verify context with `kubectl config current-context`; use a disposable cluster.
5. Run `bash scripts/k8s.sh plain up`. It creates separate runtime and realm Secrets, applies the plain manifests and waits for deployments.
6. Verify `kubectl get pods,svc,configmap,secret -n aobp-demo`. All application pods should become Ready. Never print secret contents during the presentation.
7. In separate terminals run:

```bash
kubectl port-forward -n aobp-demo svc/edge 8088:8080
kubectl port-forward -n aobp-demo svc/keycloak 8085:8080
kubectl port-forward -n aobp-demo svc/gateway 8080:8080
```

8. Open http://localhost:8088. The identity URL/callback intentionally use these stable local port-forwards. BFF server-side token/JWK calls use internal Keycloak DNS, with the same public issuer validation.
9. Run `python3 scripts/atc-simulate.py`; expect all PASS checks.
10. Run `kubectl logs -n aobp-demo deployment/flight-service --tail=50` and inspect correlation. Management scraping is internal; temporary metrics access: `kubectl port-forward -n aobp-demo svc/flight-service 9093:9090`.
11. For dashboards run `kubectl port-forward -n aobp-demo svc/grafana 3000:3000` and optionally `kubectl port-forward -n aobp-demo svc/prometheus 9090:9090`. Open Grafana and follow monitoring.md. Monitoring state is ephemeral in this demo.
12. Stop port-forwards with Ctrl-C. Run `bash scripts/k8s.sh plain down` only when you intend to remove this disposable namespace and its contents.

The public URLs/certificates must be redesigned together for a shared cluster. Do not set replicas>1 for the in-memory Flight writer. See production-checklist.md.
