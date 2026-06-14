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
import tarfile
import tempfile
from datetime import datetime, timezone
from pathlib import Path


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


def discover_backend_jar() -> Path:
    target_dir = REPO_ROOT / "xiaou-application" / "target"
    jars = sorted(
        target_dir.glob("xiaou-application-*.jar"),
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


def build_artifacts(args: argparse.Namespace) -> None:
    if args.skip_build:
        return

    run(["mvn", "-B", "-pl", "xiaou-application", "-am", "clean", "package", "-DskipTests"])
    run(["npm", "run", "build"], cwd=REPO_ROOT / "vue3-admin-front")
    run(["npm", "run", "build"], cwd=REPO_ROOT / "vue3-user-front")


def assemble_bundle(args: argparse.Namespace) -> Path:
    version = args.version
    if not version:
        try:
            branch = capture(["git", "rev-parse", "--abbrev-ref", "HEAD"])
            sha = capture(["git", "rev-parse", "--short", "HEAD"])
            version = f"{branch}-{sha}"
        except (subprocess.CalledProcessError, FileNotFoundError):
            version = "manual"
    safe_version = "".join(ch if ch.isalnum() or ch in "._-" else "-" for ch in version)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")

    stage = Path(tempfile.mkdtemp(prefix=f"code-nest-release-{safe_version}-"))
    bundle = Path(tempfile.gettempdir()) / f"code-nest-{safe_version}-{stamp}.tar.gz"

    try:
        backend_dir = stage / "backend"
        admin_dir = stage / "admin"
        user_dir = stage / "user"
        scripts_dir = stage / "scripts"
        backend_dir.mkdir(parents=True)
        admin_dir.mkdir()
        user_dir.mkdir()
        scripts_dir.mkdir()

        backend_jar = Path(args.jar) if args.jar else discover_backend_jar()
        require_file(backend_jar, "backend jar")
        shutil.copy2(backend_jar, backend_dir / "app.jar")

        copy_tree_contents(REPO_ROOT / "vue3-admin-front" / "dist", admin_dir)
        copy_tree_contents(REPO_ROOT / "vue3-user-front" / "dist", user_dir)
        shutil.copy2(REPO_ROOT / "scripts" / "deploy-release.sh", scripts_dir / "deploy-release.sh")

        try:
            sha = capture(["git", "rev-parse", "HEAD"])
        except (subprocess.CalledProcessError, FileNotFoundError):
            sha = "unknown"

        (stage / "RELEASE").write_text(
            "\n".join(
                [
                    f"version={safe_version}",
                    f"sha={sha}",
                    f"built_at={datetime.now(timezone.utc).isoformat()}",
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
                if item.name == "deploy-release.sh":
                    tar_info.mode = 0o755
                if item.is_file():
                    with item.open("rb") as handle:
                        archive.addfile(tar_info, handle)
                else:
                    archive.addfile(tar_info)
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
    args.reload_nginx = not args.no_reload_nginx

    build_artifacts(args)
    bundle = assemble_bundle(args)
    print(f"Release bundle: {bundle} ({bundle.stat().st_size} bytes)")
    deploy_bundle(args, bundle)
    print("Deployment completed.")


if __name__ == "__main__":
    main()
