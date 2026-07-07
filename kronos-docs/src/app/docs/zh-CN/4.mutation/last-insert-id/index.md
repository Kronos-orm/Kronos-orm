{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 使用场景

`LastInsertIdPlugin` 用于在自增主键插入后读取数据库生成的 ID。

```kotlin group="KPojo" name="User.kt" icon="kotlin"
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.interfaces.KPojo

data class User(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null
) : KPojo
```

> **Note**
> `lastInsertId` 适用于使用 `@PrimaryKey(identity = true)` 且主键值由数据库生成的插入操作。

## 从插入结果读取 {{ $.title("lastInsertId") }}

需要让所有自增主键插入都读取生成 ID 时，启用 `LastInsertIdPlugin`。

```kotlin group="Read Id" name="kotlin" icon="kotlin"
import com.kotlinorm.plugins.LastInsertIdPlugin
import com.kotlinorm.plugins.LastInsertIdPlugin.lastInsertId

LastInsertIdPlugin.enabled = true

val result = User(name = "Kronos")
    .insert()
    .execute()

val affectedRows = result.affectedRows
val lastInsertId = result.lastInsertId
```

`execute()` 返回 `KronosOperationResult`。`affectedRows` 来自插入执行结果，`lastInsertId` 来自操作结果暂存区。使用内置 `KronosJdbcWrapper` 时，Kronos 会在执行 insert 时读取 JDBC generated keys。

## 禁用{{ $.title("lastInsertId") }}插件

应用默认不需要收集自增 ID 时，直接设置 `LastInsertIdPlugin.enabled`。

```kotlin group="Disable" name="kotlin" icon="kotlin"
import com.kotlinorm.plugins.LastInsertIdPlugin

LastInsertIdPlugin.enabled = false
```

单次插入仍然需要读取生成 ID 时，调用 `.withId()`。

```kotlin group="Single Insert" name="kotlin" icon="kotlin"
import com.kotlinorm.plugins.LastInsertIdPlugin.lastInsertId
import com.kotlinorm.plugins.LastInsertIdPlugin.withId

val result = User(name = "Kronos")
    .insert()
    .withId()
    .execute()

val lastInsertId = result.lastInsertId
```

## wrapper 回退路径使用的方言 SQL

通过后续查询读取生成 ID 的 wrapper 会使用当前数据源方言。

```sql group="Dialect SQL" name="Mysql" icon="mysql"
SELECT LAST_INSERT_ID()
```

```sql group="Dialect SQL" name="PostgreSQL" icon="postgres"
SELECT LASTVAL()
```

```sql group="Dialect SQL" name="SQLite" icon="sqlite"
SELECT last_insert_rowid()
```

```sql group="Dialect SQL" name="SQLServer" icon="sqlserver"
SELECT SCOPE_IDENTITY()
```

```sql group="Dialect SQL" name="Oracle" icon="oracle"
SELECT * FROM DUAL
```
