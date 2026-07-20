{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 查询多个数据源

`join(...)` 用于构建可组合的 FROM source tree。每个右侧 source 对应一个明确的关系方法；所有关系建立完成后，调用 `select { ... }` 把 source 转成可执行查询。

```kotlin group="Basic join" name="kotlin" icon="kotlin"
val query = User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId }
        .select { [user.id, user.name, order.status] }
        .where { user.enabled == true }
}

val rows = query.toList()
```

```sql group="Basic join" name="Mysql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `order`.`status`
FROM `user`
LEFT JOIN `order` ON `user`.`id` = `order`.`user_id`
WHERE `user`.`enabled` = :enabled
```

source 参数只在外层 lambda 中声明一次，关系和查询子句直接捕获这些参数。`leftJoin`、`select`、`where` 和 `orderBy` 不需要重复声明参数。

## 选择连接类型

`innerJoin`、`leftJoin`、`rightJoin` 和 `fullJoin` 接收连接条件。笛卡尔积使用无条件的 `crossJoin()`。JOIN 不再提供隐式连接类型，也没有单独的 `on { ... }` 方法。

```kotlin group="Relation types" name="multiple sources" icon="kotlin"
val rows = User().join(Order(), Product()) { user, order, product ->
    innerJoin { user.id == order.userId }
        .leftJoin { order.productId == product.id }
        .select {
            [user.id, product.name.alias("productName"), order.amount]
        }
}.toList()
```

```kotlin group="Relation types" name="cross join" icon="kotlin"
val combinations = User().join(Region()) { user, region ->
    crossJoin()
        .select { [user.id, region.name.alias("regionName")] }
}.toList()
```

`fullJoin` 是否可用取决于数据库方言，见 {{ $.keyword("database/dialect-support", ["方言支持"]) }}。

## 选择、过滤、分组和排序

`select` 是原始 JOIN source 与查询之间的类型边界。调用 `select` 后，可以继续使用普通查询的 `where`、`by`、`groupBy`、`having`、`orderBy`、`distinct`、`limit`、`lock`、`patch` 和结果方法。

```kotlin group="Join query clauses" name="kotlin" icon="kotlin"
val rows = User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId }
        .select {
            [
                user.id.alias("userId"),
                f.count(order.id).alias("orderCount")
            ]
        }
        .where { user.enabled == true }
        .groupBy { user.id }
        .having { f.count(order.id) > 0 }
        .orderBy { it.orderCount.desc() }
}.toList()
```

`where`、`groupBy` 和 `having` 读取 joined Source 字段。`orderBy` 读取 select 后的 Context，因此可以访问 `orderCount` 这类选中 alias。

## 连接选中查询或分页 source

任何 `KSelectable` 都可以作为 JOIN operand，包括普通 select、已选中的 JOIN、union 和 offset page。对应 source 参数会暴露该 operand 的选中输出字段。

```kotlin group="Derived join source" name="selected source" icon="kotlin"
val paidOrders = Order()
    .select { [it.userId, it.status] }
    .where { it.status == 1 }

val rows = User().join(paidOrders) { user, order ->
    leftJoin { user.id == order.userId }
        .select { [user.id, user.name, order.status] }
}.toList()
```

```kotlin group="Derived join source" name="finite page source" icon="kotlin"
val recentOrders = Order()
    .select { [it.id, it.userId, it.createdAt] }
    .orderBy { it.createdAt.desc() }
    .page(pageIndex = 1, pageSize = 100)

val rows = User().join(recentOrders) { user, order ->
    innerJoin { user.id == order.userId }
        .select { [user.id, order.id.alias("orderId")] }
}.toList()
```

只有 `OffsetPageQuery` 可以继续作为关系 source；带总数分页和游标分页是执行阶段，不能作为 JOIN operand。

## 构建嵌套 JOIN tree

内层 JOIN block 以关系方法结束时会保留原始 `JoinSource`。这个 source 可以作为另一层 JOIN 的左侧或右侧，并保留括号分组。

```kotlin group="Nested join" name="right nested" icon="kotlin"
val companyRegion = Company().join(Region()) { company, region ->
    innerJoin { company.regionId == region.id }
}

val rows = User().join(companyRegion) { user, company, region ->
    leftJoin { user.companyId == company.id }
        .select {
            [user.id, company.name, region.name.alias("regionName")]
        }
}.toList()
```

生成的 SQL 形态是 `User LEFT JOIN (Company INNER JOIN Region)`。原始 `JoinSource` 不能直接执行、过滤、排序或分页，外层必须先调用 `select`。

## 组合已选中的 JOIN

JOIN `select` 返回 `KSelectable`，可以继续作为派生 source、参与 `union`、用于 INSERT SELECT，或再次 join。

```kotlin group="Join composition" name="derived" icon="kotlin"
val joined = User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId }
        .select { [user.id, order.status.alias("orderStatus")] }
}

val active = joined
    .select { [it.id, it.orderStatus] }
    .where { it.orderStatus == 1 }
    .toList()
```

```kotlin group="Join composition" name="union" icon="kotlin"
val all = union(joined, joined)
    .select { [it.id, it.orderStatus] }
    .toList()
```

## 自连接与重复输出名

自连接的两个 operand 即使使用同一种 KPojo 类型，也由不同 source 参数表示。建议使用体现角色的 alias。

```kotlin group="Self join" name="aliases" icon="kotlin"
val hierarchy = Employee().join(Employee()) { manager, report ->
    leftJoin { manager.id == report.managerId }
        .select {
            [
                manager.id.alias("managerId"),
                report.id.alias("reportId")
            ]
        }
}.toList()
```

确实需要重复名称时，opt-in 确定性后缀分配。第一次出现保留原名，后续值使用 `_1`、`_2` 等后缀。

```kotlin group="Self join" name="duplicate names" icon="kotlin"
import com.kotlinorm.annotations.UnsafeProjectionOverride

@OptIn(UnsafeProjectionOverride::class)
val rows = User().join(Company()) { user, company ->
    leftJoin { user.companyId == company.id }
        .select { [user.id, company.id] }
}.toList()

val userId = rows.first().id
val companyId = rows.first().id_1
```

显式名称保留和 Context 遮蔽规则见 {{ $.keyword("query/projection", ["投影"]) }}。

## 分页 JOIN 查询

分页在 `select` 后调用。offset page 直接返回记录；需要总数时，在该 page 上调用 `withTotal()`，结果是命名的 `PageResult`。

```kotlin group="Join page" name="kotlin" icon="kotlin"
val query = User().join(Order()) { user, order ->
    innerJoin { user.id == order.userId }
        .select { [user.id, order.status] }
        .orderBy { it.id.asc() }
}

val records = query.page(pageIndex = 1, pageSize = 20).toList()
val page = query.page(pageIndex = 1, pageSize = 20).withTotal().toList()

val total = page.total
val pageRecords = page.records
```

游标和派生分页契约见 {{ $.keyword("query/sorting-pagination-aggregation", ["排序、分页与聚合"]) }}。

## 选择结果形态和数据库

JOIN 查询和普通 select 一样支持生成投影、DTO、Map、单行结果方法和显式 `KronosDataSourceWrapper`。

```kotlin group="Join result" name="wrapper" icon="kotlin"
val rows = User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId }
        .select { [user.id, order.status] }
        .db(order to "archive")
}.toMapList(customWrapper)
```

终端方法行为见 {{ $.keyword("query/result-methods", ["结果方法"]) }}。
