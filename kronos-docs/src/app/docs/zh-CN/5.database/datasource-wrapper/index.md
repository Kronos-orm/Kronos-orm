{% import "../../../macros/macros-zh-CN.njk" as $ %}

`KronosDataSourceWrapper`连接Kronos SQL task和数据库执行层。内置JDBC实现是`KronosJdbcWrapper`，自定义wrapper可以把执行委托给Spring JDBC、JDBI、MyBatis或项目中的数据访问层。

## 成员属性

{{ $.members([
    ['url', '数据库连接URL', 'String'],
    ['userName', 'JDBC元信息中的数据库用户名', 'String'],
    ['dbType', 'SQL渲染和DDL statement使用的数据库类型', 'DBType'],
    ['sqlDialect', '由dbType解析出的SQL方言', 'SqlDialect']
]) }}

创建wrapper后可以读取这些元信息。

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

## 配置内置JDBC wrapper

`KronosJdbcWrapper`接收`DataSource`、可选的`DBType`和配置block。当JDBC metadata名称需要指定到某个方言时，可以传入`databaseType`。

```kotlin group="Jdbc config 1" name="database type" icon="kotlin"
import com.kotlinorm.enums.DBType
import com.kotlinorm.wrappers.KronosJdbcWrapper

val wrapper = KronosJdbcWrapper(
    dataSource = dataSource,
    databaseType = DBType.Mysql
)
```

JDBC statement设置和SQL warning处理可以放在配置block里。

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

内置wrapper也提供自定义参数绑定和结果映射注册入口。

```kotlin group="Jdbc config 3" name="mapping" icon="kotlin"
import com.kotlinorm.wrappers.KronosArgument
import com.kotlinorm.wrappers.KronosArgumentFactory
import com.kotlinorm.wrappers.KronosJdbcWrapper
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

    columnMappers.register(UUID::class) { resultSet, position, _, _, _ ->
        resultSet.getString(position)?.let(UUID::fromString)
    }
}
```

## 使用{{ $.title("forList(task)") }}查询Map列表

`forList(task)`执行select task，并把每一行返回为`Map<String, Any>`。

```kotlin group="forList maps" name="signature" icon="kotlin"
fun forList(task: KAtomicQueryTask): List<Map<String, Any>>
```

```kotlin group="forList maps" name="kotlin" icon="kotlin"
val rows = wrapper.forList(
    KronosAtomicQueryTask(
        sql = "SELECT id, name FROM user WHERE age > :age",
        paramMap = mapOf("age" to 18)
    )
)
```

```text group="forList maps" name="result"
[
  {id=1, name=Ada},
  {id=2, name=Linus}
]
```

## 使用{{ $.title("forList(task, kClass, isKPojo, superTypes)") }}查询对象列表

类型化查询API会把目标类型和映射提示传给wrapper。

```kotlin group="forList objects" name="signature" icon="kotlin"
fun forList(
    task: KAtomicQueryTask,
    kClass: KClass<*>,
    isKPojo: Boolean,
    superTypes: List<String>
): List<Any>
```

```kotlin group="forList objects" name="kotlin" icon="kotlin"
val users = wrapper.forList(
    KronosAtomicQueryTask("SELECT id, name FROM user WHERE age > :age", mapOf("age" to 18)),
    User::class,
    isKPojo = true,
    superTypes = listOf("com.kotlinorm.interfaces.KPojo")
)
```

```text group="forList objects" name="result"
[User(id=1, name=Ada), User(id=2, name=Linus)]
```

## 使用{{ $.title("forMap(task)") }}查询单行Map

`forMap(task)`读取第一行，空结果返回`null`。

```kotlin group="forMap" name="signature" icon="kotlin"
fun forMap(task: KAtomicQueryTask): Map<String, Any>?
```

```kotlin group="forMap" name="kotlin" icon="kotlin"
val row = wrapper.forMap(
    KronosAtomicQueryTask(
        sql = "SELECT id, name FROM user WHERE id = :id",
        paramMap = mapOf("id" to 1)
    )
)
```

```text group="forMap" name="result"
{id=1, name=Ada}
```

## 使用{{ $.title("forObject(task, kClass, isKPojo, superTypes)") }}查询单个对象

`forObject(...)`把第一行映射为指定类型，空结果返回`null`。

```kotlin group="forObject" name="signature" icon="kotlin"
fun forObject(
    task: KAtomicQueryTask,
    kClass: KClass<*>,
    isKPojo: Boolean,
    superTypes: List<String>
): Any?
```

```kotlin group="forObject" name="kotlin" icon="kotlin"
val user = wrapper.forObject(
    KronosAtomicQueryTask("SELECT id, name FROM user WHERE id = :id", mapOf("id" to 1)),
    User::class,
    isKPojo = true,
    superTypes = listOf("com.kotlinorm.interfaces.KPojo")
)
```

```text group="forObject" name="result"
User(id=1, name=Ada)
```

## 使用{{ $.title("update(task)") }}执行写入

`update(task)`执行insert、update、delete和DDL task，并返回影响行数。

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

## 使用{{ $.title("batchUpdate(task)") }}执行批量写入

`batchUpdate(task)`用多组参数执行同一条SQL，并返回每个批次项的JDBC update count。

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

## 使用{{ $.title("transact") }}执行事务

`transact(...)`用`TransactionScope`作为block receiver。JDBC wrapper可以在block中使用`savepoint`、`rollbackToSavepoint`和`releaseSavepoint`。

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

## 转换命名参数

`task.parsed()`把`:name`参数转换为JDBC `?`占位符。`KronosAtomicBatchTask.parsedArr()`为每个批次项返回同样的结构。

```kotlin group="parsed" name="query task" icon="kotlin"
val task = KronosAtomicQueryTask(
    sql = "SELECT id, name FROM user WHERE id = :id AND status = :status",
    paramMap = mapOf("id" to 1, "status" to "ACTIVE")
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

Spring JDBC wrapper示例见{{ $.keyword("database/custom-wrapper", ["数据源及三方框架扩展"]) }}。
