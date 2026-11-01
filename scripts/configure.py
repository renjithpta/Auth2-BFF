#!/usr/bin/env python3
"""Generate disposable demonstration credentials; never use them in production."""
import json, secrets, base64, os
from pathlib import Path

root = Path(__file__).resolve().parents[1]
runtime = root / ".runtime"
runtime.mkdir(exist_ok=True)
os.chmod(runtime, 0o700)
env = root / ".env"
if env.exists():
    raise SystemExit(
        ".env already exists. Reuse it; remove it only to deliberately replace demo identities."
    )
origin = os.environ.get("PUBLIC_ORIGIN", "http://localhost:8088")
identity = os.environ.get("IDENTITY_ORIGIN", "http://localhost:8085")
credentials = {
    k: secrets.token_urlsafe(24)
    for k in [
        "OIDC_CLIENT_SECRET",
        "ATC_CLIENT_SECRET",
        "KC_ADMIN_PASSWORD",
        "DEMO_PASSWORD",
    ]
}
credentials.update(
    TOKEN_ENCRYPTION_KEY=base64.b64encode(secrets.token_bytes(32)).decode(),
    PUBLIC_ORIGIN=origin,
    IDENTITY_ORIGIN=identity,
    COOKIE_SECURE="false",
)
env.write_text("\n".join(f"{k}={v}" for k, v in credentials.items()) + "\n")
os.chmod(env, 0o600)
roles = ["FLIGHT_VIEW", "FLIGHT_UPDATE", "WORKFLOW_VIEW", "AOBP_ADMIN"]


def mapper(name, kind, config):
    return dict(
        name=name,
        protocol="openid-connect",
        protocolMapper=kind,
        consentRequired=False,
        config=config,
    )


def audience(a):
    return mapper(
        a,
        "oidc-audience-mapper",
        {
            "included.custom.audience": a,
            "access.token.claim": "true",
            "id.token.claim": "false",
        },
    )


terminals = mapper(
    "terminals",
    "oidc-usermodel-attribute-mapper",
    {
        "user.attribute": "terminals",
        "claim.name": "terminals",
        "jsonType.label": "String",
        "multivalued": "true",
        "access.token.claim": "true",
        "id.token.claim": "true",
    },
)
realmroles = mapper(
    "roles",
    "oidc-usermodel-realm-role-mapper",
    {
        "claim.name": "realm_access.roles",
        "jsonType.label": "String",
        "multivalued": "true",
        "access.token.claim": "true",
        "id.token.claim": "true",
    },
)
clients = [
    dict(
        clientId="aobp-bff",
        enabled=True,
        protocol="openid-connect",
        publicClient=False,
        secret=credentials["OIDC_CLIENT_SECRET"],
        standardFlowEnabled=True,
        directAccessGrantsEnabled=False,
        serviceAccountsEnabled=False,
        redirectUris=[origin + "/login/oauth2/code/aobp"],
        webOrigins=[origin],
        attributes={
            "pkce.code.challenge.method": "S256",
            "post.logout.redirect.uris": origin + "/",
        },
        protocolMappers=[
            audience("flight-information-api"),
            audience("workflow-api"),
            terminals,
            realmroles,
        ],
    ),
    dict(
        clientId="atc-prod",
        enabled=True,
        protocol="openid-connect",
        publicClient=False,
        secret=credentials["ATC_CLIENT_SECRET"],
        standardFlowEnabled=False,
        directAccessGrantsEnabled=False,
        serviceAccountsEnabled=True,
        protocolMappers=[audience("flight-information-api"), terminals, realmroles],
    ),
]
users = []
for name, terminal, assigned in [
    ("ops-t1", "T1", ["FLIGHT_VIEW", "FLIGHT_UPDATE", "WORKFLOW_VIEW"]),
    ("ops-t2", "T2", ["FLIGHT_VIEW"]),
    ("platform-admin", "T1", roles),
]:
    users.append(
        dict(
            username=name,
            enabled=True,
            firstName="Demo",
            lastName=name,
            email=name + "@example.test",
            emailVerified=True,
            attributes={"terminals": [terminal]},
            realmRoles=assigned,
            credentials=[
                dict(
                    type="password", value=credentials["DEMO_PASSWORD"], temporary=False
                )
            ],
        )
    )
users.append(
    dict(
        username="service-account-atc-prod",
        enabled=True,
        serviceAccountClientId="atc-prod",
        realmRoles=["FLIGHT_VIEW"],
        attributes={"terminals": ["T1"]},
    )
)
clients[0]["defaultClientScopes"] = ["profile", "email", "roles"]
clients[1]["defaultClientScopes"] = ["roles"]
realm = dict(
    realm="aobp",
    enabled=True,
    registrationAllowed=False,
    resetPasswordAllowed=False,
    accessTokenLifespan=300,
    ssoSessionIdleTimeout=1800,
    ssoSessionMaxLifespan=3600,
    revokeRefreshToken=True,
    refreshTokenMaxReuse=0,
    roles={"realm": [{"name": r} for r in roles]},
    clients=clients,
    users=users,
)
(runtime / "aobp-realm.json").write_text(json.dumps(realm, indent=2))
# The private 0700 parent protects host access; 0644 permits the non-root Keycloak bind mount.
os.chmod(runtime / "aobp-realm.json", 0o644)
print(
    "Generated .env and .runtime/aobp-realm.json. Read DEMO_PASSWORD from .env locally. Do not commit either file."
)
