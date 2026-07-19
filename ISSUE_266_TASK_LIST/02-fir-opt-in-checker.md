# Task 2: Integrate Standard Kotlin Opt-In Reporting Into the FIR Checker

Progress: 100%
Status: Done

## Goal

Require the marker at the actual unsafe use: repeated Selected output items and reads of a shadowed Source name through same-layer Context.

## Current State

- Duplicate Selected items now enter the standard Kotlin opt-in path instead of unconditional duplicate-field rejection.
- Context shadowing is inspected from `orderBy`; harmless selection without a conflicting Context read is no longer rejected.
- Ordinary select, JOIN, nested, derived, and union duplicate projections now have standard opt-in diagnostics fixtures in the worktree.
- Context shadowing fixtures cover the actual `orderBy` use site, harmless unused shadowing, unshadowed Context reads, and function/expression opt-in acceptance.
- Compiler-wide opt-in uses the official `OPT_IN` test directive.
- The expanded `ProjectionDiagnosticsTest` is green: 33 tests executed with zero failures, errors, or skips.

## Completed

- Resolved the marker through `ClassId` and delegated acceptance/severity to Kotlin's standard FIR opt-in checker.
- Reused `FirOptInUsageBaseChecker` for standard scope acceptance and error severity.
- Registered shadowed Context names and added targeted duplicate/Context-use checks.

## Remaining Work

- None.

## Acceptance

- An unannotated repeated Selected name fails at the later projection item; opt-in accepts deterministic suffix allocation.
- Selecting an expression as a still-present Source name does not fail unless a same-layer Context clause reads that shadowed name.
- An unannotated shadowed Context read fails on that property access; standard expression/function/class/file/compiler-wide opt-in accepts it.
- A source-minus-then-restore alias does not require shadow opt-in, and explicitly distinct aliases never require this marker.
- Missing aliases, scalar-subquery shape errors, tuple errors, and insert-select diagnostics remain unchanged.
- One unsafe use produces one standard opt-in error, not duplicate Kronos and Kotlin diagnostics.

## Verification Record

- `ProjectionDiagnosticsTest.selectedAliasConflictsWithSource`: pass.
- `ProjectionDiagnosticsTest.projectionAliasOptInScopes`: pass.
- `ProjectionDiagnosticsTest.duplicateProjectionProperty`: pass after adding direct-field, expanded-source, and explicit-alias duplicate cases.
- `ProjectionBoxTest.duplicateProjectionOutputNames`: pass; opted-in ordinary select preserves deterministic names and independent same-name alias types.
- `ProjectionDiagnosticsTest.projectionQueryLayerOptIn`: pass for nested, derived, and union duplicate-name behavior.
- `ProjectionDiagnosticsTest.joinDuplicateProjection`: pass for required JOIN opt-in plus opted-in and non-conflicting positive cases.
- `ProjectionDiagnosticsTest.projectionCompilerWideOptIn`: pass using the official `OPT_IN` directive for duplicate Selected and shadowed Context acceptance.
- Expanded `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionDiagnosticsTest`: `BUILD SUCCESSFUL`; 33 tests passed with zero failures, errors, or skips.
- `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain`: compiler-plugin main `compileKotlin` passed with existing warnings, but the overall command failed later in `kronos-core:compileKotlin` on 12 `SelectFrom.kt` `KPojo`-to-`Source` generic mismatches; no Task 2 test was run by this command.
