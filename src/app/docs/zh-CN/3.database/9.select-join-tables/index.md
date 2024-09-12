{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

本章将介绍如何查询多表关联数据。

## 查询多表关联数据

在Kronos中，我们可以使用`KPojo.join(KPojo1, KPojo2, ...)`方法来查询多表关联数据。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## 指定连接条件及连接方式

在Kronos中，我们默认使用`left join`连接多表，如果不需要指定连接方式，可以使用`on`方法指定连接条件。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.query()
```

可以通过`leftJoin`、`rightJoin`、`innerJoin`、`crossJoin`、`fullJoin`等函数同时指定连接方式和连接条件。

```kotlin name="demo" icon="kotlin" {2-6}
val users: List<User> = 
    User().join(UserInfo(), UserTeam()){ user, userInfo, userTeam ->
        leftJoin { user.id == userInfo.userId }
        innerJoin { user.id == userTeam.userId }
        select { user.id + user.name + userInfo.age + userTeam.teamId }
    }.query()
```

## 指定连接数据表的数据库（跨库连表查询）

在Kronos中，我们可以使用`db`方法指定查询字段。

将一张或多张表与其所处的数据库名组合通过一个或多个`Pair`类作为参数传入该方法进行跨库连表查询

```kotlin name="demo" icon="kotlin" {4}
val users: List<User> = 
    User().join(UserInfo(), UserTeam(), UserRole()){ user, userInfo, userTeam, userRole ->
        on { user.id == userInfo.userId && user.id == userTeam.userId && user.id == userRole.userId }
        db(userInfo to "user_info_database", userRole to "user_role_database")
        select { user.id + user.name + userInfo.age + userTeam.teamId + userRole.roleName }
    }.query()
```

## {{ $.title("select") }}指定查询字段

在Kronos中，我们可以使用`select`方法指定查询字段，多个字段之间使用`+`连接。

可以使用``as``为字段指定别名，如```select { user.id + user.name.`as`("userName") + userInfo.age }```。

如需要查询某张表的所有字段，可以使用`select { user }`、`select { user + userInfo + userTeam.teamId }`。

不指定查询字段时，默认查询所有字段，我们会对不同表相同字段进行重新命名，以避免字段冲突。

可以使用字符串作为自定义查询字段，如```select { "count(`user.id`)".as("count") }```。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.queryList()
```

## {{ $.title("by") }}指定查询条件

在Kronos中，我们可以使用`by`方法指定查询字段，多个字段之间使用`+`连接。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        by { user.id + user.name + userInfo.age }
    }.query()
```

## 使用<span style="color: #DD6666">where</span>指定查询条件

在Kronos中，我们可以使用`where`方法指定查询条件。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        where { user.id == 1 }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("patch") }}为自定义查询条件添加参数

在Kronos中，我们可以使用`patch`方法为自定义查询条件添加参数。

```kotlin name="demo" icon="kotlin" {2-7}
val users: List<User> =
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
        where { "user.id = :id" }
        patch("id" to 1)
    }.query()
```

## {{ $.title("groupBy") }}、{{ $.title("having") }}设置分组和聚合条件

在Kronos中，我们可以使用`groupBy`方法指定分组字段。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        groupBy { user.id + userInfo.age }
        having { userInfo.age > 18 }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("orderBy") }}设置排序条件

在Kronos中，我们可以使用`orderBy`方法指定排序字段。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        orderBy { user.id.asc() + userInfo.age.desc() }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("limit") }}指定查询数量

在Kronos中，我们可以使用`limit`方法指定查询数量。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        limit(10)
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("distinct") }}指定查询去重

在Kronos中，我们可以使用`distinct`方法指定查询去重。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        distinct()
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("page") }}、{{ $.title("withTotal") }}指定分页查询

`page`方法用于设置分页查询，请注意，`page`方法的参数从1开始。

在不同的数据库中，分页查询的语法有所不同，Kronos会根据不同的数据库生成相应的分页查询语句。

`withTotal`方法用于查询带有总记录数的分页查询。

> **Warning**
> 使用`page`方法后，查询的结果默认**不会**包含总记录数，若需要查询总记录数，请务必使用withTotal方法</a>。

```kotlin name="demo" icon="kotlin" {2-5}
val (total, list) = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        page(1, 10)
        select { user.id + user.name + userInfo.age }
    }.withTotal().query()
```

## {{ $.title("query") }}查询Map列表

`query`方法用于执行查询并返回Map列表。

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.query()
```

## {{ $.title("queryList") }}查询指定类型列表

`queryList`方法用于执行查询并返回指定类型列表，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryList<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryList<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为查询的KPojo类型。

> **Note**
> queryList使用kronos-compiler-plugin实现Map转换为KPojo，详见：KPojo与Map互相转换

```kotlin name="demo" icon="kotlin" {2-5}
val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.queryList()
```

## {{ $.title("queryMap") }}查询单条记录Map

`queryMap`方法用于查询单条记录并返回Map，当查询结果为空时，抛出异常。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOne<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOne<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为主表的类型。

```kotlin name="demo" icon="kotlin" {2-5}
val user: Map<String, Any> =
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo
    }.queryMap()
```

## {{ $.title("queryMapOrNull") }}查询单条记录Map（可空）

`queryMapOrNull`方法用于查询单条记录并返回Map，当查询结果为空时，返回`null`。

```kotlin group="Case 15" name="demo" icon="kotlin" {2-5}
val user: Map<String, Any>? =
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo
        }.queryMapOrNull()
```

## {{ $.title("queryOne") }}查询单条记录

`queryOne`方法用于执行查询并返回单条记录，当查询结果为空时，抛出异常，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOne<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOne<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为查询的KPojo类型。

> **Note**
> queryOne使用KCP实现Map转换为KPojo，详见：KPojo与Map互相转换

```kotlin name="demo" icon="kotlin" {2-5}
val user: User =
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo
    }.queryOne()
```

## {{ $.title("queryOneOrNull") }}查询单条记录（可空）

和`queryOne`方法类似，`queryOneOrNull`方法用于执行查询并返回单条记录，当查询结果为空时，返回`null`，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOneOrNull<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOneOrNull<User>()`。

当未设置泛型参数时，Kronos会根据查询结果自动转换为查询的KPojo类型。

> **Note**
> queryOneOrNull使用KCP实现Map转换为KPojo，详见：KPojo与Map互相转换

```kotlin name="demo" icon="kotlin" {2-5}
val user: User? =
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.queryOneOrNull()
```

## 使用指定的数据源

在Kronos中，我们可以使用指定的数据源查询数据库中的记录。

```kotlin name="demo" icon="kotlin" {1}
val customWrapper = CustomWrapper()

val users: List<User> = 
    User().join(UserInfo()){ user, userInfo ->
        on { user.id == userInfo.userId }
        select { user.id + user.name + userInfo.age }
    }.queryLIST(customWrapper)
```

