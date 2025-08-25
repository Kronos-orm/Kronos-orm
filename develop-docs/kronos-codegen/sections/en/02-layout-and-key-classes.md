# 2. Layout and Key Classes (Expanded)

This chapter describes each core file in kronos-codegen with design rationale and algorithmic details.

Source: kronos-codegen/src/main/kotlin/com/kotlinorm/codegen

## ConfigReader.kt

- Global state:
  - `var mapper = ObjectMapper(TomlFactory())`
  - `var codeGenConfig: TemplateConfig? = null`
- `fun init(path: String)`
  - Algorithm:
    1. `val config = readConfig(path)`; this may recursively merge via `extend`.
    2. Extract tables (`List<Map<String,String?>>`) with strong validation; throw clear IllegalArgumentException on mismatch.
    3. Extract `output`, `dataSource`; optional `strategy` map.
    4. Build `TemplateConfig` with:
       - Tables mapped to `TableConfig(name, className)`.
       - `StrategyConfig` mapping strings to Kronos strategies:
         - `tableNamingStrategy`, `fieldNamingStrategy`: accept values `lineHumpNamingStrategy` and `noneNamingStrategy`.
         - `create|update|logicDelete|optimisticLock|primaryKey` => `KronosCommonStrategy(true, Field(name))`.
       - `OutputConfig(targetDir, packageName, tableCommentLineWords)`.
       - `dataSource = createWrapper(wrapperClassName, initialDataSource(dataSourceConf))`.
    5. Side effect: log success via `Kronos.defaultLogger(this).info(log { "Reading config file successfully: $path"[green] })`.
    6. Assign to global `codeGenConfig`.
  - Design notes:
    - Fail fast on bad config with actionable error messages.
    - Keep mapping from string to strategy minimal and explicit.

- `fun readConfig(path: String): Map<String, Any?>`
  - Algorithm:
    1. Parse TOML into `Map<String, Any?>`.
    2. While `config["extend"] != null`:
       - Read `extendPath`, log discovery.
       - Parse extendConfig.
       - Merge: `config = extendConfig + config` so child overrides base.
    3. Return final merged map.
  - Guarantees: Multi-level extension supported; last one wins.

## TemplateConfig.kt

- Holds generation context and binds Kronos global strategies.
- Key fields:
  - `table: List<TableConfig>` and derived lazy views:
    - `tableNames`, `classNames`, `tableComments`, `fields`, `indexes`.
  - `output: OutputConfig` mapped to `targetDir`, `packageName`, `tableCommentLineWords`.
  - `dataSource: KronosDataSourceWrapper` exposed as `wrapper`.
- init block:
  - `Kronos.init { ... }` sets table/field naming strategies and common strategies if provided, otherwise defaults.
  - `Kronos.dataSource = { wrapper }` exposes the wrapper as the global data source.
  - Package inference: if `output.packageName == null`, infer from `targetDir` segment after `main/kotlin/`, else fallback `com.kotlinorm.orm.table`.
- Lazy loads:
  - `tableComments`: `queryTableComment(name, wrapper)`.
  - `fields`: `SqlManager.getTableColumns(wrapper, name)`.
  - `indexes`: `SqlManager.getTableIndexes(wrapper, name)`.
- template API:
  - `companion object fun template(render: KronosTemplate.() -> Unit): List<KronosConfig>`
    - Validates `codeGenConfig` initialized.
    - Builds `KronosTemplate` per table, calls `render()`, and packs into `KronosConfig(content, "<targetDir>/<className>.kt")`.

## KronosTemplate.kt

- Purpose: Provide a friendly rendering context and helper algorithms.
- Public fields: `packageName`, `tableName`, `className`, `tableComment`, `fields`, `indexes`, `imports`, `tableCommentLineWords`.
- Content building:
  - `var content = ""`
  - `operator fun String?.unaryPlus()` appends a line + newline.
  - `fun indent(num: Int) = " ".repeat(num)`.
- Import management:
  - `imports` is a LinkedHashSet initialized with `Table` and `KPojo`.
  - All helper functions add required imports automatically.
- Algorithms:
  - `Field.annotations(): List<String>`
    - Determine PK: if global `primaryKeyStrategy.field.columnName == columnName`, set `primaryKey = IDENTITY`.
    - Emit `@PrimaryKey` (identity or default) when applicable; add imports.
    - If non-nullable and not a PK -> `@Necessary`.
    - If has defaultValue -> `@Default("value")`.
    - Determine if explicit `@ColumnType` needed:
      - If not a simple string/bool/number with trivial length/scale; compute params (length/scale) and emit.
    - Time/delete/version annotations based on Kronos global strategies (compare field.columnName to those strategy fields).
  - `KTableIndex.toAnnotations()` and `List<KTableIndex>.toAnnotations()`
    - Assemble `@TableIndex` with named parameters (`name`, `columns`, `type`, `method`, `concurrently`).
  - `val formatedComment` (lazy):
    - Split tableComment by space, wrap lines to `tableCommentLineWords`, prefix each with `// `.

## KronosConfig.kt

- Data holder for generated file content and target path.
- `List<KronosConfig>.write()` algorithm:
  - For each item:
    1. Ensure `parentFile.mkdirs()`.
    2. If file does not exist -> `createNewFile()`.
    3. `writeText(str)`.
    4. Log success via Kronos logger in green.

## DataSourceHelper.kt

- `initialDataSource(config: Map<String, Any?>): DataSource`
  - Algorithm:
    - Resolve `dataSourceClassName` (default: `org.apache.commons.dbcp2.BasicDataSource`).
    - Instantiate with zero-arg constructor.
    - For each entry in config except `dataSourceClassName` and `wrapperClassName`:
      - Generate setter names variants: `setXxx`, `setXXX`, `setxxx`.
      - Find a compatible method by name and 1 parameter where parameter type matches the value (via `isTypeCompatible`).
      - Convert the value to parameter type (`convertValue`) supporting numbers, booleans, strings, enums.
      - Invoke setter; warn if not found or conversion fails, but continue.
  - Design: robust to naming differences and minor type mismatches; prefer warnings over hard failures.
- `createWrapper(className: String?, dataSource: DataSource): KronosDataSourceWrapper`
  - Algorithm:
    - If className null -> warn and default to `com.kotlinorm.KronosBasicWrapper`.
    - Try constructor that accepts `dataSource::class.java` (may be concrete class), else fallback to `javax.sql.DataSource`.
    - On failure, throw RuntimeException with cause.

## Extensions.kt

- `const val MAX_COMMENT_LINE_WORDS = 80` default wrapping width.
- `val Field.kotlinType: String`
  - Algorithm: map KColumnType to Kotlin types:
    - Numeric: BIT->Boolean, TINYINT->Byte, SMALLINT->Short, INT/SERIAL/YEAR/MEDIUMINT->Int, BIGINT->Long, REAL/FLOAT->Float, DOUBLE->Double, DECIMAL/NUMERIC->BigDecimal.
    - Textual: various -> String.
    - Binary: various -> ByteArray.
    - Date/Time: DATE->LocalDate, TIME->LocalTime, DATETIME->LocalDateTime, TIMESTAMP->Instant.
    - Special: CHAR->Char, UUID->UUID; default String.
  - Philosophy: maximize portability and avoid driver-specific types in the model.
