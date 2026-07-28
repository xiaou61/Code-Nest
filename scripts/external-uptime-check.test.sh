#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
check_script="$repo_root/scripts/external-uptime-check.sh"
workspace="$(mktemp -d)"
trap 'rm -rf "$workspace"' EXIT

mock_bin="$workspace/bin"
call_log="$workspace/calls.log"
state_dir="$workspace/state"
mkdir -p "$mock_bin" "$state_dir"

cat >"$mock_bin/curl" <<'EOF'
#!/usr/bin/env bash
url="${!#}"
key="$(printf '%s' "$url" | tr -c 'A-Za-z0-9' '_')"
counter_file="$MOCK_STATE_DIR/$key"
count=0
[[ ! -f "$counter_file" ]] || count="$(cat "$counter_file")"
count=$((count + 1))
printf '%s' "$count" >"$counter_file"
printf '%s %s\n' "$url" "$count" >>"$MOCK_CALL_LOG"

case "$MOCK_MODE" in
  healthy)
    printf '200'
    ;;
  retry)
    if ((count < 3)); then printf '503'; else printf '200'; fi
    ;;
  down)
    printf '503'
    ;;
  *)
    exit 2
    ;;
esac
EOF

cat >"$mock_bin/sleep" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
chmod +x "$mock_bin/curl" "$mock_bin/sleep"

run_check() {
  local mode="$1"
  local output_file="$2"
  rm -rf "$state_dir"
  mkdir -p "$state_dir"
  : >"$call_log"
  PATH="$mock_bin:$PATH" \
  MOCK_MODE="$mode" \
  MOCK_STATE_DIR="$state_dir" \
  MOCK_CALL_LOG="$call_log" \
  CODE_NEST_UPTIME_ENDPOINTS="http://example.test:81/ http://example.test:82/" \
  CODE_NEST_UPTIME_ATTEMPTS=3 \
  CODE_NEST_UPTIME_RETRY_DELAY_SECONDS=0 \
  GITHUB_OUTPUT="$output_file" \
  bash "$check_script"
}

healthy_output="$workspace/healthy.out"
run_check healthy "$healthy_output"
grep -Fqx 'overall=up' "$healthy_output"
[[ "$(wc -l <"$call_log")" -eq 2 ]]

retry_output="$workspace/retry.out"
run_check retry "$retry_output"
grep -Fqx 'overall=up' "$retry_output"
[[ "$(wc -l <"$call_log")" -eq 6 ]]

down_output="$workspace/down.out"
set +e
run_check down "$down_output"
down_status=$?
set -e
[[ "$down_status" -ne 0 ]]
grep -Fqx 'overall=down' "$down_output"
grep -Fq 'http://example.test:81/' "$down_output"
grep -Fq 'http://example.test:82/' "$down_output"
[[ "$(wc -l <"$call_log")" -eq 6 ]]

printf 'external uptime probe contract passed\n'
