# 8. Strategies and Annotations (Expanded)

This chapter explains how strategies influence annotations, with implementation-level notes.

## Naming strategies

- `lineHumpNamingStrategy` — snake_case to camelCase conversion.
- `noneNamingStrategy` — preserve original names.
- Wiring: `TemplateConfig` sets `Kronos.tableNamingStrategy` and `Kronos.fieldNamingStrategy` (defaults to lineHump).

## Time/Delete/Version/PK strategies

- Defined in `[strategy]` using column names.
- `TemplateConfig` stores them in Kronos global strategies via `KronosCommonStrategy`.
- `KronosTemplate.Field.annotations()` compares `field.columnName` to each strategy’s `field.columnName`:
  - Equal -> emit the corresponding annotation (`@CreateTime`, `@UpdateTime`, `@LogicDelete`, `@Version`).
- Primary key
  - If `Kronos.primaryKeyStrategy.field.columnName == columnName` -> treat as `PrimaryKeyType.IDENTITY`.
  - Else rely on `Field.primaryKey` provided by metadata.

## ColumnType emission heuristic

- Emit `@ColumnType(type = KColumnType.X, length = L, scale = S)` when we cannot rely on defaults:
  - If type is not the common string/bool number case with trivial length/scale.
  - Only include length/scale when non-zero.
- Rationale: Keep model readable while still preserving fidelity for non-trivial columns.

## Index annotations

- `List<KTableIndex>.toAnnotations()` returns joined `@TableIndex(...)` lines or null.
- Parameters are included only when present: name, columns, type, method, concurrently.
- Import `TableIndex` is added only when list non-empty.
