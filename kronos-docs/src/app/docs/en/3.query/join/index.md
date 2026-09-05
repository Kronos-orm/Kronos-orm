{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Query from multiple sources

`join(...)` creates a composable FROM source tree. Declare one relation method for every right-side source, then call `select { ... }` to turn the completed source into an executable query.

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

The source parameters are declared once in the outer lambda and captured by relation and query clauses. Do not repeat them on `leftJoin`, `select`, `where`, or `orderBy`.

## Choose a relation type

Use `innerJoin`, `leftJoin`, `rightJoin`, or `fullJoin` with a condition. Use conditionless `crossJoin()` for a Cartesian product. There is no implicit join type or separate `on { ... }` method.

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

`fullJoin` support depends on the selected database dialect. See {{ $.keyword("database/dialect-support", ["Dialect Support"]) }}.

## Select, filter, group, and order

`select` is the type boundary between a raw JOIN source and a query. After `select`, use the same `where`, `by`, `groupBy`, `having`, `orderBy`, `distinct`, `limit`, `lock`, `patch`, and result methods as other selected queries.

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

`where`, `groupBy`, and `having` use the joined Source fields. `orderBy` uses the post-select Context and can read selected aliases such as `orderCount`.

## Join a selected or paged source

Any `KSelectable`, including a selected query, a joined query, a union, or an offset page, can be a JOIN operand. The corresponding source parameter exposes that operand's selected output fields.

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

`OffsetPageQuery` is selectable. Total-page and cursor-page execution stages are not relational sources.

## Build a nested JOIN tree

End an inner JOIN block with its relation call to keep a raw `JoinSource`. That source can become either side of another join without flattening its grouping.

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

The SQL shape is `User LEFT JOIN (Company INNER JOIN Region)`. A raw `JoinSource` cannot execute, filter, sort, or page until the outer layer calls `select`.

## Compose a selected JOIN

The value returned by JOIN `select` is a `KSelectable`. It can become a derived source, participate in `union`, feed INSERT SELECT, or be joined again.

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

## Self-join and duplicate output names

Self-joins use distinct source parameters even when both operands have the same KPojo type. Prefer aliases that describe each role.

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

If repeated names are intentional, opt in to deterministic suffixing. The first value keeps its name and later values receive `_1`, `_2`, and so on.

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

See {{ $.keyword("query/projection", ["Projection"]) }} for explicit-name reservation and Context shadowing rules.

## Page a joined query

Apply pagination after `select`. An offset page returns rows directly; call `withTotal()` on that page for a named `PageResult`.

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

Cursor and derived-page contracts are covered in {{ $.keyword("query/sorting-pagination-aggregation", ["Sorting, Pagination, and Aggregation"]) }}.

## Choose result and database

Joined queries support generated projection rows, DTOs, map results, single-row methods, and an explicit `KronosDataSourceWrapper` exactly like normal selects.

```kotlin group="Join result" name="wrapper" icon="kotlin"
val rows = User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId }
        .select { [user.id, order.status] }
        .db(order to "archive")
}.toMapList(customWrapper)
```

See {{ $.keyword("query/result-methods", ["Result Methods"]) }} for terminal method behavior.
