# Design Locks

Updated: 2026-07-12

## Terms And Boundaries

- `match-field upsert` means `upsert().on { ... }` without `onConflict()`, implemented as match query plus update or insert.
- `native conflict upsert` means `upsert().onConflict()`, rendered as dialect-native conflict SQL.
- `conflict target` means the columns used by PostgreSQL/SQLite `ON CONFLICT (...)` and SQL Server/Oracle `MERGE ... ON (...)`.

## Current Facts

- `UpsertClause.toSqlUpsertStatement()` currently reads only `onFields`, so `onConflict()` without explicit `on {}` can render an empty target.
- Existing and newly added positive tests use complete `assertEquals` checks for SQL, parameters, rows, and transaction probes.
- `KronosActionTask.execute()` currently calls `beforeExecute` before `transact` and `afterExecute` after `transact`.

## Implementation Principles

- Do not add compatibility layers or raw-string SQL fallback for these fixes.
- Keep SQL generation AST-based and parameterized.
- Do not make tests accept `ON CONFLICT ()`, `MERGE ... ON ()`, or duplicate-suffixed parameters like `deletedNewNew`.
- Hooks that prepare or follow action tasks must share one transaction with the grouped action tasks.

## Do Not Add

- Do not query live database metadata to infer conflict targets.
- Do not silently choose an empty conflict target.
- Do not weaken positive test assertions to `contains`.
- Do not move unrelated dirty work out of the branch or revert it.
