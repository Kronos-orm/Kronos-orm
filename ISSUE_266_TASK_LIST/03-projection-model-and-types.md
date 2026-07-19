# Task 3: Preserve Effective Projection Shape And Selected Types

Progress: 100%
Status: Done

## Goal

Make FIR projection models carry requested names, resolved unique names, selected expression types, source-minus shape, and Context shadow metadata across every query source.

## Current State

- `KronosProjectionField` distinguishes requested/resolved names and carries selected type/source metadata.
- FIR and runtime allocators reserve explicit requested names and suffix later conflicts.
- The composable `JoinSource`/`JoinedSelectQuery` boundary now propagates generated Selected types through JOIN, nested source, derived query, and union composition.
- The non-root JOIN cascade regression preserves the selected child receiver and its hidden local-key projection through runtime mapping.

## Completed

- Added effective source-minus filtering before Context merge.
- Added `isSourceAlias` metadata so derived replacement fields do not inherit source-only metadata.
- Added resolved-name allocation and shadowed Context names while keeping Source receiver boundaries unchanged.
- Hardened synthetic projection-name mangling so generic arguments, variance, nullability, requested/resolved names,
  source names, and source-vs-expression identity participate in the shape signature. Incompatible projection types
  therefore cannot share a generated FIR/IR class key merely because their raw classifier names match.
- Corrected the bundled `StringFunctions.length` projection stub from `Any?` to its actual SQL result type `Int?`;
  FIR aliases now carry `Int?` into generated Selected/Context fields, while runtime SQL remains the same and mapping
  consumes the generated field KType.

## Remaining Work

- None for the Issue 266 projection-shape and selected-type scope. Future projection-form extensions must retain this finalized allocation contract.

## Acceptance

- A projection of `it - it.username` followed by `length(...).alias("username")` has one effective `username` selected/context field and no collision requirement.
- A projection of `length(...).alias("username")` without source removal records Context shadowing but requires opt-in only when that Context property is read.
- The selected field type for `length(...).alias("username")` is `Int?` for a nullable input (or the actual
  function result type), not the source `String` type.
- Same-layer `orderBy { it.username }` resolves the selected replacement, while same-layer `where { it.username ... }` resolves the source property.
- A derived outer select sees the logical output name `username` and the selected expression type.
- Requested `id, id, id_1` resolves to `id, id_2, id_1` in Selected and Context.
- Generated projection names include effective field/type/name signatures so incompatible shapes do not share one synthetic class.
- JOIN and union models expose the same resolved names that runtime SQL/mapping use.

## Verification Record

- `KronosProjectionModel.kt`, alias refinement, Context merge, and existing projection testData: completed read-only.
- `ProjectionBoxTest` is green at 22/22; the non-root JOIN cascade case is included in the completed projection-shape evidence.
- `ProjectionBoxTest.duplicateProjectionOutputNames`: pass; FIR allocation resolves `id, id, id_1` to `id, id_2, id_1`, keeps different same-name alias types independent, exposes the resolved fields to a derived select, and keeps union output names from the first branch.
- `ProjectionNamesTest`: passed within the full 741-test core run; the runtime allocator matches the explicit-name reservation contract.
- Static review of `KronosProjectionCallRefinementExtension.renderForMangle`: pass after adding recursive generic,
  variance, nullable, requested/resolved-name, source-name, and source/expression markers. The final-worktree full
  compiler-plugin gate subsequently passed 281/281, covering the focused cache-key/type fixtures without regression.
- The `length` type chain and minimal projection fixtures are green. `functionAliasContext` and
  `selectedAliasOverrideType` require `Int?` for generated `length` aliases; after correcting a stale captured-condition
  fixture that compared this `Int?` result with a `String?` property, `ConditionBoxTest` passed 38/38 and the complete
  compiler-plugin suite passed 281/281.
- JOIN/nested source propagation is green in the compiler integration matrix; remaining IDEA artifact proof is tracked separately in Task 10.
- `ProjectionBoxTest.joinNonRootCascadeProjection`: pass; the non-root JOIN keeps the `profile` selected receiver and adds hidden `profileId` with the selected type used by cascade mapping.
