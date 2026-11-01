# Run on Kubernetes using Helm

1. Complete the prerequisites, image build/load and disposable-cluster checks in run-kubernetes.md, steps 1-4.
2. Install Helm 3 and verify `helm version`.
3. Run `helm lint infra/k8s/helm/aobp`.
4. Render before install: `helm template aobp infra/k8s/helm/aobp --namespace aobp-demo > .runtime/helm-rendered.yaml`. Inspect image names, configuration, mounts and service ports. Rendered resources do not contain generated Secret values; Secrets are created separately.
5. Run `bash scripts/k8s.sh helm up`. The runner provisions Secrets and installs chart into aobp-demo.
6. Run `helm status aobp -n aobp-demo`; expect deployed. Confirm pods Ready.
7. Start the three port-forwards in run-kubernetes.md and run the same browser/ATC demonstrations.
8. For registry images, set the chart images map using an environment values file; e.g. `images: {gateway: registry.example.com/aobp/gateway:<approved-tag>}`. Use immutable digests for production release promotion.
9. To inspect history: `helm history aobp -n aobp-demo`. A Helm rollback does not roll back database state or Keycloak users.
10. Stop: `bash scripts/k8s.sh helm down`. Secrets/namespace created by the runner remain for deliberate cleanup; do not assume Helm owns external secrets.

Use Helm OR plain YAML in a namespace, not both. The chart has the same demo constraints as the plain manifests. Independent production charts/releases are a future platform packaging step; this umbrella reference does not claim full HA or independently governed production rollout.
