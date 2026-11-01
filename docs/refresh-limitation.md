# Distributed refresh: guarantees and limitations

The repository uses a 25-second per-session lease, 90-second refresh skew, and atomic Redis owner-check plus token-state save. Refresh results from a worker whose lease expired are rejected. Redis state is loaded afresh under the lease, not from a stale HTTP-session snapshot.

Keycloak refresh and Redis persistence cannot commit atomically. If a worker crashes after rotating a refresh token but before saving the new token, login is required again. Fencing protects correctness, not seamless availability. Lease loss must not cause stale-token relay. Coordinate concurrent requests; do not retry invalid_grant with the old token.

Production verification must include two BFF replicas, rotation enabled, lease expiry, process crash between refresh/save, Redis failover, network delay and log inspection for secret leakage. Test evidence is recorded separately; the implementation is not a claim that those distributed failure tests have passed.
