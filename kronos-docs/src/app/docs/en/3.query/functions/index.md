{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Use Built-In Functions

Kronos exposes built-in functions through the `f` receiver inside query DSL blocks. Use them in `select`, `where`, `having`, `orderBy`, and join `on` conditions.

```kotlin group="Function entry 1" name="kotlin" icon="kotlin"
val rows = User()
    .select {
        [
            it.id,
            f.length(it.name).alias("nameLength"),
            f.abs(it.score).alias("absoluteScore")
        ]
    }
    .where { f.length(it.name) > 3 && f.abs(it.score) > 10 }
    .orderBy { it.nameLength.desc() }
    .toList()
```

```sql group="Function entry 1" name="MySQL" icon="mysql"
SELECT `id`,
       LENGTH(`name`) AS `nameLength`,
       ABS(`score`) AS `absoluteScore`
FROM `user`
WHERE LENGTH(`name`) > :nameLength
  AND ABS(`score`) > :absoluteScore
ORDER BY `nameLength` DESC
```

Function expressions in `select { ... }` should usually use `.alias("name")`. The alias becomes the `toMapList()` map key, the generated projection property name, and a field that same-layer `orderBy` can read. See {{ $.keyword("query/projection", ["Projection"]) }} for result shapes.

```kotlin group="Function entry 2" name="consume result" icon="kotlin"
val first = rows.first()
val nameLength: Int? = first.nameLength
val absoluteScore: Int? = first.absoluteScore
```

Function SQL is rendered by the active database dialect. The complete built-in dialect list is covered in {{ $.keyword("database/dialect-support", ["Dialect Support"]) }}.

## Aggregate Functions

Aggregate functions can be used with `groupBy` and `having`. Alias aggregate expressions when they are returned as selected columns.

```kotlin group="Aggregate functions" name="kotlin" icon="kotlin"
val rows = Order()
    .select {
        [
            it.userId,
            f.count(it.id).alias("orderCount"),
            f.sum(it.amount).alias("totalAmount")
        ]
    }
    .groupBy { it.userId }
    .having { f.count(it.id) > 1 }
    .toList()
```

```sql group="Aggregate functions" name="MySQL" icon="mysql"
SELECT `user_id` AS `userId`,
       COUNT(`id`) AS `orderCount`,
       SUM(`amount`) AS `totalAmount`
FROM `order`
GROUP BY `user_id`
HAVING COUNT(`id`) > :orderCount
```

```kotlin group="Aggregate functions" name="consume result" icon="kotlin"
val first = rows.first()
val userId: Int? = first.userId
val orderCount: Int? = first.orderCount
val totalAmount: java.math.BigDecimal? = first.totalAmount
```

| Function | Kotlin entry | Common use |
|----------|--------------|------------|
| `count` | `f.count(x)` | Count rows or non-null values. |
| `sum` | `f.sum(x)` | Sum numeric values. |
| `avg` | `f.avg(x)` | Average value. |
| `max` | `f.max(x)` | Maximum value. |
| `min` | `f.min(x)` | Minimum value. |
| `groupConcat` | `f.groupConcat(x)` | Concatenate grouped values; SQL rendering is dialect-specific. |

Use `groupConcat` for grouped display values when their sequence is not part of the result contract. Each database aggregates rows in its own input order.

## Math Functions And Operators

Use Kotlin operators for addition, subtraction, multiplication, division, and modulo. Use `f` for the other math functions.

```kotlin group="Math functions" name="kotlin" icon="kotlin"
val rows = User()
    .select {
        [
            it.id,
            (it.score + 10).alias("scorePlus"),
            (it.score % 2).alias("scoreMod"),
            f.ceil(it.score).alias("scoreCeil"),
            f.trunc(it.score, 2).alias("scoreTrunc")
        ]
    }
    .toList()
```

```sql group="Math functions" name="MySQL" icon="mysql"
SELECT `id`,
       (`score` + 10) AS `scorePlus`,
       MOD(`score`, 2) AS `scoreMod`,
       CEIL(`score`) AS `scoreCeil`,
       TRUNCATE(`score`, 2) AS `scoreTrunc`
FROM `user`
```

| Function | Kotlin entry |
|----------|--------------|
| Addition | `it.score + 1` or `f.add(...)` |
| Subtraction | `it.score - 1` or `f.sub(...)` |
| Multiplication | `it.score * 2` or `f.mul(...)` |
| Division | `it.score / 2` or `f.div(...)` |
| Modulo | `it.score % 2` or `f.mod(x, y)` |
| Absolute value | `f.abs(x)` |
| Binary representation | `f.bin(x)` (MySQL) |
| Ceiling | `f.ceil(x)` |
| Exponent | `f.exp(x)` |
| Floor | `f.floor(x)` |
| Greatest value | `f.greatest(x, y, ...)` |
| Least value | `f.least(x, y, ...)` |
| Natural logarithm | `f.ln(x)` |
| Logarithm | `f.log(value, base)` |
| Pi | `f.pi()` |
| Random value | `f.rand()` |
| Round | `f.round(x, scale)` |
| Sign | `f.sign(x)` |
| Square root | `f.sqrt(x)` |
| Truncate | `f.trunc(x, scale)` |

`f.log(value, base)` keeps the value-first Kotlin call shape across the supported dialects. `f.bin(x)` is available for MySQL expressions.

## String Functions

String functions can be selected as result fields or used in conditions.

```kotlin group="String functions" name="kotlin" icon="kotlin"
val users = User()
    .select {
        [
            it.id,
            f.upper(it.name).alias("upperName"),
            f.concat(it.name, "-", it.status).alias("displayName")
        ]
    }
    .where { f.length(it.name) > 3 }
    .toList()
```

```sql group="String functions" name="MySQL" icon="mysql"
SELECT `id`,
       UPPER(`name`) AS `upperName`,
       CONCAT(`name`, '-', `status`) AS `displayName`
FROM `user`
WHERE LENGTH(`name`) > :nameLength
```

| Function | Kotlin entry |
|----------|--------------|
| Length | `f.length(x)` |
| Uppercase | `f.upper(x)` |
| Lowercase | `f.lower(x)` |
| Substring | `f.substr(x, start, length)` |
| Replace | `f.replace(x, old, new)` |
| Left substring | `f.left(x, length)` |
| Right substring | `f.right(x, length)` |
| Repeat | `f.repeat(x, times)` |
| Reverse | `f.reverse(x)` |
| Trim | `f.trim(x)` |
| Left trim | `f.ltrim(x)` |
| Right trim | `f.rtrim(x)` |
| Concatenate | `f.concat(x, y, ...)` |
| Join with separator | `f.join(separator, x, y, ...)` |

`f.join(separator, x, y, ...)` joins the provided values and skips null arguments. H2 applications that need reverse text can expose a custom database function; the built-in H2 dialect reports `f.reverse` as unavailable.

## Function Availability

| API | Supported use |
|-----|---------------|
| `f.log(value, base)`, `f.trunc(x, scale)`, `f.right(x, length)` | The same Kotlin call works across MySQL, PostgreSQL, SQLite, H2, SQL Server, Oracle, and DM8. |
| `f.join(separator, x, y, ...)` | Joins present values across the seven built-in dialects. |
| `f.groupConcat(x)` | Available across the seven built-in dialects; its result order follows the database aggregate input order. |
| `f.bin(x)` | MySQL. |
| `f.reverse(x)` | Current MySQL, PostgreSQL, SQLite, SQL Server, Oracle, and DM8 drivers. H2 applications can provide a custom function. |
| `f.any(...)`, `f.all(...)` | PostgreSQL array comparisons. |

### Native Kotlin case calls in conditions

For a source `String` field in a condition, the no-argument Kotlin calls `lowercase()` and `uppercase()` use the same SQL `LOWER` and `UPPER` expressions as `f.lower(...)` and `f.upper(...)`.

```kotlin group="Native string case equality" name="kotlin" icon="kotlin"
val name = "Ada"

val users = User()
    .select()
    .where { it.userName?.lowercase() == name.lowercase() }
    .toList()
```

```sql group="Native string case equality" name="MySQL" icon="mysql"
SELECT `id`, `user_name` AS `userName`
FROM `user`
WHERE LOWER(`user_name`) = :userName
```

`contains` keeps its escaped `%value%` matching behavior, while `like` accepts the pattern you provide.

```kotlin group="Native string case match" name="kotlin" icon="kotlin"
val name = "Ada"

val users = User()
    .select()
    .where {
        it.userName?.lowercase().contains(name.lowercase()) ||
            it.userName?.uppercase() like "ADA%"
    }
    .toList()
```

```sql group="Native string case match" name="MySQL" icon="mysql"
SELECT `id`, `user_name` AS `userName`
FROM `user`
WHERE LOWER(`user_name`) LIKE :userName ESCAPE '\\'
   OR UPPER(`user_name`) LIKE :userName@1
```

The captured `name.lowercase()` value is evaluated by Kotlin and bound as a parameter.

Source fields and normal variables use their direct form. When a KPojo property's actual value is required, end that property chain with `.value`, for example `probe.userName.value`.

See {{ $.keyword("query/conditions", ["Conditions"]) }} for condition matching rules.

## Use Window Functions

Kronos currently exposes `f.rowNumber()` for window queries. Import `com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber` before using it and include an `orderBy(...)` clause in `over { ... }`.

```kotlin group="Window function 1" name="kotlin" icon="kotlin"
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber

val ranked = Order()
    .select {
        [
            it.id,
            it.userId,
            it.status,
            f.rowNumber()
                .over {
                    partitionBy(it.userId)
                    orderBy(it.status.desc())
                }
                .alias("rn")
        ]
    }

val rows = ranked
    .orderBy { it.rn.asc() }
    .toList()
```

```sql group="Window function 1" name="MySQL" icon="mysql"
SELECT `id`,
       `user_id` AS `userId`,
       `status`,
       ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` DESC) AS rn
FROM `order`
ORDER BY `rn` ASC
```

Window aliases can be sorted in the same query. When a predicate needs a window alias, select it first and use `filter` to apply the predicate through a derived query; see {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

```kotlin group="Window function 2" name="consume result" icon="kotlin"
val first = rows.first()
val rowNumber: Int? = first.rn
```

```kotlin group="Window function 2" name="filter alias" icon="kotlin"
val firstPerUser = ranked
    .filter { it.rn == 1 }
    .toList()
```

```sql group="Window function 2" name="filter sql" icon="mysql"
SELECT `q`.`id`, `q`.`userId`, `q`.`status`, `q`.`rn`
FROM (
    SELECT `id`,
           `user_id` AS `userId`,
           `status`,
           ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` DESC) AS rn
    FROM `order`
) AS `q`
WHERE `q`.`rn` = :rn
```

## PostgreSQL Array Helpers

Use `f.any(...)` and `f.all(...)` for PostgreSQL array comparisons. Import the helpers before using them.

These `f` receiver functions render PostgreSQL array comparison expressions.

```kotlin group="PostgreSQL functions" name="kotlin" icon="kotlin"
import com.kotlinorm.functions.bundled.exts.PostgresFunctions.all
import com.kotlinorm.functions.bundled.exts.PostgresFunctions.any

val acceptedIds = intArrayOf(1, 2, 3)
val blockedIds = intArrayOf(8, 9)

val rows = User()
    .select()
    .where {
        it.id == f.any(acceptedIds) &&
            it.id != f.all(blockedIds)
    }
    .toList()
```

The condition renders PostgreSQL `ANY (...)` and `ALL (...)` array predicates.

## Custom Functions

Use custom function extensions for project-specific or dialect-specific functions. See {{ $.keyword("advanced/custom-functions", ["Custom Functions"]) }}.
