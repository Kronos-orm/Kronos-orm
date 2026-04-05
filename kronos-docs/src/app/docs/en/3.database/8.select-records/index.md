{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.title("select") }}Select records

In Kronos, we can use `KPojo.select()` method to query database records.

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

Multiple fields are concatenated using `+` and the `as_` method is used to set field aliases.

```kotlin group="Case 1-1" name="kotlin" icon="kotlin" {1-5}
val listOfUser: List<User> = User()
    .select {
        it.id + it.name.as_("username") + "count(*) as total" + "1"
    }
    .queryList()
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

You can pass `KPojo` to query all columns, and use `+`, `-` to add and subtract some columns from all columns.

> **Note**
> Please note that `-` must be used after `KPojo`.

```kotlin group="Case 1-2" name="kotlin" icon="kotlin"
val listOfUser: List<User> = User()
    .select { it - it.id + "count(*) as total" }
    .queryList()
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

## Automatic query condition generation

When the `by` or `where` methods are not used, Kronos generates a query condition statement based on the value of the KPojo object.

> **Warning**
> When the field value of KPojo object is `null`, the field will not generate the query condition, if you need to query the record whose field value is `null`, please use `where { it.prop.isNull }` method to specify.

```kotlin group="Case 2" name="kotlin" icon="kotlin" {1,3}
val user: User = User(name = "Kronos")

val listOfUser: List<User> = user.select().queryList()
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
WHERE [name] = : name
```

```sql group="Case 2" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "name" = :name
```

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

Multiple fields can be concatenated using `+`:

```kotlin group="Case 3-1" name="kotlin" icon="kotlin" {7}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

val kronos: User? = user.select().by { it.id + it.name }.queryOneOrNull()
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

The `where` method is used to set the query conditions, at which point Kronos generates a query based on the fields set by the `where` method {{ $.keyword("concept/where-having-on-clause", ["Criteria Conditional Statement"]) }}.

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

The `.eq` function can be executed on the query object so that you can add other query conditions based on generating conditional statements based on KPojo object values:

```kotlin group="Case 4-2" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos"
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
  and [name] = : name
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
Kronos provides the minus operator `-` to specify columns that do not require automatic generation of conditional statements.

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
  and `status` > :statusMin
```

```sql group="Case 4-3" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "status" > :statusMin
```

```sql group="Case 4-3" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "status" > :statusMin
```

```sql group="Case 4-3" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE [id] = :id
  and [status] > :statusMin
```

```sql group="Case 4-3" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "status" > :statusMin
```

## {{ $.title("patch") }}Add parameters to custom query conditions

**Parameters**

{{ $.params([
    ["pairs", "<b>vararg</b>，Parameters for customizing query conditions", "Pair<String, Any?>"]
])}}

The `patch` method can be used to add parameters to a custom query condition when the `where` condition contains a custom Sql such as `where { "id = :id".asSql() }`.

```kotlin group="Case 19" name="kotlin" icon="kotlin" {1-4}
val user = User().select()
    .where { "id = :id" }
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

Use the `asc` method to set ascending sorting and the `desc` method to set descending sorting.

When no sort method is set, the default is ascending, e.g. `orderBy { it.id }` is equivalent to `orderBy { it.id.asc() }`.

```kotlin group="Case 5" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User().select()
    .orderBy { it.id.desc() + it.name.asc() }
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

## {{ $.title("groupBy")}}, {{ $.title("having") }}Set grouping and aggregation conditions

The `groupBy` method is used to set grouping conditions and the `having` method is used to set aggregation conditions.

For grouping based on multiple fields, you can use `+` to concatenate multiple fields.

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

By default, Kronos will use **Exclusive Lock** (`PessimisticLock.X`) from **PessimisticLock**, which can be changed to **Shared Lock** from **PessimisticLock** by passing the parameter (`PessimisticLock.S`).

For **optimistic locks**, see {{$.keyword("advanced/some-locks", ["Advanced Usage", "Locking Mechanisms"])}}.

```kotlin group="Case 18" name="kotlin" icon="kotlin"
val listOfUser: List<User> = User().select()
    .lock() // PessimisticLock.X
    .queryList()

val listOfUser: List<User> = User().select()
    .lock(PessimisticLock.S)
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
> After using the `page` method, the result of the query by default **will not** contain the total number of records, if you need to query the total number of records, be sure to use the withTotal method</a>.

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

## {{ $.title("db") }}Specify the database name(cross-database join)

The `db` method is used for cross-database queries, when Kronos sets the database to query based on the parameters of the `db` method.

```kotlin group="Case 20" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User()
    .db("user_database")
    .select { it.id + it.username }
    .queryList()

// Or the db method can be called directly after the select
// val listOfUser: List<User> = User()
//            .select { it.id + it.username }
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

## {{ $.title("single") }}Query single record(Not recommended)

The `single` method is used to query a single record, if the query result is empty, it throws an exception.

It is recommended to use `queryOne`, `queryOneOrNull`, `queryMap`, `queryMapOrNull` methods directly to query a single record.
We have automatically added the `limit(1)` statement for you.

```kotlin group="Case 9" name="kotlin" icon="kotlin" {1}
val user: User = User().select().single().queryOne()
```

```sql group="Case 9" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user` LIMIT 1
```

```sql group="Case 9" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user" LIMIT 1
```

```sql group="Case 9" name="SQLite" icon="sqlite"
SELECT "id", `name`, `age`
FROM `user` LIMIT 1
```

```sql group="Case 9" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY
```

```sql group="Case 9" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE ROWNUM <= 1
```

## {{ $.title("query") }}Query map list

The `query` method is used to execute queries and return map list.

```kotlin group="Case 10" name="demo" icon="kotlin" {1}
val listOfUser: List<Map<String, Any>> = User().select().query()
```

## {{ $.title("queryList") }}Query specified type list

The `queryList` method is used to execute queries and return specified type list, can receive generic parameters.

When querying a single column, you can directly set the generic parameter to the column type, for example: `queryList<Int>()`.

Query multiple columns, you can set the generic parameter to the KPojo subclass, for example: `queryList<User>()`.

When the generic parameter is not set, Kronos will automatically convert the query result to the type of the main table.

> **Note**
> queryList uses kronos-compiler-plugin for reflectionless generic instantiation, see: Dynamic Instantiation of KPojo, Dynamic Accessor for KPojo Properties (compile-time generation).

```kotlin group="Case 11" name="demo" icon="kotlin" {1,3,5}
val listOfUser: List<User> = User().select().queryList()

val listOfAnotherUser: List<AnotherUser> = User().select().queryList<AnotherUser>()

val listOfInt: List<Int> = User().select{ it.id }.queryList<Int>()
```

## {{ $.title("queryMap") }}Query single record Map

The `queryMap` method is used to query a single record and return Map. When the query result is empty, it throws an exception.

```kotlin group="Case 12" name="demo" icon="kotlin" {1}
val user: Map<String, Any> = User().select().queryMap()
```

## {{ $.title("queryMapOrNull") }}Query single record Map(Nullable)

The `queryMapOrNull` method is used to query a single record and return Map. When the query result is empty, it returns null.

```kotlin group="Case 15" name="demo" icon="kotlin" {1}
val user: Map<String, Any>? = User().select().queryMapOrNull()
```

## {{ $.title("queryOne") }}Query single record specified type

The `queryOne` method is used to execute queries and return a single record, when the query result is empty, it throws an exception, can receive generic parameters.

When querying a single column, you can directly set the generic parameter to the column type, for example: `queryOne<Int>()`.

Query multiple columns, you can set the generic parameter to the KPojo subclass, for example: `queryOne<User>()`.

When the generic parameter is not set, Kronos will automatically convert the query result to the type of the main table.

> **Note**
> queryOne uses kronos-compiler-plugin for reflectionless generic instantiation, see: Dynamic Instantiation of KPojo, Dynamic Accessor for KPojo Properties (compile-time generation).

```kotlin group="Case 13" name="demo" icon="kotlin" {1}
val user: User = User().select().queryOne()
```

## {{ $.title("queryOneOrNull") }}Query single record specified type (Nullable)

The `queryOneOrNull` method is used to execute queries and return a single record, when the query result is empty, it returns `null`, can receive generic parameters.

When querying a single column, you can directly set the generic parameter to the column type, for example: `queryOneOrNull<Int>()`.

Query multiple columns, you can set the generic parameter to the KPojo subclass, for example: `queryOneOrNull<User>()`.

When the generic parameter is not set, Kronos will automatically convert the query result to the type of the main table.

> **Note**
> queryOneOrNull uses kronos-compiler-plugin for reflectionless generic instantiation, see: Dynamic Instantiation of KPojo, Dynamic Accessor for KPojo Properties (compile-time generation).

```kotlin group="Case 14" name="demo" icon="kotlin" {1}
val user: User? = User().select().queryOneOrNull()
```

## Cascading query

Cascading query is a process of obtaining associated data from multiple data sources or tables by associating step by step and level by level, which usually requires multiple queries and subsequent queries depend on the results of the previous ones.

Cascading query and continuous table query are different:
- Cascading query is a step-by-step execution of multiple independent queries (e.g., first check the main table data, and then use the results to check the associated sub-tables), suitable for loosely associated or cross-data source scenarios
- A join query directly associates multiple table data through a single query, relying on the database's relational capabilities, which is more efficient; however, it requires the table structures to be closely related and within the same data source, suitable for strictly related scenarios.

Please refer to {{$.keyword("advanced/cascade-query", ["advanced usage","cascade query"])}}.

## Specify the data source to be used

In Kronos, we can use the specified data source to query the records in the database.

```kotlin group="Case 17" name="kotlin" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val listOfUser: List<User> = User().select().queryList(customWrapper)
```
