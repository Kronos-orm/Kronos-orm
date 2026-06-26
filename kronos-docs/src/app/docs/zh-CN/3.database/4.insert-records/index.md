{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 插入单条记录

在Kronos中，我们可以使用`KPojo.insert().execute()`方法向数据库中插入一条记录。

```kotlin name="demo" icon="kotlin" {7}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user.insert().execute()
```

## 自增主键和影响行数

`LastInsertId` 插件用于在插入记录后获取最后插入的 ID(自增主键)。

`LastInsertId` 默认是开启的，可以通过在 `Kronos.init` 方法中设置 `LastInsertIdPlugin.enabled = false` 来禁用它。

若已经在全局禁用了`LastInsertId`插件，在插入记录时需要调用 `.withId()` 方法以声明本次插入需要获取最后插入的 ID。

`execute()` 方法返回一个 `KronosExecuteResult` 对象，其中包含最后插入的 ID 和受影响的行数。

在此处阅读更多关于 [LastInsertId 插件](/#/documentation/en/plugins/last-insert-id) 的信息，以了解更多关于如何使用它的内容。

此插件默认启用，您可以通过在 `Kronos.init` 方法中将 `LastInsertIdPlugin.enabled` 设置为 `false` 来禁用它。

```kotlin name="demo" icon="kotlin" {6,8}
val user: User = User(
        name = "Kronos",
        age = 18
    )
// 若插件未禁用，则返回最后插入的 ID
val result = user.insert().execute().lastInsertId
// 若插件被禁用，则需要调用 withId() 方法声明本次插入需要获取最后插入的 ID
val result = user.insert().withId().execute().lastInsertId
val affectedRows = result.affectedRows
val lastInsertId = result.lastInsertId
```

## 批量插入记录

在Kronos中，我们可以使用`Iterable<KPojo>.insert().execute()`或`Array<KPojo>.insert().execute()`方法向数据库中批量插入记录。

```kotlin name="demo" icon="kotlin" {14}
val users: List<User> = listOf(
    User(
        id = 1,
        name = "Kronos",
        age = 18
    ),
    User(
        id = 2,
        name = "Kronos ORM",
        age = 18
    )
)

users.insert().execute()
```

## 指定使用的数据源
在Kronos中，我们可以将`KronosDataSourceWrapper`传入`execute`方法，以实现自定义的数据库连接。

```kotlin name="demo" icon="kotlin" {9}
val customWrapper = CustomWrapper()

val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )
    
user.insert().execute(customWrapper)
```
