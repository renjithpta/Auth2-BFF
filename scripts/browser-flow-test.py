#!/usr/bin/env python3
"""HTTP-level browser-cookie/OIDC acceptance test; does not execute React JavaScript."""
import http.cookiejar
import socket
import base64
import os
import time
import json
import urllib.request
import urllib.parse
import urllib.error
from html.parser import HTMLParser
from pathlib import Path

root = Path(__file__).resolve().parents[1]
env = dict(
    line.split("=", 1)
    for line in (root / ".env").read_text().splitlines()
    if "=" in line
)
origin = env["PUBLIC_ORIGIN"]


class SafeRedirects(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        parsed = urllib.parse.urlparse(newurl)
        print("Redirect:", parsed.netloc, parsed.path, flush=True)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


class Form(HTMLParser):
    action = None

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "form" and attrs.get("id") == "kc-form-login":
            self.action = attrs["action"]


class LoopbackCookiePolicy(http.cookiejar.DefaultCookiePolicy):
    # Browsers treat localhost as potentially trustworthy and accept local Secure cookies.
    # Match that behaviour only for this local HTTP acceptance test; never for remote hosts.
    def return_ok_secure(self, cookie, request):
        if urllib.parse.urlparse(request.full_url).hostname == "localhost":
            return True
        return super().return_ok_secure(cookie, request)


def login(user):
    cookies = http.cookiejar.CookieJar(LoopbackCookiePolicy())
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies), SafeRedirects())
    with opener.open(origin + "/oauth2/authorization/aobp", timeout=10) as r:
        form = Form()
        form.feed(r.read().decode())
    assert form.action, "Identity login form not found"
    body = urllib.parse.urlencode(
        {"username": user, "password": env["DEMO_PASSWORD"], "credentialId": ""}
    ).encode()
    try:
        with opener.open(form.action, body, timeout=15) as r:
            assert r.status == 200, "Login callback did not return to UI"
    except urllib.error.HTTPError as error:
        print(
            "Login failed at:",
            urllib.parse.urlparse(error.url).netloc,
            urllib.parse.urlparse(error.url).path,
        )
        raise AssertionError(
            "Identity form submission failed with HTTP " + str(error.code)
        ) from None
    assert any(
        c.name == "AOBPSESSION" and c.has_nonstandard_attr("HttpOnly") for c in cookies
    ), "Missing HttpOnly session"
    opener.aobp_cookies = cookies
    return opener


def call(opener, path, method="GET", body=None, headers=None):
    data = None if body is None else json.dumps(body).encode()
    try:
        with opener.open(
            urllib.request.Request(
                origin + path, data=data, method=method, headers=headers or {}
            ),
            timeout=10,
        ) as r:
            return r.status, r.headers, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers, e.read()


def websocket(opener, allowed_origin, expected):
    host = urllib.parse.urlparse(origin)
    assert (
        host.hostname == "localhost" and host.scheme == "http"
    ), "Raw handshake test is local-only"
    # Vite's development WS proxy does not relay rejected upgrade HTTP bodies.
    # Assert the gateway rejection directly; exercise accepted upgrades through the UI entry.
    port = host.port if expected == 101 else 8080
    connection = socket.create_connection(("localhost", port), timeout=10)
    cookie = "; ".join(
        c.name + "=" + c.value for c in opener.aobp_cookies if c.name == "AOBPSESSION"
    )
    key = base64.b64encode(os.urandom(16)).decode()
    request = f"GET /bff/ws/v1 HTTP/1.1\r\nHost: {host.netloc}\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\nOrigin: {allowed_origin}\r\nCookie: {cookie}\r\n\r\n"
    connection.sendall(request.encode())
    data = b""
    while b"\r\n\r\n" not in data:
        data += connection.recv(4096)
    actual = data.split(b"\r\n", 1)[0].split()[1].decode()
    assert actual == str(expected), (
        "WebSocket handshake status mismatch",
        actual,
        expected,
    )
    if expected != 101:
        connection.close()
        return None
    return connection


for user, terminal in [("ops-t1", "T1"), ("ops-t2", "T2")]:
    opener = login(user)
    status, headers, body = call(opener, "/bff/api/v1/session")
    assert status == 200 and json.loads(body)["data"]["name"] == user, (
        user,
        "session",
        status,
    )
    status, headers, body = call(opener, "/bff/api/v1/flights")
    assert status == 200, (user, "flights", status, body.decode())
    rows = json.loads(body)["data"]
    assert len(rows) == 1 and rows[0]["terminalId"] == terminal
    print("PASS browser login, HttpOnly session and scoped token relay", user)
    websocket(opener, "http://untrusted.example", 403)
    ws = websocket(opener, origin, 101)
    print("PASS authenticated WebSocket and origin rejection", user)
    if user == "ops-t1":
        status, headers, _ = call(opener, "/bff/api/v1/flights/flight-001")
        etag = headers["ETag"]
        assert status == 200
        status, _, _ = call(
            opener,
            "/bff/api/v1/flights/flight-001",
            "PATCH",
            {"status": "BOARDING"},
            {"Content-Type": "application/json", "If-Match": etag},
        )
        assert status == 403, ("CSRF missing", status)
        _, _, body = call(opener, "/bff/api/v1/csrf")
        csrf = json.loads(body)
        status, _, _ = call(
            opener,
            "/bff/api/v1/flights/flight-001",
            "PATCH",
            {"status": "BOARDING"},
            {
                "Content-Type": "application/json",
                "If-Match": etag,
                csrf["headerName"]: csrf["token"],
            },
        )
        assert status == 200, ("authorized update", status)
        print("PASS CSRF enforcement and conditional mutation")
    else:
        status, _, _ = call(opener, "/bff/api/v1/workflows")
        assert status == 403
        print("PASS workflow permission denied")
    _, _, body = call(opener, "/bff/api/v1/csrf")
    csrf = json.loads(body)
    # Avoid displaying redirect URLs containing id_token_hint.
    request = urllib.request.Request(
        origin + "/logout",
        data=urllib.parse.urlencode({"_csrf": csrf["token"]}).encode(),
        method="POST",
    )
    with opener.open(request, timeout=15) as r:
        assert r.status == 200
    status, _, _ = call(opener, "/bff/api/v1/session")
    assert status == 401
    deadline = time.time() + 12
    closed = False
    while time.time() < deadline:
        frame = ws.recv(4096)
        if not frame or (frame[0] & 0x0F) == 8:
            closed = True
            break
    ws.close()
    assert closed, "WebSocket remained open after logout"
    print("PASS logout invalidates session and closes WebSocket", user)
