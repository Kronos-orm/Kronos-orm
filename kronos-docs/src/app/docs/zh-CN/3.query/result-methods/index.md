{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 查询结果方法

`select` 和 `join` 查询使用同一组终端方法。根据需要接收的结果形态选择方法。

投影字段和 alias 见 {{ $.keyword("query/projection", ["投影"]) }}。分页 Pair 结果见 {{ $.keyword("query/sorting-pagination-aggregation", ["排序、分页与聚合"]) }}。

## {{ $.title("toMapList") }} 返回 Map 列表

需要把每行作为 `Map<String, Any?>` 接收时，使用 `toMapList()`；它等价于 `toList<Map<String, Any?>>()`。

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

返回结果是 Map 列表。

```kotlin group="toMapList result" name="kotlin" icon="kotlin"
listOf(
    mapOf("id" to 1, "name" to "Ada"),
    mapOf("id" to 2, "name" to "Grace")
)
```

## {{ $.title("toList") }} 返回类型列表

当查询列可以映射到目标类型时，使用 `toList()`。

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

查询单列或自定义行类型时，显式传入泛型。

```kotlin group="toList generic" name="kotlin" icon="kotlin"
val ids: List<Int> = User()
    .select { it.id }
    .toList<Int>()

val rows: List<UserSummary> = User()
    .select { [it.id, it.name] }
    .toList<UserSummary>()
```

生成投影也可以直接作为返回类型使用。

```kotlin group="toList projection 1" name="kotlin" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .toList()

val firstLength = rows.first().nameLength
```

上面的无参调用返回编译器生成的投影类型。业务代码中需要命名 DTO 时，使用 `select(UserSummary::class) { ... }`。

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

投影 alias 规则和生成结果形态见 {{ $.keyword("query/projection", ["投影"]) }}。

## {{ $.title("toMap") }} 返回一条 Map

期望返回一行 Map 时，使用 `toMap()`，也可以写成 `first<Map<String, Any?>>()`。Kronos 会为查询加上 `LIMIT 1`。

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

`toMap()` 返回第一行 Map。

```kotlin group="toMap result" name="kotlin" icon="kotlin"
mapOf("id" to 1, "name" to "Ada")
```

## {{ $.title("toMapOrNull") }} 返回可空 Map

查询结果为空也属于预期情况时，使用 `toMapOrNull()`，也可以写成 `firstOrNull<Map<String, Any?>>()` 或 `first<Map<String, Any?>?>()`。一般情况下，`firstOrNull<T>()` 等价于 `first<T?>()`。

```kotlin group="toMapOrNull" name="kotlin" icon="kotlin"
val row: Map<String, Any?>? = User()
    .select { [it.id, it.name] }
    .where { it.id == 404 }
    .toMapOrNull()
```

```kotlin group="toMapOrNull result" name="kotlin" icon="kotlin"
null
```

## {{ $.title("first") }} 返回一条类型结果

期望返回一行并映射到目标类型时，使用 `first()`。Kronos 会为查询加上 `LIMIT 1`。

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

查询单列或自定义行类型时，显式传入泛型。

```kotlin group="first generic" name="kotlin" icon="kotlin"
val name: String = User()
    .select { it.name }
    .where { it.id == 1 }
    .first<String>()
```

生成投影也可以用于单行结果方法。

```kotlin group="first projection" name="kotlin" icon="kotlin"
val row = User()
    .select { [it.id, it.name.alias("username")] }
    .where { it.id == 1 }
    .first()

val username = row.username
```

生成行可以像一个小结果对象一样使用，属性来自选中的字段和 alias。

## {{ $.title("firstOrNull") }} 返回可空类型结果

查询结果为空时需要返回 `null`，使用 `firstOrNull()`。

```kotlin group="firstOrNull" name="kotlin" icon="kotlin"
val user: User? = User()
    .select()
    .where { it.id == 404 }
    .firstOrNull()
```

```kotlin group="firstOrNull result" name="kotlin" icon="kotlin"
null
```

## 在 join 后使用结果方法

Join 查询使用和 `select` 相同的结果方法。

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

## 在分页后使用结果方法

分页结果需要包含总行数时，使用 `withTotal()`。

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

## 使用指定数据源

当本次查询需要使用指定数据源时，把 `KronosDataSourceWrapper` 传给任意终端方法。

```kotlin group="Wrapper" name="kotlin" icon="kotlin"
val customWrapper = CustomWrapper()

val users: List<User> = User()
    .select()
    .where { it.age >= 18 }
    .toList(customWrapper)
```

Wrapper 配置见 {{ $.keyword("database/datasource-wrapper", ["Kronos Data Source Wrapper"]) }}。
