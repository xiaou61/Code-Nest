# Single-Server Monitoring

This directory is the production baseline for one Code-Nest server. It runs
Prometheus, Alertmanager, Grafana, node-exporter, and blackbox-exporter. It is
intentionally read-only: it detects, records, and notifies; it does not restart
containers, alter databases, or execute AI-proposed actions.

## What It Covers

- Spring Boot Actuator and Micrometer metrics
- Host CPU and disk capacity
- Public HTTP availability from a blackbox probe
- HTTP error ratio, latency, and JVM heap
- QQ mail delivery for warning and critical alerts

MySQL/Redis exporters, logs, traces, incident workflow, and AI RCA belong to
later phases. Do not add their credentials to this stack until dedicated
least-privilege accounts and retention policies exist.

## Network Boundary

Before starting, close public access to TCP `9999`, `3000`, `9090`, `9093`,
`9100`, and `9115` in the cloud security group and host firewall. The public
entry points are Nginx on `80` and `81`; Nginx proxies business traffic to the
backend, while Prometheus reaches the backend through Docker's host gateway.

Blocking `/api/actuator/` in Nginx is only one layer. It does not protect a
directly exposed backend port. Verify that the server's security group does
not allow public TCP `9999`, and keep the monitoring UI ports bound to
`127.0.0.1` as configured in `.env`.

## Server Setup

Run these commands on the Linux server, from `docker/monitoring`:

```bash
cp .env.example .env
cp targets/code-nest.local.yml.example targets/code-nest.local.yml
cp targets/blackbox.local.yml.example targets/blackbox.local.yml
cp alertmanager/alertmanager.yml.example alertmanager/alertmanager.local.yml
mkdir -p secrets
chmod 700 secrets
```

Edit the two target files and the local Alertmanager file. `code-nest.local.yml`
must point at the application metrics endpoint from the monitoring Docker
network. For a host-exposed application, `host.docker.internal:9999` is the
default; Docker Compose maps that name to the Linux host gateway.

Set the QQ mailbox and recipient in `alertmanager/alertmanager.local.yml`, then
write the QQ SMTP authorization code to the secret file without placing it in
shell history:

```bash
read -rsp 'QQ SMTP authorization code: ' code
printf '%s' "$code" > secrets/qq_smtp_auth_code
unset code
chmod 600 secrets/qq_smtp_auth_code alertmanager/alertmanager.local.yml .env
```

Use QQ SMTP authorization code rather than the mailbox login password. The
authorization code and local config are ignored by git.

## Validate And Start

```bash
chmod +x scripts/validate-config.sh
./scripts/validate-config.sh
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

Prometheus and Grafana bind to `127.0.0.1` by default. Access them through an
SSH tunnel or an authenticated reverse proxy; do not expose ports 9090 or 3000
directly to the public internet.

## First Acceptance Drill

1. Confirm all Prometheus targets are `UP`.
2. Trigger a test alert with the Alertmanager UI/API only after the QQ sender is configured.
3. Stop the application in a staging window and verify `CodeNestTargetDown` and the public probe alert arrive once, then resolve after recovery.
4. Restore the application and confirm the resolved email arrives.

## Operational Notes

- Alert thresholds are intentionally conservative starting points. Tune them after two weeks of baseline data.
- A stack on the same server cannot detect complete server loss. Add an external uptime monitor or a second monitoring node before claiming host-level 24x7 coverage.
- Nginx now rejects `/api/actuator/`; Prometheus must scrape the backend directly over the host/Docker monitoring path.

## Optional SRE Event Ingestion

The Java `xiaou-sre` module and its Outbox worker are opt-in. QQ email delivery does
not depend on them. Before enabling the worker on the application server:

1. Apply `sql/v2.5.0/sre_incident.sql`, or apply the incremental
   `sql/v2.5.0/sre_incident_evidence.sql` when the four original SRE tables already exist.
2. Set a dedicated `XIAOU_SRE_WEBHOOK_TOKEN`; do not reuse an administrator token.
3. Enable `XIAOU_SRE_WEBHOOK_ENABLED=true` only after the Alertmanager receiver is
   configured to call the backend directly over the monitoring path.
4. Enable `XIAOU_SRE_OUTBOX_ENABLED=true` only after the evidence table migration succeeds.

The first worker stores an `ALERT_SNAPSHOT` evidence record and can optionally collect
read-only Prometheus/Loki evidence. Prometheus and Loki are disabled by default in the
application configuration. Enable `XIAOU_SRE_PROMETHEUS_ENABLED` only after the
Prometheus API is restricted to the monitoring network; enable `XIAOU_SRE_LOKI_ENABLED`
only after an Alloy/Loki deployment and its retention policy are verified. Neither
collector executes remediation commands, and an unavailable evidence source is recorded
as a bounded unavailable snapshot instead of blocking the QQ alert path.

When the Outbox worker is enabled, its own queue and processing health is available from the
same backend scrape endpoint. Watch `xiaou_sre_outbox_pending`,
`xiaou_sre_outbox_processing`, `xiaou_sre_outbox_lease_recoveries_total`,
`xiaou_sre_outbox_events_total{outcome="failed"}`, and the P95 of
`xiaou_sre_outbox_event_duration_seconds`. The same endpoint exposes
`xiaou_sre_alerts_ingested_total`, `xiaou_sre_alerts_duplicates_total`,
`xiaou_sre_alerts_ingestion_errors_total`, `xiaou_sre_incidents_open`, and evidence collection
success/error and duration metrics. Metric labels intentionally exclude incident IDs and payload
values and are restricted to bounded status, severity, source, event type, and outcome values.
