#!/usr/bin/env python3
"""Build and deploy a Code Nest release bundle to a production server.

This is a local/operator entrypoint that mirrors the GitHub Actions production
deployment bundle. It intentionally reads connection details from arguments or
environment variables instead of hardcoding a production host.
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import tarfile
import tempfile
from datetime import datetime, timezone
from pathlib import Path

from release_manifest import ReleaseManifest


REPO_ROOT = Path(__file__).resolve().parents[1]


def run(command: list[str], *, cwd: Path = REPO_ROOT) -> None:
    print(f"$ {' '.join(command)}")
    subprocess.run(command, cwd=cwd, check=True)


def capture(command: list[str], *, cwd: Path = REPO_ROOT) -> str:
    return subprocess.check_output(command, cwd=cwd, text=True).strip()


def require_directory(path: Path, label: str) -> None:
    if not path.is_dir():
        raise SystemExit(f"{label} does not exist: {path}")


def require_file(path: Path, label: str) -> None:
    if not path.is_file():
        raise SystemExit(f"{label} does not exist: {path}")


def repository_version() -> str:
    return ReleaseManifest.load(REPO_ROOT).release_version


def ensure_clean_worktree(allow_dirty: bool) -> None:
    if allow_dirty:
        return
    if capture(["git", "status", "--porcelain"]):
        raise SystemExit("refusing production build from dirty worktree; pass --allow-dirty only for diagnostics")


def normalize_release_version(value: str | None) -> str:
    expected = repository_version()
    candidate = expected if value is None or not value.strip() else value.strip()
    if not candidate.startswith("v"):
        candidate = f"v{candidate}"
    if candidate != expected:
        raise SystemExit(f"release version {candidate} does not match release manifest {expected}")
    return candidate


def discover_backend_jar() -> Path:
    manifest = ReleaseManifest.load(REPO_ROOT)
    source_pattern = Path(manifest.get("artifacts.backend.sourcePattern"))
    target_dir = REPO_ROOT / source_pattern.parent
    jars = sorted(
        target_dir.glob(source_pattern.name),
        key=lambda item: item.stat().st_mtime,
        reverse=True,
    )
    if not jars:
        raise SystemExit("backend jar not found; run without --skip-build first")
    return jars[0]


def copy_tree_contents(source: Path, target: Path) -> None:
    require_directory(source, f"source directory for {target.name}")
    target.mkdir(parents=True, exist_ok=True)
    for item in source.iterdir():
        destination = target / item.name
        if item.is_dir():
            shutil.copytree(item, destination)
        else:
            shutil.copy2(item, destination)


def copy_release_file(source: Path, target: Path) -> None:
    require_file(source, f"release asset for {target}")
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)


def assemble_operational_assets(stage: Path) -> None:
    copy_release_file(
        REPO_ROOT / "deploy" / "nginx" / "code-nest-production.conf",
        stage / "ops" / "nginx" / "code-nest-production.conf",
    )
    for filename in ("docker-compose.yml", "prometheus.yml", "alert_rules.yml"):
        copy_release_file(
            REPO_ROOT / "docker" / "monitoring" / filename,
            stage / "ops" / "monitoring" / filename,
        )
    copy_tree_contents(
        REPO_ROOT / "docker" / "monitoring" / "grafana",
        stage / "ops" / "monitoring" / "grafana",
    )
    for filename in ("compose.sh", "validate-config.sh"):
        copy_release_file(
            REPO_ROOT / "docker" / "monitoring" / "scripts" / filename,
            stage / "ops" / "monitoring" / "scripts" / filename,
        )
    for filename in (
        "code-nest-capacity-governance.service",
        "code-nest-capacity-governance.timer",
    ):
        copy_release_file(
            REPO_ROOT / "deploy" / "systemd" / filename,
            stage / "ops" / "systemd" / filename,
        )
    for filename in (
        "server-capacity-governance.sh",
        "sre-alertmanager-e2e.py",
        "verify-production-baseline.sh",
    ):
        source = REPO_ROOT / "scripts" / filename
        copy_release_file(source, stage / "scripts" / filename)
        copy_release_file(source, stage / "ops" / "scripts" / filename)
    copy_release_file(
        REPO_ROOT / "scripts" / "deploy-release.sh",
        stage / "scripts" / "deploy-release.sh",
    )


def build_artifacts(args: argparse.Namespace) -> None:
    if args.skip_build:
        return

    manifest = ReleaseManifest.load(REPO_ROOT)
    run([sys.executable, str(REPO_ROOT / "scripts" / "release_manifest.py"), "validate"])
    run([
        "mvn", "-B", "-pl", manifest.get("artifacts.backend.module"), "-am", "clean", "package",
        "-DskipTests", f"-Drevision={manifest.maven_revision}",
    ])
    run(["npm", "run", "build"], cwd=REPO_ROOT / manifest.get("artifacts.admin.workspace"))
    run(["npm", "run", "build"], cwd=REPO_ROOT / manifest.get("artifacts.user.workspace"))


def assemble_bundle(args: argparse.Namespace) -> Path:
    manifest = ReleaseManifest.load(REPO_ROOT)
    version = args.version or manifest.release_version
    safe_version = "".join(ch if ch.isalnum() or ch in "._-" else "-" for ch in version)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")

    stage = Path(tempfile.mkdtemp(prefix=f"code-nest-release-{safe_version}-"))
    bundle = Path(tempfile.gettempdir()) / f"code-nest-{safe_version}-{stamp}.tar.gz"

    try:
        backend_target = stage / manifest.get("artifacts.backend.bundlePath")
        admin_dir = stage / manifest.get("artifacts.admin.bundlePath")
        user_dir = stage / manifest.get("artifacts.user.bundlePath")
        backend_target.parent.mkdir(parents=True)

        backend_jar = Path(args.jar) if args.jar else discover_backend_jar()
        require_file(backend_jar, "backend jar")
        shutil.copy2(backend_jar, backend_target)

        copy_tree_contents(REPO_ROOT / manifest.get("artifacts.admin.sourceDirectory"), admin_dir)
        copy_tree_contents(REPO_ROOT / manifest.get("artifacts.user.sourceDirectory"), user_dir)
        assemble_operational_assets(stage)
        copy_release_file(REPO_ROOT / "scripts" / "db-migrate.py", stage / "scripts" / "db-migrate.py")
        copy_release_file(REPO_ROOT / "scripts" / "release_manifest.py", stage / "scripts" / "release_manifest.py")
        copy_release_file(REPO_ROOT / "scripts" / "release-smoke-test.py", stage / "scripts" / "release-smoke-test.py")
        copy_release_file(
            REPO_ROOT / "release" / "manifest.json",
            stage / manifest.get("bundle.manifestPath"),
        )
        copy_release_file(
            REPO_ROOT / manifest.get("projections.versionFile"),
            stage / manifest.get("projections.versionFile"),
        )
        shutil.copytree(REPO_ROOT / "sql", stage / "sql")

        try:
            sha = capture(["git", "rev-parse", "HEAD"])
        except (subprocess.CalledProcessError, FileNotFoundError):
            sha = "unknown"
        build_id = os.getenv("CODE_NEST_BUILD_ID") or f"{safe_version}-{sha[:7]}"

        (stage / "RELEASE").write_text(
            "\n".join(
                [
                    f"version={safe_version}",
                    f"sha={sha}",
                    f"build_id={build_id}",
                    f"built_at={datetime.now(timezone.utc).isoformat()}",
                    f"schema_version={manifest.schema_version}",
                    "",
                ]
            ),
            encoding="utf-8",
        )

        if bundle.exists():
            bundle.unlink()
        with tarfile.open(bundle, "w:gz") as archive:
            for item in stage.rglob("*"):
                tar_info = archive.gettarinfo(str(item), arcname=str(item.relative_to(stage)))
                if item.suffix in {".sh", ".py"}:
                    tar_info.mode = 0o755
                if item.is_file():
                    with item.open("rb") as handle:
                        archive.addfile(tar_info, handle)
                else:
                    archive.addfile(tar_info)
        run([sys.executable, str(REPO_ROOT / "scripts" / "release-smoke-test.py"), str(bundle)])
    finally:
        shutil.rmtree(stage, ignore_errors=True)

    return bundle


def ssh_target(args: argparse.Namespace) -> str:
    return f"{args.user}@{args.host}"


def ssh_base(args: argparse.Namespace) -> list[str]:
    command = ["ssh", "-p", str(args.port)]
    if args.identity:
        command.extend(["-i", args.identity])
    if args.strict_host_key_checking:
        command.extend(["-o", "StrictHostKeyChecking=yes"])
    return command


def scp_base(args: argparse.Namespace) -> list[str]:
    command = ["scp", "-P", str(args.port)]
    if args.identity:
        command.extend(["-i", args.identity])
    if args.strict_host_key_checking:
        command.extend(["-o", "StrictHostKeyChecking=yes"])
    return command


def deploy_bundle(args: argparse.Namespace, bundle: Path) -> None:
    remote_bundle = f"/tmp/{bundle.name}"
    remote_script = "/tmp/code-nest-deploy-release.sh"
    reload_nginx = "true" if args.reload_nginx else "false"
    release_version = args.version or bundle.name.removeprefix("code-nest-").removesuffix(".tar.gz")

    run(scp_base(args) + [str(bundle), f"{ssh_target(args)}:{remote_bundle}"])
    run(scp_base(args) + [str(REPO_ROOT / "scripts" / "deploy-release.sh"), f"{ssh_target(args)}:{remote_script}"])
    remote_command = (
        f"chmod 755 {remote_script} && "
        f"CODE_NEST_RELEASE_VERSION='{release_version}' "
        f"CODE_NEST_RELOAD_NGINX='{reload_nginx}' "
        f"{remote_script} deploy '{remote_bundle}'"
    )
    run(ssh_base(args) + [ssh_target(args), remote_command])


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build and deploy Code Nest to a production server.")
    parser.add_argument("--host", default=os.getenv("CODE_NEST_DEPLOY_HOST"), help="SSH host or IP.")
    parser.add_argument("--user", default=os.getenv("CODE_NEST_DEPLOY_USER", "root"), help="SSH user.")
    parser.add_argument("--port", default=os.getenv("CODE_NEST_DEPLOY_PORT", "22"), help="SSH port.")
    parser.add_argument("--identity", default=os.getenv("CODE_NEST_DEPLOY_KEY"), help="SSH private key path.")
    parser.add_argument("--version", default=os.getenv("CODE_NEST_RELEASE_VERSION"), help="Release version label.")
    parser.add_argument("--jar", default=None, help="Use an existing backend jar instead of auto-discovery.")
    parser.add_argument("--skip-build", action="store_true", help="Use existing backend jar and frontend dist.")
    parser.add_argument("--allow-dirty", action="store_true", help="Allow dirty worktrees for local diagnostics only.")
    parser.add_argument("--no-reload-nginx", action="store_true", help="Deploy without reloading nginx.")
    parser.add_argument(
        "--strict-host-key-checking",
        action="store_true",
        help="Enable strict SSH host-key checking for ssh/scp.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if not args.host:
        raise SystemExit("missing --host or CODE_NEST_DEPLOY_HOST")
    manifest_errors = ReleaseManifest.load(REPO_ROOT).validate_repository()
    if manifest_errors:
        raise SystemExit("release manifest validation failed:\n" + "\n".join(manifest_errors))
    args.version = normalize_release_version(args.version)
    ensure_clean_worktree(args.allow_dirty)
    args.reload_nginx = not args.no_reload_nginx

    build_artifacts(args)
    bundle = assemble_bundle(args)
    print(f"Release bundle: {bundle} ({bundle.stat().st_size} bytes)")
    deploy_bundle(args, bundle)
    print("Deployment completed.")


if __name__ == "__main__":
    main()
