#!/usr/bin/env bash
set -Eeuo pipefail

strip_cr() {
  printf '%s' "$1" | tr -d '\r'
}

APP_ROOT="${CODE_NEST_APP_ROOT:-/opt/code-nest}"
APP_DIR="${CODE_NEST_APP_DIR:-$APP_ROOT/app}"
BACKUP_DIR="${CODE_NEST_BACKUP_DIR:-$APP_ROOT/backups/releases}"
USER_WEB_DIR="${CODE_NEST_USER_WEB_DIR:-/var/www/code-nest-user}"
ADMIN_WEB_DIR="${CODE_NEST_ADMIN_WEB_DIR:-/var/www/code-nest-admin}"
OPS_ROOT="${CODE_NEST_OPS_ROOT:-$APP_ROOT/ops}"
MONITORING_DIR="${CODE_NEST_MONITORING_DIR:-$APP_ROOT/monitoring}"
BIN_DIR="${CODE_NEST_BIN_DIR:-$APP_ROOT/bin}"
SYSTEMD_DIR="${CODE_NEST_SYSTEMD_DIR:-/etc/systemd/system}"
NGINX_CONFIG="${CODE_NEST_NGINX_CONFIG:-/etc/nginx/conf.d/code-nest.conf}"
APP_ENV_FILE="${CODE_NEST_APP_ENV_FILE:-/etc/code-nest/code-nest.env}"
SERVICE_NAME="${CODE_NEST_SERVICE_NAME:-code-nest.service}"
CAPACITY_TIMER_NAME="${CODE_NEST_CAPACITY_TIMER_NAME:-code-nest-capacity-governance.timer}"
HEALTH_URL="${CODE_NEST_HEALTH_URL:-http://127.0.0.1:9999/api/actuator/health}"
PROMETHEUS_URL="${CODE_NEST_PROMETHEUS_URL:-http://127.0.0.1:19090}"
GRAFANA_URL="${CODE_NEST_GRAFANA_URL:-http://127.0.0.1:3000}"
RELOAD_NGINX="${CODE_NEST_RELOAD_NGINX:-true}"
MANAGE_OPERATIONS="${CODE_NEST_MANAGE_OPERATIONS:-true}"
MANAGE_MONITORING="${CODE_NEST_MANAGE_MONITORING:-true}"
VERIFY_BASELINE="${CODE_NEST_VERIFY_BASELINE:-true}"
MIN_FREE_GB="${CODE_NEST_MIN_FREE_GB:-8}"
KEEP_RELEASES="${CODE_NEST_KEEP_RELEASES:-4}"
RUN_MIGRATIONS="${CODE_NEST_RUN_MIGRATIONS:-false}"
RETRY_FAILED="${CODE_NEST_RETRY_FAILED:-false}"
RELEASE_VERSION="${CODE_NEST_RELEASE_VERSION:-unknown}"

for variable_name in \
  APP_ROOT APP_DIR BACKUP_DIR USER_WEB_DIR ADMIN_WEB_DIR OPS_ROOT MONITORING_DIR \
  BIN_DIR SYSTEMD_DIR NGINX_CONFIG APP_ENV_FILE SERVICE_NAME CAPACITY_TIMER_NAME \
  HEALTH_URL PROMETHEUS_URL GRAFANA_URL RELOAD_NGINX MANAGE_OPERATIONS \
  MANAGE_MONITORING VERIFY_BASELINE MIN_FREE_GB KEEP_RELEASES RUN_MIGRATIONS RETRY_FAILED RELEASE_VERSION; do
  printf -v "$variable_name" '%s' "$(strip_cr "${!variable_name}")"
done

MONITORING_MANAGED_FILES=(
  docker-compose.yml
  prometheus.yml
  alert_rules.yml
  grafana/provisioning/datasources/prometheus.yml
  grafana/provisioning/dashboards/code-nest.yml
  grafana/dashboards/code-nest-application.json
  grafana/dashboards/code-nest-sre.json
  scripts/compose.sh
  scripts/validate-config.sh
)

SYSTEMD_MANAGED_FILES=(
  code-nest-capacity-governance.service
  code-nest-capacity-governance.timer
)

BIN_MANAGED_FILES=(
  server-capacity-governance.sh
  sre-alertmanager-e2e.py
  verify-production-baseline.sh
)

BUNDLE_VERSION=""
BUNDLE_SHA=""
BUNDLE_BUILD_ID=""

usage() {
  cat <<'USAGE'
Usage:
  deploy-release.sh deploy <bundle.tar.gz>
  deploy-release.sh rollback <backup-dir>

Environment overrides:
  CODE_NEST_APP_ROOT=/opt/code-nest
  CODE_NEST_APP_DIR=/opt/code-nest/app
  CODE_NEST_BACKUP_DIR=/opt/code-nest/backups/releases
  CODE_NEST_USER_WEB_DIR=/var/www/code-nest-user
  CODE_NEST_ADMIN_WEB_DIR=/var/www/code-nest-admin
  CODE_NEST_OPS_ROOT=/opt/code-nest/ops
  CODE_NEST_MONITORING_DIR=/opt/code-nest/monitoring
  CODE_NEST_NGINX_CONFIG=/etc/nginx/conf.d/code-nest.conf
  CODE_NEST_APP_ENV_FILE=/etc/code-nest/code-nest.env
  CODE_NEST_SERVICE_NAME=code-nest.service
  CODE_NEST_RELOAD_NGINX=true
  CODE_NEST_MANAGE_OPERATIONS=true
  CODE_NEST_MANAGE_MONITORING=true
  CODE_NEST_VERIFY_BASELINE=true
  CODE_NEST_MIN_FREE_GB=8
  CODE_NEST_KEEP_RELEASES=4
  CODE_NEST_RUN_MIGRATIONS=false
  CODE_NEST_RETRY_FAILED=false
USAGE
}

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

fail() {
  log "ERROR: $*" >&2
  exit 64
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    log "missing required command: $1"
    exit 127
  }
}

assert_boolean() {
  local name="$1"
  local value="$2"
  [[ "$value" == "true" || "$value" == "false" ]] || fail "$name must be true or false"
}

assert_safe_app_root() {
  case "${APP_ROOT%/}" in
    "" | "/" | "/opt" | "/var" | "/var/www" | "/root" | "/home")
      fail "refusing to use unsafe app root: $APP_ROOT"
      ;;
  esac
}

is_within_app_root() {
  [[ "$1" == "${APP_ROOT%/}"/* ]]
}

assert_safe_target_dir() {
  local target_dir="$1"

  case "$target_dir" in
    /var/www/code-nest-* | "${APP_ROOT%/}"/*) ;;
    *) fail "refusing to modify unsafe target directory: $target_dir" ;;
  esac
}

assert_safe_backup_dir() {
  local backup_dir="$1"
  local backup_root="${APP_ROOT%/}/backups/releases"

  case "$backup_dir" in
    "$backup_root" | "$backup_root"/*) ;;
    *) fail "refusing to use unsafe backup directory: $backup_dir" ;;
  esac
}

assert_safe_operational_targets() {
  assert_safe_app_root
  assert_safe_target_dir "$OPS_ROOT"
  assert_safe_target_dir "$MONITORING_DIR"
  assert_safe_target_dir "$BIN_DIR"

  if [[ "$NGINX_CONFIG" != "/etc/nginx/conf.d/code-nest.conf" ]] \
      && ! is_within_app_root "$NGINX_CONFIG"; then
    fail "refusing unsafe nginx config target: $NGINX_CONFIG"
  fi
  if [[ "$SYSTEMD_DIR" != "/etc/systemd/system" ]] \
      && ! is_within_app_root "$SYSTEMD_DIR"; then
    fail "refusing unsafe systemd target: $SYSTEMD_DIR"
  fi
  if [[ "$APP_ENV_FILE" != "/etc/code-nest/code-nest.env" ]] \
      && ! is_within_app_root "$APP_ENV_FILE"; then
    fail "refusing unsafe application environment target: $APP_ENV_FILE"
  fi
}

sync_dir() {
  local source_dir="$1"
  local target_dir="$2"

  test -d "$source_dir"
  assert_safe_target_dir "$target_dir"
  mkdir -p "$target_dir"
  find "$target_dir" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
  cp -a "$source_dir"/. "$target_dir"/
}

install_file() {
  local source_file="$1"
  local target_file="$2"
  local mode="$3"

  test -f "$source_file"
  mkdir -p "$(dirname "$target_file")"
  cp -a "$source_file" "$target_file"
  chmod "$mode" "$target_file"
  chown root:root "$target_file"
}

install_deployment_helper() {
  local source_script="$1"
  local target_script="$BIN_DIR/deploy-release.sh"
  local pending_script="$BIN_DIR/deploy-release.sh.pending"

  mkdir -p "$BIN_DIR"
  cp -a "$source_script" "$pending_script"
  chmod 755 "$pending_script"
  chown root:root "$pending_script"

  if [[ "$(readlink -f "$0")" == "$(readlink -f "$target_script" 2>/dev/null || true)" ]]; then
    log "schedule deployment helper refresh"
    (
      sleep 1
      mv -f "$pending_script" "$target_script"
      chmod 755 "$target_script"
      chown root:root "$target_script"
    ) >/dev/null 2>&1 &
    return 0
  fi

  mv -f "$pending_script" "$target_script"
  chmod 755 "$target_script"
  chown root:root "$target_script"
}

backup_file() {
  local source_file="$1"
  local backup_dir="$2"
  local relative_path="$3"
  local backup_file="$backup_dir/$relative_path"

  mkdir -p "$(dirname "$backup_file")"
  if [[ -f "$source_file" ]]; then
    cp -a "$source_file" "$backup_file"
  else
    : >"$backup_file.missing"
  fi
}

backup_directory() {
  local source_dir="$1"
  local backup_dir="$2"
  local relative_path="$3"
  local target_dir="$backup_dir/$relative_path"

  if [[ -d "$source_dir" ]]; then
    mkdir -p "$target_dir"
    cp -a "$source_dir"/. "$target_dir"/
  else
    mkdir -p "$(dirname "$target_dir")"
    : >"$target_dir.missing"
  fi
}

backup_current() {
  local backup_dir="$1"
  local relative_path

  mkdir -p "$backup_dir"
  mkdir -p "$APP_DIR" "$USER_WEB_DIR" "$ADMIN_WEB_DIR"

  backup_file "$APP_DIR/app.jar" "$backup_dir" app.jar
  backup_file "$APP_DIR/RELEASE" "$backup_dir" RELEASE
  backup_file "$APP_ENV_FILE" "$backup_dir" app-env/code-nest.env

  mkdir -p "$backup_dir/user" "$backup_dir/admin"
  cp -a "$USER_WEB_DIR"/. "$backup_dir/user"/ 2>/dev/null || true
  cp -a "$ADMIN_WEB_DIR"/. "$backup_dir/admin"/ 2>/dev/null || true

  if [[ "$MANAGE_OPERATIONS" == "true" ]]; then
    backup_directory "$OPS_ROOT" "$backup_dir" ops-reference
    backup_file "$NGINX_CONFIG" "$backup_dir" ops/nginx/code-nest.conf
    for relative_path in "${MONITORING_MANAGED_FILES[@]}"; do
      backup_file "$MONITORING_DIR/$relative_path" "$backup_dir" "ops/monitoring/$relative_path"
    done
    for relative_path in "${SYSTEMD_MANAGED_FILES[@]}"; do
      backup_file "$SYSTEMD_DIR/$relative_path" "$backup_dir" "ops/systemd/$relative_path"
    done
    for relative_path in "${BIN_MANAGED_FILES[@]}"; do
      backup_file "$BIN_DIR/$relative_path" "$backup_dir" "ops/bin/$relative_path"
    done
  fi

  cat >"$backup_dir/metadata.env" <<EOF
RELEASE_VERSION=$RELEASE_VERSION
CREATED_AT=$(date -Iseconds)
APP_DIR=$APP_DIR
USER_WEB_DIR=$USER_WEB_DIR
ADMIN_WEB_DIR=$ADMIN_WEB_DIR
SERVICE_NAME=$SERVICE_NAME
HEALTH_URL=$HEALTH_URL
EOF
}

restore_file() {
  local backup_file="$1"
  local target_file="$2"

  if [[ -f "$backup_file" ]]; then
    mkdir -p "$(dirname "$target_file")"
    cp -a "$backup_file" "$target_file"
  elif [[ -f "$backup_file.missing" ]]; then
    rm -f "$target_file"
  fi
}

restore_directory() {
  local backup_dir="$1"
  local target_dir="$2"

  if [[ -d "$backup_dir" ]]; then
    sync_dir "$backup_dir" "$target_dir"
  elif [[ -f "$backup_dir.missing" ]]; then
    assert_safe_target_dir "$target_dir"
    if [[ -d "$target_dir" ]]; then
      find "$target_dir" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
      rmdir "$target_dir" 2>/dev/null || true
    fi
  fi
}

health_check() {
  local attempts="${1:-30}"
  local sleep_seconds="${2:-2}"
  local i

  for ((i = 1; i <= attempts; i++)); do
    if curl -fsS "$HEALTH_URL" >/tmp/code-nest-health.json 2>/tmp/code-nest-health.err; then
      log "health check passed: $HEALTH_URL"
      return 0
    fi
    log "health check pending ($i/$attempts)"
    sleep "$sleep_seconds"
  done

  log "health check failed: $HEALTH_URL"
  cat /tmp/code-nest-health.err >&2 || true
  return 1
}

wait_for_url() {
  local name="$1"
  local url="$2"
  local attempts="${3:-30}"
  local i

  for ((i = 1; i <= attempts; i++)); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "$name ready: $url"
      return 0
    fi
    sleep 2
  done
  log "$name readiness failed: $url"
  return 1
}

validate_nginx() {
  if [[ "$RELOAD_NGINX" != "true" ]]; then
    log "skip nginx validation and reload"
    return 0
  fi
  require_cmd nginx
  nginx -t
}

reload_nginx() {
  if [[ "$RELOAD_NGINX" != "true" ]]; then
    return 0
  fi
  validate_nginx
  systemctl reload nginx
}

validate_monitoring() {
  if [[ "$MANAGE_OPERATIONS" != "true" || "$MANAGE_MONITORING" != "true" ]]; then
    log "skip monitoring validation"
    return 0
  fi
  "$MONITORING_DIR/scripts/validate-config.sh"
}

monitoring_has_service() {
  local service_name="$1"

  "$MONITORING_DIR/scripts/compose.sh" \
    --env-file "$MONITORING_DIR/.env" \
    -f "$MONITORING_DIR/docker-compose.yml" config --services \
    | grep -Fxq "$service_name"
}

apply_monitoring() {
  if [[ "$MANAGE_OPERATIONS" != "true" || "$MANAGE_MONITORING" != "true" ]]; then
    log "skip monitoring apply"
    return 0
  fi

  "$MONITORING_DIR/scripts/compose.sh" \
    --env-file "$MONITORING_DIR/.env" \
    -f "$MONITORING_DIR/docker-compose.yml" up -d
  "$MONITORING_DIR/scripts/compose.sh" \
    --env-file "$MONITORING_DIR/.env" \
    -f "$MONITORING_DIR/docker-compose.yml" restart prometheus
  wait_for_url prometheus "$PROMETHEUS_URL/-/ready"
  if monitoring_has_service grafana; then
    wait_for_url grafana "$GRAFANA_URL/api/health"
  else
    log "skip grafana readiness: service is not present in active monitoring compose"
  fi
}

apply_systemd_units() {
  if [[ "$MANAGE_OPERATIONS" != "true" ]]; then
    return 0
  fi
  systemctl daemon-reload
  if [[ -f "$SYSTEMD_DIR/$CAPACITY_TIMER_NAME" ]]; then
    systemctl enable --now "$CAPACITY_TIMER_NAME"
  else
    systemctl disable --now "$CAPACITY_TIMER_NAME" 2>/dev/null || true
  fi
}

restart_service() {
  systemctl restart "$SERVICE_NAME"
  health_check 30 2
}

release_value() {
  local release_file="$1"
  local key="$2"
  awk -F= -v key="$key" '$1 == key { sub(/^[^=]*=/, ""); sub(/\r$/, ""); print; exit }' "$release_file"
}

load_release_metadata() {
  local release_file="$1"

  BUNDLE_VERSION="$(release_value "$release_file" version)"
  BUNDLE_SHA="$(release_value "$release_file" sha)"
  BUNDLE_BUILD_ID="$(release_value "$release_file" build_id)"
  [[ "$BUNDLE_VERSION" =~ ^[A-Za-z0-9._-]{1,64}$ ]] || fail "invalid bundle version"
  [[ "$BUNDLE_SHA" =~ ^[0-9a-fA-F]{7,64}$ ]] || fail "invalid bundle source SHA"
  [[ "$BUNDLE_BUILD_ID" =~ ^[A-Za-z0-9._:-]{1,128}$ ]] || fail "invalid bundle build ID"

  if [[ "$RELEASE_VERSION" == "unknown" ]]; then
    RELEASE_VERSION="$BUNDLE_VERSION"
  elif [[ "$RELEASE_VERSION" != "$BUNDLE_VERSION" ]]; then
    fail "release version mismatch: requested=$RELEASE_VERSION bundle=$BUNDLE_VERSION"
  fi
}

update_app_provenance() {
  local source_revision="$1"
  local build_id="$2"
  local build_version="${3#v}"
  local temp_file
  local file_mode
  local file_uid
  local file_gid

  [[ -f "$APP_ENV_FILE" ]] || fail "application environment file missing: $APP_ENV_FILE"
  temp_file="$(mktemp "${APP_ENV_FILE}.tmp.XXXXXX")"
  file_mode="$(stat -c %a "$APP_ENV_FILE")"
  file_uid="$(stat -c %u "$APP_ENV_FILE")"
  file_gid="$(stat -c %g "$APP_ENV_FILE")"

  awk \
    -v source_revision="$source_revision" \
    -v build_id="$build_id" \
    -v build_version="$build_version" '
      BEGIN {
        values["XIAOU_SRE_EVALUATION_SOURCE_REVISION"] = source_revision
        values["XIAOU_SRE_EVALUATION_BUILD_ID"] = build_id
        values["XIAOU_SRE_EVALUATION_BUILD_VERSION"] = build_version
      }
      {
        key = $0
        sub(/=.*/, "", key)
        if (key in values) {
          if (!(key in seen)) {
            print key "=" values[key]
            seen[key] = 1
          }
          next
        }
        print
      }
      END {
        for (key in values) {
          if (!(key in seen)) {
            print key "=" values[key]
          }
        }
      }
    ' "$APP_ENV_FILE" >"$temp_file"

  chmod "$file_mode" "$temp_file"
  chown "$file_uid:$file_gid" "$temp_file"
  mv -f "$temp_file" "$APP_ENV_FILE"
}

validate_archive_entries() {
  local bundle="$1"
  local entry

  while IFS= read -r entry; do
    entry="${entry#./}"
    [[ -n "$entry" ]] || continue
    case "$entry" in
      /* | ../* | */../* | */..)
        fail "unsafe release archive entry: $entry"
        ;;
    esac
  done < <(tar -tzf "$bundle")
}

validate_stage() {
  local stage="$1"
  local relative_path

  test -f "$stage/backend/app.jar"
  test -f "$stage/user/index.html"
  test -f "$stage/admin/index.html"
  test -f "$stage/RELEASE"
  test -f "$stage/VERSION"
  test -f "$stage/release-manifest.json"
  test -f "$stage/scripts/deploy-release.sh"
  test -f "$stage/scripts/db-migrate.py"
  test -f "$stage/scripts/release-smoke-test.py"
  test -f "$stage/sql/v2.5.3/production_governance.sql"
  test -f "$stage/scripts/server-capacity-governance.sh"
  test -f "$stage/scripts/verify-production-baseline.sh"

  if [[ "$MANAGE_OPERATIONS" == "true" ]]; then
    test -f "$stage/ops/nginx/code-nest-production.conf"
    for relative_path in "${MONITORING_MANAGED_FILES[@]}"; do
      test -f "$stage/ops/monitoring/$relative_path"
    done
    for relative_path in "${SYSTEMD_MANAGED_FILES[@]}"; do
      test -f "$stage/ops/systemd/$relative_path"
    done
    for relative_path in "${BIN_MANAGED_FILES[@]}"; do
      test -f "$stage/ops/scripts/$relative_path"
    done
  fi

  if find "$stage" -type l -print -quit | grep -q .; then
    fail "release bundle must not contain symbolic links"
  fi
  load_release_metadata "$stage/RELEASE"
}

run_schema_migrations() {
  if [[ "$RUN_MIGRATIONS" != "true" ]]; then
    log "skip database migrations (CODE_NEST_RUN_MIGRATIONS=$RUN_MIGRATIONS)"
    return 0
  fi
  require_cmd python3
  log "apply database migrations"
  if [[ "$RETRY_FAILED" == "true" ]]; then
    python3 "$1" --apply --retry-failed
  else
    python3 "$1" --apply
  fi
}

install_operational_assets() {
  local stage="$1"
  local relative_path

  if [[ "$MANAGE_OPERATIONS" != "true" ]]; then
    log "skip operational asset installation"
    return 0
  fi

  log "install managed operational assets"
  sync_dir "$stage/ops" "$OPS_ROOT"
  install_file "$OPS_ROOT/nginx/code-nest-production.conf" "$NGINX_CONFIG" 644

  for relative_path in "${MONITORING_MANAGED_FILES[@]}"; do
    case "$relative_path" in
      scripts/*) install_file "$OPS_ROOT/monitoring/$relative_path" "$MONITORING_DIR/$relative_path" 755 ;;
      *) install_file "$OPS_ROOT/monitoring/$relative_path" "$MONITORING_DIR/$relative_path" 644 ;;
    esac
  done
  for relative_path in "${SYSTEMD_MANAGED_FILES[@]}"; do
    install_file "$OPS_ROOT/systemd/$relative_path" "$SYSTEMD_DIR/$relative_path" 644
  done
  for relative_path in "${BIN_MANAGED_FILES[@]}"; do
    install_file "$OPS_ROOT/scripts/$relative_path" "$BIN_DIR/$relative_path" 755
  done

  chown -R root:root "$OPS_ROOT"
  find "$OPS_ROOT" -type d -exec chmod 755 {} +
  find "$OPS_ROOT" -type f -exec chmod 644 {} +
  chmod 755 "$OPS_ROOT/monitoring/scripts/compose.sh" \
    "$OPS_ROOT/monitoring/scripts/validate-config.sh" \
    "$OPS_ROOT/scripts/server-capacity-governance.sh" \
    "$OPS_ROOT/scripts/sre-alertmanager-e2e.py" \
    "$OPS_ROOT/scripts/verify-production-baseline.sh"
}

restore_operational_assets() {
  local backup_dir="$1"
  local relative_path

  if [[ "$MANAGE_OPERATIONS" != "true" ]]; then
    return 0
  fi

  restore_directory "$backup_dir/ops-reference" "$OPS_ROOT"
  restore_file "$backup_dir/ops/nginx/code-nest.conf" "$NGINX_CONFIG"
  for relative_path in "${MONITORING_MANAGED_FILES[@]}"; do
    restore_file "$backup_dir/ops/monitoring/$relative_path" "$MONITORING_DIR/$relative_path"
  done
  for relative_path in "${SYSTEMD_MANAGED_FILES[@]}"; do
    restore_file "$backup_dir/ops/systemd/$relative_path" "$SYSTEMD_DIR/$relative_path"
  done
  for relative_path in "${BIN_MANAGED_FILES[@]}"; do
    restore_file "$backup_dir/ops/bin/$relative_path" "$BIN_DIR/$relative_path"
  done
}

run_baseline_verification() {
  if [[ "$VERIFY_BASELINE" != "true" ]]; then
    log "skip production baseline verification"
    return 0
  fi

  CODE_NEST_APP_ROOT="$APP_ROOT" \
  CODE_NEST_APP_DIR="$APP_DIR" \
  CODE_NEST_OPS_ROOT="$OPS_ROOT" \
  CODE_NEST_MONITORING_DIR="$MONITORING_DIR" \
  CODE_NEST_BIN_DIR="$BIN_DIR" \
  CODE_NEST_SYSTEMD_DIR="$SYSTEMD_DIR" \
  CODE_NEST_NGINX_CONFIG="$NGINX_CONFIG" \
  CODE_NEST_HEALTH_URL="$HEALTH_URL" \
  CODE_NEST_PROMETHEUS_URL="$PROMETHEUS_URL" \
  CODE_NEST_GRAFANA_URL="$GRAFANA_URL" \
  "$BIN_DIR/verify-production-baseline.sh" \
    --require-grafana \
    --expected-version "$BUNDLE_VERSION" \
    --expected-sha "$BUNDLE_SHA" \
    --min-free-gb "$MIN_FREE_GB"
}

rollback_from_backup() {
  local backup_dir

  backup_dir="$(strip_cr "$1")"
  test -d "$backup_dir"
  assert_safe_target_dir "$APP_DIR"
  assert_safe_target_dir "$USER_WEB_DIR"
  assert_safe_target_dir "$ADMIN_WEB_DIR"
  assert_safe_operational_targets
  log "rolling back from $backup_dir"

  restore_file "$backup_dir/app.jar" "$APP_DIR/app.jar"
  restore_file "$backup_dir/RELEASE" "$APP_DIR/RELEASE"
  restore_file "$backup_dir/app-env/code-nest.env" "$APP_ENV_FILE"
  if [[ -d "$backup_dir/user" ]]; then
    sync_dir "$backup_dir/user" "$USER_WEB_DIR"
  fi
  if [[ -d "$backup_dir/admin" ]]; then
    sync_dir "$backup_dir/admin" "$ADMIN_WEB_DIR"
  fi
  restore_operational_assets "$backup_dir"

  validate_nginx
  validate_monitoring
  apply_systemd_units
  apply_monitoring
  reload_nginx
  restart_service
  log "rollback completed"
}

cleanup_old_backups() {
  mkdir -p "$BACKUP_DIR"
  find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' \
    | sort -rn \
    | awk -v keep="$KEEP_RELEASES" 'NR > keep { sub(/^[0-9.]+ /, ""); print }' \
    | while IFS= read -r old_backup; do
        assert_safe_backup_dir "$old_backup"
        log "remove old backup: $old_backup"
        rm -rf "$old_backup"
      done
}

deploy_bundle() {
  local bundle
  local stamp
  local stage
  local backup_dir
  local deploy_status

  bundle="$(strip_cr "$1")"
  require_cmd tar
  require_cmd curl
  require_cmd systemctl
  require_cmd readlink
  require_cmd stat
  require_cmd python3
  assert_boolean CODE_NEST_RELOAD_NGINX "$RELOAD_NGINX"
  assert_boolean CODE_NEST_MANAGE_OPERATIONS "$MANAGE_OPERATIONS"
  assert_boolean CODE_NEST_MANAGE_MONITORING "$MANAGE_MONITORING"
  assert_boolean CODE_NEST_VERIFY_BASELINE "$VERIFY_BASELINE"
  [[ "$KEEP_RELEASES" =~ ^[0-9]+$ ]] || fail "CODE_NEST_KEEP_RELEASES must be a non-negative integer"
  assert_safe_target_dir "$APP_DIR"
  assert_safe_target_dir "$USER_WEB_DIR"
  assert_safe_target_dir "$ADMIN_WEB_DIR"
  assert_safe_backup_dir "$BACKUP_DIR"
  assert_safe_operational_targets

  if [[ ! -f "$bundle" ]]; then
    log "deployment bundle not found: $bundle"
    exit 66
  fi

  mkdir -p "$APP_ROOT" "$BACKUP_DIR"
  stamp="$(date '+%Y%m%d%H%M%S')"
  stage="$(mktemp -d "$APP_ROOT/release-stage.XXXXXX")"
  backup_dir="$BACKUP_DIR/$stamp-$RELEASE_VERSION"

  log "validate and extract release bundle: $bundle"
  validate_archive_entries "$bundle"
  tar --no-same-owner --no-same-permissions -xzf "$bundle" -C "$stage"
  validate_stage "$stage"
  python3 "$stage/scripts/release-smoke-test.py" "$bundle"
  run_schema_migrations "$stage/scripts/db-migrate.py"
  backup_dir="$BACKUP_DIR/$stamp-$RELEASE_VERSION"

  log "backup current release: $backup_dir"
  backup_current "$backup_dir"

  set +e
  {
    log "install backend jar and release metadata" &&
    mkdir -p "$APP_DIR" &&
    cp -a "$stage/backend/app.jar" "$APP_DIR/app.jar" &&
    cp -a "$stage/RELEASE" "$APP_DIR/RELEASE" &&
    log "install user frontend" &&
    sync_dir "$stage/user" "$USER_WEB_DIR" &&
    log "install admin frontend" &&
    sync_dir "$stage/admin" "$ADMIN_WEB_DIR" &&
    log "install deployment helper" &&
    install_deployment_helper "$stage/scripts/deploy-release.sh" &&
    install_operational_assets "$stage" &&
    log "update immutable SRE build provenance" &&
    update_app_provenance "$BUNDLE_SHA" "$BUNDLE_BUILD_ID" "$BUNDLE_VERSION" &&
    log "set application permissions" &&
    chown -R root:root "$APP_DIR" "$USER_WEB_DIR" "$ADMIN_WEB_DIR" "$BIN_DIR" &&
    find "$USER_WEB_DIR" "$ADMIN_WEB_DIR" -type d -exec chmod 755 {} + &&
    find "$USER_WEB_DIR" "$ADMIN_WEB_DIR" -type f -exec chmod 644 {} + &&
    chmod 644 "$APP_DIR/app.jar" "$APP_DIR/RELEASE" &&
    validate_nginx &&
    validate_monitoring &&
    apply_systemd_units &&
    apply_monitoring &&
    reload_nginx &&
    restart_service &&
    run_baseline_verification
  }
  deploy_status=$?
  set -e

  if [[ "$deploy_status" -ne 0 ]]; then
    log "deploy failed; restoring previous release"
    rollback_from_backup "$backup_dir"
    rm -rf "$stage"
    exit "$deploy_status"
  fi

  rm -rf "$stage"
  cleanup_old_backups
  log "release deployed successfully: $RELEASE_VERSION"
  log "backup retained at: $backup_dir"
}

main() {
  local command="${1:-}"

  case "$command" in
    deploy)
      [[ $# -eq 2 ]] || { usage; exit 2; }
      deploy_bundle "$2"
      ;;
    rollback)
      [[ $# -eq 2 ]] || { usage; exit 2; }
      require_cmd curl
      require_cmd systemctl
      assert_boolean CODE_NEST_RELOAD_NGINX "$RELOAD_NGINX"
      assert_boolean CODE_NEST_MANAGE_OPERATIONS "$MANAGE_OPERATIONS"
      assert_boolean CODE_NEST_MANAGE_MONITORING "$MANAGE_MONITORING"
      assert_safe_backup_dir "$(strip_cr "$2")"
      rollback_from_backup "$2"
      ;;
    -h | --help | help)
      usage
      ;;
    *)
      usage
      exit 2
      ;;
  esac
}

main "$@"
