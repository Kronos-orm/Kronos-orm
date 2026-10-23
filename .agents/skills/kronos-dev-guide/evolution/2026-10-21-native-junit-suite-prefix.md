# Gradle Native JUnit reports prefix the target test task

## Symptom

The Native SQLite consumer builds successfully and its real Native ORM suite executes five cases, but
`verify-kmp-test-results.py --require-suite com.kotlinorm.example.ios.NativeOrmIntegrationTest` rejects the
JUnit report. This is a suite-name mismatch, not a missing test execution.

## Confirmed format and fix

Kotlin Gradle plugin 2.4.21 writes the Native test task name before both `testsuite.name` and
`testcase.classname`. For `macosX64Test`, the exact name is
`macosX64Test.com.kotlinorm.example.ios.NativeOrmIntegrationTest`.

Pass `${target}Test.com.kotlinorm.example.ios.NativeOrmIntegrationTest` for each Gradle Native report
folder. When checking several targets, invoke the report guard separately for each target so each folder
must contain its own exact expected suite. Keep the canonical class name for direct Native GoogleTest
binary execution; that report path is independent of Gradle's JUnit renderer.

## Verification

The final Toolchain-published repository was consumed through an exclusive Maven repository by
`kronos-example-ios` revision `a552f6fd3fd0918395903c8a777b4140d2d5ea13`. On macosX64, all 43 real AndroidX
SQLite tests passed, including all five Native ORM integration tests, with zero errors, failures or skips.
The real ORM console inserted and read ID 1. The same 43 cases and ORM console also passed through the official SQLite C API backend, again with
zero errors, failures or skips. The exact prefixed suite guard passed against both saved JUnit reports. The Native SQLite reusable workflow passed actionlint after applying this target-specific
suite name. Logs and frozen reports: `/tmp/kronos-final-published-sqlite-androidx1.log` and
`/tmp/kronos-final-published-sqlite-androidx1/macosX64Test/`.
