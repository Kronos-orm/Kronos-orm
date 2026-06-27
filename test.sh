#!/bin/bash
set -euo pipefail

script_path="${BASH_SOURCE[0]}"
script_dir="$(cd "$(dirname "$script_path")" && pwd)"
source "${script_dir}/envsetup.sh"

mysql_args=(-u "${MYSQL_USERNAME}")
if [[ -n "${MYSQL_PASSWORD}" ]]; then
  mysql_args+=("-p${MYSQL_PASSWORD}")
fi

echo "=== Checking database connectivity ==="
PGPASSWORD="${POSTGRES_PASSWORD}" psql -U "${POSTGRES_USERNAME}" -d postgres -c "CREATE DATABASE ${POSTGRES_DATABASE}" 2>/dev/null || true
PGPASSWORD="${POSTGRES_PASSWORD}" psql -U "${POSTGRES_USERNAME}" -d "${POSTGRES_DATABASE}" -c 'SHOW server_version';
mysql "${mysql_args[@]}" -e "CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE}; CREATE DATABASE IF NOT EXISTS kronos;"
mysql "${mysql_args[@]}" -D "${MYSQL_DATABASE}" -e 'SELECT VERSION()';

echo "=== Running tests ==="
cd "${script_dir}" || exit 1;
./gradlew :kronos-testing:test --stacktrace 2>&1 | tee "${script_dir}/kronos-testing-output.log"
echo "=== Test report: kronos-testing/build/reports/tests/test/index.html ==="

./gradlew :kronos-core:test --stacktrace 2>&1 | tee "${script_dir}/kronos-core-output.log"
echo "=== Test report: kronos-core/build/reports/tests/test/index.html ==="

./gradlew :kronos-codegen:test --stacktrace 2>&1 | tee "${script_dir}/kronos-codegen-output.log"
echo "=== Test report: kronos-codegen/build/reports/tests/test/index.html ==="

./gradlew :kronos-compiler-plugin:test --stacktrace 2>&1 | tee "${script_dir}/kronos-compiler-plugin-output.log"
echo "=== Test report: kronos-compiler-plugin/build/reports/tests/test/index.html ==="
