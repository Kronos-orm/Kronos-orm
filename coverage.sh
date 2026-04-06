#!/bin/bash

script_path=$0
script_dir=$(dirname "$script_path")
cd "${script_dir}" || exit 1
source "${script_dir}"/envsetup.sh
echo "=== Checking database connectivity ==="
psql -U "${POSTGRES_USERNAME}" -d kronos_testing -c 'SHOW server_version';
mysql -u "${MYSQL_USERNAME}" -p"${MYSQL_PASSWORD}" -D kronos_testing -e 'SELECT VERSION()';

echo "=== Running unit tests with coverage ==="

echo "--- kronos-core ---"
./gradlew :kronos-core:test :kronos-core:koverLog --stacktrace 2>&1 | tee "${script_dir}/kronos-core-coverage-output.log"
echo ""

echo "--- kronos-compiler-plugin ---"
./gradlew :kronos-compiler-plugin:test :kronos-compiler-plugin:koverLog --stacktrace 2>&1 | tee "${script_dir}/kronos-compiler-plugin-coverage-output.log"
echo ""

echo "--- kronos-codegen ---"
./gradlew :kronos-codegen:test :kronos-codegen:koverLog --stacktrace 2>&1 | tee "${script_dir}/kronos-codegen-coverage-output.log"
echo ""

echo "=== Coverage Summary ==="
echo "kronos-core:              $(cat kronos-core/build/kover/coverage.txt 2>/dev/null || echo 'N/A')"
echo "kronos-compiler-plugin:   $(cat kronos-compiler-plugin/build/kover/coverage.txt 2>/dev/null || echo 'N/A')"
echo "kronos-codegen:           $(cat kronos-codegen/build/kover/coverage.txt 2>/dev/null || echo 'N/A')"
echo ""

# Extract branch coverage data
extract_branch_coverage() {
    local module=$1
    local xml_file="${module}/build/reports/kover/report.xml"
    
    if [ ! -f "$xml_file" ]; then
        echo "N/A"
        return
    fi
    
    local branch_data=$(grep -o 'BRANCH.*missed="[0-9]*".*covered="[0-9]*"' "$xml_file" | tail -1)
    if [ -z "$branch_data" ]; then
        echo "N/A"
        return
    fi
    
    local branch_missed=$(echo "$branch_data" | grep -o 'missed="[0-9]*"' | cut -d'"' -f2)
    local branch_covered=$(echo "$branch_data" | grep -o 'covered="[0-9]*"' | cut -d'"' -f2)
    local branch_total=$((branch_missed + branch_covered))
    
    if [ $branch_total -gt 0 ]; then
        local percent=$(echo "scale=2; $branch_covered * 100 / $branch_total" | bc)
        echo "Branch coverage: ${percent}% (${branch_covered}/${branch_total})"
    else
        echo "Branch coverage: 0.00% (0/0)"
    fi
}

echo "=== Branch Coverage Analysis ==="
echo "kronos-core:              $(extract_branch_coverage "kronos-core")"
echo "kronos-compiler-plugin:   $(extract_branch_coverage "kronos-compiler-plugin")"
echo "kronos-codegen:           $(extract_branch_coverage "kronos-codegen")"
echo ""
echo "=== HTML Reports ==="
echo "kronos-core:              kronos-core/build/reports/kover/html/index.html"
echo "kronos-compiler-plugin:   kronos-compiler-plugin/build/reports/kover/html/index.html"
echo "kronos-codegen:           kronos-codegen/build/reports/kover/html/index.html"
