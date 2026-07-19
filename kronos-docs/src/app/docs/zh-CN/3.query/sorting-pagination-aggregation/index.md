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

## Offset 分页

使用 `page(pageIndex, pageSize)` 创建 offset page，页码从 `1` 开始。返回的 `OffsetPageQuery` 只执行一条 SELECT，`toList()` 直接返回记录。

```kotlin group="Page 1" name="kotlin" icon="kotlin"
val query = User()
    .select()
    .where { it.age >= 18 }
    .orderBy { it.id.asc() }

val rows: List<User> = query
    .page(pageIndex = 2, pageSize = 20)
    .toList()
```

```sql group="Page 1" name="Mysql page" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`age` >= :ageMin
ORDER BY `id` ASC
LIMIT 20 OFFSET 20
```

需要总数时，在 offset page 上调用 `withTotal()`。`toList()` 返回命名的 `PageResult`，包含 `total`、`records`、`totalPages`、`pageIndex` 和 `pageSize`。

```kotlin group="Page 2" name="kotlin" icon="kotlin"
val page: PageResult<User> = query
    .page(pageIndex = 2, pageSize = 20)
    .withTotal()
    .toList()

val total = page.total
val records = page.records
val totalPages = page.totalPages
```

```sql group="Page 2" name="Mysql total" icon="mysql"
SELECT COUNT(*)
FROM (
    SELECT 1
    FROM `user`
    WHERE `user`.`age` >= :ageMin
) AS total_count
```

offset page 仍是有限的 `KSelectable`，可以保留内层 LIMIT/OFFSET 并作为派生 source。带总数分页是执行阶段，不能再次 select、join、page 或 cursor。

```kotlin group="Page 3" name="derived page" icon="kotlin"
val finiteSource = query.page(pageIndex = 2, pageSize = 20)

val outerRows = finiteSource
    .select { [it.id, it.name] }
    .where { it.name != null }
    .toList()
```

## 游标分页

游标分页使用 `cursor(pageSize, after)`。下一次查询把返回的 `nextCursor` 作为 `after` 传入。`toList()` 返回命名的 `CursorResult`，包含 `hasNext`、`nextCursor` 和 `records`。

```kotlin group="Cursor" name="kotlin" icon="kotlin"
val query = User()
    .select { [it.id, it.name, it.age] }
    .where { it.age >= 18 }
    .orderBy { it.id.asc() }

val firstPage: CursorResult<User> = query
    .cursor(pageSize = 20)
    .toList<User>()

val nextPage: CursorResult<User> = query
    .cursor(pageSize = 20, after = firstPage.nextCursor)
    .toList<User>()
```

```sql group="Cursor" name="Mysql next page" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`age` >= :ageMin AND `id` > :cursor_id
ORDER BY `id` ASC
LIMIT 21
```

游标排序必须使用已选字段。如果排序本身不唯一，选中输出还必须保留完整的主键或唯一键，Kronos 才能追加稳定 tie-breaker。游标分页是执行阶段，不能调用 `page`、`withTotal` 或作为派生 source。创建 page/cursor view 不会修改可复用的基础查询。

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

JOIN 调用 `select` 后，可以使用同样的 `orderBy`、`page`、`withTotal`、`groupBy` 和 `having` API。

```kotlin group="Join aggregate" name="kotlin" icon="kotlin"
val rows = User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId }
        .select {
            [
                user.id.alias("userId"),
                f.count(order.id).alias("orderCount")
            ]
        }
        .groupBy { user.id }
        .having { f.count(order.id) > 0 }
        .orderBy { it.userId.asc() }
}.page(1, 20).withTotal().toMapList().records
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
