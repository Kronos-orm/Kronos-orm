{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Sorting, pagination, and aggregation

Kronos keeps ordering, paging, grouping, aggregate functions, and aggregate filters on the query builder. These APIs work after normal `select()` calls, generated projections, and join queries.

Projection aliases and generated result shapes are covered in {{ $.keyword("query/projection", ["Projection"]) }}. Function details are covered in {{ $.keyword("query/functions", ["Functions"]) }}.

## Sort with {{ $.title("orderBy") }}

Use `orderBy { ... }` with `asc()` or `desc()`. Multiple sort items are written with `[]`.

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

When no direction helper is used, the field sorts ascending.

```kotlin group="Sort 2" name="default asc" icon="kotlin"
User()
    .select()
    .orderBy { it.id }
    .toList()
```

## Sort by selected aliases

`orderBy { ... }` can read the current query context, including selected aliases.

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

Use the next query layer when the alias should also participate in `where`, `groupBy`, or `having`; see {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## Sort by a window alias

Window functions are selected expressions. Alias the window result, then sort by that alias in the same query.

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

Filtering a window alias uses the next query layer; see {{ $.keyword("query/subqueries", ["Subqueries"]) }}. The function entry point is covered in {{ $.keyword("query/functions", ["Functions"]) }}.

## Limit rows

Use `limit(count)` when only the first rows are needed.

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

`first()` and `toMap()` also apply a single-row limit for single-row results.

## Page rows with total count

Use `withTotal().page(pageIndex, pageSize)` for page queries with totals. Page indexes start at `1`.

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

The result is returned as a triple.

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

## Cursor rows

Use `withCursor().cursor(offset = count)` for the first cursor page. Pass the returned cursor into the next call. Cursor paging requires an `orderBy` over selected fields and returns `(hasNext, nextCursor, rows)`.

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

`withCursor()` is separate from `withTotal().page(...)`; cursor queries do not calculate totals or total pages.

## Select aggregate values

Use aggregate functions in `select { ... }`. Add aliases when the result has more than one value or when the aggregate should be returned as a named property.

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

## Group and filter aggregate rows

Use `groupBy { ... }` for group keys and `having { ... }` for aggregate filters. `having` uses the same condition DSL as `where`.

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

The returned rows contain the group key and aggregate aliases.

```kotlin group="Group having 2" name="result shape" icon="kotlin"
listOf(
    mapOf("gender" to 1, "count" to 12, "scoreAvg" to 86.5),
    mapOf("gender" to 0, "count" to 9, "scoreAvg" to 79.0)
)
```

Use `[]` for multiple group keys.

```kotlin group="Group having 3" name="multiple keys" icon="kotlin"
User()
    .select { [it.gender, it.age, f.count(1).alias("count")] }
    .groupBy { [it.gender, it.age] }
    .toMapList()
```

## Use the same APIs after joins

Join queries expose the same `orderBy`, `page`, `withTotal`, `groupBy`, and `having` entries inside the join block.

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
