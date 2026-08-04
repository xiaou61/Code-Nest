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
SERVICE_NAME="${CODE_NEST_SERVICE_NAME:-code-nest.service}"
HEALTH_URL="${CODE_NEST_HEALTH_URL:-http://127.0.0.1:9999/api/actuator/health}"
RELOAD_NGINX="${CODE_NEST_RELOAD_NGINX:-true}"
KEEP_RELEASES="${CODE_NEST_KEEP_RELEASES:-8}"
MAX_BACKUP_GB="${CODE_NEST_MAX_BACKUP_GB:-4}"
MIN_FREE_GB="${CODE_NEST_MIN_FREE_GB:-2}"
RUN_MIGRATIONS="${CODE_NEST_RUN_MIGRATIONS:-false}"
RETRY_FAILED="${CODE_NEST_RETRY_FAILED:-false}"
RELEASE_VERSION="${CODE_NEST_RELEASE_VERSION:-unknown}"

APP_ROOT="$(strip_cr "$APP_ROOT")"
APP_DIR="$(strip_cr "$APP_DIR")"
BACKUP_DIR="$(strip_cr "$BACKUP_DIR")"
USER_WEB_DIR="$(strip_cr "$USER_WEB_DIR")"
ADMIN_WEB_DIR="$(strip_cr "$ADMIN_WEB_DIR")"
SERVICE_NAME="$(strip_cr "$SERVICE_NAME")"
HEALTH_URL="$(strip_cr "$HEALTH_URL")"
RELOAD_NGINX="$(strip_cr "$RELOAD_NGINX")"
KEEP_RELEASES="$(strip_cr "$KEEP_RELEASES")"
MAX_BACKUP_GB="$(strip_cr "$MAX_BACKUP_GB")"
MIN_FREE_GB="$(strip_cr "$MIN_FREE_GB")"
RUN_MIGRATIONS="$(strip_cr "$RUN_MIGRATIONS")"
RETRY_FAILED="$(strip_cr "$RETRY_FAILED")"
RELEASE_VERSION="$(strip_cr "$RELEASE_VERSION")"

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
  CODE_NEST_SERVICE_NAME=code-nest.service
  CODE_NEST_HEALTH_URL=http://127.0.0.1:9999/api/actuator/health
  CODE_NEST_RELOAD_NGINX=true
  CODE_NEST_KEEP_RELEASES=8
  CODE_NEST_MAX_BACKUP_GB=4
  CODE_NEST_MIN_FREE_GB=2
  CODE_NEST_RUN_MIGRATIONS=false
  CODE_NEST_RETRY_FAILED=false
USAGE
}

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    log "missing required command: $1"
    exit 127
  }
}

validate_bundle() {
  local bundle="$1"
  local member
  local bundle_version
  local expected_version
  local metadata_version
  local metadata_schema_version

  require_cmd python3
  python3 - "$bundle" <<'PY'
import re
import sys
import tarfile
from pathlib import PurePosixPath

bundle = sys.argv[1]
required = {
    "RELEASE",
    "VERSION",
    "backend/app.jar",
    "user/index.html",
    "admin/index.html",
    "scripts/deploy-release.sh",
    "scripts/db-migrate.py",
    "scripts/release-smoke-test.py",
    "sql/v2.5.3/production_governance.sql",
}

with tarfile.open(bundle, "r:gz") as archive:
    members = archive.getmembers()
    names = {member.name for member in members}
    unsafe = []
    for member in members:
        normalized = member.name.replace("\\", "/")
        if normalized.startswith("/") or ".." in PurePosixPath(normalized).parts:
            unsafe.append(member.name)
        elif not (member.isdir() or member.isfile()):
            unsafe.append(member.name)
    if unsafe:
        raise SystemExit("unsafe release archive member: " + repr(unsafe[:3]))
    missing = sorted(required - names)
    if missing:
        raise SystemExit("release bundle missing: " + ", ".join(missing))

    def read(name):
        member = archive.getmember(name)
        handle = archive.extractfile(member)
        if handle is None:
            raise SystemExit("release archive member is not readable: " + name)
        return handle.read().decode("utf-8")

    version = read("VERSION").strip()
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise SystemExit("invalid release VERSION")
    metadata = dict(line.split("=", 1) for line in read("RELEASE").splitlines() if "=" in line)
    expected = "v" + version
    if metadata.get("version") != expected or metadata.get("schema_version") != expected:
        raise SystemExit("release metadata version mismatch")
    sha = metadata.get("sha", "")
    if not sha or (sha != "unknown" and not re.fullmatch(r"[0-9a-f]{40}", sha)):
        raise SystemExit("release SHA is missing or malformed")
PY
  while IFS= read -r member; do
    case "$member" in
      /* | ../* | */../* | */.. | *$'\r'*)
        log "refusing unsafe release archive member: $member"
        exit 64
        ;;
    esac
  done < <(tar -tzf "$bundle")

  bundle_version="$(tar -xOf "$bundle" VERSION | tr -d '[:space:]\r\n')"
  [[ -n "$bundle_version" ]] || { log "release bundle VERSION is empty"; exit 65; }
  expected_version="v${bundle_version#v}"
  metadata_version="$(tar -xOf "$bundle" RELEASE | awk -F= '$1 == "version" { print $2; exit }' | tr -d '\r')"
  metadata_schema_version="$(tar -xOf "$bundle" RELEASE | awk -F= '$1 == "schema_version" { print $2; exit }' | tr -d '\r')"
  if [[ "$metadata_version" != "$expected_version" || "$metadata_schema_version" != "$expected_version" ]]; then
    log "release metadata version mismatch: expected=$expected_version version=$metadata_version schema=$metadata_schema_version"
    exit 65
  fi
  if [[ "$RELEASE_VERSION" == "unknown" ]]; then
    RELEASE_VERSION="$expected_version"
  elif [[ "$RELEASE_VERSION" != "$expected_version" ]]; then
    log "release version mismatch: requested=$RELEASE_VERSION bundle=$expected_version"
    exit 65
  fi
}

check_disk_capacity() {
  local path="$1"
  local minimum_kb
  local available_kb
  minimum_kb=$((MIN_FREE_GB * 1024 * 1024))
  available_kb="$(df -Pk "$path" | awk 'NR == 2 { print $4 }')"
  if [[ -z "$available_kb" || "$available_kb" -lt "$minimum_kb" ]]; then
    log "refusing deployment: only ${available_kb:-unknown} KB free on $(df -P "$path" | awk 'NR == 2 { print $6 }'), minimum=${minimum_kb} KB"
    exit 70
  fi
  log "disk capacity check passed: ${available_kb} KB free"
}

assert_safe_target_dir() {
  local target_dir="$1"
  local app_root="${APP_ROOT%/}"

  case "$app_root" in
    "" | "/" | "/opt" | "/var" | "/var/www")
      log "refusing to use unsafe app root: $APP_ROOT"
      exit 64
      ;;
  esac

  case "$target_dir" in
    /var/www/code-nest-* | "$app_root"/*) ;;
    *)
      log "refusing to modify unsafe target directory: $target_dir"
      exit 64
      ;;
  esac
}

assert_safe_backup_dir() {
  local backup_dir="$1"
  local backup_root="${APP_ROOT%/}/backups/releases"

  case "$backup_dir" in
    "$backup_root" | "$backup_root"/*) ;;
    *)
      log "refusing to use unsafe backup directory: $backup_dir"
      exit 64
      ;;
  esac
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

install_deployment_helper() {
  local source_script="$1"
  local target_script="$APP_ROOT/bin/deploy-release.sh"
  local pending_script="$APP_ROOT/bin/deploy-release.sh.pending"

  mkdir -p "$APP_ROOT/bin"
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

backup_current() {
  local backup_dir="$1"

  check_disk_capacity "$APP_ROOT"
  mkdir -p "$backup_dir"
  mkdir -p "$APP_DIR" "$USER_WEB_DIR" "$ADMIN_WEB_DIR"

  if [[ -f "$APP_DIR/app.jar" ]]; then
    cp -a "$APP_DIR/app.jar" "$backup_dir/app.jar"
  fi

  if [[ -d "$USER_WEB_DIR" ]]; then
    mkdir -p "$backup_dir/user"
    cp -a "$USER_WEB_DIR"/. "$backup_dir/user"/ 2>/dev/null || true
  fi

  if [[ -d "$ADMIN_WEB_DIR" ]]; then
    mkdir -p "$backup_dir/admin"
    cp -a "$ADMIN_WEB_DIR"/. "$backup_dir/admin"/ 2>/dev/null || true
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

health_check() {
  local attempts="${1:-30}"
  local sleep_seconds="${2:-2}"

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

reload_nginx() {
  if [[ "$RELOAD_NGINX" != "true" ]]; then
    log "skip nginx reload"
    return 0
  fi

  require_cmd nginx
  nginx -t
  systemctl reload nginx
}

restart_service() {
  systemctl restart "$SERVICE_NAME"
  health_check 30 2
}

rollback_from_backup() {
  local backup_dir

  backup_dir="$(strip_cr "$1")"

  test -d "$backup_dir"
  assert_safe_target_dir "$APP_DIR"
  assert_safe_target_dir "$USER_WEB_DIR"
  assert_safe_target_dir "$ADMIN_WEB_DIR"
  log "rolling back from $backup_dir"

  if [[ -f "$backup_dir/app.jar" ]]; then
    mkdir -p "$APP_DIR"
    cp -a "$backup_dir/app.jar" "$APP_DIR/app.jar"
  fi

  if [[ -d "$backup_dir/user" ]]; then
    sync_dir "$backup_dir/user" "$USER_WEB_DIR"
  fi

  if [[ -d "$backup_dir/admin" ]]; then
    sync_dir "$backup_dir/admin" "$ADMIN_WEB_DIR"
  fi

  reload_nginx
  restart_service
  log "rollback completed"
}

cleanup_old_backups() {
  local max_backup_kb=$((MAX_BACKUP_GB * 1024 * 1024))
  local current_backup_kb
  local oldest_backup
  local oldest_size_kb

  mkdir -p "$BACKUP_DIR"
  find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' \
    | sort -rn \
    | awk -v keep="$KEEP_RELEASES" 'NR > keep { sub(/^[0-9.]+ /, ""); print }' \
    | while IFS= read -r old_backup; do
        log "remove old backup: $old_backup"
        rm -rf "$old_backup"
      done

  while true; do
    current_backup_kb="$(du -sk "$BACKUP_DIR" | awk '{ print $1 }')"
    if [[ -z "$current_backup_kb" || "$current_backup_kb" -le "$max_backup_kb" ]]; then
      break
    fi
    oldest_backup="$(find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' | sort -n | head -n 1 | sed 's/^[0-9.]* //')"
    [[ -n "$oldest_backup" ]] || break
    assert_safe_backup_dir "$oldest_backup"
    oldest_size_kb="$(du -sk "$oldest_backup" | awk '{ print $1 }')"
    log "remove backup due to capacity: $oldest_backup (${oldest_size_kb:-0} KB)"
    rm -rf "$oldest_backup"
  done
}

run_schema_migrations() {
  if [[ "$RUN_MIGRATIONS" != "true" ]]; then
    log "skip database migrations (CODE_NEST_RUN_MIGRATIONS=$RUN_MIGRATIONS)"
    return 0
  fi
  require_cmd python3
  log "apply database migrations"
  if [[ "$RETRY_FAILED" == "true" ]]; then
    log "retrying incomplete migration rows after explicit operator approval"
    python3 "$1" --apply --retry-failed
  else
    python3 "$1" --apply
  fi
}

deploy_bundle() {
  local bundle
  local stamp
  local stage
  local backup_dir

  bundle="$(strip_cr "$1")"
  require_cmd tar
  require_cmd curl
  require_cmd systemctl
  assert_safe_target_dir "$APP_DIR"
  assert_safe_target_dir "$USER_WEB_DIR"
  assert_safe_target_dir "$ADMIN_WEB_DIR"
  assert_safe_backup_dir "$BACKUP_DIR"

  if [[ ! -f "$bundle" ]]; then
    log "deployment bundle not found: $bundle"
    exit 66
  fi

  validate_bundle "$bundle"

  mkdir -p "$APP_ROOT" "$BACKUP_DIR"
  check_disk_capacity "$APP_ROOT"
  stamp="$(date '+%Y%m%d%H%M%S')"
  stage="$(mktemp -d "$APP_ROOT/release-stage.XXXXXX")"
  backup_dir="$BACKUP_DIR/$stamp-$RELEASE_VERSION"

  log "extract release bundle: $bundle"
  tar --no-same-owner --no-same-permissions -xzf "$bundle" -C "$stage"

  python3 "$stage/scripts/release-smoke-test.py" "$bundle"

  test -f "$stage/backend/app.jar"
  test -f "$stage/user/index.html"
  test -f "$stage/admin/index.html"

  run_schema_migrations "$stage/scripts/db-migrate.py"

  log "backup current release: $backup_dir"
  backup_current "$backup_dir"

  set +e
  {
    log "install backend jar" &&
    mkdir -p "$APP_DIR" &&
    cp -a "$stage/backend/app.jar" "$APP_DIR/app.jar" &&
    log "install user frontend" &&
    sync_dir "$stage/user" "$USER_WEB_DIR" &&
    log "install admin frontend" &&
    sync_dir "$stage/admin" "$ADMIN_WEB_DIR" &&
    log "install deployment helper" &&
    install_deployment_helper "$stage/scripts/deploy-release.sh" &&
    log "set permissions" &&
    chown -R root:root "$APP_DIR" "$USER_WEB_DIR" "$ADMIN_WEB_DIR" "$APP_ROOT/bin" &&
    find "$USER_WEB_DIR" "$ADMIN_WEB_DIR" -type d -exec chmod 755 {} + &&
    find "$USER_WEB_DIR" "$ADMIN_WEB_DIR" -type f -exec chmod 644 {} + &&
    chmod 644 "$APP_DIR/app.jar" &&
    reload_nginx &&
    restart_service
  }
  local deploy_status=$?
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
      [[ $# -eq 2 ]] || {
        usage
        exit 2
      }
      deploy_bundle "$2"
      ;;
    rollback)
      [[ $# -eq 2 ]] || {
        usage
        exit 2
      }
      require_cmd curl
      require_cmd systemctl
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

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
