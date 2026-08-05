#!/usr/bin/env python3
"""Read, validate, and synchronize the Code Nest release manifest."""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as element_tree
from pathlib import Path, PurePosixPath
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
MANIFEST_PATH = Path("release/manifest.json")
MAVEN_NAMESPACE = "http://maven.apache.org/POM/4.0.0"
VERSION_PATTERN = re.compile(r"\d+\.\d+\.\d+")
RELEASE_VERSION_PATTERN = re.compile(r"v\d+\.\d+\.\d+")


class ManifestError(ValueError):
    """Raised when release metadata is incomplete or inconsistent."""


class ReleaseManifest:
    """The single interface for repository release metadata."""

    def __init__(self, root: Path, payload: dict[str, Any]) -> None:
        self.root = root.resolve()
        self.payload = payload

    @classmethod
    def load(cls, root: Path = REPO_ROOT) -> "ReleaseManifest":
        path = root / MANIFEST_PATH
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except FileNotFoundError as exc:
            raise ManifestError(f"release manifest not found: {path}") from exc
        except json.JSONDecodeError as exc:
            raise ManifestError(f"invalid release manifest JSON: {exc}") from exc
        if not isinstance(payload, dict):
            raise ManifestError("release manifest root must be an object")
        return cls(root, payload)

    def get(self, dotted_path: str) -> str:
        current: Any = self.payload
        for segment in dotted_path.split("."):
            if not isinstance(current, dict) or segment not in current:
                raise ManifestError(f"release manifest field not found: {dotted_path}")
            current = current[segment]
        if not isinstance(current, (str, int, float, bool)):
            raise ManifestError(f"release manifest field is not scalar: {dotted_path}")
        return str(current)

    @property
    def version(self) -> str:
        return self.get("version")

    @property
    def release_version(self) -> str:
        return self.get("releaseVersion")

    @property
    def maven_revision(self) -> str:
        return self.get("mavenRevision")

    @property
    def schema_version(self) -> str:
        return self.get("database.schemaVersion")

    def validate_definition(self) -> list[str]:
        errors: list[str] = []
        if self.payload.get("manifestSchema") != 1:
            errors.append("manifestSchema must be 1")

        required_fields = (
            "version",
            "releaseVersion",
            "mavenRevision",
            "database.schemaVersion",
            "database.requiredMigration",
            "docker.image",
            "docker.tag",
            "artifacts.backend.module",
            "artifacts.backend.sourcePattern",
            "artifacts.backend.bundlePath",
            "artifacts.admin.workspace",
            "artifacts.admin.sourceDirectory",
            "artifacts.admin.bundlePath",
            "artifacts.user.workspace",
            "artifacts.user.sourceDirectory",
            "artifacts.user.bundlePath",
            "artifacts.docs.workspace",
            "artifacts.docs.sourceDirectory",
            "bundle.manifestPath",
            "projections.versionFile",
            "projections.rootPom",
            "projections.flattenedPomGlob",
        )
        for field in required_fields:
            try:
                value = self.get(field)
            except ManifestError as exc:
                errors.append(str(exc))
                continue
            if not value.strip():
                errors.append(f"release manifest field is empty: {field}")

        version = self.payload.get("version")
        if not isinstance(version, str) or not VERSION_PATTERN.fullmatch(version):
            errors.append(f"version must use X.Y.Z format, got {version!r}")
        else:
            expected_release = f"v{version}"
            if self.payload.get("releaseVersion") != expected_release:
                errors.append(f"releaseVersion must be {expected_release}")
            if self.payload.get("mavenRevision") != expected_release:
                errors.append(f"mavenRevision must be {expected_release}")
            docker = self.payload.get("docker")
            if not isinstance(docker, dict) or docker.get("tag") != expected_release:
                errors.append(f"docker.tag must be {expected_release}")

        schema_version = self._optional_value("database.schemaVersion")
        if schema_version and not RELEASE_VERSION_PATTERN.fullmatch(schema_version):
            errors.append(f"database.schemaVersion must use vX.Y.Z format, got {schema_version!r}")

        path_fields = (
            "database.requiredMigration",
            "artifacts.backend.sourcePattern",
            "artifacts.backend.bundlePath",
            "artifacts.admin.workspace",
            "artifacts.admin.sourceDirectory",
            "artifacts.admin.bundlePath",
            "artifacts.user.workspace",
            "artifacts.user.sourceDirectory",
            "artifacts.user.bundlePath",
            "artifacts.docs.workspace",
            "artifacts.docs.sourceDirectory",
            "bundle.manifestPath",
            "projections.versionFile",
            "projections.rootPom",
        )
        for field in path_fields:
            value = self._optional_value(field)
            if value and not self._is_safe_relative_path(value):
                errors.append(f"{field} must be a safe repository-relative path: {value}")

        required_paths = self.payload.get("bundle", {}).get("requiredPaths")
        if not isinstance(required_paths, list) or not required_paths:
            errors.append("bundle.requiredPaths must be a non-empty array")
        elif any(not isinstance(path, str) or not self._is_safe_relative_path(path) for path in required_paths):
            errors.append("bundle.requiredPaths contains an unsafe or invalid path")
        else:
            for first, second in self._case_insensitive_path_collisions(required_paths):
                errors.append(
                    f"bundle.requiredPaths has a case-insensitive path collision: {first!r} and {second!r}"
                )
            expected_paths = {
                "RELEASE",
                self._optional_value("projections.versionFile"),
                self._optional_value("bundle.manifestPath"),
                self._optional_value("artifacts.backend.bundlePath"),
                f"{self._optional_value('artifacts.admin.bundlePath')}/index.html",
                f"{self._optional_value('artifacts.user.bundlePath')}/index.html",
                self._optional_value("database.requiredMigration"),
            }
            missing = sorted(path for path in expected_paths if path and path not in required_paths)
            if missing:
                errors.append(f"bundle.requiredPaths is missing: {missing}")

        projections = self.payload.get("projections")
        if not isinstance(projections, dict):
            errors.append("projections must be an object")
        else:
            for key in ("packageJson", "packageLocks", "environment"):
                if not isinstance(projections.get(key), list):
                    errors.append(f"projections.{key} must be an array")
        return errors

    def validate_repository(self) -> list[str]:
        errors = self.validate_definition()
        if errors:
            return errors

        expected_version = self.version
        expected_maven = self.maven_revision
        projections = self.payload["projections"]

        version_path = self.root / projections["versionFile"]
        actual_version = self._read_text(version_path).strip()
        if actual_version != expected_version:
            errors.append(
                f"{version_path.relative_to(self.root)} must be {expected_version}, got {actual_version!r}"
            )

        root_pom_path = self.root / projections["rootPom"]
        try:
            root_pom = element_tree.parse(root_pom_path).getroot()
            revision = root_pom.find(f"{{{MAVEN_NAMESPACE}}}properties/{{{MAVEN_NAMESPACE}}}revision")
            actual_revision = "" if revision is None or revision.text is None else revision.text.strip()
            if actual_revision != expected_maven:
                errors.append(
                    f"{root_pom_path.relative_to(self.root)} revision must be {expected_maven}, "
                    f"got {actual_revision!r}"
                )
            modules = {
                node.text.strip()
                for node in root_pom.findall(f"{{{MAVEN_NAMESPACE}}}modules/{{{MAVEN_NAMESPACE}}}module")
                if node.text
            }
            backend_module = self.get("artifacts.backend.module")
            if backend_module not in modules:
                errors.append(f"backend module is not declared in pom.xml: {backend_module}")
        except (FileNotFoundError, element_tree.ParseError) as exc:
            errors.append(f"cannot parse {root_pom_path.relative_to(self.root)}: {exc}")

        flattened_paths = sorted(self.root.glob(projections["flattenedPomGlob"]))
        if not flattened_paths:
            errors.append(f"no flattened POMs match {projections['flattenedPomGlob']}")
        for path in flattened_paths:
            try:
                pom = element_tree.parse(path).getroot()
            except element_tree.ParseError as exc:
                errors.append(f"cannot parse {path.relative_to(self.root)}: {exc}")
                continue
            project_versions = {
                (node.text or "").strip()
                for node in pom.iter()
                if node.tag.rsplit("}", 1)[-1] in {"version", "revision"}
                and RELEASE_VERSION_PATTERN.fullmatch((node.text or "").strip())
            }
            if project_versions != {expected_maven}:
                errors.append(
                    f"{path.relative_to(self.root)} release versions must be {expected_maven}, "
                    f"got {sorted(project_versions)}"
                )

        package_names: set[str] = set()
        for relative in projections["packageJson"]:
            path = self.root / relative
            payload = self._read_json(path, errors)
            if payload is None:
                continue
            actual = str(payload.get("version", ""))
            if actual != expected_version:
                errors.append(f"{relative} version must be {expected_version}, got {actual!r}")
            name = payload.get("name")
            if isinstance(name, str):
                package_names.add(name)

        for relative in projections["packageLocks"]:
            path = self.root / relative
            payload = self._read_json(path, errors)
            if payload is None:
                continue
            if str(payload.get("version", "")) != expected_version:
                errors.append(f"{relative} top-level version must be {expected_version}")
            packages = payload.get("packages")
            if not isinstance(packages, dict):
                errors.append(f"{relative} packages must be an object")
                continue
            root_package = packages.get("")
            if not isinstance(root_package, dict) or str(root_package.get("version", "")) != expected_version:
                errors.append(f"{relative} root package version must be {expected_version}")
            for package_path, package in packages.items():
                if not isinstance(package, dict) or package.get("name") not in package_names:
                    continue
                if str(package.get("version", "")) != expected_version:
                    errors.append(
                        f"{relative} linked package {package_path} version must be {expected_version}"
                    )

        for projection in projections["environment"]:
            if not isinstance(projection, dict):
                errors.append("projections.environment entries must be objects")
                continue
            relative = projection.get("path")
            key = projection.get("key")
            source = projection.get("source")
            if not all(isinstance(value, str) and value for value in (relative, key, source)):
                errors.append("projections.environment entry is incomplete")
                continue
            expected = self.get(source)
            content = self._read_text(self.root / relative)
            match = re.search(rf"^{re.escape(key)}=(.*)$", content, re.MULTILINE)
            actual = "" if match is None else match.group(1).strip()
            if actual != expected:
                errors.append(f"{relative} {key} must be {expected}, got {actual!r}")

        for field in (
            "database.requiredMigration",
            "artifacts.backend.module",
            "artifacts.admin.workspace",
            "artifacts.user.workspace",
            "artifacts.docs.workspace",
        ):
            relative = self.get(field)
            if not (self.root / relative).exists():
                errors.append(f"release manifest target does not exist: {relative}")
        return errors

    def synchronize(self, version: str, schema_version: str | None = None) -> None:
        if not VERSION_PATTERN.fullmatch(version):
            raise ManifestError(f"version must use X.Y.Z format, got {version!r}")
        if schema_version is not None:
            normalized_schema = schema_version if schema_version.startswith("v") else f"v{schema_version}"
            if not RELEASE_VERSION_PATTERN.fullmatch(normalized_schema):
                raise ManifestError(f"schema version must use X.Y.Z or vX.Y.Z format, got {schema_version!r}")
            self.payload["database"]["schemaVersion"] = normalized_schema

        release_version = f"v{version}"
        self.payload["version"] = version
        self.payload["releaseVersion"] = release_version
        self.payload["mavenRevision"] = release_version
        self.payload["docker"]["tag"] = release_version
        self._write_json(self.root / MANIFEST_PATH, self.payload)

        projections = self.payload["projections"]
        (self.root / projections["versionFile"]).write_text(f"{version}\n", encoding="utf-8")

        root_pom_path = self.root / projections["rootPom"]
        root_pom = self._read_text(root_pom_path)
        updated_pom, count = re.subn(
            r"(<revision>\s*)v\d+\.\d+\.\d+(\s*</revision>)",
            rf"\g<1>{release_version}\g<2>",
            root_pom,
            count=1,
        )
        if count != 1:
            raise ManifestError(f"cannot update revision in {root_pom_path.relative_to(self.root)}")
        root_pom_path.write_text(updated_pom, encoding="utf-8")

        flattened_paths = sorted(self.root.glob(projections["flattenedPomGlob"]))
        if not flattened_paths:
            raise ManifestError(f"no flattened POMs match {projections['flattenedPomGlob']}")
        for path in flattened_paths:
            content = self._read_text(path)
            updated = re.sub(
                r"(<(?:version|revision)>)v\d+\.\d+\.\d+(</(?:version|revision)>)",
                rf"\g<1>{release_version}\g<2>",
                content,
            )
            path.write_text(updated, encoding="utf-8")

        package_names: set[str] = set()
        for relative in projections["packageJson"]:
            path = self.root / relative
            package = json.loads(self._read_text(path))
            package["version"] = version
            name = package.get("name")
            if isinstance(name, str):
                package_names.add(name)
            self._write_json(path, package)

        for relative in projections["packageLocks"]:
            path = self.root / relative
            lock = json.loads(self._read_text(path))
            lock["version"] = version
            packages = lock.get("packages")
            if not isinstance(packages, dict) or not isinstance(packages.get(""), dict):
                raise ManifestError(f"package lock has no root package: {relative}")
            packages[""]["version"] = version
            for package in packages.values():
                if isinstance(package, dict) and package.get("name") in package_names:
                    package["version"] = version
            self._write_json(path, lock)

        for projection in projections["environment"]:
            path = self.root / projection["path"]
            key = projection["key"]
            value = self.get(projection["source"])
            content = self._read_text(path)
            updated, count = re.subn(
                rf"^{re.escape(key)}=.*$",
                f"{key}={value}",
                content,
                count=1,
                flags=re.MULTILINE,
            )
            if count != 1:
                raise ManifestError(f"cannot update {key} in {path.relative_to(self.root)}")
            path.write_text(updated, encoding="utf-8")

        refreshed = ReleaseManifest.load(self.root)
        errors = refreshed.validate_repository()
        if errors:
            raise ManifestError("release synchronization failed:\n" + "\n".join(errors))
        self.payload = refreshed.payload

    def _optional_value(self, dotted_path: str) -> str:
        try:
            return self.get(dotted_path)
        except ManifestError:
            return ""

    @staticmethod
    def _is_safe_relative_path(value: str) -> bool:
        if "\\" in value or not value:
            return False
        path = PurePosixPath(value)
        return not path.is_absolute() and ".." not in path.parts and "." not in path.parts

    @staticmethod
    def _case_insensitive_path_collisions(values: list[str]) -> list[tuple[str, str]]:
        collisions: list[tuple[str, str]] = []
        normalized: list[tuple[str, tuple[str, ...]]] = []
        for value in values:
            parts = tuple(part.casefold() for part in PurePosixPath(value).parts)
            for previous_value, previous_parts in normalized:
                shared_length = min(len(parts), len(previous_parts))
                if parts[:shared_length] == previous_parts[:shared_length] and (
                    len(parts) == shared_length or len(previous_parts) == shared_length
                ):
                    collisions.append((previous_value, value))
            normalized.append((value, parts))
        return collisions

    @staticmethod
    def _read_text(path: Path) -> str:
        try:
            return path.read_text(encoding="utf-8")
        except FileNotFoundError as exc:
            raise ManifestError(f"release projection not found: {path}") from exc

    @staticmethod
    def _read_json(path: Path, errors: list[str]) -> dict[str, Any] | None:
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except (FileNotFoundError, json.JSONDecodeError) as exc:
            errors.append(f"cannot read {path}: {exc}")
            return None
        if not isinstance(payload, dict):
            errors.append(f"JSON root must be an object: {path}")
            return None
        return payload

    @staticmethod
    def _write_json(path: Path, payload: dict[str, Any]) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"{json.dumps(payload, ensure_ascii=False, indent=2)}\n", encoding="utf-8")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("validate", help="Validate the manifest and every governed projection.")

    get_parser = subparsers.add_parser("get", help="Print one scalar manifest field.")
    get_parser.add_argument("field", help="Dot-separated manifest field, for example releaseVersion.")

    sync_parser = subparsers.add_parser("sync", help="Set the release version and synchronize projections.")
    sync_parser.add_argument("version", help="Release version in X.Y.Z format.")
    sync_parser.add_argument(
        "--schema-version",
        help="Latest database schema version; omitted when a release has no migration.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        manifest = ReleaseManifest.load()
        if args.command == "get":
            errors = manifest.validate_definition()
            if errors:
                raise ManifestError("\n".join(errors))
            print(manifest.get(args.field))
            return 0
        if args.command == "sync":
            manifest.synchronize(args.version, args.schema_version)
            print(
                f"release projections synchronized: {manifest.release_version} "
                f"(schema {manifest.schema_version})"
            )
            return 0

        errors = manifest.validate_repository()
        if errors:
            for error in errors:
                print(f"release manifest violation: {error}", file=sys.stderr)
            return 1
        print(
            f"release manifest passed: {manifest.release_version} "
            f"(schema {manifest.schema_version})"
        )
        return 0
    except ManifestError as exc:
        print(f"release manifest error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
