{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Insert a single record

In Kronos, we can use the `KPojo.insert().execute()` method to insert a record into the database.

```kotlin name="demo" icon="kotlin" {7}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user.insert().execute()
```

## Last insert ID and affected rows

`execute()` returns a `KronosOperationResult`.

```kotlin name="demo" icon="kotlin" {6,8,9}
val user: User = User(
    name = "Kronos",
    age = 18
)

val result = user.insert().withId().execute()
val affectedRows = result.affectedRows
val lastInsertId = result.lastInsertId
```

`.withId()` declares that this insert should read the generated ID. The target KPojo needs an identity primary key.

Read more about {{ $.keyword("mutation/last-insert-id", ["generated identity IDs"]) }}.

## Batch insert records

Use {{ $.keyword("mutation/batch-operations", ["Batch Operations"]) }} when the same SQL insert should run with many parameter sets.

## INSERT SELECT

Use `KSelectable<Selected>.insert<Target>()` to insert rows from a query. Without explicit values, Kronos inserts the source select list into the target insertable fields.

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

`execute()` returns `KronosOperationResult`. For INSERT SELECT, `affectedRows` is the number of rows inserted by the generated statement.

```kotlin group="INSERT SELECT default 2" name="result" icon="kotlin"
val result = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderArchive>()
    .execute()

val insertedRows = result.affectedRows
```

The source select list must match the target insertable field count when no explicit values are provided.

```kotlin group="INSERT SELECT default 3" name="column count" icon="kotlin"
Order()
    .select { it.id }
    .insert<OrderArchive>()
    .build()
```

```text group="INSERT SELECT default 3" name="error"
Insert-select source column count (1) must match target insertable field count (3).
```

Use explicit values when inserted values come from source expressions or scalar subqueries.

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

The source can also be a derived selectable. The target field order still comes from the target KPojo insertable fields.

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

When the target KPojo uses an identity primary key, Kronos excludes that field from the target field list.

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

For join sources, union sources, scalar subquery rules, and projection details, see {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## Specify the data source to use

In Kronos, we can pass `KronosDataSourceWrapper` into the `execute` method to achieve a custom database connection.

```kotlin name="demo" icon="kotlin" {9}
val customWrapper = CustomWrapper()

val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user.insert().execute(customWrapper)
```
