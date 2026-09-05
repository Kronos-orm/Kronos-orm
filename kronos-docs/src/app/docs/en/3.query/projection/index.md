{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Projection

Projection is the field list passed to `select { ... }` or `join { select { ... } }`. Use it to choose result columns, name expressions with `alias`, and shape the rows returned by `toMapList()`, `toList()`, `first()`, and join queries.

Result methods and nullable single-row methods are covered in {{ $.keyword("query/result-methods", ["Result Methods"]) }}.

## Select fields

Use one field for a scalar result, or `[]` for a row with multiple fields.

```kotlin group="Field projection 1" name="kotlin" icon="kotlin"
val names: List<String> = User()
    .select { it.name }
    .toList<String>()

val rows = User()
    .select { [it.id, it.name] }
    .toList()
```

```sql group="Field projection 1" name="Mysql" icon="mysql"
SELECT `name`
FROM `user`

SELECT `id`, `name`
FROM `user`
```

The multi-field query returns a generated projection with the selected field names. You usually do not name this class. The compiler creates a result type for this select call, and Kotlin type inference lets you read its properties directly.

```kotlin group="Field projection 2" name="result shape" icon="kotlin"
data class UserProjection(
    val id: Int?,
    val name: String?
)
```

```kotlin group="Field projection 2" name="consume result" icon="kotlin"
val first = rows.first()
val id: Int? = first.id
val name: String? = first.name
```

The generated class is an internal `KronosSelectResult_...` type at runtime. Treat its public shape as the selected property list, not as a stable class name.

## Use alias for result property names

Use `.alias("name")` when the selected column, function, aggregate, scalar subquery, or raw SQL expression should have a specific result name.

```kotlin group="Alias projection 1" name="kotlin" icon="kotlin"
val rows = User()
    .select {
        [
            it.id,
            it.name.alias("username"),
            f.length(it.name).alias("nameLength"),
            "1".alias("constantValue")
        ]
    }
    .toList()
```

```sql group="Alias projection 1" name="Mysql" icon="mysql"
SELECT `id`,
       `name` AS `username`,
       LENGTH(`name`) AS `nameLength`,
       1 AS `constantValue`
FROM `user`
```

The alias becomes the map key for `toMapList()` and the property name for generated projections.

```kotlin group="Alias projection 2" name="result shape" icon="kotlin"
mapOf(
    "id" to 1,
    "username" to "Ada",
    "nameLength" to 3,
    "constantValue" to 1
)

data class UserAliasProjection(
    val id: Int?,
    val username: String?,
    val nameLength: Int?,
    val constantValue: Int?
)
```

Alias computed items when the selected item is not a direct field, or when a direct field should use a different result property name.

## Resolve duplicate output names

Duplicate requested output names are rejected unless the declaration or expression opts in to `UnsafeProjectionOverride`. After opt-in, Kronos preserves every selected value: the first occurrence keeps its name, and later occurrences receive `_1`, `_2`, and so on.

```kotlin group="Duplicate projection" name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.UnsafeProjectionOverride

@OptIn(UnsafeProjectionOverride::class)
val rows = User()
    .select { [it.id, it.id, it.name.alias("id_1")] }
    .toList()

val first = rows.first()
val originalId = first.id
val repeatedId = first.id_2
val reservedAlias = first.id_1
```

Explicit names are reserved before suffixes are allocated. The requested names `id`, `id`, `id_1` therefore resolve to `id`, `id_2`, `id_1`, not `id`, `id_1`, `id_1`. The same names are used for SQL labels, generated properties, typed mapping, map keys, derived queries, and union output.

Prefer explicit aliases such as `userId` and `orderId` when the values have different meanings. Use the opt-in when preserving repeated names is intentional.

### Replace or shadow a source name

A selected alias may reuse a source field name without opt-in if no same-layer Context clause reads that name. `where`, `groupBy`, and `having` still use source fields. `orderBy` uses the post-select Context, so reading a shadowed name there requires opt-in and resolves to the selected value and its Kotlin type.

```kotlin group="Context shadow" name="opt-in" icon="kotlin"
@OptIn(UnsafeProjectionOverride::class)
val ordered = User()
    .select { [it.id, f.length(it.name).alias("name")] }
    .where { it.name != null }       // Source.name
    .orderBy { it.name.desc() }      // selected Int? name
```

Remove the source field before restoring the same name when replacement is the intended shape; this form does not require opt-in.

```kotlin group="Context shadow" name="source minus" icon="kotlin"
val replaced = User()
    .select { [it - it.name, f.length(it.name).alias("name")] }
    .orderBy { it.name.desc() }
```

## Use raw SQL select items

String expressions can be used as raw SQL select items. Use `.alias("name")` when the raw expression should be returned as a map key or generated projection property.

```kotlin group="Raw SQL projection" name="kotlin" icon="kotlin"
val rows = User()
    .select {
        [
            "count(*)".alias("total"),
            "now()"
        ]
    }
    .toMapList()

val first = rows.first()
val total = first["total"]
```

```sql group="Raw SQL projection" name="Mysql" icon="mysql"
SELECT count(*) AS total, now()
FROM `user`
```

`"count(*)".alias("total")` keeps `count(*)` as the SQL expression and uses `total` as the result name. Raw select items are useful for database-specific expressions that do not have a typed function helper. Keep parameters in `where { ... }` plus `patch(...)` when values should be bound.

## Consume projection results

Use `toMapList()` when each row should be a map. The map keys are field names and aliases.

```kotlin group="Consume projection 1" name="map" icon="kotlin"
val maps: List<Map<String, Any?>> = User()
    .select { [it.id, it.name.alias("username")] }
    .toMapList()

val first = maps.first()
val id = first["id"]
val username = first["username"]
```

Use `toList()` or `first()` without a generic argument when you want the generated projection type.

```kotlin group="Consume projection 2" name="generated" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .toList()

val nameLength: Int? = rows.first().nameLength

val one = User()
    .select { [it.id, it.name.alias("username")] }
    .where { it.id == 1 }
    .first()

val username: String? = one.username
```

Use a DTO class when the result type should be named in your code. Pass the class to `select(...)`, then use the normal result methods.

```kotlin group="Consume projection 3" name="dto" icon="kotlin"
data class UserSummary(
    var id: Int? = null,
    var username: String? = null
) : KPojo

val rows: List<UserSummary> = User()
    .select(UserSummary::class) {
        [it.id, it.name.alias("username")]
    }
    .toList()
```

The selected output names must match the DTO property names you want to fill.

## Full projections, exclusions, and []

`select { it }`, `select { [it] }`, and `select { listOf(it) }` keep the same result type as `select()`. Use `-` to remove fields. Exclusions and mixed lists create generated projection result types.

```kotlin name="kotlin" icon="kotlin"
val allDirect = User().select { it }.toList()
val allInList = User().select { [it] }.toList()

val withoutAge = User().select { it - it.age }.toList()
val withoutAgeInList = User().select { [it - it.age] }.toList()
val withoutIdAndAge = User().select { it - it.id - it.age }.toList()
val withoutIdAndAgeWithAlias = User().select { [it - [it.id, it.age], it.id.alias("sourceId")] }.toList()
```

```kotlin group="Exclude projection" name="kotlin" icon="kotlin"
val rows = User()
    .select { it - it.age }
    .toList()
```

```sql group="Exclude projection" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
```

The right side of `-` may also use `[]`, so `[it - [it.id, it.age], it.id.alias("sourceId")]` is a valid projection list.

A full projection can share a list with ordinary fields, aliases, or function projections. Expanded fields keep their source order, and following items are appended to the generated result type.

```kotlin group="Mixed full projection" name="kotlin" icon="kotlin"
val rows = User()
    .select { [it, it.id.alias("sourceId")] }
    .toList()

val id: Int? = rows.first().id
val sourceId: Int? = rows.first().sourceId
```

```sql group="Mixed full projection" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`, `id` AS `sourceId`
FROM `user`
```

Use `it - ...` when most columns should be returned with a few exclusions, or when aliases should be appended after a full projection. Resolved output names are always unique; repeated requested names require the opt-in described above.

## Project from join queries

Inside a join block, `select { ... }` can read fields from each joined table. Use aliases when two tables expose the same field name or when a row type should have stable property names.

```kotlin group="Join projection" name="kotlin" icon="kotlin"
data class UserOrderRow(
    val userId: Int?,
    val username: String?,
    val orderId: Int?,
    val status: Int?
)

val rows: List<UserOrderRow> = User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId }
        .select {
        [
            user.id.alias("userId"),
            user.name.alias("username"),
            order.id.alias("orderId"),
            order.status
        ]
        }
}.toList<UserOrderRow>()
```

```sql group="Join projection" name="Mysql" icon="mysql"
SELECT `user`.`id` AS `userId`,
       `user`.`name` AS `username`,
       `order`.`id` AS `orderId`,
       `order`.`status`
FROM `user`
LEFT JOIN `order`
ON `user`.`id` = `order`.`user_id`
```

Join syntax and join filtering are covered in {{ $.keyword("query/join", ["Join"]) }}.

## Project scalar subqueries

A scalar subquery can be part of the select list when it returns one column. Use `limit(1)` and give the subquery an alias.

```kotlin group="Scalar projection" name="kotlin" icon="kotlin"
val rows = User()
    .select { user ->
        [
            user.id,
            user.name,
            Order()
                .select { order -> order.status }
                .where { order -> order.userId == user.id }
                .orderBy { order -> order.id.desc() }
                .limit(1)
                .alias("lastOrderStatus")
        ]
    }
    .toList()
```

```sql group="Scalar projection" name="Mysql" icon="mysql"
SELECT `id`,
       `name`,
       (
           SELECT `status`
           FROM `order`
           WHERE `order`.`user_id` = `user`.`id`
           ORDER BY `id` DESC
           LIMIT 1
       ) AS `lastOrderStatus`
FROM `user`
```

Subquery forms, derived query sources, and projection visibility rules are covered in {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## Filter derived query results

A selected projection can become the source for the next query layer. Use `filter { ... }` when an alias should be filtered after it has been selected and the output shape should stay unchanged.

```kotlin group="Derived projection" name="kotlin" icon="kotlin"
val nameLengths = User()
    .select {
        [
            it.id,
            it.name,
            f.length(it.name).alias("nameLength")
        ]
    }

val rows = nameLengths
    .filter { it.nameLength > 8 }
    .toList()
```

```sql group="Derived projection" name="Mysql" icon="mysql"
SELECT `q`.`id`, `q`.`name`, `q`.`nameLength`
FROM (
    SELECT `id`, `name`, LENGTH(`name`) AS `nameLength`
    FROM `user`
) AS `q`
WHERE `q`.`nameLength` > :nameLengthMin
```

Use {{ $.keyword("query/sorting-pagination-aggregation", ["Sorting, Pagination, and Aggregation"]) }} when the projection is sorted, paged, grouped, or aggregated.

The `filter` receiver is the generated `Selected` projection from the first query. It exposes `id`, `name`, and `nameLength`; it does not expose source fields that were not selected. `nameLengths.filter { ... }` is equivalent to `nameLengths.select().where { ... }`. Use an explicit outer `select { ... }` when that layer should also change the returned fields.
