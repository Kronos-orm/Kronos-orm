{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Create Table As Select，简称 CTAS，用于根据 Kronos 查询创建数据库表。

> **Note**
> CTAS 使用查询输出决定表形态。需要通过 KPojo schema 元数据创建主键、注释、默认值和索引时，使用 {{ $.keyword("database/schema-sync", ["表结构同步"]) }}。

## 根据 select 创建表

把目标 KPojo 和来源查询传给 `Kronos.dataSource.table.createTable(target, query)`。

```kotlin group="CTAS" name="kotlin" icon="kotlin"
@Table("tb_order_archive")
data class OrderArchive(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null
) : KPojo

val paidOrders = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 16 }

Kronos.dataSource.table.createTable(OrderArchive(), paidOrders)
```

```sql group="CTAS" name="Mysql" icon="mysql"
CREATE TABLE IF NOT EXISTS `tb_order_archive` AS
SELECT `id`, `user_id` AS `userId`, `status`
FROM `tb_order`
WHERE `tb_order`.`status` = :status
```

```text group="CTAS" name="params"
status = 16
```

## 使用 alias 控制列名

select 列表决定目标表字段。计算值和标量子查询使用 `.alias("name")` 指定列名。

```kotlin group="Alias" name="kotlin" icon="kotlin"
val archiveSource = Order()
    .select {
        [
            it.id,
            it.userId,
            (it.amount.sum()).alias("totalAmount")
        ]
    }
    .groupBy { [it.id, it.userId] }

Kronos.dataSource.table.createTable(OrderArchive(), archiveSource)
```

```sql group="Alias" name="Mysql" icon="mysql"
CREATE TABLE IF NOT EXISTS `tb_order_archive` AS
SELECT `id`, `user_id` AS `userId`, SUM(`amount`) AS `totalAmount`
FROM `tb_order`
GROUP BY `id`, `user_id`
```

投影规则和生成结果形态见 {{ $.keyword("query/projection", ["投影"]) }}。

## 使用标量子查询投影创建表

标量子查询作为 CTAS select 列时，需要设置 alias。

```kotlin group="Scalar" name="kotlin" icon="kotlin"
val source = Order()
    .select {
        [
            it.id,
            it.userId,
            Order()
                .select { order -> order.status }
                .where { order -> order.status == 38 }
                .limit(1)
                .alias("latestStatus")
        ]
    }
    .where { it.status == 37 }

Kronos.dataSource.table.createTable(OrderArchive(), source)
```

```sql group="Scalar" name="Mysql" icon="mysql"
CREATE TABLE IF NOT EXISTS `tb_order_archive` AS
SELECT `id`, `user_id` AS `userId`,
       (SELECT `status`
        FROM `tb_order`
        WHERE `tb_order`.`status` = :status@1
        LIMIT 1) AS `latestStatus`
FROM `tb_order`
WHERE `tb_order`.`status` = :status
```

子查询 source、union source 和 INSERT SELECT 示例见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## 执行前查看任务

需要在执行前查看 SQL 和参数时，使用 `buildCreateTableAsSelectTask()`。

```kotlin group="Task" name="kotlin" icon="kotlin"
val task = Kronos.dataSource.table.buildCreateTableAsSelectTask(
    OrderArchive(),
    paidOrders
)

println(task.sql)
println(task.paramMap)
```

```text group="Task" name="result"
CREATE TABLE IF NOT EXISTS `tb_order_archive` AS SELECT ...
{status=16}
```
