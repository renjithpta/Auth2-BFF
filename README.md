# Auth2-BFF — Distributed authentication reference

This branch introduces the architecture and deployment contract for a React SPA, Spring Boot BFF, Spring Cloud Gateway, Redis and downstream OAuth2 resource servers.

## Modules
- `ui`: React SPA; no OAuth tokens stored in the browser.
- `gateway`: ingress routing, throttling and trusted proxy boundaries.
- `bff`: OIDC confidential client, HttpOnly session cookie, CSRF, server-side token management.
- `flight-service`, `workflow-service`: resource servers; validate issuer, audience and permissions.
- `infra`: local Keycloak/Redis and Kubernetes deployment assets.

## Status
Architecture and deployment scaffolding only on this branch. **Not a production-ready implementation.** Do not deploy until the complete source, build verification, integration tests, secret management, and security review have been completed.

## Token handling
Never expose access or refresh tokens to React. Store session and authorized client server-side. On logout, invalidate the session, perform OIDC RP-initiated logout, and denylist issued tokens until their expiration. Downstream services must fail closed on unavailable revocation checks.

See `docs/architecture.md` and `docs/production-checklist.md`.
