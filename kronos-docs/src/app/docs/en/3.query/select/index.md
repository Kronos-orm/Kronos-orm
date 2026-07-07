{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.title("select") }}Select records

In Kronos, we can use `KPojo.select()` method to query database records.

Field lists, aliases, generated projection objects, and join projection examples are covered in {{ $.keyword("query/projection", ["Projection"]) }}.

```kotlin group="Case 1" name="kotlin" icon="kotlin" {1}
val users: List<User> = User().select().queryList()
```

```sql group="Case 1" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
```

```sql group="Case 1" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
```

```sql group="Case 1" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
```

## {{ $.title("select") }}Specify query fields

The `select` method is used to specify the query fields. At this time, Kronos will generate query field statements according to the fields set by the `select` method.

It's supported to use `String` as a custom query field.

Multiple fields are written with `[]` and the `alias` method is used to set field aliases.

For field projection, aliases, generated result shapes, and DTO consumption, see {{ $.keyword("query/projection", ["Projection"]) }}. For derived query sources, scalar subqueries, predicate subqueries, and window-result filtering, see {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

```kotlin group="Case 1-1" name="kotlin" icon="kotlin" {1-5}
val rows: List<Map<String, Any>> = User()
    .select {
        [it.id, it.name.alias("username"), "count(*) as total", "1"]
    }
    .query()
```

```sql group="Case 1-1" name="Mysql" icon="mysql"
SELECT `id`, `name` AS `username`, count(*) AS total, 1
FROM `user`
```

```sql group="Case 1-1" name="PostgreSQL" icon="postgres"
SELECT "id", "name" AS "username", count(*) AS total, 1
FROM "user"
```

```sql group="Case 1-1" name="SQLite" icon="sqlite"
SELECT "id", `name` AS `username`, count(*) AS total, 1
FROM `user`
```

```sql group="Case 1-1" name="SQLServer" icon="sqlserver"
SELECT [id], [name] AS [username], count (*) AS total, 1
FROM [user]
```

```sql group="Case 1-1" name="Oracle" icon="oracle"
SELECT "id", "name" AS "username", count(*) AS total, 1
FROM "user"
```

### Query all fields, exclude some columns

You can pass `KPojo` to query all columns, use `-` to exclude columns, and use `[]` to combine the final select list.

> **Note**
> Please note that `-` must be used after `KPojo`.

```kotlin group="Case 1-2" name="kotlin" icon="kotlin"
val rows: List<Map<String, Any>> = User()
    .select { [it - it.id, "count(*) as total"] }
    .query()
```

```sql group="Case 1-2" name="Mysql" icon="mysql"
SELECT `name`, `age`, count(*) AS total
FROM `user`
```

```sql group="Case 1-2" name="PostgreSQL" icon="postgres"
SELECT "name", "age", count(*) AS total
FROM "user"
```

```sql group="Case 1-2" name="SQLite" icon="sqlite"
SELECT "name", "age", count(*) AS total
FROM "user"
```

```sql group="Case 1-2" name="SQLServer" icon="sqlserver"
SELECT [name], [age], count (*) AS total
FROM [user]
```

```sql group="Case 1-2" name="Oracle" icon="oracle"
SELECT "name", "age", count(*) AS total
FROM "user"
```

## Use subqueries and generated projections

Select fields can contain scalar subqueries, and `where` can consume selectable subqueries with `in`, `exists`, `any`, `all`, and row-value tuple conditions.

```kotlin group="Case 1-3" name="kotlin" icon="kotlin"
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
    .where { user ->
        user.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
    }
    .queryList()
```

```sql group="Case 1-3" name="Mysql" icon="mysql"
SELECT `id`,
       `name`,
       (SELECT `status`
        FROM `order`
        WHERE `order`.`user_id` = `user`.`id`
        LIMIT 1) AS `lastOrderStatus`
FROM `user`
WHERE `user`.`id` IN (
    SELECT `user_id` AS `userId`
    FROM `order`
    WHERE `order`.`status` = :status
)
```

Generated projections can be returned directly by no-argument `queryList()` or `queryOne()`, and they can be used as the next query source.

```kotlin group="Case 1-4" name="kotlin" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val rows = nameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .queryList()
```

Generated projection consumption is covered in {{ $.keyword("query/projection", ["Projection"]) }}. Derived query sources, scalar subqueries, predicate subqueries, and window-result filtering are covered in {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## Generate query conditions from object values with {{ $.title("where") }}()

An empty `where()` reads queryable non-null fields from the current KPojo object and generates equality conditions.

> **Warning**
> `select().queryList()` reads from the current table. Empty `where()` and `by { ... }` read object property values; `where { ... }` uses the lambda condition you write.

```kotlin group="Case 2" name="kotlin" icon="kotlin" {1,3}
val user: User = User(name = "Kronos", age = null)

val listOfUser: List<User> = user.select().where().queryList()
```

```sql group="Case 2" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `name` = :name
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE "name" = :name
```

```sql group="Case 2" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE "name" = :name
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE [name] = :name
```

```sql group="Case 2" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "name" = :name
```

Use `by { ... }` when only selected object fields should participate in the match.

## {{ $.title("by") }}Set query condition

The `by` method is used to set the query condition, at which point Kronos generates a query condition statement based on the fields set by the `by` method.

```kotlin group="Case 3" name="kotlin" icon="kotlin" {7}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

val kronos: User? = user.select().by { it.id }.queryOneOrNull()
```

```sql group="Case 3" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `id` = :id
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
```

```sql group="Case 3" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE [id] = :id
```

```sql group="Case 3" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
```

Use `[]` to select multiple condition fields:

```kotlin group="Case 3-1" name="kotlin" icon="kotlin" {7}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

val kronos: User? = user.select().by { [it.id, it.name] }.queryOneOrNull()
```

```sql group="Case 3-1" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `id` = :id and `name` = :name
```

```sql group="Case 3-1" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id and "name" = :name
```

```sql group="Case 3-1" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id and "name" = :name
```

```sql group="Case 3-1" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE [id] = :id and [name] = :name
```

```sql group="Case 3-1" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id and "name" = :name
```


## {{ $.title("where") }}Set the query conditions

`where()` generates query-by-example conditions from object values, and `where { ... }` generates a {{ $.keyword("query/conditions", ["conditional expression"]) }} from the lambda.

Subquery predicates such as `field in query`, `exists(query)`, `any(query)`, `all(query)`, and row-value tuple `IN` are covered in {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

```kotlin group="Case 4" name="kotlin" icon="kotlin" {7, 9-11}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

val kronos: User? = user.select().where { it.id }.queryOneOrNull()

val listOfUser: List<User> = user.select()
    .where { it.id > 1 && it.age < 20 }
    .queryList()
```

```sql group="Case 4" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `id` = :id

SELECT `id`, `name`, `age`
FROM `user`
WHERE `id` > :id
  and `age` < :age
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id

SELECT "id", "name", "age"
FROM "user"
WHERE "id" > :id
  and "age" < :age
```

```sql group="Case 4" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id

SELECT "id", "name", "age"
FROM "user"
WHERE "id" > :id
  and "age" < :age
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE [id] = :id

SELECT [id], [name], [age]
FROM [user]
WHERE [id] > :id and [age] < :age
```

```sql group="Case 4" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id

SELECT "id", "name", "age"
FROM "user"
WHERE "id" > :id
  and "age" < :age
```

Inside an explicit `where` block, `.eq` can expand the current KPojo object's field values into equality conditions, and you can combine those conditions with other expressions.

```kotlin group="Case 4-2" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos",
    status = 2
)

user.select().where { it.eq && it.status > 1 }.queryOneOrNull()
```

```sql group="Case 4-2" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `id` = :id
  and `name` = :name
  and `status` = :status
  and `status` > :statusMin
```

```sql group="Case 4-2" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "status" = :status
  and "status" > :statusMin
```

```sql group="Case 4-2" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "status" = :status
  and "status" > :statusMin
```

```sql group="Case 4-2" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE [id] = :id
  and [name] = :name
  and [status] = :status
  and [status] > :statusMin
```

```sql group="Case 4-2" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "status" = :status
  and "status" > :statusMin
```
Kronos provides the minus operator `-` to exclude fields from the explicit `.eq` expansion.

```kotlin group="Case 4-3" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos"
)

user.select().where { (it - it.name).eq && it.status == 1 }.queryOneOrNull()
```

```sql group="Case 4-3" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `id` = :id
  and `status` = :status
```

```sql group="Case 4-3" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "status" = :status
```

```sql group="Case 4-3" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "status" = :status
```

```sql group="Case 4-3" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE [id] = :id
  and [status] = :status
```

```sql group="Case 4-3" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "status" = :status
```

## {{ $.title("patch") }}Add parameters to custom query conditions

**Parameters**

{{ $.params([
    ["pairs", "<b>vararg</b>，Parameters for customizing query conditions", "Pair<String, Any?>"]
])}}

The `patch` method can be used to add parameters to a custom query condition when the `where` condition contains a custom Sql such as `where { "id = :id".asSql() }`.

```kotlin group="Case 19" name="kotlin" icon="kotlin" {1-4}
val user = User().select()
    .where { "id = :id".asSql() }
    .patch("id" to 1)
    .queryOneOrNull()
```

```sql group="Case 19" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE id = :id
```

```sql group="Case 19" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE id = :id
```

```sql group="Case 19" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE id = :id
```

```sql group="Case 19" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE id = :id
```

```sql group="Case 19" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE id = :id
```

## {{ $.title("orderBy") }}Set sorting conditions

The `orderBy` method is used to set the sort condition, at which point Kronos generates a sort condition statement based on the fields set by the `orderBy` method.

For the focused guide to sorting, `limit`, `page`, `withTotal`, `groupBy`, `having`, and aggregates, see {{ $.keyword("query/sorting-pagination-aggregation", ["Sorting, Pagination, and Aggregation"]) }}.

Use the `asc` method to set ascending sorting and the `desc` method to set descending sorting.

When no sort method is set, the default is ascending, e.g. `orderBy { it.id }` is equivalent to `orderBy { it.id.asc() }`.

```kotlin group="Case 5" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User().select()
    .orderBy { [it.id.desc(), it.name.asc()] }
    .queryList()
```

```sql group="Case 5" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
ORDER BY `id` DESC, `name` ASC
```

```sql group="Case 5" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
ORDER BY "id" DESC, "name" ASC
```

```sql group="Case 5" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
ORDER BY "id" DESC, "name" ASC
```

```sql group="Case 5" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
ORDER BY [id] DESC, [name] ASC
```

```sql group="Case 5" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
ORDER BY "id" DESC, "name" ASC
```

Selected aliases and scalar subqueries can also be used as sort items.

```kotlin group="Case 5-1" name="kotlin" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .orderBy { it.nameLength.desc() }
    .queryList()
```

```sql group="Case 5-1" name="Mysql" icon="mysql"
SELECT `id`, LENGTH(`name`) AS `nameLength`
FROM `user`
ORDER BY `nameLength` DESC
```

Scalar subquery sorting and derived-source paging are covered in {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## {{ $.title("groupBy")}}, {{ $.title("having") }}Set grouping and aggregation conditions

The `groupBy` method is used to set grouping conditions and the `having` method is used to set aggregation conditions.

Use `[]` when grouping by multiple fields, such as `groupBy { [it.age, it.status] }`.

For aggregate projections that become generated fields in the next query layer, see {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

```kotlin group="Case 6" name="kotlin" icon="kotlin" {1-4}
val listOfUser: List<User> = User().select()
    .groupBy { it.age }
    .having { it.age > 18 }
    .queryList()
```

```sql group="Case 6" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
GROUP BY `age`
HAVING `age` > :age
```

```sql group="Case 6" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
GROUP BY "age"
HAVING "age" > :age
```

```sql group="Case 6" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
GROUP BY "age"
HAVING "age" > :age
```

```sql group="Case 6" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
GROUP BY [age]
HAVING [age] > :age
```

```sql group="Case 6" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
GROUP BY "age"
HAVING "age" > :age
```

## {{ $.title("limit") }}Specify the maximum number of records to query

The `limit` method is used to set the maximum number of records to query, at which point Kronos generates a limit condition statement based on the number set by the `limit` method.

```kotlin group="Case 7" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User().select()
    .limit(10)
    .queryList()
```

```sql group="Case 7" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user` LIMIT 10
```

```sql group="Case 7" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user" LIMIT 10
```

```sql group="Case 7" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user" LIMIT 10
```

```sql group="Case 7" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
```

```sql group="Case 7" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE ROWNUM <= 10
```

## {{ $.title("lock") }}Set row lock

The `lock` method is used to set a **row lock** on a query, at which point Kronos will add a lock based on the lock type set by the `lock` method.

By default, Kronos will use **Exclusive Lock** (`SqlLock.Update()`) from **SqlLock**, which can be changed to **Shared Lock** from **SqlLock** by passing the parameter (`SqlLock.Share()`).

For **optimistic locks**, see {{$.keyword("query/locks", ["Advanced Usage", "Locking Mechanisms"])}}.

```kotlin group="Case 18" name="kotlin" icon="kotlin"
val listOfUser: List<User> = User().select()
    .lock() // SqlLock.Update()
    .queryList()

val listOfUser: List<User> = User().select()
    .lock(SqlLock.Share())
    .queryList()
```

```sql group="Case 18" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user` FOR UPDATE

SELECT `id`, `name`, `age`
FROM `user` LOCK IN SHARE MODE
```

```sql group="Case 18" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user" FOR UPDATE

SELECT "id", "name", "age"
FROM "user" FOR SHARE
```

```sql group="Case 18" name="SQLite" icon="sqlite"
# Don't support adding row locks to Sqlite because Sqlite itself doesn't have row locks.
```

```sql group="Case 18" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY ROWLOCK
```

```sql group="Case 18" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user" FOR UPDATE(NOWAIT)

SELECT "id", "name", "age"
FROM "user" LOCK IN SHARE MODE
```

## {{ $.title("page") }}, {{ $.title("withTotal") }}Query paging

The `page` method is used to specify paging queries, please note that the `page` method parameter starts from 1.

In different databases, paging queries have different syntaxes, Kronos generates paging queries based on different databases.

The `withTotal` method is used to query paging queries with total record counts.

> **Warning**
> After using the `page` method, the result of the query by default **will not** contain the total number of records. Use `withTotal()` when the total number is required.

```kotlin group="Case 8" name="kotlin" icon="kotlin" {1-4}
val (total, listOfUser) = User().select()
    .page(1, 10)
    .withTotal()
    .queryList()

// total: Int, listOfUser: List<User>
```

```sql group="Case 8" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user` LIMIT 10
OFFSET 0
```

```sql group="Case 8" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user" LIMIT 10
OFFSET 0
```

```sql group="Case 8" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user" LIMIT 10
OFFSET 0
```

```sql group="Case 8" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
```

```sql group="Case 8" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE ROWNUM <= 10
```

Generated projections can be paged after they enter the next query layer.

```kotlin group="Case 8-1" name="kotlin" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val (total, rows) = nameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .page(1, 10)
    .withTotal()
    .queryList()
```

```sql group="Case 8-1" name="Mysql" icon="mysql"
SELECT `q`.`id`, `q`.`nameLength`
FROM (
    SELECT `id`, LENGTH(`name`) AS `nameLength`
    FROM `user`
) AS `q`
WHERE `q`.`nameLength` > :nameLength
LIMIT 10
OFFSET 0
```

For projection visibility and total-count SQL shape, see {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## {{ $.title("db") }}Specify the database name(cross-database join)

The `db` method is used for cross-database queries, when Kronos sets the database to query based on the parameters of the `db` method.

```kotlin group="Case 20" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User()
    .db("user_database")
    .select { [it.id, it.username] }
    .queryList()

// Or the db method can be called directly after the select
// val listOfUser: List<User> = User()
//            .select { [it.id, it.username] }
//            .db("user_database")
//            .queryList()
```

```sql group="Case 20" name="Mysql" icon="mysql"
SELECT `id`, `username`
FROM `user_database`.`user`
```

```sql group="Case 20" name="PostgreSQL" icon="postgres"
SELECT "id", "username"
FROM "user_database"."user"
```

```sql group="Case 20" name="SQLite" icon="sqlite"
#Sqlite cross-database querying is not supported because Sqlite cross-database querying requires using the ATTACH command to attach another database to the current database, and then using the SELECT statement to query the data across databases.
```

```sql group="Case 20" name="SQLServer" icon="sqlserver"
SELECT [id], [username]
FROM [user_database].[user]
```

```sql group="Case 20" name="Oracle" icon="oracle"
#Oracle cross-library query function is not supported because Oracle cross-library query needs to be configured with dblink and query based on it.
```

## Result methods

Use a terminal result method at the end of a select chain to choose the returned shape.

```kotlin group="Result methods" name="kotlin" icon="kotlin"
val users: List<User> = User()
    .select()
    .queryList()

val row: User? = User()
    .select()
    .where { it.id == 1 }
    .queryOneOrNull()

val mapRows: List<Map<String, Any>> = User()
    .select { [it.id, it.name] }
    .query()
```

Result shapes, nullable single-row methods, generated projection results, pagination totals, and custom wrappers are covered in {{ $.keyword("query/result-methods", ["Result Methods"]) }}.

## {{ $.title("single") }}Add a single-row limit

Use `single()` when a query chain should add a one-row limit before the terminal method.

```kotlin group="Single" name="kotlin" icon="kotlin" {1}
val user: User = User().select().single().queryOne()
```

```sql group="Single" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user` LIMIT 1
```

## Cascading query

Cascading query loads related data through separate query steps. A common flow is to load parent rows first, then load child rows from those parent results.

The detailed cascade select API is covered in {{$.keyword("advanced/cascade-select", ["advanced usage","cascade query"])}}.

## Specify the data source to be used

In Kronos, we can use the specified data source to query the records in the database.

```kotlin group="Case 17" name="kotlin" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val listOfUser: List<User> = User().select().queryList(customWrapper)
```
