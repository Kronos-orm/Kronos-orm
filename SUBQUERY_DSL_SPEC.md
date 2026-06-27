# Kronos 子查询与窗口函数 DSL 设计规范

状态：草案

本文档设计 Kronos 的完整子查询与窗口函数支持。核心原则是：不引入一套割裂的新 SQL
构造器语法，而是在现有 Kronos DSL 上扩展能力。也就是说，用户仍然主要写：

```kotlin
User()
    .select { it.id + it.name }
    .where { it.status == 1 }
    .queryList()
```

子查询只是这个现有 `select()` 链的另一种使用方式，可以被嵌入 SQL 允许子查询出现的位置。

## 目标

- 保持现有 Kronos 写法：
  `KPojo().select { ... }.where { ... }.orderBy { ... }.queryList()`。
- 子查询由现有 `SelectClause`、`SelectFrom`、`UnionClause` 派生，不提供割裂的
  `query { select(); from(); where() }` 风格入口。
- 支持子查询出现在 `SELECT`、`FROM`、`JOIN`、`WHERE`、`HAVING`、`ORDER BY`、
  `UPDATE SET`、`DELETE WHERE`、`INSERT SELECT`、`UPSERT`、DDL 视图/表创建等场景。
- 支持 `EXISTS`、`NOT EXISTS`、`IN`、`NOT IN`、标量子查询、量词比较
  `ANY` / `SOME` / `ALL`、元组 `IN`、派生表、CTE、窗口函数。
- 在 Kotlin 能表达的地方尽量保持类型安全；动态 SQL 场景保留字符串别名和原生 SQL 兜底。
- 尽量复用当前编译器插件的 `SelectTransformer`、`ConditionTransformer`、
  `SetTransformer`、`SortTransformer`，不要重写一套查询编译管线。

## 非目标

- 不替换现有 select/update/delete/insert API。
- 不要求用户为了写子查询学习另一套 DSL。
- 不把 raw SQL 作为主要能力；raw SQL 只作为兜底。
- 不要求所有高级 SQL 都必须在编译器插件中一次性完成；能用运行时 AST 安全表达的场景，
  可以先通过运行时 API 落地。

## 当前 DSL 基线

当前查询风格：

```kotlin
val listOfUser: List<User> = User()
    .select { it.id + it.username }
    .where { it.id < 10 && it.age >= 18 }
    .distinct()
    .groupBy { it.id }
    .orderBy { it.username.desc() }
    .queryList()
```

当前联表风格：

```kotlin
val listOfMap = User().join(UserRelation(), UserRole()) { user, relation, role ->
    on { user.id == relation.userId && user.id == role.userId }
    select {
        user.id + user.username + relation.id.as_("relationId") +
            role.role + f.count(1).as_("count")
    }
    where { user.id < 10 }
}.query()
```

子查询设计必须像这些写法的自然延伸。

## 总体设计原则

子查询不是新的根级查询语言，而是现有 `SelectClause` 或 `SelectFrom` 转换出来的嵌入值：

```kotlin
SelectClause<T>.as_(block: DerivedSelectScope<T>.(T) -> Unit): SelectClause<T>
SelectClause<T>.as_(alias: String, block: DerivedSelectScope<T>.(T) -> Unit): SelectClause<T>
SelectClause<T>.asTable(alias: String? = null): DerivedTable<T>
SelectClause<T>.scalar<R>(): ScalarSubquery<R>
SelectClause<T>.exists(): ExistsSubquery

SelectFrom<T>.as_(block: DerivedSelectScope<T>.(T) -> Unit): SelectClause<T>
SelectFrom<T>.as_(alias: String, block: DerivedSelectScope<T>.(T) -> Unit): SelectClause<T>
SelectFrom<T>.asTable(alias: String? = null): DerivedTable<T>
SelectFrom<T>.scalar<R>(): ScalarSubquery<R>
SelectFrom<T>.exists(): ExistsSubquery
```

`UnionClause` 同理：

```kotlin
UnionClause.as_(block: DerivedSelectScope<Nothing>.(Nothing) -> Unit): SelectClause<Nothing>
UnionClause.as_(alias: String, block: DerivedSelectScope<Nothing>.(Nothing) -> Unit): SelectClause<Nothing>
UnionClause.asTable(alias: String? = null): DerivedTable<Nothing>
UnionClause.scalar<R>(): ScalarSubquery<R>
UnionClause.exists(): ExistsSubquery
```

派生表本身仍可显式创建并复用：

```kotlin
SelectClause<T>.asTable(alias: String? = null): DerivedTable<T>
DerivedTable<T>.select(fields: ToDerivedSelect<T, Any?> = null): SelectClause<T>
DerivedTable<T>.join(...)
DerivedTable<T>.leftJoin(...)
DerivedTable<T>.rightJoin(...)
```

最重要的用户写法是：

```kotlin
T()
    .select { ... }
    .where { ... }
    .as_ { x ->
        select { ... }
        where { ... }
    }
    .queryList()
```

也就是说，`as_ { x -> ... }` 会把前一个查询包装成 SQL 派生表，并进入一个新的外层
select scope。`x` 是派生表行的 lambda 参数，SQL 表别名由 Kronos 自动生成。等价于：

```sql
SELECT ...
FROM (
    SELECT ...
) _k_subq_0
WHERE ...
```

如果用户需要复用派生表、让生成 SQL 更易读、或与 raw SQL 片段互操作，可以显式命名：

```kotlin
T()
    .select { ... }
    .where { ... }
    .as_("tmp") { x ->
        select { ... }
        where { ... }
    }
```

或者显式创建可复用表源：

```kotlin
val tmp = T()
    .select { ... }
    .where { ... }
    .asTable("tmp")

tmp.select { it.id + it.name }
```

## 核心示例：按组取第一条

目标 SQL：

```sql
SELECT id, group_col, sort_col, val
FROM (
    SELECT
        *,
        ROW_NUMBER() OVER (PARTITION BY group_col ORDER BY sort_col ASC) AS rn
    FROM t
    WHERE create_time >= '2026-01-01'
) tmp
WHERE rn = 1
```

推荐 Kronos DSL：

```kotlin
val rn = alias<Int>("rn")

val rows = T()
    .select {
        it +
            f.rowNumber()
                .over {
                    partitionBy(it.groupCol)
                    orderBy(it.sortCol.asc())
                }
                .as_(rn)
    }
    .where { it.createTime >= LocalDate.parse("2026-01-01") }
    .as_ { x ->
        select {
            x.id + x.groupCol + x.sortCol + x.val
        }
        where {
            x[rn] == 1
        }
    }
    .queryList()
```

字符串别名兜底写法：

```kotlin
val rows = T()
    .select {
        it +
            f.rowNumber()
                .over {
                    partitionBy(it.groupCol)
                    orderBy(it.sortCol.asc())
                }
                .as_("rn")
    }
    .where { it.createTime >= LocalDate.parse("2026-01-01") }
    .as_ { x ->
        select { x.id + x.groupCol + x.sortCol + x.val }
        where { x["rn"] == 1 }
    }
    .queryList()
```

## 别名模型

当前 Kronos 已支持 `as_("alias")`。为了让子查询、窗口函数、标量子查询中的表达式别名更好复用，
新增类型化别名 token：

```kotlin
val rn = alias<Int>("rn")
val lastOrderAt = alias<LocalDateTime>("lastOrderAt")
```

API：

```kotlin
fun <T> alias(name: String): QueryAlias<T>

infix fun Any?.as_(alias: QueryAlias<*>): QueryAlias<*>
infix fun Any?.as_(alias: String): String

operator fun <R> DerivedRow.get(alias: QueryAlias<R>): R?
operator fun DerivedRow.get(alias: String): Any?
```

规则：

- `as_("name")` 继续保留，兼容现有写法。
- `alias<T>("name")` 用于需要复用的投影表达式，推荐在子查询和窗口函数中使用。
- `it[alias]` 类型可感知。
- `it["name"]` 是动态兜底。
- 派生表可以暴露内层已选出的 KPojo 字段。
- 派生表可以暴露内层 `select` 中声明的表达式别名。

## 派生表 API

### 基础 FROM 子查询

```kotlin
val rows = User()
    .select { it.id + it.name + it.status }
    .where { it.status == 1 }
    .as_("active_users") { activeUsers ->
        select { activeUsers.id + activeUsers.name }
    }
    .queryList()
```

期望 SQL：

```sql
SELECT active_users.id, active_users.name
FROM (
    SELECT id, name, status
    FROM user
    WHERE status = :status
) active_users
```

不关心 SQL 中派生表别名时，可以省略字符串别名：

```kotlin
val rows = User()
    .select { it.id + it.name + it.status }
    .where { it.status == 1 }
    .as_ { activeUsers ->
        select { activeUsers.id + activeUsers.name }
    }
    .queryList()
```

需要复用派生表，或希望先把表源传给 `join` / `using` / raw SQL 互操作时，再显式创建
`DerivedTable`：

```kotlin
val activeUsers = User()
    .select { it.id + it.name + it.status }
    .where { it.status == 1 }
    .asTable("active_users")

val rows = activeUsers
    .select { it.id + it.name }
    .queryList()
```

### 派生表外层过滤

```kotlin
val countAlias = alias<Long>("orderCount")

val rows = Order()
    .select {
        it.userId + f.count(1).as_(countAlias)
    }
    .groupBy { it.userId }
    .as_("oc") { oc ->
        select {
            oc.userId + oc[countAlias]
        }
        where {
            oc[countAlias] > 10
        }
    }
    .query()
```

### 派生表字段可见性

如果内层选择了全部列：

```kotlin
User()
    .select { it }
    .as_ { u ->
        select { u.id + u.name }
    }
```

外层的 `it.id` 和 `it.name` 合法。

如果内层只选择了部分列：

```kotlin
User()
    .select { it.id }
    .as_ { u ->
        select { u.name }
    }
```

应尽早失败：

- 编译器插件能证明字段未投影时，给出编译期错误或警告。
- 动态场景无法证明时，在 `DerivedSelectScope.select` 或 `DerivedTable.select` 运行期校验。

表达式别名必须显式访问：

```kotlin
val total = alias<Long>("total")

User()
    .select { f.count(1).as_(total) }
    .as_ { u ->
        select { u[total] }
    }
```

## SELECT 列表中的子查询

标量子查询可以直接作为 select item：

```kotlin
val lastOrderAt = alias<LocalDateTime>("lastOrderAt")

val rows = User()
    .select { u ->
        u.id + u.name +
            Order()
                .select { o -> f.max(o.createTime) }
                .where { o -> o.userId == u.id }
                .scalar<LocalDateTime>()
                .as_(lastOrderAt)
    }
    .query()
```

期望 SQL：

```sql
SELECT
    u.id,
    u.name,
    (
        SELECT MAX(o.create_time)
        FROM orders o
        WHERE o.user_id = u.id
    ) AS lastOrderAt
FROM user u
```

注意：

- 内层 `where` 必须能引用外层 `u`。
- `scalar<R>()` 声明该子查询返回单列。
- `scalar<R>()` 不保证只返回一行；多行时由数据库决定是报错还是按方言语义处理。

## WHERE 子查询

### IN

```kotlin
val rows = User()
    .select { it.id + it.name }
    .where { u ->
        u.id in Order()
            .select { o -> o.userId }
            .where { o -> o.status == 1 }
    }
    .queryList()
```

期望 SQL：

```sql
SELECT id, name
FROM user
WHERE id IN (
    SELECT user_id
    FROM orders
    WHERE status = :status
)
```

### NOT IN

```kotlin
val rows = User()
    .select { it.id + it.name }
    .where { u ->
        u.id notIn Order()
            .select { o -> o.userId }
            .where { o -> o.cancelled == true }
    }
    .queryList()
```

### EXISTS

```kotlin
val rows = User()
    .select { it.id + it.name }
    .where { u ->
        exists(
            Order()
                .select { "1" }
                .where { o -> o.userId == u.id && o.status == 1 }
        )
    }
    .queryList()
```

### NOT EXISTS

```kotlin
val rows = User()
    .select { it.id + it.name }
    .where { u ->
        notExists(
            Order()
                .select { "1" }
                .where { o -> o.userId == u.id && o.status == 1 }
        )
    }
    .queryList()
```

### 标量子查询比较

```kotlin
val rows = User()
    .select { it.id + it.score }
    .where { u ->
        u.score > Score()
            .select { s -> f.avg(s.score) }
            .where { s -> s.groupId == u.groupId }
            .scalar<BigDecimal>()
    }
    .queryList()
```

### ANY / SOME / ALL

```kotlin
val rows = Product()
    .select { it.id + it.price }
    .where { p ->
        p.price > any(
            ProductPrice()
                .select { it.price }
                .where { it.categoryId == p.categoryId }
        )
    }
    .queryList()
```

```kotlin
val rows = Product()
    .select { it.id + it.price }
    .where { p ->
        p.price <= all(
            ProductPrice()
                .select { it.price }
                .where { it.categoryId == p.categoryId }
        )
    }
    .queryList()
```

`some(...)` 是 `any(...)` 的别名。

### 元组 IN

```kotlin
val rows = Order()
    .select { it.id + it.userId + it.createTime }
    .where { o ->
        tuple(o.userId, o.createTime) in Order()
            .select { i -> i.userId + f.max(i.createTime) }
            .groupBy { i -> i.userId }
    }
    .queryList()
```

期望 SQL：

```sql
WHERE (user_id, create_time) IN (
    SELECT user_id, MAX(create_time)
    FROM orders
    GROUP BY user_id
)
```

元组 IN 需要按方言开关控制。若方言不支持 row-value expression，可以抛出不支持异常，
或在能安全改写时转换为等价 EXISTS。

## HAVING 子查询

```kotlin
val rows = User()
    .select { it.groupId + f.count(1).as_("cnt") }
    .groupBy { it.groupId }
    .having { u ->
        f.count(1) > GroupLimit()
            .select { it.minCount }
            .where { it.groupId == u.groupId }
            .scalar<Int>()
    }
    .query()
```

`having` 与 `where` 共用子查询表达式能力。

## ORDER BY 子查询

SQL 允许在 `ORDER BY` 中使用表达式。当前 Kronos 排序主要基于字段，本设计扩展为可排序表达式：

```kotlin
val rows = User()
    .select { it.id + it.name }
    .orderBy { u ->
        Order()
            .select { o -> f.max(o.createTime) }
            .where { o -> o.userId == u.id }
            .scalar<LocalDateTime>()
            .desc()
    }
    .queryList()
```

实现上可以先让新表达式排序路径支持该能力，同时保留现有 `Field + SortType` 的排序路径。

## JOIN 子查询

### JOIN 派生表

```kotlin
val lastOrderAt = alias<LocalDateTime>("lastOrderAt")

val lastOrder = Order()
    .select {
        it.userId + f.max(it.createTime).as_(lastOrderAt)
    }
    .groupBy { it.userId }
    .asTable("last_order")

val rows = User().leftJoin(lastOrder) { user, lo ->
    on { user.id == lo.userId }
    select { user.id + user.name + lo[lastOrderAt] }
}.query()
```

期望 SQL：

```sql
SELECT user.id, user.name, last_order.lastOrderAt
FROM user
LEFT JOIN (
    SELECT user_id, MAX(create_time) AS lastOrderAt
    FROM orders
    GROUP BY user_id
) last_order ON user.id = last_order.user_id
```

### JOIN 嵌套派生表

```kotlin
val rn = alias<Int>("rn")

val latestOrder = Order()
    .select {
        it +
            f.rowNumber()
                .over {
                    partitionBy(it.userId)
                    orderBy(it.createTime.desc())
                }
                .as_(rn)
    }
    .as_("ranked_order") { ranked ->
        select { ranked.id + ranked.userId + ranked.createTime }
        where { ranked[rn] == 1 }
    }
    .asTable("latest_order")

val rows = User().join(latestOrder) { user, order ->
    on { user.id == order.userId }
    select { user.id + user.name + order.createTime.as_("lastOrderAt") }
}.query()
```

## 窗口函数 DSL

窗口函数是表达式，应可用于 `select`、派生表过滤、支持该语义的 `having` / `orderBy` 等位置。

### ROW_NUMBER

```kotlin
val rn = alias<Int>("rn")

User()
    .select {
        it.id +
            f.rowNumber()
                .over {
                    partitionBy(it.groupId)
                    orderBy(it.createTime.desc())
                }
                .as_(rn)
    }
```

### 聚合函数窗口化

```kotlin
val userTotal = alias<BigDecimal>("userTotal")

Order()
    .select {
        it.id + it.amount +
            f.sum(it.amount)
                .over {
                    partitionBy(it.userId)
                }
                .as_(userTotal)
    }
```

### 窗口 frame

```kotlin
val rollingAmount = alias<BigDecimal>("rollingAmount")

Order()
    .select {
        it.id + it.amount +
            f.sum(it.amount)
                .over {
                    partitionBy(it.userId)
                    orderBy(it.createTime.asc())
                    rows {
                        between(2.preceding, currentRow)
                    }
                }
                .as_(rollingAmount)
    }
```

### 窗口 DSL 类型

```kotlin
class WindowBuilder<T : KPojo> {
    fun partitionBy(vararg fields: Any?)
    fun orderBy(vararg fields: Pair<Any?, SortType>)
    fun rows(block: WindowFrameBuilder.() -> Unit)
    fun range(block: WindowFrameBuilder.() -> Unit)
    fun groups(block: WindowFrameBuilder.() -> Unit)
}

class WindowFrameBuilder {
    val unboundedPreceding: FrameBoundary
    val currentRow: FrameBoundary
    val unboundedFollowing: FrameBoundary

    val Int.preceding: FrameBoundary
    val Int.following: FrameBoundary

    fun between(start: FrameBoundary, end: FrameBoundary)
}
```

## UPDATE 子查询

### WHERE EXISTS

```kotlin
User()
    .update()
    .set { it.status = 2 }
    .where { u ->
        exists(
            Order()
                .select { "1" }
                .where { o -> o.userId == u.id && o.status == 1 }
        )
    }
    .execute()
```

### SET 标量子查询

```kotlin
User()
    .update()
    .set { u ->
        u.lastOrderAt = Order()
            .select { o -> f.max(o.createTime) }
            .where { o -> o.userId == u.id }
            .scalar<LocalDateTime>()
    }
    .where { it.status == 1 }
    .execute()
```

### SET 中使用统计子查询

```kotlin
UserStats()
    .update()
    .set { s ->
        s.orderCount = Order()
            .select { o -> f.count(1) }
            .where { o -> o.userId == s.userId }
            .scalar<Long>()
    }
    .where { it.userId in activeUserIds }
    .execute()
```

### UPDATE FROM / JOIN UPDATE

`UPDATE ... FROM`、`UPDATE ... JOIN` 的方言差异较大。第一阶段不建议抽象 joined update，
优先用相关标量子查询和 `EXISTS` 覆盖可移植场景。

后续可考虑：

```kotlin
User()
    .update()
    .from(Order().select { ... }.asTable("o")) { o ->
        set { u -> u.lastOrderAt = o.createTime }
        where { u.id == o.userId }
    }
```

该能力应作为后续阶段处理。

## DELETE 子查询

### WHERE EXISTS

```kotlin
User()
    .delete()
    .where { u ->
        exists(
            UserBan()
                .select { "1" }
                .where { b -> b.userId == u.id }
        )
    }
    .execute()
```

### WHERE IN

```kotlin
Order()
    .delete()
    .where { o ->
        o.userId in User()
            .select { it.id }
            .where { it.status == 0 }
    }
    .execute()
```

### DELETE USING / 多表 DELETE

多表删除同样存在明显方言差异。第一阶段只要求 `where` 中支持子查询，覆盖可移植场景。

后续 API 可考虑：

```kotlin
Order()
    .delete()
    .using(User().select { it.id }.where { it.status == 0 }.asTable("u")) { u ->
        where { o -> o.userId == u.id }
    }
```

## INSERT 子查询

### INSERT VALUES 中的标量子查询

```kotlin
UserStats(userId = userId)
    .insert()
    .value {
        it.orderCount = Order()
            .select { o -> f.count(1) }
            .where { o -> o.userId == userId }
            .scalar<Long>()
    }
    .execute()
```

这需要新增 insert value override API。如果第一阶段优先做 `INSERT SELECT`，该能力可以后置。

### INSERT SELECT

```kotlin
UserArchive()
    .insert {
        it.id + it.name + it.lastOrderAt
    }
    .select(
        User()
            .select { u ->
                u.id + u.name +
                    Order()
                        .select { o -> f.max(o.createTime) }
                        .where { o -> o.userId == u.id }
                        .scalar<LocalDateTime>()
                        .as_("lastOrderAt")
            }
            .where { it.status == 0 }
    )
    .execute()
```

期望 SQL：

```sql
INSERT INTO user_archive (id, name, last_order_at)
SELECT id, name, (
    SELECT MAX(create_time)
    FROM orders
    WHERE orders.user_id = user.id
) AS lastOrderAt
FROM user
WHERE status = :status
```

### INSERT SELECT 来源为派生表

```kotlin
UserArchive()
    .insert { it.id + it.name }
    .select(
        User()
            .select { it.id + it.name }
            .where { it.status == 0 }
            .as_("inactive_users") { inactiveUsers ->
                select { inactiveUsers.id + inactiveUsers.name }
            }
    )
    .execute()
```

## UPSERT 子查询

原生 upsert 方言差异较大，但表达式能力应共用。

### 冲突更新中使用标量子查询

```kotlin
UserStats(userId = userId)
    .upsert { it.userId + it.orderCount }
    .on { it.userId }
    .onConflict()
    .set {
        it.orderCount = Order()
            .select { o -> f.count(1) }
            .where { o -> o.userId == userId }
            .scalar<Long>()
    }
    .execute()
```

如果 `UpsertClause` 当前没有暴露 `set`，应在 update SET 子查询能力稳定后再补。

### UPSERT INSERT SELECT

后续 API：

```kotlin
UserStats()
    .upsert { it.userId + it.orderCount }
    .select(
        Order()
            .select { it.userId + f.count(1).as_("orderCount") }
            .groupBy { it.userId }
    )
    .on { it.userId }
    .onConflict()
    .execute()
```

## DDL 与子查询

DDL 支持需要保守，因为不同数据库语法差异较大。

### CREATE VIEW AS SELECT

```kotlin
Kronos.dataSource().view
    .create("active_user_order_view")
    .as_(
        User().join(Order()) { u, o ->
            on { u.id == o.userId }
            select { u.id + u.name + o.id.as_("orderId") }
            where { u.status == 1 }
        }
    )
```

期望 SQL：

```sql
CREATE VIEW active_user_order_view AS
SELECT ...
```

### CREATE TABLE AS SELECT

```kotlin
Kronos.dataSource().table
    .create("active_user_snapshot")
    .as_(
        User()
            .select { it.id + it.name + it.status }
            .where { it.status == 1 }
    )
```

期望 SQL：

```sql
CREATE TABLE active_user_snapshot AS
SELECT id, name, status
FROM user
WHERE status = :status
```

方言注意：

- MySQL、PostgreSQL、SQLite、Oracle、SQL Server 都有类似 CTAS 的能力，但语法和限制不同。
- CTAS 通常不会保留索引、主键、注释、KPojo 注解等元数据，需要明确写入文档。
- `syncTable` 不应使用子查询，仍然保持基于 schema diff 的表结构同步。

### CREATE MATERIALIZED VIEW

后续按方言开关支持：

```kotlin
Kronos.dataSource().view
    .createMaterialized("daily_user_stats")
    .as_(
        UserStatsDaily().select { it.date + it.userCount }
    )
```

### CHECK 约束与生成列

不建议支持 CHECK 约束中的子查询。多数数据库禁止或语义复杂。生成列应走表达式 DSL，
不要走子查询 DSL，除非某个方言明确支持。

## CTE 支持

CTE 是具名子查询，也应该来自现有 `select()`。

### 单个 CTE

```kotlin
val activeUsers = User()
    .select { it.id + it.name }
    .where { it.status == 1 }
    .cte("active_users")

val rows = activeUsers
    .select { it.id + it.name }
    .queryList()
```

期望 SQL：

```sql
WITH active_users AS (
    SELECT id, name
    FROM user
    WHERE status = :status
)
SELECT id, name
FROM active_users
```

### 多个 CTE

```kotlin
val activeUsers = User()
    .select { it.id + it.name }
    .where { it.status == 1 }
    .cte("active_users")

val orderCounts = Order()
    .select { it.userId + f.count(1).as_("cnt") }
    .groupBy { it.userId }
    .cte("order_counts")

val rows = activeUsers.join(orderCounts) { u, oc ->
    on { u.id == oc.userId }
    select { u.id + u.name + oc["cnt"] }
}.query()
```

### 递归 CTE

递归 CTE 需要 union 和列名列表：

```kotlin
val tree = Category()
    .select { it.id + it.parentId + it.name }
    .where { it.parentId.isNull }
    .unionAll(
        Category().join(cteRef<Category>("tree")) { c, t ->
            on { c.parentId == t.id }
            select { c.id + c.parentId + c.name }
        }
    )
    .cte("tree", recursive = true, columns = listOf("id", "parent_id", "name"))

tree.select { it.id + it.parentId + it.name }.queryList()
```

递归 CTE 可作为后续阶段。第一版完整子查询支持只要求非递归 CTE。

## Raw SQL 兼容

Raw SQL 继续保留：

```kotlin
User()
    .select { it.id + "ROW_NUMBER() OVER (PARTITION BY group_id ORDER BY id) AS rn" }
    .where { "rn = :rn".asSql() }
    .patch("rn" to 1)
```

但常见子查询和窗口函数应由类型化 DSL 覆盖，降低用户写 raw SQL 的频率。

## Core AST 设计

当前 AST 已有一些基础节点：

- `SelectStatement`
- `SubqueryExpression`
- `SpecialExpression.InSubqueryExpression`
- `SubqueryTable`
- `TableReferenceImpl.SubqueryTableReference`
- `FunctionCall(over = WindowClause?)`
- `WindowClause`
- `WindowFrame`

本设计建议统一并补齐这些能力。

### 新增或调整的 AST 节点

```kotlin
sealed interface QueryExpression : Expression

data class ScalarSubqueryExpression(
    val query: SelectStatement
) : QueryExpression

data class ExistsSubqueryExpression(
    val query: SelectStatement,
    val not: Boolean = false
) : QueryExpression

data class QuantifiedSubqueryExpression(
    val left: Expression,
    val operator: SqlOperator,
    val quantifier: Quantifier,
    val query: SelectStatement
) : QueryExpression

enum class Quantifier {
    ANY,
    SOME,
    ALL
}
```

如果继续沿用现有 `SubqueryExpression` sealed class，也可以在其中补齐缺失行为，不必平行新增。

元组表达式：

```kotlin
data class TupleExpression(
    val expressions: List<Expression>
) : Expression
```

派生表：

```kotlin
data class DerivedTableReference(
    val query: SelectStatement,
    val alias: String,
    val projectedFields: List<ProjectionField>
) : TableReference
```

CTE：

```kotlin
data class WithClause(
    val items: List<CteItem>,
    val recursive: Boolean = false
)

data class CteItem(
    val name: String,
    val columns: List<String>? = null,
    val query: SelectStatement
)
```

`SelectStatement`：

```kotlin
class SelectStatement(
    var selectList: MutableList<SelectItem> = mutableListOf(),
    var from: TableReference,
    var where: Expression? = null,
    var groupBy: MutableList<Expression>? = null,
    var having: Expression? = null,
    var orderBy: MutableList<OrderByItem>? = null,
    var limit: LimitClause? = null,
    var distinct: Boolean = false,
    var lock: PessimisticLock? = null,
    var with: WithClause? = null
) : Statement
```

`InsertStatement` 的来源需要从单一 values 扩展为 values/select：

```kotlin
sealed interface InsertSource {
    data class Values(val values: List<Expression>) : InsertSource
    data class Select(val query: SelectStatement) : InsertSource
}

data class InsertStatement(
    val table: TableReference,
    val columns: List<ColumnReference>,
    val source: InsertSource,
    val conflictResolver: ConflictResolver? = null
) : Statement
```

为了兼容旧代码，可以保留旧 `values` 构造器或提供 adapter。

### 投影元数据

派生表需要记录投影信息：

```kotlin
data class ProjectionField(
    val sourceField: Field? = null,
    val alias: QueryAlias<*>? = null,
    val aliasName: String? = null,
    val expression: Expression,
    val kotlinType: String? = null
)
```

用途：

- 校验外层 `it.field` 是否来自内层投影。
- 解析 `it[alias]`。
- 渲染 select item 别名和带表别名的列引用。
- 支持 `queryList<T>()`、`query()` 的结果映射。

## DSL 运行时类型

### QueryAlias

```kotlin
data class QueryAlias<T>(
    val name: String,
    val kotlinType: KClass<*>? = null
)
```

### DerivedTable

```kotlin
class DerivedTable<T : KPojo>(
    val alias: String,
    val source: KSelectable<T>,
    val statement: SelectStatement,
    val projections: List<ProjectionField>
)
```

`DerivedTable<T>` 不应实现 `KPojo`。它不是持久化实体，而是表源。select/join DSL
应接受它作为 table-like receiver。

### DerivedRow

外层 lambda 的接收对象可以抽象为：

```kotlin
class DerivedRow<T : KPojo>(
    val table: DerivedTable<T>,
    val pojoProxy: T
) {
    operator fun <R> get(alias: QueryAlias<R>): R? = null
    operator fun get(alias: String): Any? = null
}
```

但为了保留 `it.id` 这种干净写法，编译器插件可以继续把原始 `T` 传入 lambda，
只是在字段解析阶段把字段渲染为派生表别名下的列。别名访问通过扩展 operator 提供：

```kotlin
operator fun <T : KPojo, R> T.get(alias: QueryAlias<R>): R? = null
operator fun <T : KPojo> T.get(alias: String): Any? = null
```

这些 operator 只在派生表 DSL 作用域内有意义。

## Clause API 调整

### SelectClause

```kotlin
fun <T : KPojo> SelectClause<T>.as_(
    block: DerivedSelectScope<T>.(T) -> Unit
): SelectClause<T>

fun <T : KPojo> SelectClause<T>.as_(
    alias: String,
    block: DerivedSelectScope<T>.(T) -> Unit
): SelectClause<T>

fun <T : KPojo> SelectClause<T>.asTable(alias: String? = null): DerivedTable<T>
fun <T : KPojo, R> SelectClause<T>.scalar(): ScalarSubquery<R>
fun <T : KPojo> SelectClause<T>.exists(): ExistsSubquery

fun <T : KPojo> DerivedTable<T>.select(fields: ToSelect<T, Any?> = null): SelectClause<T>
fun <T : KPojo> DerivedTable<T>.where(condition: ToFilter<T, Boolean?>): SelectClause<T>
```

`as_ { ... }` 是首选的嵌入式 FROM 子查询入口。它内部生成 `DerivedTable`，并把块内的
`select`、`where`、`groupBy`、`having`、`orderBy` 等调用收集到外层 `SelectClause`。
`asTable(...)` 只在需要复用表源、传入 join/using、或显式控制 SQL alias 时使用。
`DerivedTable.select` 返回的 clause 必须把 `from` 设置为子查询表引用。

### DerivedSelectScope

```kotlin
class DerivedSelectScope<T : KPojo> {
    fun select(fields: ToSelect<T, Any?> = null): Unit
    fun where(condition: ToFilter<T, Boolean?>): Unit
    fun groupBy(fields: ToSelect<T, Any?>): Unit
    fun having(condition: ToFilter<T, Boolean?>): Unit
    fun orderBy(fields: ToSort<T, Any?>): Unit
    fun page(pageIndex: Int, pageSize: Int): Unit
    fun limit(limit: Int): Unit
}
```

`DerivedSelectScope` 的职责是把外层查询 clauses 写在同一个 lambda 里，避免
`selectFrom(tmp)` 或 `tmp.select()` 成为用户写 FROM 子查询时的默认心智模型。

### SelectFrom 与 UnionClause

联表查询和 union 查询也使用同一套派生表入口，避免因为来源不同而出现第二套语法：

```kotlin
fun <T : KPojo> SelectFrom<T>.as_(
    block: DerivedSelectScope<T>.(T) -> Unit
): SelectClause<T>

fun <T : KPojo> SelectFrom<T>.as_(
    alias: String,
    block: DerivedSelectScope<T>.(T) -> Unit
): SelectClause<T>

fun <T : KPojo> SelectFrom<T>.asTable(alias: String? = null): DerivedTable<T>
fun <T : KPojo, R> SelectFrom<T>.scalar(): ScalarSubquery<R>
fun <T : KPojo> SelectFrom<T>.exists(): ExistsSubquery

fun UnionClause.as_(
    block: DerivedSelectScope<Nothing>.(Nothing) -> Unit
): SelectClause<Nothing>

fun UnionClause.as_(
    alias: String,
    block: DerivedSelectScope<Nothing>.(Nothing) -> Unit
): SelectClause<Nothing>

fun UnionClause.asTable(alias: String? = null): DerivedTable<Nothing>
fun <R> UnionClause.scalar(): ScalarSubquery<R>
fun UnionClause.exists(): ExistsSubquery
```

`SelectFrom` 派生出的 row scope 应暴露 join select list 中真实投影的字段与别名。若 join select
中存在同名列，外层必须通过显式 alias 或字符串列名访问，不能静默选择其中一个。

### Join

新增 overload：

```kotlin
fun <T1 : KPojo, T2 : KPojo> T1.join(
    derived: DerivedTable<T2>,
    block: SelectFrom2<T1, T2>.() -> Unit
): SelectFrom2<T1, T2>

fun <T1 : KPojo, T2 : KPojo> DerivedTable<T1>.join(
    derived: DerivedTable<T2>,
    block: SelectFrom2<T1, T2>.() -> Unit
): SelectFrom2<T1, T2>
```

`SelectFrom` 内部应允许每个参与方是基础 `KPojo` 表，也可以是 `DerivedTable`。

### 条件 DSL

新增辅助函数：

```kotlin
fun exists(select: KSelectable<*>): Boolean
fun notExists(select: KSelectable<*>): Boolean

infix fun <T> T?.notIn(select: KSelectable<*>): Boolean
fun <T> any(select: KSelectable<*>): QuantifiedSubquery<T>
fun <T> some(select: KSelectable<*>): QuantifiedSubquery<T>
fun <T> all(select: KSelectable<*>): QuantifiedSubquery<T>

fun tuple(vararg values: Any?): TupleValue
```

现有 Kotlin `in` 操作符应支持：

```kotlin
u.id in UserRole().select { it.userId }
tuple(u.orgId, u.userId) in UserRole().select { it.orgId + it.userId }
```

### Set

允许右值为表达式：

```kotlin
u.lastOrderAt = Order().select { ... }.scalar<LocalDateTime>()
```

当前 `KTableForSet.fieldParamMap` 存的是普通 value，需要泛化：

```kotlin
sealed interface SetValue {
    data class ParameterValue(val value: Any?) : SetValue
    data class ExpressionValue(val expression: Expression) : SetValue
}
```

### Insert

新增 `INSERT SELECT`：

```kotlin
fun <T : KPojo> T.insert(fields: ToSelect<T, Any?> = null): InsertClause<T>
fun <T : KPojo> InsertClause<T>.select(source: KSelectable<*>): InsertClause<T>
```

调用 `.select(source)` 后，`InsertStatement.source` 使用 `InsertSource.Select`。

## 编译器插件设计

不要新增一套顶层 query transformer。应扩展现有 transformer，并抽出共享表达式分析层。

### 新增 ExpressionAnalysis

文件：

```text
kronos-compiler-plugin/src/main/kotlin/com/kotlinorm/compiler/core/ExpressionAnalysis.kt
```

职责：

- 将属性访问转换为 `ColumnReference` 或 `Field`。
- 将函数调用转换为 `FunctionField` 或 `FunctionCall`。
- 将 `scalar()`、`exists()`、`notExists()`、`any()`、`all()` 转换为子查询 AST 表达式。
- 解析派生表别名访问 `it[alias]` 和 `it["alias"]`。
- 解析 tuple 表达式。
- 解析窗口函数 `.over { ... }`。
- 保留外层作用域字段引用，用于相关子查询。

### 查询作用域栈

嵌套子查询需要引用外层字段：

```kotlin
User().select { u ->
    Order()
        .select { f.count(1) }
        .where { o -> o.userId == u.id }
        .scalar<Long>()
}
```

编译器插件需要维护 query scope stack：

```kotlin
data class QueryScope(
    val tableAlias: String?,
    val pojoType: IrType,
    val tableKind: TableKind,
    val projections: List<ProjectionField>
)

enum class TableKind {
    BASE_TABLE,
    DERIVED_TABLE,
    CTE
}
```

解析规则：

1. 优先匹配当前 lambda 参数。
2. 再匹配最近的外层 query scope。
3. 如果 receiver 属于外层 scope，生成列引用，不生成参数。
4. 如果表达式是普通运行时值，则生成参数。

### SelectTransformer

扩展 `FieldAnalysis`，或让它委托给 `ExpressionAnalysis` 处理表达式 select item。

必须支持：

```kotlin
f.rowNumber().over { ... }.as_(rn)
Order().select { ... }.scalar<Long>().as_("orderCount")
it[alias]
it["alias"]
```

当前 `FunctionField` 路径不足以表达窗口函数，因为窗口函数需要 `over`、`filter`、frame 等元数据。
可选方案：

1. 扩展 `FunctionField`：
   ```kotlin
   var window: WindowSpec? = null
   var filterCriteria: Criteria? = null
   ```
2. 推荐新增 `ExpressionField`：
   ```kotlin
   class ExpressionField(
       val expression: Expression,
       alias: String?
   ) : Field(alias ?: expression.defaultName)
   ```

建议：新能力使用 `ExpressionField`，`FunctionField` 保持兼容。

### ConditionTransformer

当前 `ConditionAnalysis` 输出 `Criteria`。这对简单字段条件有效，但 `EXISTS` 没有 field，
继续强塞进 `Criteria(field, type, value)` 会越来越别扭。

第一阶段：

- 新增 `ConditionType.EXISTS`、`NOT_EXISTS`、`IN_SUBQUERY`、`NOT_IN_SUBQUERY`、
  `SCALAR_COMPARE`、`TUPLE_IN`、`QUANTIFIED_COMPARE`。
- 将子查询 AST 存进 `Criteria.value`。
- 更新 `CriteriaToAstConverter`。

第二阶段：

- 新 DSL 条件块直接返回 `Expression`。
- `Criteria` 保留为旧 DSL 的兼容 IR。

推荐最终模型：

```kotlin
sealed interface ConditionIr
data class CriteriaIr(val criteria: Criteria) : ConditionIr
data class ExpressionIr(val expression: Expression) : ConditionIr
```

### SetTransformer

增强赋值分析：

```kotlin
it.lastOrderAt = Order().select { ... }.scalar<LocalDateTime>()
```

右侧应识别为 `ScalarSubqueryExpression`，不能当成普通参数。

### SortTransformer

支持表达式排序：

```kotlin
orderBy {
    Order().select { f.max(it.createTime) }
        .where { o -> o.userId == it.id }
        .scalar<LocalDateTime>()
        .desc()
}
```

保留现有字段排序逻辑。

### Symbols 和 Constants

新增符号：

- `DerivedTable`
- `QueryAlias`
- `ScalarSubquery`
- `ExistsSubquery`
- `QuantifiedSubquery`
- `ExpressionField`
- `WindowBuilder`
- `WindowFrameBuilder`
- `alias`
- `exists`
- `notExists`
- `any`
- `some`
- `all`
- `tuple`
- `scalar`
- `as_` 的 alias 与 derived table overload
- `get(QueryAlias)` 和 `get(String)` operator

## 运行时构建与参数处理

嵌套子查询需要共享参数命名逻辑。当前 clause builder 多处使用 mutable map 收集参数，
子查询嵌套后必须避免参数名冲突。

引入：

```kotlin
class QueryBuildContext(
    val parameterValues: MutableMap<String, Any?> = mutableMapOf(),
    private val counters: MutableMap<String, Int> = mutableMapOf()
) {
    fun bind(baseName: String, value: Any?): Parameter.NamedParameter
    fun child(prefix: String? = null): QueryBuildContext
}
```

规则：

- 所有嵌套 statement builder 接收同一个 `QueryBuildContext`。
- 如果外层和内层都绑定 `:id`，第二个变成 `:id@1`。
- 渲染出的 SQL 必须使用重命名后的参数。
- 参数后处理仍尽量使用字段元数据。
- 对别名字段和表达式字段，无法找到字段元数据时直接使用原值。

示例：

```kotlin
User(id = 1)
    .select()
    .where { u ->
        exists(
            Order(id = 1)
                .select { "1" }
                .where { o -> o.id == 1 && o.userId == u.id }
        )
    }
```

可能绑定为：

```text
:id      -> 外层 user id
:id@1    -> 内层 order id
```

## SQL 渲染

### 派生表

```sql
(SELECT ...) AS alias
```

别名和标识符按方言引用规则渲染。

### 标量子查询

```sql
(SELECT ...)
```

### EXISTS

```sql
EXISTS (SELECT ...)
NOT EXISTS (SELECT ...)
```

### IN 子查询

```sql
expr IN (SELECT ...)
expr NOT IN (SELECT ...)
```

### 量词比较

```sql
expr > ANY (SELECT ...)
expr <= ALL (SELECT ...)
```

方言注意：

- PostgreSQL 支持 `ANY` / `ALL`。
- MySQL 支持 `ANY` / `SOME` / `ALL`。
- SQLite 支持有限，不支持时抛出方言特性异常。
- SQL Server 和 Oracle 支持常见量词比较，但必须补测试。

### 窗口函数

```sql
ROW_NUMBER() OVER (
    PARTITION BY ...
    ORDER BY ...
    ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
)
```

当前 `FunctionCall(over = WindowClause?)` 已经接近目标。需要确保：

- `FunctionManager.renderFunctionCall` 不丢失 `over`。
- 各 function builder 要么复用统一窗口渲染逻辑，要么至少不能丢弃 `WindowClause.frame`。
- 注册 `ROW_NUMBER`、`RANK`、`DENSE_RANK`、`LAG`、`LEAD`、`FIRST_VALUE`、
  `LAST_VALUE`、`NTILE` 等函数，并按方言判断支持情况。

## 方言兼容矩阵

目标矩阵：

| 功能 | MySQL | PostgreSQL | SQLite | SQL Server | Oracle |
| --- | --- | --- | --- | --- | --- |
| FROM 派生表 | 支持 | 支持 | 支持 | 支持 | 支持 |
| JOIN 派生表 | 支持 | 支持 | 支持 | 支持 | 支持 |
| EXISTS | 支持 | 支持 | 支持 | 支持 | 支持 |
| IN 子查询 | 支持 | 支持 | 支持 | 支持 | 支持 |
| 标量子查询 | 支持 | 支持 | 支持 | 支持 | 支持 |
| ANY / SOME / ALL | 支持 | 支持 | 有限 | 支持 | 支持 |
| 元组 IN | 支持 | 支持 | 有限 | 有限 | 支持 |
| 窗口函数 | MySQL 8+ | 支持 | 3.25+ | 支持 | 支持 |
| INSERT SELECT | 支持 | 支持 | 支持 | 支持 | 支持 |
| CTAS | 支持 | 支持 | 支持 | 变体 | 支持 |
| CREATE VIEW AS | 支持 | 支持 | 支持 | 支持 | 支持 |

当能力依赖数据库版本时，应给出清晰异常：

```kotlin
UnsupportedSqlFeatureException(
    dbType = DBType.Mysql,
    feature = "window functions",
    requirement = "MySQL 8.0+"
)
```

## 结果映射

派生表查询可以返回：

- `query()` 的 Map。
- `queryList<T>()`，前提是 select alias 能匹配目标 KPojo 或 DTO 字段。
- 现有 typed query 支持的 DTO 映射。

示例：

```kotlin
data class UserWithRank(
    val id: Int,
    val groupCol: String,
    val rn: Int
)

val rows = T()
    .select { ... f.rowNumber().as_("rn") }
    .as_ { x ->
        select { x.id + x.groupCol + x["rn"] }
    }
    .queryList<UserWithRank>()
```

如果现有 type-parameter fixer 已支持非 KPojo DTO 映射，则可以直接复用。否则先文档化为
map-first 使用方式。

## 校验规则

尽量编译期校验：

- `scalar()` 只能用于恰好选择一个表达式的查询。
- `exists()` 可用于任意 select。
- `IN subquery` 应选择一个表达式，除非左侧是 tuple。
- tuple 左侧元素数量必须与子查询 select list 数量一致。
- 派生表字段访问必须引用内层已投影字段。
- 窗口 `orderBy` 必须是排序表达式。
- 窗口 frame 边界必须符合 SQL 规则。

运行期兜底校验：

- 动态字符串 alias。
- 条件化 select list。
- 原生 SQL select item。
- 跨模块 KPojo 元数据在插件中不可见的场景。

## 测试计划

### Core AST 渲染测试

补充：

- `ScalarSubqueryExpression`
- `ExistsSubqueryExpression`
- `InSubqueryExpression`
- `QuantifiedSubqueryExpression`
- `TupleExpression`
- `DerivedTableReference`
- `WithClause`
- `InsertSource.Select`
- 带 partition/order/frame 的窗口函数

覆盖方言：

- MySQL renderer
- PostgreSQL renderer
- SQLite renderer
- SQL Server renderer
- Oracle renderer

### ORM Clause 测试

补充 SQL 和参数测试：

- `select().as_ { x -> select { ... } }`
- `select().as_("tmp") { x -> select { ... } }`
- `select().asTable("tmp").select()`
- 带 alias 字段的派生表
- 相关 `exists`
- select list 中的相关标量子查询
- IN 子查询
- NOT EXISTS
- HAVING 标量子查询
- ORDER BY 标量子查询
- UPDATE SET 标量子查询
- UPDATE WHERE EXISTS
- DELETE WHERE IN
- INSERT SELECT
- CREATE VIEW AS SELECT
- CREATE TABLE AS SELECT

### 编译器插件测试

补充：

- `f.rowNumber().over { ... }.as_(rn)` 的字段分析
- `it[alias]` 的字段分析
- `exists(selectClause)` 的条件分析
- `it.id in selectClause` 的条件分析
- `it.score > scalar` 的条件分析
- 相关子查询引用外层 scope
- `SetTransformer` 处理 RHS scalar subquery
- `SortTransformer` 处理表达式排序

### 集成测试

优先级：

1. MySQL 8+ 和 PostgreSQL：窗口函数、派生表、CTE。
2. SQLite：基础子查询、派生表。
3. SQL Server 和 Oracle：渲染兼容与语法差异。

## 实施计划

### 第一阶段：表达式与渲染基础

- 统一或新增 scalar、exists、quantified、tuple、derived table、insert select AST。
- 引入 `QueryBuildContext`。
- 确保各方言 renderer 支持这些 AST。
- 补 core rendering tests。

### 第二阶段：嵌入式 SelectClause API

- 添加 `SelectClause.as_ { ... }`。
- 添加 `SelectClause.as_("tmp") { ... }`。
- 添加 `SelectClause.asTable(alias)`。
- 添加 `SelectClause.scalar<R>()`。
- 添加 `exists(select)`、`notExists(select)`。
- 添加 `DerivedTable.select`。
- 添加 `DerivedTable` 投影元数据与字段校验。
- 尽可能先写不依赖编译器插件的新 SQL 生成测试。

### 第三阶段：编译器插件支持

- 新增 `ExpressionAnalysis`。
- 扩展 `FieldAnalysis`：表达式字段、alias token、窗口函数、标量子查询。
- 扩展 `ConditionAnalysis`：exists/in/scalar/any/all/tuple。
- 扩展 `SetTransformer`：scalar RHS。
- 扩展 `SortTransformer`：表达式排序。

### 第四阶段：DML 与 DDL

- 添加 `InsertSource.Select`。
- 添加 `InsertClause.select(source)`。
- 添加 create view as select。
- 添加 create table as select。
- 补 DML 和 DDL 测试。

### 第五阶段：CTE 与高级方言特性

- 添加非递归 CTE。
- 后续添加递归 CTE。
- 添加 tuple、quantified comparison 的方言开关。
- 按方言添加 materialized view。

## 待确认问题

1. `DerivedTable<T>` 是否只暴露已投影字段，还是允许访问所有原始 KPojo 字段并在 SQL 生成时失败？
   推荐：只暴露已投影字段，动态场景运行期兜底。
2. `SelectClause.as_("tmp") { ... }` 表示进入具名派生表外层查询，而
   `select { expr.as_("alias") }` 表示 select item alias。这个重载是否可接受？
   推荐接受，因为二者的调用形态不同：前者有外层查询 block，后者出现在 select 表达式内。
3. `scalar<R>()` 是否必须显式调用？推荐比较场景显式，`IN` 场景可以隐式接受单列 select。
4. `exists(select)` 是否接受任意 `KSelectable`，包括 union？推荐接受。
5. 方言版本相关能力如何判断？推荐提供 feature flag，让 wrapper 可覆盖。

## 总结

核心设计是：

```kotlin
KPojo().select { ... }.where { ... }          // 现有查询
KPojo().select { ... }.where { ... }.as_ { x -> ... } // 嵌入式 FROM 子查询
KPojo().select { ... }.where { ... }.asTable("tmp")   // 可复用派生表源
KPojo().select { ... }.where { ... }.scalar() // 标量子查询
exists(KPojo().select { ... }.where { ... })  // EXISTS 子查询
```

FROM 子查询的主推荐写法是：

```kotlin
T()
    .select { ... }
    .where { ... }
    .as_ { x ->
        select { ... }
        where { ... }
    }
```

这样既保留了 Kronos 当前链式 DSL 的心智模型，又能覆盖查询、DML 和 DDL 中的完整子查询场景。
