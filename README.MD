![logo](https://cdn.leinbo.com/assets/images/kronos/logo_dark.png)

##### Kronos ORM(Kotlin Reactive Object-Relational-Mapping)是一款前所未有的、开创性的、现代化的kotlin ORM框架。

*Kronos*是一个轻量级的框架，为开发者提供了一种高效的数据持久化解决方案。

*Kronos*使用Kotlin K2编译器插件分析Kotlin Ir表达式树，实现了简洁而又语义化的数据持久层框架，让使用者在开发过程中可以更加高效地操作数据。

*Kronos*弥补了现有Kotlin ORM框架中不足之处，并基于协程和任务机制对数据操作提供更加便捷和高效的编写体验.

*Kronos*支持多种常见关系型数据库，包括Mysql、Oracle、Postgres、Mssql、SQLite、DB2、Sybase和H2，以及国产数据库OceanBase和DM8。

-------
> 查询示例：

```kotlin
User(1).insert().execute() // 插入单行数据
listOfUser.insert().execute() // 批量插入多行数据

User(1, "name").update { it.username }.by { it.id }.execute() //条件更新
User(username = "name").update { it.username }.where { it.id between 1..100 }.execute() //条件更新
listOfUser.update { it.username }.by { it.id }.execute() //批量条件更新

val user: User = User().select().where { it.id > 1 && it.username.notNull }.queryForObjectOrNull()

// 2.多条件查询 / 查询多个字段 / 类型解析 / 带分页 / 带去重 / 带排序
val (users, total) =
    user.select { it.id + it.userName + it.authCode + "(select xxx from xxx limit 1)".alias("t") }
        .where { it.id == 1 && "complex condition".toSql() }
        .by { it.id }
        .page(1, 10)
        .orderBy { it.updateTime.desc }
        .distinct()
        .withTotal()
        .queryForList() // Pair<List<User>, Int>


// 3.连表查询 / 使用query()查询List<Map<String, Any>>多行数据
val result: List<Map<String, Any>> =
    User().join(ShoppingCart(), Good()) { user, cart, good ->
        leftJoin(cart)
            .on(user.id == cart.id && user.age > 35)
        rightJoin(good)
            .on(good.id == cart.id)
        select(user, good.id, cart.id)
        where(
            user.id == 1 &&
                    user.age >= 20 &&
                    user.email like "%@qq.com" &&
                    user.telephone notLike "159%" &&
                    (
                            user.userName in listOf("a", "b") ||
                                    user.id !in listOf(1, 2, 3)
                            ) &&
                    user.nickname.notNull &&
                    user.age between 1..2 &&
                    user.age notBetween 1..2
        )
        groupBy(user.age)
        page(1, 100)
    }
        .withTotal()
        .query()
```

------
> 插入示例：

```kotlin
// 1.插入一行数据
val affectRowNumber: Pair<Int, Int> =
    User(1).insert().execute()

// 2.根据主键更新或插入一行数据
val affectRowNumber: Pair<Int, Int> =
    User(1).upsert()
        .set { it.createTime = "YYYY-MM-DD" }
        .execute()

// 3.根据部分列更新或插入一行数据
val affectRowNumber: Pair<Int, Int> =
    User(1).upsert { it.id }
        .onDuplicate() // 默认
        .execute()

// 4.根据部分列更新或插入一行数据
val affectRowNumber: Pair<Int, Int> =
    User(1, "name", "email@example.com").upsert { it.id }
        .by { it.name + it.email }
        .execute()

// 5.根据部分列更新或插入一行数据 / 更新时排除部分列
val affectRowNumber: Pair<Int, Int> =
    User(1, "name", "email@example.com").upsertExcept { it.id }
        .by { it.name + it.email }
        .execute()
```

------
> 更新示例：

```kotlin
// 更新行
val affectRowNumber: Int =
    User(1).update()
        .set { it.id = 2 }
        .by { it.id }
        .execute()

// 更新行 / 仅更新部分字段
val affectRowNumber: Int =
    User(2).update { it.id }
        .where { it.id == 1 }
        .execute()

// 更新行 / 更新时排除部分列
val affectRowNumber: Int =
    User(2).updateExcept { it.id }
        .where { it.id == 1 }
        .execute()
```

------
> 删除示例：

```kotlin
val affectRowNumber: Int =
    User(id = 1, status = 1).remove()
        .by { it.id + it.status }
        //.where()
        .execute()

val affectRowNumber: Int =
    User().remove()
        .where { it.id > 1 && it.id < 10 }
        .execute()
```

------
[Read more on GitHub](https://github.com/ousc/kotlinorm-v2)