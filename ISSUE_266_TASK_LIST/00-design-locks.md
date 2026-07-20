# Design Locks

Updated: 2026-07-19

## Terms And Boundaries

- `Source` means the KPojo receiver used by the current query's `where`, `groupBy`, and `having` clauses.
- `Selected` means the generated projection result fields emitted by the current `select`.
- `Context` means the same-layer receiver used by `orderBy`; it exposes source fields plus selected output fields after the effective projection shape is applied.
- A `requested output name` is the logical field name before collision allocation.
- A `resolved output name` is the final SQL label, Selected property, Context property, and Map key after collision allocation.
- An `effective source field set` is calculated after source-minus operations. A field removed by `it - ...` is not considered present for the replacement check.
- `FromSource` is one SQL table reference: a table, derived query, or recursively nested JOIN tree.
- `JoinSource` is a `FromSource` whose root is a JOIN expression and which is not executable until `select` returns a query.
- `JoinedSelectQuery` is the single-query result returned by JOIN `select`; it is a `KSelectable<Selected>` and can be executed or used as a derived source.
- `OffsetPageQuery`, `TotalPageQuery`, and `CursorPageQuery` are distinct public query stages with non-overlapping capabilities.

## Current Facts

- `UnsafeProjectionOverride` is an error-level Kotlin opt-in marker and its current message covers deterministic suffixing and selected-field Context replacement.
- FIR and runtime allocators for deterministic projection names exist in the working tree, including global reservation of explicitly requested names.
- The current `SelectFromPlanner` folds a root table plus a flat `joinables` list; it cannot preserve `A JOIN (B JOIN C)` grouping as a first-class source tree.
- The current outer JOIN lambda returns `Unit`, so refining the inner `select` cannot expose a generated property such as `id_1` on the outer call result.
- `SelectClause` uses `Context` for `orderBy` and `Source` for `where`, `groupBy`, and `having`.
- Local `0.2.4` and `0.2.5-SNAPSHOT` signed IDEA ZIPs contain `<idea-version since-build="262"/>`; the supplied screenshot still shows the published plugin incompatible with IDEA 2026.2.

## Projection Semantics

- Repeated requested Selected names require `@OptIn(UnsafeProjectionOverride::class)`; without opt-in compilation fails at the conflicting projection item.
- After opt-in, all Selected fields are preserved. The first occurrence keeps its requested name and later occurrences receive `_1`, `_2`, and so on.
- Explicit requested names are globally reserved before suffix allocation. For requested `id, id, id_1`, the resolved names are `id, id_2, id_1`.
- Explicit aliases are the recommended remediation and diagnostic/IDE guidance must say so.
- Projection allocation applies equally to direct fields, expanded KPojo fields, aliases, functions, aggregates, scalar subqueries, raw SQL expressions, window expressions, and JOIN fields.
- SQL labels, generated Selected properties, Context properties, `toMap` keys, typed mapping, nested-query sources, union output, and IDEA completion/documentation use the same resolved names and order.
- Union output names come from the first selectable branch; branch compatibility remains positional. Each branch resolves its own projection names before union validation/materialization.
- A selected field that shadows a still-present Source name does not require opt-in merely because it was selected. Opt-in is required when a same-layer Context clause actually reads the shadowed name, for example `orderBy { it.id }`.
- After that opt-in, the Context name resolves to the Selected value/type. `where`, `groupBy`, and `having` continue to resolve Source and are unaffected.
- Source-minus replacement remains intentional: remove the Source field first and restore the name with an alias without requiring shadow opt-in.
- Opt-in acceptance uses Kotlin's standard mechanism, including expression, function, class, file, and compiler-wide opt-in where supported by the target Kotlin compiler. Do not implement custom parent-scope traversal.
- The selected expression's resolved Kotlin type is authoritative for generated Selected and Context properties, IR materialization, mapper target type, and derived-query input. A source property with the same name must not override that type.

## JOIN Semantics

- JOIN is a recursive `FromSource`, not a query-only operation and not a flat list of tables.
- The public DSL keeps source parameters once in the outer JOIN lambda. Relation conditions and query clauses capture those parameters instead of repeating `(a, b, c)` on every layer.
- Relation methods are chained and explicit: `innerJoin`, `leftJoin`, `rightJoin`, `fullJoin`, and conditionless `crossJoin`. Do not retain an `on()` method with an implicit LEFT JOIN default.
- The outer JOIN block returns its final expression. Relation methods return `JoinSource`; `select` changes the type to `JoinedSelectQuery`; query methods preserve that query type.
- No `query`, custom `run`, custom `apply`, or other wrapper API is added. Standard Kotlin scope functions remain ordinary Kotlin but are not required by the documented DSL.
- A JOIN block ending in a relation method returns a raw `JoinSource`, allowing `A JOIN (B JOIN C)`. A JOIN block ending in `select { ... }.where { ... }` returns an executable/queryable `JoinedSelectQuery`.
- The old statement-style JOIN body is not source-compatible. `JoinSource` exposes relation methods and one first-layer `select`; `where`, `orderBy`, pagination, execution, and other query APIs live only on the selected query type.
- A selected JOIN query implements `KSelectable<Selected>`, so calling `select` again creates a derived query layer and union can consume it.

## Pagination Semantics

- Plain query execution remains `toList()` and existing result-method naming is retained.
- `query.page(pageIndex, pageSize)` returns `OffsetPageQuery : KSelectable<Selected>`; its `toList()` returns `List<Selected>`, and the paged SQL can become a derived source through normal `KSelectable` composition.
- `query.page(...).withTotal()` returns `TotalPageQuery`; its `toList()` returns a named `PageResult<Selected>` and it cannot call `cursor`, `page`, or another `withTotal`.
- `query.cursor(pageSize, after = null)` returns `CursorPageQuery`; its `toList()` returns a named `CursorResult<Selected>` and it cannot call `page` or `withTotal`.
- Pagination specs are carried by helper types and applied when building tasks; creating a page or cursor view must not mutate the reusable base query.
- `OffsetPageQuery` is a single relational `SELECT ... LIMIT/OFFSET`, so `page(...).select { ... }`, JOIN, union, insert-select, scalar/subquery consumption, and an independently paged/cursored outer layer are valid. It does not expose another same-stage `page` or `cursor` transition itself.
- `TotalPageQuery` is a composite count-plus-record operation and is not a `KSelectable`. `CursorPageQuery` is also not a `KSelectable`, because terminal cursor execution fetches `pageSize + 1` and may add hidden token columns; neither implementation detail may leak into a derived source.
- `PageResult` exposes total, records, totalPages, pageIndex, and pageSize. `CursorResult` exposes hasNext, nextCursor, and records.
- `TotalPageQuery.build()` returns a named task holder rather than an unlabelled `Pair`.

## IDEA 2026.2 Compatibility

- Stable IntelliJ IDEA 2026.2 is the build and verification target; EAP coordinates are not used.
- The patched `plugin.xml` inside the final signed ZIP explicitly covers IDE build `262` (recommended bounded range: `since-build="262"`, `until-build="262.*"`).
- CI and manual release checks must inspect the final signed ZIP, not only Gradle source configuration or an unsigned intermediate artifact.
- Marketplace verification must confirm the uploaded version advertises IDEA 2026.2 compatibility and can be installed from Marketplace in the installed 2026.2 IDE.

## Locked Examples

Duplicate JOIN projection names are preserved only after opt-in:

```kotlin
@OptIn(UnsafeProjectionOverride::class)
val query = User().join(Company()) { user, company ->
    leftJoin {
        user.companyId == company.id
    }.select {
        [user.id, company.id]
    }.where {
        user.enabled == true
    }
}

val rows = query.toList()
val userId = rows.first().id
val companyId = rows.first().id_1
```

A nested JOIN remains a raw source until its outer query calls `select`:

```kotlin
val companyRegion = Company().join(Region()) { company, region ->
    innerJoin {
        company.regionId == region.id
    }
}

val query = User().join(companyRegion) { user, company, region ->
    leftJoin {
        user.companyId == company.id
    }.select {
        [user.id, company.name, region.name.alias("regionName")]
    }
}
```

Pagination and derived-query transitions remain separate:

```kotlin
val records = query.page(pageIndex = 1, pageSize = 20).toList()
val page = query.page(pageIndex = 1, pageSize = 20).withTotal().toList()
val cursorPage = query.cursor(pageSize = 20, after = null).toList()

val outer = query
    .page(pageIndex = 1, pageSize = 20)
    .select { [it.id, it.regionName] }
    .where { it.regionName != null }
```

## Do Not Add

- Do not make all `.alias(...)` calls experimental; non-colliding aliases remain ordinary API.
- Do not add a custom suppression annotation or manual declaration-parent traversal when Kotlin opt-in machinery can report acceptance.
- Do not silently permit collisions by deleting the diagnostic.
- Do not use `Any?` as a type-resolution escape hatch for a known selected expression.
- Do not change the Source receiver rules or make selected aliases available to same-layer `where`, `groupBy`, or `having`.
- Do not change unrelated duplicate-field, missing-alias, scalar-subquery, or insert-select diagnostics.
- Do not flatten nested JOIN trees during AST planning.
- Do not preserve the old statement-style JOIN body with deprecated forwarding APIs.
- Do not implement pagination exclusivity as runtime Boolean flags when public helper types can make invalid chains unrepresentable.
- Do not treat a local ZIP's compatibility metadata as proof of what Marketplace actually serves.
