{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 事务

使用 `Kronos.transact` 可以把多次数据库操作放入同一个事务。block 返回最后一个表达式，正常结束时提交，异常离开 block 时回滚。

## {{ $.title("Kronos.transact")}}事务入口

不传 wrapper 时，`Kronos.transact` 使用默认的 `Kronos.dataSource`。需要指定数据源时，把 `KronosDataSourceWrapper` 作为第一个参数传入。

```kotlin group="Transaction 1" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos.transact

val result = transact {
    User(name = "Ada").insert().execute()
    User(id = 1).update().set { it.name = "Ada Lovelace" }.where().execute()
    "committed"
}

val customResult = transact(wrapper) {
    User(name = "Grace").insert().execute()
    "committed on wrapper"
}
```

```kotlin group="Transaction 1" name="signature" icon="kotlin"
fun transact(
    wrapper: KronosDataSourceWrapper? = null,
    isolation: TransactionIsolation? = null,
    timeout: Int? = null,
    block: TransactionScope.() -> Any?
): Any?
```

需要返回类型化结果时，直接从 block 返回值。

```kotlin group="Transaction 2" name="return value" icon="kotlin"
fun createUser(): String {
    return transact {
        User(name = "Ada").insert().execute()
        "ok"
    } as String
}
```

## 异常时回滚

block 抛出异常时，JDBC wrapper 会回滚事务，并继续抛出该异常。

```kotlin group="Rollback" name="kotlin" icon="kotlin"
Kronos.transact {
    User(name = "Ada").insert().execute()
    error("stop this transaction")
}
```

## 嵌套事务复用连接

`KronosJdbcWrapper` 上的嵌套 `Kronos.transact` 会复用当前事务连接。最终提交或回滚由外层事务负责。

```kotlin group="Nested" name="kotlin" icon="kotlin"
Kronos.transact {
    User(name = "Ada").insert().execute()

    Kronos.transact {
        User(id = 1).update().set { it.name = "Ada Lovelace" }.where().execute()
    }
}
```

## 隔离级别和超时

通过 `TransactionIsolation` 和 `timeout` 配置事务。`timeout` 的单位是秒。

```kotlin group="Options" name="kotlin" icon="kotlin"
import com.kotlinorm.enums.TransactionIsolation

Kronos.transact(
    isolation = TransactionIsolation.READ_COMMITTED,
    timeout = 30
) {
    User(name = "Ada").insert().execute()
}
```

可用隔离级别包括 `READ_UNCOMMITTED`、`READ_COMMITTED`、`REPEATABLE_READ` 和 `SERIALIZABLE`。

## 保存点

`TransactionScope` 是事务 block 的 receiver。JDBC wrapper 可以把当前事务连接传给 `TransactionScope`，从而使用 `savepoint`、`rollbackToSavepoint` 和 `releaseSavepoint`。

```kotlin group="Savepoint" name="kotlin" icon="kotlin"
Kronos.transact {
    User(name = "Ada").insert().execute()
    val point = savepoint("before_status_update")

    try {
        User(id = 1).update().set { it.status = "ACTIVE" }.where().execute()
        releaseSavepoint(point)
    } catch (e: Exception) {
        rollbackToSavepoint(point)
        throw e
    }
}
```

> **Note**
> 保存点需要 `TransactionScope` 持有 JDBC connection。`KronosJdbcWrapper` 会提供该 connection；自定义 wrapper 执行 block 时也应传入自己的事务 connection。
