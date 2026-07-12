{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.title("set") }} Fields and values to Update Configuration

In Kronos, we can use the `KPojo.update().set` method to set the fields and values that need to be updated, and then use the `execute` method to perform the update operation.

Use `where()` or `by { ... }` to choose target rows. An empty `where()` generates equality conditions from the current KPojo's queryable non-null fields.

> **Warning**
> `update().execute()` updates every row allowed by framework strategies. Enable DataGuard when your application should reject full-table writes.

For strategy fields added to generated updates, see {{ $.keyword("mutation/logic-delete", ["Logic Delete"]) }} and {{ $.keyword("mutation/optimistic-lock", ["Optimistic Lock"]) }}.

```kotlin group="Case 1" name="kotlin" icon="kotlin" {5-9}
val user: User = User(
    id = 1
)

user
    .update()
    .set { it.name = "Kronos ORM" }
    .where()
    .execute()

// Dynamically sets the assignment column based on the string:
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

### {{ $.title("+=") }} {{ $.title("-=") }} Compound assignment operator

Kronos supports semantic `+=` and `-=` addition and subtraction assignment operations to update numeric columns in the database

```kotlin group="Case 1-1" name="kotlin" icon="kotlin" {1-4}
user
    .update()
    .set { it.age += 2 }
    .where()
    .execute()

// Dynamically sets the assignment column based on the string:
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

### Kotlin expressions as assignment values

The right side of a `set` assignment can be a normal runtime Kotlin expression. Kronos binds the expression result as the assignment value.

```kotlin group="Expression assignment" name="kotlin" icon="kotlin"
fun displayName(): String? = null

User(id = 1)
    .update()
    .set { it.name = displayName() ?: "Anonymous" }
    .where()
    .execute()
```

```sql group="Expression assignment" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
```

### Set a value from a scalar subquery

Use a scalar subquery in `set` when the new value should come from another query. The subquery selects one column and uses `limit(1)`.

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

## {{ $.title("by") }} Update Condition Configuration

In Kronos, we can use the `by` method to set an update condition, at which point Kronos generates an update condition statement based on the fields set by the `by` method.

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

## {{ $.title("where") }} Update Condition Configuration

`where()` generates query-by-example update conditions from the current object values. `where { ... }` generates an update {{ $.keyword("query/conditions", ["conditional expression"]) }} from the lambda.

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

### Filter updates with an IN subquery

Use `field in query` inside `where` to update rows selected by another query.

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

More scalar and predicate subquery forms are covered in {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

Inside an explicit `where` block, `.eq` can expand the current KPojo object's field values into equality conditions, and you can combine those conditions with other expressions.

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

Kronos provides the minus operator `-` to exclude fields from the explicit `.eq` expansion.

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

## {{ $.title("patch") }} Adding Parameters to Custom Update Conditions

In Kronos, we can use the `patch` method to add parameters to custom update conditions.

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

## {{ $.title("update") }} Fields to Update Configuration

In Kronos, we set the fields that need to be updated in the `update` method, and fields not set in the `update` method will not be updated.

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

## {{ $.title("update") }} {{ $.title("-") }} Fields to Exclude Configuration

In Kronos, we can use the minus expression `-` to set the fields to be excluded.

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

## Affected Rows

In Kronos, we can use the `execute` method to perform an update operation and get the number of lines affected by the update operation.

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

## Batch update records

Use {{ $.keyword("mutation/batch-operations", ["Batch Operations"]) }} when the same SQL update should run with many parameter sets.

## Specify the data source to be used

In Kronos, we can pass `KronosDataSourceWrapper` into the `execute` method to implement a customized database connection.

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
