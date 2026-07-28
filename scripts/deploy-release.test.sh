#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
deploy_script="$repo_root/scripts/deploy-release.sh"
workspace="$(mktemp -d)"
trap 'rm -rf "$workspace"' EXIT

app_root="$workspace/code-nest"
app_dir="$app_root/app"
user_dir="$app_root/www/user"
admin_dir="$app_root/www/admin"
monitoring_dir="$app_root/monitoring"
ops_root="$app_root/ops"
systemd_dir="$app_root/systemd"
nginx_config="$app_root/nginx/code-nest.conf"
app_env_file="$app_root/config/code-nest.env"
mock_bin="$workspace/mock-bin"
call_log="$workspace/calls.log"
stage="$workspace/stage"
bundle="$workspace/code-nest-v2.5.1.tar.gz"

mkdir -p "$app_dir" "$user_dir" "$admin_dir" "$monitoring_dir" \
  "$ops_root" "$systemd_dir" "$(dirname "$nginx_config")" \
  "$(dirname "$app_env_file")" "$mock_bin" "$stage/backend" "$stage/user" \
  "$stage/admin" "$stage/scripts" "$stage/ops/nginx" \
  "$stage/ops/monitoring/grafana/provisioning/datasources" \
  "$stage/ops/monitoring/grafana/provisioning/dashboards" \
  "$stage/ops/monitoring/grafana/dashboards" "$stage/ops/monitoring/scripts" \
  "$stage/ops/systemd" "$stage/ops/scripts"

cat >"$mock_bin/systemctl" <<'EOF'
#!/usr/bin/env bash
printf 'systemctl %s\n' "$*" >>"$CALL_LOG"
exit 0
EOF
cat >"$mock_bin/nginx" <<'EOF'
#!/usr/bin/env bash
printf 'nginx %s\n' "$*" >>"$CALL_LOG"
exit 0
EOF
cat >"$mock_bin/curl" <<'EOF'
#!/usr/bin/env bash
printf 'curl %s\n' "$*" >>"$CALL_LOG"
if [[ "$*" == *"grafana.test"* ]] &&
  ! grep -Eq '^[[:space:]]*grafana:' "$CODE_NEST_MONITORING_DIR/docker-compose.yml"; then
  exit 22
fi
printf '{"status":"UP","database":"ok"}'
EOF
cat >"$mock_bin/sleep" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
cat >"$mock_bin/chown" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
chmod +x "$mock_bin/systemctl" "$mock_bin/nginx" "$mock_bin/curl" \
  "$mock_bin/sleep" "$mock_bin/chown"

printf 'old-jar\n' >"$app_dir/app.jar"
printf 'old-version\n' >"$app_dir/RELEASE"
printf 'old-user\n' >"$user_dir/index.html"
printf 'old-admin\n' >"$admin_dir/index.html"
cat >"$app_env_file" <<'EOF'
DATABASE_PASSWORD=must-stay
XIAOU_SRE_EVALUATION_SOURCE_REVISION=old-sha
XIAOU_SRE_EVALUATION_BUILD_ID=old-build
XIAOU_SRE_EVALUATION_BUILD_VERSION=v2.5.0
EOF

mkdir -p "$monitoring_dir/alertmanager" "$monitoring_dir/secrets" \
  "$monitoring_dir/targets" "$monitoring_dir/scripts" "$ops_root/monitoring/scripts"
printf 'LOCAL_ENV=must-stay\n' >"$monitoring_dir/.env"
printf 'local-alertmanager\n' >"$monitoring_dir/alertmanager/alertmanager.local.yml"
printf 'smtp-secret\n' >"$monitoring_dir/secrets/qq_smtp_auth_code"
printf 'webhook-secret\n' >"$monitoring_dir/secrets/sre_webhook_token"
printf 'local-code-target\n' >"$monitoring_dir/targets/code-nest.local.yml"
printf 'local-blackbox-target\n' >"$monitoring_dir/targets/blackbox.local.yml"

managed_files=(
  monitoring/docker-compose.yml
  monitoring/prometheus.yml
  monitoring/alert_rules.yml
  monitoring/grafana/provisioning/datasources/prometheus.yml
  monitoring/grafana/provisioning/dashboards/code-nest.yml
  monitoring/grafana/dashboards/code-nest-application.json
  monitoring/grafana/dashboards/code-nest-sre.json
  systemd/code-nest-capacity-governance.service
  systemd/code-nest-capacity-governance.timer
  scripts/server-capacity-governance.sh
  scripts/sre-alertmanager-e2e.py
  scripts/verify-production-baseline.sh
)

active_path() {
  case "$1" in
    monitoring/*) printf '%s/%s' "$monitoring_dir" "${1#monitoring/}" ;;
    systemd/*) printf '%s/%s' "$systemd_dir" "${1#systemd/}" ;;
    scripts/*) printf '%s/%s' "$app_root/bin" "${1#scripts/}" ;;
  esac
}

printf 'old-nginx\n' >"$nginx_config"
mkdir -p "$ops_root/nginx"
printf 'old-nginx\n' >"$ops_root/nginx/code-nest-production.conf"
for relative_path in "${managed_files[@]}"; do
  expected="$ops_root/$relative_path"
  active="$(active_path "$relative_path")"
  mkdir -p "$(dirname "$expected")" "$(dirname "$active")"
  printf 'old-%s\n' "$relative_path" >"$expected"
  printf 'old-%s\n' "$relative_path" >"$active"
done
cat >"$monitoring_dir/docker-compose.yml" <<'EOF'
services:
  prometheus:
    image: prometheus:test
EOF

cat >"$monitoring_dir/scripts/validate-config.sh" <<'EOF'
#!/usr/bin/env bash
printf 'monitoring validate\n' >>"$CALL_LOG"
EOF
cat >"$monitoring_dir/scripts/compose.sh" <<'EOF'
#!/usr/bin/env bash
printf 'monitoring compose %s\n' "$*" >>"$CALL_LOG"
EOF
chmod +x "$monitoring_dir/scripts/validate-config.sh" "$monitoring_dir/scripts/compose.sh"
cp "$monitoring_dir/scripts/validate-config.sh" "$ops_root/monitoring/scripts/validate-config.sh"
cp "$monitoring_dir/scripts/compose.sh" "$ops_root/monitoring/scripts/compose.sh"

printf 'new-jar\n' >"$stage/backend/app.jar"
printf 'new-user\n' >"$stage/user/index.html"
printf 'new-admin\n' >"$stage/admin/index.html"
cat >"$stage/RELEASE" <<'EOF'
version=v2.5.1
sha=1111111111111111111111111111111111111111
build_id=release-251-1111111
built_at=2026-07-28T12:00:00Z
EOF
cp "$deploy_script" "$stage/scripts/deploy-release.sh"

cat >"$stage/scripts/server-capacity-governance.sh" <<'EOF'
#!/usr/bin/env bash
printf 'capacity %s\n' "$*" >>"$CALL_LOG"
EOF
cat >"$stage/scripts/verify-production-baseline.sh" <<'EOF'
#!/usr/bin/env bash
printf 'baseline %s\n' "$*" >>"$CALL_LOG"
EOF
cat >"$stage/scripts/sre-alertmanager-e2e.py" <<'EOF'
#!/usr/bin/env python3
print("synthetic drill")
EOF
chmod +x "$stage/scripts/"*.sh
chmod +x "$stage/scripts/sre-alertmanager-e2e.py"
cp "$stage/scripts/server-capacity-governance.sh" "$stage/ops/scripts/server-capacity-governance.sh"
cp "$stage/scripts/sre-alertmanager-e2e.py" "$stage/ops/scripts/sre-alertmanager-e2e.py"
cp "$stage/scripts/verify-production-baseline.sh" "$stage/ops/scripts/verify-production-baseline.sh"

printf 'new-nginx\n' >"$stage/ops/nginx/code-nest-production.conf"
for relative_path in "${managed_files[@]}"; do
  case "$relative_path" in
    scripts/*) continue ;;
  esac
  target="$stage/ops/$relative_path"
  mkdir -p "$(dirname "$target")"
  printf 'new-%s\n' "$relative_path" >"$target"
done
cat >"$stage/ops/monitoring/scripts/validate-config.sh" <<'EOF'
#!/usr/bin/env bash
printf 'monitoring validate\n' >>"$CALL_LOG"
EOF
cat >"$stage/ops/monitoring/scripts/compose.sh" <<'EOF'
#!/usr/bin/env bash
printf 'monitoring compose %s\n' "$*" >>"$CALL_LOG"
if [[ "$*" == *"config --services"* ]]; then
  printf 'prometheus\n'
  if grep -Eq '^[[:space:]]*grafana:' "$CODE_NEST_MONITORING_DIR/docker-compose.yml"; then
    printf 'grafana\n'
  fi
fi
EOF
chmod +x "$stage/ops/monitoring/scripts/"*.sh
cat >"$stage/ops/monitoring/docker-compose.yml" <<'EOF'
services:
  prometheus:
    image: prometheus:test
  grafana:
    image: grafana:test
EOF

tar -czf "$bundle" -C "$stage" .

export CALL_LOG="$call_log"
export PATH="$mock_bin:$PATH"
export CODE_NEST_APP_ROOT="$app_root"
export CODE_NEST_APP_DIR="$app_dir"
export CODE_NEST_BACKUP_DIR="$app_root/backups/releases"
export CODE_NEST_USER_WEB_DIR="$user_dir"
export CODE_NEST_ADMIN_WEB_DIR="$admin_dir"
export CODE_NEST_OPS_ROOT="$ops_root"
export CODE_NEST_MONITORING_DIR="$monitoring_dir"
export CODE_NEST_SYSTEMD_DIR="$systemd_dir"
export CODE_NEST_NGINX_CONFIG="$nginx_config"
export CODE_NEST_APP_ENV_FILE="$app_env_file"
export CODE_NEST_RELEASE_VERSION="v2.5.1"
export CODE_NEST_HEALTH_URL="http://health.test/api/actuator/health"
export CODE_NEST_PROMETHEUS_URL="http://prometheus.test"
export CODE_NEST_GRAFANA_URL="http://grafana.test"
export CODE_NEST_MIN_FREE_GB=0

bash "$deploy_script" deploy "$bundle"

grep -Fqx 'new-jar' "$app_dir/app.jar"
grep -Fqx 'new-user' "$user_dir/index.html"
grep -Fqx 'new-admin' "$admin_dir/index.html"
cmp -s "$stage/RELEASE" "$app_dir/RELEASE"
cmp -s "$stage/ops/nginx/code-nest-production.conf" "$nginx_config"
cmp -s "$stage/ops/monitoring/alert_rules.yml" "$monitoring_dir/alert_rules.yml"
cmp -s "$stage/ops/systemd/code-nest-capacity-governance.timer" \
  "$systemd_dir/code-nest-capacity-governance.timer"
grep -Fq 'DATABASE_PASSWORD=must-stay' "$app_env_file"
grep -Fq 'XIAOU_SRE_EVALUATION_SOURCE_REVISION=1111111111111111111111111111111111111111' "$app_env_file"
grep -Fq 'XIAOU_SRE_EVALUATION_BUILD_ID=release-251-1111111' "$app_env_file"
grep -Fq 'XIAOU_SRE_EVALUATION_BUILD_VERSION=2.5.1' "$app_env_file"
grep -Fqx 'LOCAL_ENV=must-stay' "$monitoring_dir/.env"
grep -Fqx 'local-alertmanager' "$monitoring_dir/alertmanager/alertmanager.local.yml"
grep -Fqx 'smtp-secret' "$monitoring_dir/secrets/qq_smtp_auth_code"
grep -Fqx 'webhook-secret' "$monitoring_dir/secrets/sre_webhook_token"
grep -Fqx 'local-code-target' "$monitoring_dir/targets/code-nest.local.yml"
grep -Fqx 'local-blackbox-target' "$monitoring_dir/targets/blackbox.local.yml"
grep -Fq 'nginx -t' "$call_log"
grep -Fq 'systemctl daemon-reload' "$call_log"
grep -Fq 'systemctl enable --now code-nest-capacity-governance.timer' "$call_log"
grep -Fq 'monitoring validate' "$call_log"
grep -Fq 'monitoring compose --env-file' "$call_log"
grep -Fq 'baseline --require-grafana --expected-version v2.5.1 --expected-sha 1111111111111111111111111111111111111111 --min-free-gb 0' "$call_log"

backup_dir="$(find "$app_root/backups/releases" -mindepth 1 -maxdepth 1 -type d | head -1)"
[[ -n "$backup_dir" ]]
[[ -f "$backup_dir/app-env/code-nest.env" ]]
[[ -f "$backup_dir/ops/nginx/code-nest.conf" ]]

bash "$deploy_script" rollback "$backup_dir"

grep -Fqx 'old-jar' "$app_dir/app.jar"
grep -Fqx 'old-version' "$app_dir/RELEASE"
grep -Fqx 'old-user' "$user_dir/index.html"
grep -Fqx 'old-admin' "$admin_dir/index.html"
grep -Fqx 'old-nginx' "$nginx_config"
grep -Fq 'XIAOU_SRE_EVALUATION_BUILD_VERSION=v2.5.0' "$app_env_file"
grep -Fqx 'LOCAL_ENV=must-stay' "$monitoring_dir/.env"
grep -Fqx 'webhook-secret' "$monitoring_dir/secrets/sre_webhook_token"
[[ "$(grep -Fc 'curl -fsS http://grafana.test/api/health' "$call_log")" -eq 1 ]]

printf 'release deployment governance contract passed\n'
