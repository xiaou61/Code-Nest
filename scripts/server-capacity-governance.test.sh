#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
script="$repo_root/scripts/server-capacity-governance.sh"
workspace="$(mktemp -d)"
trap 'rm -rf "$workspace"' EXIT

app_root="$workspace/code-nest"
runner_root="$app_root/actions-runner"
source_dir="$runner_root/source"
release_backups="$app_root/backups/releases"
database_backups="$app_root/backups/database"
build_releases="$runner_root/releases"
mkdir -p "$release_backups" "$database_backups" "$build_releases"

for index in 1 2 3 4 5; do
  mkdir -p "$release_backups/release-$index"
  printf 'release-%s\n' "$index" >"$release_backups/release-$index/app.jar"
  touch -d "2026-01-0$index 00:00:00" "$release_backups/release-$index"

  printf 'database-%s\n' "$index" >"$database_backups/2026010${index}000000.sql.gz"
  touch -d "2026-01-0$index 00:00:00" "$database_backups/2026010${index}000000.sql.gz"

  printf 'bundle-%s\n' "$index" >"$build_releases/code-nest-v$index.tar.gz"
  touch -d "2026-01-0$index 00:00:00" "$build_releases/code-nest-v$index.tar.gz"
done

mkdir -p \
  "$app_root/release-stage.abcd" \
  "$runner_root/bin.1" \
  "$runner_root/bin.2" \
  "$runner_root/externals.1" \
  "$runner_root/externals.2" \
  "$runner_root/_work/_update" \
  "$source_dir/vue3-admin-front/node_modules" \
  "$source_dir/vue3-user-front/dist" \
  "$source_dir/xiaou-sre/target"
ln -s "$runner_root/bin.2" "$runner_root/bin"
ln -s "$runner_root/externals.2" "$runner_root/externals"
printf 'runner archive\n' >"$runner_root/actions-runner-linux-x64-1.tar.gz"
touch -d '2 days ago' "$app_root/release-stage.abcd"

export CODE_NEST_APP_ROOT="$app_root"
export CODE_NEST_RUNNER_ROOT="$runner_root"
export CODE_NEST_SOURCE_DIR="$source_dir"
export CODE_NEST_RELEASE_BACKUP_DIR="$release_backups"
export CODE_NEST_DATABASE_BACKUP_DIR="$database_backups"
export CODE_NEST_BUILD_RELEASE_DIR="$build_releases"
export CODE_NEST_RUNNER_CURRENT_BIN="$runner_root/bin.2"
export CODE_NEST_RUNNER_CURRENT_EXTERNALS="$runner_root/externals.2"
export CODE_NEST_CAPACITY_LOCK_FILE="$workspace/capacity.lock"
export CODE_NEST_KEEP_RELEASE_BACKUPS=2
export CODE_NEST_KEEP_DATABASE_BACKUPS=3
export CODE_NEST_KEEP_BUILD_BUNDLES=2
export CODE_NEST_ORPHAN_STAGE_MINUTES=60

report="$(bash "$script" --include-build-outputs --include-runner-cache)"
grep -q 'would_remove category=release_backup' <<<"$report"
grep -q 'would_remove category=generated_build_output' <<<"$report"
grep -q 'would_remove category=runner_old_version' <<<"$report"
grep -q 'would_remove category=runner_update_cache' <<<"$report"
[[ "$(find "$release_backups" -mindepth 1 -maxdepth 1 -type d | wc -l)" -eq 5 ]]
[[ -d "$app_root/release-stage.abcd" ]]
[[ -d "$source_dir/vue3-admin-front/node_modules" ]]
[[ -d "$runner_root/bin.1" ]]
[[ -d "$runner_root/_work/_update" ]]

bash "$script" --apply
[[ "$(find "$release_backups" -mindepth 1 -maxdepth 1 -type d | wc -l)" -eq 2 ]]
[[ "$(find "$database_backups" -maxdepth 1 -type f | wc -l)" -eq 3 ]]
[[ "$(find "$build_releases" -maxdepth 1 -type f | wc -l)" -eq 2 ]]
[[ ! -d "$app_root/release-stage.abcd" ]]
[[ -d "$source_dir/vue3-admin-front/node_modules" ]]

bash "$script" --apply --include-build-outputs
[[ ! -d "$source_dir/vue3-admin-front/node_modules" ]]
[[ ! -d "$source_dir/vue3-user-front/dist" ]]
[[ ! -d "$source_dir/xiaou-sre/target" ]]
[[ -d "$runner_root/bin.1" ]]

bash "$script" --apply --include-runner-cache
[[ ! -d "$runner_root/bin.1" ]]
[[ ! -d "$runner_root/externals.1" ]]
[[ ! -d "$runner_root/_work/_update" ]]
[[ ! -f "$runner_root/actions-runner-linux-x64-1.tar.gz" ]]
[[ -d "$runner_root/bin.2" ]]
[[ -d "$runner_root/externals.2" ]]
if [[ -L "$runner_root/bin" && -L "$runner_root/externals" ]]; then
  [[ "$(readlink -f "$runner_root/bin")" == "$(readlink -f "$runner_root/bin.2")" ]]
  [[ "$(readlink -f "$runner_root/externals")" == "$(readlink -f "$runner_root/externals.2")" ]]
fi

if CODE_NEST_APP_ROOT=/ bash "$script" --apply >/dev/null 2>&1; then
  printf 'unsafe root was accepted\n' >&2
  exit 1
fi

printf 'capacity governance contract passed\n'
