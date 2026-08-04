#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
version_file="${CODE_NEST_VERSION_FILE:-$repo_root/VERSION}"
if [[ -z "${CODE_NEST_RELEASE_VERSION:-}" && -f "$version_file" ]]; then
  CODE_NEST_RELEASE_VERSION="v$(tr -d '[:space:]\r\n' < "$version_file")"
fi
release_version="${CODE_NEST_RELEASE_VERSION:-${GITHUB_REF_NAME:-manual}}"
reload_nginx="${CODE_NEST_RELOAD_NGINX:-true}"
node_home="${CODE_NEST_NODE_HOME:-/opt/code-nest/node-v24.15.0}"
release_root="${CODE_NEST_RELEASE_ROOT:-/opt/code-nest/actions-runner/releases}"
stage_dir=""

export PATH="$node_home/bin:/usr/local/bin:/usr/bin:/bin:$PATH"
export http_proxy="${CODE_NEST_HTTP_PROXY:-http://127.0.0.1:38457}"
export https_proxy="${CODE_NEST_HTTPS_PROXY:-http://127.0.0.1:38457}"
export npm_config_fetch_timeout="${CODE_NEST_NPM_FETCH_TIMEOUT:-300000}"
export npm_config_fetch_retries="${CODE_NEST_NPM_FETCH_RETRIES:-3}"
export npm_config_fetch_retry_maxtimeout="${CODE_NEST_NPM_FETCH_RETRY_MAXTIMEOUT:-120000}"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    log "missing required command: $1"
    exit 127
  }
}

safe_name() {
  printf '%s' "$1" | sed 's/[^A-Za-z0-9._-]/-/g'
}

copy_tree() {
  local source_dir="$1"
  local target_dir="$2"

  test -d "$source_dir"
  mkdir -p "$target_dir"
  cp -a "$source_dir"/. "$target_dir"/
}

copy_release_file() {
  local source_file="$1"
  local target_file="$2"
  local mode="${3:-644}"

  test -f "$source_file"
  mkdir -p "$(dirname "$target_file")"
  cp -a "$source_file" "$target_file"
  chmod "$mode" "$target_file"
}

assemble_operational_assets() {
  local target_root="$1"

  copy_release_file \
    "$repo_root/deploy/nginx/code-nest-production.conf" \
    "$target_root/ops/nginx/code-nest-production.conf"
  copy_release_file \
    "$repo_root/docker/monitoring/docker-compose.yml" \
    "$target_root/ops/monitoring/docker-compose.yml"
  copy_release_file \
    "$repo_root/docker/monitoring/prometheus.yml" \
    "$target_root/ops/monitoring/prometheus.yml"
  copy_release_file \
    "$repo_root/docker/monitoring/alert_rules.yml" \
    "$target_root/ops/monitoring/alert_rules.yml"
  copy_tree \
    "$repo_root/docker/monitoring/grafana" \
    "$target_root/ops/monitoring/grafana"
  copy_release_file \
    "$repo_root/docker/monitoring/scripts/compose.sh" \
    "$target_root/ops/monitoring/scripts/compose.sh" 755
  copy_release_file \
    "$repo_root/docker/monitoring/scripts/validate-config.sh" \
    "$target_root/ops/monitoring/scripts/validate-config.sh" 755
  copy_release_file \
    "$repo_root/deploy/systemd/code-nest-capacity-governance.service" \
    "$target_root/ops/systemd/code-nest-capacity-governance.service"
  copy_release_file \
    "$repo_root/deploy/systemd/code-nest-capacity-governance.timer" \
    "$target_root/ops/systemd/code-nest-capacity-governance.timer"

  copy_release_file \
    "$repo_root/scripts/deploy-release.sh" \
    "$target_root/scripts/deploy-release.sh" 755
  copy_release_file \
    "$repo_root/scripts/server-capacity-governance.sh" \
    "$target_root/scripts/server-capacity-governance.sh" 755
  copy_release_file \
    "$repo_root/scripts/verify-production-baseline.sh" \
    "$target_root/scripts/verify-production-baseline.sh" 755
  copy_release_file \
    "$repo_root/scripts/sre-alertmanager-e2e.py" \
    "$target_root/scripts/sre-alertmanager-e2e.py" 755
  copy_release_file \
    "$repo_root/scripts/server-capacity-governance.sh" \
    "$target_root/ops/scripts/server-capacity-governance.sh" 755
  copy_release_file \
    "$repo_root/scripts/verify-production-baseline.sh" \
    "$target_root/ops/scripts/verify-production-baseline.sh" 755
  copy_release_file \
    "$repo_root/scripts/sre-alertmanager-e2e.py" \
    "$target_root/ops/scripts/sre-alertmanager-e2e.py" 755
}

install_frontend_dependencies() {
  local frontend_dir="$1"

  npm ci \
    --prefer-offline \
    --no-audit \
    --fund=false \
    --ignore-scripts \
    --prefix "$frontend_dir"
}

main() {
  require_cmd git
  require_cmd mvn
  require_cmd node
  require_cmd npm
  require_cmd tar
  require_cmd python3

  cd "$repo_root"
  if [[ "${CODE_NEST_ALLOW_DIRTY_BUILD:-false}" != "true" ]] && [[ -n "$(git status --porcelain)" ]]; then
    log "refusing release build from dirty worktree; set CODE_NEST_ALLOW_DIRTY_BUILD=true only for local diagnostics"
    exit 65
  fi
  python3 scripts/check-version-consistency.py
  mkdir -p "$release_root"

  local short_sha
  short_sha="$(git rev-parse --short HEAD)"
  local full_sha
  full_sha="$(git rev-parse HEAD)"
  local safe_version
  safe_version="$(safe_name "$release_version")"
  local bundle
  bundle="$release_root/code-nest-${safe_version}-${short_sha}.tar.gz"
  local build_id
  build_id="$(safe_name "${CODE_NEST_BUILD_ID:-$safe_version-$short_sha}")"
  stage_dir="$(mktemp -d "$release_root/stage.XXXXXX")"

  trap 'rm -rf "$stage_dir"' EXIT

  log "build backend"
  mvn -B -pl xiaou-application -am clean package -DskipTests -Drevision="v$(tr -d '[:space:]\r\n' < "$version_file")"

  log "install admin frontend dependencies"
  install_frontend_dependencies vue3-admin-front
  log "build admin frontend"
  npm run build --prefix vue3-admin-front

  log "install user frontend dependencies"
  install_frontend_dependencies vue3-user-front
  log "build user frontend"
  npm run build --prefix vue3-user-front

  log "assemble release bundle"
  mkdir -p "$stage_dir/backend" "$stage_dir/admin" "$stage_dir/user" "$stage_dir/scripts"
  cp xiaou-application/target/xiaou-application-*.jar "$stage_dir/backend/app.jar"
  copy_tree vue3-admin-front/dist "$stage_dir/admin"
  copy_tree vue3-user-front/dist "$stage_dir/user"
  assemble_operational_assets "$stage_dir"
  copy_release_file "$repo_root/scripts/db-migrate.py" "$stage_dir/scripts/db-migrate.py" 755
  copy_release_file "$repo_root/scripts/release-smoke-test.py" "$stage_dir/scripts/release-smoke-test.py" 755
  copy_release_file "$version_file" "$stage_dir/VERSION"
  copy_tree "$repo_root/sql" "$stage_dir/sql"
  cat >"$stage_dir/RELEASE" <<EOF
version=$safe_version
sha=$full_sha
build_id=$build_id
built_at=$(date -Iseconds)
schema_version=$safe_version
EOF

  rm -f "$bundle"
  tar -czf "$bundle" -C "$stage_dir" backend admin user ops scripts sql VERSION RELEASE
  python3 scripts/release-smoke-test.py "$bundle"
  log "release bundle ready: $bundle"

  if [[ ! -f "$bundle" ]]; then
    log "release bundle missing after build: $bundle"
    exit 66
  fi

  log "deploy release bundle"
  sudo -n /opt/code-nest/actions-runner/deploy-from-workspace.sh \
    "$safe_version" \
    "$reload_nginx" \
    "$bundle"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
