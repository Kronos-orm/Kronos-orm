{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos subqueries are built from normal `select` DSL calls. A query result can be used as a scalar value, a predicate source, a derived table, an ordered or paged source, an INSERT SELECT source, or a CREATE TABLE AS SELECT source.

For the focused guide to field projection, aliases, join projection, generated result shapes, and DTO consumption, see {{ $.keyword("query/projection", ["Projection"]) }}.
For the function entry point, aggregate functions, string functions, math functions, and window function syntax, see {{ $.keyword("query/functions", ["Functions"]) }}.

## Generated projections

When `select { ... }` returns a custom field list, Kronos exposes the selected fields as a generated projection type. The generated shape behaves like a small result object whose properties are the selected fields and aliases.

```kotlin group="Generated projection 1" name="kotlin" icon="kotlin"
val nameLengths = User()
    .select {
        [
            it.id,
            f.length(it.name).alias("nameLength")
        ]
    }

val rows = nameLengths.toList()
```

The returned projection is used like this shape:

```kotlin group="Generated projection 2" name="projection shape" icon="kotlin"
data class UserNameLengthProjection(
    val id: Int?,
    val nameLength: Int?
)
```

The generated SQL names the expression with the alias:

```sql group="Generated projection 3" name="Mysql" icon="mysql"
SELECT `id`, LENGTH(`name`) AS `nameLength`
FROM `user`
```

Use `.alias("name")` for non-direct select items such as functions, aggregates, scalar subqueries, and window functions. The alias becomes the projection property name.

Use {{ $.keyword("query/projection", ["Projection"]) }} when choosing between map results, generated projection rows, and named DTO rows.

## Filter a selected projection

`KSelectable<Selected>.filter { ... }` starts the next query layer and keeps `Selected` as its result type. The filter receiver contains only the fields and aliases selected by the inner query.

```kotlin group="Derived source" name="kotlin" icon="kotlin"
val activeUsers = User()
    .where { it.status == 1 }
    .select { [it.id, it.name] }

val rows = activeUsers
    .filter { it.name like "A%" }
    .toList()
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

This pattern is useful when a selected alias should be filtered after it has been computed. `activeUsers.filter { ... }` is equivalent to `activeUsers.select().where { ... }`; use an explicit outer `select { ... }` when the next layer should change the result shape.

## Select a scalar subquery

A scalar subquery returns one value. It selects exactly one column and uses `limit(1)`.

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
    .toList()
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

When Kotlin needs a type hint, cast the scalar subquery after `limit(1)`. The cast only guides the DSL type.

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

## Use a scalar subquery in a comparison

Scalar subqueries can be used in `where` comparisons.

```kotlin group="Scalar comparison" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where {
        it.id > Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
            .limit(1)
    }
    .toList()
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

## Filter with IN and NOT IN

Use `field in query` when the subquery returns one column.

```kotlin group="IN subquery 1" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where {
        it.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
    }
    .toList()
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

Use `!in` for `NOT IN`.

```kotlin group="IN subquery 2" name="not in" icon="kotlin"
val users = User()
    .select()
    .where {
        it.id !in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 4 }
    }
    .toList()
```

## Filter with EXISTS and NOT EXISTS

Use `exists(query)` when the subquery checks whether a related row exists. The subquery can reference fields from the outer query.

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
    .toList()
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

Use `!exists(query)` for `NOT EXISTS`.

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
    .toList()
```

## Use ANY, SOME, and ALL comparisons

Use `any<T>(query)`, `some<T>(query)`, and `all<T>(query)` with comparison operators.

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
    .toList()
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

`some<T>(query)` renders `SOME`. `all<T>(query)` renders `ALL`.

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

## Match row-value tuples

Use `[a, b] in query` when the database should match multiple columns together. The left side and the subquery select list use the same column count.

```kotlin group="Tuple subquery" name="kotlin" icon="kotlin"
val orders = Order()
    .select()
    .where {
        [it.userId, it.status] in Order()
            .select { order -> [order.userId, order.status] }
            .where { order -> order.status == 1 }
    }
    .toList()
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

Use `[a, b] !in query` for tuple `NOT IN`.

## Order by a scalar subquery

Use `addSortSubquery(query, ordering)` when sort order comes from a scalar subquery.

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
    .toList()
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

## Order by a selected alias

`orderBy { ... }` reads the current query context, so it can order by a selected alias in the same layer.

```kotlin group="Sort subquery 2" name="selected alias" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .orderBy { it.nameLength.desc() }
    .toList()
```

```sql group="Sort subquery 2" name="selected alias sql" icon="mysql"
SELECT `id`, LENGTH(`name`) AS `nameLength`
FROM `user`
ORDER BY `nameLength` DESC
```

## Page a derived source

`page(pageIndex, pageSize).withTotal()` can be used after `filter` establishes the derived query.

```kotlin group="Paged source" name="kotlin" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val page = nameLengths
    .filter { it.nameLength > 8 }
    .orderBy { it.nameLength.desc() }
    .page(1, 10)
    .withTotal()
    .toList()

val rows = page.records
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

## Filter a window result

Window function aliases are selected fields. Use `filter` to establish a derived query and apply the predicate to that selected result.

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
    .filter { it.rn == 1 }
    .toList()
```

```sql group="Window source" name="Mysql" icon="mysql"
SELECT `q`.`id`, `q`.`userId`, `q`.`status`, `q`.`rn`
FROM (
    SELECT `id`,
           `user_id` AS `userId`,
           `status`,
           ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` ASC) AS rn
    FROM `order`
) AS `q`
WHERE `q`.`rn` = :rn
```

The `filter` receiver exposes `id`, `userId`, `status`, and `rn`, exactly matching `ranked`'s `Selected` projection. It cannot read an `Order` field that was not selected.

## Join a selectable source

A `KSelectable` can be passed to `join(...)`. The join lambda receives the generated projection from the selectable source.

```kotlin group="Join source" name="kotlin" icon="kotlin"
val paidOrders = Order()
    .select { [it.userId, it.status] }
    .where { it.status == 1 }

val users = User().join(paidOrders) { user, order ->
    leftJoin { user.id == order.userId }
        .select { [user.id, user.name, order.status] }
}.toList()
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

## Use a union as a source

`union(...)` results can be consumed by `select`, `insert`, and CTAS. Use `.all()` or the infix `unionAll` when the SQL should use `UNION ALL`.

```kotlin group="Union source 1" name="kotlin" icon="kotlin"
val paid = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }

val shipped = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 2 }

val rows = union(paid, shipped)
    .select { [it.id, it.userId, it.status] }
    .toList()
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

The union result also supports ordering and limiting before the terminal method.

```kotlin group="Union source 2" name="order and limit" icon="kotlin"
val latest = union(paid, shipped)
    .orderBy("id" to SqlOrdering.Desc)
    .limit(10)
    .toList()
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

## Insert from a query

`KSelectable<Selected>.insert<Target>()` creates an INSERT SELECT statement. Without an explicit value list, Kronos maps the source select list to the target insertable fields.

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

The default source select list must have the same column count as the target insertable field list.

```kotlin group="INSERT SELECT 2" name="column count" icon="kotlin"
Order()
    .select { it.id }
    .insert<OrderArchive>()
    .build()
```

```text group="INSERT SELECT 2" name="error"
Insert-select source column count (1) must match target insertable field count (3).
```

Identity primary keys are excluded from the target field list.

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

`execute()` returns `KronosOperationResult`; `affectedRows` is the number of rows inserted by the INSERT SELECT statement.

```kotlin group="INSERT SELECT 4" name="result" icon="kotlin"
val result = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderArchive>()
    .execute()

val insertedRows = result.affectedRows
```

Use an explicit value list when the target values come from expressions, constants, `null`, or scalar subqueries.

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

## Create a table from a query

`wrapper.table.createTable(target, query)` creates a table with `CREATE TABLE AS SELECT`.

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

CTAS uses the query output as the table shape. Use normal `createTable` and then INSERT SELECT when the table needs KPojo schema metadata such as indexes, comments, defaults, or primary key definitions.

The created table has the query output columns. In the example above, the table shape is based on `id`, `userId`, and `status`.

```kotlin group="CTAS 2" name="result shape" icon="kotlin"
data class OrderArchiveRow(
    val id: Int?,
    val userId: Int?,
    val status: Int?
)
```

## Update with subqueries

Subqueries can select the rows to update.

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

Scalar subqueries can also assign update values.

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

## Delete with subqueries

Subqueries can select the rows to delete.

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

When the target KPojo uses logical delete, Kronos keeps the subquery predicate and adds the logical delete predicate.

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

## Upsert with scalar subquery assignments

`upsert().onConflict()` can use a scalar subquery in the conflict update assignment.

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

## Visibility and alias rules

`where`, `groupBy`, and `having` read the current source fields. `orderBy` reads the source fields and the selected fields. `filter` starts an outer layer whose source is exactly the current `Selected` result.

```kotlin group="Rules 1" name="next layer" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val rows = nameLengths
    .filter { it.nameLength > 8 }
    .toList()
```

The derived layer exposes `nameLength` as a source field:

```sql group="Rules 2" name="next layer sql" icon="mysql"
SELECT `q`.`id`, `q`.`nameLength`
FROM (
    SELECT `id`, LENGTH(`name`) AS `nameLength`
    FROM `user`
) AS `q`
WHERE `q`.`nameLength` > :nameLength
```

Parameter names from nested queries are kept distinct. When two layers use the same logical name, Kronos suffixes the nested parameter name.

```sql group="Rules 3" name="parameter names" icon="mysql"
WHERE `order`.`status` = :status
  AND `order`.`user_id` IN (
      SELECT `user_id` AS `userId`
      FROM `order`
      WHERE `order`.`status` = :status@1
  )
```

For operation-specific entry points, see {{ $.keyword("query/select", ["Select Records"]) }}, {{ $.keyword("mutation/insert", ["Insert Records"]) }}, {{ $.keyword("mutation/update", ["Update Records"]) }}, {{ $.keyword("mutation/delete", ["Delete Records"]) }}, {{ $.keyword("mutation/upsert", ["Upsert Records"]) }}, and {{ $.keyword("query/join", ["Select Join Tables"]) }}.
