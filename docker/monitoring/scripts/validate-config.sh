#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
ENV_FILE="$ROOT_DIR/.env"
ALERTMANAGER_CONFIG="$ROOT_DIR/alertmanager/alertmanager.local.yml"
CODE_NEST_TARGETS="$ROOT_DIR/targets/code-nest.local.yml"
BLACKBOX_TARGETS="$ROOT_DIR/targets/blackbox.local.yml"
SMTP_AUTH_CODE="$ROOT_DIR/secrets/qq_smtp_auth_code"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing $ENV_FILE. Copy .env.example first." >&2
  exit 1
fi

if [ ! -f "$ALERTMANAGER_CONFIG" ]; then
  echo "Missing $ALERTMANAGER_CONFIG. Copy alertmanager.yml.example first." >&2
  exit 1
fi

if [ ! -f "$CODE_NEST_TARGETS" ]; then
  echo "Missing $CODE_NEST_TARGETS. Copy code-nest.local.yml.example first." >&2
  exit 1
fi

if [ ! -f "$BLACKBOX_TARGETS" ]; then
  echo "Missing $BLACKBOX_TARGETS. Copy blackbox.local.yml.example first." >&2
  exit 1
fi

if [ ! -s "$SMTP_AUTH_CODE" ]; then
  echo "Missing or empty $SMTP_AUTH_CODE. Configure the QQ SMTP authorization code first." >&2
  exit 1
fi

set -a
. "$ENV_FILE"
set +a

docker run --rm --entrypoint /bin/promtool \
  -v "$ROOT_DIR/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  -v "$ROOT_DIR/alert_rules.yml:/etc/prometheus/rules/alert_rules.yml:ro" \
  -v "$ROOT_DIR/targets:/etc/prometheus/targets:ro" \
  "$PROMETHEUS_IMAGE" check config /etc/prometheus/prometheus.yml

docker run --rm --entrypoint /bin/promtool \
  -v "$ROOT_DIR/alert_rules.yml:/etc/prometheus/rules/alert_rules.yml:ro" \
  "$PROMETHEUS_IMAGE" check rules /etc/prometheus/rules/alert_rules.yml

docker run --rm --entrypoint /bin/amtool \
  -v "$ALERTMANAGER_CONFIG:/etc/alertmanager/alertmanager.yml:ro" \
  "$ALERTMANAGER_IMAGE" check-config /etc/alertmanager/alertmanager.yml

docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.yml" config -q
