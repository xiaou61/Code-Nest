#!/usr/bin/env python3
"""Validate the repository's build and frontend versions against VERSION."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read_version() -> str:
    value = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
    if not re.fullmatch(r"\d+\.\d+\.\d+", value):
        raise SystemExit(f"invalid VERSION value: {value!r}")
    return value


def read_json_version(path: Path) -> str:
    payload = json.loads(path.read_text(encoding="utf-8"))
    return str(payload.get("version", ""))


def main() -> int:
    version = read_version()
    expected_maven = f"v{version}"
    pom = (ROOT / "pom.xml").read_text(encoding="utf-8")
    match = re.search(r"<revision>\s*([^<]+?)\s*</revision>", pom)
    if not match or match.group(1).strip() != expected_maven:
        raise SystemExit(f"pom.xml revision must be {expected_maven}")

    package_paths = [
        ROOT / "vue3-user-front" / "package.json",
        ROOT / "vue3-admin-front" / "package.json",
        ROOT / "code-nest-design-system" / "package.json",
        ROOT / "docs-site" / "package.json",
    ]
    for path in package_paths:
        actual = read_json_version(path)
        if actual != version:
            raise SystemExit(f"{path.relative_to(ROOT)} version must be {version}, got {actual}")

    lock_paths = [
        ROOT / "vue3-user-front" / "package-lock.json",
        ROOT / "vue3-admin-front" / "package-lock.json",
        ROOT / "docs-site" / "package-lock.json",
    ]
    for path in lock_paths:
        payload = json.loads(path.read_text(encoding="utf-8"))
        actual = str(payload.get("packages", {}).get("", {}).get("version", ""))
        if actual != version:
            raise SystemExit(f"{path.relative_to(ROOT)} root package version must be {version}, got {actual}")

    print(f"version consistency passed: v{version}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
