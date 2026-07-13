{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.title("set") }} 设置更新的字段和值

在Kronos中，我们可以使用`KPojo.update().set`方法设置需要更新的字段和值，然后使用`execute`方法执行更新操作。

使用 `where()` 或 `by { ... }` 指定目标行条件。空 `where()` 会按当前 KPojo 的可查询非空字段生成等值条件。

> **Warning**
> `update().execute()` 会更新当前表中所有符合框架策略的行。需要拒绝全表写入时，请启用 DataGuard。

生成 update 中追加的策略字段见 {{ $.keyword("mutation/logic-delete", ["逻辑删除"]) }} 和 {{ $.keyword("mutation/optimistic-lock", ["乐观锁"]) }}。

```kotlin group="Case 1" name="kotlin" icon="kotlin" {5-9}
val user: User = User(
    id = 1
)

user
    .update()
    .set { it.name = "Kronos ORM" }
    .where()
    .execute()

// 根据字符串动态设置赋值列：
//    .set { it["name"] = "Kronos ORM" }
```

```sql group="Case 1" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
```

```sql group="Case 1" name="SQLite" icon="sqlite"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [name] = :nameNew
WHERE [id] = :id
```

```sql group="Case 1" name="Oracle" icon="oracle"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
```

### {{ $.title("+=") }} {{ $.title("-=") }} 复合赋值运算符

Kronos支持语义化的`+=`和`-=`加减法赋值运算来更新数据库中的数值列

```kotlin group="Case 1-1" name="kotlin" icon="kotlin" {1-4}
user
    .update()
    .set { it.age += 2 }
    .where()
    .execute()

// 根据字符串动态设置赋值列：
//    .set { it["age"] += 2 }
```

```sql group="Case 1-1" name="Mysql" icon="mysql"
UPDATE `user`
SET `age` = `age` + :age2PlusNew
WHERE `id` = :id
```

```sql group="Case 1-1" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "age" = "age" + :age2PlusNew
WHERE "id" = :id
```

```sql group="Case 1-1" name="SQLite" icon="sqlite"
UPDATE `user`
SET `age` = `age` + :age2PlusNew
WHERE `id` = :id
```

```sql group="Case 1-1" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [age] = [age] + :age2PlusNew
WHERE [id] = :id
```

```sql group="Case 1-1" name="Oracle" icon="oracle"
UPDATE "user"
SET "age" = "age" + :age2PlusNew
WHERE "id" = :id
```

### 使用 Kotlin 表达式作为赋值

`set` 赋值右侧可以是普通运行时 Kotlin 表达式。Kronos 会把表达式结果绑定为本次更新值。

```kotlin group="Expression assignment" name="kotlin" icon="kotlin"
fun displayName(): String? = null

User(id = 1)
    .update()
    .set { it.name = displayName() ?: "匿名" }
    .where()
    .execute()
```

```sql group="Expression assignment" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
```

### 使用标量子查询赋值

当更新值来自另一条查询时，可以在`set`中使用标量子查询。子查询选择一列，并使用`limit(1)`限定单值。

```kotlin group="Scalar assignment" name="kotlin" icon="kotlin"
Order()
    .update()
    .set {
        it.status = (Order()
            .select { order -> order.status }
            .where { order -> order.userId == 44 }
            .limit(1) as Int?)
    }
    .where { it.id == 3 }
    .execute()
```

```sql group="Scalar assignment" name="Mysql" icon="mysql"
UPDATE `order`
SET `status` = (
    SELECT `status`
    FROM `order`
    WHERE `order`.`user_id` = :userId
    LIMIT 1
)
WHERE `order`.`id` = :id
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
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
```

```sql group="Case 2" name="SQLite" icon="sqlite"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [name] = :nameNew
WHERE [id] = :id
```

```sql group="Case 2" name="Oracle" icon="oracle"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
```

## {{ $.title("where") }} 设置更新条件

`where()` 按当前对象值生成 query-by-example 更新条件。`where { ... }` 使用 lambda 生成更新{{ $.keyword("query/conditions", ["Criteria条件语句"]) }}。

```kotlin group="Case 3" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user
    .update()
    .set { it.name = "Kronos ORM" }
    .where { it.id == 1 }
    .execute()
```

```sql group="Case 3" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
```

```sql group="Case 3" name="SQLite" icon="sqlite"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [name] = :nameNew
WHERE [id] = :id
```

```sql group="Case 3" name="Oracle" icon="oracle"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
```

### 使用 IN 子查询筛选更新行

在`where`中使用`field in query`，可以更新另一条查询选中的行。

```kotlin group="Update subquery" name="kotlin" icon="kotlin"
User(name = "active")
    .update { [it.name] }
    .where {
        it.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
    }
    .execute()
```

```sql group="Update subquery" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `user`.`id` IN (
    SELECT `user_id` AS `userId`
    FROM `order`
    WHERE `order`.`status` = :status
)
```

更多标量和谓词子查询写法见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

在显式`where`块中，可以使用`.eq`将当前 KPojo 对象的字段值展开为等值条件，并继续组合其他条件表达式：

```kotlin group="Case 3-1" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos"
)

user.update { it.name }.where { it.eq && it.status > 1 }.execute()
```

```sql group="Case 3-1" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
  and `name` = :name
  and `status` = :status
  and `status` > :statusMin
```

```sql group="Case 3-1" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
  and "name" = :name
  and "status" = :status
  and "status" > :statusMin
```

```sql group="Case 3-1" name="SQLite" icon="sqlite"
UPDATE `user`
SET `name` = :nameNew
WHERE "id" = :id
  and "name" = :name
  and "status" = :status
  and "status" > :statusMin
```

```sql group="Case 3-1" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [name] = :nameNew
WHERE [id] = :id
  and [name] = :name
  and [status] = :status
  and [status] > :statusMin
```

```sql group="Case 3-1" name="Oracle" icon="oracle"
UPDATE "user"
SET "name" = :nameNew
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

user.update { it.name }.where { (it - it.name).eq && it.status > 1 }.execute()
```

```sql group="Case 3-2" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
  and `status` > :statusMin
```

```sql group="Case 3-2" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
  and "status" > :statusMin
```

```sql group="Case 3-2" name="SQLite" icon="sqlite"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
  and "status" > :statusMin
```

```sql group="Case 3-2" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [name] = :nameNew
WHERE [id] = :id
  and [status] > :statusMin
```

```sql group="Case 3-2" name="Oracle" icon="oracle"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
  and "status" > :statusMin
```

## {{ $.title("patch") }}为自定义更新条件添加参数

在Kronos中，我们可以使用`patch`方法为自定义更新条件添加参数。

```kotlin group="Case 3-3" name="kotlin" icon="kotlin" {7-12}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user
    .update()
    .set { it.name = "Kronos ORM" }
    .where { "id = :id".asSql() }
    .patch("id" to 1)
    .execute()
```

```sql group="Case 3-3" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE id = :id
```

```sql group="Case 3-3" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "name" = :nameNew
WHERE id = :id
```

```sql group="Case 3-3" name="SQLite" icon="sqlite"
UPDATE `user`
SET `name` = :nameNew
WHERE id = :id
```

```sql group="Case 3-3" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [name] = :nameNew
WHERE id = :id
```

```sql group="Case 3-3" name="Oracle" icon="oracle"
UPDATE "user"
SET "name" = :nameNew
WHERE id = :id
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
    .update { [it.name, it.age] }
    .where { it.id == 1 }
    .execute()
```

```sql group="Case 4" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew,
    `age`  = :ageNew
WHERE `id` = :id
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "name" = :nameNew,
    "age"  = :ageNew
WHERE "id" = :id
```

```sql group="Case 4" name="SQLite" icon="sqlite"
UPDATE `user`
SET `name` = :nameNew,
    `age`  = :ageNew
WHERE `id` = :id
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [name] = :nameNew, [age] = :ageNew
WHERE [id] = :id
```

```sql group="Case 4" name="Oracle" icon="oracle"
UPDATE "user"
SET "name" = :nameNew,
    "age"  = :ageNew
WHERE "id" = :id
```

## {{ $.title("update") }} {{ $.title("-") }}设置排除的字段

在Kronos中，我们可以使用减号表达式`-`设置需要排除的字段。

```kotlin group="Case 5" name="kotlin" icon="kotlin" {7-10}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user
    .update { it - it.id }
    .where { it.id == 1 }
    .execute()
```

```sql group="Case 5" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew,
    `age`  = :ageNew
WHERE `id` = :id
```

```sql group="Case 5" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "name" = :nameNew,
    "age"  = :ageNew
WHERE "id" = :id
```

```sql group="Case 5" name="SQLite" icon="sqlite"
UPDATE `user`
SET `name` = :nameNew,
    `age`  = :ageNew
WHERE `id` = :id
```

```sql group="Case 5" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [name] = :nameNew, [age] = :ageNew
WHERE [id] = :id
```

```sql group="Case 5" name="Oracle" icon="oracle"
UPDATE "user"
SET "name" = :nameNew,
    "age"  = :ageNew
WHERE "id" = :id
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

同一条 SQL 需要用多组参数执行时，见 {{ $.keyword("mutation/batch-operations", ["批量操作"]) }}。

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
