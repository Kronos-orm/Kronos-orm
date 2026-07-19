# Projection, JOIN, Pagination, And IDEA Compatibility Task List

Updated: 2026-07-20

This document is based on [Issue 266](https://github.com/Kronos-orm/Kronos-orm/issues/266), the current uncommitted implementation branch, the agreed projection-name rules, the JOIN API design discussion, the pagination type-stage design, and the 2026-07-19 IDEA 2026.2 incompatibility screenshot. It is the implementation and verification plan for finishing the projection work and the related query/API boundaries without preserving the old JOIN syntax.

> **Current focus: finish the Task 10 IDEA 2026.2 artifact proof. Tasks 1-9 are verified 100% and Done; the final compiler-plugin gate is green at 281/281, the non-root JOIN cascade regression is green, and the formal IDEA 2026.2 test gate is green at 32/32. Signed ZIP, Plugin Verifier, Marketplace, and installation proof remain.**

## Entrypoints

- [Design locks](00-design-locks.md)
- [Recommended implementation order](implementation-order.md)
- [Current verification gaps](verification-gaps.md)
- [Recent verification](verification-log.md)
- [Out of scope](out-of-scope.md)

## Task Documents

- [Task 1: Lock the public opt-in contract](01-public-opt-in-contract.md)
- [Task 2: Integrate standard Kotlin opt-in reporting into the FIR checker](02-fir-opt-in-checker.md)
- [Task 3: Preserve effective projection shape and selected types](03-projection-model-and-types.md)
- [Task 4: Materialize overridden fields with the selected type and metadata](04-ir-materialization-and-runtime.md)
- [Task 5: Build the compiler and runtime verification matrix](05-verification-matrix.md)
- [Task 6: Synchronize diagnostics documentation and release notes](06-documentation-sync.md)
- [Task 7: Redesign JOIN as a composable FROM source tree](07-join-from-source-api.md)
- [Task 8: Introduce mutually exclusive page and cursor query stages](08-pagination-query-stages.md)
- [Task 9: Align IDEA projection analysis and presentation](09-idea-projection-support.md)
- [Task 10: Publish an IDEA 2026.2-installable plugin](10-idea-2026-2-compatibility.md)

## Current Judgment

- Duplicate Selected names are now intentionally supported only after `UnsafeProjectionOverride` opt-in; all values are preserved with deterministic `_N` suffixes.
- Context shadowing is checked at the conflicting Context use, such as `orderBy { it.id }`, rather than at a harmless `select { expression.alias("id") }` that never reads the shadowed Context field.
- Traversal-order-independent generated Selected materialization is implemented. The current serialized `DslIntegrationBoxTest` completed with `BUILD SUCCESSFUL` in 1m16s and all 23 tests passed, including operand-matrix and non-root JOIN receiver coverage.
- The compiler pipeline verifies generated terminals, recursive JOIN shapes, all 12 table / derived `KSelectable` / nested `JoinSource` operand-matrix shapes, derived-plus-union composition, and pagination type stages. Column qualification now follows one source-aware rule: a field renders against the `FromSource` that exposes it, so derived outputs retain aliases such as `q.rankStatus` in both direct selectable and planner paths.
- Targeted `JoinSelectableSqlTest` plus `SelectFromPlannerCoverageTest` verification completed with `BUILD SUCCESSFUL` in 1m48s. The subsequent exact core JOIN/pagination/CTAS/insert-select/MySQL JOIN/union batch completed with `BUILD SUCCESSFUL` in 26s and all 114 tests passed, clearing all eight historical failures.
- The latest `ProjectionDiagnosticsTest` run completed with `BUILD SUCCESSFUL`; all 33 tests passed with zero failures, errors, or skips. It covers standard `OPT_IN_USAGE_ERROR` behavior for ordinary select, JOIN, nested, derived, union, Context use sites, expression/file/compiler-wide opt-in, and false-positive boundaries.
- The latest `ProjectionBoxTest` run completed with `BUILD SUCCESSFUL` in 1m36s; its XML report records all 22 tests passing.
- The production static scan is green: removed `SelectFrom2..16`, `PagedClause`, `OffsetPageable`, and `CursorClause` symbols are absent; no `/tmp` debug writes or old pre-select forwarding APIs remain. Historical release-note mentions and one legacy-named test class are non-production references.
- After test-only `UserRelation` isolation, focused `UnionClauseBehaviorTest` completed with `BUILD SUCCESSFUL` in 1m21s and all 7 tests passed. `NoneDataSourceWrapper` limit planning is proven dialect-neutral, while explicit SQL Server still rejects `limit` without `orderBy`.
- The non-root JOIN fixture proves that a directly selected child field keeps its captured receiver type and survives a derived select even when the root has a same-name property of a different type.
- The non-root JOIN cascade regression proves the selected child relation keeps its hidden local key: SQL emits physical `profile_id AS profileId` without a `profile` column, mapping retains hidden `profileId`, and cascade backfills `profile`.
- The final-worktree compiler-plugin gate is green at 281/281. Before that full rerun, the focused `ConditionBoxTest` passed 38/38 after its captured `length` fixture was aligned with the intentional `StringFunctions.length -> Int?` contract; the focused projection diagnostics suite remains green at 33/33.
- The current full core gate completed with `BUILD SUCCESSFUL` in 32s. Aggregated XML covers 87 suites and 756 tests with zero failures, errors, or skipped tests; final `git diff --check` passed, and production legacy/debug scans again returned no matches.
- Offset pagination, total-count pagination, and cursor pagination use distinct helper types so invalid same-layer combinations are absent from autocomplete and fail at compile time. `OffsetPageQuery` is the one relational stage: it remains composable as a `KSelectable`, while total and cursor stages remain execution-only.
- The exact pagination gate is green across MySQL, PostgreSQL, SQL Server, Oracle, and SQLite: six inherited scenarios per database, 30/30 total, with no image pull or container recreation.
- Bilingual source-maintained docs, both READMEs, release notes, and the ORM guide now match the final projection, JOIN, pagination, and IDEA contracts. Repository stale-API scans and `git diff --check` pass; no docs build was run per user instruction.
- IDEA projection support now distinguishes direct rows, selectable queries, collections, execution stages, and result envelopes; completion is restricted to direct rows, while documentation retains recursive shape discovery. Shared declaration/documentation rendering preserves bridge types, nullable defaults, allocated suffixes, and deterministic class deduplication. The formal IU-262.8665.258 fixture and complete IDEA plugin test gate now pass 32/32, closing Task 9; signed artifact, Plugin Verifier, Marketplace, and installation proof remain isolated in Task 10.
- The IDEA build/workflow now statically declares the explicit `262..262.*` range, Temurin 25, signed-archive publication, fail-fast checks, and artifact inspection; signing, Plugin Verifier, uploaded-artifact, and Marketplace-installation proof remain pending.

## Overview

| Progress | Task | Status | Notes |
|----------|------|--------|-------|
| 100% | Task 1: Public opt-in contract | Done | The marker now describes both deterministic suffixing and Context replacement. |
| 100% | Task 2: FIR opt-in checker | Done | The expanded 33-test diagnostics suite is green across ordinary select, JOIN, nested, derived, union, Context use sites, standard opt-in scopes, and false-positive boundaries. |
| 100% | Task 3: Projection model and types | Done | Final compiler 281/281 plus the non-root JOIN cascade regression prove selected types, resolved names, hidden local keys, and nested/JOIN/union propagation. |
| 100% | Task 4: IR materialization and runtime | Done | Typed/Map mapping, SQL labels, hidden `profileId`, and cascade backfill agree across the focused projection/runtime matrix. |
| 100% | Task 5: Verification matrix | Done | Registration, focused suites, compiler 281/281, core 756/756, composition 114/114, five-database 30/30, and the non-root JOIN cascade regression are green; IDEA artifact proof remains Task 10. |
| 100% | Task 6: Documentation sync | Done | Bilingual docs, READMEs, release notes, and ORM skill match the final contracts; stale-API scans and final diff checks are green. |
| 100% | Task 7: JOIN FromSource API | Done | Full compiler-plugin and core gates pass; final diff and production legacy/debug sanity are clean. |
| 100% | Task 8: Pagination query stages | Done | Full core is 756/756, full compiler is 270/270, the five-database pagination gate is 30/30, and final static/diff checks are green. |
| 100% | Task 9: IDEA projection support | Done | Carrier-aware completion, exact Analysis/FIR type rendering, deterministic declaration presentation, cancellation propagation, latest-session bridge replacement, formal IU-262.8665.258 fixture coverage, and the complete 32/32 IDEA test gate are green. |
| 35% | Task 10: IDEA 2026.2 compatibility | In Progress | Explicit 262 range, JDK 25, signed-archive binding, fail-fast gates, and artifact inspection are statically reviewed; signing and Verifier have not run. |

## Success Definition

This workstream is complete only when projection names and types agree across FIR, IR, SQL labels, typed mapping, `toMap`, nested queries, union, and IDEA; JOINs can form both flat and nested `FromSource` trees while returning the generated Selected type; page/cursor combinations are constrained by public types; and the signed Marketplace artifact installs into IntelliJ IDEA 2026.2.
