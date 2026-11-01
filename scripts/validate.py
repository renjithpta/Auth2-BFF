#!/usr/bin/env python3
"""Repository contract checks (install scripts/requirements.txt); complements Maven/browser/runtime tests."""
import json, xml.etree.ElementTree as ET, subprocess
import yaml
from pathlib import Path

root = Path(__file__).resolve().parents[1]
for p in root.rglob("pom.xml"):
    if "node_modules" not in p.parts:
        ET.parse(p)
for p in (root / "infra/k8s/plain").glob("*.yaml"):
    yaml.safe_load(p.read_text())
for p in [
    root / "infra/docker/compose.yml",
    root / "ui/public/mfe-manifest.json",
    root / "infra/observability/grafana/dashboards/aobp.json",
]:
    json.loads(p.read_text())
for p in (root / "scripts").glob("*.sh"):
    subprocess.run(["bash", "-n", str(p)], check=True)
assert not any(p.name == ".env" for p in root.glob("infra/**/.env"))
print("PASS XML, JSON/YAML manifests, shell syntax and layout contracts")
