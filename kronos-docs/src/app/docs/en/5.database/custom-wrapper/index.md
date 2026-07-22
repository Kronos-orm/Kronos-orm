{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Build a wrapper for an existing framework

`KronosDataSourceWrapper` lets Kronos use an execution layer that already exists in your project. A wrapper receives Kronos SQL tasks and delegates them to the framework's query, update, batch, and transaction APIs.

For the full interface contract, see {{ $.keyword("database/datasource-wrapper", ["Data source wrapper"]) }}.

## Add Spring JDBC and Kronos

Spring Boot projects usually manage Spring dependency versions through the selected Boot version. Use the latest stable JDBC driver that matches your database server and JDK.

```xml group="Spring JDBC" name="maven" icon="maven"
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kotlinorm</groupId>
        <artifactId>kronos-core</artifactId>
        <version>{{ $.kronosVersion() }}</version>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>${mysql-connector-j.version}</version>
    </dependency>
</dependencies>
```

```kotlin group="Spring JDBC" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

## Read metadata from `DataSource`

The wrapper exposes `url`, `userName`, and `dbType` from JDBC metadata. Kronos uses `dbType` to select SQL rendering and table statements.

```kotlin group="Spring wrapper 1" name="metadata" icon="kotlin"
class SpringJdbcKronosWrapper(
    private val dataSource: DataSource
) : KronosDataSourceWrapper {
    override val url: String
    override val userName: String
    override val dbType: DBType

    private val namedJdbc = NamedParameterJdbcTemplate(dataSource)
    private val transactionManager = DataSourceTransactionManager(dataSource)

    init {
        dataSource.connection.use { conn ->
            url = conn.metaData.url
            userName = conn.metaData.userName ?: ""
            dbType = DBType.fromName(conn.metaData.databaseProductName)
        }
    }
}
```

## Delegate queries

`KAtomicQueryTask.sql` uses named parameters, `paramMap` contains the values, and `targetType` is the complete requested `KType`. A custom wrapper implements one list method and one first-row method for maps, scalar values, KPojo classes, and DTOs.

```kotlin group="Spring wrapper 2" name="query mapping" icon="kotlin"
import kotlin.reflect.jvm.jvmErasure

private fun rowMapper(targetType: KType): RowMapper<Any?> {
    val targetClass = targetType.jvmErasure

    return when {
        targetClass == Map::class -> ColumnMapRowMapper()
        targetClass in setOf(String::class, Int::class, Long::class, Boolean::class) ->
            SingleColumnRowMapper(targetClass.java)
        else -> DataClassRowMapper(targetClass.java)
    }
}

override fun toList(task: KAtomicQueryTask): List<Any?> =
    namedJdbc.query(task.sql, task.paramMap, rowMapper(task.targetType))

override fun first(task: KAtomicQueryTask): Any? =
    toList(task).firstOrNull()
```

```kotlin group="Spring wrapper 2" name="query usage" icon="kotlin"
val users = wrapper.toList(
    KronosAtomicQueryTask(
        sql = "SELECT id, name FROM user WHERE status = :status",
        paramMap = mapOf("status" to "ACTIVE"),
        targetType = typeOf<User>()
    )
)
```

## Delegate writes

`KAtomicActionTask` carries one parameter map. `KronosAtomicBatchTask` carries one SQL statement and multiple parameter maps.

```kotlin group="Spring wrapper 4" name="writes" icon="kotlin"
override fun update(task: KAtomicActionTask): Int {
    return namedJdbc.update(task.sql, task.paramMap)
}

override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
    return namedJdbc.batchUpdate(task.sql, task.paramMapArr ?: emptyArray())
}
```

```kotlin group="Spring wrapper 4" name="write usage" icon="kotlin"
val affectedRows = wrapper.batchUpdate(
    KronosAtomicBatchTask(
        sql = "UPDATE user SET status = :status WHERE id = :id",
        paramMapArr = arrayOf(
            mapOf("status" to "ACTIVE", "id" to 1),
            mapOf("status" to "LOCKED", "id" to 2)
        )
    )
)
```

## Delegate transactions

`transact(...)` receives Kronos transaction options and runs the block with `TransactionScope`. Passing the Spring-managed JDBC connection enables `TransactionScope` savepoints.

```kotlin group="Spring wrapper 5" name="transaction" icon="kotlin"
override fun transact(
    isolation: TransactionIsolation?,
    timeout: Int?,
    block: TransactionScope.() -> Any?
): Any? {
    val definition = DefaultTransactionDefinition().apply {
        isolation?.let { isolationLevel = it.level }
        timeout?.let { this.timeout = it }
    }

    return TransactionTemplate(transactionManager, definition).execute {
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            TransactionScope(connection).block()
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }
}
```

```kotlin group="Spring wrapper 5" name="transaction usage" icon="kotlin"
wrapper.transact(timeout = 30) {
    val point = savepoint("before_status_update")
    try {
        wrapper.update(
            KronosAtomicActionTask(
                sql = "UPDATE user SET status = :status WHERE id = :id",
                paramMap = mapOf("status" to "ACTIVE", "id" to 1)
            )
        )
        releaseSavepoint(point)
    } catch (e: Exception) {
        rollbackToSavepoint(point)
        throw e
    }
}
```

## Register the wrapper

Register the wrapper once during application startup, or expose it as a Spring bean and assign it where your application initializes Kronos.

```kotlin group="Spring wrapper 6" name="register" icon="kotlin"
@Bean
fun kronosWrapper(dataSource: DataSource): KronosDataSourceWrapper {
    return SpringJdbcKronosWrapper(dataSource)
}

@Bean
fun kronosConfiguration(wrapper: KronosDataSourceWrapper): Any {
    Kronos.dataSource = { wrapper }
    return Any()
}
```

## Use frameworks with positional parameters

Frameworks that execute JDBC `?` parameters can use `task.parsed()` and `KronosAtomicBatchTask.parsedArr()`.

```kotlin group="Positional parameters" name="single task" icon="kotlin"
val task = KronosAtomicActionTask(
    sql = "UPDATE user SET status = :status WHERE id = :id",
    paramMap = mapOf("status" to "ACTIVE", "id" to 1)
)

val (jdbcSql, params) = task.parsed()

println(jdbcSql)
println(params.toList())
```

```text group="Positional parameters" name="output"
UPDATE user SET status = ? WHERE id = ?
[ACTIVE, 1]
```

```kotlin group="Positional parameters" name="batch task" icon="kotlin"
val (jdbcSql, paramList) = batchTask.parsedArr()

paramList.forEach { params ->
    preparedStatement.setObject(1, params[0])
    preparedStatement.setObject(2, params[1])
    preparedStatement.addBatch()
}
```

The built-in JDBC implementation is a complete reference for positional parameter execution: [KronosJdbcWrapper.kt](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-jdbc-wrapper/src/main/kotlin/com/kotlinorm/wrappers/KronosJdbcWrapper.kt).
