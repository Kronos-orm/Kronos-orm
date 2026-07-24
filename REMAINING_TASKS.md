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

Status: Pending

- Define the public availability of `f.bin`; MySQL has a native implementation, while H2 and the Oracle family do not provide the same function.
- Decide whether Oracle/DM8 `repeat(value, 0)` should retain the database's `NULL` result caused by Oracle empty-string semantics or receive a compatibility expression.
- Keep unsupported-function validation and documentation aligned with the decision.

Acceptance:

- The behavior is documented, validated before SQL execution where appropriate, and covered by dialect tests.

## 3. Finish The Kotlin Function-Sugar Inventory

Status: Pending

- Produce the requested root-level inventory comparing existing Kotlin syntax sugar, public SQL functions, and common Kotlin APIs.
- Prioritize additions that have stable cross-dialect semantics and specify the compiler-plugin and renderer tests needed for each candidate.

Acceptance:

- The inventory names supported, candidate, and intentionally unsupported APIs with their target SQL semantics.

## 4. Complete The Documentation Audit

Status: Pending

- Review all field-type, annotation-mapping, and custom-dialect pages for H2/DM8 coverage.
- Keep regex, `bin`, `reverse`, aggregate ordering, and database-specific function availability clear in both languages.
- Refresh the Android example reference once the example repository's final commit is known.

Acceptance:

- English and Chinese pages describe supported behavior positively and link users to executable examples.
- No page implies that a database-specific function is universally available.

## 5. Run The Broad Verification Before Release

Status: Pending

- Run the relevant unit, compiler-plugin, and seven-database integration suites in a clean environment.
- Confirm the CI workflow uses the maintained DM8 image and that the H2/DM8 jobs remain reproducible.

Acceptance:

- Required CI gates pass with the committed code and the verification results are recorded in the release or pull-request discussion.
