# {{ NgDocPage.title }}

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

## 使用<span style="color: #DD6666">select</span>方法指定查询字段

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
    }.query()
```

## 使用<span style="color: #DD6666">`query`</span>方法执行查询
