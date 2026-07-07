{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

本章将介绍如何查询多表关联数据（或许您也想看看{{ $.keyword("advanced/cascade-select", ["级联查询"]) }}）。

## 查询多表关联数据

在Kronos中，我们可以使用`KPojo.join(KPojo1, KPojo2, ...)`方法来查询多表关联数据。

`KSelectable` 查询结果也可以作为派生 join source 使用，规则见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

## Join 派生查询源

`select { ... }` 得到的 `KSelectable` 可以传给 `join(...)`。join lambda 右侧参数会暴露派生查询源中选中的字段和 alias。

```kotlin group="Join source 1" name="kotlin" icon="kotlin"
val paidOrders = Order()
    .select { [it.userId, it.status] }
    .where { it.status == 1 }

val users = User().join(paidOrders) { user, order ->
    leftJoin(order) { user.id == order.userId }
    select { [user.id, user.name, order.status] }
}.queryList()
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

join 结果可以继续使用 `orderBy`、`page` 和 `withTotal()`。

```kotlin group="Join source 2" name="paged source" icon="kotlin"
val (total, rows) = User().join(paidOrders) { user, order ->
    leftJoin(order) { user.id == order.userId }
    orderBy { order.status.desc() }
    page(1, 10)
    select { [user.id, user.name, order.status] }
}.withTotal().query()
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

## 指定连接条件及连接方式

在Kronos中，我们默认使用`left join`连接多表，如果不需要指定连接方式，可以使用`on`方法指定连接条件。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

可以通过`leftJoin`、`rightJoin`、`innerJoin`、`crossJoin`、`fullJoin`等函数同时指定连接方式和连接条件。

```kotlin name="demo" icon="kotlin" {2-6}
val users: List<User> =
    User().join(UserInfo(), UserTeam()) { user, userInfo, userTeam ->
        leftJoin { user.id == userInfo.userId }
        innerJoin { user.id == userTeam.userId }
        select { [user.id, user.name, userInfo.age, userTeam.teamId] }
    }.queryList()
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

## {{ $.title("db") }}指定连接数据表的数据库（跨库连表查询）

在Kronos中，我们可以使用`db`方法指定查询字段。

将一张或多张表与其所处的数据库名组合通过一个或多个`Pair`类作为参数传入该方法进行跨库连表查询

```kotlin name="demo" icon="kotlin" {4}
val users: List<User> =
    User().join(UserInfo(), UserTeam(), UserRole()) { user, userInfo, userTeam, userRole ->
        on { user.id == userInfo.userId && user.id == userTeam.userId && user.id == userRole.userId }
        db(userInfo to "user_info_database", userRole to "user_role_database")
        select { [user.id, user.name, userInfo.age, userTeam.teamId, userRole.roleName] }
    }.queryList()
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

## {{ $.title("select") }}指定查询字段

在Kronos中，我们可以使用`select`方法指定查询字段，多个字段使用`[]`书写。

可以使用`alias`为字段指定别名，如```select { [user.id, user.name.alias("userName"), userInfo.age] }```。

如需要查询某张表的所有字段，可以使用`select { user }`、`select { [user, userInfo, userTeam.teamId] }`。

不指定查询字段时，默认查询所有字段，我们会对不同表相同字段进行重新命名，以避免字段冲突。

可以使用字符串作为自定义查询字段，如```select { "count(`user.id`)".alias("count") }```。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

```text name="result shape"
rows.first().id
rows.first().name
rows.first().age
```

### 查询全部字段、排除部分列

可以传入`KPojo`查询全部列，使用`-`排除列，并使用`[]`组合最终查询字段列表。

```kotlin name="kotlin" icon="kotlin" {6}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user - user.id, userInfo.age] }
    }
        .queryList()
```

## {{ $.title("by") }}指定查询条件

在Kronos中，我们可以使用`by`方法指定查询字段，多个字段使用`[]`书写。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        by { [user.id, user.name, userInfo.age] }
    }.queryList()
```

## {{ $.title("where") }}指定查询条件

在Kronos中，我们可以使用`where`方法指定查询{{ $.keyword("query/conditions", ["Criteria条件语句"]) }}。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        where { user.id == 1 }
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

```sql name="where sql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
WHERE `user`.`id` = :id
```

在显式`where`块中，可以使用`.eq`将当前 KPojo 对象的字段值展开为等值条件，并继续组合其他条件表达式：

```kotlin name="kotlin" icon="kotlin" {6}
val users: List<User> =
    User(1, "Kronos").join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        where { user.eq }
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

Kronos提供了减号运算符`-`，用于在显式`.eq`展开时排除指定字段。

```kotlin name="kotlin" icon="kotlin" {6}
val users: List<User> =
    User(1, "Kronos").join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        where { (user - user.id).eq }
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

## {{ $.title("patch") }}为自定义查询条件添加参数

在Kronos中，我们可以使用`patch`方法为自定义查询条件添加参数。

```kotlin name="demo" icon="kotlin" {2-7}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
        where { "user.id = :id".asSql() }
        patch("id" to 1)
    }.queryList()
```

## {{ $.title("groupBy") }}、{{ $.title("having") }}设置分组和聚合条件

在Kronos中，我们可以使用`groupBy`方法指定分组字段。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        groupBy { [user.id, userInfo.age] }
        having { userInfo.age > 18 }
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

```sql name="group sql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
GROUP BY `user`.`id`, `user_info`.`age`
HAVING `user_info`.`age` > :age
```

## {{ $.title("orderBy") }}设置排序条件

在Kronos中，我们可以使用`orderBy`方法指定排序字段。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        orderBy { [user.id.asc(), userInfo.age.desc()] }
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

```sql name="order sql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
ORDER BY `user`.`id` ASC, `user_info`.`age` DESC
```

## {{ $.title("limit") }}指定查询数量

在Kronos中，我们可以使用`limit`方法指定查询数量。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        limit(10)
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

```sql name="limit sql" icon="mysql"
SELECT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
LIMIT 10
```

## {{ $.title("distinct") }}指定查询去重

在Kronos中，我们可以使用`distinct`方法指定查询去重。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        distinct()
        select { [user.id, user.name, userInfo.age] }
    }.queryList()
```

```sql name="distinct sql" icon="mysql"
SELECT DISTINCT `user`.`id`, `user`.`name`, `user_info`.`age`
FROM `user`
LEFT JOIN `user_info` ON `user`.`id` = `user_info`.`user_id`
```

## {{ $.title("page") }}、{{ $.title("withTotal") }}指定分页查询

`page`方法用于设置分页查询，请注意，`page`方法的参数从1开始。

在不同的数据库中，分页查询的语法有所不同，Kronos会根据不同的数据库生成相应的分页查询语句。

`withTotal`方法用于查询带有总记录数的分页查询。

> **Warning**
> 使用`page`方法后，查询的结果默认**不会**包含总记录数，若需要查询总记录数，请务必使用`withTotal`方法。

```kotlin name="demo" icon="kotlin" {2-5}
val (total, list) =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        page(1, 10)
        select { [user.id, user.name, userInfo.age] }
    }.withTotal().query()
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

## 结果方法

Join 查询使用和 select 查询相同的终端结果方法。

```kotlin name="Result methods" icon="kotlin"
val mapRows: List<Map<String, Any>> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.query()

val typedRows: List<UserInfoRow> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.queryList<UserInfoRow>()
```

结果形态、单行方法、分页总数和自定义 wrapper 见 {{ $.keyword("query/result-methods", ["结果方法"]) }}。

## 使用指定的数据源

本次 join 查询需要使用指定数据库连接时，把 `KronosDataSourceWrapper` 传给终端结果方法。

```kotlin name="demo" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { [user.id, user.name, userInfo.age] }
    }.queryList(customWrapper)
```
