{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 使用显式条件删除记录

在Kronos中，我们可以使用`KPojo.delete().execute()`方法用于删除数据库中的记录

空 `where()` 会读取当前 KPojo 对象中的可查询非空字段，并生成删除条件。`by { ... }` 用于选择参与匹配的对象字段，`where { ... }` 用于编写自定义条件。

> **Warning**
> `delete().execute()` 会删除当前表中所有符合框架策略的行。需要拒绝全表删除时，请启用 DataGuard。

写入标记列的 delete 行为见 {{ $.keyword("mutation/logic-delete", ["逻辑删除"]) }}。

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user.delete().where().execute()
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
WHERE [id] = :id and [name] = :name and [age] = :age
```

```sql group="Case 1" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "age" = :age
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

`where()` 按当前对象值生成 query-by-example 删除条件。`where { ... }` 使用 lambda 生成删除{{ $.keyword("query/conditions", ["Criteria条件语句"]) }}。

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
WHERE [id] = :id and [name] like :name and [age] > :ageMin
```

```sql group="Case 3" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" like :name
  and "age" > :ageMin
```

### 使用 IN 子查询筛选删除行

在`where`中使用`field in query`，可以删除另一条查询选中的行。

```kotlin group="Delete subquery 1" name="in" icon="kotlin"
User()
    .delete()
    .where {
        it.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 3 }
    }
    .execute()
```

```sql group="Delete subquery 1" name="in sql" icon="mysql"
DELETE FROM `user`
WHERE `user`.`id` IN (
    SELECT `user_id` AS `userId`
    FROM `order`
    WHERE `order`.`status` = :status
)
```

### 使用 EXISTS 筛选删除行

当关联行决定目标行是否删除时，可以使用`exists(query)`。子查询可以引用外层`where`接收者。

```kotlin group="Delete subquery 2" name="exists" icon="kotlin"
User()
    .delete()
    .where { user ->
        exists(
            Order()
                .select()
                .where { order -> order.userId == user.id && order.status == 0 }
        )
    }
    .execute()
```

```sql group="Delete subquery 2" name="exists sql" icon="mysql"
DELETE FROM `user`
WHERE EXISTS (
    SELECT `id`, `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`user_id` = `user`.`id`
      AND `order`.`status` = :status
)
```

更多谓词子查询写法见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

在显式`where`块中，可以使用`.eq`将当前 KPojo 对象的字段值展开为等值条件，并继续组合其他条件表达式：

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
  and [name] = :name
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

Kronos提供了减号运算符`-`，用于在显式`.eq`展开时排除指定字段。

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

逻辑删除的开启与字段设置设置请参考{{ $.keyword("configuration/global-config", ["全局设置", "逻辑删除策略"]) }}、{{ $.keyword("mapping/annotations", ["注解设置", "表逻辑删除"]) }}、{{ $.keyword("mapping/annotations", ["注解设置", "逻辑删除列"]) }}。

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

逻辑删除使用子查询条件时，Kronos 会保留子查询谓词，并追加逻辑删除谓词。

```kotlin group="Case 4-1" name="kotlin" icon="kotlin"
User()
    .delete()
    .logic()
    .where {
        it.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 5 }
    }
    .execute()
```

```sql group="Case 4-1" name="Mysql" icon="mysql"
UPDATE `user`
SET `deleted` = :deletedNew
WHERE `user`.`id` IN (
    SELECT `user_id` AS `userId`
    FROM `order`
    WHERE `order`.`status` = :status
)
AND `deleted` = 0
```

## 影响行数

`execute`方法返回的`KronosOperationResult`对象中包含了影响行数。

```kotlin group="Case 5" name="kotlin" icon="kotlin" {5}
val user: User = User(
    id = 1,
)

val (affectedRows) = user.delete().by { it.id }.execute()
```

## 批量删除记录

同一条 SQL 需要用多组参数执行时，见 {{ $.keyword("mutation/batch-operations", ["批量操作"]) }}。

## 指定使用的数据源

在Kronos中，我们可以将`KronosDataSourceWrapper`传入`execute`方法，以实现自定义的数据库连接。

```kotlin group="Case 7" name="kotlin" icon="kotlin" {7}
val customWrapper = CustomWrapper()

val user: User = User(
    id = 1,
)

user.delete().by { it.id }.execute(customWrapper)
```
