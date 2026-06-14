#!/usr/bin/env python3
"""Build and deploy Code-Nest frontend static bundles.

Password handling:
  Set CODE_NEST_DEPLOY_PASSWORD for non-interactive use, or enter it when prompted.
  The password is never written to disk by this script.
"""

from __future__ import annotations

import argparse
import getpass
import os
import shlex
import subprocess
import sys
import tarfile
import time
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
DEPLOY_DIR = REPO_ROOT / "output" / "deploy"

APP_CONFIG = {
    "user": {
        "local_dir": REPO_ROOT / "vue3-user-front",
        "remote_dir": "/var/www/code-nest-user",
    },
    "admin": {
        "local_dir": REPO_ROOT / "vue3-admin-front",
        "remote_dir": "/var/www/code-nest-admin",
    },
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Deploy Code-Nest frontends to the server.")
    parser.add_argument("--host", default=os.getenv("CODE_NEST_DEPLOY_HOST"))
    parser.add_argument("--port", type=int, default=int(os.getenv("CODE_NEST_DEPLOY_PORT", "22")))
    parser.add_argument("--user", default=os.getenv("CODE_NEST_DEPLOY_USER", "root"))
    parser.add_argument(
        "--apps",
        nargs="+",
        choices=sorted(APP_CONFIG.keys()),
        default=["user", "admin"],
        help="Frontend apps to deploy.",
    )
    parser.add_argument("--skip-build", action="store_true", help="Deploy existing dist folders.")
    parser.add_argument("--no-reload", action="store_true", help="Skip nginx reload after replacement.")
    return parser.parse_args()


def run(cmd: list[str], cwd: Path) -> None:
    print(f"$ {' '.join(cmd)}  # cwd={cwd}")
    subprocess.run(cmd, cwd=cwd, check=True)


def build_app(app: str) -> None:
    npm = "npm.cmd" if sys.platform.startswith("win") else "npm"
    run([npm, "run", "build"], APP_CONFIG[app]["local_dir"])


def make_archive(app: str, stamp: str) -> Path:
    dist_dir = APP_CONFIG[app]["local_dir"] / "dist"
    if not (dist_dir / "index.html").is_file():
        raise FileNotFoundError(f"Missing dist/index.html for {app}: {dist_dir}")
    if not (dist_dir / "assets").is_dir():
        raise FileNotFoundError(f"Missing dist/assets for {app}: {dist_dir}")

    DEPLOY_DIR.mkdir(parents=True, exist_ok=True)
    archive_path = DEPLOY_DIR / f"code-nest-{app}-dist-{stamp}.tar.gz"
    with tarfile.open(archive_path, "w:gz") as archive:
        for path in dist_dir.rglob("*"):
            archive.add(path, arcname=path.relative_to(dist_dir).as_posix())
    print(f"packed {app}: {archive_path}")
    return archive_path


def remote_deploy_command(app: str, remote_archive: str, reload_nginx: bool) -> str:
    target = APP_CONFIG[app]["remote_dir"]
    stage = f"/tmp/code-nest-{app}-dist-{int(time.time())}"
    backup = f"/var/backups/code-nest-{app}-{time.strftime('%Y%m%d%H%M%S')}"
    reload_part = "nginx -t && systemctl reload nginx" if reload_nginx else "nginx -t"
    return f"""
set -euo pipefail
TARGET={shlex.quote(target)}
ARCHIVE={shlex.quote(remote_archive)}
STAGE={shlex.quote(stage)}
BACKUP={shlex.quote(backup)}
rm -rf "$STAGE"
mkdir -p "$STAGE" "$TARGET" /var/backups
tar -xzf "$ARCHIVE" -C "$STAGE"
test -f "$STAGE/index.html"
test -d "$STAGE/assets"
cp -a "$TARGET" "$BACKUP"
rm -rf "$TARGET"/*
cp -a "$STAGE"/. "$TARGET"/
chown -R root:root "$TARGET"
find "$TARGET" -type d -exec chmod 755 {{}} +
find "$TARGET" -type f -exec chmod 644 {{}} +
rm -rf "$STAGE" "$ARCHIVE"
echo "{app} deployed: $(find "$TARGET/assets" -maxdepth 1 -type f | wc -l) asset files, backup=$BACKUP"
{reload_part}
stat -c '%y %n' "$TARGET/index.html"
"""


def deploy_archives(args: argparse.Namespace, archives: dict[str, Path], password: str) -> None:
    try:
        import paramiko
    except ImportError as exc:
        raise SystemExit("paramiko is required: python -m pip install paramiko") from exc

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(
        args.host,
        port=args.port,
        username=args.user,
        password=password,
        timeout=25,
        look_for_keys=False,
        allow_agent=False,
    )
    sftp = ssh.open_sftp()
    try:
        for app, archive_path in archives.items():
            remote_archive = f"/tmp/{archive_path.name}"
            print(f"upload {app}: {archive_path.name} -> {remote_archive}")
            sftp.put(str(archive_path), remote_archive)

            command = remote_deploy_command(app, remote_archive, reload_nginx=not args.no_reload)
            stdin, stdout, stderr = ssh.exec_command(command, timeout=180)
            out = stdout.read().decode("utf-8", "replace")
            err = stderr.read().decode("utf-8", "replace")
            code = stdout.channel.recv_exit_status()
            if out:
                print(out.rstrip())
            if err:
                print(err.rstrip())
            if code != 0:
                raise RuntimeError(f"remote deploy failed for {app}: exit {code}")
    finally:
        sftp.close()
        ssh.close()


def main() -> None:
    args = parse_args()
    if not args.host:
        raise SystemExit("Missing deploy host. Pass --host or set CODE_NEST_DEPLOY_HOST.")

    stamp = time.strftime("%Y%m%d%H%M%S")

    if not args.skip_build:
        for app in args.apps:
            build_app(app)

    archives = {app: make_archive(app, stamp) for app in args.apps}
    password = os.getenv("CODE_NEST_DEPLOY_PASSWORD") or getpass.getpass(
        f"{args.user}@{args.host} password: "
    )
    deploy_archives(args, archives, password)


if __name__ == "__main__":
    main()
