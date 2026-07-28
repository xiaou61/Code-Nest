#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
baseline_script="$repo_root/scripts/verify-production-baseline.sh"
workspace="$(mktemp -d)"
trap 'rm -rf "$workspace"' EXIT

mock_bin="$workspace/bin"
app_root="$workspace/code-nest"
ops_root="$app_root/ops"
monitoring_dir="$app_root/monitoring"
active_root="$workspace/active"
mkdir -p "$mock_bin" "$app_root/app" "$monitoring_dir/secrets" \
  "$ops_root/nginx" "$ops_root/monitoring/grafana/provisioning/datasources" \
  "$ops_root/monitoring/grafana/provisioning/dashboards" \
  "$ops_root/monitoring/grafana/dashboards" "$ops_root/monitoring/scripts" \
  "$ops_root/systemd" "$ops_root/scripts" \
  "$active_root/nginx" "$active_root/systemd" "$active_root/bin"

cat >"$mock_bin/systemctl" <<'EOF'
#!/usr/bin/env bash
[[ "${1:-}" == "is-active" ]]
EOF

cat >"$mock_bin/curl" <<'EOF'
#!/usr/bin/env bash
url="${!#}"
if [[ " $* " == *" %{http_code} "* ]]; then
  case "$url" in
    */api/actuator | */api/actuator/* | */api/internal/sre | */api/internal/sre/*)
      printf '404'
      ;;
    *)
      printf '200'
      ;;
  esac
  exit 0
fi

case "$url" in
  */api/actuator/health)
    printf '{"status":"UP"}'
    ;;
  */api/v1/targets)
    printf '{"status":"success","data":{"activeTargets":[{"health":"up"},{"health":"up"},{"health":"up"},{"health":"up"}]}}'
    ;;
  */api/health)
    printf '{"database":"ok"}'
    ;;
  *)
    printf 'ok'
    ;;
esac
EOF

cat >"$mock_bin/jq" <<'EOF'
#!/usr/bin/env bash
input="$(cat)"
[[ "$input" != *'"health":"down"'* ]]
EOF

cat >"$mock_bin/df" <<'EOF'
#!/usr/bin/env bash
printf 'Filesystem 1024-blocks Used Available Capacity Mounted on\n'
printf '/dev/mock 41943040 1048576 %s 3%% /\n' "${MOCK_AVAILABLE_KB:-12582912}"
EOF

cat >"$mock_bin/stat" <<'EOF'
#!/usr/bin/env bash
format="${2:-}"
path="${3:-}"
case "$format:$path" in
  %a:*/secrets)
    printf '711\n'
    ;;
  %a:*/.env)
    printf '600\n'
    ;;
  %a:*/alertmanager.local.yml)
    printf '644\n'
    ;;
  %a:*)
    printf '400\n'
    ;;
  %u:* | %g:*)
    printf '65534\n'
    ;;
  *)
    exit 2
    ;;
esac
EOF

chmod +x "$mock_bin/systemctl" "$mock_bin/curl" "$mock_bin/jq" "$mock_bin/df" "$mock_bin/stat"

cp "$repo_root/deploy/nginx/code-nest-production.conf" "$ops_root/nginx/code-nest-production.conf"
cp "$repo_root/docker/monitoring/docker-compose.yml" "$ops_root/monitoring/docker-compose.yml"
cp "$repo_root/docker/monitoring/prometheus.yml" "$ops_root/monitoring/prometheus.yml"
cp "$repo_root/docker/monitoring/alert_rules.yml" "$ops_root/monitoring/alert_rules.yml"
cp "$repo_root/docker/monitoring/grafana/provisioning/datasources/prometheus.yml" \
  "$ops_root/monitoring/grafana/provisioning/datasources/prometheus.yml"
cp "$repo_root/docker/monitoring/grafana/provisioning/dashboards/code-nest.yml" \
  "$ops_root/monitoring/grafana/provisioning/dashboards/code-nest.yml"
cp "$repo_root/docker/monitoring/grafana/dashboards/code-nest-application.json" \
  "$ops_root/monitoring/grafana/dashboards/code-nest-application.json"
cp "$repo_root/docker/monitoring/grafana/dashboards/code-nest-sre.json" \
  "$ops_root/monitoring/grafana/dashboards/code-nest-sre.json"
cp "$repo_root/docker/monitoring/scripts/compose.sh" \
  "$ops_root/monitoring/scripts/compose.sh"
cp "$repo_root/docker/monitoring/scripts/validate-config.sh" \
  "$ops_root/monitoring/scripts/validate-config.sh"
cp "$repo_root/deploy/systemd/code-nest-capacity-governance.service" \
  "$ops_root/systemd/code-nest-capacity-governance.service"
cp "$repo_root/deploy/systemd/code-nest-capacity-governance.timer" \
  "$ops_root/systemd/code-nest-capacity-governance.timer"
cp "$repo_root/scripts/server-capacity-governance.sh" "$ops_root/scripts/server-capacity-governance.sh"
cp "$repo_root/scripts/sre-alertmanager-e2e.py" "$ops_root/scripts/sre-alertmanager-e2e.py"
cp "$baseline_script" "$ops_root/scripts/verify-production-baseline.sh"

cp "$ops_root/nginx/code-nest-production.conf" "$active_root/nginx/code-nest.conf"
cp -a "$ops_root/monitoring/." "$monitoring_dir/"
chmod +x "$monitoring_dir/scripts/compose.sh" "$monitoring_dir/scripts/validate-config.sh"
cp -a "$ops_root/systemd/." "$active_root/systemd/"
cp -a "$ops_root/scripts/." "$active_root/bin/"
mkdir -p "$monitoring_dir/alertmanager"
printf 'smtp-secret' >"$monitoring_dir/secrets/qq_smtp_auth_code"
printf 'webhook-secret' >"$monitoring_dir/secrets/sre_webhook_token"
printf 'GRAFANA_ADMIN_PASSWORD=test-grafana-password\n' >"$monitoring_dir/.env"
touch "$monitoring_dir/alertmanager/alertmanager.local.yml"

cat >"$app_root/app/RELEASE" <<'EOF'
version=v2.5.1
sha=1111111111111111111111111111111111111111
built_at=2026-07-28T12:00:00Z
EOF

run_baseline() {
  PATH="$mock_bin:$PATH" \
  CODE_NEST_APP_ROOT="$app_root" \
  CODE_NEST_OPS_ROOT="$ops_root" \
  CODE_NEST_NGINX_CONFIG="$active_root/nginx/code-nest.conf" \
  CODE_NEST_MONITORING_DIR="$monitoring_dir" \
  CODE_NEST_SYSTEMD_DIR="$active_root/systemd" \
  CODE_NEST_BIN_DIR="$active_root/bin" \
  CODE_NEST_EXPECTED_VERSION="v2.5.1" \
  CODE_NEST_EXPECTED_SHA="1111111111111111111111111111111111111111" \
  CODE_NEST_GRAFANA_REQUIRED="true" \
  "$baseline_script"
}

healthy_output="$(run_baseline)"
grep -Fq 'PASS check=production_baseline' <<<"$healthy_output"

printf 'drift\n' >>"$monitoring_dir/alert_rules.yml"
set +e
drift_output="$(run_baseline 2>&1)"
drift_status=$?
set -e
[[ "$drift_status" -ne 0 ]]
grep -Fq 'FAIL check=config_drift' <<<"$drift_output"
cp "$ops_root/monitoring/alert_rules.yml" "$monitoring_dir/alert_rules.yml"

set +e
disk_output="$(MOCK_AVAILABLE_KB=1024 run_baseline 2>&1)"
disk_status=$?
set -e
[[ "$disk_status" -ne 0 ]]
grep -Fq 'FAIL check=disk_capacity' <<<"$disk_output"

printf 'GRAFANA_ADMIN_PASSWORD=change-this-before-starting\n' >"$monitoring_dir/.env"
set +e
grafana_password_output="$(run_baseline 2>&1)"
grafana_password_status=$?
set -e
[[ "$grafana_password_status" -ne 0 ]]
grep -Fq 'FAIL check=grafana_credentials' <<<"$grafana_password_output"

printf 'production baseline contract passed\n'
