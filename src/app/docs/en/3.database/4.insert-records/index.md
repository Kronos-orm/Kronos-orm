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

When the primary key is self-incrementing, Kronos automatically gets the value of the self-incrementing primary key.

```kotlin name="demo" icon="kotlin" {6}
val user: User = User(
        name = "Kronos",
        age = 18
    )
    
val (affectRows, lastInsertId) = user.insert().execute()
```

## Batch insert records

In Kronos, we can use the `Iterable<KPojo>.insert().execute()` or `Array<KPojo>.insert().execute()` method to batch insert records into the database.

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
