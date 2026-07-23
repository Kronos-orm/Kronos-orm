{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 为现有框架实现wrapper

`KronosDataSourceWrapper`让Kronos使用项目中已有的数据库执行层。wrapper接收Kronos SQL task，并把它们委托给框架的query、update、batch和transaction API。

完整接口说明见{{ $.keyword("database/datasource-wrapper", ["数据源包装器"]) }}。

## Android SQLite 参考实现

独立的 {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }} 章节提供 Android/JVM `SQLiteDatabase` 配置和完整 wrapper 参考实现。

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

## 委托查询

`KAtomicQueryTask.sql`使用命名参数，`paramMap`保存参数值，`targetType`保存完整的目标`KType`。自定义wrapper只需为Map、标量、KPojo和DTO实现一个列表方法和一个单行方法。

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
    Kronos.dataSource = { wrapper }
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
