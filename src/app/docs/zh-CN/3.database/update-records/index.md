{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.title("set") }} 设置更新的字段和值

在Kronos中，我们可以使用`KPojo.update().set`方法设置需要更新的字段和值，然后使用`execute`方法执行更新操作。

当未使用`by`或`where`方法时，Kronos会根据KPojo对象的值生成更新条件语句。

> **Warning**
> 当KPojo对象的字段值为`null`时，该字段不会生成更新条件，若需要更新字段值为`null`的记录，请使用`where`方法指定。

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7-10}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .update()
  .set { it.name = "Kronos ORM" }
  .execute()
```

```sql group="Case 1" name="Mysql" icon="mysql"
UPDATE `user` SET `name` = :nameNew WHERE `id` = :id and `name` = :name and `age` = :age
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
UPDATE "user" SET "name" = :nameNew WHERE "id" = :id and "name" = :name and "age" = :age
```

```sql group="Case 1" name="SQLite" icon="sqlite"
UPDATE `user` SET `name` = :nameNew WHERE `id` = :id and `name` = :name and `age` = :age
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
UPDATE [user] SET [name] = :nameNew WHERE [id] = :id and [name] = :name and [age] = :age
```

```sql group="Case 1" name="Oracle" icon="oracle"
UPDATE "user" SET "name" = :nameNew WHERE "id" = :id and "name" = :name and "age" = :age
```

## {{ $.title("by") }} 设置更新条件

在Kronos中，我们可以使用`by`方法设置更新条件，此时Kronos会根据`by`方法设置的字段生成更新条件语句。

```kotlin group="Case 2" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .update()
  .set { it.name = "Kronos ORM" }
  .by { it.id }
  .execute()
```

```sql group="Case 2" name="Mysql" icon="mysql"
UPDATE `user` SET `name` = :nameNew WHERE `id` = :id
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
UPDATE "user" SET "name" = :nameNew WHERE "id" = :id
```

```sql group="Case 2" name="SQLite" icon="sqlite"
UPDATE `user` SET `name` = :nameNew WHERE `id` = :id
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
UPDATE [user] SET [name] = :nameNew WHERE [id] = :id
```

```sql group="Case 2" name="Oracle" icon="oracle"
UPDATE "user" SET "name" = :nameNew WHERE "id" = :id
```

## {{ $.title("where") }} 设置更新条件

在Kronos中，我们可以使用`where`方法设置更新条件，此时Kronos会根据`where`方法设置的字段生成更新条件语句。

```kotlin group="Case 3" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .update()
  .set { it.name = "Kronos ORM" }
  .where { it.id eq 1 }
  .execute()
```

```sql group="Case 3" name="Mysql" icon="mysql"
UPDATE `user` SET `name` = :nameNew WHERE `id` = :id
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
UPDATE "user" SET "name" = :nameNew WHERE "id" = :id
```

```sql group="Case 3" name="SQLite" icon="sqlite"
UPDATE `user` SET `name` = :nameNew WHERE `id` = :id
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
UPDATE [user] SET [name] = :nameNew WHERE [id] = :id
```

```sql group="Case 3" name="Oracle" icon="oracle"
UPDATE "user" SET "name" = :nameNew WHERE "id" = :id
```

## {{ $.title("patch") }}为自定义删除条件添加参数

在Kronos中，我们可以使用`patch`方法为自定义删除条件添加参数。

```kotlin group="Case 3-1" name="kotlin" icon="kotlin" {7-12}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
    .update()
    .set { it.name = "Kronos ORM" }
    .where { "id = :id" }
    .patch("id" to 1)
    .execute()
```

```sql group="Case 3-1" name="Mysql" icon="mysql"
UPDATE `user` SET `name` = :nameNew WHERE id = :id
```

```sql group="Case 3-1" name="PostgreSQL" icon="postgres"
UPDATE "user" SET "name" = :nameNew WHERE id = :id
```

```sql group="Case 3-1" name="SQLite" icon="sqlite"
UPDATE `user` SET `name` = :nameNew WHERE id = :id
```

```sql group="Case 3-1" name="SQLServer" icon="sqlserver"
UPDATE [user] SET [name] = :nameNew WHERE id = :id
```

```sql group="Case 3-1" name="Oracle" icon="oracle"
UPDATE "user" SET "name" = :nameNew WHERE id = :id
```

## {{ $.title("update") }} 设置需要更新的字段

在Kronos中，我们在`update`方法中设置需要更新的字段，未在`update`方法中设置的字段将不会被更新。

```kotlin group="Case 4" name="kotlin" icon="kotlin" {7-10}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .update { it.name + it.age }
  .where { it.id == 1 }
  .execute()
```

```sql group="Case 4" name="Mysql" icon="mysql"
UPDATE `user` SET `name` = :nameNew, `age` = :ageNew WHERE `id` = :id
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
UPDATE "user" SET "name" = :nameNew, "age" = :ageNew WHERE "id" = :id
```

```sql group="Case 4" name="SQLite" icon="sqlite"
UPDATE `user` SET `name` = :nameNew, `age` = :ageNew WHERE `id` = :id
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
UPDATE [user] SET [name] = :nameNew, [age] = :ageNew WHERE [id] = :id
```

```sql group="Case 4" name="Oracle" icon="oracle"
UPDATE "user" SET "name" = :nameNew, "age" = :ageNew WHERE "id" = :id
```

## {{ $.title("updateExcept") }} 设置排除的字段

在Kronos中，我们可以使用`updateExcept`方法设置需要排除的字段，未在`updateExcept`方法中设置的字段将会被更新。

```kotlin group="Case 5" name="kotlin" icon="kotlin" {7-10}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .updateExcept { it.id }
  .where { it.id == 1 }
  .execute()
```

```sql group="Case 5" name="Mysql" icon="mysql"
UPDATE `user` SET `name` = :nameNew, `age` = :ageNew WHERE `id` = :id
```

```sql group="Case 5" name="PostgreSQL" icon="postgres"
UPDATE "user" SET "name" = :nameNew, "age" = :ageNew WHERE "id" = :id
```

```sql group="Case 5" name="SQLite" icon="sqlite"
UPDATE `user` SET `name` = :nameNew, `age` = :ageNew WHERE `id` = :id
```

```sql group="Case 5" name="SQLServer" icon="sqlserver"
UPDATE [user] SET [name] = :nameNew, [age] = :ageNew WHERE [id] = :id
```

```sql group="Case 5" name="Oracle" icon="oracle"
UPDATE "user" SET "name" = :nameNew, "age" = :ageNew WHERE "id" = :id
```

## 影响行数

在Kronos中，我们可以使用`execute`方法执行更新操作，并获取更新操作的影响行数。

```kotlin name="demo" icon="kotlin" {7-10}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )
    
val (affectedRows) = user
                .update()
                .set { it.name = "Kronos ORM" }
                .execute()
```

## 批量更新记录

在Kronos中，我们可以使用`Iterable<KPojo>.update().execute()`或`Array<KPojo>.update().execute()`方法用于批量更新数据库中的记录。

```kotlin name="demo" icon="kotlin" {14-17}
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

users
  .update { it.name + it.age }
  .where { it.id eq 1 }
  .execute()
```

## 指定使用的数据源

在Kronos中，我们可以将`KronosDataSourceWrapper`传入`execute`方法，以实现自定义的数据库连接。

```kotlin name="demo" icon="kotlin" {9-12}
val customWrapper = CustomWrapper()

val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .update()
  .set { it.name = "Kronos ORM" }
  .execute(customWrapper)
```
