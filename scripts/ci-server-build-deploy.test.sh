#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$repo_root/scripts/ci-server-build-deploy.sh"

declare -a npm_calls=()

npm() {
  npm_calls+=("$*")
}

workspace="$(mktemp -d)"
trap 'rm -rf "$workspace"' EXIT
mkdir -p \
  "$workspace/vue3-admin-front/node_modules" \
  "$workspace/vue3-user-front/node_modules"

cd "$workspace"
install_frontend_dependencies vue3-admin-front
install_frontend_dependencies vue3-user-front

if [[ "${#npm_calls[@]}" -ne 2 ]]; then
  printf 'expected npm ci for both frontends, got %s calls\n' "${#npm_calls[@]}" >&2
  exit 1
fi

for call in "${npm_calls[@]}"; do
  if [[ "$call" != ci\ * ]]; then
    printf 'expected npm ci, got: npm %s\n' "$call" >&2
    exit 1
  fi
done

for frontend_dir in vue3-admin-front vue3-user-front; do
  if ! printf '%s\n' "${npm_calls[@]}" | grep -Fq -- "--prefix $frontend_dir"; then
    printf 'missing npm ci call for %s\n' "$frontend_dir" >&2
    exit 1
  fi
done

ops_stage="$workspace/release-stage"
assemble_operational_assets "$ops_stage"

required_ops_files=(
  ops/nginx/code-nest-production.conf
  ops/monitoring/docker-compose.yml
  ops/monitoring/prometheus.yml
  ops/monitoring/alert_rules.yml
  ops/monitoring/grafana/provisioning/datasources/prometheus.yml
  ops/monitoring/grafana/provisioning/dashboards/code-nest.yml
  ops/monitoring/grafana/dashboards/code-nest-application.json
  ops/monitoring/grafana/dashboards/code-nest-sre.json
  ops/monitoring/scripts/compose.sh
  ops/monitoring/scripts/validate-config.sh
  ops/systemd/code-nest-capacity-governance.service
  ops/systemd/code-nest-capacity-governance.timer
  ops/scripts/server-capacity-governance.sh
  ops/scripts/sre-alertmanager-e2e.py
  ops/scripts/verify-production-baseline.sh
  scripts/deploy-release.sh
  scripts/server-capacity-governance.sh
  scripts/sre-alertmanager-e2e.py
  scripts/verify-production-baseline.sh
)

for relative_path in "${required_ops_files[@]}"; do
  if [[ ! -f "$ops_stage/$relative_path" ]]; then
    printf 'release operations asset missing: %s\n' "$relative_path" >&2
    exit 1
  fi
done

for forbidden_path in \
  ops/monitoring/.env \
  ops/monitoring/alertmanager/alertmanager.local.yml \
  ops/monitoring/targets/code-nest.local.yml \
  ops/monitoring/targets/blackbox.local.yml \
  ops/monitoring/secrets; do
  if [[ -e "$ops_stage/$forbidden_path" ]]; then
    printf 'release bundle contains local monitoring state: %s\n' "$forbidden_path" >&2
    exit 1
  fi
done

capacity_command='sudo -n /usr/bin/bash "${CODE_NEST_SOURCE_DIR}/scripts/server-capacity-governance.sh" --apply'
if ! grep -Fq "$capacity_command" "$repo_root/.github/workflows/deploy-production.yml"; then
  printf 'production workflow must invoke capacity governance through the sudoers-approved absolute path\n' >&2
  exit 1
fi

printf 'deployment dependency refresh contract passed\n'
