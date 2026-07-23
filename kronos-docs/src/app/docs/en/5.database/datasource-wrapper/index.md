{% import "../../../macros/macros-en.njk" as $ %}

`KronosDataSourceWrapper` connects Kronos SQL tasks to a database execution engine. The built-in JDBC implementation is `KronosJdbcWrapper`, and custom wrappers can delegate execution to Spring JDBC, JDBI, MyBatis, or another data access layer.

## Android SQLite

For Android/JVM `SQLiteDatabase` setup and a complete wrapper reference, see {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }}.

## Properties

{{ $.members([
    ['url', 'Database connection URL', 'String'],
    ['userName', 'Database username from JDBC metadata', 'String'],
    ['dbType', 'Database type used by SQL rendering and DDL statements', 'DBType'],
    ['sqlDialect', 'SQL dialect resolved from dbType', 'SqlDialect']
]) }}

Read the metadata after creating the wrapper.

```kotlin group="Properties" name="kotlin" icon="kotlin"
val wrapper = KronosJdbcWrapper(dataSource)

println(wrapper.url)
println(wrapper.userName)
println(wrapper.dbType)
println(wrapper.sqlDialect.family)
```

```text group="Properties" name="output"
jdbc:postgresql://localhost:5432/kronos
postgres
Postgres
PostgreSql
```

## Configure the built-in JDBC wrapper

`KronosJdbcWrapper` accepts a `DataSource`, an optional `DBType`, and a configuration block. Pass `databaseType` when the JDBC metadata name does not map to the dialect you want Kronos to use.

```kotlin group="Jdbc config 1" name="database type" icon="kotlin"
import com.kotlinorm.enums.DBType
import com.kotlinorm.wrappers.KronosJdbcWrapper

val wrapper = KronosJdbcWrapper(
    dataSource = dataSource,
    databaseType = DBType.Mysql
)
```

Use the configuration block for JDBC statement settings and SQL warning handling.

```kotlin group="Jdbc config 2" name="statement" icon="kotlin"
import com.kotlinorm.wrappers.KronosDuplicateKeyException
import com.kotlinorm.wrappers.KronosJdbcWrapper
import com.kotlinorm.wrappers.KronosSqlWarningPolicy
import com.kotlinorm.wrappers.SqlStateSQLExceptionTranslator

val wrapper = KronosJdbcWrapper(dataSource) {
    statement.fetchSize = 1000
    statement.maxRows = 5000
    statement.queryTimeoutSeconds = 30
    warningPolicy = KronosSqlWarningPolicy.THROW
    exceptionTranslator = { sql, params, exception ->
        if (exception.errorCode == 1062) {
            KronosDuplicateKeyException("Duplicate key", sql, params, exception)
        } else {
            SqlStateSQLExceptionTranslator().translate(sql, params, exception)
        }
    }
}
```

The built-in wrapper also exposes registries for custom parameter binding and result mapping.

```kotlin group="Jdbc config 3" name="mapping" icon="kotlin"
import com.kotlinorm.wrappers.KronosArgument
import com.kotlinorm.wrappers.KronosArgumentFactory
import com.kotlinorm.wrappers.KronosJdbcWrapper
import com.kotlinorm.wrappers.KronosPhysicalReadResult
import com.kotlinorm.wrappers.KronosPhysicalValueReader
import java.sql.Types
import java.util.UUID

val wrapper = KronosJdbcWrapper(dataSource) {
    arguments.register(KronosArgumentFactory { value, _ ->
        if (value is UUID) {
            KronosArgument { position, statement, _ ->
                statement.setObject(position, value, Types.OTHER)
            }
        } else {
            null
        }
    })

    columnMappers.register(KronosPhysicalValueReader { resultSet, position, _ ->
        val jdbcTypeName = resultSet.metaData.getColumnTypeName(position)
        if (jdbcTypeName.equals("jsonb", ignoreCase = true)) {
            KronosPhysicalReadResult.Handled(resultSet.getString(position))
        } else {
            KronosPhysicalReadResult.NotHandled
        }
    })
}
```

Physical readers inspect JDBC metadata or vendor behavior before `ResultSet.getObject(position)`. Return `Handled(null)` when the column was handled and its value is SQL `NULL`; return `NotHandled` to continue with the default JDBC read. Register a custom value mapping when the resulting string should become a domain value such as `UUID`.

## Query lists with {{ $.title("toList(task)") }}

`toList(task)` executes a select task and maps every row according to `task.targetType`. The `KType` keeps generic arguments and nullability, so wrappers do not need separate `KClass`, `isKPojo`, or supertype parameters.

```kotlin group="Query task list" name="signature" icon="kotlin"
fun toList(task: KAtomicQueryTask): List<Any?>
```

```kotlin group="Query task list" name="kotlin" icon="kotlin"
val users = wrapper.toList(
    KronosAtomicQueryTask(
        sql = "SELECT id, name FROM user WHERE age > :age",
        paramMap = mapOf("age" to 18),
        targetType = typeOf<User>()
    )
)
```

The built-in JDBC wrapper recognizes scalar types, typed row maps, and `KPojo` types from this target `KType`. JDBC row mapping materializes each row as a `LinkedHashMap`, so the supported map containers are only direct `Map` and `MutableMap` declarations (including a nullable top-level target). Fixed, reordered, and custom `Map` subtypes are rejected because Kronos cannot return their declared container safely. Map keys must be `String`, `String?`, or `*`; `Any`, `Any?`, and `*` values remain raw, while a typed value argument is converted using its complete `KType`.

## Query one value with {{ $.title("first(task)") }}

`first(task)` uses the same target type and returns `null` for an empty result. Higher-level `first<T>()` decides whether an empty result should throw based on the requested type's nullability.

```kotlin group="Query task first" name="signature" icon="kotlin"
fun first(task: KAtomicQueryTask): Any?
```

```kotlin group="Query task first" name="kotlin" icon="kotlin"
val user = wrapper.first(
    KronosAtomicQueryTask(
        sql = "SELECT id, name FROM user WHERE id = :id",
        paramMap = mapOf("id" to 1),
        targetType = typeOf<User?>()
    )
)
```

## Execute writes with {{ $.title("update(task)") }}

`update(task)` executes insert, update, delete, and DDL tasks and returns affected rows.

```kotlin group="update" name="signature" icon="kotlin"
fun update(task: KAtomicActionTask): Int
```

```kotlin group="update" name="kotlin" icon="kotlin"
val affectedRows = wrapper.update(
    KronosAtomicActionTask(
        sql = "UPDATE user SET name = :name WHERE id = :id",
        paramMap = mapOf("name" to "Ada", "id" to 1)
    )
)
```

```text group="update" name="result"
1
```

## Execute batch writes with {{ $.title("batchUpdate(task)") }}

`batchUpdate(task)` executes one SQL statement with multiple parameter maps and returns the JDBC update count for each batch item.

```kotlin group="batchUpdate" name="signature" icon="kotlin"
fun batchUpdate(task: KronosAtomicBatchTask): IntArray
```

```kotlin group="batchUpdate" name="kotlin" icon="kotlin"
val affectedRows = wrapper.batchUpdate(
    KronosAtomicBatchTask(
        sql = "UPDATE user SET name = :name WHERE id = :id",
        paramMapArr = arrayOf(
            mapOf("name" to "Ada", "id" to 1),
            mapOf("name" to "Linus", "id" to 2)
        )
    )
)
```

```text group="batchUpdate" name="result"
[1, 1]
```

## Execute transactions with {{ $.title("transact") }}

`transact(...)` runs the block with a `TransactionScope` receiver. JDBC wrappers can use `savepoint`, `rollbackToSavepoint`, and `releaseSavepoint` inside the block.

```kotlin group="transact" name="signature" icon="kotlin"
fun transact(
    isolation: TransactionIsolation? = null,
    timeout: Int? = null,
    block: TransactionScope.() -> Any?
): Any?
```

```kotlin group="transact" name="kotlin" icon="kotlin"
val result = wrapper.transact(
    isolation = TransactionIsolation.READ_COMMITTED,
    timeout = 30
) {
    val point = savepoint("before_user_update")

    try {
        wrapper.update(
            KronosAtomicActionTask(
                sql = "UPDATE user SET name = :name WHERE id = :id",
                paramMap = mapOf("name" to "Ada", "id" to 1)
            )
        )
        releaseSavepoint(point)
    } catch (e: Exception) {
        rollbackToSavepoint(point)
        throw e
    }
}
```

## Convert named parameters for JDBC

`task.parsed()` converts `:name` parameters to JDBC `?` placeholders. `KronosAtomicBatchTask.parsedArr()` returns the same shape for every batch item.

```kotlin group="parsed" name="query task" icon="kotlin"
val task = KronosAtomicQueryTask(
    sql = "SELECT id, name FROM user WHERE id = :id AND status = :status",
    paramMap = mapOf("id" to 1, "status" to "ACTIVE"),
    targetType = typeOf<Map<String, Any?>>()
)

val parsed = task.parsed()

println(parsed.jdbcSql)
println(parsed.jdbcParamList.toList())
```

```text group="parsed" name="output"
SELECT id, name FROM user WHERE id = ? AND status = ?
[1, ACTIVE]
```

```kotlin group="parsed" name="batch task" icon="kotlin"
val batchTask = KronosAtomicBatchTask(
    sql = "UPDATE user SET status = :status WHERE id = :id",
    paramMapArr = arrayOf(
        mapOf("status" to "ACTIVE", "id" to 1),
        mapOf("status" to "LOCKED", "id" to 2)
    )
)

val (jdbcSql, batchParams) = batchTask.parsedArr()

println(jdbcSql)
println(batchParams)
```

```text group="parsed" name="batch output"
UPDATE user SET status = ? WHERE id = ?
[[ACTIVE, 1], [LOCKED, 2]]
```

For a Spring JDBC wrapper example, see {{ $.keyword("database/custom-wrapper", ["Data Sources and Third-party Framework"]) }}.
