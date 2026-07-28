#!/usr/bin/env bash
set -Eeuo pipefail

APP_ROOT="${CODE_NEST_APP_ROOT:-/opt/code-nest}"
APP_DIR="${CODE_NEST_APP_DIR:-$APP_ROOT/app}"
OPS_ROOT="${CODE_NEST_OPS_ROOT:-$APP_ROOT/ops}"
MONITORING_DIR="${CODE_NEST_MONITORING_DIR:-$APP_ROOT/monitoring}"
BIN_DIR="${CODE_NEST_BIN_DIR:-$APP_ROOT/bin}"
SYSTEMD_DIR="${CODE_NEST_SYSTEMD_DIR:-/etc/systemd/system}"
NGINX_CONFIG="${CODE_NEST_NGINX_CONFIG:-/etc/nginx/conf.d/code-nest.conf}"
SERVICE_NAME="${CODE_NEST_SERVICE_NAME:-code-nest.service}"
NGINX_SERVICE_NAME="${CODE_NEST_NGINX_SERVICE_NAME:-nginx.service}"
HEALTH_URL="${CODE_NEST_HEALTH_URL:-http://127.0.0.1:9999/api/actuator/health}"
USER_BASE_URL="${CODE_NEST_USER_BASE_URL:-http://127.0.0.1:81}"
ADMIN_BASE_URL="${CODE_NEST_ADMIN_BASE_URL:-http://127.0.0.1:82}"
PROMETHEUS_URL="${CODE_NEST_PROMETHEUS_URL:-http://127.0.0.1:19090}"
GRAFANA_URL="${CODE_NEST_GRAFANA_URL:-http://127.0.0.1:3000}"
GRAFANA_REQUIRED="${CODE_NEST_GRAFANA_REQUIRED:-false}"
EXPECTED_VERSION="${CODE_NEST_EXPECTED_VERSION:-}"
EXPECTED_SHA="${CODE_NEST_EXPECTED_SHA:-}"
MIN_FREE_GB="${CODE_NEST_MIN_FREE_GB:-8}"
MIN_PROMETHEUS_TARGETS="${CODE_NEST_MIN_PROMETHEUS_TARGETS:-4}"
DISK_PATH="${CODE_NEST_DISK_PATH:-$APP_ROOT}"
RELEASE_FILE="${CODE_NEST_RELEASE_FILE:-$APP_DIR/RELEASE}"
EXPECTED_SECRET_UID="${CODE_NEST_EXPECTED_SECRET_UID:-65534}"
EXPECTED_SECRET_GID="${CODE_NEST_EXPECTED_SECRET_GID:-65534}"

failures=0
warnings=0

usage() {
  cat <<'USAGE'
Usage: verify-production-baseline.sh [options]

Options:
  --require-grafana          Fail when Grafana is unavailable.
  --expected-version VALUE   Require RELEASE version to match VALUE.
  --expected-sha VALUE       Require RELEASE SHA to start with VALUE.
  --min-free-gb VALUE        Require at least VALUE GiB free (default: 8).
  -h, --help                 Show this help.
USAGE
}

pass() {
  printf 'PASS check=%s %s\n' "$1" "${2:-}"
}

fail() {
  failures=$((failures + 1))
  printf 'FAIL check=%s %s\n' "$1" "${2:-}" >&2
}

warn() {
  warnings=$((warnings + 1))
  printf 'WARN check=%s %s\n' "$1" "${2:-}" >&2
}

require_commands() {
  local command_name
  for command_name in systemctl curl jq df stat cmp awk; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
      fail dependency "missing=$command_name"
    fi
  done
}

check_service() {
  local check_name="$1"
  local service_name="$2"
  if systemctl is-active --quiet "$service_name"; then
    pass "$check_name" "service=$service_name state=active"
  else
    fail "$check_name" "service=$service_name state=inactive"
  fi
}

check_http_status() {
  local check_name="$1"
  local url="$2"
  local expected_status="$3"
  local actual_status

  actual_status="$(curl -sS -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || true)"
  if [[ "$actual_status" == "$expected_status" ]]; then
    pass "$check_name" "status=$actual_status url=$url"
  else
    fail "$check_name" "expected=$expected_status actual=${actual_status:-unreachable} url=$url"
  fi
}

check_health() {
  local response
  if response="$(curl -fsS "$HEALTH_URL" 2>/dev/null)" \
      && jq -e '.status == "UP"' >/dev/null 2>&1 <<<"$response"; then
    pass application_health "status=UP url=$HEALTH_URL"
  else
    fail application_health "url=$HEALTH_URL"
  fi
}

check_prometheus_targets() {
  local response
  if response="$(curl -fsS "$PROMETHEUS_URL/api/v1/targets" 2>/dev/null)" \
      && jq -e --argjson minimum "$MIN_PROMETHEUS_TARGETS" '
        .status == "success"
        and (.data.activeTargets | length) >= $minimum
        and all(.data.activeTargets[]; .health == "up")
      ' >/dev/null 2>&1 <<<"$response"; then
    pass prometheus_targets "minimum=$MIN_PROMETHEUS_TARGETS state=all_up"
  else
    fail prometheus_targets "minimum=$MIN_PROMETHEUS_TARGETS url=$PROMETHEUS_URL"
  fi
}

check_grafana() {
  local response
  if response="$(curl -fsS "$GRAFANA_URL/api/health" 2>/dev/null)" \
      && jq -e '.database == "ok"' >/dev/null 2>&1 <<<"$response"; then
    pass grafana_health "database=ok url=$GRAFANA_URL"
    return
  fi

  if [[ "$GRAFANA_REQUIRED" == "true" ]]; then
    fail grafana_health "required=true url=$GRAFANA_URL"
  else
    warn grafana_health "required=false url=$GRAFANA_URL"
  fi
}

release_value() {
  local key="$1"
  awk -F= -v key="$key" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "$RELEASE_FILE"
}

check_release() {
  local actual_version
  local actual_sha

  if [[ ! -f "$RELEASE_FILE" ]]; then
    fail release_metadata "missing=$RELEASE_FILE"
    return
  fi

  actual_version="$(release_value version)"
  actual_sha="$(release_value sha)"
  if [[ -z "$actual_version" || ! "$actual_sha" =~ ^[0-9a-fA-F]{7,64}$ ]]; then
    fail release_metadata "invalid=$RELEASE_FILE"
    return
  fi

  if [[ -n "$EXPECTED_VERSION" && "$actual_version" != "$EXPECTED_VERSION" ]]; then
    fail release_version "expected=$EXPECTED_VERSION actual=$actual_version"
  else
    pass release_version "actual=$actual_version"
  fi

  if [[ -n "$EXPECTED_SHA" && "$actual_sha" != "$EXPECTED_SHA"* ]]; then
    fail release_sha "expected=$EXPECTED_SHA actual=$actual_sha"
  else
    pass release_sha "actual=$actual_sha"
  fi
}

check_mode() {
  local path="$1"
  local expected_mode="$2"
  local actual_mode

  if [[ ! -e "$path" ]]; then
    fail secret_permissions "missing=$path"
    return
  fi
  actual_mode="$(stat -c %a "$path" 2>/dev/null || true)"
  if [[ "$actual_mode" == "$expected_mode" ]]; then
    pass secret_permissions "mode=$actual_mode path=$path"
  else
    fail secret_permissions "expected_mode=$expected_mode actual_mode=${actual_mode:-unknown} path=$path"
  fi
}

check_secret_owner() {
  local path="$1"
  local actual_uid
  local actual_gid

  if [[ ! -f "$path" ]]; then
    return
  fi
  actual_uid="$(stat -c %u "$path" 2>/dev/null || true)"
  actual_gid="$(stat -c %g "$path" 2>/dev/null || true)"
  if [[ "$actual_uid" == "$EXPECTED_SECRET_UID" && "$actual_gid" == "$EXPECTED_SECRET_GID" ]]; then
    pass secret_owner "uid=$actual_uid gid=$actual_gid path=$path"
  else
    fail secret_owner "expected=$EXPECTED_SECRET_UID:$EXPECTED_SECRET_GID actual=${actual_uid:-unknown}:${actual_gid:-unknown} path=$path"
  fi
}

check_grafana_credentials() {
  local env_file="$MONITORING_DIR/.env"
  local password

  if [[ ! -f "$env_file" ]]; then
    fail grafana_credentials "missing=$env_file"
    return
  fi
  password="$(awk -F= '
    $1 == "GRAFANA_ADMIN_PASSWORD" {
      sub(/^[^=]*=/, "")
      sub(/\r$/, "")
      print
      exit
    }
  ' "$env_file")"
  case "$password" in
    "" | admin | changeme | change-me | change-this-before-starting | password)
      fail grafana_credentials "configured=false"
      return
      ;;
  esac
  if ((${#password} < 16)); then
    fail grafana_credentials "configured=true minimum_length=16"
    return
  fi
  pass grafana_credentials "configured=true"
}

check_secrets() {
  local secret_dir="$MONITORING_DIR/secrets"
  local smtp_secret="$secret_dir/qq_smtp_auth_code"
  local webhook_secret="$secret_dir/sre_webhook_token"

  check_mode "$secret_dir" 711
  check_mode "$MONITORING_DIR/.env" 600
  check_mode "$smtp_secret" 400
  check_mode "$webhook_secret" 400
  check_secret_owner "$smtp_secret"
  check_secret_owner "$webhook_secret"
  check_grafana_credentials

  if [[ ! -s "$smtp_secret" ]]; then
    fail secret_content "empty=$smtp_secret"
  fi
  if [[ ! -s "$webhook_secret" ]]; then
    fail secret_content "empty=$webhook_secret"
  fi
}

check_disk_capacity() {
  local available_kb
  local required_kb

  if [[ ! "$MIN_FREE_GB" =~ ^[0-9]+$ ]]; then
    fail disk_capacity "invalid_min_free_gb=$MIN_FREE_GB"
    return
  fi
  available_kb="$(df -Pk "$DISK_PATH" 2>/dev/null | awk 'NR == 2 { print $4 }')"
  required_kb=$((MIN_FREE_GB * 1024 * 1024))
  if [[ "$available_kb" =~ ^[0-9]+$ ]] && ((available_kb >= required_kb)); then
    pass disk_capacity "available_kb=$available_kb required_kb=$required_kb path=$DISK_PATH"
  else
    fail disk_capacity "available_kb=${available_kb:-unknown} required_kb=$required_kb path=$DISK_PATH"
  fi
}

check_drift_file() {
  local expected="$1"
  local active="$2"

  if [[ ! -f "$expected" ]]; then
    fail config_drift "missing_expected=$expected"
  elif [[ ! -f "$active" ]]; then
    fail config_drift "missing_active=$active"
  elif cmp -s "$expected" "$active"; then
    pass config_drift "target=$active"
  else
    fail config_drift "expected=$expected target=$active"
  fi
}

check_managed_config_drift() {
  check_drift_file "$OPS_ROOT/nginx/code-nest-production.conf" "$NGINX_CONFIG"
  check_drift_file "$OPS_ROOT/monitoring/docker-compose.yml" "$MONITORING_DIR/docker-compose.yml"
  check_drift_file "$OPS_ROOT/monitoring/prometheus.yml" "$MONITORING_DIR/prometheus.yml"
  check_drift_file "$OPS_ROOT/monitoring/alert_rules.yml" "$MONITORING_DIR/alert_rules.yml"
  check_drift_file "$OPS_ROOT/monitoring/grafana/provisioning/datasources/prometheus.yml" \
    "$MONITORING_DIR/grafana/provisioning/datasources/prometheus.yml"
  check_drift_file "$OPS_ROOT/monitoring/grafana/provisioning/dashboards/code-nest.yml" \
    "$MONITORING_DIR/grafana/provisioning/dashboards/code-nest.yml"
  check_drift_file "$OPS_ROOT/monitoring/grafana/dashboards/code-nest-application.json" \
    "$MONITORING_DIR/grafana/dashboards/code-nest-application.json"
  check_drift_file "$OPS_ROOT/monitoring/grafana/dashboards/code-nest-sre.json" \
    "$MONITORING_DIR/grafana/dashboards/code-nest-sre.json"
  check_drift_file "$OPS_ROOT/monitoring/scripts/compose.sh" \
    "$MONITORING_DIR/scripts/compose.sh"
  check_drift_file "$OPS_ROOT/monitoring/scripts/validate-config.sh" \
    "$MONITORING_DIR/scripts/validate-config.sh"
  check_drift_file "$OPS_ROOT/systemd/code-nest-capacity-governance.service" \
    "$SYSTEMD_DIR/code-nest-capacity-governance.service"
  check_drift_file "$OPS_ROOT/systemd/code-nest-capacity-governance.timer" \
    "$SYSTEMD_DIR/code-nest-capacity-governance.timer"
  check_drift_file "$OPS_ROOT/scripts/server-capacity-governance.sh" \
    "$BIN_DIR/server-capacity-governance.sh"
  check_drift_file "$OPS_ROOT/scripts/sre-alertmanager-e2e.py" \
    "$BIN_DIR/sre-alertmanager-e2e.py"
  check_drift_file "$OPS_ROOT/scripts/verify-production-baseline.sh" \
    "$BIN_DIR/verify-production-baseline.sh"
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --require-grafana)
        GRAFANA_REQUIRED=true
        shift
        ;;
      --expected-version)
        [[ $# -ge 2 ]] || { usage >&2; exit 2; }
        EXPECTED_VERSION="$2"
        shift 2
        ;;
      --expected-sha)
        [[ $# -ge 2 ]] || { usage >&2; exit 2; }
        EXPECTED_SHA="$2"
        shift 2
        ;;
      --min-free-gb)
        [[ $# -ge 2 ]] || { usage >&2; exit 2; }
        MIN_FREE_GB="$2"
        shift 2
        ;;
      -h | --help)
        usage
        exit 0
        ;;
      *)
        usage >&2
        exit 2
        ;;
    esac
  done
}

main() {
  parse_args "$@"
  require_commands
  if ((failures > 0)); then
    fail production_baseline "dependency_check_failed=true"
    exit 1
  fi

  check_service application_service "$SERVICE_NAME"
  check_service nginx_service "$NGINX_SERVICE_NAME"
  check_health
  check_http_status user_frontend "$USER_BASE_URL/" 200
  check_http_status admin_frontend "$ADMIN_BASE_URL/" 200

  local base_url
  for base_url in "$USER_BASE_URL" "$ADMIN_BASE_URL"; do
    check_http_status public_actuator_boundary "$base_url/api/actuator" 404
    check_http_status public_actuator_boundary "$base_url/api/actuator/health" 404
    check_http_status public_sre_boundary "$base_url/api/internal/sre" 404
    check_http_status public_sre_boundary "$base_url/api/internal/sre/alertmanager/v1/alerts" 404
  done

  check_prometheus_targets
  check_grafana
  check_release
  check_secrets
  check_disk_capacity
  check_managed_config_drift

  if ((failures > 0)); then
    printf 'FAIL check=production_baseline failures=%s warnings=%s\n' "$failures" "$warnings" >&2
    exit 1
  fi
  printf 'PASS check=production_baseline failures=0 warnings=%s\n' "$warnings"
}

main "$@"
