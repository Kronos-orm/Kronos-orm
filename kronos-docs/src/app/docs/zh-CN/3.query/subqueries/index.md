{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos 子查询由普通 `select` DSL 组成。查询结果可以作为标量值、谓词来源、派生表、可排序或分页的来源、INSERT SELECT 来源，也可以作为 CREATE TABLE AS SELECT 的来源。

字段投影、alias、join 投影、生成结果形态和 DTO 消费方式的集中说明见 {{ $.keyword("query/projection", ["投影"]) }}。
函数入口、聚合函数、字符串函数、数学函数和窗口函数语法见 {{ $.keyword("query/functions", ["内置函数"]) }}。

## 生成投影

`select { ... }` 返回自定义字段列表时，Kronos 会把选中的字段暴露为生成投影类型。这个类型可以像一个结果对象一样使用，属性来自选中字段和 alias。

```kotlin group="Generated projection 1" name="kotlin" icon="kotlin"
val nameLengths = User()
    .select {
        [
            it.id,
            f.length(it.name).alias("nameLength")
        ]
    }

val rows = nameLengths.queryList()
```

返回的投影大致按下面的形态使用：

```kotlin group="Generated projection 2" name="projection shape" icon="kotlin"
data class UserNameLengthProjection(
    val id: Int?,
    val nameLength: Int?
)
```

生成 SQL 时，表达式会使用 alias 作为列名：

```sql group="Generated projection 3" name="Mysql" icon="mysql"
SELECT `id`, LENGTH(`name`) AS `nameLength`
FROM `user`
```

函数、聚合、标量子查询、窗口函数等非直接字段需要使用 `.alias("name")` 命名。alias 会成为投影属性名。

需要在 Map 结果、生成投影行和命名 DTO 行之间选择时，见 {{ $.keyword("query/projection", ["投影"]) }}。

## 把投影作为下一层查询源

`KSelectable<Selected>.select { ... }` 会进入下一层查询。上一层的 `Selected` 投影会成为新的 source 类型。

```kotlin group="Derived source" name="kotlin" icon="kotlin"
val activeUsers = User()
    .where { it.status == 1 }
    .select { [it.id, it.name] }

val rows = activeUsers
    .select { [it.id, it.name] }
    .where { it.name like "A%" }
    .queryList()
```

```sql group="Derived source" name="Mysql" icon="mysql"
SELECT `q`.`id`, `q`.`name`
FROM (
    SELECT `id`, `name`
    FROM `user`
    WHERE `user`.`status` = :status
) AS `q`
WHERE `q`.`name` LIKE :name
```

当 selected alias 需要参与 `where`、`groupBy` 或 `having` 时，可以使用这个写法。

## 查询标量子查询

标量子查询返回一个值。它只选择一列，并使用 `limit(1)`。

```kotlin group="Scalar subquery 1" name="kotlin" icon="kotlin"
val users = User()
    .select { user ->
        [
            user.id,
            user.name,
            Order()
                .select { order -> order.status }
                .where { order -> order.userId == user.id }
                .limit(1)
                .alias("lastOrderStatus")
        ]
    }
    .queryList()
```

```sql group="Scalar subquery 1" name="Mysql" icon="mysql"
SELECT `id`,
       `name`,
       (
           SELECT `status`
           FROM `order`
           WHERE `order`.`user_id` = `user`.`id`
           LIMIT 1
       ) AS `lastOrderStatus`
FROM `user`
```

当 Kotlin 需要类型提示时，可以在 `limit(1)` 后添加 cast。这个 cast 只用于 DSL 类型推断。

```kotlin group="Scalar subquery 2" name="type hint" icon="kotlin"
User()
    .select()
    .where {
        it.id > (Order()
            .select { order -> order.userId }
            .where { order -> order.status == 3 }
            .limit(1) as Int?)
    }
```

## 用标量子查询比较

标量子查询可以放在 `where` 比较表达式中。

```kotlin group="Scalar comparison" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where {
        it.id > Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
            .limit(1)
    }
    .queryList()
```

```sql group="Scalar comparison" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `user`.`id` > (
    SELECT `user_id` AS `userId`
    FROM `order`
    WHERE `order`.`status` = :status
    LIMIT 1
)
```

## 使用 IN 和 NOT IN 过滤

子查询返回一列时，可以使用 `field in query`。

```kotlin group="IN subquery 1" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where {
        it.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
    }
    .queryList()
```

```sql group="IN subquery 1" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `user`.`id` IN (
    SELECT `user_id` AS `userId`
    FROM `order`
    WHERE `order`.`status` = :status
)
```

`!in` 用于生成 `NOT IN`。

```kotlin group="IN subquery 2" name="not in" icon="kotlin"
val users = User()
    .select()
    .where {
        it.id !in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 4 }
    }
    .queryList()
```

## 使用 EXISTS 和 NOT EXISTS 过滤

需要判断相关记录是否存在时，可以使用 `exists(query)`。子查询可以引用外层查询的字段。

```kotlin group="EXISTS subquery 1" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { user ->
        exists(
            Order()
                .select()
                .where { order -> order.userId == user.id && order.status == 1 }
        )
    }
    .queryList()
```

```sql group="EXISTS subquery 1" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE EXISTS (
    SELECT `id`, `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`user_id` = `user`.`id`
      AND `order`.`status` = :status
)
```

`!exists(query)` 用于生成 `NOT EXISTS`。

```kotlin group="EXISTS subquery 2" name="not exists" icon="kotlin"
val users = User()
    .select()
    .where { user ->
        !exists(
            Order()
                .select()
                .where { order -> order.userId == user.id }
        )
    }
    .queryList()
```

## 使用 ANY、SOME 和 ALL 比较

`any<T>(query)`、`some<T>(query)`、`all<T>(query)` 可以和比较运算符组合使用。

```kotlin group="Quantified subquery 1" name="any" icon="kotlin"
val orders = Order()
    .select()
    .where {
        it.status > any<Int>(
            Order()
                .select { order -> order.status }
                .where { order -> order.userId == 27 }
        )
    }
    .queryList()
```

```sql group="Quantified subquery 1" name="Mysql" icon="mysql"
SELECT `id`, `user_id` AS `userId`, `status`
FROM `order`
WHERE `order`.`status` > ANY (
    SELECT `status`
    FROM `order`
    WHERE `order`.`user_id` = :userId
)
```

`some<T>(query)` 渲染为 `SOME`。`all<T>(query)` 渲染为 `ALL`。

```kotlin group="Quantified subquery 2" name="all" icon="kotlin"
Order()
    .delete()
    .where {
        it.status <= all<Int>(
            Order().select { order -> order.status }
        )
    }
    .execute()
```

## 匹配 row-value tuple

需要同时匹配多列时，可以使用 `[a, b] in query`。左侧字段数量与子查询 select 列数保持一致。

```kotlin group="Tuple subquery" name="kotlin" icon="kotlin"
val orders = Order()
    .select()
    .where {
        [it.userId, it.status] in Order()
            .select { order -> [order.userId, order.status] }
            .where { order -> order.status == 1 }
    }
    .queryList()
```

```sql group="Tuple subquery" name="Mysql" icon="mysql"
SELECT `id`, `user_id` AS `userId`, `status`
FROM `order`
WHERE (`order`.`user_id`, `order`.`status`) IN (
    SELECT `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`status` = :status
)
```

`[a, b] !in query` 用于生成 tuple `NOT IN`。

## 按标量子查询排序

排序值来自标量子查询时，可以使用 `addSortSubquery(query, ordering)`。

```kotlin group="Sort subquery 1" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .orderBy {
        addSortSubquery(
            Order()
                .select { order -> order.status }
                .where { order -> order.userId == 29 }
                .limit(1),
            SqlOrdering.Desc
        )
    }
    .queryList()
```

```sql group="Sort subquery 1" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
ORDER BY (
    SELECT `status`
    FROM `order`
    WHERE `order`.`user_id` = :userId
    LIMIT 1
) DESC
```

## 按 selected alias 排序

`orderBy { ... }` 读取当前查询 context，因此可以在同一层按 selected alias 排序。

```kotlin group="Sort subquery 2" name="selected alias" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .orderBy { it.nameLength.desc() }
    .queryList()
```

```sql group="Sort subquery 2" name="selected alias sql" icon="mysql"
SELECT `id`, LENGTH(`name`) AS `nameLength`
FROM `user`
ORDER BY `nameLength` DESC
```

## 对派生来源分页

`page(pageIndex, pageSize)` 和 `withTotal()` 可以在 selectable source 进入下一层查询后继续使用。

```kotlin group="Paged source" name="kotlin" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val (total, rows) = nameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .orderBy { it.nameLength.desc() }
    .page(1, 10)
    .withTotal()
    .queryList()
```

```sql group="Paged source" name="Mysql" icon="mysql"
SELECT `q`.`id`, `q`.`nameLength`
FROM (
    SELECT `id`, LENGTH(`name`) AS `nameLength`
    FROM `user`
) AS `q`
WHERE `q`.`nameLength` > :nameLength
ORDER BY `q`.`nameLength` DESC
LIMIT 10
OFFSET 0
```

```sql group="Paged source" name="total sql" icon="mysql"
SELECT COUNT(*) FROM (
    SELECT 1
    FROM (
        SELECT `id`, LENGTH(`name`) AS `nameLength`
        FROM `user`
    ) AS `q`
    WHERE `q`.`nameLength` > :nameLength
    ORDER BY `q`.`nameLength` DESC
) AS total_count
```

## 在下一层过滤窗口函数结果

窗口函数 alias 是 selected 字段。`where` 需要过滤窗口 alias 时，可以进入下一层查询。

```kotlin group="Window source" name="kotlin" icon="kotlin"
val ranked = Order()
    .select {
        [
            it.id,
            it.userId,
            it.status,
            f.rowNumber()
                .over {
                    partitionBy(it.userId)
                    orderBy(it.status.asc())
                }
                .alias("rn")
        ]
    }

val firstOrders = ranked
    .select { [it.id, it.userId, it.status] }
    .where { it.rn == 1 }
    .queryList()
```

```sql group="Window source" name="Mysql" icon="mysql"
SELECT `q`.`id`, `q`.`userId`, `q`.`status`
FROM (
    SELECT `id`,
           `user_id` AS `userId`,
           `status`,
           ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` ASC) AS rn
    FROM `order`
) AS `q`
WHERE `q`.`rn` = :rn
```

## Join 派生查询源

`KSelectable` 可以传给 `join(...)`。join lambda 会接收该 selectable source 的生成投影。

```kotlin group="Join source" name="kotlin" icon="kotlin"
val paidOrders = Order()
    .select { [it.userId, it.status] }
    .where { it.status == 1 }

val users = User().join(paidOrders) { user, order ->
    leftJoin(order) { user.id == order.userId }
    select { [user.id, user.name, order.status] }
}.queryList()
```

```sql group="Join source" name="Mysql" icon="mysql"
SELECT `user`.`id` AS `id`,
       `user`.`name` AS `name`,
       `q`.`status` AS `status`
FROM `user`
LEFT JOIN (
    SELECT `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`status` = :status
) AS `q`
ON `user`.`id` = `q`.`userId`
```

## 把 union 作为来源

`union(...)` 的结果可以继续用于 `select`、`insert` 和 CTAS。需要生成 `UNION ALL` 时，使用 `.all()` 或中缀 `unionAll`。

```kotlin group="Union source 1" name="kotlin" icon="kotlin"
val paid = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }

val shipped = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 2 }

val rows = union(paid, shipped)
    .select { [it.id, it.userId, it.status] }
    .queryList()
```

```sql group="Union source 1" name="Mysql" icon="mysql"
(SELECT `id`, `user_id` AS `userId`, `status`
 FROM `order`
 WHERE `order`.`status` = :status)
UNION
(SELECT `id`, `user_id` AS `userId`, `status`
 FROM `order`
 WHERE `order`.`status` = :status@1)
```

union 结果也可以在执行终端方法前继续排序和限制条数。

```kotlin group="Union source 2" name="order and limit" icon="kotlin"
val latest = union(paid, shipped)
    .orderBy("id" to SqlOrdering.Desc)
    .limit(10)
    .queryList()
```

```sql group="Union source 2" name="order and limit sql" icon="mysql"
(SELECT `id`, `user_id` AS `userId`, `status`
 FROM `order`
 WHERE `order`.`status` = :status)
UNION
(SELECT `id`, `user_id` AS `userId`, `status`
 FROM `order`
 WHERE `order`.`status` = :status@1)
ORDER BY `id` DESC
LIMIT 10
```

## 从查询插入

`KSelectable<Selected>.insert<Target>()` 会创建 INSERT SELECT。没有显式 values 时，Kronos 按来源 select 列表和目标可插入字段映射。

```kotlin group="INSERT SELECT 1" name="default mapping" icon="kotlin"
Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderArchive>()
    .execute()
```

```sql group="INSERT SELECT 1" name="Mysql" icon="mysql"
INSERT INTO `order_archive` (`id`, `user_id`, `status`)
SELECT `id`, `user_id` AS `userId`, `status`
FROM `order`
WHERE `order`.`status` = :status
```

没有显式 values 时，来源 select 列数需要和目标可插入字段数一致。

```kotlin group="INSERT SELECT 2" name="column count" icon="kotlin"
Order()
    .select { it.id }
    .insert<OrderArchive>()
    .build()
```

```text group="INSERT SELECT 2" name="error"
Insert-select source column count (1) must match target insertable field count (3).
```

目标表使用自增主键时，默认插入字段会排除 identity 主键。

```kotlin group="INSERT SELECT 3" name="identity target" icon="kotlin"
Order()
    .select { [it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderIdentityArchive>()
    .execute()
```

```sql group="INSERT SELECT 3" name="identity target sql" icon="mysql"
INSERT INTO `order_identity_archive` (`user_id`, `status`)
SELECT `user_id` AS `userId`, `status`
FROM `order`
WHERE `order`.`status` = :status
```

`execute()` 返回 `KronosOperationResult`；`affectedRows` 是 INSERT SELECT 语句插入的行数。

```kotlin group="INSERT SELECT 4" name="result" icon="kotlin"
val result = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderArchive>()
    .execute()

val insertedRows = result.affectedRows
```

目标值来自表达式、常量、`null` 或标量子查询时，可以使用显式 values。

```kotlin group="INSERT SELECT 5" name="explicit values" icon="kotlin"
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

```sql group="INSERT SELECT 5" name="explicit values sql" icon="mysql"
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

## 从查询创建表

`wrapper.table.createTable(target, query)` 会使用 `CREATE TABLE AS SELECT` 创建表。

```kotlin group="CTAS 1" name="kotlin" icon="kotlin"
val paidOrders = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }

wrapper.table.createTable(OrderArchive(), paidOrders)
```

```sql group="CTAS 1" name="Mysql" icon="mysql"
CREATE TABLE IF NOT EXISTS `order_archive` AS
SELECT `id`, `user_id` AS `userId`, `status`
FROM `order`
WHERE `order`.`status` = :status
```

CTAS 使用查询输出作为表结构。需要索引、注释、默认值、主键等 KPojo schema 元数据时，先使用普通 `createTable` 建表，再使用 INSERT SELECT 写入数据。

创建出的表列来自查询输出。上面的示例中，表结构基于 `id`、`userId` 和 `status`。

```kotlin group="CTAS 2" name="result shape" icon="kotlin"
data class OrderArchiveRow(
    val id: Int?,
    val userId: Int?,
    val status: Int?
)
```

## 在 Update 中使用子查询

子查询可以筛选要更新的行。

```kotlin group="DML subquery 1 1" name="update where" icon="kotlin"
User(name = "active")
    .update { [it.name] }
    .where {
        it.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
    }
    .execute()
```

```sql group="DML subquery 1 1" name="update where sql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `user`.`id` IN (
    SELECT `user_id` AS `userId`
    FROM `order`
    WHERE `order`.`status` = :status
)
```

标量子查询也可以作为 update 赋值。

```kotlin group="DML subquery 1 2" name="update set" icon="kotlin"
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

```sql group="DML subquery 1 2" name="update set sql" icon="mysql"
UPDATE `order`
SET `status` = (
    SELECT `status`
    FROM `order`
    WHERE `order`.`user_id` = :userId
    LIMIT 1
)
WHERE `order`.`id` = :id
```

## 在 Delete 中使用子查询

子查询可以筛选要删除的行。

```kotlin group="DML subquery 2 1" name="delete where" icon="kotlin"
User()
    .delete()
    .where { user ->
        !exists(
            Order()
                .select()
                .where { order -> order.userId == user.id }
        )
    }
    .execute()
```

```sql group="DML subquery 2 1" name="delete where sql" icon="mysql"
DELETE FROM `user`
WHERE NOT EXISTS (
    SELECT `id`, `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`user_id` = `user`.`id`
)
```

目标 KPojo 使用逻辑删除时，Kronos 会保留子查询谓词并追加逻辑删除谓词。

```sql group="DML subquery 2 2" name="logic delete sql" icon="mysql"
UPDATE `user`
SET `deleted` = :deletedNew
WHERE `user`.`id` IN (
    SELECT `user_id` AS `userId`
    FROM `order`
    WHERE `order`.`status` = :status
)
AND `deleted` = 0
```

## 在 Upsert 冲突更新中使用标量子查询

`upsert().onConflict()` 的冲突更新赋值可以使用标量子查询。

```kotlin group="DML subquery 3" name="upsert conflict" icon="kotlin"
User(id = 1, name = "seed")
    .upsert()
    .patch(
        "name" to Order()
            .select { it.status }
            .where { it.status == 1 }
            .limit(1)
    )
    .on { it.id }
    .onConflict()
    .execute()
```

```sql group="DML subquery 3" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`)
VALUES (:id, :name)
ON DUPLICATE KEY UPDATE `name` = (
    SELECT `status`
    FROM `order`
    WHERE `order`.`status` = :status
    LIMIT 1
)
```

```sql group="DML subquery 3" name="PostgreSQL" icon="postgres"
INSERT INTO "user" ("id", "name")
VALUES (:id, :name)
ON CONFLICT ("id") DO UPDATE SET "name" = (
    SELECT "status"
    FROM "order"
    WHERE "order"."status" = :status
    LIMIT 1
)
```

## 可见性和 alias 规则

`where`、`groupBy`、`having` 读取当前 source 字段。`orderBy` 读取 source 字段和 selected 字段。

```kotlin group="Rules 1" name="next layer" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val rows = nameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .queryList()
```

下一层会把 `nameLength` 暴露为 source 字段：

```sql group="Rules 2" name="next layer sql" icon="mysql"
SELECT `q`.`id`, `q`.`nameLength`
FROM (
    SELECT `id`, LENGTH(`name`) AS `nameLength`
    FROM `user`
) AS `q`
WHERE `q`.`nameLength` > :nameLength
```

嵌套查询中的参数名会保持独立。多层使用同一个逻辑名称时，Kronos 会为内层参数添加后缀。

```sql group="Rules 3" name="parameter names" icon="mysql"
WHERE `order`.`status` = :status
  AND `order`.`user_id` IN (
      SELECT `user_id` AS `userId`
      FROM `order`
      WHERE `order`.`status` = :status@1
  )
```

按操作查看入口：{{ $.keyword("query/select", ["查询记录"]) }}、{{ $.keyword("mutation/insert", ["插入记录"]) }}、{{ $.keyword("mutation/update", ["更新记录"]) }}、{{ $.keyword("mutation/delete", ["删除记录"]) }}、{{ $.keyword("mutation/upsert", ["Upsert 记录"]) }} 和 {{ $.keyword("query/join", ["联表查询"]) }}。
