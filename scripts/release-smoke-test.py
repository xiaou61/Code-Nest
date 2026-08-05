#!/usr/bin/env python3
"""Validate the contents and metadata of a Code-Nest release bundle."""

from __future__ import annotations

import argparse
import json
import re
import tarfile
from pathlib import Path, PurePosixPath

from release_manifest import ReleaseManifest

MANIFEST_MEMBER = "release-manifest.json"


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
        if MANIFEST_MEMBER not in names:
            raise SystemExit(f"release bundle missing: {MANIFEST_MEMBER}")
        manifest_payload = json.loads(read_member(archive, MANIFEST_MEMBER))
        manifest = ReleaseManifest(Path.cwd(), manifest_payload)
        definition_errors = manifest.validate_definition()
        if definition_errors:
            raise SystemExit("invalid bundled release manifest: " + "; ".join(definition_errors))
        if manifest.get("bundle.manifestPath") != MANIFEST_MEMBER:
            raise SystemExit(f"bundle.manifestPath must be {MANIFEST_MEMBER}")
        required_files = set(manifest_payload["bundle"]["requiredPaths"])
        missing = sorted(required_files - names)
        if missing:
            raise SystemExit(f"release bundle missing: {', '.join(missing)}")
        release = read_member(archive, "RELEASE")
        version = read_member(archive, "VERSION").strip()

    metadata = dict(
        line.split("=", 1)
        for line in release.splitlines()
        if "=" in line
    )
    if version != manifest.version:
        raise SystemExit(f"VERSION must be {manifest.version}")
    expected = manifest.release_version
    if metadata.get("version") != expected:
        raise SystemExit(f"RELEASE version must be {expected}")
    if metadata.get("schema_version") != manifest.schema_version:
        raise SystemExit(f"RELEASE schema_version must be {manifest.schema_version}")
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
