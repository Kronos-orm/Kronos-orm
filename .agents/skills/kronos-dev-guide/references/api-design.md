# Kronos Syntax and API Design Experience

Use this reference before designing or changing any user-facing Kronos DSL, public API, examples, or syntax specification. The goal is to keep Kronos APIs compact, Kotlin-native, and consistent with the existing ORM style.

## Design Order

When designing a new DSL capability:

1. Start from the user syntax, not implementation classes.
2. Enumerate SQL/user scenarios one by one.
3. Confirm each scenario with examples before generalizing.
4. Only after syntax is stable, discuss runtime classes, AST nodes, compiler transformations, and migration work.
5. Remove unresolved or intentionally unsupported scenarios from the active spec instead of leaving them as "TODO" sections.

Do not mix syntax drafts with implementation plans unless the document explicitly says it is an implementation plan.

## Kronos DSL Preferences

Prefer extending existing concepts over introducing new named APIs.

- Prefer `select { ... }`, `where { ... }`, `having { ... }`, `orderBy { ... }`, `groupBy { ... }`, `join(...)`, `insert<Target> { ... }`, and existing DDL entry points.
- Avoid adding visible helper APIs such as `asTable`, `createTableAs`, `setSubquery`, `inSubquery`, or manual alias declarations unless there is no natural Kotlin syntax.
- Keep internal table aliases automatic. Users should not manage derived table names for normal ORM queries.
- Use `KSelectable` as the conceptual boundary for query-like sources, while keeping user-facing syntax concrete and natural.
- When a query result can come from ordinary select, join select, union, or derived query, design the consumer around `KSelectable`.

## Kotlin-Native Syntax

Use Kotlin syntax when it clearly maps to SQL:

- `field in query` for `IN (SELECT ...)`.
- `field !in query` for `NOT IN (SELECT ...)`.
- `exists(query)` for `EXISTS`.
- `!exists(query)` for `NOT EXISTS`; do not add a separate `notExists(...)` user-facing API.
- `[a, b] in query` for row-value tuple `IN`.
- Do not allow single-element tuple `IN`; use `field in query` for single-column `IN`.
- Single-item selectors may omit `[]`: `select { it.name }`, `orderBy { it.name.asc() }`.
- Multiple-item selectors should use `[]`: `select { [it.id, it.name] }`, `orderBy { [it.name.asc(), it.id.desc()] }`.
- Keep `[]` as the unified user-side list syntax. The compiler should interpret it by context as a projection list, sort list, window field list, row-value tuple, and similar internal structures.
- `exists` queries should still enter through `select()`: use `exists(Order().select().where { ... })`, not `Order().where { ... }`.

Do not use `+` as a field-list operator. `+` is reserved for real arithmetic or string concatenation semantics.

## Projection Rules

The source lambda receiver and the query result projection are different concepts.

- In `User().select { ... }`, `it` is the complete source KPojo by default.
- Prefer omitting explicit lambda parameters when the lambda does not need to be referenced from a nested lambda.
- Keep explicit names only when they improve clarity or are required for correlated subqueries, joins, or multiple simultaneously visible receivers.
- The result projection should be generated from selected fields by the compiler plugin when possible.
- Users should not write `select<User, UserView>` in the final syntax.
- `.as_("name")` names a selected result field and should become a property on the generated projection type.
- After a projected `select { ... }`, `where { ... }`, `orderBy { ... }`, and `having { ... }` should operate on a compiler-generated context class.
- The generated context class contains every column from the original source DTO plus every newly selected projection field such as `lastOrderAmount` or `rn`.
- Source-field filters should not require projecting the filtered field because the generated context already contains all source columns.
- Filtering selected aliases or window outputs should use ordinary `where { it.alias }`; Kronos decides whether SQL needs an outer derived query layer.
- `having` may operate on aggregation/projection outputs when the SQL semantics require it.

Avoid designs where `select` lambda `it` becomes the projection type; that loses access to the full source DTO and breaks subquery/join composition.

## Query Chain Shape

For select-like queries, keep the chain in user reading order:

```kotlin
KPojo()
    .select { ... }
    .where { ... }
    .groupBy { ... }
    .having { ... }
    .orderBy { ... }
    .limit(...)
```

Do not place `where` or `orderBy` before `select` in syntax examples unless the existing API being discussed already works that way and the distinction is intentional.

## Scalar Subquery Rules

A scalar subquery must be single-column and single-row.

- Aggregate queries without `groupBy` may be accepted without `limit(1)`.
- Other non-aggregate scalar subqueries must use `limit(1)` in examples and tests.
- Unique-key proof can be added later, but should not be the first-version main rule.
- Use `limit(1) as T` as the optional type hint. Do not introduce `.scalar<T>()` for the user-facing syntax.
- The cast is only a compiler-recognized type hint; it must not change SQL or bypass single-column/single-row validation.

## Alias and Generated Names

Avoid user-managed alias tokens for ordinary query flow.

- Do not require `val rn = alias<Int>("rn")` just so later clauses can refer to a selected value.
- Prefer `.as_("rn")` on the selected expression and then `it.rn` in later clauses.
- If the same alias appears twice, the design must define how ambiguity is prevented or reported.
- Derived table aliases should be generated internally unless the SQL feature genuinely exposes a user-visible name as part of semantics.

## Expression Extension Mechanisms

When adding function modifiers, prefer a reusable expression mechanism instead of special-casing one SQL function.

Examples:

- `f.rowNumber().over(...)`
- future `f.count(...).filter(...)`
- future aggregate ordering or `withinGroup(...)`

The design should let other functions use the same modifier capability rather than adding one-off parser rules for `rowNumber`.

## Insert-Select Mapping

For `KSelectable.insert<Target> { [...] }`, keep the existing ordered mapping model.

- Do not rename it to `insertInto` just to express source-to-target mapping.
- Map values by the target KPojo insertable-field order, not by source alias or field name.
- The value count must exactly match the target insertable-field count.
- Each value type must be compatible with the target field in the same position.
- Ignored fields, non-database fields, and fields excluded by Kronos insert rules do not enter the target sequence.
- Strategy fields and default-value fields enter or leave the sequence according to Kronos' existing insertable-field rules.
- If a field is in the sequence, the user must provide a value for it; `null` is allowed when the target field permits it.

## Scenario Coverage

For SQL feature specs, organize the document by scenario blocks. Each block should include:

- target SQL use case;
- recommended Kotlin syntax;
- compact expected SQL when helpful;
- current confirmations and exclusions.

For subquery design, cover only agreed scenarios. Do not leave a numbered scenario for a feature that is not being designed. If CTE is out of scope, delete the CTE section entirely instead of writing a placeholder.

## Documentation Discipline

Examples in README, docs, skills, and testData should follow the same syntax conventions.

- Update docs when old syntax is intentionally removed.
- Scan for stale examples such as old `+` field-list syntax or old explicit projection DTO syntax.
- Keep English and Chinese docs aligned when changing public DSL behavior.
- Prefer examples that can become tests; avoid syntax that cannot compile yet unless clearly marked as future design.
