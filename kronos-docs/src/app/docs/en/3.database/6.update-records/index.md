{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.title("set") }} Fields and values to Update Configuration

In Kronos, we can use the `KPojo.update().set` method to set the fields and values that need to be updated, and then use the `execute` method to perform the update operation.

When the `by` or `where` methods are not used, Kronos generates update conditional statements based on the value of the KPojo object.

> **Warning**
> When the field value of a KPojo object is `null`, the field will not generate an update condition, if you need to update a record with a field value of `null`, please use the `where` method to specify it.

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7-10}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user
    .update()
    .set { it.name = "Kronos ORM" }
    .execute()

// Dynamically sets the assignment column based on the string:
//    .set { it["name"] = "Kronos ORM" }
```

```sql group="Case 1" name="Mysql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
  and `name` = :name
  and `age` = :age
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
  and "name" = :name
  and "age" = :age
```

```sql group="Case 1" name="SQLite" icon="sqlite"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
  and `name` = :name
  and `age` = :age
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
UPDATE [user]
SET [name] = :nameNew
WHERE [id] = :id and [name] = : name and [age] = :age
```

```sql group="Case 1" name="Oracle" icon="oracle"
UPDATE "user"
SET "name" = :nameNew
WHERE "id" = :id
  and "name" = :name
  and "age" = :age
```

### {{ $.title("+=") }} {{ $.title("-=") }} Compound assignment operator

Kronos supports semantic `+=` and `-=` addition and subtraction assignment operations to update numeric columns in the database

```kotlin group="Case 1-1" name="kotlin" icon="kotlin" {1-4}
user
    .update()
    .set { it.age += 2 }
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

In Kronos, we can use the `where` method to set an update condition, at which point Kronos generates an update based on the field set by the `where` method {{ $.keyword("concept/where-having-on-clause", ["Criteria Conditional Statement"]) }}.

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

You can execute the `.eq` function on the query object so that you can add other query conditions based on generating conditional statements based on KPojo object values:.

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
  and [name] = : name
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

Kronos provides the minus operator `-` to specify columns that do not require automatic generation of conditional statements.

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

## {{ $.title("patch") }} Adding Parameters to Custom Deletion Condition

In Kronos, we can use the `patch` method to add parameters to custom deletion condition.

```kotlin group="Case 3-3" name="kotlin" icon="kotlin" {7-12}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user
    .update()
    .set { it.name = "Kronos ORM" }
    .where { "id = :id" }
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
    .update { it.name + it.age }
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

In Kronos, we can use `Iterable<KPojo>.update().execute()` or `Array<KPojo>.update().execute()` methods for bulk updating records in the database.

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
    .update { it.name + it.age }
    .where { it.id eq 1 }
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
    .update()
    .set { it.name = "Kronos ORM" }
    .execute(customWrapper)
```
