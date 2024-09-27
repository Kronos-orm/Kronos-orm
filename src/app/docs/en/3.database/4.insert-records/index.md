{% import "../../../macros/macros-en.njk" as $ %}
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

当主键为自增时，Kronos会自动获取自增主键的值。

```kotlin name="demo" icon="kotlin" {6}
val user: User = User(
        name = "Kronos",
        age = 18
    )
    
val (affectRows, lastInsertId) = user.insert().execute()
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
