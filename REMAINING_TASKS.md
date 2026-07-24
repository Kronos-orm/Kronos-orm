# Remaining Tasks

Updated: 2026-07-24

This file records follow-up work after the H2/DM8 dialect, JDBC mapping, Android support, and function-rendering updates in the current branch. It deliberately lists only work that still needs a decision, broader proof, or a separate focused change.

## 1. Complete The Function Compatibility Matrix

Status: Pending

- Execute every public `f.*` function against each supported database and record the expected value, null behavior, and unsupported cases.
- Expand real-database coverage beyond the current representative checks for `right`, `join`, `log`, `trunc`, `groupConcat`, and `rowNumber`.
- Cover regular expressions, `reverse`, `bin`, aggregate ordering, window functions, and boundary values such as empty strings and zero repetition.

Acceptance:

- Each supported function/dialect pair has either a passing result assertion or an explicit, user-facing unsupported contract.
- The integration suite exercises the rendered SQL against MySQL, PostgreSQL, SQLite, SQL Server, Oracle, H2, and DM8 where the feature is supported.

## 2. Decide The Remaining Cross-Dialect Semantics

Status: Implementation Complete, Execution Verification Deferred (2026-07-24)

- `f.bin(x)` is MySQL-only and returns binary text (`String?`). Every other built-in dialect rejects it during SQL build rather than emitting a database-specific approximation.
- Oracle and DM8 retain their native `f.repeat(value, 0) == NULL` behavior. Kronos does not add a compatibility expression that would claim Kotlin empty-string semantics.
- Renderer tests, core dialect-build tests, and an Oracle/DM8 real-database integration suite now express those contracts. The user deferred test execution.

Acceptance:

- The behavior is documented, rejected before SQL execution where appropriate, and has focused test coverage ready to run.

## 3. Finish The Kotlin Function-Sugar Inventory

Status: Done (2026-07-24)

- Completed the requested root-level audit in [KOTLIN_FUNCTION_SUGAR_INVENTORY.md](KOTLIN_FUNCTION_SUGAR_INVENTORY.md), using Kotlin 2.4 standard-library source and the current compiler rule model as evidence.
- The inventory records actual scalar source forms, separates direct scalar `minOf` / `maxOf` calls from aggregates, and excludes collection, range, regex, locale, lambda, and other non-scalar inputs.

Acceptance:

- The inventory names currently supported and transformable scalar APIs, their target SQL semantics, and the compiler-plugin / renderer / integration proof required for implementation.

## 4. Complete The H2/DM8 Documentation Audit

Status: Done, Documentation Build Deferred (2026-07-24)

- Reviewed the English and Chinese dialect support, connection, field-type, annotation, index, custom-dialect, README, and function-availability pages against `H2Statements`, `Dm8Statements`, `OracleStatements`, and renderer rules.
- H2/DM8 support, H2 `MERGE`, DM8 identity columns, JDBC generated keys, type fragments, `bin`, `reverse`, `groupConcat`, and zero repetition now have matched user-facing wording in both languages.
- Documentation build is intentionally not run for this audit.

Acceptance:

- English and Chinese pages describe supported behavior positively and link users to executable examples.
- No page implies that a database-specific function is universally available.

## 5. Run The Broad Verification Before Release

Status: Pending

- Run the relevant unit, compiler-plugin, and seven-database integration suites in a clean environment.
- Confirm the CI workflow uses the maintained DM8 image and that the H2/DM8 jobs remain reproducible.

Acceptance:

- Required CI gates pass with the committed code and the verification results are recorded in the release or pull-request discussion.

## 6. Refresh The Android Example Reference

Status: Pending External Commit

- Refresh the Android example reference after the example repository's final commit is known.

Acceptance:

- The Android documentation links to the final example revision and the referenced setup still compiles.

## Verification Record

2026-07-24:

- Static evidence reviewed: Kotlin 2.4 standard-library source, `KotlinSqlFunctionRules`, `TypeUtils`, H2/DM8/Oracle statement renderers, focused test sources, and English/Chinese user documentation.
- Not run: unit, compiler-plugin, integration, and documentation builds. Test execution was deferred by request; Task 5 remains the release gate.
