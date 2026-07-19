# Task 6: Synchronize Diagnostics Documentation And Release Notes

Progress: 100%
Status: Done

## Goal

Synchronize English/Chinese docs, examples, migration guidance, release notes, and maintainer skills with the final projection, JOIN, pagination, and IDEA contracts.

## Current State

- The bilingual user docs, READMEs, release notes, and ORM guide describe the implemented projection, JOIN, pagination, result-method, select, subquery, condition, dialect, multi-tenant, and IDEA contracts.
- Repository-wide stale-API scans are clean for the removed JOIN and pagination forms and the superseded projection-collision narrative.
- The docs build was intentionally not run per user instruction; Task 6 acceptance uses static source comparison, stale-API scans, manual false-positive review, and `git diff --check`.

## Completed

- Updated both diagnostics tables and added opt-in/source-minus examples.
- Removed stale unconditional-collision remediation text.
- Documented duplicate Selected opt-in, deterministic `_N` allocation, global explicit-name reservation, explicit alias guidance, and Context-use-only opt-in in English and Chinese.
- Replaced the bilingual JOIN chapter with chained relation-to-select examples for relation types, derived and paged operands, nested source trees, selected JOIN derivation, union, self-join, duplicate names, and pagination.
- Replaced canonical pagination tuples and legacy cursor chains with `OffsetPageQuery`, `PageResult`, `CursorResult`, and immutable typed-stage examples.
- Synchronized the ORM skill, advanced projection reference, and `UnsafeProjectionOverride` annotation reference.
- Reviewed the IDEA plugin artifact description in `kronos-idea-plugin/build.gradle.kts`; its high-level project-model, projection-doc, diagnostics, and code-generator claims do not require a wording change in this scoped pass.
- Migrated bilingual dialect and multi-tenant pagination examples to `page(...).withTotal()` and named `PageResult` values.
- Replaced the remaining statement-style JOIN example in the bilingual IDEA page and refreshed the current release-note pagination bullets.
- Migrated both READMEs from `withTotal().page(...)`, tuple destructuring, and statement-style JOIN `on { ... }` to the current typed pagination and chained JOIN APIs.
- Rewrote the bilingual IDEA duplicate/Context guidance for standard opt-in, deterministic suffix allocation, explicit-name reservation, Source clauses, Context-use-only reporting, and Source-minus replacement.

## Remaining Work

- None for Task 6.

## Acceptance

- English and Chinese docs describe the same marker, allocation, Context, JOIN, pagination, and IDEA rules.
- Every copyable snippet compiles under the supported compiler configuration or is explicitly marked as a conceptual example.
- No stale old JOIN body, legacy pagination chain, duplicate-error-only rule, or unsupported Marketplace compatibility claim remains.
- No documentation claims implementation completion before Tasks 1-5 pass.
- Static example/link scans pass; a local docs build is not required by user instruction.

## Verification Record

- Bilingual user docs, READMEs, release notes, and ORM skill sync: completed by static source/test comparison; no Gradle or docs build was run per user instruction.
- Repository stale-API scans: no legacy `withCursor`, cursor `offset`, operand-argument relation call, standalone JOIN `on { ... }`, `Triple` pagination result, or superseded alias/duplicate-property guidance remains in source-maintained docs, READMEs, or the ORM skill.
- Multiline old-order scan: the only matches are separate correct examples in the bilingual result-method pages; manual inspection confirms each total-count example uses `page(...).withTotal()`.
- `git diff --check`: passed for the final worktree.
