{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Query from multiple tables

In Kronos, we can use `KPojo.join(KPojo1, KPojo2, ...)` to query associated data from multiple tables.

`KSelectable` query results can also be consumed as derived join sources. See {{ $.keyword("query/subqueries", ["Subqueries"]) }} for derived query source rules.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

## Join a selectable source

A `KSelectable` built from `select { ... }` can be passed to `join(...)`. The right-side parameter exposes the selected fields and aliases from the derived source.

```kotlin group="Join source 1" name="kotlin" icon="kotlin"
val paidOrders = Order()
    .select { [it.userId, it.status] }
    .where { it.status == 1 }

val users = User().join(paidOrders) { user, order ->
    leftJoin(order) { user.id == order.userId }
    select { [user.id, user.name, order.status] }
}.toList()
```

```sql group="Join source 1" name="Mysql" icon="mysql"
SELECT `user`.`id` AS `id`,
       `user`.`name` AS `name`,
       `q`.`status` AS `status`
FROM `user`
LEFT JOIN (
    SELECT `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`status` = :status
) AS `q`
ON `user`.`id` = `q`.`userId`
```

The join result can keep using `orderBy`, `page`, and `withTotal()`.

```kotlin group="Join source 2" name="paged source" icon="kotlin"
val (total, rows) = User().join(paidOrders) { user, order ->
    leftJoin(order) { user.id == order.userId }
    orderBy { order.status.desc() }
    page(1, 10)
    select { [user.id, user.name, order.status] }
}.withTotal().toMapList()
```

```sql group="Join source 2" name="paged sql" icon="mysql"
SELECT `user`.`id` AS `id`,
       `user`.`name` AS `name`,
       `q`.`status` AS `status`
FROM `user`
LEFT JOIN (
    SELECT `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`status` = :status
) AS `q`
ON `user`.`id` = `q`.`userId`
ORDER BY `q`.`status` DESC
LIMIT 10
OFFSET 0
```

## Specify the join condition and join type

In Kronos, we use `left join` to connect multiple tables by default. If you don't need to specify the connection method, you can use the `on` method to specify the connection conditions.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

You can specify both the join method and the join condition with the `leftJoin`, `rightJoin`, `innerJoin`, `crossJoin`, and `fullJoin` functions.

```kotlin name="demo" icon="kotlin" {2-6}
val users: List<User> =
    User().join(UserInfo(), UserTeam()) { user, userInfo, userTeam ->
        leftJoin { user.id == userInfo.userId }
        innerJoin { user.id == userTeam.userId }
        select { [user.id, user.name, userInfo.age, userTeam.teamId] }
    }.toList()
```

```sql name="join type sql" icon="mysql"
SELECT `user`.`id`,
       `user`.`name`,
       `user_info`.`age`,
       `user_team`.`team_id` AS `teamId`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
INNER JOIN `user_team` ON `user`.`id` = `user_team`.`user_id`
```

## {{ $.title("db") }}Specify the database name(cross-database join)

In Kronos, we can use the `db` method to specify the database name.

Combine one or more tables with their respective database names and pass them as parameters to the method using one or more `Pair` classes for cross-database joint queries.

```kotlin name="demo" icon="kotlin" {4}
val users: List<User> =
    User().join(UserInfo(), UserTeam(), UserRole()) { user, userInfo, userTeam, userRole ->
        on { user.id == userInfo.userId && user.id == userTeam.userId && user.id == userRole.userId }
        db(userInfo to "user_info_database", userRole to "user_role_database")
        select { [user.id, user.name, userInfo.age, userTeam.teamId, userRole.roleName] }
    }.toList()
```

```sql name="cross database sql" icon="mysql"
SELECT `user`.`id`,
       `user`.`name`,
       `user_info_database`.`user_info`.`age`,
       `user_team`.`team_id` AS `teamId`,
       `user_role_database`.`user_role`.`role_name` AS `roleName`
FROM `user`
LEFT JOIN `user_info_database`.`user_info` ON `user`.`id` = `user_info_database`.`user_info`.`user_id`
LEFT JOIN `user_team` ON `user`.`id` = `user_team`.`user_id`
LEFT JOIN `user_role_database`.`user_role` ON `user`.`id` = `user_role_database`.`user_role`.`user_id`
```

## {{ $.title("select") }}Specify the query fields

In Kronos, we can specify query fields using the `select` method, with `[]` for multiple fields.

You can use `alias` to specify aliases for the field, such as `select { [user.id, user.name.alias("userName"), userInfo.age] }`.

If you need to query all fields of a table, you can use `select { user }`, `select { [user, userInfo, userTeam.teamId] }`.

When you don't specify query fields, all fields are queried by default. We will rename the same fields in different tables to avoid field conflicts.

Strings can be used as custom query fields, such as `select { "count(`user.id`)".alias("count") }`.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

```text name="result shape"
rows.first().id
rows.first().name
rows.first().age
```

### Query all fields or exclude some columns

You can pass `KPojo` to query all columns, use `-` to exclude columns, and use `[]` to combine the final select list.

```kotlin name="kotlin" icon="kotlin" {6}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user - user.id, userInfo.age] }
    }
        .toList()
```

## {{ $.title("by") }}Specifying Query Fields

In Kronos, we can use `by` method to specify the query fields, with `[]` for multiple fields.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        by { [user.id, user.name, userInfo.age] }
    }.toList()
```

## {{ $.title("where") }}Specify the query conditions

In Kronos, we can use the `where` method to specify the query {{ $.keyword("query/conditions", ["conditional expression"]) }}.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        where { user.id == 1 }
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

```sql name="where sql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
WHERE `user`.`id` = :id
```

Inside an explicit `where` block, `.eq` can expand the current KPojo object's field values into equality conditions, and you can combine those conditions with other expressions.

```kotlin name="kotlin" icon="kotlin" {6}
val users: List<User> =
    User(1, "Kronos").join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        where { user.eq }
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

Kronos supports the minus operator `-` to exclude fields from the explicit `.eq` expansion.

```kotlin name="kotlin" icon="kotlin" {6}
val users: List<User> =
    User(1, "Kronos").join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        where { (user - user.id).eq }
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

## {{ $.title("patch") }}Add parameters to custom query conditions

In Kronos, we can use the `patch` method to add parameters to a custom query condition.

```kotlin name="demo" icon="kotlin" {2-7}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
        where { "user.id = :id".asSql() }
        patch("id" to 1)
    }.toList()
```

## {{ $.title("groupBy") }}, {{ $.title("having") }}Set grouping and aggregation conditions

In Kronos, we can specify grouping fields using the `groupBy` method and specify aggregation conditions using the `having` method.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        groupBy { [user.id, userInfo.age] }
        having { userInfo.age > 18 }
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

```sql name="group sql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
GROUP BY `user`.`id`, `user_info`.`age`
HAVING `user_info`.`age` > :age
```

## {{ $.title("orderBy") }}Set sorting conditions

In Kronos, we can specify sorting conditions using the `orderBy` method.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        orderBy { [user.id.asc(), userInfo.age.desc()] }
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

```sql name="order sql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
ORDER BY `user`.`id` ASC, `user_info`.`age` DESC
```

## {{ $.title("limit") }}Specify the maximum number of records to query

In Kronos, we can specify the maximum number of records to query using the `limit` method.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        limit(10)
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

```sql name="limit sql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
LIMIT 10
```

## {{ $.title("distinct") }}Specify query deduplication

In Kronos, we can specify query deduplication using the `distinct` method.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        distinct()
        select { [user.id, user.name, userInfo.age] }
    }.toList()
```

```sql name="distinct sql" icon="mysql"
SELECT DISTINCT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
```

## {{ $.title("page") }}, {{ $.title("withTotal") }}Query paging

The `page` method is used to specify paging queries, please note that the `page` method parameter starts from 1.

In different databases, paging queries have different syntaxes, Kronos generates paging queries based on different databases.

The `withTotal` method is used to query paging queries with total record counts.

> **Warning**
> After using the `page` method, the result of the query **will not** include the total number of records by default, if you need to query the total number of records, be sure to use the `withTotal` method.

```kotlin name="demo" icon="kotlin" {2-5}
val (total, list) =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        page(1, 10)
        select { [user.id, user.name, userInfo.age] }
    }.withTotal().toMapList()
```

```sql name="page sql" icon="mysql"
SELECT COUNT(*) FROM (
    SELECT 1
    FROM `user`
    LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
) AS total_count

SELECT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
LIMIT 10
OFFSET 0
```

## Result methods

Join queries use the same terminal result methods as select queries.

```kotlin name="Result methods" icon="kotlin"
val mapRows: List<Map<String, Any?>> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.toMapList()

val typedRows: List<UserInfoRow> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.toList<UserInfoRow>()
```

Result shapes, single-row methods, pagination totals, and custom wrappers are covered in {{ $.keyword("query/result-methods", ["Result Methods"]) }}.

## Specify the data source to use

Pass `KronosDataSourceWrapper` to a terminal result method when this join query should use a specific database connection.

```kotlin name="demo" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.toList(customWrapper)
```
