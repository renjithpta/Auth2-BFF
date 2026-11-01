#!/usr/bin/env python3
"""Run application/data containers using Docker CLI only (same images as Compose)."""
import json, subprocess, sys, re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
env = dict(
    line.split("=", 1)
    for line in (root / ".env").read_text().splitlines()
    if "=" in line
)
cfg = json.loads((root / "infra/docker/compose.yml").read_text())
names = [
    "redis",
    "keycloak",
    "flight-service",
    "workflow-service",
    "bff",
    "gateway",
    "shell",
    "flight-mfe",
    "workflow-mfe",
    "edge",
    "tempo",
    "otel",
    "prometheus",
    "grafana",
]


def run(*args, check=True):
    return subprocess.run(["docker", *args], check=check)


if sys.argv[1] == "down":
    for name in reversed(names):
        run("rm", "-f", "aobp-" + name, check=False)
    sys.exit(0)
run("network", "inspect", "aobp-demo", check=False)
if subprocess.run(
    ["docker", "network", "inspect", "aobp-demo"], capture_output=True
).returncode:
    run("network", "create", "aobp-demo")


def expand(s):
    return re.sub(r"\$\{([A-Z_]+)(?::[-?][^}]*)?\}", lambda m: env.get(m[1], ""), s)


for name in names:
    s = cfg["services"][name]
    if "build" in s:
        b = s["build"]
        args = ["build", "-f", str(root / b["dockerfile"]), "-t", s["image"]]
        for k, v in b.get("args", {}).items():
            args += ["--build-arg", k + "=" + v]
        run(*args, str(root))
    args = [
        "run",
        "-d",
        "--name",
        "aobp-" + name,
        "--network",
        "aobp-demo",
        "--network-alias",
        name,
    ]
    for k, v in s.get("environment", {}).items():
        args += ["-e", k + "=" + expand(str(v))]
    for v in s.get("volumes", []):
        bits = v.split(":")
        source = bits[0]
        source = (
            str((root / "infra/docker" / source).resolve())
            if source.startswith(".")
            else "aobp-" + source
        )
        args += ["-v", ":".join([source, *bits[1:]])]
    for port in s.get("ports", []):
        args += ["-p", port]
    run(*args, s["image"], *s.get("command", []))
print(
    "Containers started. Keycloak can need 30-60 seconds. App containers may require docker restart aobp-bff after identity is ready. Verify with scripts/atc-simulate.py."
)
