{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Insert a single record

In Kronos, we can use the `KPojo.insert().execute()` method to insert a record into the database.

```kotlin name="demo" icon="kotlin" {7}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user.insert().execute()
```

## Last insert ID and affected rows

The `LastInsertId` plugin is used to obtain the ID (auto-increment primary key) of the last inserted record after adding a record.

`LastInsertId` is enabled by default, and you can disable it by setting `LastInsertIdPlugin.enabled = false` in the `Kronos.init` method.

If the `LastInsertId` plugin has been globally disabled, you need to call the `.withId()` method when inserting a record to declare that you want to obtain the last inserted ID for this insertion.

The `execute()` method returns a `KronosExecuteResult` object, which includes the last inserted ID and the number of affected rows.

Read more about the [LastInsertId plugin](/#/documentation/en/plugins/last-insert-id) here to learn more about how to use it.

This plugin is enabled by default, and you can disable it by setting `LastInsertIdPlugin.enabled = false` in the `Kronos.init` block.

```kotlin name="demo" icon="kotlin" {6,8}
val user: User = User(
        name = "Kronos",
        age = 18
    )
// If the plugin is enabled, return the last inserted ID
val result = user.insert().execute().lastInsertId
// If the plugin is disabled, you need to call the withId() method to inform Kronos that the insertion requires to query the lastInsertId
val result = user.insert().withId().execute().lastInsertId
val affectedRows = result.affectedRows
val lastInsertId = result.lastInsertId
```

## Batch insert records

In Kronos, we can use the `Iterable<KPojo>.insert().execute()` or `Array<KPojo>.insert().execute()` method to batch
insert records into the database.

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

## Specify the data source to use

In Kronos, we can pass `KronosDataSourceWrapper` into the `execute` method to achieve a custom database connection.

```kotlin name="demo" icon="kotlin" {9}
val customWrapper = CustomWrapper()

val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )
    
user.insert().execute(customWrapper)
```
