#!/usr/bin/env python3
"""Own only the process groups started by this reference's local runner."""
import os
import signal
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
runtime = root / ".runtime"
pidfile = runtime / "native.pids"
if sys.argv[1] == "down":
    if pidfile.exists():
        for line in pidfile.read_text().splitlines():
            try:
                os.killpg(int(line), signal.SIGTERM)
            except ProcessLookupError:
                pass
        pidfile.unlink()
    sys.exit(0)
if pidfile.exists():
    raise SystemExit("Existing PID file. Run native-down.sh before starting again.")
env = os.environ.copy()
env.update(
    dict(
        line.split("=", 1)
        for line in (root / ".env").read_text().splitlines()
        if "=" in line
    )
)
kc = Path(env["KEYCLOAK_HOME"]) / "bin/kc.sh"
logs = runtime / "logs"
logs.mkdir(exist_ok=True)
env.update(
    KC_BOOTSTRAP_ADMIN_USERNAME="admin",
    KC_BOOTSTRAP_ADMIN_PASSWORD=env["KC_ADMIN_PASSWORD"],
)
with (logs / "keycloak-import.log").open("w") as log:
    subprocess.run(
        [
            str(kc),
            "import",
            "--file",
            str(runtime / "aobp-realm.json"),
            "--override",
            "false",
        ],
        env=env,
        stdout=log,
        stderr=subprocess.STDOUT,
        check=True,
    )


def start(name, args, extra=None, cwd=root):
    with (logs / (name + ".log")).open("w") as log:
        process = subprocess.Popen(
            args,
            env={**env, **(extra or {})},
            cwd=cwd,
            stdout=log,
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
    with pidfile.open("a") as f:
        f.write(str(process.pid) + "\n")


try:
    start(
        "redis",
        [
            env.get("REDIS_BIN", "valkey-server"),
            "--port",
            "6379",
            "--appendonly",
            "yes",
            "--dir",
            str(runtime),
        ],
    )
    start(
        "keycloak",
        [
            str(kc),
            "start-dev",
            "--http-port=8085",
            "--hostname=" + env["IDENTITY_ORIGIN"],
        ],
    )
    for name, port in [
        ("gateway", 9091),
        ("bff", 9092),
        ("flight-service", 9093),
        ("workflow-service", 9094),
    ]:
        start(
            name,
            [
                "java",
                "-Xmx256m",
                "-jar",
                str(root / name / "target" / (name + "-1.0.0-SNAPSHOT.jar")),
            ],
            {"MANAGEMENT_PORT": str(port)},
        )
    for name, port in [("ui-flight", 8090), ("ui-workflow", 8093)]:
        start(
            name,
            ["npm", "run", "preview", "--", "--port", str(port), "--strictPort"],
            cwd=root / name,
        )
    start("ui", ["npm", "run", "dev"], cwd=root / "ui")
except Exception:
    subprocess.run([sys.executable, __file__, "down"], check=False)
    raise
print(
    "Started local process groups. UI: http://localhost:8088. Check .runtime/logs and run ATC simulation."
)
