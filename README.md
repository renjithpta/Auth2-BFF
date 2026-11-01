# AOBP — secure integration reference

An executable reference for independently deployed React microfrontends, a replaceable gateway, session-based BFF, Keycloak and independently secured Flight/Workflow APIs.

## Start here

1. Read [architecture and security boundaries](docs/architecture.md).
2. Choose **one** execution mode; do not run conflicting modes on the same ports.
   - [Docker Compose](docs/run-compose.md): simplest full-stack demonstration, including Grafana/Tempo.
   - [Docker CLI only](docs/run-docker.md): explicit containers/network lifecycle.
   - [Native processes](docs/run-native.md): no Docker or Kubernetes.
   - [Kubernetes plain manifests](docs/run-kubernetes.md).
   - [Kubernetes Helm](docs/run-helm.md).
3. Follow [Keycloak setup/onboarding](docs/keycloak.md).
4. Present [browser and ATC demonstrations](docs/demo.md).
5. Use [API/error/log contracts](docs/api-contracts.md) and [monitoring](docs/monitoring.md).
6. Review [verification evidence](docs/verification.md) and [production gates](docs/production-checklist.md).

## Implemented paths

| Caller           | Path                                           | Credential                                |
| ---------------- | ---------------------------------------------- | ----------------------------------------- |
| Browser          | UI entry → gateway → BFF → Flight/Workflow API | Cookie to BFF; access token to API        |
| ATC              | Gateway → Flight API                           | Client Credentials bearer token           |
| Browser realtime | UI entry → gateway → BFF WebSocket             | Existing session; no browser access token |

UI images are independent from the gateway. The shell runtime-loads flight/workflow remotes from a same-origin manifest. The gateway exposes no arbitrary catch-all proxy. Domain APIs validate issuer, signature, expiry and their own audience, then enforce roles and terminal scope.

## Reference boundaries

Flight data and workflow tasks are demonstration fixtures. Flight updates use process-local optimistic concurrency; they are **not a persistent or multi-replica AODB implementation**. Native/Compose/Kubernetes examples are local development deployments. They use HTTP and disposable credentials, require no production data, and are not approved production configurations. Production TLS, durable business/audit storage, HA/DR, dependency/security scans and load testing remain release gates.

No access/refresh tokens are returned to React. Generated credentials live only in ignored `.env` / `.runtime`. Do not commit them or expose the local identity admin console to the network.
