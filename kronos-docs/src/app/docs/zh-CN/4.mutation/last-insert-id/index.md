{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 使用场景

当一次 insert 需要返回数据库生成的自增主键时，调用 `.withId()`。

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
> `lastInsertId` 适用于使用 `@PrimaryKey(identity = true)` 且主键值保持为 `null` 的插入。

## 读取 {{ $.title("lastInsertId") }}

在需要生成 ID 的那次 insert 上调用 `.withId()`。

```kotlin group="Read Id" name="kotlin" icon="kotlin"
val result = User(name = "Kronos")
    .insert()
    .withId()
    .execute()

val affectedRows = result.affectedRows
val lastInsertId = result.lastInsertId
```

`execute()` 返回 `KronosOperationResult`。`affectedRows` 是插入行数，`lastInsertId` 是 wrapper 返回或内部方言 fallback 读取到的自增主键值。

## 不返回 ID 的情况

没有 `.withId()` 时，Kronos 不会请求生成 ID。

```kotlin group="No Id" name="kotlin" icon="kotlin"
val result = User(name = "Kronos")
    .insert()
    .execute()

val lastInsertId = result.lastInsertId // null
```

如果主键值已经赋好，insert 会直接使用该值，`lastInsertId` 为空。

```kotlin group="Assigned Id" name="kotlin" icon="kotlin"
val result = User(id = 1001, name = "Kronos")
    .insert()
    .withId()
    .execute()

val lastInsertId = result.lastInsertId // null
```

## 方言 fallback SQL

内置 `KronosJdbcWrapper` 会优先在 insert 执行时读取 JDBC generated keys。若 wrapper 无法直接返回 generated keys，Kronos 会使用当前方言的后续查询 SQL。

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
SELECT MAX("ID") FROM "USER"
```
