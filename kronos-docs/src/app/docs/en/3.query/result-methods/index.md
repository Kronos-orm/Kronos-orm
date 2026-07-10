{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Query result methods

`select` and `join` queries use the same terminal methods. Choose the method by the result shape you want to receive.

Projection fields and aliases are covered in {{ $.keyword("query/projection", ["Projection"]) }}. Pagination result pairs are covered in {{ $.keyword("query/sorting-pagination-aggregation", ["Sorting, Pagination, and Aggregation"]) }}.

## {{ $.title("toMapList") }} returns a map list

Use `toMapList()` when you want each row as `Map<String, Any?>`; it is equivalent to `toList<Map<String, Any?>>()`.

```kotlin group="toMapList" name="kotlin" icon="kotlin"
val rows: List<Map<String, Any?>> = User()
    .select { [it.id, it.name] }
    .where { it.age >= 18 }
    .toMapList()
```

```sql group="toMapList" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `user`.`age` >= :ageMin
```

The result shape is a list of maps.

```kotlin group="toMapList result" name="kotlin" icon="kotlin"
listOf(
    mapOf("id" to 1, "name" to "Ada"),
    mapOf("id" to 2, "name" to "Grace")
)
```

## {{ $.title("toList") }} returns a typed list

Use `toList()` when the selected columns map to the query projection type.

```kotlin group="toList" name="kotlin" icon="kotlin"
val users: List<User> = User()
    .select()
    .where { it.age >= 18 }
    .toList()
```

```sql group="toList" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`age` >= :ageMin
```

Use a generic argument for scalar or custom row types.

```kotlin group="toList generic" name="kotlin" icon="kotlin"
val ids: List<Int> = User()
    .select { it.id }
    .toList<Int>()

val rows: List<UserSummary> = User()
    .select { [it.id, it.name] }
    .toList<UserSummary>()
```

Generated projections can also be returned directly.

```kotlin group="toList projection 1" name="kotlin" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .toList()

val firstLength = rows.first().nameLength
```

The no-argument call above returns a compiler-generated projection type. Use `select(UserSummary::class) { ... }` when you want a named DTO in your code.

```kotlin group="toList projection 2" name="dto" icon="kotlin"
data class UserSummary(
    var id: Int? = null,
    var nameLength: Int? = null
) : KPojo

val summaries: List<UserSummary> = User()
    .select(UserSummary::class) {
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .toList()
```

Projection alias rules and generated result shapes are covered in {{ $.keyword("query/projection", ["Projection"]) }}.

## {{ $.title("toMap") }} returns one map

Use `toMap()` when you expect one row as a map; `first<Map<String, Any?>>()` is equivalent. Kronos applies `LIMIT 1` to the query.

```kotlin group="toMap" name="kotlin" icon="kotlin"
val row: Map<String, Any?> = User()
    .select { [it.id, it.name] }
    .where { it.id == 1 }
    .toMap()
```

```sql group="toMap" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `user`.`id` = :id
LIMIT 1
```

`toMap()` returns the first row as a map.

```kotlin group="toMap result" name="kotlin" icon="kotlin"
mapOf("id" to 1, "name" to "Ada")
```

## {{ $.title("toMapOrNull") }} returns one nullable map

Use `toMapOrNull()` when an empty result is an expected outcome; `firstOrNull<Map<String, Any?>>()` and `first<Map<String, Any?>?>()` are equivalent. In general, `firstOrNull<T>()` is equivalent to `first<T?>()`.

```kotlin group="toMapOrNull" name="kotlin" icon="kotlin"
val row: Map<String, Any?>? = User()
    .select { [it.id, it.name] }
    .where { it.id == 404 }
    .toMapOrNull()
```

```kotlin group="toMapOrNull result" name="kotlin" icon="kotlin"
null
```

## {{ $.title("first") }} returns one typed row

Use `first()` when you expect one row mapped to a type. Kronos applies `LIMIT 1` to the query.

```kotlin group="first" name="kotlin" icon="kotlin"
val user: User = User()
    .select()
    .where { it.id == 1 }
    .first()
```

```sql group="first" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`id` = :id
LIMIT 1
```

Use a generic argument for scalar or custom row types.

```kotlin group="first generic" name="kotlin" icon="kotlin"
val name: String = User()
    .select { it.name }
    .where { it.id == 1 }
    .first<String>()
```

Generated projections work with single-row methods as well.

```kotlin group="first projection" name="kotlin" icon="kotlin"
val row = User()
    .select { [it.id, it.name.alias("username")] }
    .where { it.id == 1 }
    .first()

val username = row.username
```

The generated row behaves like a small result object whose properties are the selected fields and aliases.

## {{ $.title("firstOrNull") }} returns one nullable typed row

Use `firstOrNull()` when an empty result should return `null`.

```kotlin group="firstOrNull" name="kotlin" icon="kotlin"
val user: User? = User()
    .select()
    .where { it.id == 404 }
    .firstOrNull()
```

```kotlin group="firstOrNull result" name="kotlin" icon="kotlin"
null
```

## Use result methods after joins

Join queries use the same result methods as `select`.

```kotlin group="Join result" name="kotlin" icon="kotlin"
val rows: List<UserOrderRow> = User().join(Order()) { user, order ->
    leftJoin(order) { user.id == order.userId }
    select { [user.id, user.name, order.status] }
}.toList<UserOrderRow>()
```

```sql group="Join result" name="Mysql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `order`.`status`
FROM `user`
LEFT JOIN `order`
ON `user`.`id` = `order`.`user_id`
```

## Use result methods after pagination

Use `withTotal()` when the page result should include a total row count.

```kotlin group="Page query" name="kotlin" icon="kotlin"
val (total, rows): Pair<Int, List<Map<String, Any?>>> = User()
    .select { [it.id, it.name] }
    .page(1, 20)
    .withTotal()
    .toMapList()

val (typedTotal, users): Pair<Int, List<User>> = User()
    .select()
    .page(1, 20)
    .withTotal()
    .toList()
```

## Use a specific data source

Pass a `KronosDataSourceWrapper` to any terminal method when this query should use a specific data source.

```kotlin group="Wrapper" name="kotlin" icon="kotlin"
val customWrapper = CustomWrapper()

val users: List<User> = User()
    .select()
    .where { it.age >= 18 }
    .toList(customWrapper)
```

Wrapper configuration is covered in {{ $.keyword("database/datasource-wrapper", ["Kronos Data Source Wrapper"]) }}.
