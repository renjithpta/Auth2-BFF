# Production release gates — not a certification

## Implemented reference controls

- Browser BFF tokens are server-side and AES-GCM encrypted at rest in shared authorized-client storage.
- PKCE, state/nonce handling, CSRF, session cookie and explicit login/logout boundaries.
- Distinct ATC client and gateway bearer routes; independent resource-server validation.
- Role and terminal authorization, scoped list/detail, typed mutation and optimistic concurrency.
- Redis token/subject revocation checks fail closed; refresh saves are owner-fenced.
- Native WebSocket origin/session enforcement and authorized REST refresh hints.
- Structured logs, server traces, metrics and a provisioned local dashboard.

## Must be completed before production

- [ ] Supported/patched dependency/image baseline, SBOM, licence review and security scans; pin promoted image digests.
- [ ] Replace development HTTP with TLS, controlled public DNS, secure cookies and strict trusted-proxy/network policy.
- [ ] Corporate IdP federation/MFA, account lifecycle, privilege approval and deprovisioning latency tested.
- [ ] Secret manager, encryption-key rotation, certificate rotation, backups and privileged access control.
- [ ] Persistent Flight/workflow business storage, multi-writer transactions, migrations and durable audit/outbox.
- [ ] Shared-store/identity HA, bounded Redis timeouts, disaster recovery and failback exercise.
- [ ] Concurrent refresh/lease-loss/crash/Redis-failover tests with two BFF replicas.
- [ ] Rate limits, resource limits, payload protections, API penetration tests and dependency failure/load tests.
- [ ] Browser CSP/remote integrity, immutable MFE releases, accessibility and compatibility review.
- [ ] Central logs, browser telemetry, SLO alerts, data retention, protected monitoring and operational runbooks.
- [ ] Production Kubernetes topology spread, PDB/HPA (only for stateless components), probes, network policy and persistent services.

Do not scale the in-memory Flight service and claim shared state or durable audit. Do not expose demo identity admin/network ports. Delivery evidence is recorded in verification.md; unchecked requirements remain explicit.
