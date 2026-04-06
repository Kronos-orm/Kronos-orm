#!/bin/bash

script_path=$0
script_dir=$(dirname "$script_path")
source "${script_dir}"/envsetup.sh
echo "=== Checking database connectivity ==="
psql -U "${POSTGRES_USERNAME}" -d kronos_testing -c 'SHOW server_version';
mysql -u "${MYSQL_USERNAME}" -p"${MYSQL_PASSWORD}" -D kronos_testing -e 'SELECT VERSION()';
echo "=== Running tests ==="
cd "${script_dir}"/../ || exit 1;
./gradlew :kronos-testing:test --stacktrace 2>&1 | tee "${script_dir}/kronos-testing-output.log"
echo "=== Test report: kronos-testing/build/reports/tests/test/index.html ==="

./gradlew :kronos-core:test --stacktrace 2>&1 | tee "${script_dir}/kronos-core-output.log"
echo "=== Test report: kronos-testing/build/reports/tests/test/index.html ==="

./gradlew :kronos-codegen:test --stacktrace 2>&1 | tee "${script_dir}/kronos-codegen-output.log"
echo "=== Test report: kronos-testing/build/reports/tests/test/index.html ==="

./gradlew :kronos-compiler-plugin:test --stacktrace 2>&1 | tee "${script_dir}/kronos-compiler-plugin-output.log"
echo "=== Test report: kronos-testing/build/reports/tests/test/index.html ==="