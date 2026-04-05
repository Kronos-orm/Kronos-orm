{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 事务

在Kronos中，事务是一个重要的概念，它允许你将多个数据库操作组合成一个原子操作。也就是说，要么所有操作都成功执行，要么都不执行。这对于确保数据的一致性和完整性非常重要。

## {{ $.title("Kronos.transact")}}事务的使用

Kronos的事务使用非常简单。你只需要在你的代码中使用`Kronos.transact`方法来开始一个事务，并在事务块中执行你的数据库操作。

```kotlin
import com.kotlinorm.Kronos.transact

transact {
    // 在这里执行数据库操作，使用默认数据源
}

transact(dataSourceWrapper) {
    // 在这里执行数据库操作，使用指定的数据源
}
```

transact方法的返回值为`Any?`类型，表示事务的执行结果
为你带有返回值的函数添加事务包裹，你可以这样做：

```
// before
fun doRequest(): String {
    // do something
    return "result"
}
// after
fun doRequest(): String {
    return transact {
        // do something
        "result"
        // or return@transact "result"
    } as String
}
```

在事务块中，你可以执行任何数据库操作，比如插入、更新、删除等。如果在事务块中发生了异常，Kronos会自动回滚事务，确保数据的一致性。
如果事务块中的所有操作都成功执行，Kronos会自动提交事务。
这意味着所有的操作都会被永久保存到数据库中。

> **Note**
> 在使用事务时，请保证你使用的KronosDataSourceWrapper和你的数据库支持事务操作。

## 事务的嵌套

Kronos支持事务的嵌套。这意味着你可以在一个事务块中再开始一个事务块。Kronos会自动管理嵌套事务的提交和回滚。

```kotlin
Kronos.transact {
    // 在这里执行数据库操作
    Kronos.transact {
        // 在这里执行数据库操作
    }
}
```

在上面的代码中，外层事务块会在内层事务块成功执行后提交。如果内层事务块发生了异常，Kronos会自动回滚外层事务块。

## 事务的隔离级别(WIP)

Kronos支持多种事务隔离级别，包括：

- READ_UNCOMMITTED
- READ_COMMITTED
- REPEATABLE_READ
- SERIALIZABLE

你可以在创建KronosDataSourceWrapper时指定事务隔离级别。

```kotlin
val dataSource = KronosDataSourceWrapper().apply {
    isolationLevel = IsolationLevel.READ_COMMITTED
}
```

## 事务的超时时间(WIP)

Kronos支持设置事务的超时时间。你可以在创建KronosDataSourceWrapper时指定事务超时时间。

```kotlin
val dataSource = KronosDataSourceWrapper().apply {
    transactionTimeout = 30 // 设置事务超时时间为30秒
}
```

## 事务的保存点(WIP)

Kronos支持事务的保存点。你可以在事务块中使用`savepoint`方法来设置保存点，然后在事务块中使用`rollbackToSavepoint`方法来回滚到保存点。

```kotlin
Kronos.transact {
    // 在这里执行数据库操作
    val savepoint = savepoint("savepoint1")
    // 在这里执行数据库操作
    rollbackToSavepoint(savepoint)
}
```
