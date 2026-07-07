{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Delete records with explicit conditions

In Kronos, we can use the `KPojo.delete().execute()` function to delete records in the database.

An empty `where()` reads queryable non-null fields from the current KPojo object and generates delete conditions. Use `by { ... }` to choose object fields for matching, and use `where { ... }` to write custom conditions.

> **Warning**
> `delete().execute()` deletes every row allowed by framework strategies. Enable DataGuard when your application should reject full-table deletes.

For delete statements that write a marker column, see {{ $.keyword("mutation/logic-delete", ["Logic Delete"]) }}.

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

## {{ $.title("by") }} Delete Condition Configuration

In Kronos, we can use the `by` function to set the deletion condition, at which point Kronos will generate the deletion condition statement based on the field set by the `by` method.

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

## {{ $.title("where") }} Delete Condition Configuration

`where()` generates query-by-example delete conditions from the current object values. `where { ... }` generates a delete {{ $.keyword("query/conditions", ["conditional expression"]) }} from the lambda.

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

### Filter deletes with an IN subquery

Use `field in query` inside `where` to delete rows selected by another query.

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

### Filter deletes with EXISTS

Use `exists(query)` when a related row should decide whether the target row is deleted. The subquery can reference the outer `where` receiver.

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

More predicate subquery forms are covered in {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

Inside an explicit `where` block, `.eq` can expand the current KPojo object's field values into equality conditions, and you can combine those conditions with other expressions.

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

Kronos provides the minus operator `-` to exclude fields from the explicit `.eq` expansion.

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

## {{ $.title("patch") }}Add parameters for custom deletion conditions

In Kronos, we can use the `patch` method to add parameters to a custom delete condition.

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

## {{ $.title("logic") }} Logical deletion

In Kronos, we can set up logical deletion using the `logic` method, at which point Kronos generates the SQL statement for logical deletion.

Please refer to Enabling Logical Deletion and Field Setting Settings for logical deletion: {{ $.keyword("configuration/global-config", ["Global Config", "Logical Deletion Strategy"]) }}, {{ $.keyword("mapping/annotations", ["Annotation Config", "Table logical deletion"]) }}, {{ $.keyword("mapping/annotations", ["Annotation Config", "Column logical deletion"]) }}.

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

When logical deletion uses a subquery condition, Kronos keeps the subquery predicate and adds the logical-delete predicate.

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

## Affected rows

The `KronosOperationResult` object returned by the `execute` method contains the number of affected rows.

```kotlin group="Case 5" name="kotlin" icon="kotlin" {5}
val user: User = User(
    id = 1,
)

val (affectedRows) = user.delete().by { it.id }.execute()
```

## Batch delete records

Use {{ $.keyword("mutation/batch-operations", ["Batch Operations"]) }} when the same SQL delete should run with many parameter sets.

## Specify the data source to be used

In Kronos, we can pass `KronosDataSourceWrapper` into the `execute` method to implement a customized database connection.

```kotlin group="Case 7" name="kotlin" icon="kotlin" {7}
val customWrapper = CustomWrapper()

val user: User = User(
    id = 1,
)

user.delete().by { it.id }.execute(customWrapper)
```
