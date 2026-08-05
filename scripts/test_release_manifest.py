#!/usr/bin/env python3
"""Focused tests for the release manifest interface."""

from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from release_manifest import MANIFEST_PATH, ReleaseManifest


class ReleaseManifestTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary_directory.name)
        self._write_fixture()

    def tearDown(self) -> None:
        self.temporary_directory.cleanup()

    def test_validate_repository_detects_projection_drift(self) -> None:
        manifest = ReleaseManifest.load(self.root)
        self.assertEqual([], manifest.validate_repository())

        (self.root / "VERSION").write_text("9.9.9\n", encoding="utf-8")

        errors = manifest.validate_repository()
        self.assertTrue(any("VERSION must be 1.2.3" in error for error in errors))

    def test_synchronize_updates_every_projection_and_keeps_schema_baseline(self) -> None:
        manifest = ReleaseManifest.load(self.root)

        manifest.synchronize("1.2.4")

        refreshed = ReleaseManifest.load(self.root)
        self.assertEqual("1.2.4", refreshed.version)
        self.assertEqual("v1.2.4", refreshed.maven_revision)
        self.assertEqual("v1.2.0", refreshed.schema_version)
        self.assertEqual([], refreshed.validate_repository())
        self.assertNotIn(
            "v1.2.3",
            (self.root / "pom-xml-flattened").read_text(encoding="utf-8"),
        )

        package_lock = json.loads((self.root / "app/package-lock.json").read_text(encoding="utf-8"))
        self.assertEqual("1.2.4", package_lock["packages"]["../shared"]["version"])
        self.assertIn("XIAOU_BUILD_VERSION=1.2.4", (self.root / "example.env").read_text(encoding="utf-8"))

    def test_get_rejects_non_scalar_fields(self) -> None:
        manifest = ReleaseManifest.load(self.root)

        with self.assertRaisesRegex(ValueError, "is not scalar"):
            manifest.get("artifacts")

    def test_validate_definition_rejects_case_insensitive_path_collisions(self) -> None:
        manifest = ReleaseManifest.load(self.root)
        manifest.payload["bundle"]["requiredPaths"].append("RELEASE/manifest.json")

        errors = manifest.validate_definition()

        self.assertTrue(any("case-insensitive path collision" in error for error in errors))

    def _write_fixture(self) -> None:
        manifest = {
            "manifestSchema": 1,
            "version": "1.2.3",
            "releaseVersion": "v1.2.3",
            "mavenRevision": "v1.2.3",
            "database": {
                "schemaVersion": "v1.2.0",
                "requiredMigration": "sql/v1.2.0/migration.sql",
            },
            "docker": {"image": "code-nest", "tag": "v1.2.3"},
            "artifacts": {
                "backend": {
                    "module": "backend",
                    "sourcePattern": "backend/target/backend-*.jar",
                    "bundlePath": "backend/app.jar",
                },
                "admin": {
                    "workspace": "app",
                    "sourceDirectory": "app/dist",
                    "bundlePath": "admin",
                },
                "user": {
                    "workspace": "app",
                    "sourceDirectory": "app/dist",
                    "bundlePath": "user",
                },
                "docs": {
                    "workspace": "docs",
                    "sourceDirectory": "docs/dist",
                },
            },
            "bundle": {
                "manifestPath": "release-manifest.json",
                "requiredPaths": [
                    "RELEASE",
                    "VERSION",
                    "release-manifest.json",
                    "backend/app.jar",
                    "admin/index.html",
                    "user/index.html",
                    "sql/v1.2.0/migration.sql",
                ]
            },
            "projections": {
                "versionFile": "VERSION",
                "rootPom": "pom.xml",
                "flattenedPomGlob": "**/pom-xml-flattened",
                "packageJson": ["app/package.json", "shared/package.json"],
                "packageLocks": ["app/package-lock.json"],
                "environment": [
                    {"path": "example.env", "key": "XIAOU_BUILD_VERSION", "source": "version"}
                ],
            },
        }
        self._write_json(MANIFEST_PATH, manifest)
        self._write("VERSION", "1.2.3\n")
        self._write(
            "pom.xml",
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
            "  <modules><module>backend</module></modules>\n"
            "  <properties><revision>v1.2.3</revision></properties>\n"
            "</project>\n",
        )
        self._write(
            "pom-xml-flattened",
            "<project><version>v1.2.3</version><properties><revision>v1.2.3</revision></properties></project>\n",
        )
        self._write_json("app/package.json", {"name": "app", "version": "1.2.3"})
        self._write_json("shared/package.json", {"name": "shared", "version": "1.2.3"})
        self._write_json(
            "app/package-lock.json",
            {
                "name": "app",
                "version": "1.2.3",
                "packages": {
                    "": {"name": "app", "version": "1.2.3"},
                    "../shared": {"name": "shared", "version": "1.2.3"},
                },
            },
        )
        self._write("example.env", "XIAOU_BUILD_VERSION=1.2.3\n")
        self._write("sql/v1.2.0/migration.sql", "-- fixture\n")
        for directory in ("backend", "app", "docs"):
            (self.root / directory).mkdir(parents=True, exist_ok=True)

    def _write(self, relative: str | Path, content: str) -> None:
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    def _write_json(self, relative: str | Path, payload: dict[str, object]) -> None:
        self._write(relative, f"{json.dumps(payload, indent=2)}\n")


if __name__ == "__main__":
    unittest.main()
