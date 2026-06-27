{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Delete records with explicit conditions

In Kronos, we can use the `KPojo.delete().execute()` function to delete records in the database.

Kronos does not generate delete conditions from KPojo instance values unless you explicitly call `by` or `where`. Use `by` to match fields with values from the object, or use `where` to write a custom condition.

> **Warning**
> Calling `delete().execute()` without `by` or `where` may affect every row that is not filtered by framework strategies such as logical delete. Enable DataGuard when your application should reject full-table `UPDATE` or `DELETE` statements.

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user.delete().by { [it.id, it.name, it.age] }.execute()
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
WHERE [id] = :id and [name] like :name and [age] > :ageMin
```

```sql group="Case 3" name="Oracle" icon="oracle"
DELETE
FROM "user"
WHERE "id" = :id
  and "name" like :name
  and "age" > :ageMin
```

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

val (affectedRows) = user.delete().by { it.id }.execute()
```

## Batch delete records

In Kronos, we can use `Iterable<KPojo>.delete().by { ... }.execute()` or `Array<KPojo>.delete().by { ... }.execute()` methods to delete multiple records from the database.

```kotlin group="Case 6" name="kotlin" icon="kotlin" {10}
val users: List<User> = listOf(
    User(
        id = 1,
    ),
    User(
        id = 2,
    )
)

users.delete().by { it.id }.execute()
```

## Specify the data source to be used

In Kronos, we can pass `KronosDataSourceWrapper` into the `execute` method to implement a customized database connection.

```kotlin group="Case 7" name="kotlin" icon="kotlin" {7}
val customWrapper = CustomWrapper()

val user: User = User(
    id = 1,
)

user.delete().by { it.id }.execute(customWrapper)
```
