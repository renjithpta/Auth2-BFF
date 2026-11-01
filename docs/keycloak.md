# Keycloak setup and application onboarding

## Generate a local realm

1. From repository root run `python3 scripts/configure.py` once.
2. This creates ignored `.env` (mode 600) and `.runtime/aobp-realm.json`. The latter contains disposable client/user secrets. Do not commit or share either file.
3. Read DEMO_PASSWORD from `.env` locally. Do not paste the whole file into logs/chat.
4. Start your selected deployment. Initial Keycloak realm import runs only when the realm does not already exist.
5. Open `http://localhost:8085/admin/`; use `admin` and KC_ADMIN_PASSWORD from `.env`.
6. Select realm **aobp**. Confirm users, clients, roles and mappers below.

## Validate BFF client

Clients → aobp-bff: Client authentication ON; Standard flow ON; Direct access grants OFF; service accounts OFF. Redirect URI exactly `http://localhost:8088/login/oauth2/code/aobp`. Post logout redirect `http://localhost:8088/`. PKCE method S256. Credentials must match OIDC_CLIENT_SECRET in BFF configuration.

Protocol mappers: Flight audience `flight-information-api`, Workflow audience `workflow-api`, terminals user attribute as multivalued access-token claim, realm roles in realm_access.roles. BFF role display/admin method checks also receive controlled ID-token roles. Do not put API secrets in ID tokens. Verify the BFF's own ID token audience is aobp-bff; API access tokens have intended API audiences.

## Validate users

| Username       | Terminal attribute | Explicit roles                            | Expected                                                                     |
| -------------- | ------------------ | ----------------------------------------- | ---------------------------------------------------------------------------- |
| ops-t1         | T1                 | FLIGHT_VIEW, FLIGHT_UPDATE, WORKFLOW_VIEW | One flight; can set boarding; sees workflow                                  |
| ops-t2         | T2                 | FLIGHT_VIEW                               | One different flight; no update action; workflow 403                         |
| platform-admin | T1                 | All four demo roles                       | Can demonstrate account blocking plus explicitly granted business operations |

Users → user → Attributes: terminals must be set to the approved terminal. Missing attribute means no flight scope. In a new incognito window sign in as each user and verify scope. The sample password is generated; no fixed password is committed.

## Validate ATC

Clients → atc-prod: Client authentication ON, Service accounts ON, Standard flow OFF, Direct access grants OFF. Audience only Flight API. Service account roles include FLIGHT_VIEW, not FLIGHT_UPDATE. Service-account user terminals attribute is T1. Run `python3 scripts/atc-simulate.py`; all assertions must pass. No human password or browser session is used.

## Onboard an additional machine integration

1. Create a confidential OpenID Connect client with a distinct ID/environment.
2. Enable service accounts and disable interactive/password grants unless separately justified.
3. Configure the intended API audience mapper; do not accept a token intended only for another service.
4. Assign the minimal service-account roles and trusted terminal/airport entitlements.
5. Provision client authentication securely to the partner. Prefer supported certificate/private-key methods when practical; the demo uses a generated secret.
6. Test token acquisition, allowed read, forbidden scope, wrong audience, expiry and revoked credentials.
7. Set rate/network restrictions, owner and credential-rotation procedure before enabling production traffic.

## Corporate SSO / no local production accounts

Identity providers → add OpenID Connect or SAML provider → enter airport metadata/endpoints and securely provision credentials → map approved identity/group attributes → review first-broker-login/account-linking policy → test MFA, logout and role removal. Do not link accounts solely because unverified emails match. Disable normal local-password login for production employees after federation/recovery procedures are verified. The local users above are explicitly disposable demo accounts, not the production identity policy. Keycloak still maintains federated identity metadata.

## Changing/resetting the import

Editing an import JSON does not update an existing realm automatically. Use reviewed admin/API updates, or deliberately reset the disposable identity volume after stopping the stack. `docker compose down -v` destroys all demo volumes: use only when you intend to discard state. Never use reset instructions on a shared/production installation. Regenerating .env also invalidates existing client configuration and encrypted token state; clear stale demo state deliberately.
