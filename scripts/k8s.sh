#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mode=${1:-plain}; action=${2:-up}
ns=aobp-demo
if [[ "$action" == down ]]; then
 if [[ "$mode" == helm ]]; then helm uninstall aobp -n "$ns"; else kubectl delete -f infra/k8s/plain --ignore-not-found; fi
 exit
fi
test -f .env || python3 scripts/configure.py
kubectl create namespace "$ns" --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic aobp-runtime -n "$ns" --from-env-file=.env --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic aobp-realm -n "$ns" --from-file=aobp-realm.json=.runtime/aobp-realm.json --dry-run=client -o yaml | kubectl apply -f -
if [[ "$mode" == helm ]]; then helm upgrade --install aobp infra/k8s/helm/aobp -n "$ns" --wait --timeout 10m; else kubectl apply -f infra/k8s/plain; fi
for app in keycloak redis gateway bff flight-service workflow-service shell flight-mfe workflow-mfe edge tempo otel prometheus grafana; do kubectl rollout status "deployment/$app" -n "$ns" --timeout=300s; done
echo 'Now run the three port-forwards in docs/run-kubernetes.md.'
