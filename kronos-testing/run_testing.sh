#!/bin/bash

script_path=$0
script_dir=$(dirname "$script_path")
source "${script_dir}"/envsetup.sh
echo "=== Checking database connectivity ==="
psql -U "${POSTGRES_USERNAME}" -d kronos_testing -c 'SHOW server_version';
mysql -u "${MYSQL_USERNAME}" -p"${MYSQL_PASSWORD}" -D kronos_testing -e 'SELECT VERSION()';
echo "=== Running tests ==="
cd "${script_dir}"/../ || exit 1;
./gradlew :kronos-testing:test --stacktrace 2>&1 | tee "${script_dir}/test-output.log"
echo "=== Test report: kronos-testing/build/reports/tests/test/index.html ==="