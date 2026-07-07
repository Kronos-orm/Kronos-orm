# Kronos IntelliJ IDEA 插件

<center>
<img src="/assets/images/features/img-2.png" width="300"/>
</center>

--------

Kronos 的 DSL 很依赖 Kotlin 编译期信息。`select { ... }` 会生成查询结果行的投影字段，查询结果继续 `select { ... }` 时又会成为新的输入类型，`orderBy { ... }` 还会拿到当前查询可排序的字段集合。

IntelliJ IDEA 插件会把这些编译期生成的信息交给 IDEA K2 分析，让编辑器里的补全、类型展示和红线尽量贴近真实编译结果。

## 插件支持哪些能力

插件目前重点覆盖这些编辑场景：

- 识别 `KPojo` 生成成员，例如 `toDataMap`、`fromMapData`、字段 metadata 和动态访问能力。
- 识别 `select { ... }` 编译期生成的结果行字段。
- 识别查询结果继续 `.select { ... }` 时的输入类型。
- 在 `orderBy { ... }` 中同时提示原始字段和当前查询生成的 alias。
- 在 `where`、`groupBy`、`having` 中按 Kronos 规则提示当前可用字段。
- 将缺 alias、投影名重复、标量子查询形态错误、谓词子查询列数错误、insert-select 字段数量和类型错误等 DSL 问题直接显示在编辑器中。

## 生成投影字段会被 IDEA 理解

下面的查询里同时出现了三个形状：用户自己写的 `User`、Kronos 生成的结果行 `UserNameLengthRow`、Kronos 生成的排序上下文 `UserNameLengthOrderContext`。

```kotlin
data class User(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

data class UserNameLengthRow(
    var id: Int? = null,
    var nameLength: Int? = null,
) : KPojo

data class UserNameLengthOrderContext(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
    var nameLength: Int? = null,
) : KPojo
```

```kotlin
val query = User()
    .select {
        // it: User
        // 生成结果行 UserNameLengthRow(id, nameLength)
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .where {
        // it: User
        it.status == 1
    }
    .orderBy {
        // it: UserNameLengthOrderContext
        it.nameLength.desc()
    }

val rows = query.queryList()
// rows: List<UserNameLengthRow>

rows.first().id
rows.first().nameLength
```

这类代码里，IDEA 可以围绕 `id`、`nameLength` 做补全和类型分析。`queryList()` 的返回结果会展示当前结果行形状，包含编译期生成的投影字段。

同一个查询里的 `where` 读取输入字段。刚刚 select 出来的 alias 要等查询结果继续作为输入时再过滤。IDEA 会在下面的 `nameLength` 上提示错误：

```kotlin
User()
    .select {
        // it: User
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .where {
        // it: User
        it.nameLength > 8
    }
    .queryList()
```

查询结果继续作为输入时，`it` 就是上一段查询生成的结果行：

```kotlin
val q = User()
    .select {
        // it: User
        [it.id, f.length(it.name).alias("nameLength")]
    }

q.select { [it.id, it.nameLength] }
    .where {
        // it: UserNameLengthRow
        it.nameLength > 8
    }
    .queryList()
```

## 窗口函数结果过滤

窗口函数 alias 可以在同一个查询的 `orderBy` 中使用。需要按窗口函数结果过滤时，把当前查询结果继续作为输入。

```kotlin
val ranked = Order()
    .select {
        // it: Order
        [
            it.id,
            it.userId,
            f.rowNumber()
                .over {
                    // 这里继续引用外层 it: Order 的字段组织窗口表达式
                    partitionBy(it.userId)
                    orderBy(it.createTime.desc())
                }
                .alias("rn")
        ]
    }

ranked
    .select {
        // it: RankedOrderRow
        [it.id, it.userId]
    }
    .where {
        // it: RankedOrderRow
        it.rn == 1
    }
    .queryList()
```

如果直接写成下面这样，IDEA 会把 `rn` 标红：

```kotlin
Order()
    .select {
        // it: Order
        [
            it.id,
            f.rowNumber()
                .over { orderBy(it.createTime.desc()) }
                .alias("rn")
        ]
    }
    .where { it.rn == 1 }
```

## alias 相关报错

直接字段可以继承字段名，函数、聚合、窗口函数、标量子查询和计算表达式作为 select item 时必须显式写 `.alias("name")`。

```kotlin
User()
    .select { [it.id, f.length(it.name)] }
```

IDEA 会提示：非直接字段投影需要 `.alias("name")`。

```kotlin
User()
    .select { [it.id, f.length(it.name).alias("id")] }
```

这里会提示投影字段名重复。下面这种 alias 和原始字段冲突的写法也会报错，因为排序上下文无法区分两个 `name`：

```kotlin
User()
    .select { [it.id, f.length(it.name).alias("name")] }
```

## 标量子查询报错

标量子查询只能返回一列，并且通常需要显式 `.limit(1)`。作为 select item 时还需要 alias。

```kotlin
User()
    .select { u ->
        [
            u.id,
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .alias("lastAmount")
        ]
    }
```

这些写法会在编辑器中报错：

```kotlin
// 缺少 alias
User().select { u ->
    [u.id, Order().select { it.amount }.where { it.userId == u.id }.limit(1)]
}

// 缺少 limit(1)
User().select { u ->
    [
        u.id,
        Order()
            .select { it.amount }
            .where { it.userId == u.id }
            .alias("lastAmount")
    ]
}

// 返回多列
User().select { u ->
    [
        u.id,
        Order()
            .select { [it.amount, it.status] }
            .where { it.userId == u.id }
            .limit(1)
            .alias("lastOrder")
    ]
}
```

## 谓词子查询报错

插件也会提示 `IN`、`EXISTS`、`ANY`、`SOME`、`ALL` 和 row-value tuple 的形态问题。

```kotlin
User()
    .select()
    .where { user ->
        user.id in Order().select { it.userId } &&
            exists(Order().select().where { it.userId == user.id })
    }
```

下面这些会报错：

```kotlin
// 单值 IN 右侧不能返回多列
User().select().where {
    it.id in Order().select { [it.userId, it.status] }
}

// row-value tuple 左右列数必须一致
User().select().where {
    [it.id, it.status] in Order().select { it.userId }
}

// 单元素 tuple 不合法，应该写 it.id in query
User().select().where {
    [it.id] in Order().select { it.userId }
}

// any/some/all 右侧也必须是单列查询
Order().select().where {
    it.status > any(Order().select { [it.status, it.amount] })
}
```

## insert-select 的提示

`insert<Target> { ... }` 的 lambda receiver 来自源查询生成的结果行。IDEA 插件会让你在 values 中访问源投影字段，并提示数量或类型不匹配的问题。

```kotlin
data class UserOrderSummary(
    var userId: Int? = null,
    var orderCount: Long? = null,
) : KPojo

Order()
    .select { [it.userId, f.count(it.id).alias("orderCount")] }
    .groupBy { it.userId }
    .insert<UserOrderSummary> { [it.userId, it.orderCount] }
    .execute()
```

如果少给字段，或者字段顺序导致类型对不上，IDEA 会直接在 values 列表中提示：

```kotlin
val orderCounts = Order()
    .select { [it.userId, f.count(it.id).alias("orderCount")] }
    .groupBy { it.userId }

orderCounts.insert<UserOrderSummary> {
    [it.userId]
}

orderCounts.insert<UserOrderSummary> {
    [it.orderCount, it.userId]
}
```

## 推荐阅读

更完整的规则和 SQL 语义见文档中的子查询章节与 IntelliJ IDEA 插件章节：

- [子查询](/documentation/zh-CN/query/subqueries)
- [IntelliJ IDEA 插件](/documentation/zh-CN/resources/idea-plugin)
