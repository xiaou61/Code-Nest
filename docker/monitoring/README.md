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

MySQL/Redis exporters, logs, and traces belong to later phases. Do not add
their credentials to this stack until dedicated least-privilege accounts and
retention policies exist. The optional SRE webhook records incidents and
collects read-only evidence; it does not execute repairs.

## Network Boundary

The production stack uses Linux host networking so it can scrape the Java
process that intentionally listens only on `127.0.0.1:9999`. Every monitoring
service explicitly listens on loopback: Prometheus `19090`, Alertmanager
`19093` and `19094`, node-exporter `19100`, blackbox-exporter `19115`, and
Grafana `3000`. Do not expose those ports in the cloud security group or host
firewall. The public entry points remain Nginx on `80` and `81`.

Blocking `/api/actuator/` in Nginx is only one layer. It does not protect a
directly exposed backend port. Verify that the server's security group does
not allow public TCP `9999`, and keep the monitoring UI ports bound to
`127.0.0.1` as configured in `.env`. The production host already uses `9090`
for Cockpit, so `.env.example` selects local-only `19090` for Prometheus.

## Server Setup

Run these commands on the Linux server, from `docker/monitoring`:

```bash
cp .env.example .env
cp targets/code-nest.local.yml.example targets/code-nest.local.yml
cp targets/blackbox.local.yml.example targets/blackbox.local.yml
cp alertmanager/alertmanager.yml.example alertmanager/alertmanager.local.yml
mkdir -p secrets
: > secrets/sre_webhook_token
chown 65534:65534 secrets/sre_webhook_token
chmod 400 secrets/sre_webhook_token
chmod 711 secrets
```

The checked-in `.env.example` uses the registry mirror already reachable from
the production server. Verify any replacement registry before starting the
stack; a failed image pull must not be discovered during an incident.

Edit the two target files and the local Alertmanager file. `code-nest.local.yml`
must point at the application metrics endpoint on loopback. The default
`127.0.0.1:9999` is deliberate: bridge networking cannot reach an application
that only binds loopback, while host networking can. This monitoring setup is
for Linux hosts; do not use this host-network composition unchanged on Docker
Desktop.

Set the QQ mailbox and recipient in `alertmanager/alertmanager.local.yml`, then
write the QQ SMTP authorization code to the secret file without placing it in
shell history:

```bash
read -rsp 'QQ SMTP authorization code: ' code
printf '%s' "$code" > secrets/qq_smtp_auth_code
unset code
chown 65534:65534 secrets/qq_smtp_auth_code
chmod 400 secrets/qq_smtp_auth_code
chmod 600 .env
chmod 644 alertmanager/alertmanager.local.yml
```

Use QQ SMTP authorization code rather than the mailbox login password. The
authorization code and local config are ignored by git.

Alertmanager runs as UID `65534`. The two secret files are mounted individually
and must remain owned by `65534:65534` with mode `0400`; the directory is
traversable but not listable. Do not replace these protections with `644`.

## Validate And Start

```bash
chmod +x scripts/validate-config.sh
chmod +x scripts/compose.sh
./scripts/validate-config.sh
./scripts/compose.sh --env-file .env up -d
./scripts/compose.sh --env-file .env ps
```

Prometheus and Grafana bind to `127.0.0.1` by default. Access them through an
SSH tunnel or an authenticated reverse proxy; do not expose their configured
ports directly to the public internet.

## First Acceptance Drill

1. Confirm the monitoring API is ready and all Prometheus targets are `UP`:

   ```bash
   curl --fail --silent --show-error http://127.0.0.1:19090/-/ready
   curl --fail --silent --show-error http://127.0.0.1:19090/api/v1/targets
   ```

2. Trigger a named synthetic alert without stopping the only production app:

   ```bash
   ./scripts/compose.sh --env-file .env exec alertmanager \
     amtool --alertmanager.url=http://127.0.0.1:19093 alert add \
     alertname=CodeNestSreE2E severity=warning service=code-nest instance=acceptance
   ```

   The QQ recipient should receive one firing email after the configured
   `group_wait` period. Record its Alertmanager fingerprint, expire it with
   `amtool alert expire <fingerprint>`, and verify the resolved email.

3. When the optional SRE webhook is enabled, verify the same synthetic alert
   creates exactly one incident and its evidence through the administrator API.
   Repeat the alert once to verify that the fingerprint is deduplicated.

## Operational Notes

- Alert thresholds are intentionally conservative starting points. Tune them after two weeks of baseline data.
- Disk rules ignore Podman's overlay mirror mount because it reports the same backing filesystem as `/`; the root filesystem remains monitored.
- A stack on the same server cannot detect complete server loss. Add an external uptime monitor or a second monitoring node before claiming host-level 24x7 coverage.
- Nginx now rejects `/api/actuator/`; Prometheus scrapes the backend directly over host loopback.

## Optional SRE Event Ingestion

The Java `xiaou-sre` module and its Outbox worker are opt-in. QQ email delivery does
not depend on them. Before enabling the worker on the application server:

1. Apply `sql/v2.5.0/sre_incident.sql`, or apply the incremental
   `sql/v2.5.0/sre_incident_evidence.sql` when the four original SRE tables already exist.
2. Apply `sql/v2.5.0/sre_investigation_run.sql` before deploying a backend that exposes
   persistent RCA history and feedback. The migration is idempotent and creates the run,
   step, and append-only feedback tables. Re-run it when upgrading from the earlier v2.5.0
   investigation schema.
3. For the administrator RCA evaluation workbench, apply
   `sql/v2.5.0/sre_rca_evaluation.sql` and then
   `sql/v2.5.0/sre_rca_evaluation_suite.sql`. The second migration creates stable suites,
   immutable versions and ordered members, then extends evaluation runs with frozen gate
   policy and results. Apply it once after the base evaluation migration; its `ALTER TABLE`
   statements are not rerunnable.
4. Set a dedicated `XIAOU_SRE_WEBHOOK_TOKEN`; do not reuse an administrator token.
   Put the identical value in `secrets/sre_webhook_token` with owner `65534:65534`
   and mode `0400`.
5. Replace the email-only local config with the webhook template, then edit the
   QQ sender and recipient values:

   ```bash
   cp alertmanager/alertmanager.sre-webhook.yml.example alertmanager/alertmanager.local.yml
   read -rsp 'SRE webhook token: ' token
   printf '%s' "$token" > secrets/sre_webhook_token
   unset token
   chown 65534:65534 secrets/sre_webhook_token
   chmod 400 secrets/sre_webhook_token
   chmod 644 alertmanager/alertmanager.local.yml
   ```

   The template sends every alert independently to QQ and the private Java
   endpoint `http://127.0.0.1:9999/api/internal/sre/alertmanager/v1/alerts`.
   Do not publish that path through Nginx or the public firewall.
6. Enable `XIAOU_SRE_WEBHOOK_ENABLED=true` only after the Alertmanager receiver is
   configured to call the backend directly over the monitoring path.
7. Enable `XIAOU_SRE_OUTBOX_ENABLED=true` only after the evidence table migration succeeds.

Before releasing RCA changes, run `./scripts/code-nest-eval.ps1 -Tier sre`. This is a
deterministic gate over synthetic, redacted fixtures; it does not read production data or
call an online model. Validate the configured online model separately through an explicit
administrator suite replay.

For Prometheus evidence on the current production host, set
`XIAOU_SRE_PROMETHEUS_ENDPOINT=http://127.0.0.1:19090` in the application
environment before enabling `XIAOU_SRE_PROMETHEUS_ENABLED=true`.

The first worker stores an `ALERT_SNAPSHOT` evidence record and can optionally collect
read-only Prometheus/Loki evidence. Prometheus and Loki are disabled by default in the
application configuration. Enable `XIAOU_SRE_PROMETHEUS_ENABLED` only after the
Prometheus API is restricted to the monitoring network; enable `XIAOU_SRE_LOKI_ENABLED`
only after an Alloy/Loki deployment and its retention policy are verified. Neither
collector executes remediation commands, and an unavailable evidence source is recorded
as a bounded unavailable snapshot instead of blocking the QQ alert path.
