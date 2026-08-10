# Database migrations

Incremental SQL lives under `sql/v*/`. Apply it through
`scripts/db-migrate.py`, which records the migration key and SHA-256 checksum
in `code_nest_schema_migration`.

```bash
# CI and release preflight (read-only)
python scripts/db-migrate.py --dry-run

# New database or an explicitly verified database
python scripts/db-migrate.py --apply

# Only after reviewing a FAILED row and the database's partial DDL state
python scripts/db-migrate.py --apply --retry-failed

# Existing database that was upgraded manually before the ledger existed
python scripts/db-migrate.py --baseline --baseline-to v2.5.2
python scripts/db-migrate.py --apply
```

For the `v2.5.10` release from a `v2.5.8` baseline, the runner applies
`sql/v2.5.9/admin_agent_tasks.sql` before
`sql/v2.5.10/admin_agent_workflow_control_plane.sql`. Keep the administrator
agent task Worker disabled until both migrations and the task-event metrics are
verified.

Do not edit an applied SQL file. Add a new version directory instead. The
runner stops on checksum drift or the first failed migration and records the
failure. A failed row blocks future applies until an operator explicitly uses
`--retry-failed` after reviewing partial DDL state. MySQL
`GET_LOCK` serializes concurrent operators so two release jobs cannot execute
the same migration at once. DDL is kept expand/contract compatible so the
previous application can run while a new column or index is being introduced.
