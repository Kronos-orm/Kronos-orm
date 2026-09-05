#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${BASH_SOURCE:-}" ]]; then
  script_path="${BASH_SOURCE[0]}"
elif [[ -n "${ZSH_VERSION:-}" ]]; then
  script_path="${(%):-%N}"
else
  script_path="$0"
fi

script_dir="$(cd "$(dirname "$script_path")" && pwd)"
compose_file="${script_dir}/kronos-testing/docker-compose.integration.yml"

source "${script_dir}/envsetup.sh"

run_gradle_task() {
  local task="$1"
  local log_name="$2"
  local module="${task#:}"
  module="${module%%:*}"

  echo "=== Running ${task} ==="
  cd "${script_dir}" || exit 1
  ./gradlew "${task}" --stacktrace --console=plain 2>&1 | tee "${script_dir}/${log_name}"
  echo "=== Test report: ${module}/build/reports/tests/test/index.html ==="
}

compose() {
  docker compose -f "${compose_file}" "$@"
}

sqlserver_exec() {
  docker exec kronos-testing-sqlserver bash -lc \
    "/opt/mssql-tools18/bin/sqlcmd -S localhost -U '${SQLSERVER_USERNAME}' -P '${SQLSERVER_PASSWORD}' -C -Q \"$1\" || /opt/mssql-tools/bin/sqlcmd -S localhost -U '${SQLSERVER_USERNAME}' -P '${SQLSERVER_PASSWORD}' -Q \"$1\""
}

if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon is not reachable. Start Docker Desktop or set up Docker before running ./test.sh." >&2
  exit 1
fi

echo "=== Starting integration databases with Docker ==="
compose up -d --wait

if [[ "${KRONOS_TESTING_KEEP_DOCKER:-false}" != "true" ]]; then
  cleanup() {
    echo "=== Stopping integration databases ==="
    compose down
  }
  trap cleanup EXIT
fi

echo "=== Preparing SQL Server database ==="
sqlserver_exec "IF DB_ID('${SQLSERVER_DATABASE}') IS NULL CREATE DATABASE ${SQLSERVER_DATABASE}; SELECT DB_NAME(DB_ID('${SQLSERVER_DATABASE}'));"

echo "=== Running tests ==="
run_gradle_task ":kronos-testing:test" "kronos-testing-output.log"
run_gradle_task ":kronos-core:test" "kronos-core-output.log"
run_gradle_task ":kronos-syntax:test" "kronos-syntax-output.log"
run_gradle_task ":kronos-codegen:test" "kronos-codegen-output.log"
run_gradle_task ":kronos-compiler-plugin:test" "kronos-compiler-plugin-output.log"
