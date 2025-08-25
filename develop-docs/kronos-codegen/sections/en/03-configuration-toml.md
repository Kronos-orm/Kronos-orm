# 3. Configuration (TOML) — Expanded

This chapter covers the full config schema, validation rules, and the merge algorithm with `extend`.

## Schema

- `[[table]]` (required, 1..N)
  - `name: String` (required) — DB table name.
  - `className: String?` (optional) — Desired Kotlin class name; defaults to name transformed by `tableNamingStrategy` with first char uppercased.
- `[strategy]` (optional)
  - `tableNamingStrategy: String?` — `lineHumpNamingStrategy` | `noneNamingStrategy`.
  - `fieldNamingStrategy: String?` — same options as above.
  - `createTimeStrategy: String?` — column name for `@CreateTime`.
  - `updateTimeStrategy: String?` — column name for `@UpdateTime`.
  - `logicDeleteStrategy: String?` — column name for `@LogicDelete`.
  - `optimisticLockStrategy: String?` — column name for `@Version`.
  - `primaryKeyStrategy: String?` — column name for `@PrimaryKey`; if matches, treated as identity by default in KronosTemplate.
- `[output]` (required)
  - `targetDir: String` (required) — Absolute/prepared path to output `.kt` files.
  - `packageName: String?` (optional) — If absent, inferred from `targetDir` segment after `main/kotlin/`.
  - `tableCommentLineWords: Int?` (optional) — Wrap width for table comments; default `MAX_COMMENT_LINE_WORDS`.
- `[dataSource]` (required)
  - `dataSourceClassName: String?` — Defaults to `org.apache.commons.dbcp2.BasicDataSource`.
  - `wrapperClassName: String?` — Defaults to `com.kotlinorm.KronosBasicWrapper`.
  - Arbitrary additional properties mapped to setters via reflection: `url`, `username`, `password`, `driverClassName`, pool configs, etc.

## Validation

- `table` must be a list; each entry must contain `name`.
- `output.targetDir` must be non-empty.
- `dataSource` must exist; missing keys are tolerated if the default DataSource accepts them omitted.
- Strategy keys when provided must be strings; naming strategies limited to known literal names.

## Extend Merge Algorithm

Pseudocode for `readConfig(path)`:

```
config = parseToml(path)
while ("extend" in config):
  base = parseToml(config["extend"])  // throws if not found/empty
  log("Config extension found: $extendPath")
  config = base + config  // right-bias merge (child overrides base)
return config
```

- Multi-level extends supported.
- Cycles are not explicitly detected; avoid circular `extend`.

## Examples

- Base shared DB config + project-specific overrides via `extend`.
- Different build profiles (dev/ci/prod) by layering multiple files.
