{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

In Kronos, we can use the `KPojo.upsert().execute()` method to upsert a record into the database.

Since implementations differ from database to database, in Kronos we have unified the `upsert` operation to achieve cross-database compatibility.

For version fields on the update and insert paths, see {{ $.keyword("mutation/optimistic-lock", ["Optimistic Lock"]) }}. For logically deleted rows restored by upsert, see {{ $.keyword("mutation/logic-delete", ["Logic Delete"]) }}.

## {{ $.title("on") }} Set match fields

The `on` fields match an existing row. Kronos updates the matched row, or inserts a new row when no row matches. The `on` fields can be one field or multiple fields. They build the match query and update condition; they do not create database unique constraints.

When the matched row is logically deleted, Kronos updates the existing row and restores the logic-delete marker to the active value.

Regular `on { ... }` upsert runs a match query before choosing insert or update. When the optimistic lock strategy is not enabled, the match query uses an update lock by default.

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert()
  .on { [it.id, it.name] }
  .execute()
```

```sql group="Case 1" name="Mysql" icon="mysql"
SELECT COUNT(1) FROM `user` WHERE `id` = :id and `name` = :name LIMIT 1 FOR UPDATE;
# Update if record exists
UPDATE `user` SET `id` = :id, `name` = :name, `age` = :age WHERE `id` = :id and `name` = :name;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name LIMIT 1 FOR UPDATE;
# Update if record exists
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 1" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name LIMIT 1;
# Update if record exists
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WITH (UPDLOCK, ROWLOCK) WHERE [id] = :id and [name] = :name;
# Update if record exists
UPDATE [user] SET [id] = :id, [name] = :name, [age] = :age WHERE [id] = :id and [name] = :name;
# Insert if record does not exist
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 1" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name FETCH NEXT 1 ROWS ONLY FOR UPDATE;
# Update if record exists
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

## {{ $.title("upsert") }} Set the fields to be updated

The `upsert` method is used to set the fields to be updated.

```kotlin group="Case 2" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { it.name }
  .on { it.id }
  .execute()
```

```sql group="Case 2" name="Mysql" icon="mysql"
SELECT COUNT(1) FROM `user` WHERE `id` = :id LIMIT 1 FOR UPDATE;
# Update if record exists
UPDATE `user` SET `name` = :name WHERE `id` = :id;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id LIMIT 1 FOR UPDATE;
# Update if record exists
UPDATE "user" SET "name" = :name WHERE "id" = :id;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 2" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM "user" WHERE "id" = :id LIMIT 1;
# Update if record exists
UPDATE "user" SET "name" = :name WHERE "id" = :id;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WITH (UPDLOCK, ROWLOCK) WHERE [id] = :id;
# Update if record exists
UPDATE [user] SET [name] = :name WHERE [id] = :id;
# Insert if record does not exist
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id FETCH NEXT 1 ROWS ONLY FOR UPDATE;
# Update if record exists
UPDATE "user" SET "name" = :name WHERE "id" = :id;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

## {{ $.title("upsert") }} {{ $.title("-") }} Set the excluded fields

The `upsert` method is used to set the excluded fields.

```kotlin group="Case 3" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { it - it.id }
  .on { it.id }
  .execute()
```

```sql group="Case 3" name="Mysql" icon="mysql"
SELECT COUNT(1) FROM `user` WHERE `id` = :id LIMIT 1 FOR UPDATE;
# Update if record exists
UPDATE `user` SET `name` = :name, `age` = :age WHERE `id` = :id;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id LIMIT 1 FOR UPDATE;
# Update if record exists
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 3" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM "user" WHERE "id" = :id LIMIT 1;
# Update if record exists
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WITH (UPDLOCK, ROWLOCK) WHERE [id] = :id;
# Update if record exists
UPDATE [user] SET [name] = :name, [age] = :age WHERE [id] = :id;
# Insert if record does not exist
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id FETCH NEXT 1 ROWS ONLY FOR UPDATE;
# Update if record exists
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

## {{ $.title("onConflict") }} Use database uniqueness conflicts

Calling `onConflict()` means: insert the row, and update it when a database uniqueness constraint is hit. Kronos uses the current dialect to generate the required SQL. Add `on { ... }` before `onConflict()` when the conflict target must be a specific key. When `on { ... }` is omitted, Kronos infers the target from KPojo uniqueness metadata: usable primary-key values first, then `@TableIndex(type = "UNIQUE")` / `@TableIndex(method = "UNIQUE")` fields whose values are present.

Strategy fields are maintained in both paths: insert initializes create/update time, logic delete, and version fields; conflict update refreshes update time, restores the active logic-delete value, and increments the version field.

```kotlin group="Case 4" name="kotlin" icon="kotlin" {7-10}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { [it.name, it.age] }
  .onConflict()
  .execute()
```

```sql group="Case 4" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`, `age`)
VALUES (:id, :name, :age)
ON DUPLICATE KEY UPDATE `name` = :name, `age` = :age;
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
INSERT INTO "user" ("id", "name", "age")
VALUES (:id, :name, :age)
ON CONFLICT ("id") DO UPDATE SET "name" = :name, "age" = :age;
```

```sql group="Case 4" name="SQLite" icon="sqlite"
INSERT INTO "user" ("id", "name", "age")
VALUES (:id, :name, :age)
ON CONFLICT ("id") DO UPDATE SET "name" = :name, "age" = :age;
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
MERGE INTO [user] AS [t1]
USING (SELECT :id AS [id], :name AS [name], :age AS [age]) AS [t2]
ON ([t1].[id] = [t2].[id])
WHEN MATCHED THEN UPDATE SET [t1].[name] = :name, [t1].[age] = :age
WHEN NOT MATCHED THEN INSERT ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 4" name="Oracle" icon="oracle"
MERGE INTO "USER" "T1"
USING (SELECT :id AS "ID", :name AS "NAME", :age AS "AGE") "T2"
ON ("T1"."ID" = "T2"."ID")
WHEN MATCHED THEN UPDATE SET "T1"."NAME" = :name, "T1"."AGE" = :age
WHEN NOT MATCHED THEN INSERT ("ID", "NAME", "AGE") VALUES (:id, :name, :age)
```

Use explicit conflict fields for a unique constraint other than the primary key. For example, if the table has a unique key on `email`:

```kotlin group="Case 4-target" name="kotlin" icon="kotlin"
User(email = "ada@example.com", name = "Ada")
  .upsert { it.name }
  .on { it.email }
  .onConflict()
  .execute()
```

If the identity primary key is not provided and the model declares a unique index, `onConflict()` can infer that index:

```kotlin group="Case 4-unique" name="model" icon="kotlin"
@Table("user")
@TableIndex("uk_user_email", ["email"], type = "UNIQUE")
data class User(
  @PrimaryKey(identity = true)
  var id: Int? = null,
  var email: String? = null,
  var name: String? = null
) : KPojo
```

```kotlin group="Case 4-unique" name="kotlin" icon="kotlin"
User(email = "ada@example.com", name = "Ada")
  .upsert { it.name }
  .onConflict()
  .execute()
```

```sql group="Case 4-unique" name="PostgreSQL" icon="postgres"
INSERT INTO "user" ("email", "name")
VALUES (:email, :name)
ON CONFLICT ("email") DO UPDATE SET "name" = :name;
```

For composite unique indexes, every indexed field must have a value for inference. Use explicit `on { ... }` when multiple unique keys are possible and the intended target should be unambiguous.

### Set a conflict-update value from a scalar subquery

`set` can assign a scalar subquery to the field updated by `onConflict`. The generated conflict update keeps the scalar subquery as the assigned value.

```kotlin group="Case 4-1" name="kotlin" icon="kotlin"
Order(id = 1, status = 0)
  .upsert()
  .set {
      it.status = (Order()
          .select { order -> order.status }
          .where { order -> order.userId == 44 }
          .limit(1) as Int?)
  }
  .onConflict()
  .execute()
```

```sql group="Case 4-1" name="Mysql" icon="mysql"
INSERT INTO `order` (`id`, `status`)
VALUES (:id, :status)
ON DUPLICATE KEY UPDATE `status` = (
    SELECT `status`
    FROM `order`
    WHERE `order`.`user_id` = :userId
    LIMIT 1
)
```

```sql group="Case 4-1" name="PostgreSQL" icon="postgres"
INSERT INTO "order" ("id", "status")
VALUES (:id, :status)
ON CONFLICT ("id") DO UPDATE SET "status" = (
    SELECT "status"
    FROM "order"
    WHERE "order"."user_id" = :userId
    LIMIT 1
)
```

More scalar subquery assignment forms are covered in {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

### Patch dynamic conflict-update values

`patch(...)` adds fields to the conflict update list and can provide the value used by `onConflict()`. Use it when the conflict update value is chosen dynamically.

```kotlin group="Case 4-2" name="SqlExpr and function" icon="kotlin"
import com.kotlinorm.beans.dsl.KronosFunctionExpr
import com.kotlinorm.syntax.expr.SqlExpr

User(id = 7, name = "seed", count = 2)
  .upsert { it.name }
  .patch(
      "count" to SqlExpr.NumberLiteral("10"),
      "name" to KronosFunctionExpr(SqlExpr.StringLiteral("patched"), "literal")
  )
  .onConflict()
  .execute()
```

```sql group="Case 4-2" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`, `count`)
VALUES (:id, :name, :count)
ON DUPLICATE KEY UPDATE `name` = 'patched', `count` = 10
```

Pass a `Field` when the conflict update should reuse another column expression.

```kotlin group="Case 4-3" name="field expression" icon="kotlin"
val countField = User().__columns.single { it.name == "count" }

User(id = 8, name = "seed", count = 5)
  .upsert { it.name }
  .patch("name" to countField)
  .onConflict()
  .execute()
```

```sql group="Case 4-3" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`, `count`)
VALUES (:id, :name, :count)
ON DUPLICATE KEY UPDATE `name` = `user`.`count`
```

Pass a scalar `KSelectable` when the update value should come from a subquery.

```kotlin group="Case 4-4" name="scalar subquery" icon="kotlin"
User(id = 1, name = "seed")
  .upsert()
  .patch(
      "name" to Order()
          .select { it.status }
          .where { it.status == 15 }
          .limit(1)
  )
  .onConflict()
  .execute()
```

```sql group="Case 4-4" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`)
VALUES (:id, :name)
ON DUPLICATE KEY UPDATE `name` = (
    SELECT `status`
    FROM `order`
    WHERE `order`.`status` = :status
    LIMIT 1
)
```

`patch(...)` values are used as conflict-update assignments in the `onConflict()` path. In the regular upsert path, the same fields are added to the update set used after a row match.

## {{ $.title("lock") }} set row lock

Regular `on { ... }` upsert uses an update lock for the match query by default. Call `lock(...)` when the lock should be explicit or use a different lock type.

```kotlin group="Case 18" name="kotlin" icon="kotlin" {1-3}
import com.kotlinorm.syntax.statement.SqlLock

val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { [it.name, it.age] }
  .on { it.id }
  .lock(SqlLock.Update())
  .execute()
```

```sql group="Case 18" name="Mysql" icon="mysql"
SELECT COUNT(1) FROM `user` WHERE `id` = :id LIMIT 1 FOR UPDATE;
# Update if record exists
UPDATE `user` SET `name` = :nameNew, `age` = :ageNew WHERE `id` = :id;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

## Affected rows

In Kronos, we can use the `execute()` method to get the number of affected rows.

```kotlin name="demo" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

val (affectedRows) = user
                .upsert()
                .on { it.id }
                .execute()
```

## Batch upsert records

Use {{ $.keyword("mutation/batch-operations", ["Batch Operations"]) }} when one raw upsert SQL should run with many parameter sets.

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
  .upsert { [it.name, it.age] }
  .on { it.id }
  .execute(customWrapper)
```
