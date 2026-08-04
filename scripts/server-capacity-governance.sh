#!/usr/bin/env bash
set -Eeuo pipefail

APP_ROOT="${CODE_NEST_APP_ROOT:-/opt/code-nest}"
RUNNER_ROOT="${CODE_NEST_RUNNER_ROOT:-$APP_ROOT/actions-runner}"
SOURCE_DIR="${CODE_NEST_SOURCE_DIR:-$RUNNER_ROOT/source}"
RELEASE_BACKUP_DIR="${CODE_NEST_RELEASE_BACKUP_DIR:-$APP_ROOT/backups/releases}"
DATABASE_BACKUP_DIR="${CODE_NEST_DATABASE_BACKUP_DIR:-$APP_ROOT/backups/database}"
BUILD_RELEASE_DIR="${CODE_NEST_BUILD_RELEASE_DIR:-$RUNNER_ROOT/releases}"
RUNNER_CURRENT_BIN="${CODE_NEST_RUNNER_CURRENT_BIN:-}"
RUNNER_CURRENT_EXTERNALS="${CODE_NEST_RUNNER_CURRENT_EXTERNALS:-}"
LOCK_FILE="${CODE_NEST_CAPACITY_LOCK_FILE:-/tmp/code-nest-capacity-governance.lock}"

KEEP_RELEASE_BACKUPS="${CODE_NEST_KEEP_RELEASE_BACKUPS:-4}"
KEEP_DATABASE_BACKUPS="${CODE_NEST_KEEP_DATABASE_BACKUPS:-14}"
KEEP_BUILD_BUNDLES="${CODE_NEST_KEEP_BUILD_BUNDLES:-2}"
ORPHAN_STAGE_MINUTES="${CODE_NEST_ORPHAN_STAGE_MINUTES:-1440}"

APPLY=false
INCLUDE_BUILD_OUTPUTS=false
INCLUDE_RUNNER_CACHE=false

usage() {
  cat <<'USAGE'
Usage: server-capacity-governance.sh [--apply] [--include-build-outputs] [--include-runner-cache]

The default mode is read-only and reports what would be removed. --apply enables
bounded deletion under CODE_NEST_APP_ROOT. Build outputs are never selected unless
--include-build-outputs is also supplied. Inactive runner update caches, installer
archives and old runner versions require --include-runner-cache.
USAGE
}

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

fail() {
  log "ERROR: $*" >&2
  exit 64
}

canonical_path() {
  readlink -m -- "$1"
}

assert_positive_integer() {
  local name="$1"
  local value="$2"
  [[ "$value" =~ ^[0-9]+$ ]] || fail "$name must be a non-negative integer"
}

assert_safe_app_root() {
  APP_ROOT="$(canonical_path "$APP_ROOT")"
  case "$APP_ROOT" in
    "" | / | /opt | /var | /var/www | /root | /home)
      fail "unsafe CODE_NEST_APP_ROOT: $APP_ROOT"
      ;;
  esac
}

assert_within() {
  local candidate
  local allowed_root
  candidate="$(canonical_path "$1")"
  allowed_root="$(canonical_path "$2")"
  if [[ "$candidate" == "$allowed_root" || "$candidate" != "$allowed_root"/* ]]; then
    fail "refusing target outside managed child paths: $candidate"
  fi
}

candidate_size() {
  du -sh -- "$1" 2>/dev/null | awk '{print $1}' || printf 'unknown'
}

remove_candidate() {
  local category="$1"
  local path="$2"
  local allowed_root="$3"

  [[ -e "$path" || -L "$path" ]] || return 0
  assert_within "$path" "$allowed_root"
  if [[ "$APPLY" == true ]]; then
    log "remove category=$category size=$(candidate_size "$path") path=$path"
    rm -rf -- "$path"
  else
    log "would_remove category=$category size=$(candidate_size "$path") path=$path"
  fi
}

prune_newest_directories() {
  local root="$1"
  local keep="$2"
  local category="$3"
  local index=0
  local entry
  local path

  [[ -d "$root" ]] || return 0
  while IFS= read -r -d '' entry; do
    index=$((index + 1))
    if (( index <= keep )); then
      continue
    fi
    path="${entry#* }"
    remove_candidate "$category" "$path" "$root"
  done < <(find "$root" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\0' | sort -z -nr)
}

prune_newest_files() {
  local root="$1"
  local pattern="$2"
  local keep="$3"
  local category="$4"
  local index=0
  local entry
  local path

  [[ -d "$root" ]] || return 0
  while IFS= read -r -d '' entry; do
    index=$((index + 1))
    if (( index <= keep )); then
      continue
    fi
    path="${entry#* }"
    remove_candidate "$category" "$path" "$root"
  done < <(find "$root" -mindepth 1 -maxdepth 1 -type f -name "$pattern" -printf '%T@ %p\0' | sort -z -nr)
}

prune_orphan_stages() {
  local entry
  [[ -d "$APP_ROOT" ]] || return 0
  while IFS= read -r -d '' entry; do
    remove_candidate "orphan_release_stage" "$entry" "$APP_ROOT"
  done < <(find "$APP_ROOT" -mindepth 1 -maxdepth 1 -type d -name 'release-stage.*' \
    -mmin "+$ORPHAN_STAGE_MINUTES" -print0)
}

runner_job_active() {
  pgrep -af 'Runner.Worker|ci-server-build-deploy\.sh|scripts/ci-server-build-deploy' >/dev/null 2>&1
}

prune_build_outputs() {
  local path
  [[ -d "$SOURCE_DIR" ]] || return 0
  if runner_job_active; then
    log "skip build outputs because a deployment runner job is active"
    return 75
  fi

  for path in \
    "$SOURCE_DIR/vue3-admin-front/node_modules" \
    "$SOURCE_DIR/vue3-admin-front/dist" \
    "$SOURCE_DIR/vue3-user-front/node_modules" \
    "$SOURCE_DIR/vue3-user-front/dist" \
    "$SOURCE_DIR/docs-site/node_modules" \
    "$SOURCE_DIR/docs-site/.vitepress/dist"; do
    remove_candidate "generated_build_output" "$path" "$SOURCE_DIR"
  done

  while IFS= read -r -d '' path; do
    remove_candidate "maven_target" "$path" "$SOURCE_DIR"
  done < <(find "$SOURCE_DIR" -mindepth 2 -maxdepth 2 -type d -name target -print0)
}

prune_runner_cache() {
  local current_bin
  local current_externals
  local path

  if runner_job_active; then
    log "skip runner cache because a deployment runner job is active"
    return 75
  fi
  if [[ -n "$RUNNER_CURRENT_BIN" && -n "$RUNNER_CURRENT_EXTERNALS" ]]; then
    current_bin="$(canonical_path "$RUNNER_CURRENT_BIN")"
    current_externals="$(canonical_path "$RUNNER_CURRENT_EXTERNALS")"
  elif [[ -L "$RUNNER_ROOT/bin" && -L "$RUNNER_ROOT/externals" ]]; then
    current_bin="$(canonical_path "$RUNNER_ROOT/bin")"
    current_externals="$(canonical_path "$RUNNER_ROOT/externals")"
  else
    log "skip runner cache because current runner version targets are unavailable"
    return 75
  fi
  [[ -d "$current_bin" && -d "$current_externals" ]] \
    || fail "current runner version targets are missing"
  assert_within "$current_bin" "$RUNNER_ROOT"
  assert_within "$current_externals" "$RUNNER_ROOT"

  for path in "$RUNNER_ROOT"/bin.*; do
    [[ -d "$path" ]] || continue
    [[ "$(canonical_path "$path")" == "$current_bin" ]] \
      || remove_candidate "runner_old_version" "$path" "$RUNNER_ROOT"
  done
  for path in "$RUNNER_ROOT"/externals.*; do
    [[ -d "$path" ]] || continue
    [[ "$(canonical_path "$path")" == "$current_externals" ]] \
      || remove_candidate "runner_old_version" "$path" "$RUNNER_ROOT"
  done

  remove_candidate "runner_update_cache" "$RUNNER_ROOT/_work/_update" "$RUNNER_ROOT/_work"
  while IFS= read -r -d '' path; do
    remove_candidate "runner_installer_archive" "$path" "$RUNNER_ROOT"
  done < <(find "$RUNNER_ROOT" -mindepth 1 -maxdepth 1 -type f \
    -name 'actions-runner-*.tar.gz' -print0)
}

disk_summary() {
  df -h "$APP_ROOT" | awk 'NR == 2 {printf "filesystem=%s used=%s available=%s use_percent=%s\n", $1, $3, $4, $5}'
}

main() {
  while (( $# > 0 )); do
    case "$1" in
      --apply)
        APPLY=true
        ;;
      --include-build-outputs)
        INCLUDE_BUILD_OUTPUTS=true
        ;;
      --include-runner-cache)
        INCLUDE_RUNNER_CACHE=true
        ;;
      -h | --help)
        usage
        return 0
        ;;
      *)
        usage >&2
        fail "unknown argument: $1"
        ;;
    esac
    shift
  done

  command -v readlink >/dev/null 2>&1 || fail "readlink is required"
  command -v find >/dev/null 2>&1 || fail "find is required"
  command -v sort >/dev/null 2>&1 || fail "sort is required"
  assert_positive_integer CODE_NEST_KEEP_RELEASE_BACKUPS "$KEEP_RELEASE_BACKUPS"
  assert_positive_integer CODE_NEST_KEEP_DATABASE_BACKUPS "$KEEP_DATABASE_BACKUPS"
  assert_positive_integer CODE_NEST_KEEP_BUILD_BUNDLES "$KEEP_BUILD_BUNDLES"
  assert_positive_integer CODE_NEST_ORPHAN_STAGE_MINUTES "$ORPHAN_STAGE_MINUTES"
  assert_safe_app_root

  RUNNER_ROOT="$(canonical_path "$RUNNER_ROOT")"
  SOURCE_DIR="$(canonical_path "$SOURCE_DIR")"
  RELEASE_BACKUP_DIR="$(canonical_path "$RELEASE_BACKUP_DIR")"
  DATABASE_BACKUP_DIR="$(canonical_path "$DATABASE_BACKUP_DIR")"
  BUILD_RELEASE_DIR="$(canonical_path "$BUILD_RELEASE_DIR")"
  assert_within "$RUNNER_ROOT" "$APP_ROOT"
  assert_within "$SOURCE_DIR" "$APP_ROOT"
  assert_within "$RELEASE_BACKUP_DIR" "$APP_ROOT"
  assert_within "$DATABASE_BACKUP_DIR" "$APP_ROOT"
  assert_within "$BUILD_RELEASE_DIR" "$APP_ROOT"

  mkdir -p "$(dirname "$LOCK_FILE")"
  if command -v flock >/dev/null 2>&1; then
    exec 9>"$LOCK_FILE"
    flock -n 9 || {
      log "another capacity governance run is active"
      return 75
    }
  fi

  log "capacity governance mode=$([[ "$APPLY" == true ]] && printf apply || printf report)"
  disk_summary
  prune_newest_directories "$RELEASE_BACKUP_DIR" "$KEEP_RELEASE_BACKUPS" "release_backup"
  prune_newest_files "$DATABASE_BACKUP_DIR" '*.sql.gz' "$KEEP_DATABASE_BACKUPS" "database_backup"
  prune_newest_files "$BUILD_RELEASE_DIR" '*.tar.gz' "$KEEP_BUILD_BUNDLES" "build_bundle"
  prune_orphan_stages
  if [[ "$INCLUDE_BUILD_OUTPUTS" == true ]]; then
    prune_build_outputs
  fi
  if [[ "$INCLUDE_RUNNER_CACHE" == true ]]; then
    prune_runner_cache
  fi
  disk_summary
}

main "$@"
