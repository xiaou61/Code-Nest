#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
release_version="${CODE_NEST_RELEASE_VERSION:-${GITHUB_REF_NAME:-manual}}"
reload_nginx="${CODE_NEST_RELOAD_NGINX:-true}"
node_home="${CODE_NEST_NODE_HOME:-/opt/code-nest/node-v24.15.0}"
release_root="${CODE_NEST_RELEASE_ROOT:-/opt/code-nest/actions-runner/releases}"
stage_dir=""

export PATH="$node_home/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

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

main() {
  require_cmd git
  require_cmd mvn
  require_cmd node
  require_cmd npm
  require_cmd tar

  cd "$repo_root"
  mkdir -p "$release_root"

  local short_sha
  short_sha="$(git rev-parse --short HEAD)"
  local full_sha
  full_sha="$(git rev-parse HEAD)"
  local safe_version
  safe_version="$(safe_name "$release_version")"
  local bundle
  bundle="$release_root/code-nest-${safe_version}-${short_sha}.tar.gz"
  stage_dir="$(mktemp -d "$release_root/stage.XXXXXX")"

  trap 'rm -rf "$stage_dir"' EXIT

  log "build backend"
  mvn -B -pl xiaou-application -am clean package -DskipTests

  log "build admin frontend"
  if [[ ! -d vue3-admin-front/node_modules ]]; then
    npm ci --prefer-offline --no-audit --fund=false --timeout=300000 --prefix vue3-admin-front
  fi
  npm run build --prefix vue3-admin-front

  log "build user frontend"
  if [[ ! -d vue3-user-front/node_modules ]]; then
    npm ci --prefer-offline --no-audit --fund=false --timeout=300000 --prefix vue3-user-front
  fi
  npm run build --prefix vue3-user-front

  log "assemble release bundle"
  mkdir -p "$stage_dir/backend" "$stage_dir/admin" "$stage_dir/user" "$stage_dir/scripts"
  cp xiaou-application/target/xiaou-application-*.jar "$stage_dir/backend/app.jar"
  copy_tree vue3-admin-front/dist "$stage_dir/admin"
  copy_tree vue3-user-front/dist "$stage_dir/user"
  cp scripts/deploy-release.sh "$stage_dir/scripts/deploy-release.sh"
  chmod 755 "$stage_dir/scripts/deploy-release.sh"
  cat >"$stage_dir/RELEASE" <<EOF
version=$safe_version
sha=$full_sha
built_at=$(date -Iseconds)
EOF

  rm -f "$bundle"
  tar -czf "$bundle" -C "$stage_dir" .
  log "release bundle ready: $bundle"

  sudo /opt/code-nest/actions-runner/deploy-from-workspace.sh \
    "$safe_version" \
    "$reload_nginx" \
    "$bundle"
}

main "$@"
