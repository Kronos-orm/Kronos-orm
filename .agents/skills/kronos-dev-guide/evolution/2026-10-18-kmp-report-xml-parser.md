# KMP report guards must reject unsafe XML and require executed tests

## Symptom

JUnit report validation must reject entities and external resources while preserving nested Android
report discovery and required-suite checks. A migration of the gate's scripting language must preserve
these execution and parser constraints.

## Confirmed approach

- Run `.github/scripts/verify-kmp-test-results.main.kts` through `scripts/run`; it imports the shared
  `xml(Path)` function from `scripts/common.main.kts`.
- The JDK DOM factory explicitly disallows DOCTYPE declarations, disables external general and
  parameter entities, and sets external DTD/schema access to the empty string. XML parser failures
  fail the gate. These settings need no separate Python dependency installation.
- Discover `TEST-*.xml` recursively, including Android device directories. Reject missing reports,
  failing suites or cases, and skipped-only execution. Each required suite must contain a successful,
  executed test case; unrelated passing tests cannot substitute for it.

## Verification

The official Kotlin 2.4.21 script runner passed 10 actual JUnit-input checks, including nested reports,
case-level failures, skipped required suites, internal entity declarations, and external DOCTYPEs.
The helper also accepted 43 actual Native SQLite JUnit tests with the required ORM suite and the 25
compiler configuration tests. Evidence: `/tmp/kronos-gate-kts-verification73/verification.json` and
`/tmp/kronos-junit-helper-real78.log`.
