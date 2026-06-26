# ORM Operations, DSL & Task Execution

## Table of Contents
1. [DSL Beans](#dsl-beans)
2. [ORM Clause Classes](#orm-clause-classes)
3. [Task System](#task-system)
4. [Plugin / Hook System](#plugin-hook-system)
5. [Transaction System](#transaction-system)
6. [Value Transformers](#value-transformers)
7. [Cascade Operations](#cascade-operations)
8. [Join Operations](#join-operations)
9. [Union Operations](#union-operations)
10. [DDL Operations](#ddl-operations)
11. [Global Configuration (Kronos.kt)](#global-configuration)

---

## DSL Beans

Located in `kronos-core/src/main/kotlin/com/kotlinorm/beans/dsl/`.

### Field
The fundamental column/property representation:
```kotlin
open class Field(
    var columnName: String,
    var name: String,              // Kotlin property name
    val type: KColumnType = UNDEFINED,
    var primaryKey: PrimaryKeyType = NOT,
    val dateFormat: String? = null,
    var tableName: String = "",
    val cascade: KCascade? = null,
    val cascadeIsCollectionOrArray: Boolean = false,
    val kClass: KClass<out KPojo>? = null,
    val superTypes: List<String> = emptyList(),
    val ignore: Array<IgnoreAction>? = null,
    val isColumn: Boolean = true,  // false = cascade/reference field
    val length: Int = 0,
    val scale: Int = 0,
    val defaultValue: String? = null,
    val nullable: Boolean = true,
    val serializable: Boolean = false,
    val kDoc: String? = null
)
```
Equality: based on `(columnName, name, tableName)`.

### FunctionField
```kotlin
class FunctionField(
    var functionName: String,
    var fields: List<Pair<Field?, Any?>> = listOf()
) : Field(functionName)
```

### Criteria
Condition tree node:
```kotlin
class Criteria(
    var field: Field,
    var type: ConditionType,       // Equal, NotEqual, GreaterThan, In, Like, Between, IsNull, And, Or, Root, Sql, ...
    var not: Boolean = false,
    var value: Any? = null,
    val tableName: String? = "",
    var noValueStrategyType: NoValueStrategyType? = Auto,
    var children: MutableList<Criteria?> = mutableListOf()
)
```
`children` enables AND/OR nesting. `valueAcceptable` returns false for IsNull, And, Or, Root (no value param).

### KTableForCondition<T>
DSL class for `where {}` / `having {}` / `on {}` blocks. Provides operator overloads that return dummy values at runtime (real logic is in the compiler plugin):
- `==`, `!=`, `>`, `<`, `>=`, `<=` via `compareTo`
- `contains` for `in` checks
- `like`, `notLike`, `between`, `notBetween`, `startsWith`, `endsWith`, `regexp`
- `eq`, `neq`, `isNull`, `notNull`, `lt`, `gt`, `le`, `ge` as property getters
- `ifNoValue(strategy)` — null-value handling (ignore, alwaysTrue, alwaysFalse)
- `takeIf(boolean)` — conditional criteria
- `asSql()` — raw SQL injection
- `cast()` — type casting

### KTableForSelect<T>
DSL for `select {}` blocks:
- `it.field1 + it.field2` (operator `+` returns 1, compiler captures fields)
- `+it.field1` (unary plus for single field)
- `it.field1 as_ "alias"` (aliasing)
- `addField(Field(...))` (manual field addition)
- `it - it.id` (exclusion — all fields except id)
- `it.eq` (all columns)

### KTableForSet<T>
DSL for `set {}` blocks in updates:
- `it.name = "value"` — set field to literal
- `it.age = it.age + 1` — increment
- Compiler plugin captures assignments as field-value pairs

### KTableForSort<T>
DSL for `orderBy {}` blocks:
- `it.age.desc()` — descending
- `it.name.asc()` — ascending
- `it.age.desc() + it.name.asc()` — multiple sort fields

### KTableForReference<T>
DSL for `cascade {}` blocks:
- Selects which cascade fields to include in cascade operations

### KCascade
```kotlin
data class KCascade(
    val properties: Array<String>,       // FK fields on this entity
    val targetProperties: Array<String>, // PK fields on target entity
    val usage: Array<CascadeUsage>       // SELECT, INSERT, UPDATE, DELETE
)
```

---

## ORM Clause Classes

Located in `kronos-core/src/main/kotlin/com/kotlinorm/orm/`. Each operation follows the same pattern:

### Entry Points (Patch.kt files)
Each operation has a `Patch.kt` with extension functions on `KPojo`:
```kotlin
// orm/select/Patch.kt
inline fun <reified T : KPojo> T.select(noinline fields: ToSelect<T, Any?> = null): SelectClause<T>

// orm/insert/Patch.kt
inline fun <reified T : KPojo> T.insert(noinline fields: ToSelect<T, Any?> = null): InsertClause<T>
inline fun <reified T : KPojo> List<T>.insert(noinline fields: ToSelect<T, Any?> = null): InsertClause<T>

// orm/update/Patch.kt
inline fun <reified T : KPojo> T.update(noinline fields: ToSelect<T, Any?> = null): UpdateClause<T>

// orm/delete/Patch.kt
inline fun <reified T : KPojo> T.delete(): DeleteClause<T>

// orm/upsert/Patch.kt
inline fun <reified T : KPojo> T.upsert(noinline fields: ToSelect<T, Any?> = null): UpsertClause<T>
```

### SelectClause
Chain methods: `where {}`, `orderBy {}`, `groupBy {}`, `having {}`, `page(pageIndex, pageSize)`, `limit(n)`, `lock()`, `cascade()`, `patch()`, `withTotal()`

Terminal methods: `queryList()`, `queryOne()`, `queryOneOrNull()`, `queryMap()`, `queryMapOrNull()`

Build flow in `toStatement()`:
1. Collects selected fields (from `select {}` lambda or all columns)
2. Builds WHERE from `where {}` criteria + entity non-null values
3. Applies logic delete filter (auto-adds `deleted = 0`)
4. Applies pagination (dialect-specific)
5. Applies locking
6. Returns `SelectStatement` AST

### InsertClause
Chain methods: `cascade()`, `patch()`

Terminal: `execute()` → returns `KronosActionTask` with `lastInsertId`

Build flow:
1. Collects fields to insert (from lambda or non-null properties)
2. Applies strategies: primary key generation (identity/UUID/snowflake), create time, update time
3. Handles `@Default` values for null fields
4. Builds `InsertStatement` AST
5. For batch: groups by field set, creates multiple statements

### UpdateClause
Chain methods: `set {}`, `by {}`, `where {}`, `cascade()`, `lock()`, `patch()`

Terminal: `execute()`

Build flow:
1. Collects fields to update (from `set {}` or `update {}` lambda)
2. Builds WHERE from `by {}` fields or `where {}` criteria
3. Applies strategies: update time, optimistic lock (adds `version = :version` to WHERE, increments version)
4. Applies logic delete filter to WHERE
5. Builds `UpdateStatement` AST

### DeleteClause
Chain methods: `by {}`, `where {}`, `logic(enabled)`, `cascade()`, `patch()`

Terminal: `execute()`

Build flow:
1. If `logic = true` and `logicDeleteStrategy` is set → generates `UpdateStatement` (sets deleted flag + update time + version increment) instead of `DeleteStatement`
2. Otherwise generates `DeleteStatement` with WHERE
3. Applies logic delete condition to WHERE (only delete non-deleted records)

### UpsertClause
Chain methods: `on {}` (conflict fields), `onConflict()`, `cascade()`, `lock()`, `patch()`

Two modes:
1. `onConflict()` — generates DB-native upsert SQL via `ConflictResolver` (ON DUPLICATE KEY UPDATE / ON CONFLICT DO UPDATE / etc.)
2. Default — uses `doBeforeExecute` hook: SELECT COUNT with pessimistic lock → if exists, UPDATE; else INSERT

---

## Task System

Located in `kronos-core/src/main/kotlin/com/kotlinorm/beans/task/`.

### Task Hierarchy
```
KAtomicTask (base)
├── KAtomicQueryTask(sql, paramMap)     — single SELECT
├── KAtomicActionTask(sql, paramMap)    — single INSERT/UPDATE/DELETE
└── KronosAtomicBatchTask(sql, paramMapArr)  — batch DML

KronosQueryTask(atomicTask)             — wraps query + execution
KronosActionTask(atomicTasks: List)     — wraps multiple actions + execution
```

### Execution Flow
```kotlin
// Query execution
val task = selectClause.build()  // → KronosQueryTask
task.queryList(wrapper)          // → wrapper.forList(atomicTask)

// Action execution
val task = insertClause.build()  // → KronosActionTask
task.execute(wrapper)            // → wrapper.transact { tasks.map { it.execute() } }
```

`KronosActionTask.execute()` automatically wraps grouped tasks in a transaction.

### Type Aliases (types/)
```kotlin
typealias KTableField<T, R> = KTableForSelect<T>.() -> R
typealias KTableConditionalField<T, R> = KTableForCondition<T>.() -> R
typealias KTableSortField<T, R> = KTableForSort<T>.() -> R
typealias ToSelect<T, R> = (KTableForSelect<T>.() -> R)?
```

---

## Plugin / Hook System

### DataGuardPlugin
`plugins/DataGuardPlugin.kt` — prevents full-table UPDATE/DELETE without WHERE:
```kotlin
object DataGuardPlugin {
    var enabled = false
    fun validate(task: KAtomicActionTask) {
        if (!enabled) return
        // Throws if UPDATE/DELETE has no WHERE clause
    }
}
```

### LastInsertIdPlugin
`plugins/LastInsertIdPlugin.kt` — captures last insert ID after INSERT:
```kotlin
object LastInsertIdPlugin {
    var enabled = false  // auto-enabled by Kronos.init {}
}
```
When enabled, INSERT execution returns the auto-generated key.

### doBeforeExecute Hook
ORM clauses support `doBeforeExecute` callbacks that run before the main SQL execution. Used by:
- UpsertClause (default mode): SELECT COUNT → decide INSERT or UPDATE
- CascadeInsertClause: insert related entities first
- CascadeUpdateClause: update related entities
- CascadeDeleteClause: delete related entities

---

## Transaction System

### API
```kotlin
import com.kotlinorm.Kronos.transact

transact { /* operations */ }
transact(wrapper, isolation = READ_COMMITTED, timeout = 30) { /* ... */ }
transact {
    val sp = savepoint("sp1")
    // ... operations ...
    rollbackToSavepoint(sp)  // partial rollback
    // or: releaseSavepoint(sp)
}
```

### TransactionScope
```kotlin
class TransactionScope(internal val connection: Connection? = null) {
    fun savepoint(name: String): Savepoint
    fun rollbackToSavepoint(savepoint: Savepoint)
    fun releaseSavepoint(savepoint: Savepoint)
}
```

### KronosBasicWrapper Implementation
- `ThreadLocal<Connection?>` for connection propagation
- Nested transactions: inner `transact` reuses outer connection (join semantics, like Spring REQUIRED)
- Outer transaction: sets autoCommit=false, applies isolation level, commits on success, rollbacks on exception
- `obtainConnection()`: if ThreadLocal has connection → reuse (don't close); else → new connection (caller closes)
- All `forList`/`forMap`/`update`/`batchUpdate` calls automatically participate in active transaction

### Isolation Levels
```kotlin
enum class TransactionIsolation(val level: Int) {
    READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE
}
```

---

## Value Transformers

`beans/transformers/` — type conversion utilities used during result set mapping:
- `getSafeValue(value, targetType)` — converts JDBC values to Kotlin types safely
- Handles: Number→Int/Long/Float/Double/BigDecimal, String→Enum, Timestamp→LocalDateTime/Date, Boolean conversions
- Used by `safeFromMapData()` (generated by compiler plugin) and `KronosBasicWrapper`

---

## Cascade Operations

`orm/cascade/` — handles `@Cascade` annotated fields.

### CascadeSelectClause
After main SELECT, for each cascade field:
1. Extracts FK values from main result
2. Builds SELECT for related entity with FK condition
3. Maps results back to parent entity's cascade field
4. Supports `depth` parameter for recursive cascade

### CascadeInsertClause
1. Inserts main entity first (gets generated PK)
2. Sets FK on related entities
3. Inserts related entities
4. For collections: batch insert

### CascadeUpdateClause / CascadeDeleteClause
Similar pattern: operates on main entity, then cascades to related entities.

### Cascade Usage Control
`@Cascade(usage = [SELECT, INSERT])` — limits which operations cascade. `refUseFor(usage)` checks this.

---

## Join Operations

`orm/join/` — supports 2-16 table joins.

Entry: `T1.join(T2) { t1, t2 -> on { ... }; select { ... }; where { ... } }`

Variants: `join` (INNER), `leftJoin`, `rightJoin`, `fullJoin`, `crossJoin`

`SelectFrom` base class extends `KSelectable<T1>`:
- `on {}` — join condition
- `select {}` — field selection across tables
- `where {}` — filter condition
- `orderBy {}`, `groupBy {}`, `having {}`, `page()`, `limit()`, `lock()`
- `withTotal()` — also returns total count
- `queryList()`, `queryOne()`, `queryOneOrNull()`

Cross-database joins: when tables are on different data sources, Kronos fetches from each source separately and joins in memory.

---

## Union Operations

`orm/union/` — UNION / UNION ALL support.

```kotlin
val query1 = User().select { it.name }
val query2 = Admin().select { it.name }
query1.union(query2).queryList()
query1.unionAll(query2).queryList()
```

Builds `UnionStatement` AST with optional orderBy and limit.

---

## DDL Operations

`orm/ddl/` — table lifecycle management.

```kotlin
val ds = Kronos.dataSource()
ds.table.createTable(User())    // CREATE TABLE IF NOT EXISTS
ds.table.syncTable(User())      // ALTER TABLE — add/modify columns, create/drop indexes
ds.table.dropTable(User())      // DROP TABLE IF EXISTS
```

`syncTable` compares KPojo definition with actual DB schema:
1. Queries existing columns via `showColumnsFrom()`
2. Queries existing indexes via `showIndexesFrom()`
3. Generates ALTER TABLE statements for differences
4. Handles column additions, type changes, index creation/deletion

---

## Global Configuration

`Kronos.kt` singleton:
```kotlin
object Kronos {
    var dataSource: () -> KronosDataSourceWrapper    // default data source factory
    var fieldNamingStrategy: KronosNamingStrategy     // property → column name
    var tableNamingStrategy: KronosNamingStrategy     // class → table name
    var primaryKeyStrategy: KronosCommonStrategy      // default PK strategy
    var createTimeStrategy: KronosCommonStrategy
    var updateTimeStrategy: KronosCommonStrategy
    var logicDeleteStrategy: KronosCommonStrategy
    var optimisticLockStrategy: KronosCommonStrategy
    var defaultDateFormat: String                     // "yyyy-MM-dd HH:mm:ss"
    var timeZone: ZoneId
    var serializeProcessor: KronosSerializeProcessor  // JSON serialization
    var strictSetValue: Boolean                       // strict type checking in set
    var noValueStrategy: NoValueStrategy              // default null-value handling
    var defaultLogger: KLoggerFactory
    var loggerType: KLoggerType
    var logPath: List<String>

    @KronosInit
    fun init(action: Kronos.() -> Unit)  // enables LastInsertIdPlugin, runs config block
    fun transact(wrapper?, isolation?, timeout?, block)
}
```

---

## KPojo Interface

The core entity contract. All method bodies are generated by the compiler plugin at compile time.

```kotlin
interface KPojo {
    fun kClass() = KPojo::class                           // overridden → actual KClass
    fun toDataMap() = mutableMapOf<String, Any?>()         // overridden → property→value map
    fun <T : KPojo> safeFromMapData(map: Map<String, Any?>) = this as T  // overridden → type-safe map→instance
    fun <T : KPojo> fromMapData(map: Map<String, Any?>) = this as T      // overridden → map→instance
    operator fun get(name: String): Any? = null            // overridden → dynamic property access
    operator fun set(name: String, value: Any?) {}         // overridden → dynamic property set
    var __tableName: String                                // overridden → table name
    var __tableComment: String                             // overridden → table comment from KDoc
    fun kronosTableIndex() = mutableListOf<KTableIndex>()  // overridden → index definitions
    fun kronosColumns() = mutableListOf<Field>()           // overridden → all column metadata
    fun kronosPrimaryKey() = primaryKeyStrategy            // overridden → PK strategy from annotation
    fun kronosCreateTime() = createTimeStrategy
    fun kronosUpdateTime() = updateTimeStrategy
    fun kronosLogicDelete() = logicDeleteStrategy
    fun kronosOptimisticLock() = optimisticLockStrategy
}
```

Default implementations return global strategy values. The compiler plugin replaces them with entity-specific logic based on annotations.

---

## KronosDataSourceWrapper Interface

The DB operation contract that all data source implementations must fulfill:

```kotlin
interface KronosDataSourceWrapper {
    val url: String
    val userName: String
    fun forList(task: KAtomicQueryTask): List<Map<String, Any>>
    fun forMap(task: KAtomicQueryTask): Map<String, Any>?
    fun forObject(task: KAtomicQueryTask, kClass: KClass<*>, isKPojo: Boolean, superTypes: List<String>): Any?
    fun update(task: KAtomicActionTask): Int
    fun batchUpdate(task: KronosAtomicBatchTask): IntArray
    fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any?
}
```

---

## Raw SQL Execution

Via `SqlHandler` extension functions on `KronosDataSourceWrapper`:

```kotlin
// Query
val results = dataSource.forList(
    KronosAtomicQueryTask(
        "SELECT * FROM user WHERE name = :name AND age > :age",
        mapOf("name" to "Kronos", "age" to 18)
    )
)

// Single object
val user = dataSource.queryOne<User>(
    KronosAtomicQueryTask("SELECT * FROM user WHERE id = :id", mapOf("id" to 1))
)

// Action (INSERT/UPDATE/DELETE)
val affected = dataSource.update(
    KronosAtomicActionTask(
        "UPDATE user SET name = :name WHERE id = :id",
        mapOf("name" to "new", "id" to 1)
    )
)
```

Named parameters (`:name`) are automatically converted to positional (`?`) by `NamedParameterUtils`.

---

## patch() Method

Available on all clause classes. Adds extra named parameters to the SQL execution:

```kotlin
User().select()
    .where { "name = :name".asSql() }
    .patch("name" to "Kronos")  // injects :name parameter
    .queryList()
```

Used when raw SQL conditions reference parameters not derived from entity properties.

---

## Naming Strategies

```kotlin
interface KronosNamingStrategy {
    fun k2db(name: String): String   // Kotlin property name → DB column name
    fun db2k(name: String): String   // DB column name → Kotlin property name
}
```

Built-in:
- `lineHumpNamingStrategy` — camelCase ↔ snake_case (`userName` ↔ `user_name`)
- `noneNamingStrategy` — no conversion (name used as-is)

Custom:
```kotlin
Kronos.init {
    fieldNamingStrategy = object : KronosNamingStrategy {
        override fun k2db(name: String) = name.uppercase()
        override fun db2k(name: String) = name.lowercase()
    }
}
```

---

## Serialization

For complex object fields stored as JSON in the database:

```kotlin
data class User(
    @Serialize
    var tags: List<String>? = null,
    @Serialize
    var profile: Profile? = null
) : KPojo
```

Configure a processor:
```kotlin
Kronos.init {
    serializeProcessor = GsonProcessor()  // or JacksonProcessor, custom impl
}
```

Interface:
```kotlin
interface KronosSerializeProcessor {
    fun serialize(value: Any?): String
    fun <T> deserialize(value: String, kClass: KClass<T>): T
}
```

---

## Cross-Database Join Details

When join tables are on different data sources:
1. Each table's data is fetched from its own data source independently
2. Results are joined in memory (Kotlin-side)
3. Pagination and sorting are applied after the in-memory join
4. Use `withTotal()` to get the total count alongside results

This enables querying across MySQL + PostgreSQL (or any combination) in a single join operation.

---

## Internationalization (i18n)

`kronos-core/src/main/kotlin/com/kotlinorm/i18n/` — error messages and logging support multiple languages. Exception messages can be localized.

---

## Contributing Guidelines

- Branch from `main` for features/fixes
- Follow Kotlin coding conventions
- Commit format: `<type>(<scope>): <subject>` — types: feat, fix, docs, refactor, test, ci, chore, perf, style, build. Scope should be module name prefixed with `module:` (e.g., `feat(module:kronos-core): add new feature`)
- All public API methods must be documented
- Pass all tests and static analysis (Detekt & Codacy) before PR
- PR checklist: tests pass, Detekt clean, docs updated if needed
