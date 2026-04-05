{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Query from multiple tables

In Kronos, we can use `KPojo.join(KPojo1, KPojo2, ...) `The method is to query the associated data of multiple tables.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## Specify the join condition and join type

In Kronos, we use `left join` to connect multiple tables by default. If you don't need to specify the connection method, you can use the `on` method to specify the connection conditions.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.query()
```

You can specify both the join method and the join condition with the `leftJoin`, `rightJoin`, `innerJoin`, `crossJoin`, and `fullJoin` functions.

```kotlin name="demo" icon="kotlin" {2-6}
val users: List<User> =
    User().join(UserInfo(), UserTeam()) { user, userInfo, userTeam ->
        leftJoin { user.id == userInfo.userId }
        innerJoin { user.id == userTeam.userId }
        select { user.id + user.name + userInfo.age + userTeam.teamId }
    }.query()
```

## {{ $.title("db") }}Specify the database name(cross-database join)

In Kronos, we can use the `db` method to specify the database name.

Combine one or more tables with their respective database names and pass them as parameters to the method using one or more `Pair` classes for cross-database joint queries.

```kotlin name="demo" icon="kotlin" {4}
val users: List<User> =
    User().join(UserInfo(), UserTeam(), UserRole()) { user, userInfo, userTeam, userRole ->
        on { user.id == userInfo.userId && user.id == userTeam.userId && user.id == userRole.userId }
        db(userInfo to "user_info_database", userRole to "user_role_database")
        select { user.id + user.name + userInfo.age + userTeam.teamId + userRole.roleName }
    }.query()
```

## {{ $.title("select") }}Specify the query fields

In Kronos, we can specify query fields using the `select` method, with `+` joins between multiple fields.

You can use `as_` to specify aliases for the field, such as `select { user.id + user.name.as_("userName") + userInfo.age }`.

If you need to query all fields of a table, you can use `select { user }`, `select { user + userInfo + userTeam.teamId }`.

When you don't specify query fields, all fields are queried by default. We will rename the same fields in different tables to avoid field conflicts.

Strings can be used as custom query fields, such as `select { "count(`user.id`)".as_("count") }`.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.queryList()
```

### Query all fields or exclude some columns

You can pass `KPojo` to query all columns, and use `+`, `-` to add and subtract some columns from all columns.

```kotlin name="kotlin" icon="kotlin" {6}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { user - user.id + userInfo.age }
    }
        .query()
```

## {{ $.title("by") }}Specifying Query Fields

In Kronos, we can use `by` method to specify the query fields, with `+` joins between multiple fields.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        by { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("where") }}Specify the query conditions

In Kronos, we can use the `where` method to specify the query {{ $.keyword("concept/where-having-on-clause", ["Criteria conditional statement"]) }}.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        where { user.id == 1 }
        select { user.id + user.name + userInfo.age }
    }.query()
```

The `.eq` function can be executed on the query object so that you can add other query conditions based on generating conditional statements based on KPojo object values:

```kotlin name="demo" name="kotlin" icon="kotlin" {6}
val users: List<User> =
    User(1, "Kronos").join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        where { user.eq }
        select { user.id + user.name + userInfo.age }
    }.query()
```

Kronos supports the minus operator `-` to specify columns that do not require automatic generation of conditional statements.

```kotlin name="demo" name="kotlin" icon="kotlin" {6}
val users: List<User> =
    User(1, "Kronos").join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        where { user - user.id }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("patch") }}Add parameters to custom query conditions

In Kronos, we can use the `patch` method to add parameters to a custom query condition.

```kotlin name="demo" icon="kotlin" {2-7}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
        where { "user.id = :id" }
        patch("id" to 1)
    }.query()
```

## {{ $.title("groupBy") }}、{{ $.title("having") }}Set grouping and aggregation conditions

In Kronos, we can specify grouping fields using the `groupBy` method and specify aggregation conditions using the `having` method.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        groupBy { user.id + userInfo.age }
        having { userInfo.age > 18 }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("orderBy") }}Set sorting conditions

In Kronos, we can specify sorting conditions using the `orderBy` method.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        orderBy { user.id.asc() + userInfo.age.desc() }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("limit") }}Specify the maximum number of records to query

In Kronos, we can specify the maximum number of records to query using the `limit` method.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        limit(10)
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("distinct") }}Specify query deduplication

In Kronos, we can specify query deduplication using the `distinct` method.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        distinct()
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("page") }}、{{ $.title("withTotal") }}Query paging

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
        select { user.id + user.name + userInfo.age }
    }.withTotal().query()
```

## {{ $.title("query") }}Query map list

The `query` method is used to execute queries and return map list.

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("queryList") }}Query specified type list

The `queryList` method is used to execute queries and return specified type list, can receive generic parameters.

When querying a single column, you can directly set the generic parameter to the column type, for example: `queryList<Int>()`.

Query multiple columns, you can set the generic parameter to the KPojo subclass, for example: `queryList<User>()`.

When the generic parameter is not set, Kronos will automatically convert the query result to the type of the main table.

> **Note**
> queryList uses kronos-compiler-plugin for reflectionless generic instantiation, see: Dynamic Instantiation of KPojo, Dynamic Accessor for KPojo Properties (compile-time generation).

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.queryList()
```

## {{ $.title("queryMap") }}Query single record Map

The `queryMap` method is used to query a single record and return Map. When the query result is empty, it throws an exception.

```kotlin name="demo" icon="kotlin" {2-5}
val user: Map<String, Any> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select {
            user.id + user.name + userInfo
        }.queryMap()
```

## {{ $.title("queryMapOrNull") }}Query single record Map (Nullable)

`queryMapOrNull`方法用于查询单条记录并返回Map，当查询结果为空时，返回`null`。

```kotlin group="Case 15" name="demo" icon="kotlin" {2-5}
val user: Map<String, Any>? =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select {
            user.id + user.name + userInfo
        }.queryMapOrNull()
```

## {{ $.title("queryOne") }}Query single record specified type

The `queryOne` method is used to execute queries and return a single record, when the query result is empty, it throws an exception, can receive generic parameters.

When querying a single column, you can directly set the generic parameter to the column type, for example: `queryOne<Int>()`.

Query multiple columns, you can set the generic parameter to the KPojo subclass, for example: `queryOne<User>()`.

When the generic parameter is not set, Kronos will automatically convert the query result to the type of the main table.

> **Note**
> queryOne uses kronos-compiler-plugin for reflectionless generic instantiation, see: Dynamic Instantiation of KPojo, Dynamic Accessor for KPojo Properties (compile-time generation).

```kotlin name="demo" icon="kotlin" {2-5}
val user: User =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select {
            user.id + user.name + userInfo
        }.queryOne()
```

## {{ $.title("queryOneOrNull") }}Query single record specified type (Nullable)

The `queryOneOrNull` method is used to execute queries and return a single record, when the query result is empty, it returns `null`, can receive generic parameters.

When querying a single column, you can directly set the generic parameter to the column type, for example: `queryOneOrNull<Int>()`.

Query multiple columns, you can set the generic parameter to the KPojo subclass, for example: `queryOneOrNull<User>()`.

When the generic parameter is not set, Kronos will automatically convert the query result to the type of the main table.

> **Note**
> queryOneOrNull uses kronos-compiler-plugin for reflectionless generic instantiation, see: Dynamic Instantiation of KPojo, Dynamic Accessor for KPojo Properties (compile-time generation).

```kotlin name="demo" icon="kotlin" {2-5}
val user: User? =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.queryOneOrNull()
```

## Specify the data source to use

In Kronos, we can pass `KronosDataSourceWrapper` into the `execute` method to implement a customized database connection.

```kotlin name="demo" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val users: List<User> =
    User().join(UserInfo()) { user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.queryLIST(customWrapper)
```

