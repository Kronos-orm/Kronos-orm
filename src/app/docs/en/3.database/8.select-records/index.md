{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 查询所有记录

在Kronos中，我们可以使用`KPojo.select()`方法用于查询数据库中的记录。

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

## {{ $.title("select") }}指定查询字段

`select`方法用于指定查询字段，此时Kronos会根据`select`方法设置的字段生成查询字段语句。

可以传入字符串作为查询字段，使用`+`连接多个字段，`as`方法用于设置字段别名。

可以使用`+`连接多个查询字段。

```kotlin group="Case 1-1" name="kotlin" icon="kotlin" {1-5}
val listOfUser: List<User> = User()
    .select {
        it.id + it.name.`as`("username") + "count(*) as total" + "1"
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

### 查询全部字段、排除部分列

可以传入`KPojo`查询全部列，并使用`+`、`-`在全部列基础上增加减去部分列。

> **Note**
> 请注意，`-`必须用在`KPojo`之后。

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

## 根据KPojo对象值生成条件语句并查询记录

当未使用`by`或`where`方法时，Kronos会根据KPojo对象的值生成查询条件语句。

> **Warning**
> 当KPojo对象的字段值为`null`时，该字段不会生成查询条件，若需要查询字段值为`null`的记录，请使用`where`方法指定。

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

## {{ $.title("by") }}设置查询条件

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

多个字段可以使用`+`连接：

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


## {{ $.title("where") }}设置查询条件

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

可以对查询对象执行`.eq`函数，这样您可以以根据KPojo对象值生成条件语句为基础，添加其他查询条件：

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
Kronos提供了减号运算符`-`用来指定不需要自动生成条件语句的列。

```kotlin group="Case 4-3" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos"
)

user.select().where { (it - it.status).eq && it.status == 1 }.queryOneOrNull()
```

```sql group="Case 4-3" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `id` = :id
  and `name` = :name
  and `status` > :statusMin
```

```sql group="Case 4-3" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "status" > :statusMin
```

```sql group="Case 4-3" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "status" > :statusMin
```

```sql group="Case 4-3" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE [id] = :id
  and [name] = :name
  and [status] > :statusMin
```

```sql group="Case 4-3" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = :id
  and "name" = :name
  and "status" > :statusMin
```

## {{ $.title("patch") }}为自定义查询条件添加参数

**参数**:

{{ $.params([
    ["pairs", "<b>vararg</b>，自定义查询条件的参数", "Pair<String, Any?>"]
])}}

当`where`条件内包含自定义Sql如：`where { "id = :id".asSql() }`时，可以使用`patch`方法为自定义查询条件添加参数。

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

## {{ $.title("orderBy") }}设置排序条件

`orderBy`方法用于设置排序条件，此时Kronos会根据`orderBy`方法设置的字段生成排序条件语句。

使用`asc`方法设置升序排序，使用`desc`方法设置降序排序。

当不设置排序方法时，默认为升序排序，如：`orderBy { it.id }`。

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

## {{ $.title("groupBy")}}, {{ $.title("having") }}设置分组和聚合条件

`groupBy`方法用于设置分组条件，`having`方法用于设置聚合条件。

如需根据多个字段分组，可以使用`+`连接多个字段。

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

## {{ $.title("limit") }}设置查询记录数

`limit`方法用于设置查询记录数，此时Kronos会根据`limit`方法设置的记录数生成查询条件语句。

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

## {{ $.title("lock") }}设置查询时行锁

`lock`方法用于设置查询时**行锁**，此时Kronos会根据`lock`方法设置的锁类型进行锁的添加。

默认情况下，Kronos会使用**悲观锁**中的**排他锁**（`PessimisticLock.X`）， 可以通过传入参数(`PessimisticLock.S`)改用**悲观锁**中的**共享锁**。

如需使用**乐观锁**，请参考{{$.keyword("advanced/some-locks", ["进阶用法","加锁机制"])}}。

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
#不支持对Sqlite添加行锁功能因为Sqlite本身没有行锁功能
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

## {{ $.title("page") }}, {{ $.title("withTotal") }}设置分页查询

`page`方法用于设置分页查询，请注意，`page`方法的参数从1开始。

在不同的数据库中，分页查询的语法有所不同，Kronos会根据不同的数据库生成相应的分页查询语句。

`withTotal`方法用于查询带有总记录数的分页查询。

> **Warning**
> 使用`page`方法后，查询的结果默认**不会**包含总记录数，若需要查询总记录数，请务必使用withTotal方法</a>。

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

## {{ $.title("db") }}跨库查询

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
SELECT `id`, `username`
FROM `user_database`.`user` 
```

```sql group="Case 20" name="PostgreSQL" icon="postgres"
SELECT "id", "username"
FROM "user_database"."user" 
```

```sql group="Case 20" name="SQLite" icon="sqlite"
#不支持Sqlite跨库查询功能因为Sqlite进行跨库查询需要使用ATTACH命令将另一个数据库附加到当前数据库中
，
然后使用SELECT语句来跨数据库查询数据
```

```sql group="Case 20" name="SQLServer" icon="sqlserver"
SELECT [id], [username]
FROM [user_database].[user]
```

```sql group="Case 20" name="Oracle" icon="oracle"
#不支持Oracle跨库查询功能因为Oracle进行跨库查询需要配置dblink并以此为基础进行查询
```

## {{ $.title("single") }}查询单条记录

`single`方法实际上是`limit(1)`的简写，用于查询单条记录。

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

## {{ $.title("query") }}查询Map列表

`query`方法用于执行查询并返回Map列表。

```kotlin group="Case 10" name="demo" icon="kotlin" {1}
val listOfUser: List<Map<String, Any>> = User().select().query()
```

## {{ $.title("queryList") }}查询指定类型列表

`queryList`方法用于执行查询并返回指定类型列表，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryList<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryList<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为查询的KPojo类型。

> **Note**
> queryList使用kronos-compiler-plugin实现Map转换为KPojo，详见：KPojo与Map互相转换

```kotlin group="Case 11" name="demo" icon="kotlin" {1,3,5}
val listOfUser: List<User> = User().select().queryList()

val listOfAnotherUser: List<AnotherUser> = User().select().queryList<AnotherUser>()

val listOfInt: List<Int> = User().select{ it.id }.queryList<Int>()
```

## {{ $.title("queryMap") }}查询单条记录Map

`queryMap`方法用于查询单条记录并返回Map，当查询结果为空时，抛出异常。

```kotlin group="Case 12" name="demo" icon="kotlin" {1}
val user: Map<String, Any> = User().select().queryMap()
```

## {{ $.title("queryMapOrNull") }}查询单条记录Map（可空）

`queryMapOrNull`方法用于查询单条记录并返回Map，当查询结果为空时，返回`null`。

```kotlin group="Case 15" name="demo" icon="kotlin" {1}
val user: Map<String, Any>? = User().select().queryMapOrNull()
```

## {{ $.title("queryOne") }}查询单条记录

`queryOne`方法用于执行查询并返回单条记录，当查询结果为空时，抛出异常，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOne<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOne<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为查询的KPojo类型。

> **Note**
> queryOne使用KCP实现Map转换为KPojo，详见：KPojo与Map互相转换

```kotlin group="Case 13" name="demo" icon="kotlin" {1}
val user: User = User().select().queryOne()
```

## {{ $.title("queryOneOrNull") }}查询单条记录（可空）

和`queryOne`方法类似，`queryOneOrNull`方法用于执行查询并返回单条记录，当查询结果为空时，返回`null`，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOneOrNull<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOneOrNull<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为查询的KPojo类型。

> **Note**
> queryOneOrNull使用KCP实现Map转换为KPojo，详见：KPojo与Map互相转换

```kotlin group="Case 14" name="demo" icon="kotlin" {1}
val user: User? = User().select().queryOneOrNull()
```

## 级联查询

请参考{{$.keyword("advanced/cascade-query", ["进阶用法","级联查询"])}}。

## 使用指定的数据源

在Kronos中，我们可以使用指定的数据源查询数据库中的记录。

```kotlin group="Case 17" name="kotlin" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val listOfUser: List<User> = User().select().queryList(customWrapper)
```
