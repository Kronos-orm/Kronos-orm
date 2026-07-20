{% import "../../../macros/macros-en.njk" as $ %}

## Conditions for {{ $.title("where") }}, {{ $.title("having") }}, `filter`, and {{ $.title("on") }}

Kronos condition DSL is used in `where`, `having`, `filter`, and `on` blocks. You can write Kotlin operators and Kronos condition helpers in the same expression.

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

## Choose the filtering layer

`where` filters the current SQL layer's source fields. `having` filters grouped rows with source expressions and aggregates. Use `filter` after `select { ... }` when the predicate must read the selected result, including generated aliases.

```kotlin group="Result filter" name="kotlin" icon="kotlin"
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber

val rows = Order()
    .select {
        [
            it.id,
            f.rowNumber()
                .over { orderBy(it.id.asc()) }
                .alias("rn")
        ]
    }
    .filter { it.rn > 1 }
    .toList()
```

```sql group="Result filter" name="Mysql" icon="mysql"
SELECT `q`.`id`, `q`.`rn`
FROM (
    SELECT `id`,
           ROW_NUMBER() OVER (ORDER BY `id` ASC) AS rn
    FROM `order`
) AS `q`
WHERE `q`.`rn` > :rnMin
```

`filter` always creates this derived-query boundary. Its receiver is only the `Selected` result, so this example exposes `id` and `rn`, but no unselected `Order` fields. It preserves that result shape and is equivalent to calling `select().where { ... }` on the selected query explicitly.

## Choose the {{ $.title("where") }} call

`select()` creates a query for the current table. Object property values enter the condition when you call `where()` or `by { ... }`.

```kotlin group="Where calls 1" name="select" icon="kotlin"
val users = User(id = 1)
    .select()
    .toList()
```

```sql group="Where calls 1" name="select sql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
```

An empty `where()` uses the current KPojo's queryable non-null fields as equality conditions.

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

Multiple queryable non-null fields participate in the same query-by-example condition.

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

Use `== null` or `isNull` in a lambda when the target value is SQL `NULL`.

```kotlin group="Where calls 4" name="null value" icon="kotlin"
val users = User()
    .select()
    .where { it.email == null }
    .toList()
```

```sql group="Where calls 4" name="null value sql" icon="mysql"
SELECT `id`, `name`, `age`, `email`
FROM `user`
WHERE `user`.`email` IS NULL
```

Dynamic nullable values still use the no-value strategy. When `email` is `null`, the default `SELECT` behavior omits this condition.

```kotlin group="Where calls 4b" name="dynamic null" icon="kotlin"
val email: String? = null

val users = User()
    .select()
    .where { it.email == email }
    .toList()
```

A `where { ... }` call uses the lambda expression for that condition.

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

Multiple `where` calls append conditions with `AND`.

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

Put complex `OR`, grouping, and function predicates in the same lambda.

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

`select().where()` keeps the query unconditional when the object has no queryable non-null fields.

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

`update().where()` and `delete().where()` use the same query-by-example rule to select target rows. Write operations with no queryable fields enter write-safety checks, and DataGuard can reject full-table writes consistently.

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

Logic-delete fields, cascade properties, ignored fields, and non-column properties are handled by their own strategies. Empty `where()` reads queryable database columns for query-by-example conditions.

## Compare field values

Use Kotlin comparison operators for equality and range comparisons.

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

Use the no-argument condition properties when the value comes from the current KPojo instance.

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

`eq`, `neq`, `lt`, `gt`, `le`, and `ge` read the property value from the object that starts the operation.

## Combine conditions

Use `&&`, `||`, `!`, and parentheses to control boolean composition.

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

Kronos keeps the same expression order and parameterizes repeated field values with suffixes such as `status@1`.

## Match a collection

Use `in` and `!in` with lists, arrays, or selectable subqueries.

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

Arrays use the same `IN` predicate shape.

```kotlin group="In array" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.id in arrayOf(1, 2, 3) }
    .toList()
```

Use a selectable query on the right side when the set comes from another table.

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

More subquery predicates, row-value tuples, and quantified comparisons are covered in {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## Match a range

Use `between` and `notBetween` with Kotlin ranges.

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

## Match strings

Use `like` and `notLike` when the pattern is already prepared.

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

Use `startsWith`, `endsWith`, and `contains` to let Kronos build the `%` pattern.

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

Use `regexp` and `notRegexp` for database regular-expression predicates.

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

## Read values from the current object

No-argument match helpers read the value from the object that starts the operation.

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

The bound value is `Ada%`.

## Test null values

Use `== null` / `!= null` or `isNull` / `notNull` for SQL null checks.

```kotlin group="Null" name="kotlin" icon="kotlin"
val users = User()
    .select()
    .where { it.deletedAt == null || it.email != null }
    .toList()
```

```sql group="Null" name="Mysql" icon="mysql"
SELECT `id`, `name`, `email`, `deleted_at` AS `deletedAt`
FROM `user`
WHERE `user`.`deleted_at` IS NULL OR `user`.`email` IS NOT NULL
```

## Compare fields and object values

Use a field on the right side to compare two columns.

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

Relationship field chains should use a safe call. Kronos reads the related field metadata and compares columns; it does not read the runtime `director` object.

```kotlin group="Related field" name="kotlin" icon="kotlin"
val movies = Movie()
    .select()
    .where { it.directorId == it.director?.id }
    .toList()

// it.director!!.id is also supported.
```

```sql group="Related field" name="Mysql" icon="mysql"
SELECT `id`, `director_id` AS `directorId`, `title`
FROM `movie`
WHERE `movie`.`director_id` = `director`.`id`
```

Use `.value` when you need the Kotlin property value from another object.

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

## Use functions and arithmetic

Built-in functions and arithmetic expressions can appear inside conditions.

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

Function details are covered in {{ $.keyword("query/functions", ["Built-in Functions"]) }}.

Window functions are selected expressions. Use them in `select { ... }`, then use `filter` to apply a predicate to their aliases through a derived query; see {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## Expand object equality

Use `it.eq` to expand all non-empty fields of the current object into equality conditions.

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

Use `-` before `.eq` to exclude fields from the expansion.

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

## Add raw SQL predicates

Use `.asSql()` when a predicate must be written as SQL text.

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

`patch` supplies named parameters that are not read from the current KPojo object.

## Build dynamic predicates

Use `.takeIf(...)` to keep a predicate when a Kotlin condition is `true`, or `.takeUnless(...)` to keep it when the condition is `false`.

```kotlin group="No value" name="kotlin" icon="kotlin"
val minAge: Int? = null
val includeInactive = false

val users = User()
    .select()
    .where {
        (it.age >= minAge).takeIf(minAge != null) &&
            (it.status == 0).takeUnless(includeInactive)
    }
    .toList()
```

```sql group="No value" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `status` = :status
```

Ordinary Kotlin `if` and `when` expressions can select complete SQL predicate branches:

```kotlin group="Dynamic branches" name="kotlin" icon="kotlin"
data class UserFilter(val id: Int? = null, val name: String? = null)

val filter = UserFilter(id = 7)
val users = User()
    .select()
    .where {
        when {
            filter.id != null -> it.id == filter.id
            filter.name != null -> it.name == filter.name
            else -> it.active == true
        }
    }
    .toList()
```

The Boolean arguments of `takeIf`/`takeUnless` and the conditions of `if`/`when` are ordinary Kotlin control flow. Properties of ordinary Kotlin objects are runtime values in SQL comparisons too, so `it.id == filter.id` does not use `.value`. This includes regular classes, data classes, objects, companion or `@JvmStatic` properties, and top-level properties. A captured KPojo property is field-shaped; when that KPojo is not a source of the current query, use `.value`, for example `it.id == probe.id.value`.

Default no-value handling for `null` values and empty collections is described in {{ $.keyword("configuration/no-value-strategy", ["No Value Strategy"]) }}.

## Use conditions in {{ $.title("having") }}

`having` uses the same condition DSL as `where`.

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

## Use conditions in {{ $.title("on") }}

`leftJoin`, `rightJoin`, `innerJoin`, and `fullJoin` use the same condition DSL for join predicates. `crossJoin()` is conditionless.

```kotlin group="On" name="kotlin" icon="kotlin"
val rows = User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId && order.status == 1 }
        .select { [user.id, user.name, order.status] }
}.toList()
```

```sql group="On" name="Mysql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `order`.`status`
FROM `user`
LEFT JOIN `order`
ON `user`.`id` = `order`.`user_id` AND `order`.`status` = :status
```

Join syntax is covered in {{ $.keyword("query/join", ["Select Join Tables"]) }}.
