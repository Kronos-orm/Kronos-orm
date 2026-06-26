{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

In Kronos, we can use the `KPojo.upsert().execute()` method to upsert a record into the database.

Since implementations differ from database to database, in Kronos we have unified the `upsert` operation to achieve cross-database compatibility.

## {{ $.title("on") }} Set the unique constraint field

The `on` method is used to uniquely set a constraint field, either a single field or multiple fields. When the record exists, Kronos generates an update conditional statement based on the fields set by the `on` method, otherwise it generates an insert statement.

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert()
  .on { it.id + it.name }
  .execute()
```

```sql group="Case 1" name="Mysql" icon="mysql"
SELECT COUNT(1) FROM `user` WHERE `id` = :id and `name` = :name;
# Update if record exists
UPDATE `user` SET `id` = :id, `name` = :name, `age` = :age WHERE `id` = :id and `name` = :name;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name;
# Update if record exists
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 1" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name;
# Update if record exists
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WHERE [id] = :id and [name] = :name;
# Update if record exists
UPDATE [user] SET [id] = :id, [name] = :name, [age] = :age WHERE [id] = :id and [name] = :name;
# Insert if record does not exist
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 1" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name;
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
SELECT COUNT(1) FROM `user` WHERE `id` = :id;
# Update if record exists
UPDATE `user` SET `name` = :name WHERE `id` = :id;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id;
# Update if record exists
UPDATE "user" SET "name" = :name WHERE "id" = :id;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 2" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM `user` WHERE `id` = :id;
# Update if record exists
UPDATE `user` SET `name` = :name WHERE `id` = :id;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WHERE [id] = :id;
# Update if record exists
UPDATE [user] SET [name] = :name WHERE [id] = :id;
# Insert if record does not exist
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id;
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
SELECT COUNT(1) FROM `user` WHERE `id` = :id;
# Update if record exists
UPDATE `user` SET `name` = :name, `age` = :age WHERE `id` = :id;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id;
# Update if record exists
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 3" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM `user` WHERE `id` = :id;
# Update if record exists
UPDATE `user` SET `name` = :name, `age` = :age WHERE `id` = :id;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WHERE [id] = :id;
# Update if record exists
UPDATE [user] SET [name] = :name, [age] = :age WHERE [id] = :id;
# Insert if record does not exist
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id;
# Update if record exists
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

## {{ $.title("onConflict") }} update when conflicted, otherwise insert

When using the `upsert` method, we can use the `onConflict` method to set the processing policy to update when there is a conflict, that is, update the record when it exists.

```kotlin group="Case 4" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { it.name + it.age }
  .onConflict()
  .execute()
```

```sql group="Case 4" name="Mysql" icon="mysql"
# use on duplicate key
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age) ON DUPLICATE KEY UPDATE `name` = :name, `age` = :age;
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
INSERT INTO "user" ("id", "name", "age") SELECT :id, :name, :age WHERE NOT EXISTS (SELECT 1 FROM "user" WHERE "name" = :name and "age" = :age);
UPDATE "user" SET "name" = :name, "age" = :age WHERE "name" = :name and "age" = :age;
```

```sql group="Case 4" name="SQLite" icon="sqlite"
# use on conflict
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age) ON CONFLICT(`name`, `age`) DO UPDATE SET `name` = :name, `age` = :age;
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
IF EXISTS (SELECT 1 FROM [user] WHERE [name] = :name and [age] = :age)
  BEGIN 
    UPDATE [user] SET [name] = :name, [age] = :age WHERE [name] = :name and [age] = :age
  END
ELSE
  BEGIN
    INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
  END
```

```sql group="Case 4" name="Oracle" icon="oracle"
BEGIN
  INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
EXCEPTION
  WHEN DUP_VAL_ON_INDEX THEN
    UPDATE "user" SET "name" = :name, "age" = :age WHERE "name" = :name and "age" = :age;
END;
```

## {{ $.title("lock") }} set row lock

`limit`方法用于设置查询时行锁，此时Kronos会根据`lock`方法设置的锁类型进行锁的添加。

```kotlin group="Case 18" name="kotlin" icon="kotlin" {1-3}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { it.name + it.age }
  .lock()
  .execute()
```

```sql group="Case 18" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` FOR UPDATE
# Update if record exists
UPDATE `user` SET `name` = :name WHERE `id` = :id;
# Insert if record does not exist
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 18" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" FOR UPDATE
# Update if record exists
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 18" name="SQLite" icon="sqlite"
# It is not supported to add a row lock function to Sqlite because Sqlite itself does not have a row lock function.
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name;
# Update if record exists
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 18" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY ROWLOCK
# Update if record exists
UPDATE [user] SET [id] = :id, [name] = :name, [age] = :age WHERE [id] = :id and [name] = :name;
# Insert if record does not exist
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 18" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" FOR UPDATE(NOWAIT)
# Update if record exists
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# Insert if record does not exist
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
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

In Kronos, we can use `Iterable<KPojo>.upsert().execute()` or `Array<KPojo>.upsert().execute()` methods to batch upsert records.

```kotlin name="demo" icon="kotlin" {14-17}
val users: List<User> = listOf(
    User(
        id = 1,
        name = "Kronos",
        age = 18
    ),
    User(
        id = 2,
        name = "Kronos ORM",
        age = 18
    )
)

users
  .upsert { it.name + it.age }
  .on { it.id }
  .execute()
```

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
  .upsert { it.name + it.age }
  .on { it.id }
  .execute(customWrapper)
```
