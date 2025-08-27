# 5. Developer API (Expanded)

This chapter dives deeper into the API surface exposed to developers.

## Entry points

- `init(path: String)` — Initialize from TOML; sets `codeGenConfig` and Kronos strategies.
- `TemplateConfig.template(render: KronosTemplate.() -> Unit): List<KronosConfig>` — Build a per-table rendering context.
- `List<KronosConfig>.write()` — Persist files.

## Template context (KronosTemplate)

- `packageName: String` — Computed from OutputConfig.
- `tableName: String`, `className: String` — From table config and naming strategy.
- `tableComment: String` — From DB comment via queryTableComment.
- `fields: List<Field>` — From SqlManager.getTableColumns; contains type/length/scale/nullability/default/primaryKey metadata.
- `indexes: List<KTableIndex>` — From SqlManager.getTableIndexes.
- `imports: MutableSet<String>` — Starts with `Table` and `KPojo` and expands as helpers are invoked.
- `formatedComment: String` — Wrapped tableComment with `// ` prefix.
- `indent(num: Int)` — Spaces helper.
- `operator fun String?.unaryPlus()` — Append a line to content.

## Helper algorithms

- `Field.annotations(): List<String>` — returns a list of annotation lines:
  - Primary key annotations decided by Field.primaryKey with override when matches global PK strategy.
  - Necessary when `!nullable && primaryKey == NOT`.
  - Default when `defaultValue != null`.
  - ColumnType when type requires explicit declaration (see logic in source).
  - Time/Delete/Version based on Kronos strategies.
- `List<KTableIndex>.toAnnotations(): String?` — returns null when empty; else lines with `@TableIndex(...)`.
- `Field.kotlinType: String` — DB type to Kotlin type mapping, see Extensions.kt.

## Patterns and best practices

- Always render imports using `imports.joinToString` so the set contains only what’s needed.
- Don’t assume strategies exist; always let helper methods decide which annotations to emit.
- Keep line formatting consistent using `indent()`.
