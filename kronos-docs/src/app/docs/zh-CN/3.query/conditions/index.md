{% import "../../../macros/macros-zh-CN.njk" as $ %}

## {{ $.title("where") }}、{{ $.title("having") }} 和 {{ $.title("on") }} 条件

Kronos 条件 DSL 用在 `where`、`having` 和 `on` 块中。你可以在同一个表达式中写 Kotlin 操作符和 Kronos 条件函数。

```kotlin group="Condition entry" name="kotlin" icon="kotlin"
val users = User()
    .select { [it.id, it.name, it.age] }
    .where { (it.age >= 18 && it.name like "Ada%") || it.id == 1 }
    .toList()
```

```sql group="Condition entry" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE (`user`.`age` >= :ageMin AND `user`.`name` LIKE :name) OR `user`.`id` = :id
```

## 选择 {{ $.title("where") }} 调用方式

`select()` 只创建当前表查询，对象字段值会在调用 `where()` 或 `by { ... }` 时进入条件。

```kotlin group="Where calls 1" name="select" icon="kotlin"
val users = User(id = 1)
    .select()
    .toList()
```

```sql group="Where calls 1" name="select sql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
```

空 `where()` 按当前 KPojo 的可查询非空字段生成等值条件。

```kotlin group="Where calls 2" name="empty where" icon="kotlin"
val users = User(id = 1)
    .select()
    .where()
    .toList()
```

```sql group="Where calls 2" name="empty where sql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`id` = :id
```

多个可查询非空字段会同时参与 query-by-example。

```kotlin group="Where calls 3" name="dynamic example" icon="kotlin"
val users = User(id = 1, name = "A", email = null)
    .select()
    .where()
    .toList()
```

```sql group="Where calls 3" name="dynamic sql" icon="mysql"
SELECT `id`, `name`, `age`, `email`
FROM `user`
WHERE `user`.`id` = :id
  AND `user`.`name` = :name
```

需要查询空值时，在 lambda 中显式写 `isNull`。

```kotlin group="Where calls 4" name="null value" icon="kotlin"
val users = User()
    .select()
    .where { it.email.isNull }
    .toList()
```

```sql group="Where calls 4" name="null value sql" icon="mysql"
SELECT `id`, `name`, `age`, `email`
FROM `user`
WHERE `user`.`email` IS NULL
```

带 lambda 的 `where { ... }` 使用 lambda 表达式生成本次条件。

```kotlin group="Where calls 5" name="lambda where" icon="kotlin"
val users = User(id = 1)
    .select()
    .where { it.name == "A" }
    .toList()
```

```sql group="Where calls 5" name="lambda where sql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`name` = :name
```

多次 `where` 调用会按 `AND` 追加条件。

```kotlin group="Where calls 6" name="append where" icon="kotlin"
val users = User(id = 1)
    .select()
    .where()
    .where { it.name == "A" }
    .toList()
```

```sql group="Where calls 6" name="append where sql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`id` = :id
  AND `user`.`name` = :name
```

复杂 `OR`、分组和函数条件写在同一个 lambda 中。

```kotlin group="Where calls 7" name="grouped where" icon="kotlin"
val users = User(id = 1)
    .select()
    .where()
    .where { it.name == "A" || it.age > 18 }
    .toList()
```

```sql group="Where calls 7" name="grouped where sql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`id` = :id
  AND (`user`.`name` = :name OR `user`.`age` > :ageMin)
```

`select().where()` 没有可查询非空字段时保留无条件查询。

```kotlin group="Where calls 8" name="empty object" icon="kotlin"
val users = User()
    .select()
    .where()
    .toList()
```

```sql group="Where calls 8" name="empty object sql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
```

`update().where()` 和 `delete().where()` 使用同样的 query-by-example 规则筛选目标行。没有可查询字段的写操作会进入写入安全检查，DataGuard 可以统一拦截全表写入。

```kotlin group="Where calls 9" name="write where" icon="kotlin"
User(id = 1)
    .update()
    .set { it.name = "Kronos ORM" }
    .where()
    .execute()

User(id = 1)
    .delete()
    .where()
    .execute()
```

```sql group="Where calls 9" name="write where sql" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `user`.`id` = :id

DELETE FROM `user`
WHERE `user`.`id` = :id
```

逻辑删除字段、级联属性、忽略字段和非数据库列由各自策略处理，空 `where()` 的 query-by-example 条件只读取可查询数据库列。

## 比较字段值

使用 Kotlin 比较操作符写相等和大小范围条件。

```kotlin group="Compare" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where {
        it.name == "Ada" &&
            it.age >= 18 &&
            it.score < 100
    }
    .toList()
```

```sql group="Compare" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`, `score`
FROM `user`
WHERE `user`.`name` = :name
  AND `user`.`age` >= :ageMin
  AND `user`.`score` < :scoreMax
```

当条件值来自当前 KPojo 对象时，使用无参条件属性。

```kotlin group="Compare from object" name="kotlin" icon="kotlin"
val probe = User(name = "Ada", age = 18)

val users = probe
    .select()
    .where { it.name.eq && it.age.ge }
    .toList()
```

```sql group="Compare from object" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`name` = :name
  AND `user`.`age` >= :ageMin
```

`eq`、`neq`、`lt`、`gt`、`le`、`ge` 会读取发起操作的对象属性值。

## 组合条件

使用 `&&`、`||`、`!` 和括号控制条件组合。

```kotlin group="Boolean" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where {
        (it.status == 1 || it.status == 2) &&
            !(it.name like "test%")
    }
    .toList()
```

```sql group="Boolean" name="Mysql" icon="mysql"
SELECT `id`, `name`, `status`
FROM `user`
WHERE (`user`.`status` = :status OR `user`.`status` = :status@1)
  AND `user`.`name` NOT LIKE :name
```

Kronos 会保留表达式组合顺序，并为重复字段参数追加 `status@1` 这样的后缀。

## 匹配集合

使用 `in` 和 `!in` 匹配列表、数组或可查询子查询。

```kotlin group="In list" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.id in listOf(1, 2, 3) }
    .toList()
```

```sql group="In list" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`id` IN (:idList)
```

数组使用同样的 `IN` 谓词形态。

```kotlin group="In array" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.id in arrayOf(1, 2, 3) }
    .toList()
```

当集合来自另一张表时，把可查询对象放在右侧。

```kotlin group="In subquery" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { user ->
        user.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
    }
    .toList()
```

```sql group="In subquery" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`id` IN (
    SELECT `user_id` AS `userId`
    FROM `order`
    WHERE `order`.`status` = :status
)
```

更多子查询谓词、行值 tuple 和 quantified comparison 见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## 匹配范围

使用 `between` 和 `notBetween` 匹配 Kotlin range。

```kotlin group="Between" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.age between 18..40 }
    .toList()
```

```sql group="Between" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`age` BETWEEN 18 AND 40
```

```kotlin group="Not between" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.age notBetween 1..17 }
    .toList()
```

```sql group="Not between" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`age` NOT BETWEEN 1 AND 17
```

## 匹配字符串

当通配符已经准备好时，使用 `like` 和 `notLike`。

```kotlin group="Like" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.name like "Ada%" }
    .toList()
```

```sql group="Like" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`name` LIKE :name
```

使用 `startsWith`、`endsWith`、`contains` 让 Kronos 生成 `%` 模式。

```kotlin group="String helpers" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where {
        it.name.startsWith("A") ||
            it.name.endsWith("son") ||
            it.name.contains("ron")
    }
    .toList()
```

```sql group="String helpers" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`name` LIKE :name
   OR `user`.`name` LIKE :name@1
   OR `user`.`name` LIKE :name@2
```

使用 `regexp` 和 `notRegexp` 写数据库正则谓词。

```kotlin group="Regexp" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.name regexp "^A.*" }
    .toList()
```

```sql group="Regexp" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`name` REGEXP :namePattern
```

## 读取当前对象的值

无参匹配属性会读取发起操作的对象属性值。

```kotlin group="Object string value" name="kotlin" icon="kotlin"
val probe = User(name = "Ada")

val users = probe
    .select()
    .where { it.name.startsWith }
    .toList()
```

```sql group="Object string value" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`name` LIKE :name
```

绑定值为 `Ada%`。

## 判断空值

使用 `isNull` 和 `notNull` 生成 SQL 空值判断。

```kotlin group="Null" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.deletedAt.isNull || it.email.notNull }
    .toList()
```

```sql group="Null" name="Mysql" icon="mysql"
SELECT `id`, `name`, `email`, `deleted_at` AS `deletedAt`
FROM `user`
WHERE `user`.`deleted_at` IS NULL OR `user`.`email` IS NOT NULL
```

## 比较字段和对象值

右侧写字段时，Kronos 会生成列与列比较。

```kotlin group="Field value" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.age == it.otherAge }
    .toList()
```

```sql group="Field value" name="Mysql" icon="mysql"
SELECT `id`, `age`, `other_age` AS `otherAge`
FROM `user`
WHERE `user`.`age` = `user`.`other_age`
```

需要读取另一个对象的 Kotlin 属性值时，使用 `.value`。

```kotlin group="Kotlin value" name="kotlin" icon="kotlin"
val probe = User(otherAge = 40)

val users = User()
    .select()
    .where { it.age == probe.otherAge.value }
    .toList()
```

```sql group="Kotlin value" name="Mysql" icon="mysql"
SELECT `id`, `age`
FROM `user`
WHERE `user`.`age` = :age
```

## 使用函数和算术表达式

内置函数和算术表达式可以写在条件中。

```kotlin group="Expression" name="kotlin" icon="kotlin"
val users = User()
    .select { [it.id, it.name] }
    .where {
        it.score + 10 > it.score - 10 &&
            f.length(it.name) > 5
    }
    .toList()
```

```sql group="Expression" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE (`score` + 10) > (`score` - 10)
  AND LENGTH(`name`) > :lengthMin
```

函数详情见 {{ $.keyword("query/functions", ["内置函数"]) }}。

窗口函数是 selected 表达式。先在 `select { ... }` 中选择窗口结果，再进入下一层查询过滤 alias；见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## 展开对象相等条件

使用 `it.eq` 把当前对象中的非空字段展开为相等条件。

```kotlin group="KPojo eq" name="kotlin" icon="kotlin"
val probe = User(id = 1, name = "Ada", age = 36)

val users = probe
    .select()
    .where { it.eq }
    .toList()
```

```sql group="KPojo eq" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`id` = :id
  AND `user`.`name` = :name
  AND `user`.`age` = :age
```

在 `.eq` 前使用 `-` 排除不参与展开的字段。

```kotlin group="KPojo eq exclude" name="kotlin" icon="kotlin"
val probe = User(id = 1, name = "Ada", age = 36)

val users = probe
    .select()
    .where { (it - it.age).eq }
    .toList()
```

```sql group="KPojo eq exclude" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`id` = :id
  AND `user`.`name` = :name
```

## 添加原生 SQL 条件

当条件需要直接写 SQL 文本时，使用 `.asSql()`。

```kotlin group="Raw SQL" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { "`name` = :name AND `age` > :age".asSql() }
    .patch("name" to "Ada", "age" to 18)
    .toList()
```

```sql group="Raw SQL" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `name` = :name AND `age` > :age
```

`patch` 用于补充当前 KPojo 对象中没有提供的命名参数。

## 设置无值策略

使用 `.ifNoValue(...)` 处理条件值为 `null` 或空集合时的行为。

```kotlin group="No value" name="kotlin" icon="kotlin"
val minAge: Int? = null

val users = User()
    .select()
    .where { (it.age >= minAge).ifNoValue(NoValueStrategyType.Ignore) }
    .toList()
```

```sql group="No value" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
```

`Ignore` 会跳过当前条件。其他策略见 {{ $.keyword("configuration/no-value-strategy", ["无值策略"]) }}。

## 在 {{ $.title("having") }} 中使用条件

`having` 使用和 `where` 相同的条件 DSL。

```kotlin group="Having" name="kotlin" icon="kotlin"
val rows: List<Map<String, Any?>> = Order()
    .select { [it.userId, f.count(1).alias("orderCount")] }
    .groupBy { it.userId }
    .having { f.count(1) > 3 }
    .toMapList()
```

```sql group="Having" name="Mysql" icon="mysql"
SELECT `user_id` AS `userId`, COUNT(1) AS orderCount
FROM `order`
GROUP BY `user_id`
HAVING COUNT(1) > :countMin
```

## 在 {{ $.title("on") }} 中使用条件

`on`、`leftJoin`、`rightJoin`、`innerJoin`、`crossJoin` 和 `fullJoin` 都使用同一套条件 DSL 生成关联条件。

```kotlin group="On" name="kotlin" icon="kotlin"
val rows = User().join(Order()) { user, order ->
    leftJoin(order) { user.id == order.userId && order.status == 1 }
    select { [user.id, user.name, order.status] }
}.toList()
```

```sql group="On" name="Mysql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `order`.`status`
FROM `user`
LEFT JOIN `order`
ON `user`.`id` = `order`.`user_id` AND `order`.`status` = :status
```

Join 语法见 {{ $.keyword("query/join", ["联表查询"]) }}。
