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

`execute()` 返回 `KronosOperationResult`。

```kotlin name="demo" icon="kotlin" {6,8,9}
val user: User = User(
    name = "Kronos",
    age = 18
)

val result = user.insert().withId().execute()
val affectedRows = result.affectedRows
val lastInsertId = result.lastInsertId
```

`.withId()` 表示本次插入需要读取数据库生成的 ID。目标 KPojo 需要使用自增主键。

更多用法见 {{ $.keyword("mutation/last-insert-id", ["生成的自增主键 ID"]) }}。

## 批量插入记录

同一条 SQL 需要用多组参数执行时，见 {{ $.keyword("mutation/batch-operations", ["批量操作"]) }}。

## INSERT SELECT

使用 `KSelectable<Selected>.insert<Target>()` 可以把查询结果插入目标表。没有显式 values 时，Kronos 会把来源 select 列表写入目标可插入字段。

```kotlin group="INSERT SELECT default 1" name="kotlin" icon="kotlin"
Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderArchive>()
    .execute()
```

```sql group="INSERT SELECT default 1" name="Mysql" icon="mysql"
INSERT INTO `order_archive` (`id`, `user_id`, `status`)
SELECT `id`, `user_id` AS `userId`, `status`
FROM `order`
WHERE `order`.`status` = :status
```

`execute()` 返回 `KronosOperationResult`。对于 INSERT SELECT，`affectedRows` 是生成语句插入的行数。

```kotlin group="INSERT SELECT default 2" name="result" icon="kotlin"
val result = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderArchive>()
    .execute()

val insertedRows = result.affectedRows
```

没有显式 values 时，来源 select 列表需要匹配目标可插入字段数。

```kotlin group="INSERT SELECT default 3" name="column count" icon="kotlin"
Order()
    .select { it.id }
    .insert<OrderArchive>()
    .build()
```

```text group="INSERT SELECT default 3" name="error"
Insert-select source column count (1) must match target insertable field count (3).
```

当写入值来自来源表达式或标量子查询时，可以使用显式 values。

```kotlin group="INSERT SELECT values" name="kotlin" icon="kotlin"
Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderArchive> {
        [
            it.id,
            it.userId + 1,
            Order()
                .select { order -> order.status }
                .where { order -> order.userId == 38 }
                .limit(1)
        ]
    }
    .execute()
```

```sql group="INSERT SELECT values" name="Mysql" icon="mysql"
INSERT INTO `order_archive` (`id`, `user_id`, `status`)
SELECT `id`,
       (`user_id` + 1),
       (
           SELECT `status`
           FROM `order`
           WHERE `order`.`user_id` = :userId
           LIMIT 1
       )
FROM `order`
WHERE `order`.`status` = :status
```

来源也可以是派生 selectable。目标字段顺序仍来自目标 KPojo 的可插入字段。

```kotlin group="INSERT SELECT source" name="derived source" icon="kotlin"
val paidOrders = Order()
    .where { it.status == 1 }
    .select { [it.id, it.userId, it.status] }
    .where { it.userId == 42 }

paidOrders
    .insert<OrderArchive>()
    .execute()
```

```sql group="INSERT SELECT source" name="Mysql" icon="mysql"
INSERT INTO `order_archive` (`id`, `user_id`, `status`)
SELECT `q`.`id`, `q`.`user_id` AS `userId`, `q`.`status`
FROM (
    SELECT `id`, `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`status` = :status
) AS `q`
WHERE `q`.`user_id` = :userId
```

目标 KPojo 使用自增主键时，Kronos 会从目标字段列表排除该字段。

```kotlin group="INSERT SELECT identity" name="kotlin" icon="kotlin"
Order()
    .select { [it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderIdentityArchive>()
    .execute()
```

```sql group="INSERT SELECT identity" name="Mysql" icon="mysql"
INSERT INTO `order_identity_archive` (`user_id`, `status`)
SELECT `user_id` AS `userId`, `status`
FROM `order`
WHERE `order`.`status` = :status
```

join source、union source、标量子查询规则和投影细节见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

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
