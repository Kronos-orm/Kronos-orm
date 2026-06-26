# Module Internals: Codegen, Logging, JDBC Wrapper, Build Plugins

## Table of Contents
1. [kronos-codegen](#kronos-codegen)
2. [kronos-logging](#kronos-logging)
3. [kronos-jdbc-wrapper](#kronos-jdbc-wrapper)
4. [kronos-gradle-plugin](#kronos-gradle-plugin)
5. [kronos-maven-plugin](#kronos-maven-plugin)
6. [kronos-compiler-plugin-legacy](#kronos-compiler-plugin-legacy)

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
wrapperClassName = "com.kotlinorm.KronosBasicWrapper"
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

### KronosBasicWrapper
```kotlin
class KronosBasicWrapper(val dataSource: DataSource) : KronosDataSourceWrapper {
    companion object {
        private val transactionConnection = ThreadLocal<Connection?>()
    }
}
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
- `forList(task)` → `List<Map<String, Any>>` — maps ResultSet to list of maps
- `forMap(task)` → `Map<String, Any>?` — single row
- `forObject(task, kClass, isKPojo, superTypes)` → `Any?` — maps to KPojo or scalar
- KPojo instantiation: uses `kClassCreator` map (from @KronosInit) or `createInstance()` fallback

### Action Methods
- `update(task)` → `Int` — returns affected rows
- `batchUpdate(task)` → `IntArray` — batch with `addBatch()`/`executeBatch()`
- Last insert ID: when `LastInsertIdPlugin.enabled`, retrieves `RETURN_GENERATED_KEYS`

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

Wires the compiler plugin into Gradle Kotlin compilation. **Included build** (not a regular subproject).

### KronosGradlePlugin
```kotlin
class KronosGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun getCompilerPluginId() = "com.kotlinorm.kronos-compiler-gradle-plugin"
    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = "com.kotlinorm",
        artifactId = "kronos-compiler-plugin",
        version = "0.1.0-SNAPSHOT"
    )
    override fun getPluginArtifactForNative() = getPluginArtifact()
    override fun applyToCompilation(compilation) = provider {
        listOf(SubpluginOption("timestamp", System.currentTimeMillis().toString()))
    }
    override fun isApplicable(compilation) = true
}
```

Registered as `com.kotlinorm.kronos-gradle-plugin` in `gradlePlugin {}` block.

### Version Sync
Version in two places (must stay in sync):
1. `build-logic/src/main/kotlin/publishing.gradle.kts` → `project.version`
2. `kronos-gradle-plugin/.../KronosGradlePlugin.kt` → `version`

`bump-version.sh` updates both.

### Build
- Plugins: `java-gradle-plugin`, `kronos.publishing`
- Dependency: `kotlin-gradle-plugin-api` only
- Included via `settings.gradle.kts`: `includeBuild("kronos-gradle-plugin")`

---

## kronos-maven-plugin

Maven counterpart to the Gradle plugin.

```kotlin
class KronosMavenPlugin : KotlinMavenPluginExtension {
    override fun isApplicable(project, execution) = true
    override fun getCompilerPluginId() = "com.kotlinorm.kronos-maven-plugin"
    override fun getPluginOptions(project, execution) = emptyList<PluginOption>()
}
```

- Dependencies: `api(project(":kronos-compiler-plugin"))`, `kotlin-maven-plugin`, `maven-core`
- Copies `META-INF/services` from kronos-compiler-plugin so Maven discovers the `ComponentRegistrar`

---

## kronos-compiler-plugin-legacy

Pre-K2 Kotlin compiler plugin. Same transformation goals, older internal structure.
- Maintained for backward compatibility with older Kotlin versions
- Same IR approach: KPojo augmentation, DSL lambda parsing
- Not actively developed — new features go into `kronos-compiler-plugin`
