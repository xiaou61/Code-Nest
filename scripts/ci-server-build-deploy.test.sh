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

printf 'deployment dependency refresh contract passed\n'
