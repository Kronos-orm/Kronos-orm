# {{ NgDocPage.title }}

## Query all records

In Kronos, we can use the `KPojo.select()` method to query records in the database.

```kotlin group="Case 1" name="kotlin" icon="kotlin" {1}
val users: List<User> = User().select().queryList()
```

```sql group="Case 1" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user`
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user"
```

```sql group="Case 1" name="SQLite" icon="sqlite"
SELECT "id", "name", "age" FROM "user"
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user]
```

```sql group="Case 1" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user"
```

## Use <span style="color: #DD6666">select</span> to specify the query field

The `select` method is used to specify the query field. At this time, Kronos will generate a query field statement based on the field set by the `select` method.

You can pass in a string as the query field, use `+` to connect multiple fields, and the `as` method is used to set the field alias.

```kotlin group="Case 16" name="kotlin" icon="kotlin" {1-5}
val listOfUser: List<User> = User()
    .select { 
        it.id + it.name.`as`("username") + "count(*)".`as`("total") + "1"
    }
    .queryList()
```

```sql group="Case 16" name="Mysql" icon="mysql"
SELECT `id`, `name` AS `username`, count(*) AS `total`, 1 FROM `user`
```

```sql group="Case 16" name="PostgreSQL" icon="postgres"
SELECT "id", "name" AS "username", count(*) AS "total", 1 FROM "user"
```

```sql group="Case 16" name="SQLite" icon="sqlite"
SELECT "id", `name` AS `username`, count(*) AS `total`, 1 FROM `user`
```

```sql group="Case 16" name="SQLServer" icon="sqlserver"
SELECT [id], [name] AS [username], count(*) AS [total], 1 FROM [user]
```

```sql group="Case 16" name="Oracle" icon="oracle"
SELECT "id", "name" AS "username", count(*) AS "total", 1 FROM "user"
```


## Generate conditional statements and query records based on KPojo object values

When the `by` or `where` method is not used, Kronos will generate query conditional statements based on the values of the KPojo object.

> **Warning**
> `null` values will not be included in the query conditions, that is, `null` values will not be used to generate query conditional statements. If you need to query `null` values, please use the `where` method to set the query conditions.

```kotlin group="Case 2" name="kotlin" icon="kotlin" {1,3}
val user: User = User(name = "Kronos")

val listOfUser: List<User> = user.select().queryList()
```

```sql group="Case 2" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` WHERE `name` = :name
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" WHERE "name" = :name
```

```sql group="Case 2" name="SQLite" icon="sqlite"
SELECT "id", "name", "age" FROM "user" WHERE "name" = :name
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] WHERE [name] = :name
```

```sql group="Case 2" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" WHERE "name" = :name
```

## Use <span style="color: #DD6666">by</span> to set query conditions

The `by` method is used to set query conditions. At this time, Kronos will generate query condition statements based on the fields set by the `by` method.

```kotlin group="Case 3" name="kotlin" icon="kotlin" {7}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

val kronos: User? = user.select().by { it.id }.queryOneOrNull()
```

```sql group="Case 3" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` WHERE `id` = :id
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" WHERE "id" = :id
```

```sql group="Case 3" name="SQLite" icon="sqlite"
SELECT "id", "name", "age" FROM "user" WHERE "id" = :id
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] WHERE [id] = :id
```

```sql group="Case 3" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" WHERE "id" = :id
```

## Use <span style="color: #DD6666">where</span> to set query conditions

The `where` method is used to set query conditions. At this time, Kronos will generate query condition statements based on the fields set by the `where` method.

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
SELECT `id`, `name`, `age` FROM `user` WHERE `id` = :id

SELECT `id`, `name`, `age` FROM `user` WHERE `id` > :id and `age` < :age
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" WHERE "id" = :id

SELECT "id", "name", "age" FROM "user" WHERE "id" > :id and "age" < :age
```

```sql group="Case 4" name="SQLite" icon="sqlite"
SELECT "id", "name", "age" FROM "user" WHERE "id" = :id

SELECT "id", "name", "age" FROM "user" WHERE "id" > :id and "age" < :age
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] WHERE [id] = :id

SELECT [id], [name], [age] FROM [user] WHERE [id] > :id and [age] < :age
```

```sql group="Case 4" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" WHERE "id" = :id

SELECT "id", "name", "age" FROM "user" WHERE "id" > :id and "age" < :age
```

## Use <span style="color: #DD6666">patch</span> to add parameters to custom query conditions

When the `where` condition contains custom SQL such as: `where { "id = :id".asSql() }`, you can use the `patch` method to add parameters to the custom query condition.

```kotlin group="Case 19" name="kotlin" icon="kotlin" {1-3}
val user = User().select()
    .where { "id = :id" }
    .patch("id" to 1)
    .queryOneOrNull()
```

```sql group="Case 19" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` WHERE id = :id
```

```sql group="Case 19" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" WHERE id = :id
```

```sql group="Case 19" name="SQLite" icon="sqlite"
SELECT "id", "name", "age" FROM "user" WHERE id = :id
```

```sql group="Case 19" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] WHERE id = :id
```

```sql group="Case 19" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" WHERE id = :id
```


## Use <span style="color: #DD6666">orderBy</span> to set sorting conditions

The `orderBy` method is used to set sorting conditions. At this time, Kronos will generate sorting condition statements based on the fields set by the `orderBy` method.

Use the `asc` method to set ascending sorting, and use the `desc` method to set descending sorting.

When the sorting method is not set, the default is ascending sorting, such as: `orderBy { it.id }`.

```kotlin group="Case 5" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User().select()
                          .orderBy { it.id.desc() + it.name.asc() }
                          .queryList()
```

```sql group="Case 5" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` ORDER BY `id` DESC, `name` ASC
```

```sql group="Case 5" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" ORDER BY "id" DESC, "name" ASC
```

```sql group="Case 5" name="SQLite" icon="sqlite"
SELECT "id", "name", "age" FROM "user" ORDER BY "id" DESC, "name" ASC
```

```sql group="Case 5" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] ORDER BY [id] DESC, [name] ASC
```

```sql group="Case 5" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" ORDER BY "id" DESC, "name" ASC
```

## Use <span style="color: #DD6666">groupBy</span> and <span style="color: #DD6666">having</span> to set grouping and aggregation conditions

The `groupBy` method is used to set grouping conditions, and the `having` method is used to set aggregation conditions.

```kotlin group="Case 6" name="kotlin" icon="kotlin" {1-4}
val listOfUser: List<User> = User().select()
                          .groupBy { it.age }
                          .having { it.age > 18 }
                          .queryList()
```

```sql group="Case 6" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` GROUP BY `age` HAVING `age` > :age
```

```sql group="Case 6" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" GROUP BY "age" HAVING "age" > :age
```

```sql group="Case 6" name="SQLite" icon="sqlite"
SELECT "id", "name", "age" FROM "user" GROUP BY "age" HAVING "age" > :age
```

```sql group="Case 6" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] GROUP BY [age] HAVING [age] > :age
```

```sql group="Case 6" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" GROUP BY "age" HAVING "age" > :age
```

## Use <span style="color: #DD6666">limit</span> to set the number of query records

The `limit` method is used to set the number of query records. At this time, Kronos will generate query condition statements based on the number of records set by the `limit` method.

```kotlin group="Case 7" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User().select()
                          .limit(10)
                          .queryList()
```

```sql group="Case 7" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` LIMIT 10
```

```sql group="Case 7" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" LIMIT 10
```

```sql group="Case 7" name="SQLite" icon="sqlite"
SELECT "id", "name", "age" FROM "user" LIMIT 10
```

```sql group="Case 7" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
```

```sql group="Case 7" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" WHERE ROWNUM <= 10
```

## Use <span style="color: #DD6666">lock</span> to set a row lock during query

The `lock` method is used to set a row lock during query. At this time, Kronos will add a lock according to the lock type set by the `lock` method.

```kotlin group="Case 18" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User().select()
                          .lock()
//                          .lock(PessimisticLock.X)
                          .queryList()
                          
val listOfUser: List<User> = User().select()
                          .lock(PessimisticLock.S)
                          .queryList()
```

```sql group="Case 18" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` FOR UPDATE

SELECT `id`, `name`, `age` FROM `user` LOCK IN SHARE MODE
```

```sql group="Case 18" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" FOR UPDATE

SELECT "id", "name", "age" FROM "user" FOR SHARE
```

```sql group="Case 18" name="SQLite" icon="sqlite"
# Does not support adding row lock function to Sqlite because Sqlite itself does not have row lock function
```

```sql group="Case 18" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY ROWLOCK
```

```sql group="Case 18" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" FOR UPDATE(NOWAIT)

SELECT "id", "name", "age" FROM "user" LOCK IN SHARE MODE
```

## Use <span style="color: #DD6666">page</span> to set up paging query

The `page` method is used to set up paging query. Please note that the parameters of the `page` method start from 1.

The syntax of paging query is different in different databases. Kronos will generate corresponding paging query statements based on different databases.

> **Warning**
> After using the `page` method, the query result will **not** include the total number of records by default. If you need to query the total number of records, please use the <a href="/documentation/en/database/select-records#Use withtotal to query a paging query with the total number of records">withTotal method</a>.
```kotlin group="Case 8" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User().select()
                          .page(1, 10)
                          .queryList()
```

```sql group="Case 8" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` LIMIT 10 OFFSET 0
```

```sql group="Case 8" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" LIMIT 10 OFFSET 0
```

```sql group="Case 8" name="SQLite" icon="sqlite"
SELECT "id", "name", "age" FROM "user" LIMIT 10 OFFSET 0
```

```sql group="Case 8" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
```

```sql group="Case 8" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" WHERE ROWNUM <= 10
```

## Use <span style="color: #DD6666">db</span> to set the query database (cross-database query)

The `db` method is used for cross-database query. In this case, Kronos will set the query database according to the parameters of the `db` method.

```kotlin group="Case 20" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User()
            .db("user_database")
            .select { it.id + it.username }
            .queryList()

// Or the db method can be called directly after select
// val listOfUser: List<User> = User()
//            .select { it.id + it.username }
//            .db("user_database")
//            .queryList()
```

```sql group="Case 20" name="Mysql" icon="mysql"
SELECT `id`, `username` FROM `user_database`.`user` 
```

```sql group="Case 20" name="PostgreSQL" icon="postgres"
SELECT "id", "username" FROM "user_database"."user" 
```

```sql group="Case 20" name="SQLite" icon="sqlite"
# The Sqlite cross-database query function is not supported because Sqlite needs to configure dblink for cross-database query and query based on it
```

```sql group="Case 20" name="SQLServer" icon="sqlserver"
SELECT [id], [username] FROM [user_database].[user]
```

```sql group="Case 20" name="Oracle" icon="oracle"
# Oracle cross-database query function is not supported because Oracle cross-database query requires the configuration of dblink and query based on it
```

## Use the <span style="color: #DD6666">single</span> method to query a single record

The `single` method is actually an abbreviation of `limit(1)` and is used to query a single record.

```kotlin group="Case 9" name="kotlin" icon="kotlin" {1}
val user: User = User().select().single().queryOne()
```

```sql group="Case 9" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` LIMIT 1
```

```sql group="Case 9" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" LIMIT 1
```

```sql group="Case 9" name="SQLite" icon="sqlite"
SELECT "id", `name`, `age` FROM `user` LIMIT 1
```

```sql group="Case 9" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY
```

```sql group="Case 9" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" WHERE ROWNUM <= 1
```

## Use the <span style="color: #DD6666">query</span> method to query the Map list

The `query` method is used to execute a query and return a Map list.

```kotlin group="Case 10" name="demo" icon="kotlin" {1}
val listOfUser: List<Map<String, Any>> = User().select().query()
```

## Use queryList to query a list of a specified type
The queryList method is used to execute a query and return a list of a specified type. It can accept generic parameters.

When querying a single column, you can directly set the generic parameter to the type of the column, for example: queryList<Int>().

When querying multiple columns, you can set the generic parameter to a subclass of KPojo, for example: queryList<User>().

When no generic parameter is set, Kronos automatically converts the query result to the KPojo type of the query.

> **Note**
> queryList uses KCP to convert Map to KPojo. For details, see: Conversion between KPojo and Map

```kotlin group="Case 11" name="demo" icon="kotlin" {1,3}
val listOfUser: List<User> = User().select().queryList()

val listOfAnotherUser: List<AnotherUser> = User().select().queryList<AnotherUser>()
```

## Use <span style="color: #DD6666">queryMap</span> to query Map

The `queryMap` method is used to execute a query and return a Map. When the query result is empty, `null` is returned.

```kotlin group="Case 12" name="demo" icon="kotlin" {1}
val user: Map<String, Any> = User().select().queryMap()
```

## Use queryOne to query a single record

The queryOne method is used to execute a query and return a single record. When the query result is empty, an exception is thrown. Generic parameters can be accepted.

When querying a single column, the generic parameter can be directly set to the column type, for example: queryOne<Int>()`.

When querying multiple columns, the generic parameter can be set to a subclass of KPojo, for example: queryOne<User>()`.

When the generic parameter is not set, Kronos automatically converts the query result to the KPojo type of the query.

> **Note**
> queryOne uses KCP to convert Map to KPojo. For details, see: KPojo and Map conversion

```kotlin group="Case 13" name="demo" icon="kotlin" {1}
val user: User = User().select().queryOne()
```

## Use queryOneOrNull to query a single record (optional)

Similar to the queryOne method, the queryOneOrNull method is used to execute a query and return a single record. When the query result is empty, it returns null and can accept generic parameters.

When querying a single column, you can directly set the generic parameter to the column type, for example: queryOneOrNull<Int>()`.

When querying multiple columns, you can set the generic parameter to a subclass of KPojo, for example: queryOneOrNull<User>()`.

When the generic parameter is not set, Kronos automatically converts the query result to the KPojo type of the query.

> **Note**
> queryOneOrNull uses KCP to implement Map conversion to KPojo. For details, see: KPojo and Map conversion

```kotlin group="Case 14" name="demo" icon="kotlin" {1}
val user: User? = User().select().queryOneOrNull()
```

## Use <span style="color: #DD6666">withTotal</span> to query a paginated query with a total number of records

The `withTotal` method is used to query a paginated query with a total number of records. In this case, Kronos will include the total number of records in the query results.

The withTotal method returns a `PageClause` object, and you can use the `query`, `queryList` and other methods to get the query results.

```kotlin group="Case 15" name="demo" icon="kotlin" {1-4}
val (total, listOfUser) = User().select()
                        .page(1, 10)
                        .withTotal()
                        .queryList()
                          
// total: Int, listOfUser: List<User>
```

## Use a specified data source

In Kronos, we can use a specified data source to query records in the database.

```kotlin group="Case 17" name="kotlin" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val listOfUser: List<User> = User().select().queryList(customWrapper)
```
