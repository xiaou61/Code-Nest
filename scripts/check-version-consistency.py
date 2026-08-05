#!/usr/bin/env python3
"""Compatibility entrypoint for validating release metadata projections."""

from release_manifest import main as release_manifest_main


if __name__ == "__main__":
    raise SystemExit(release_manifest_main(["validate"]))
