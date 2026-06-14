#!/usr/bin/env bash
set -Eeuo pipefail

APP_ROOT="${CODE_NEST_APP_ROOT:-/opt/code-nest}"
APP_DIR="${CODE_NEST_APP_DIR:-$APP_ROOT/app}"
BACKUP_DIR="${CODE_NEST_BACKUP_DIR:-$APP_ROOT/backups/releases}"
USER_WEB_DIR="${CODE_NEST_USER_WEB_DIR:-/var/www/code-nest-user}"
ADMIN_WEB_DIR="${CODE_NEST_ADMIN_WEB_DIR:-/var/www/code-nest-admin}"
SERVICE_NAME="${CODE_NEST_SERVICE_NAME:-code-nest.service}"
HEALTH_URL="${CODE_NEST_HEALTH_URL:-http://127.0.0.1:9999/api/actuator/health}"
RELOAD_NGINX="${CODE_NEST_RELOAD_NGINX:-true}"
KEEP_RELEASES="${CODE_NEST_KEEP_RELEASES:-8}"
RELEASE_VERSION="${CODE_NEST_RELEASE_VERSION:-unknown}"

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

backup_current() {
  local backup_dir="$1"

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
  local backup_dir="$1"

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
  mkdir -p "$BACKUP_DIR"
  find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' \
    | sort -rn \
    | awk -v keep="$KEEP_RELEASES" 'NR > keep { sub(/^[0-9.]+ /, ""); print }' \
    | while IFS= read -r old_backup; do
        log "remove old backup: $old_backup"
        rm -rf "$old_backup"
      done
}

deploy_bundle() {
  local bundle="$1"
  local stamp
  local stage
  local backup_dir

  require_cmd tar
  require_cmd curl
  require_cmd systemctl
  assert_safe_target_dir "$APP_DIR"
  assert_safe_target_dir "$USER_WEB_DIR"
  assert_safe_target_dir "$ADMIN_WEB_DIR"
  assert_safe_backup_dir "$BACKUP_DIR"

  test -f "$bundle"
  mkdir -p "$APP_ROOT" "$BACKUP_DIR"
  stamp="$(date '+%Y%m%d%H%M%S')"
  stage="$(mktemp -d "$APP_ROOT/release-stage.XXXXXX")"
  backup_dir="$BACKUP_DIR/$stamp-$RELEASE_VERSION"

  log "extract release bundle: $bundle"
  tar -xzf "$bundle" -C "$stage"

  test -f "$stage/backend/app.jar"
  test -f "$stage/user/index.html"
  test -f "$stage/admin/index.html"

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
    mkdir -p "$APP_ROOT/bin" &&
    cp -a "$stage/scripts/deploy-release.sh" "$APP_ROOT/bin/deploy-release.sh" &&
    chmod 755 "$APP_ROOT/bin/deploy-release.sh" &&
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

main "$@"
