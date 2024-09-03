# {{ NgDocPage.title }}

## 根据KPojo对象值生成条件语句并删除记录

在Kronos中，我们可以使用`KPojo.delete().execute()`方法用于删除数据库中的记录

当未使用`by`或`where`方法时，Kronos会根据KPojo对象的值生成删除条件语句。

> **Warning**
> `null`值不会被包含在删除条件中，即`null`值不会被用于生成删除条件语句，若需要删除`null`值，请使用`where`方法设置删除条件。

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user.delete().execute()
```

```sql group="Case 1" name="Mysql" icon="mysql"
DELETE FROM `user` WHERE `id` = :id and `name` = :name and `age` = :age
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
DELETE FROM "user" WHERE "id" = :id and "name" = :name and "age" = :age
```

```sql group="Case 1" name="SQLite" icon="sqlite"
DELETE FROM `user` WHERE `id` = :id and `name` = :name and `age` = :age
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
DELETE FROM [user] WHERE [id] = :id and [name] = :name and [age] = :age
```

```sql group="Case 1" name="Oracle" icon="oracle"
DELETE FROM "user" WHERE "id" = :id
```

## 使用<span style="color: #DD6666">by</span>设置删除条件

在Kronos中，我们可以使用`by`方法设置删除条件，此时Kronos会根据`by`方法设置的字段生成删除条件语句。

```kotlin group="Case 2" name="kotlin" icon="kotlin" {7}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user.delete().by { it.id }.execute()
```

```sql group="Case 2" name="Mysql" icon="mysql"
DELETE FROM `user` WHERE `id` = :id
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
DELETE FROM "user" WHERE "id" = :id
```

```sql group="Case 2" name="SQLite" icon="sqlite"
DELETE FROM `user` WHERE `id` = :id
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
DELETE FROM [user] WHERE [id] = :id
```

```sql group="Case 2" name="Oracle" icon="oracle"
DELETE FROM "user" WHERE "id" = :id
```

## 使用<span style="color: #DD6666">where</span>设置删除条件

在Kronos中，我们可以使用`where`方法设置删除条件，此时Kronos会根据`where`方法设置的条件生成删除条件语句。

```kotlin group="Case 3" name="kotlin" icon="kotlin" {5}
val user: User = User(
        id = 1,
    )

user.delete().where { it.id.eq && it.name like "Kronos%" and it.age > 18 }.execute()
```

```sql group="Case 3" name="Mysql" icon="mysql"
DELETE FROM `user` WHERE `id` = :id and `name` like :name and `age` > :ageMin
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
DELETE FROM "user" WHERE "id" = :id and "name" like :name and "age" > :ageMin
```

```sql group="Case 3" name="SQLite" icon="sqlite"
DELETE FROM `user` WHERE `id` = :id and `name` like :name and `age` > :ageMin
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
DELETE FROM [user] WHERE [id] = :id and [name] like :name and [age] > :ageMin
```

```sql group="Case 3" name="Oracle" icon="oracle"
DELETE FROM "user" WHERE "id" = :id and "name" like :name and "age" > :ageMin
```

## 逻辑删除

在Kronos中，我们可以使用`logic`方法设置逻辑删除，此时Kronos会生成逻辑删除的SQL语句。

逻辑删除的开启与字段设置设置请参考 [逻辑删除策略](/documentation/en/class-definition/table-class-definition#逻辑删除策略)及[表逻辑删除](/documentation/class-definition/table-class-definition#表逻辑删除)
  
```kotlin group="Case 4" name="kotlin" icon="kotlin" {5}
val user: User = User(
        id = 1,
    )

user.delete().by { it.id }.logic().execute()
```

```sql group="Case 4" name="Mysql" icon="mysql"
UPDATE `user` SET `deleted` = 1 WHERE `id` = :id
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
UPDATE "user" SET "deleted" = 1 WHERE "id" = :id
```

```sql group="Case 4" name="SQLite" icon="sqlite"
UPDATE `user` SET `deleted` = 1 WHERE `id` = :id
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
UPDATE [user] SET [deleted] = 1 WHERE [id] = :id
```

```sql group="Case 4" name="Oracle" icon="oracle"
UPDATE "user" SET "deleted" = 1 WHERE "id" = :id
```

## 影响行数

`execute`方法返回的`KronosOperationResult`对象中包含了影响行数。

```kotlin group="Case 5" name="kotlin" icon="kotlin" {5}
val user: User = User(
        id = 1,
    )
    
val (affectedRows) = user.delete().execute()
```

## 批量删除记录
在Kronos中，我们可以使用`Iterable<KPojo>.delete().execute()`或`Array<KPojo>.delete().execute()`方法删除数据库中的记录。

```kotlin group="Case 6" name="kotlin" icon="kotlin" {10}
val users: List<User> = listOf(
    User(
        id = 1,
    ),
    User(
        id = 2,
    )
)

users.delete().execute()
```

## 指定使用的数据源
在Kronos中，我们可以将`KronosDataSourceWrapper`传入`execute`方法，以实现自定义的数据库连接。

```kotlin group="Case 7" name="kotlin" icon="kotlin" {7}
val customWrapper = CustomWrapper()

val user: User = User(
        id = 1,
    )

user.delete().execute(customWrapper)
```