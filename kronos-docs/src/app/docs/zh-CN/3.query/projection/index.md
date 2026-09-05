{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 投影

投影是传给 `select { ... }` 或 `join { select { ... } }` 的字段列表。它用于选择返回列、用 `alias` 命名表达式，并决定 `toMapList()`、`toList()`、`first()` 和 join 查询的结果形态。

结果方法和可空单行方法见 {{ $.keyword("query/result-methods", ["结果方法"]) }}。

## 选择字段

查询单列时直接返回字段，查询多列时使用 `[]`。

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

多字段查询会返回一个包含所选字段名的生成投影。通常不需要为这个类命名；编译器会为本次 select 调用生成结果类型，Kotlin 类型推断会让这些属性可以直接访问。

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

运行时生成类是内部的 `KronosSelectResult_...` 类型。文档中的 data class 用来说明公开可用的属性形态，不表示稳定类名。

## 使用 alias 命名结果属性

当列、函数、聚合、标量子查询或原生 SQL 表达式需要指定结果名时，使用 `.alias("name")`。

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

alias 会成为 `toMapList()` 的 Map key，也会成为生成投影的属性名。

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

非直接字段需要 alias；直接字段需要换成另一个结果属性名时，也使用 alias。

## 处理重复输出名

如果多个投影项请求相同输出名，默认会产生编译错误。只有在声明或表达式上 opt-in `UnsafeProjectionOverride` 后，Kronos 才会保留全部选中值：第一次出现保留原名，后续值依次使用 `_1`、`_2` 等后缀。

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

Kronos 会先保留全部显式请求名，再分配后缀。因此 `id`、`id`、`id_1` 会解析成 `id`、`id_2`、`id_1`，而不是 `id`、`id_1`、`id_1`。SQL label、生成属性、类型化映射、Map key、派生查询和 union 输出都使用同一组名称。

不同值具有不同业务含义时，优先使用 `userId`、`orderId` 这类显式 alias。只有确实需要保留重复名称时才使用 opt-in。

### 替换或遮蔽源字段名

投影 alias 可以复用源字段名；如果同层 Context 没有读取该名称，则不需要 opt-in。`where`、`groupBy` 和 `having` 仍读取 Source。`orderBy` 读取 select 后的 Context，因此排序读取被遮蔽名称时需要 opt-in，并解析为选中值及其 Kotlin 类型。

```kotlin group="Context shadow" name="opt-in" icon="kotlin"
@OptIn(UnsafeProjectionOverride::class)
val ordered = User()
    .select { [it.id, f.length(it.name).alias("name")] }
    .where { it.name != null }       // Source.name
    .orderBy { it.name.desc() }      // 选中的 Int? name
```

如果本意就是替换源字段，可以先移除源字段，再用相同 alias 恢复；这种写法不需要 opt-in。

```kotlin group="Context shadow" name="source minus" icon="kotlin"
val replaced = User()
    .select { [it - it.name, f.length(it.name).alias("name")] }
    .orderBy { it.name.desc() }
```

## 使用原生 SQL select item

字符串表达式可以作为原生 SQL select item。原生表达式需要作为 Map key 或生成投影属性返回时，使用 `.alias("name")`。

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

`"count(*)".alias("total")` 会保留 `count(*)` 作为 SQL 表达式，并使用 `total` 作为结果名。原生 select item 适合数据库专用表达式。需要绑定值时，把参数放在 `where { ... }` 和 `patch(...)` 中。

## 消费投影结果

需要每行作为 Map 时，使用 `toMapList()`。Map key 来自字段名和 alias。

```kotlin group="Consume projection 1" name="map" icon="kotlin"
val maps: List<Map<String, Any?>> = User()
    .select { [it.id, it.name.alias("username")] }
    .toMapList()

val first = maps.first()
val id = first["id"]
val username = first["username"]
```

需要编译器生成投影类型时，`toList()` 或 `first()` 不传泛型。

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

结果类型需要在业务代码中显式命名时，定义 DTO，并把 class 传给 `select(...)`。

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

select 输出名需要和要填充的 DTO 属性名对应。

## 完整投影、排除列与 []

`select { it }`、`select { [it] }` 和 `select { listOf(it) }` 都和 `select()` 一样返回源 KPojo 类型。`-` 可以排除字段；排除列或和其他投影项混合时，会生成投影结果类型。

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

`-` 右侧也可以使用 `[]`，所以 `[it - [it.id, it.age], it.id.alias("sourceId")]` 也是合法的投影列表。

完整投影还可以和普通字段、alias 或函数投影放在同一个列表中。展开后的字段保持原顺序，后续项继续追加到生成结果类型。

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

多数表字段都需要返回、只跳过少数字段，或需要在完整字段后追加 alias 时，可以使用 `it - ...`。解析后的输出名始终唯一；请求重复名称时需要使用前述 opt-in。

## 在 join 查询中投影

join 块中的 `select { ... }` 可以读取每张表的字段。当多张表有同名字段，或结果类型需要稳定属性名时，使用 alias。

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

join 语法和 join 过滤见 {{ $.keyword("query/join", ["连表查询"]) }}。

## 投影标量子查询

标量子查询返回一列时，可以放进 select 列表。配合 `limit(1)` 使用，并为子查询设置 alias。

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

子查询形式、派生查询源和投影可见性规则见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## 筛选派生查询结果

已选择的投影可以成为下一层查询源。alias 需要在选择后参与筛选，并且输出形态应保持不变时，使用 `filter { ... }`。

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

投影需要排序、分页、分组或聚合时，见 {{ $.keyword("query/sorting-pagination-aggregation", ["排序、分页与聚合"]) }}。

`filter` 的 receiver 是第一层查询生成的 `Selected` 投影。它暴露 `id`、`name` 和 `nameLength`，没有被第一层选出的 source 字段不会出现在这个 receiver 上。`nameLengths.filter { ... }` 等价于 `nameLengths.select().where { ... }`；外层还需要改变返回字段时，继续使用显式 `select { ... }`。
