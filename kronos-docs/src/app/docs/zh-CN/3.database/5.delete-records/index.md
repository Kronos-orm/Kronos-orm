{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 根据KPojo对象值自动生成条件语句并删除记录

在Kronos中，我们可以使用`KPojo.delete().execute()`方法用于删除数据库中的记录

**当未使用`by`或`where`方法时，Kronos会根据KPojo对象的值生成删除条件语句。**

> **Warning**
> 当KPojo对象的字段值为`null`时，该字段不会生成删除条件，若需要删除字段值为`null`的记录，请使用`where`方法指定。

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user.delete().execute()
// 等同于
// user.delete().by { it.id  + it.name + it.age }.execute()
// 或
// user.delete().where { it.eq }.execute()
// 或
// user.delete().where { it.id.eq && it.name.eq && it.age.eq }.execute()
```

```sql group="Case 1" name="Mysql" icon="mysql"
DELETE
FROM `user`
WHERE `id` = :id
  and `name` = :name
  and `age` = :age
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "age" = :age
```

```sql group="Case 1" name="SQLite" icon="sqlite"
DELETE
FROM `user`
WHERE `id` = :id
  and `name` = :name
  and `age` = :age
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
DELETE
FROM [user]
WHERE [id] = :id and [name] = : name and [age] = :age
```

```sql group="Case 1" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
```

## {{ $.title("by") }} 设置删除条件

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
DELETE
FROM `user`
WHERE `id` = :id
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
DELETE
FROM "user"
WHERE "id" = :id
```

```sql group="Case 2" name="SQLite" icon="sqlite"
DELETE
FROM "user"
WHERE "id" = :id
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
DELETE
FROM [user]
WHERE [id] = :id
```

```sql group="Case 2" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
```

## {{ $.title("where") }} 设置删除条件

在Kronos中，我们可以使用`where`方法设置删除条件，此时Kronos会根据`where`方法设置的条件生成删除{{ $.keyword("concept/where-having-on-clause", ["Criteria条件语句"]) }}。

```kotlin group="Case 3" name="kotlin" icon="kotlin" {5}
val user: User = User(
    id = 1,
)

user.delete().where { it.id.eq && it.name like "Kronos%" and it.age > 18 }.execute()
```

```sql group="Case 3" name="Mysql" icon="mysql"
DELETE
FROM `user`
WHERE `id` = :id
  and `name` like :name
  and `age` > :ageMin
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" like :name
  and "age" > :ageMin
```

```sql group="Case 3" name="SQLite" icon="sqlite"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" like :name
  and "age" > :ageMin
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
DELETE
FROM [user]
WHERE [id] = :id and [name] like : name and [age] > :ageMin
```

```sql group="Case 3" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" like :name
  and "age" > :ageMin
```

可以对查询对象执行`.eq`函数，这样您可以以根据KPojo对象值生成条件语句为基础，添加其他查询条件:

```kotlin group="Case 3-1" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos"
)

user.delete().where { it.eq && it.status > 1 }.execute()
```

```sql group="Case 3-1" name="Mysql" icon="mysql"
DELETE
FROM `user`
WHERE `id` = :id
  and `name` = :name
  and `status` = :status
  and `status` > :statusMin
```

```sql group="Case 3-1" name="PostgreSQL" icon="postgres"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "status" = :status
  and "status" > :statusMin
```

```sql group="Case 3-1" name="SQLite" icon="sqlite"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "status" = :status
  and "status" > :statusMin
```

```sql group="Case 3-1" name="SQLServer" icon="sqlserver"
DELETE
FROM [user]
WHERE [id] = :id 
  and [name] = : name
  and [status] = :status
  and [status] > :statusMin
```

```sql group="Case 3-1" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "status" = :status
  and "status" > :statusMin
```

Kronos提供了减号运算符`-`用来指定不需要自动生成条件语句的列。

```kotlin group="Case 3-2" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos"
)

user.delete().where { (it - it.name).eq && it.status > 1 }.execute()
```

```sql group="Case 3-2" name="Mysql" icon="mysql"
DELETE
FROM `user`
WHERE `id` = :id
  and `status` > :statusMin
```

```sql group="Case 3-2" name="PostgreSQL" icon="postgres"
DELETE
FROM "user"
WHERE "id" = :id
  and "status" > :statusMin
```

```sql group="Case 3-2" name="SQLite" icon="sqlite"
DELETE
FROM "user"
WHERE "id" = :id
  and "status" > :statusMin
```

```sql group="Case 3-2" name="SQLServer" icon="sqlserver"
DELETE
FROM [user]
WHERE [id] = :id
  and [status] > :statusMin
```

```sql group="Case 3-2" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
  and "status" > :statusMin
```

## {{ $.title("patch") }}为自定义删除条件添加参数

在Kronos中，我们可以使用`patch`方法为自定义删除条件添加参数。

```kotlin group="Case 3-3" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos"
)

user.delete().where { it.eq && "status = :status".asSql() }.patch("status" to 1).execute()
```

```sql group="Case 3-3" name="Mysql" icon="mysql"
DELETE
FROM `user`
WHERE `id` = :id
  and status = :status
```

```sql group="Case 3-3" name="PostgreSQL" icon="postgres"
DELETE
FROM "user"
WHERE "id" = :id
  and status = :status
```

```sql group="Case 3-3" name="SQLite" icon="sqlite"
DELETE
FROM `user`
WHERE `id` = :id
  and status = :status
```

```sql group="Case 3-3" name="SQLServer" icon="sqlserver"
DELETE
FROM [user]
WHERE [id] = :id
  and status = :status
```

```sql group="Case 3-3" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
  and status = :status
```

## {{ $.title("logic") }} 逻辑删除

在Kronos中，我们可以使用`logic`方法设置逻辑删除，此时Kronos会生成逻辑删除的SQL语句。

逻辑删除的开启与字段设置设置请参考{{ $.keyword("getting-started/global-config", ["全局设置", "逻辑删除策略"]) }}、{{
$.keyword("class-definition/annotation-config", ["注解设置", "表逻辑删除"]) }}、{{
$.keyword("class-definition/annotation-config", ["注解设置", "逻辑删除列"]) }}。

```kotlin group="Case 4" name="kotlin" icon="kotlin" {5}
val user: User = User(
    id = 1,
)

user.delete().by { it.id }.logic().execute()
```

```sql group="Case 4" name="Mysql" icon="mysql"
UPDATE `user`
SET `deleted` = 1
WHERE `id` = :id
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "deleted" = 1
WHERE "id" = :id
```

```sql group="Case 4" name="SQLite" icon="sqlite"
UPDATE `user`
SET `deleted` = 1
WHERE `id` = :id
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [deleted] = 1
WHERE [id] = :id
```

```sql group="Case 4" name="Oracle" icon="oracle"
UPDATE "user"
SET "deleted" = 1
WHERE "id" = :id
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
