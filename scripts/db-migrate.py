#!/usr/bin/env python3
"""Apply Code-Nest SQL migrations with an auditable checksum ledger.

The repository keeps historical migrations in sql/v*/. This runner adds the
missing execution contract without rewriting those files. Use --dry-run in CI,
--baseline once for an already provisioned database, and --apply for writes.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import re
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import parse_qs, urlparse


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MIGRATIONS_DIR = ROOT / "sql"
LEDGER_TABLE = "code_nest_schema_migration"
MIGRATION_LOCK_NAME = "code_nest_schema_migration_lock"
COMPLETED_STATUSES = frozenset({"APPLIED", "BASELINED"})


@dataclass(frozen=True)
class Migration:
    version: str
    name: str
    path: Path
    checksum: str

    @property
    def key(self) -> str:
        return f"{self.version}/{self.name}"


def discover_migrations(directory: Path) -> list[Migration]:
    migrations: list[Migration] = []
    for path in sorted(directory.glob("v*/*.sql"), key=lambda item: (version_sort_key(item.parent.name), item.name)):
        version = path.parent.name
        name = path.name
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        migrations.append(Migration(version, name, path, digest))
    return migrations


def version_sort_key(value: str) -> tuple[tuple[int, ...], str]:
    """Sort semantic migration directories numerically, not lexicographically."""
    numbers = tuple(int(part) for part in re.findall(r"\d+", value))
    return numbers, value


def split_sql(content: str) -> list[str]:
    statements: list[str] = []
    start = 0
    quote: str | None = None
    index = 0
    while index < len(content):
        char = content[index]
        if quote:
            if char == "\\":
                index += 2
                continue
            if char == quote:
                if index + 1 < len(content) and content[index + 1] == quote:
                    index += 2
                    continue
                quote = None
            index += 1
            continue
        if char in ("'", '"', "`"):
            quote = char
            index += 1
            continue
        if char == ";":
            statement = strip_leading_comments(content[start:index])
            if statement:
                statements.append(statement)
            start = index + 1
        index += 1
    tail = strip_leading_comments(content[start:])
    if tail:
        statements.append(tail)
    return statements


def strip_leading_comments(value: str) -> str:
    lines = value.strip().splitlines()
    while lines and (not lines[0].strip() or lines[0].lstrip().startswith("--") or lines[0].lstrip().startswith("#")):
        lines.pop(0)
    return "\n".join(lines).strip()


def parse_jdbc_url(value: str) -> tuple[str, int, str]:
    match = re.match(r"^jdbc:mysql://([^/:?#]+)(?::(\d+))?/([^?]+)", value)
    if not match:
        raise SystemExit(f"unsupported MySQL URL: {value}")
    return match.group(1), int(match.group(2) or 3306), match.group(3)


def connection_args(args: argparse.Namespace) -> dict[str, object]:
    jdbc_url = args.url or os.getenv("XIAOU_MYSQL_URL")
    host = args.host or os.getenv("XIAOU_MYSQL_HOST")
    port = args.port or int(os.getenv("XIAOU_MYSQL_PORT", "3306"))
    database = args.database or os.getenv("XIAOU_MYSQL_DATABASE")
    if jdbc_url:
        host, port, database = parse_jdbc_url(jdbc_url)
    return {
        "host": host or "127.0.0.1",
        "port": port,
        "user": args.user or os.getenv("XIAOU_MYSQL_USERNAME", "root"),
        "password": args.password if args.password is not None else os.getenv("XIAOU_MYSQL_PASSWORD", ""),
        "database": database or "code_nest",
        "autocommit": False,
        "charset": "utf8mb4",
    }


def ensure_ledger(connection) -> None:
    connection.cursor().execute(
        f"""
        CREATE TABLE IF NOT EXISTS `{LEDGER_TABLE}` (
            `id` BIGINT NOT NULL AUTO_INCREMENT,
            `migration_key` VARCHAR(255) NOT NULL,
            `version` VARCHAR(64) NOT NULL,
            `name` VARCHAR(190) NOT NULL,
            `checksum` CHAR(64) NOT NULL,
            `status` VARCHAR(16) NOT NULL DEFAULT 'APPLIED',
            `execution_ms` BIGINT NOT NULL DEFAULT 0,
            `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `error_message` VARCHAR(500) NULL,
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_schema_migration_key` (`migration_key`),
            KEY `idx_schema_migration_version` (`version`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """
    )
    connection.commit()


def applied_migrations(connection) -> dict[str, tuple[str, str]]:
    cursor = connection.cursor()
    cursor.execute(f"SELECT migration_key, checksum, status FROM `{LEDGER_TABLE}`")
    return {row[0]: (row[1], row[2]) for row in cursor.fetchall()}


def validate_checksums(migrations: list[Migration], applied: dict[str, tuple[str, str]]) -> None:
    for migration in migrations:
        record = applied.get(migration.key)
        if record and record[0] != migration.checksum:
            raise SystemExit(f"checksum mismatch for {migration.key}; restore the original file or repair explicitly")


def baseline(connection, migrations: list[Migration], target: str | None) -> int:
    applied = applied_migrations(connection)
    pending = [
        migration for migration in migrations
        if migration.key not in applied or applied[migration.key][1] not in COMPLETED_STATUSES
    ]
    if target:
        target_key = version_sort_key(target)
        pending = [migration for migration in pending if version_sort_key(migration.version) <= target_key]
    cursor = connection.cursor()
    for migration in pending:
        cursor.execute(
            f"INSERT INTO `{LEDGER_TABLE}` (migration_key, version, name, checksum, status, error_message) "
            "VALUES (%s, %s, %s, %s, 'BASELINED', 'marked existing by operator') "
            "ON DUPLICATE KEY UPDATE status='BASELINED', error_message='marked existing by operator', "
            "checksum=VALUES(checksum), applied_at=CURRENT_TIMESTAMP",
            (migration.key, migration.version, migration.name, migration.checksum),
        )
    connection.commit()
    print(f"baselined {len(pending)} migrations")
    return 0


def apply_migrations(connection, migrations: list[Migration], dry_run: bool, retry_failed: bool = False) -> int:
    applied = applied_migrations(connection)
    validate_checksums(migrations, applied)
    incomplete = [key for key, (_, status) in applied.items() if status not in COMPLETED_STATUSES]
    if incomplete and not retry_failed:
        raise SystemExit(
            "incomplete migrations require manual review before retry: "
            + ", ".join(sorted(incomplete))
            + "; rerun with --retry-failed only after verifying the database"
        )
    pending = [
        migration for migration in migrations
        if migration.key not in applied or applied[migration.key][1] not in COMPLETED_STATUSES
    ]
    if not pending:
        print("database schema is up to date")
        return 0
    for migration in pending:
        print(f"pending: {migration.key} sha256={migration.checksum}")
    if dry_run:
        return 0

    cursor = connection.cursor()
    for migration in pending:
        started = time.monotonic()
        try:
            cursor.execute(
                f"INSERT INTO `{LEDGER_TABLE}` (migration_key, version, name, checksum, status, error_message) "
                "VALUES (%s, %s, %s, %s, 'RUNNING', NULL) "
                "ON DUPLICATE KEY UPDATE status='RUNNING', error_message=NULL, checksum=VALUES(checksum), "
                "applied_at=CURRENT_TIMESTAMP",
                (migration.key, migration.version, migration.name, migration.checksum),
            )
            for statement in split_sql(migration.path.read_text(encoding="utf-8")):
                cursor.execute(statement)
            elapsed = int((time.monotonic() - started) * 1000)
            cursor.execute(
                f"UPDATE `{LEDGER_TABLE}` SET status='APPLIED', execution_ms=%s, error_message=NULL, "
                "checksum=%s, applied_at=CURRENT_TIMESTAMP WHERE migration_key=%s",
                (elapsed, migration.checksum, migration.key),
            )
            connection.commit()
            print(f"applied: {migration.key} ({elapsed} ms)")
        except Exception as exc:
            connection.rollback()
            message = str(exc)[:500]
            cursor.execute(
                f"INSERT INTO `{LEDGER_TABLE}` (migration_key, version, name, checksum, status, error_message) "
                "VALUES (%s, %s, %s, %s, 'FAILED', %s) "
                "ON DUPLICATE KEY UPDATE status='FAILED', error_message=VALUES(error_message), "
                "checksum=VALUES(checksum), applied_at=CURRENT_TIMESTAMP",
                (migration.key, migration.version, migration.name, migration.checksum, message),
            )
            connection.commit()
            raise SystemExit(f"migration failed: {migration.key}: {message}") from exc
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Apply Code-Nest MySQL migrations with checksums.")
    parser.add_argument("--dry-run", action="store_true", help="List pending migrations without connecting or writing.")
    parser.add_argument("--apply", action="store_true", help="Apply pending migrations.")
    parser.add_argument(
        "--retry-failed",
        action="store_true",
        help="Retry FAILED ledger rows after an operator has verified partial DDL state.",
    )
    parser.add_argument("--baseline", action="store_true", help="Mark existing migrations as applied without executing SQL.")
    parser.add_argument("--baseline-to", default=None, help="Only baseline migrations up to this version, e.g. v2.5.2.")
    parser.add_argument("--migrations-dir", type=Path, default=DEFAULT_MIGRATIONS_DIR)
    parser.add_argument("--url", default=None)
    parser.add_argument("--host", default=None)
    parser.add_argument("--port", type=int, default=None)
    parser.add_argument("--user", default=None)
    parser.add_argument("--password", default=None)
    parser.add_argument("--database", default=None)
    return parser.parse_args()


def acquire_migration_lock(connection) -> None:
    cursor = connection.cursor()
    cursor.execute("SELECT GET_LOCK(%s, 60)", (MIGRATION_LOCK_NAME,))
    acquired = cursor.fetchone()[0]
    if acquired != 1:
        raise SystemExit("could not acquire database migration lock within 60 seconds")


def release_migration_lock(connection) -> None:
    cursor = connection.cursor()
    cursor.execute("SELECT RELEASE_LOCK(%s)", (MIGRATION_LOCK_NAME,))


def main() -> int:
    args = parse_args()
    if not args.dry_run and not args.apply and not args.baseline:
        raise SystemExit("choose --dry-run, --apply, or --baseline")
    migrations = discover_migrations(args.migrations_dir)
    if not migrations:
        raise SystemExit(f"no migrations found under {args.migrations_dir}")
    if args.dry_run:
        for migration in migrations:
            print(f"available: {migration.key} sha256={migration.checksum}")
        return 0

    try:
        import pymysql
    except ImportError as exc:
        raise SystemExit("pymysql is required for --apply/--baseline: python -m pip install pymysql") from exc

    connection = pymysql.connect(**connection_args(args))
    lock_acquired = False
    try:
        acquire_migration_lock(connection)
        lock_acquired = True
        ensure_ledger(connection)
        if args.baseline:
            return baseline(connection, migrations, args.baseline_to)
        return apply_migrations(connection, migrations, dry_run=False, retry_failed=args.retry_failed)
    finally:
        if lock_acquired:
            release_migration_lock(connection)
        connection.close()


if __name__ == "__main__":
    sys.exit(main())
