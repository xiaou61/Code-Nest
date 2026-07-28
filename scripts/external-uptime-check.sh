#!/usr/bin/env bash
set -Eeuo pipefail

ENDPOINTS_TEXT="${CODE_NEST_UPTIME_ENDPOINTS:-http://36.140.150.167:81/ http://36.140.150.167:82/}"
ATTEMPTS="${CODE_NEST_UPTIME_ATTEMPTS:-3}"
RETRY_DELAY_SECONDS="${CODE_NEST_UPTIME_RETRY_DELAY_SECONDS:-10}"
CONNECT_TIMEOUT_SECONDS="${CODE_NEST_UPTIME_CONNECT_TIMEOUT_SECONDS:-5}"
MAX_TIME_SECONDS="${CODE_NEST_UPTIME_MAX_TIME_SECONDS:-15}"

fail() {
  printf 'ERROR %s\n' "$*" >&2
  exit 2
}

assert_bounded_integer() {
  local name="$1"
  local value="$2"
  local minimum="$3"
  local maximum="$4"
  [[ "$value" =~ ^[0-9]+$ ]] || fail "$name must be an integer"
  ((value >= minimum && value <= maximum)) \
    || fail "$name must be between $minimum and $maximum"
}

write_github_output() {
  local overall="$1"
  local checked_at="$2"
  local details="$3"

  [[ -n "${GITHUB_OUTPUT:-}" ]] || return 0
  {
    printf 'overall=%s\n' "$overall"
    printf 'checked_at=%s\n' "$checked_at"
    printf 'details<<CODE_NEST_UPTIME_EOF\n'
    printf '%s\n' "$details"
    printf 'CODE_NEST_UPTIME_EOF\n'
  } >>"$GITHUB_OUTPUT"
}

main() {
  command -v curl >/dev/null 2>&1 || fail "curl is required"
  assert_bounded_integer CODE_NEST_UPTIME_ATTEMPTS "$ATTEMPTS" 1 5
  assert_bounded_integer CODE_NEST_UPTIME_RETRY_DELAY_SECONDS "$RETRY_DELAY_SECONDS" 0 60
  assert_bounded_integer CODE_NEST_UPTIME_CONNECT_TIMEOUT_SECONDS "$CONNECT_TIMEOUT_SECONDS" 1 30
  assert_bounded_integer CODE_NEST_UPTIME_MAX_TIME_SECONDS "$MAX_TIME_SECONDS" 1 60

  local -a endpoints
  local -a failures=()
  local endpoint
  local attempt
  local status
  local endpoint_up
  read -r -a endpoints <<<"$ENDPOINTS_TEXT"
  ((${#endpoints[@]} > 0)) || fail "at least one endpoint is required"

  for endpoint in "${endpoints[@]}"; do
    [[ "$endpoint" =~ ^https?://[^[:space:]]+$ ]] || fail "invalid endpoint: $endpoint"
    endpoint_up=false
    status="unreachable"
    for ((attempt = 1; attempt <= ATTEMPTS; attempt++)); do
      if status="$(curl \
          --silent \
          --show-error \
          --output /dev/null \
          --write-out '%{http_code}' \
          --connect-timeout "$CONNECT_TIMEOUT_SECONDS" \
          --max-time "$MAX_TIME_SECONDS" \
          "$endpoint" 2>/dev/null)"; then
        :
      else
        status="unreachable"
      fi
      printf 'probe endpoint=%s attempt=%s/%s status=%s\n' \
        "$endpoint" "$attempt" "$ATTEMPTS" "$status"
      if [[ "$status" == "200" ]]; then
        endpoint_up=true
        break
      fi
      if ((attempt < ATTEMPTS)); then
        sleep "$RETRY_DELAY_SECONDS"
      fi
    done

    if [[ "$endpoint_up" != "true" ]]; then
      failures+=("$endpoint status=$status attempts=$ATTEMPTS")
    fi
  done

  local checked_at
  local details
  checked_at="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  if ((${#failures[@]} == 0)); then
    details="All configured endpoints returned HTTP 200."
    write_github_output up "$checked_at" "$details"
    printf 'external uptime status=up checked_at=%s\n' "$checked_at"
    return 0
  fi

  details="$(printf '%s\n' "${failures[@]}")"
  write_github_output down "$checked_at" "$details"
  printf 'external uptime status=down checked_at=%s\n%s\n' "$checked_at" "$details" >&2
  return 1
}

main "$@"
