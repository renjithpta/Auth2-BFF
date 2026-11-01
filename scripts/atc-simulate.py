#!/usr/bin/env python3
"""Machine-to-machine integration smoke tests. Tokens never printed."""
import json, os, base64, urllib.request, urllib.error
from pathlib import Path

root = Path(__file__).resolve().parents[1]
env = dict(
    line.split("=", 1)
    for line in (root / ".env").read_text().splitlines()
    if "=" in line and not line.startswith("#")
)
env.update(os.environ)
identity = env.get("IDENTITY_ORIGIN", "http://localhost:8085")
api = env.get("API_ORIGIN", "http://localhost:8080")


def call(method, url, headers=None, body=None):
    try:
        with urllib.request.urlopen(
            urllib.request.Request(
                url, data=body, headers=headers or {}, method=method
            ),
            timeout=10,
        ) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


basic = base64.b64encode(("atc-prod:" + env["ATC_CLIENT_SECRET"]).encode()).decode()
status, body = call(
    "POST",
    identity + "/realms/aobp/protocol/openid-connect/token",
    {
        "Authorization": "Basic " + basic,
        "Content-Type": "application/x-www-form-urlencoded",
    },
    b"grant_type=client_credentials",
)
assert status == 200, ("Token request failed", status)
token = json.loads(body)["access_token"]
headers = {"Authorization": "Bearer " + token, "Accept": "application/json"}
cases = [
    ("anonymous", {}, "/api/v1/flights", 401),
    ("read T1", headers, "/api/v1/flights?terminalId=T1", 200),
    ("deny T2", headers, "/api/v1/flights?terminalId=T2", 403),
    ("conceal T2 object", headers, "/api/v1/flights/flight-002", 404),
]
for name, h, path, expected in cases:
    status, body = call("GET", api + path, h)
    assert status == expected, (name, status, body.decode())
    print("PASS", name, status)
    if name == "read T1":
        assert all(f["terminalId"] == "T1" for f in json.loads(body)["data"])
status, body = call(
    "PATCH",
    api + "/api/v1/flights/flight-001",
    {**headers, "Content-Type": "application/json", "If-Match": '"1"'},
    b'{"status":"BOARDING"}',
)
assert status == 403, ("ATC must be read-only", status)
print("PASS deny ATC mutation", status)
