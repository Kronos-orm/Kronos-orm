{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Query result methods

`select` and `join` queries use the same terminal methods. Choose the method by the result shape you want to receive.

Projection fields and aliases are covered in {{ $.keyword("query/projection", ["Projection"]) }}. Pagination result pairs are covered in {{ $.keyword("query/sorting-pagination-aggregation", ["Sorting, Pagination, and Aggregation"]) }}.

## {{ $.title("query") }} returns a map list

Use `query()` when you want each row as `Map<String, Any>`.

```kotlin group="query" name="kotlin" icon="kotlin"
val rows: List<Map<String, Any>> = User()
    .select { [it.id, it.name] }
    .where { it.age >= 18 }
    .query()
```

```sql group="query" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `user`.`age` >= :ageMin
```

The result shape is a list of maps.

```kotlin group="query result" name="kotlin" icon="kotlin"
listOf(
    mapOf("id" to 1, "name" to "Ada"),
    mapOf("id" to 2, "name" to "Grace")
)
```

## {{ $.title("queryList") }} returns a typed list

Use `queryList()` when the selected columns map to the query projection type.

```kotlin group="queryList" name="kotlin" icon="kotlin"
val users: List<User> = User()
    .select()
    .where { it.age >= 18 }
    .queryList()
```

```sql group="queryList" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`age` >= :ageMin
```

Use a generic argument for scalar or custom row types.

```kotlin group="queryList generic" name="kotlin" icon="kotlin"
val ids: List<Int> = User()
    .select { it.id }
    .queryList<Int>()

val rows: List<UserSummary> = User()
    .select { [it.id, it.name] }
    .queryList<UserSummary>()
```

Generated projections can also be returned directly.

```kotlin group="queryList projection 1" name="kotlin" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .queryList()

val firstLength = rows.first().nameLength
```

The no-argument call above returns a compiler-generated projection type. Use `select(UserSummary::class) { ... }` when you want a named DTO in your code.

```kotlin group="queryList projection 2" name="dto" icon="kotlin"
data class UserSummary(
    var id: Int? = null,
    var nameLength: Int? = null
) : KPojo

val summaries: List<UserSummary> = User()
    .select(UserSummary::class) {
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .queryList()
```

Projection alias rules and generated result shapes are covered in {{ $.keyword("query/projection", ["Projection"]) }}.

## {{ $.title("queryMap") }} returns one map

Use `queryMap()` when you expect one row as a map. Kronos applies `LIMIT 1` to the query.

```kotlin group="queryMap" name="kotlin" icon="kotlin"
val row: Map<String, Any> = User()
    .select { [it.id, it.name] }
    .where { it.id == 1 }
    .queryMap()
```

```sql group="queryMap" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `user`.`id` = :id
LIMIT 1
```

`queryMap()` returns the first row as a map.

```kotlin group="queryMap result" name="kotlin" icon="kotlin"
mapOf("id" to 1, "name" to "Ada")
```

## {{ $.title("queryMapOrNull") }} returns one nullable map

Use `queryMapOrNull()` when an empty result is an expected outcome.

```kotlin group="queryMapOrNull" name="kotlin" icon="kotlin"
val row: Map<String, Any>? = User()
    .select { [it.id, it.name] }
    .where { it.id == 404 }
    .queryMapOrNull()
```

```kotlin group="queryMapOrNull result" name="kotlin" icon="kotlin"
null
```

## {{ $.title("queryOne") }} returns one typed row

Use `queryOne()` when you expect one row mapped to a type. Kronos applies `LIMIT 1` to the query.

```kotlin group="queryOne" name="kotlin" icon="kotlin"
val user: User = User()
    .select()
    .where { it.id == 1 }
    .queryOne()
```

```sql group="queryOne" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `user`.`id` = :id
LIMIT 1
```

Use a generic argument for scalar or custom row types.

```kotlin group="queryOne generic" name="kotlin" icon="kotlin"
val name: String = User()
    .select { it.name }
    .where { it.id == 1 }
    .queryOne<String>()
```

Generated projections work with single-row methods as well.

```kotlin group="queryOne projection" name="kotlin" icon="kotlin"
val row = User()
    .select { [it.id, it.name.alias("username")] }
    .where { it.id == 1 }
    .queryOne()

val username = row.username
```

The generated row behaves like a small result object whose properties are the selected fields and aliases.

## {{ $.title("queryOneOrNull") }} returns one nullable typed row

Use `queryOneOrNull()` when an empty result should return `null`.

```kotlin group="queryOneOrNull" name="kotlin" icon="kotlin"
val user: User? = User()
    .select()
    .where { it.id == 404 }
    .queryOneOrNull()
```

```kotlin group="queryOneOrNull result" name="kotlin" icon="kotlin"
null
```

## Use result methods after joins

Join queries use the same result methods as `select`.

```kotlin group="Join result" name="kotlin" icon="kotlin"
val rows: List<UserOrderRow> = User().join(Order()) { user, order ->
    leftJoin(order) { user.id == order.userId }
    select { [user.id, user.name, order.status] }
}.queryList<UserOrderRow>()
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
val (total, rows): Pair<Int, List<Map<String, Any>>> = User()
    .select { [it.id, it.name] }
    .page(1, 20)
    .withTotal()
    .query()

val (typedTotal, users): Pair<Int, List<User>> = User()
    .select()
    .page(1, 20)
    .withTotal()
    .queryList()
```

## Use a specific data source

Pass a `KronosDataSourceWrapper` to any terminal method when this query should use a specific data source.

```kotlin group="Wrapper" name="kotlin" icon="kotlin"
val customWrapper = CustomWrapper()

val users: List<User> = User()
    .select()
    .where { it.age >= 18 }
    .queryList(customWrapper)
```

Wrapper configuration is covered in {{ $.keyword("database/datasource-wrapper", ["Kronos Data Source Wrapper"]) }}.
