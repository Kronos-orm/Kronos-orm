# Task 4: Materialize Overridden Fields With The Selected Type And Metadata

Progress: 100%
Status: Done

## Goal

Make IR materialization, generated metadata, SQL labels, typed mapping, Map mapping, and query composition honor finalized resolved names and selected expression types.

## Current State

- Materialization uses FIR selected types and avoids inheriting source-only metadata for derived replacements.
- Runtime projection items can carry allocated output names and planners render them as SQL aliases.
- Focused JOIN/nested/union, result-method, and non-root JOIN cascade runtime coverage is green.

## Completed

- Added FIR-to-IR type conversion for class-like selected field types.
- Derived aliases no longer inherit `@Serialize` metadata or source type solely because names match.
- Added runtime mapping and `__columns` assertions in `selectedAliasOverrideType.kt`.

## Remaining Work

- None for the Issue 266 IR materialization and runtime scope. Future projection forms must continue to consume the same allocated labels and selected types.

## Acceptance

- A `String` source field replaced by an `Int` selected expression produces an `Int` generated property in IR and at runtime.
- Mapping a database result map with the overridden alias populates the generated property without `ClassCastException`, silent `Any?`, or conversion through the source type.
- `__columns` reports the logical alias and selected type/metadata expected by downstream mapping.
- Direct source aliases preserve valid source metadata; derived aliases do not inherit incompatible source-only metadata.
- SQL rendering, parameter binding, query result mapping, and derived-query chaining agree on the selected expression type.
- Duplicate opted-in fields remain present as `id`, `id_1`, and later suffixes in SQL rows, typed results, and Map results.
- No consumer independently invents a different suffix or applies allocation twice.
- No reflection-based runtime workaround or blanket `Any?` fallback is introduced.

## Verification Record

- IR materializer lines 236-243 identified as the concrete type-risk location: completed read-only.
- `ProjectionBoxTest.selectedAliasOverrideType`: pass.
- Earlier full `ProjectionBoxTest`: pass; the expanded runtime/JOIN/union/Map matrix is supplemented by the non-root JOIN cascade regression.
- `ProjectionBoxTest.duplicateProjectionOutputNames`: pass; generated properties preserve `id`, `id_2`, and `id_1`, different same-name alias types remain `Int` and `String`, and typed/Map/derived/union consumers agree.
- `MysqlSelectSqlTest.testDuplicateProjectionNamesReserveExplicitSuffixes`: passed within the full 741-test core run; SQL labels and `resultColumnTypes` match the allocator.
- `KronosResultMappingTest.map rows preserve allocated duplicate projection labels and target types`: pass; two allocated labels (`id`, `id_1`) remain separate Map keys and their JDBC numeric values are converted independently to `Int` and `Long` through `resultColumnTypes`.
- The final-worktree compiler-plugin gate passed 281/281, including typed projection, JOIN/nested/union composition,
  and result-method box coverage; IDEA presentation stays isolated in Task 9.
- `ProjectionBoxTest.joinNonRootCascadeProjection`: pass; generated SQL uses `profile_id AS profileId` even though
  `profile` is not a physical column, the hidden `profileId` survives mapping, and the cascade query backfills `profile`.
