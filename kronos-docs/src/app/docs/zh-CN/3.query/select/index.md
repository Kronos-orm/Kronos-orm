{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.title("select") }}查询记录

在Kronos中，我们可以使用`KPojo.select()`方法用于查询数据库中的记录。

字段列表、alias、生成投影对象和 join 投影示例见 {{ $.keyword("query/projection", ["投影"]) }}。

```kotlin group="Case 1" name="kotlin" icon="kotlin" {1}
val users: List<User> = User().select().toList()
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

Kronos支持直接传入字符串作为查询字段。

使用`[]`书写多个字段，`alias`方法用于设置字段别名。

字段投影、alias、生成结果形态和 DTO 消费方式见 {{ $.keyword("query/projection", ["投影"]) }}。派生查询源、标量子查询、谓词子查询和窗口函数结果过滤见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

```kotlin group="Case 1-1" name="kotlin" icon="kotlin" {1-5}
val rows: List<Map<String, Any?>> = User()
    .select {
        [it.id, it.name.alias("username"), "count(*) as total", "1"]
    }
    .toMapList()
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

在显式投影中返回 `it` 会展开当前 KPojo 的全部数据库列；`it` 可以直接返回，也可以放进 `[]`。使用 `-` 可以从完整投影中排除列。

> **Note**
> `select()` 返回源 KPojo 类型；`select { it }` 和 `select { [it] }` 返回包含全部列的生成投影类型。`-` 必须用在 KPojo 之后。

```kotlin name="kotlin" icon="kotlin"
val allDirect = User().select { it }.toList()
val allInList = User().select { [it] }.toList()
val withoutId = User().select { it - it.id }.toList()
val withoutIdInList = User().select { [it - it.id] }.toList()

val withAlias = User()
    .select { [it, it.id.alias("sourceId")] }
    .toList()
```

`[]` 是推荐写法。`listOf`、`arrayOf`、`mutableListOf` 和 `setOf` 也可以构造同样的投影列表，例如 `select { arrayOf<Any?>(it, it.id.alias("sourceId")) }`。完整矩阵和生成结果属性见 {{ $.keyword("query/projection", ["投影"]) }}。

```kotlin group="Case 1-2" name="kotlin" icon="kotlin"
val rows: List<Map<String, Any?>> = User()
    .select { [it - it.id, "count(*) as total"] }
    .toMapList()
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

## 使用子查询和生成投影

Select 字段可以包含标量子查询，`where` 可以通过 `in`、`exists`、`any`、`all` 和 row-value tuple 条件消费 selectable subquery。

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
    .toList()
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

生成投影可以由无参 `toList()` 或 `first()` 直接返回，也可以作为下一层查询源使用。

```kotlin group="Case 1-4" name="kotlin" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val rows = nameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .toList()
```

生成投影消费方式见 {{ $.keyword("query/projection", ["投影"]) }}。派生查询源、标量子查询、谓词子查询和窗口函数结果过滤见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## 使用 {{ $.title("where") }}() 根据对象值生成查询条件

空 `where()` 会读取当前 KPojo 对象中的可查询非空字段，并生成等值查询条件。

> **Warning**
> `select().toList()` 按当前表读取数据。空 `where()` 和 `by { ... }` 会读取对象字段值；`where { ... }` 使用你在 lambda 中写出的条件。

```kotlin group="Case 2" name="kotlin" icon="kotlin" {1,3}
val user: User = User(name = "Kronos", age = null)

val listOfUser: List<User> = user.select().where().toList()
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

若只想使用对象中的部分字段参与匹配，使用 `by { ... }` 选择字段。

## {{ $.title("by") }}设置查询条件

`by`方法用于设置查询条件，此时Kronos会根据`by`方法设置的字段生成查询条件语句。

```kotlin group="Case 3" name="kotlin" icon="kotlin" {7}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

val kronos: User? = user.select().by { it.id }.firstOrNull()
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

多个条件字段使用`[]`书写：

```kotlin group="Case 3-1" name="kotlin" icon="kotlin" {7}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

val kronos: User? = user.select().by { [it.id, it.name] }.firstOrNull()
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

`where()` 按对象值生成 query-by-example 条件，`where { ... }` 按 lambda 表达式生成 {{ $.keyword("query/conditions", ["Criteria条件语句"]) }}。

`field in query`、`exists(query)`、`any(query)`、`all(query)` 和 row-value tuple `IN` 等子查询谓词见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

```kotlin group="Case 4" name="kotlin" icon="kotlin" {7, 9-11}
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

val kronos: User? = user.select().where { it.id }.firstOrNull()

val listOfUser: List<User> = user.select()
    .where { it.id > 1 && it.age < 20 }
    .toList()
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

在显式`where`块中，可以使用`.eq`将当前 KPojo 对象的字段值展开为等值条件，并继续组合其他条件表达式：

```kotlin group="Case 4-2" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos",
    status = 2
)

user.select().where { it.eq && it.status > 1 }.firstOrNull()
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
Kronos提供了减号运算符`-`，用于在显式`.eq`展开时排除指定字段。

```kotlin group="Case 4-3" name="kotlin" icon="kotlin" {6}
val user: User = User(
    id = 1,
    name = "Kronos"
)

user.select().where { (it - it.name).eq && it.status == 1 }.firstOrNull()
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

## {{ $.title("patch") }}为自定义查询条件添加参数

**参数**:

{{ $.params([
    ["pairs", "<b>vararg</b>，自定义查询条件的参数", "Pair<String, Any?>"]
])}}

当`where`条件内包含自定义Sql如：`where { "id = :id".asSql() }`时，可以使用`patch`方法为自定义查询条件添加参数。

```kotlin group="Case 19" name="kotlin" icon="kotlin" {1-4}
val user = User().select()
    .where { "id = :id".asSql() }
    .patch("id" to 1)
    .firstOrNull()
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

排序、`limit`、`page`、`withTotal`、`groupBy`、`having` 和聚合的集中说明见 {{ $.keyword("query/sorting-pagination-aggregation", ["排序、分页与聚合"]) }}。

使用`asc`方法设置升序排序，使用`desc`方法设置降序排序。

当不设置排序方法时，默认为升序排序，如：`orderBy { it.id }`等同于`orderBy { it.id.asc() }`

```kotlin group="Case 5" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User().select()
    .orderBy { [it.id.desc(), it.name.asc()] }
    .toList()
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

selected alias 和标量子查询也可以作为排序项。

```kotlin group="Case 5-1" name="kotlin" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .orderBy { it.nameLength.desc() }
    .toList()
```

```sql group="Case 5-1" name="Mysql" icon="mysql"
SELECT `id`, LENGTH(`name`) AS `nameLength`
FROM `user`
ORDER BY `nameLength` DESC
```

标量子查询排序和派生来源分页见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## {{ $.title("groupBy")}}, {{ $.title("having") }}设置分组和聚合条件

`groupBy`方法用于设置分组条件，`having`方法用于设置聚合条件。

如需根据多个字段分组，使用 `[]`，例如 `groupBy { [it.age, it.status] }`。

聚合投影进入下一层查询后作为 generated field 使用的规则见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

```kotlin group="Case 6" name="kotlin" icon="kotlin" {1-4}
val listOfUser: List<User> = User().select()
    .groupBy { it.age }
    .having { it.age > 18 }
    .toList()
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
    .toList()
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

默认情况下，Kronos 会使用 `SqlLock.Update()`。可以通过传入 `SqlLock.Share()` 改用共享锁。

如需使用**乐观锁**，请参考{{$.keyword("query/locks", ["进阶用法","加锁机制"])}}。

```kotlin group="Case 18" name="kotlin" icon="kotlin"
val listOfUser: List<User> = User().select()
    .lock() // SqlLock.Update()
    .toList()

val listOfUser: List<User> = User().select()
    .lock(SqlLock.Share())
    .toList()
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
> 使用 `page` 方法后，查询结果默认**不会**包含总记录数。需要总数时使用 `withTotal()`。

```kotlin group="Case 8" name="kotlin" icon="kotlin" {1-4}
val (total, listOfUser) = User().select()
    .page(1, 10)
    .withTotal()
    .toList()

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

生成投影进入下一层查询后，也可以继续分页。

```kotlin group="Case 8-1" name="kotlin" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val (total, rows) = nameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .page(1, 10)
    .withTotal()
    .toList()
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

投影可见性和总数查询 SQL 形态见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## {{ $.title("db") }}跨库查询

`db`方法用于跨库查询，此时Kronos会根据`db`方法的参数设置查询的数据库。

```kotlin group="Case 20" name="kotlin" icon="kotlin" {1-3}
val listOfUser: List<User> = User()
    .db("user_database")
    .select { [it.id, it.username] }
    .toList()

// 或者db方法可以直接在select后面调用
// val listOfUser: List<User> = User()
//            .select { [it.id, it.username] }
//            .db("user_database")
//            .toList()
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

## 结果方法

在 select 查询链末尾使用结果方法决定返回形态。

```kotlin group="Result methods" name="kotlin" icon="kotlin"
val users: List<User> = User()
    .select()
    .toList()

val row: User? = User()
    .select()
    .where { it.id == 1 }
    .firstOrNull()

val mapRows: List<Map<String, Any?>> = User()
    .select { [it.id, it.name] }
    .toMapList()
```

结果形态、可空单行查询、生成投影返回值、分页总数和自定义 wrapper 见 {{ $.keyword("query/result-methods", ["结果方法"]) }}。

## {{ $.title("single") }}添加单行限制

查询链需要在终端方法前添加单行限制时，使用 `single()`。

```kotlin group="Single" name="kotlin" icon="kotlin" {1}
val user: User = User().select().single().first()
```

```sql group="Single" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user` LIMIT 1
```

## 级联查询

级联查询通过分步骤读取关联数据。常见流程是先读取主表记录，再根据这些主表结果读取子表记录。

详细 API 见{{$.keyword("advanced/cascade-select", ["进阶用法","级联查询"])}}。

## 使用指定的数据源

在Kronos中，我们可以使用指定的数据源查询数据库中的记录。

```kotlin group="Case 17" name="kotlin" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val listOfUser: List<User> = User().select().toList(customWrapper)
```
