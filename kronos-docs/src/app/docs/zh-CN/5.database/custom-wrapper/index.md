{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 为现有框架实现wrapper

`KronosDataSourceWrapper`让Kronos使用项目中已有的数据库执行层。wrapper接收Kronos SQL task，并把它们委托给框架的query、update、batch和transaction API。

完整接口说明见{{ $.keyword("database/datasource-wrapper", ["数据源包装器"]) }}。

## 添加Spring JDBC和Kronos

Spring Boot项目通常由选定的Boot版本管理Spring依赖版本。JDBC Driver使用与数据库服务端、JDK匹配的最新稳定版。

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

## 从`DataSource`读取元信息

wrapper从JDBC元信息中提供`url`、`userName`和`dbType`。Kronos使用`dbType`选择SQL渲染和表结构statement。

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

## 委托Map查询

`KAtomicQueryTask.sql`使用命名参数，`KAtomicQueryTask.paramMap`保存参数值。

```kotlin group="Spring wrapper 2" name="map queries" icon="kotlin"
override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
    return namedJdbc.queryForList(task.sql, task.paramMap)
}

override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
    return try {
        namedJdbc.queryForMap(task.sql, task.paramMap)
    } catch (e: DataAccessException) {
        null
    }
}
```

```kotlin group="Spring wrapper 2" name="map usage" icon="kotlin"
val rows = wrapper.forList(
    KronosAtomicQueryTask(
        sql = "SELECT id, name FROM user WHERE status = :status",
        paramMap = mapOf("status" to "ACTIVE")
    )
)
```

## 委托对象查询

`isKPojo`表示Kronos需要映射对象。`kClass`是查询API选定的结果类型。

```kotlin group="Spring wrapper 3" name="object queries" icon="kotlin"
override fun forList(
    task: KAtomicQueryTask,
    kClass: KClass<*>,
    isKPojo: Boolean,
    superTypes: List<String>
): List<Any> {
    return if (isKPojo) {
        namedJdbc.query(task.sql, task.paramMap, DataClassRowMapper(kClass.java))
    } else {
        namedJdbc.queryForList(task.sql, task.paramMap, kClass.java)
    }
}

override fun forObject(
    task: KAtomicQueryTask,
    kClass: KClass<*>,
    isKPojo: Boolean,
    superTypes: List<String>
): Any? {
    return try {
        if (isKPojo) {
            namedJdbc.queryForObject(task.sql, task.paramMap, DataClassRowMapper(kClass.java))
        } else {
            namedJdbc.queryForObject(task.sql, task.paramMap, kClass.java)
        }
    } catch (e: DataAccessException) {
        null
    }
}
```

```kotlin group="Spring wrapper 3" name="object usage" icon="kotlin"
val users = wrapper.forList(
    KronosAtomicQueryTask(
        sql = "SELECT id, name FROM user WHERE status = :status",
        paramMap = mapOf("status" to "ACTIVE")
    ),
    User::class,
    isKPojo = true,
    superTypes = listOf("com.kotlinorm.interfaces.KPojo")
)
```

## 委托写入

`KAtomicActionTask`携带一组参数。`KronosAtomicBatchTask`携带一条SQL和多组参数。

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

## 委托事务

`transact(...)`接收Kronos事务选项，并用`TransactionScope`执行block。传入Spring管理的JDBC connection后，可以使用`TransactionScope` savepoint。

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

## 注册wrapper

应用启动时注册wrapper，或把它声明为Spring bean，并在初始化Kronos的位置赋值。

```kotlin group="Spring wrapper 6" name="register" icon="kotlin"
@Bean
fun kronosWrapper(dataSource: DataSource): KronosDataSourceWrapper {
    return SpringJdbcKronosWrapper(dataSource)
}

@Bean
fun kronosConfiguration(wrapper: KronosDataSourceWrapper): Any {
    with(Kronos) {
        dataSource = { wrapper }
    }
    return Any()
}
```

## 使用顺序参数框架

执行JDBC `?`参数的框架可以使用`task.parsed()`和`KronosAtomicBatchTask.parsedArr()`。

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

内置JDBC实现可以作为顺序参数执行的完整参考：[KronosJdbcWrapper.kt](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-jdbc-wrapper/src/main/kotlin/com/kotlinorm/wrappers/KronosJdbcWrapper.kt)。
