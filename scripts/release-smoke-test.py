#!/usr/bin/env python3
"""Validate the contents and metadata of a Code-Nest release bundle."""

from __future__ import annotations

import argparse
import io
import re
import tarfile
from pathlib import Path, PurePosixPath


REQUIRED_FILES = {
    "RELEASE",
    "VERSION",
    "backend/app.jar",
    "user/index.html",
    "admin/index.html",
    "scripts/deploy-release.sh",
    "scripts/db-migrate.py",
    "scripts/release-smoke-test.py",
    "sql/v2.5.3/production_governance.sql",
}


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a Code-Nest release bundle.")
    parser.add_argument("bundle", type=Path)
    args = parser.parse_args()
    if not args.bundle.is_file():
        raise SystemExit(f"bundle not found: {args.bundle}")

    with tarfile.open(args.bundle, "r:gz") as archive:
        names = {member.name for member in archive.getmembers()}
        unsafe = [
            member.name
            for member in archive.getmembers()
            if member.name.replace("\\", "/").startswith("/")
            or ".." in PurePosixPath(member.name.replace("\\", "/")).parts
            or not (member.isdir() or member.isfile())
        ]
        if unsafe:
            raise SystemExit(f"unsafe archive paths: {unsafe[:3]}")
        missing = sorted(REQUIRED_FILES - names)
        if missing:
            raise SystemExit(f"release bundle missing: {', '.join(missing)}")
        release = read_member(archive, "RELEASE")
        version = read_member(archive, "VERSION").strip()

    metadata = dict(
        line.split("=", 1)
        for line in release.splitlines()
        if "=" in line
    )
    expected = f"v{version}"
    if metadata.get("version") != expected:
        raise SystemExit(f"RELEASE version must be {expected}")
    if metadata.get("schema_version") != expected:
        raise SystemExit(f"RELEASE schema_version must be {expected}")
    if not metadata.get("sha") or (
        metadata["sha"] != "unknown" and not re.fullmatch(r"[0-9a-f]{40}", metadata["sha"])
    ):
        raise SystemExit("RELEASE sha is missing")
    print(f"release smoke test passed: version={expected}, files={len(names)}")
    return 0


def read_member(archive: tarfile.TarFile, name: str) -> str:
    member = archive.getmember(name)
    handle = archive.extractfile(member)
    if handle is None:
        raise SystemExit(f"cannot read archive member: {name}")
    return handle.read().decode("utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
