# Module Internals: Codegen, Logging, JDBC Wrapper, Build Plugins

## Table of Contents
1. [kronos-codegen](#kronos-codegen)
2. [kronos-logging](#kronos-logging)
3. [kronos-jdbc-wrapper](#kronos-jdbc-wrapper)
4. [kronos-gradle-plugin](#kronos-gradle-plugin)
5. [kronos-maven-plugin](#kronos-maven-plugin)

---

## kronos-codegen

DB schema → annotated KPojo Kotlin source files. Uses TOML config.

### Architecture
```
TOML config file
  → ConfigReader.init(path) parses TOML via Jackson
    → Builds TemplateConfig (table, output, dataSource, strategy sections)
    → Configures global Kronos settings (naming, strategies)
  → TemplateConfig.toKronosConfigs()
    → Connects to DB via KronosDataSourceWrapper
    → Reads table metadata (columns, indexes, comments)
    → Creates KronosTemplate per table
  → KronosTemplate renders Kotlin source
    → Generates data class with KPojo, annotations, imports
  → KronosConfig.write() outputs files to disk
```

### Key Classes

**ConfigReader** (`codegen/ConfigReader.kt`)
- `init(path: String)` — parses TOML, supports config extension via `extend` key
- Resolves naming strategies by name: `"lineHumpNamingStrategy"`, `"noneNamingStrategy"`
- Maps string field names to `KronosCommonStrategy` objects

**TemplateConfig** (`codegen/TemplateConfig.kt`)
- Holds `table`, `strategy`, `output`, `dataSource`
- On init: configures `Kronos` singleton (naming, strategies, data source)
- Lazy properties resolve table names, class names, fields, indexes from DB
- `template(render: KronosTemplate.() -> Unit)` — DSL entry point

**KronosTemplate** (`codegen/KronosTemplate.kt`)
- Holds `packageName`, `tableName`, `className`, `tableComment`, `fields`, `indexes`
- `Field.annotations()` — generates annotation strings (@PrimaryKey, @Necessary, @Default, @ColumnType, @CreateTime, @UpdateTime, @LogicDelete, @Version) and auto-adds imports
- `KTableIndex.toAnnotations()` — generates @TableIndex annotations
- `operator fun String?.unaryPlus()` — appends content lines to output
- `field.kotlinType` — maps `KColumnType` → Kotlin type string (INT→"Int", DATETIME→"java.time.LocalDateTime")
- `formatedComment` — word-wraps table comment to configured line width
- `indent(n)` — returns n spaces for indentation
- `imports` — `LinkedHashSet<String>` auto-managed by annotation generators

### Template DSL
The codegen uses a DSL pattern via `TemplateConfig.template {}`:
```kotlin
init("config.toml")
val files = template {
    // `this` is KronosTemplate
    +"""package $packageName"""
    +imports.joinToString("\n") { "import $it" }
    +formatedComment
    +indexes.toAnnotations()
    +"""@Table("$tableName")"""
    +"""data class $className("""
    fields.forEachIndexed { i, field ->
        val annotations = field.annotations().joinToString(" ")
        +"""    $annotations var ${field.name}: ${field.kotlinType}? = null${if (i < fields.size - 1) "," else ""}"""
    }
    +""") : KPojo"""
}
files.write()
```

### Auto-Generated Annotations
Based on field metadata and global strategy config:
- `@PrimaryKey(identity = true)` — field matches primary key strategy
- `@Necessary` — non-nullable, non-PK fields
- `@Default("value")` — fields with default values
- `@ColumnType(type, length, scale)` — non-standard column types
- `@CreateTime` / `@UpdateTime` / `@LogicDelete` / `@Version` — field matches global strategy

**DataSourceHelper** (`codegen/DataSourceHelper.kt`)
- `createWrapper(className, dataSource)` — reflectively instantiates KronosDataSourceWrapper
- `initialDataSource(config)` — reflectively creates DataSource, sets properties via setters

**Extensions** (`codegen/Extensions.kt`)
- `Field.kotlinType` — maps `KColumnType` → Kotlin type string

### TOML Config Format
```toml
[dataSource]
className = "org.apache.commons.dbcp2.BasicDataSource"
wrapperClassName = "com.kotlinorm.wrappers.KronosJdbcWrapper"
driverClassName = "com.mysql.cj.jdbc.Driver"
url = "jdbc:mysql://localhost:3306/mydb"
username = "root"
password = "pass"

[output]
targetDir = "src/main/kotlin"
packageName = "com.example.entity"
tableCommentLineWords = 40

[[table]]
name = "user"
className = "User"

[[table]]
name = "order"

[strategy]
tableNamingStrategy = "lineHumpNamingStrategy"
fieldNamingStrategy = "lineHumpNamingStrategy"
createTimeStrategy = "createTime"
updateTimeStrategy = "updateTime"
```

### Dependencies
- `implementation`: kronos-core, jackson-dataformat-toml
- `testImplementation`: kronos-jdbc-wrapper, dbcp2, mysql-driver

---

## kronos-logging

Pluggable logging with auto-detection. Zero hard dependencies on logging frameworks.

### Detection Order
```
KronosLoggerApp.detectLoggerImplementation()
  → Try Android Log (reflection)     → AndroidUtilLoggerAdapter
  → Try Apache Commons (reflection)  → ApacheCommonsLoggerAdapter
  → Try JDK java.util.logging        → JavaUtilLoggerAdapter
  → Try SLF4J (reflection)           → Slf4jLoggerAdapter
  → Fallback                         → BundledSimpleLoggerAdapter (from kronos-core)
```

All adapters use reflection to avoid compile-time dependencies on logging frameworks.

### KLogger Interface (defined in kronos-core)
```kotlin
interface KLogger {
    fun info(msg: Logging)
    fun debug(msg: Logging)
    fun warn(msg: Logging)
    fun error(msg: Logging, throwable: Throwable? = null)
    fun trace(msg: Logging)
}
```

### Logging DSL
```kotlin
typealias Logging = KLogMessage.() -> Unit
logger.info(log { +"Starting server"[green] })
```

### Dependencies
- `compileOnly`: kronos-core
- `implementation`: kotlin-reflect

---

## kronos-jdbc-wrapper

Default JDBC `KronosDataSourceWrapper` implementation.

### KronosJdbcWrapper
```kotlin
class KronosJdbcWrapper(
    val dataSource: DataSource,
    databaseType: DBType? = null,
    configure: KronosJdbcConfig.() -> Unit = {}
) : KronosDataSourceWrapper
```

### Connection Management
```kotlin
private fun obtainConnection(): Pair<Connection, Boolean> {
    val txConn = transactionConnection.get()
    return if (txConn != null) txConn to false   // reuse, don't close
    else dataSource.connection to true             // new, caller closes
}
```

### Query Methods
- `toList(task)` → `List<Any?>` — maps ResultSet rows to maps, scalars, or KPojo values
- `first(task)` → `Any?` — returns the first decoded row
- KPojo construction uses exact generated metadata through `Kronos.createKPojo(type)` and can be overridden through `Kronos.registerKPojoFactory(...)`

### Action Methods
- `update(task)` → `Int` — returns affected rows
- `batchUpdate(task)` → `IntArray` — batch with `addBatch()`/`executeBatch()`
- Generated identity ID: when the action task has a `GeneratedKeyRequest`, retrieves `RETURN_GENERATED_KEYS`

### Transaction
- ThreadLocal connection propagation
- Nested: inner `transact` reuses outer connection (join semantics)
- Outer: autoCommit=false, isolation, commit/rollback, restore+close in finally
- Savepoints via `TransactionScope`

### Type Conversion
`getTypeSafeValue()` handles: Timestamp→LocalDateTime, CLOB→String, BLOB→ByteArray, Number→Int/Long/Double, etc.

### Dependencies
- `compileOnly`: kronos-core only (no external deps beyond JDK javax.sql)

---

## kronos-gradle-plugin

Wires the compiler plugin into Gradle Kotlin compilation and attaches generated-provider service resources for JVM source sets and Android application/library variants. Kotlin Toolchain builds and publishes this JVM library.

### KronosGradlePlugin
```kotlin
class KronosGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun getCompilerPluginId() = "kronos-compiler-plugin"
    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = "com.kotlinorm",
        artifactId = "kronos-compiler-plugin",
        version = version
    )
    override fun applyToCompilation(compilation) = provider { generatedProviderOptions(...) }
    override fun isApplicable(compilation) = true
}
```

Registered as `com.kotlinorm.kronos-gradle-plugin` by the descriptor under `src/main/resources/META-INF/gradle-plugins/`.

`GenerateKronosGeneratedTypeService` writes the provider descriptor after compilation only when the generated provider class exists. Android variants add the output to merged Java resources and merge duplicate provider descriptors. `kronos-core` packages a consumer R8 rule that preserves generated provider classes in minified Android artifacts.

### Version Sync
The catalog's `kronos-compiler-plugin` version, common Toolchain publication template and compiler version properties agree. `scripts/version.main.kts` updates all three. `KronosGradlePlugin` reads the packaged resource at runtime.

### Build
- Model: `kronos-gradle-plugin/module.yaml`, `product: jvm/lib`.
- Build resources: checked-in Gradle descriptor and compiler version properties under `src/main/resources/`.
- Gradle API is compile-only; Kotlin Gradle plugin dependencies retain Gradle's variant selection in published consumers.
- `scripts/run build test -m kronos-gradle-plugin -p jvm` runs the provider and Gradle API unit tests. Android, Spring Boot, Ktor and Vert.x examples verify Gradle consumption of staged publications; Solon and Spring Boot Maven examples verify Maven integration.

---

## kronos-maven-plugin

Maven integration through `KotlinMavenPluginExtension`, registered as the Plexus component
`kronos-maven-plugin` in `META-INF/plexus/components.xml`.

- The published POM exports `kronos-compiler-plugin`; Kotlin Maven and Maven Core APIs are runtime dependencies.
- Static `META-INF/services` descriptors expose `KronosCompilerPluginRegistrar` and `KronosCommandLineProcessor` from the compiler dependency.
- `getPluginOptions` derives `maven:<groupId>:<artifactId>:<executionId>` (falling back to the goal for an empty execution ID), hashes it, and supplies `generated-provider-id` and `generated-provider-fq-name` to `kronos-compiler-plugin`.
- The extension writes the matching `GeneratedTypeProvider` service entry into the main or test compilation output; source-free compilations remove a stale entry. Explicit compile and test executions use separate execution IDs.
- Kotlin Maven configuration enables `-Xcollection-literals` for collection-literal DSL expressions and `-Xcontext-sensitive-resolution` for contextual enum references. `kronos-maven-plugin/README.md` contains the complete plugin configuration.
