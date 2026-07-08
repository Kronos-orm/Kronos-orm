{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 查询结果方法

`select` 和 `join` 查询使用同一组终端方法。根据需要接收的结果形态选择方法。

投影字段和 alias 见 {{ $.keyword("query/projection", ["投影"]) }}。分页 Pair 结果见 {{ $.keyword("query/sorting-pagination-aggregation", ["排序、分页与聚合"]) }}。

## {{ $.title("query") }} 返回 Map 列表

需要把每行作为 `Map<String, Any>` 接收时，使用 `query()`。

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

返回结果是 Map 列表。

```kotlin group="query result" name="kotlin" icon="kotlin"
listOf(
    mapOf("id" to 1, "name" to "Ada"),
    mapOf("id" to 2, "name" to "Grace")
)
```

## {{ $.title("queryList") }} 返回类型列表

当查询列可以映射到目标类型时，使用 `queryList()`。

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

查询单列或自定义行类型时，显式传入泛型。

```kotlin group="queryList generic" name="kotlin" icon="kotlin"
val ids: List<Int> = User()
    .select { it.id }
    .queryList<Int>()

val rows: List<UserSummary> = User()
    .select { [it.id, it.name] }
    .queryList<UserSummary>()
```

生成投影也可以直接作为返回类型使用。

```kotlin group="queryList projection 1" name="kotlin" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .queryList()

val firstLength = rows.first().nameLength
```

上面的无参调用返回编译器生成的投影类型。业务代码中需要命名 DTO 时，使用 `select(UserSummary::class) { ... }`。

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

投影 alias 规则和生成结果形态见 {{ $.keyword("query/projection", ["投影"]) }}。

## {{ $.title("queryMap") }} 返回一条 Map

期望返回一行 Map 时，使用 `queryMap()`。Kronos 会为查询加上 `LIMIT 1`。

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

`queryMap()` 返回第一行 Map。

```kotlin group="queryMap result" name="kotlin" icon="kotlin"
mapOf("id" to 1, "name" to "Ada")
```

## {{ $.title("queryMapOrNull") }} 返回可空 Map

查询结果为空也属于预期情况时，使用 `queryMapOrNull()`。

```kotlin group="queryMapOrNull" name="kotlin" icon="kotlin"
val row: Map<String, Any>? = User()
    .select { [it.id, it.name] }
    .where { it.id == 404 }
    .queryMapOrNull()
```

```kotlin group="queryMapOrNull result" name="kotlin" icon="kotlin"
null
```

## {{ $.title("queryOne") }} 返回一条类型结果

期望返回一行并映射到目标类型时，使用 `queryOne()`。Kronos 会为查询加上 `LIMIT 1`。

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

查询单列或自定义行类型时，显式传入泛型。

```kotlin group="queryOne generic" name="kotlin" icon="kotlin"
val name: String = User()
    .select { it.name }
    .where { it.id == 1 }
    .queryOne<String>()
```

生成投影也可以用于单行结果方法。

```kotlin group="queryOne projection" name="kotlin" icon="kotlin"
val row = User()
    .select { [it.id, it.name.alias("username")] }
    .where { it.id == 1 }
    .queryOne()

val username = row.username
```

生成行可以像一个小结果对象一样使用，属性来自选中的字段和 alias。

## {{ $.title("queryOneOrNull") }} 返回可空类型结果

查询结果为空时需要返回 `null`，使用 `queryOneOrNull()`。

```kotlin group="queryOneOrNull" name="kotlin" icon="kotlin"
val user: User? = User()
    .select()
    .where { it.id == 404 }
    .queryOneOrNull()
```

```kotlin group="queryOneOrNull result" name="kotlin" icon="kotlin"
null
```

## 在 join 后使用结果方法

Join 查询使用和 `select` 相同的结果方法。

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

## 在分页后使用结果方法

分页结果需要包含总行数时，使用 `withTotal()`。

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

## 使用指定数据源

当本次查询需要使用指定数据源时，把 `KronosDataSourceWrapper` 传给任意终端方法。

```kotlin group="Wrapper" name="kotlin" icon="kotlin"
val customWrapper = CustomWrapper()

val users: List<User> = User()
    .select()
    .where { it.age >= 18 }
    .queryList(customWrapper)
```

Wrapper 配置见 {{ $.keyword("database/datasource-wrapper", ["Kronos Data Source Wrapper"]) }}。
