{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Create Table As Select, or CTAS, creates a database table from a Kronos selectable query.

> **Note**
> CTAS uses the query output as the table shape. Use {{ $.keyword("database/schema-sync", ["Schema Sync"]) }} when the table should be created from KPojo schema metadata such as primary keys, comments, defaults, and indexes.

## Create a table from a select

Pass the target KPojo and the source query to `Kronos.dataSource.table.createTable(target, query)`.

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

## Use aliases as column names

The select list controls the target columns. Use `.alias("name")` for computed values and scalar subqueries.

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

For projection rules and generated result shape, see {{ $.keyword("query/projection", ["Projection"]) }}.

## Create a table from a scalar subquery projection

Scalar subqueries can be part of the CTAS select list when they are aliased.

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

For subquery source, union source, and INSERT SELECT examples, see {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## Build the task before execution

Use `buildCreateTableAsSelectTask()` when you need to inspect SQL and parameters before execution.

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
