{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 排序、分页与聚合

Kronos 在查询构建器上提供排序、分页、分组、聚合函数和聚合过滤。这些 API 可用于普通 `select()`、生成投影和 join 查询。

投影 alias 和生成结果形态见 {{ $.keyword("query/projection", ["投影"]) }}。函数细节见 {{ $.keyword("query/functions", ["内置函数"]) }}。

## 使用 {{ $.title("orderBy") }} 排序

使用 `orderBy { ... }` 配合 `asc()` 或 `desc()` 排序。多个排序项使用 `[]`。

```kotlin group="Sort 1" name="kotlin" icon="kotlin"
val users = User()
    .select { [it.id, it.name, it.age] }
    .where { it.age >= 18 }
    .orderBy { [it.age.desc(), it.name.asc()] }
    .toList()
```

```sql group="Sort 1" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`age` >= :ageMin
ORDER BY `age` DESC, `name` ASC
```

字段没有调用方向方法时，按升序排序。

```kotlin group="Sort 2" name="default asc" icon="kotlin"
User()
    .select()
    .orderBy { it.id }
    .toList()
```

## 按选中 alias 排序

`orderBy { ... }` 可以读取当前查询 context，包括已经选中的 alias。

```kotlin group="Alias sort" name="kotlin" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .orderBy { it.nameLength.desc() }
    .toList()
```

```sql group="Alias sort" name="Mysql" icon="mysql"
SELECT `id`, LENGTH(`name`) AS `nameLength`
FROM `user`
ORDER BY `nameLength` DESC
```

alias 还需要参与 `where`、`groupBy` 或 `having` 时，使用下一层查询，见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## 按窗口函数 alias 排序

窗口函数是 selected 表达式。先为窗口结果设置 alias，再在同一层按这个 alias 排序。

```kotlin group="Window sort" name="kotlin" icon="kotlin"
val rows = Order()
    .select {
        [
            it.id,
            it.userId,
            f.rowNumber()
                .over {
                    partitionBy(it.userId)
                    orderBy(it.status.desc())
                }
                .alias("rn")
        ]
    }
    .orderBy { it.rn.asc() }
    .toList()
```

```sql group="Window sort" name="Mysql" icon="mysql"
SELECT `id`,
       `user_id` AS `userId`,
       ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` DESC) AS rn
FROM `order`
ORDER BY `rn` ASC
```

过滤窗口 alias 时使用下一层查询，见 {{ $.keyword("query/subqueries", ["子查询"]) }}。函数入口见 {{ $.keyword("query/functions", ["内置函数"]) }}。

## 限制返回行数

只需要前几行时，使用 `limit(count)`。

```kotlin group="Limit" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .orderBy { it.id.asc() }
    .limit(10)
    .toList()
```

```sql group="Limit" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
ORDER BY `id` ASC
LIMIT 10
```

`first()` 和 `toMap()` 也会为单行结果应用单行限制。

## 分页并返回总数

使用 `withTotal().page(pageIndex, pageSize)` 查询带总数的分页。页码从 `1` 开始。

```kotlin group="Page 1" name="kotlin" icon="kotlin"
val (total, rows, totalPages): Triple<Int, List<User>, Int> = User()
    .select()
    .where { it.age >= 18 }
    .orderBy { it.id.asc() }
    .withTotal()
    .page(2, 20)
    .toList()
```

```sql group="Page 1" name="Mysql page" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`age` >= :ageMin
ORDER BY `id` ASC
LIMIT 20 OFFSET 20
```

```sql group="Page 1" name="Mysql total" icon="mysql"
SELECT COUNT(*)
FROM (
    SELECT 1
    FROM `user`
    WHERE `user`.`age` >= :ageMin
) AS total_count
```

返回值是一个 Triple。

```kotlin group="Page 2" name="result shape" icon="kotlin"
Triple(
    42,
    listOf(
        User(id = 21, name = "Ada", age = 18),
        User(id = 22, name = "Grace", age = 19)
    ),
    3
)
```

## 游标分页

首次游标分页使用 `withCursor().cursor(offset = count)`。下一次查询把返回的 cursor 传回去。游标分页要求 `orderBy` 使用已选中的字段，返回值是 `(hasNext, nextCursor, rows)`。

```kotlin group="Cursor" name="kotlin" icon="kotlin"
val (hasNext, nextCursor, rows) = User()
    .select { [it.id, it.name, it.age] }
    .where { it.age >= 18 }
    .orderBy { it.id.asc() }
    .withCursor()
    .cursor(offset = 20)
    .toList<User>()

val nextPage = User()
    .select { [it.id, it.name, it.age] }
    .where { it.age >= 18 }
    .orderBy { it.id.asc() }
    .withCursor()
    .cursor(nextCursor, offset = 20)
    .toList<User>()
```

```sql group="Cursor" name="Mysql next page" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`age` >= :ageMin AND `id` > :cursor_id
ORDER BY `id` ASC
LIMIT 21
```

`withCursor()` 与 `withTotal().page(...)` 是分开的入口；游标分页不计算总数和总页数。

## 选择聚合值

在 `select { ... }` 中使用聚合函数。返回多个值或需要命名属性时，为聚合表达式添加 alias。

```kotlin group="Aggregate" name="kotlin" icon="kotlin"
val rows = User()
    .select {
        [
            f.count(1).alias("count"),
            f.sum(it.score).alias("scoreSum"),
            f.avg(it.score).alias("scoreAvg"),
            f.min(it.score).alias("scoreMin"),
            f.max(it.score).alias("scoreMax")
        ]
    }
    .toList()
```

```sql group="Aggregate" name="Mysql" icon="mysql"
SELECT COUNT(1) AS count,
       SUM(`score`) AS `scoreSum`,
       AVG(`score`) AS `scoreAvg`,
       MIN(`score`) AS `scoreMin`,
       MAX(`score`) AS `scoreMax`
FROM `user`
```

```kotlin group="Aggregate" name="result shape" icon="kotlin"
data class UserScoreStats(
    val count: Number?,
    val scoreSum: Number?,
    val scoreAvg: Number?,
    val scoreMin: Number?,
    val scoreMax: Number?
)
```

## 分组并过滤聚合结果

使用 `groupBy { ... }` 指定分组键，使用 `having { ... }` 过滤聚合结果。`having` 使用和 `where` 相同的条件 DSL。

```kotlin group="Group having 1" name="kotlin" icon="kotlin"
val rows = User()
    .select {
        [
            it.gender,
            f.count(1).alias("count"),
            f.avg(it.score).alias("scoreAvg")
        ]
    }
    .groupBy { it.gender }
    .having { f.count(1) > 5 && f.avg(it.score) > 50 }
    .orderBy { it.scoreAvg.desc() }
    .toList()
```

```sql group="Group having 1" name="Mysql" icon="mysql"
SELECT `gender`,
       COUNT(1) AS count,
       AVG(`score`) AS `scoreAvg`
FROM `user`
GROUP BY `gender`
HAVING COUNT(1) > :countMin AND AVG(`score`) > :avgMin
ORDER BY `scoreAvg` DESC
```

返回行包含分组键和聚合 alias。

```kotlin group="Group having 2" name="result shape" icon="kotlin"
listOf(
    mapOf("gender" to 1, "count" to 12, "scoreAvg" to 86.5),
    mapOf("gender" to 0, "count" to 9, "scoreAvg" to 79.0)
)
```

多个分组键使用 `[]`。

```kotlin group="Group having 3" name="multiple keys" icon="kotlin"
User()
    .select { [it.gender, it.age, f.count(1).alias("count")] }
    .groupBy { [it.gender, it.age] }
    .toMapList()
```

## 在 join 后使用相同 API

join 查询在 join 块内提供同样的 `orderBy`、`page`、`withTotal`、`groupBy` 和 `having` 入口。

```kotlin group="Join aggregate" name="kotlin" icon="kotlin"
val rows = User().join(Order()) { user, order ->
    leftJoin(order) { user.id == order.userId }
    select {
        [
            user.id.alias("userId"),
            f.count(order.id).alias("orderCount")
        ]
    }
    groupBy { user.id }
    having { f.count(order.id) > 0 }
    orderBy { user.id.asc() }
    page(1, 20)
}.withTotal().toMapList()
```

```sql group="Join aggregate" name="Mysql" icon="mysql"
SELECT `user`.`id` AS `userId`,
       COUNT(`order`.`id`) AS `orderCount`
FROM `user`
LEFT JOIN `order`
ON `user`.`id` = `order`.`user_id`
GROUP BY `user`.`id`
HAVING COUNT(`order`.`id`) > :countMin
ORDER BY `user`.`id` ASC
LIMIT 20 OFFSET 0
```
