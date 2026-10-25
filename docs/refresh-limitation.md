# Distributed refresh and user-blocking limitations

The Redis refresh lock in DistributedTokenCoordinator prevents most concurrent refresh calls, but does **not** implement fencing. If a refresh exceeds the 25-second lock TTL, another replica can refresh concurrently and stale state can overwrite a rotated refresh token. Do not deploy without fencing/versioned authorized-client writes and concurrency tests.

The administrative account-block endpoint writes a Redis key, but the downstream JWT filters must check this key against the authenticated token subject to enforce user-wide blocking. Method-level security must also be enabled for @PreAuthorize to take effect. These are release blockers.

Validate the Keycloak issuer URL, trusted proxy headers, cookie scope, OIDC logout, Redis TLS and session serialization. Never commit real credentials.
