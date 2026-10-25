# Production release gates

- [ ] Compile all Java modules and build the React frontend
- [ ] Keycloak authorization code + PKCE S256 tested
- [ ] Shared Redis sessions tested across 2+ BFF replicas
- [ ] Concurrent refresh with rotating refresh tokens and fencing tested
- [ ] CSRF-protected logout and OIDC logout tested
- [ ] Revoked JWT rejected by every downstream resource server
- [ ] User-wide block invalidates all active sessions and tokens
- [ ] Redis outage behavior is fail closed and observable
- [ ] Issuer, audience, expiry, roles and ABAC tests pass
- [ ] Trusted proxy and forwarded header spoofing tested
- [ ] TLS/mTLS, secrets and cookie security reviewed
- [ ] Gateway request limits, rate limits and CORS reviewed
- [ ] K8s probes, HPA, PDB, network policies and graceful shutdown tested
- [ ] Dependency/SAST/container scans and integration tests pass
- [ ] Load, DR and rollback tests completed

This branch is not approved for production until these gates are complete.
