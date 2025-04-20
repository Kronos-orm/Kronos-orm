{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Automatically generate conditional statements and delete records based on KPojo instance values

In Kronos, we can use the `KPojo.delete().execute()` function to delete records in the database.

When the `by` or `where` function is not used, Kronos will generate a delete condition statement based on the value of the KPojo object.

> **Warning**
> When the field value of a KPojo object is `null`, the field will not generate a deletion condition, if you need to delete a record with a field value of `null`, please use the `where` method to specify it.

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user.delete().execute()
// Equivalent to
// user.delete().by { it.id  + it.name + it.age }.execute()
// or
// user.delete().where { it.eq }.execute()
// or
// user.delete().where { it.id.eq && it.name.eq && it.age.eq }.execute()
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
WHERE [id] = :id and [name] = : name and [age] = :age
```

```sql group="Case 1" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
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

In Kronos, we can use the `where` method to set the deletion condition, at which point Kronos generates the deletion {{ $.keyword("concept/where-having-on-clause", ["Criteria Conditional Statement"]) }} based on the condition set by the `where` method.

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
WHERE [id] = :id and [name] like : name and [age] > :ageMin
```

```sql group="Case 3" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" like :name
  and "age" > :ageMin
```

You can execute the `.eq` function on the query object so that you can add other query conditions based on generating conditional statements based on KPojo object values:.

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
  and [name] = : name
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

Kronos provides the minus operator `-` to specify columns that do not require automatic generation of conditional statements.

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

Please refer to Enabling Logical Deletion and Field Setting Settings for logical deletion: {{ $.keyword("getting-started/global-config", ["Global Config", "Logical Deletion Strategy"]) }}、{{
$.keyword("class-definition/annotation-config", ["Annotation Config", "Table logical deletion"]) }}、{{
$.keyword("class-definition/annotation-config", ["Annotation Config", "Column logical deletion"]) }}。

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

## Affected rows

The `KronosOperationResult` object returned by the `execute` method contains the number of affected rows.

```kotlin group="Case 5" name="kotlin" icon="kotlin" {5}
val user: User = User(
    id = 1,
)

val (affectedRows) = user.delete().execute()
```

## Batch delete records

In Kronos, we can use `Iterable<KPojo>.delete().execute()` or `Array<KPojo>.delete().execute()` methods to delete records from the database.

```kotlin group="Case 6" name="kotlin" icon="kotlin" {10}
val users: List<User> = listOf(
    User(
        id = 1,
    ),
    User(
        id = 2,
    )
)

users.delete().execute()
```

## Specify the data source to be used

In Kronos, we can pass `KronosDataSourceWrapper` into the `execute` method to implement a customized database connection.

```kotlin group="Case 7" name="kotlin" icon="kotlin" {7}
val customWrapper = CustomWrapper()

val user: User = User(
    id = 1,
)

user.delete().execute(customWrapper)
```
