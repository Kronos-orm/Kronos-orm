# {{ NgDocPage.title }}

## 查询所有记录

在Kronos中，我们可以使用`KPojo.select()`方法用于查询数据库中的记录。

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

## 使用<span style="color: #DD6666">select</span>指定查询字段

`select`方法用于指定查询字段，此时Kronos会根据`select`方法设置的字段生成查询字段语句。

可以传入字符串作为查询字段，使用`+`连接多个字段，`as`方法用于设置字段别名。

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


## 根据KPojo对象值生成条件语句并查询记录

当未使用`by`或`where`方法时，Kronos会根据KPojo对象的值生成查询条件语句。

> **Warning**
> `null`值不会被包含在查询条件中，即`null`值不会被用于生成查询条件语句，若需要查询`null`值，请使用`where`方法设置查询条件。

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

## 使用<span style="color: #DD6666">by</span>设置查询条件

`by`方法用于设置查询条件，此时Kronos会根据`by`方法设置的字段生成查询条件语句。

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

## 使用<span style="color: #DD6666">where</span>设置查询条件

`where`方法用于设置查询条件，此时Kronos会根据`where`方法设置的字段生成查询条件语句。

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

## 使用<span style="color: #DD6666">patch</span>为自定义查询条件添加参数

当`where`条件内包含自定义Sql如：`where { "id = :id".asSql() }`时，可以使用`patch`方法为自定义查询条件添加参数。

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
    

## 使用<span style="color: #DD6666">orderBy</span>设置排序条件

`orderBy`方法用于设置排序条件，此时Kronos会根据`orderBy`方法设置的字段生成排序条件语句。

使用`asc`方法设置升序排序，使用`desc`方法设置降序排序。

当不设置排序方法时，默认为升序排序，如：`orderBy { it.id }`。

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

## 使用<span style="color: #DD6666">groupBy</span>和<span style="color: #DD6666">having</span>设置分组和聚合条件

`groupBy`方法用于设置分组条件，`having`方法用于设置聚合条件。

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

## 使用<span style="color: #DD6666">limit</span>设置查询记录数

`limit`方法用于设置查询记录数，此时Kronos会根据`limit`方法设置的记录数生成查询条件语句。

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

## 使用<span style="color: #DD6666">lock</span>设置查询时行锁

`lock`方法用于设置查询时行锁，此时Kronos会根据`lock`方法设置的锁类型进行锁的添加。

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
# 不支持对Sqlite添加行锁功能因为Sqlite本身没有行锁功能
```

```sql group="Case 18" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY ROWLOCK
```

```sql group="Case 18" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" FOR UPDATE(NOWAIT)

SELECT "id", "name", "age" FROM "user" LOCK IN SHARE MODE
```

## 使用<span style="color: #DD6666">page</span>设置分页查询

`page`方法用于设置分页查询，请注意，`page`方法的参数从1开始。

在不同的数据库中，分页查询的语法有所不同，Kronos会根据不同的数据库生成相应的分页查询语句。

> **Warning**
> 使用`page`方法后，查询的结果默认**不会**包含总记录数，若需要查询总记录数，请使用<a href="/documentation/database/select-records#使用withtotal查询带有总记录数的分页查询">withTotal方法</a>。

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

## 使用<span style="color: #DD6666">db</span>设置查询数据库（跨库查询）

`db`方法用于跨库查询，此时Kronos会根据`db`方法的参数设置查询的数据库。

```kotlin group="Case 20" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User()
            .db("user_database")
            .select { it.id + it.username }
            .queryList()

// 或者db方法可以直接在select后面调用
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
# 不支持Sqlite跨库查询功能因为Sqlite进行跨库查询需要配置dblink并以此为基础进行查询
```

```sql group="Case 20" name="SQLServer" icon="sqlserver"
SELECT [id], [username] FROM [user_database].[user]
```

```sql group="Case 20" name="Oracle" icon="oracle"
# 不支持Oracle跨库查询功能因为Oracle进行跨库查询需要配置dblink并以此为基础进行查询
```

## 使用<span style="color: #DD6666">single</span>方法查询单条记录

`single`方法实际上是`limit(1)`的简写，用于查询单条记录。

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

## 使用<span style="color: #DD6666">query</span>方法查询Map列表

`query`方法用于执行查询并返回Map列表。

```kotlin group="Case 10" name="demo" icon="kotlin" {1}
val listOfUser: List<Map<String, Any>> = User().select().query()
```

## 使用<span style="color: #DD6666">queryList</span>查询指定类型列表
`queryList`方法用于执行查询并返回指定类型列表，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryList<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryList<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为查询的KPojo类型。

> **Note**
> queryList使用KCP实现Map转换为KPojo，详见：KPojo与Map互相转换

```kotlin group="Case 11" name="demo" icon="kotlin" {1,3}
val listOfUser: List<User> = User().select().queryList()

val listOfAnotherUser: List<AnotherUser> = User().select().queryList<AnotherUser>()
```

## 使用<span style="color: #DD6666">queryMap</span>查询Map

`queryMap`方法用于执行查询并返回Map，当查询结果为空时，返回`null`。

```kotlin group="Case 12" name="demo" icon="kotlin" {1}
val user: Map<String, Any> = User().select().queryMap()
```

## 使用<span style="color: #DD6666">queryOne</span>查询单条记录

`queryOne`方法用于执行查询并返回单条记录，当查询结果为空时，抛出异常，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOne<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOne<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为查询的KPojo类型。

> **Note**
> queryOne使用KCP实现Map转换为KPojo，详见：KPojo与Map互相转换

```kotlin group="Case 13" name="demo" icon="kotlin" {1}
val user: User = User().select().queryOne()
```

## 使用<span style="color: #DD6666">queryOneOrNull</span>查询单条记录（可空）

和`queryOne`方法类似，`queryOneOrNull`方法用于执行查询并返回单条记录，当查询结果为空时，返回`null`，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOneOrNull<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOneOrNull<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为查询的KPojo类型。

> **Note**
> queryOneOrNull使用KCP实现Map转换为KPojo，详见：KPojo与Map互相转换

```kotlin group="Case 14" name="demo" icon="kotlin" {1}
val user: User? = User().select().queryOneOrNull()
```

## 使用<span style="color: #DD6666">withTotal</span>查询带有总记录数的分页查询

`withTotal`方法用于查询带有总记录数的分页查询，此时Kronos会在查询结果中包含总记录数。

withTotal方法返回一个`PageClause`对象，您可以通过`query`、`queryList`等方法获取查询结果。

```kotlin group="Case 15" name="demo" icon="kotlin" {1-4}
val (total, listOfUser) = User().select()
                        .page(1, 10)
                        .withTotal()
                        .queryList()
                          
// total: Int, listOfUser: List<User>
```

## 使用指定的数据源

在Kronos中，我们可以使用指定的数据源查询数据库中的记录。

```kotlin group="Case 17" name="kotlin" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val listOfUser: List<User> = User().select().queryList(customWrapper)
```
