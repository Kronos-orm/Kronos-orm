# Kronos 子查询 DSL 语法设计

状态：语法草案

本文档只讨论用户侧 DSL 长什么样。当前正在逐个确认子查询使用场景。

实现进度请看 `SUBQUERY_TASK_LIST.md`。截至 2026-07-01，core / AST / renderer 已覆盖大量子查询底层能力，真实 `Context` FIR 生成、基础字段 alias 补全和 selected alias 自动 SQL 分层已打通第一条竖切；函数/标量子查询/聚合/window alias 字段模型、窗口函数用户 DSL、derived join 和 set/upsert 类型安全入口仍在推进中。本文档中的示例代表目标语法，不表示每个场景都已完整可用。

## 场景目录

- 场景 1：`SELECT` 列表中的标量子查询
- 场景 2：`WHERE` / `HAVING` 中的 `IN` 与 `NOT IN` 子查询
- 场景 3：`WHERE` / `HAVING` 中的 `EXISTS` 与 `NOT EXISTS` 子查询
- 场景 4：`WHERE` / `HAVING` 中的标量比较、`ANY` / `SOME` / `ALL`
- 场景 5：元组 `IN`
- 场景 6：`FROM` / `JOIN` 派生查询
- 场景 7：窗口函数与自动 SQL 分层
- 场景 8：`ORDER BY` 中的子查询或表达式排序
- 场景 9：`UPDATE SET` 标量子查询
- 场景 10：`UPDATE` / `DELETE WHERE` 子查询
- 场景 11：`INSERT SELECT`
- 场景 12：`UPSERT` 中的子查询表达式
- 场景 13：`CREATE TABLE AS SELECT`

## 基本原则

- 用户只写 `select { ... }`，不写 `select<User, Xxx>`。
- `select` lambda 的参数默认是源 KPojo；能直接用 `it` 时优先省略显式参数。
- 返回投影类型不需要用户声明 DTO，也不需要用户写任何第二泛型参数。
- 返回投影类型由 FIR 插件根据 `select` 内容自动生成。
- `.as_("name")` 只表示结果字段名，用于自动生成投影字段；它不是表别名，也不是派生表名。
- `.as_("name")` 会在 where/orderBy/having 使用的生成上下文类中生成同名属性，例如 `.as_("lastOrderAmount")` 对应后续的 `it.lastOrderAmount`。
- 不要求用户提前声明 `val rn = alias<Int>("rn")` 这类 alias token。
- 查询链统一先 `select { ... }`，再接 `where`、`orderBy`、`groupBy`、`having`、`limit` 等子句。
- `[]` 是用户侧统一列表语法；编译器按上下文生成投影列表、排序列表、窗口字段列表、row-value tuple 等内部结构。
- `select { ... }` 之后的 `where { ... }`、`orderBy { ... }`、`having { ... }` 的 `it` 是 FIR 插件生成的上下文类。
- 生成上下文类包含原 DTO 的全部列，以及 `select { ... }` 中通过 `.as_("xxx")` 等方式新增的投影列。
- 用户侧不需要关心 SQL 同层是否允许引用 select alias；`where` / `having` 引用上下文类中的投影字段时，Kronos 按五种数据库的兼容语义自动决定是否包外层派生查询。
- 不推荐对同一个 query 连续写多个 `.where { ... }` 来表达同一组筛选条件。示例和文档应优先把同一阶段条件合并到一个 `where { ... }` 中，便于判断哪些条件可以下推、哪些条件需要外层过滤。

## 类型模型

概念上，`select { ... }` 之后存在三种类型：

- `Source`：原始查询源 KPojo，例如 `User`。
- `Selected`：最终查询结果投影类型，只包含 `select { ... }` 选择出来的字段。
- `Context`：后续查询子句可见的上下文类型，包含 `Source` 的全部列，以及 `Selected` 中由字段选择、表达式、`.as_("xxx")`、窗口函数等产生的投影字段。

因此概念模型可以理解为：

```kotlin
SelectClause<Source, Selected, Context>
```

用户不显式书写这些泛型。FIR 插件负责生成 `Selected` 和 `Context`：

```kotlin
User()
    .select { it.name }      // it: Source
    .where { it.status == 1 } // it: Context
    .queryList()             // List<Selected>
```

`where` / `having` 使用 `Context` 不表示 SQL 会在同层直接引用 select alias。Kronos 必须根据字段来源自动分层：

- 引用 `Source` 字段的条件可以下推到当前查询的 `WHERE` / `HAVING`。
- 引用 `Selected` 新增投影字段、计算字段、聚合 alias、窗口 alias 的条件，如果目标数据库不能合法在同层使用，则提升到自动生成的外层派生查询中过滤。
- `queryList()`、`queryOne()`、`queryOneOrNull()` 等最终返回 `Selected`，不是 `Context`。

## 术语：KSelectable

本文档用 `KSelectable` 指代所有可被当作查询源消费的 select-like 对象。

- `select { ... }` 仍然返回具体的 `SelectClause<Source, Selected, Context>`。
- `SelectClause<Source, Selected, Context>` 实现 `KSelectable`。
- join 查询、union 查询等只要实现 `KSelectable`，也可以被子查询、insert-select、CTAS 等能力消费。
- 用户通常不需要显式声明 `KSelectable` 类型；它主要用于描述 DSL 能力边界。

## 场景 1：SELECT 列表中的标量子查询

目标：在 `select { ... }` 中放一个返回单值的子查询，例如用户最近一笔订单金额。

推荐语法：

```kotlin
val rows = User()
    .select { u ->
        [
            u.id,
            u.name,
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .as_("lastOrderAmount")
        ]
    }
    .where {
        it.status == 1
    }
    .queryList()
```

语义：

- `u` 是完整 `User`。
- `Order().select { it.amount }` 作为 select item 出现时，默认按标量子查询处理。
- `rows` 的元素类型由 FIR 插件根据 select 内容自动生成。
- 返回投影类型至少包含 `id`、`name`、`lastOrderAmount` 三个字段。
- `lastOrderAmount` 来自 `.as_("lastOrderAmount")`。
- `where` 的 `it` 是生成上下文类，包含 `User` 全部列和 select 新增列，所以可以同时访问 `it.status` 与 `it.lastOrderAmount`。

因此可以在普通 `where` 中同时过滤源字段和 select alias：

```kotlin
val rows = User()
    .select { u ->
        [
            u.id,
            u.name,
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .as_("lastOrderAmount")
        ]
    }
    .where {
        it.status == 1 &&
            it.lastOrderAmount > 100
    }
    .queryList()
```

如果当前数据库不能在同层 `WHERE` 中引用 select alias，Kronos 根据 `where` 是否引用新增投影列自动决定是否包外层派生查询；用户语法不变。

推荐把同一阶段的过滤写在同一个 `where { ... }` 中，而不是拆成多次 `.where { ... }`：

```kotlin
val rows = User()
    .select { u ->
        [
            u.id,
            u.name,
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .as_("lastOrderAmount")
        ]
    }
    .where {
        it.status == ACTIVE &&
            it.lastOrderAmount > 100
    }
    .queryList()
```

### 嵌套子查询示例

子查询可以继续包含子查询。下面示例查询“最近订单金额大于该用户所在等级最低额度”的用户：

```kotlin
val rows = User()
    .select { u ->
        [
            u.id,
            u.name,
            Order()
                .select { o -> o.amount }
                .where { o ->
                    o.userId == u.id &&
                        o.amount > UserLevel()
                            .select { l -> l.minOrderAmount }
                            .where { l -> l.level == u.level }
                            .limit(1)
                }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .as_("qualifiedLastOrderAmount")
        ]
    }
    .where {
        it.status == ACTIVE &&
            it.qualifiedLastOrderAmount > 0
    }
    .queryList()
```

SQL 形态上，外层 select item 是标量子查询，该标量子查询的 `where` 内部又包含一个标量子查询：

```sql
SELECT
    u.id,
    u.name,
    (
        SELECT o.amount
        FROM orders o
        WHERE o.user_id = u.id
          AND o.amount > (
              SELECT l.min_order_amount
              FROM user_level l
              WHERE l.level = u.level
              LIMIT 1
          )
        ORDER BY o.create_time DESC
        LIMIT 1
    ) AS qualifiedLastOrderAmount
FROM user u
WHERE u.status = ?
```

如果后续 `where` 同时引用 `it.qualifiedLastOrderAmount`，Kronos 仍按上下文字段来源决定是否自动包外层派生查询，确保 MySQL、PostgreSQL、SQLite、SQL Server、Oracle 的语义一致。

如果需要显式类型提示，可以使用 Kotlin cast：

```kotlin
val rows = User()
    .select { u ->
        [
            u.id,
            u.name,
            (
                Order()
                    .select { it.amount }
                    .where { it.userId == u.id }
                    .orderBy { it.createTime.desc() }
                    .limit(1) as BigDecimal
            ).as_("lastOrderAmount")
        ]
    }
    .queryList()
```

`limit(1) as BigDecimal` 中的 cast 是标量子查询类型提示，不改变 SQL，也不能绕过单列/单行校验。默认应根据子查询选择的字段或表达式推断类型。

### limit 规则

标量子查询必须能保证只返回一行。

第一版采用保守规则：聚合且无 `groupBy` 的标量子查询可以不写 `.limit(1)`；其他非聚合标量子查询必须显式写 `.limit(1)`：

```kotlin
Order()
    .select { it.amount }
    .where { it.userId == u.id }
    .orderBy { it.createTime.desc() }
    .limit(1)
```

下面这种写法不应该被接受：

```kotlin
Order()
    .select { it.amount }
    .where { it.userId == u.id }
```

Kronos 不自动补 `.limit(1)`。

原因：

- 自动补 limit 会改变业务语义。
- 如果没有 `orderBy`，最近、最大、最小等业务含义都不明确。
- 标量子查询返回多行本来就是用户应该显式处理的问题。

聚合且无 `groupBy` 的子查询可以不写 `.limit(1)`：

```kotlin
Order()
    .select { f.max(it.amount) }
    .where { it.userId == u.id }
```

唯一键证明可以后续增强，但不作为第一版主规则。

### having 中的用法

`having` 通常用于聚合结果过滤，可以访问 select alias / 聚合结果，并由 Kronos 决定是否需要自动 SQL 分层：

```kotlin
val rows = Order()
    .select {
        [
            it.userId,
            f.sum(it.amount).as_("totalAmount")
        ]
    }
    .groupBy { it.userId }
    .having {
        it.totalAmount > 1000
    }
    .queryList()
```

这里 `.as_("totalAmount")` 在生成上下文类中生成 `totalAmount` 属性，所以 `having` 中可以写 `it.totalAmount`。

### 期望 SQL

```sql
SELECT
    u.id,
    u.name,
    (
        SELECT o.amount
        FROM orders o
        WHERE o.user_id = u.id
        ORDER BY o.create_time DESC
        LIMIT 1
    ) AS lastOrderAmount
FROM user u
WHERE u.status = ?
```

如果 `where` 引用了 select alias，实际渲染时如果当前数据库不能在同层过滤 select alias，Kronos 可以自动包一层派生查询；用户语法不变。

### 当前确认

- 只使用 `select { ... }`。
- 不需要声明投影 DTO。
- 不需要 `select<User, UserOrderView>`。
- 返回投影类型和后续子句使用的上下文类由 FIR 插件根据 select 内容生成。
- `.as_("xxx")` 生成自动投影属性 `it.xxx`。
- `where` 的 `it` 是生成上下文类，包含原 DTO 全部列和 select 新增列。
- `where` 可以直接访问 select alias / 窗口结果；Kronos 根据引用情况自动决定是否包外层派生查询。
- 子查询 select item 默认支持标量化。
- 类型提示使用 `limit(1) as T`。
- 聚合且无 `groupBy` 可不写 `.limit(1)`；其他非聚合标量子查询必须写 `.limit(1)`。

## 场景 2：WHERE / HAVING 中的 IN 与 NOT IN 子查询

目标：用一个子查询结果集筛选当前查询，例如“查询有已支付订单的用户”、“查询不在黑名单中的用户”、“筛选聚合结果属于某个集合的分组”。

### WHERE IN

推荐使用 Kotlin 原生 `in`：

```kotlin
val rows = User()
    .select {
        [
            it.id,
            it.name
        ]
    }
    .where {
        it.id in Order()
            .select { it.userId }
            .where { it.status == PAID }
    }
    .queryList()
```

语义：

- `where` 的 `it` 是生成上下文类，包含 `User` 全部列和 select 新增列。
- `Order().select { it.userId }` 是 `IN` 右侧的子查询。
- 右侧子查询必须只选择一个字段或表达式。
- `select { ... }` 后自动生成投影类型，返回结果只包含 `id`、`name`。

期望 SQL：

```sql
SELECT u.id, u.name
FROM user u
WHERE u.id IN (
    SELECT o.user_id
    FROM orders o
    WHERE o.status = ?
)
```

### WHERE NOT IN

推荐使用 Kotlin 原生 `!in`：

```kotlin
val rows = User()
    .select {
        [
            it.id,
            it.name
        ]
    }
    .where {
        it.id !in UserBlacklist()
            .select { it.userId }
            .where { it.enabled == true }
    }
    .queryList()
```

期望 SQL：

```sql
SELECT u.id, u.name
FROM user u
WHERE u.id NOT IN (
    SELECT b.user_id
    FROM user_blacklist b
    WHERE b.enabled = ?
)
```

### 源字段过滤不要求投影

因为 `where` 的生成上下文类包含源 KPojo 全部列，过滤源字段不要求该字段出现在 select 列表中：

```kotlin
val rows = User()
    .select {
        [
            it.id,
            it.name
        ]
    }
    .where {
        it.status in UserStatusRule()
            .select { it.allowedStatus }
            .where { it.scene == "public" }
    }
    .queryList()
```

### HAVING IN

`having` 同样可以使用 `in`，并可访问聚合投影字段：

```kotlin
val rows = Order()
    .select {
        [
            it.userId,
            f.sum(it.amount).as_("totalAmount")
        ]
    }
    .groupBy { it.userId }
    .having {
        it.totalAmount in VipLevel()
            .select { it.minAmount }
            .where { it.enabled == true }
    }
    .queryList()
```

这里 `it.totalAmount` 来自 `.as_("totalAmount")` 生成的自动投影属性。

### 子查询选择多列

普通 `IN` 右侧只能选择一列：

```kotlin
u.id in Order()
    .select { it.userId }
```

多列匹配放到“元组 IN”场景，使用 `[]`：

```kotlin
[u.id, u.createTime] in Order()
    .select { [it.userId, f.max(it.createTime)] }
    .groupBy { it.userId }
```

### 当前确认

- `IN` 使用 Kotlin 原生 `in`。
- `NOT IN` 使用 Kotlin 原生 `!in`。
- 右侧直接写 `KSelectable` 子查询，不引入 `inSubquery { ... }`。
- 普通 `IN` 的右侧子查询必须只选择一列。
- `where` 的 lambda 参数是生成上下文类，包含源 KPojo 全部列和 select 新增列。
- `having` 可以访问聚合投影字段。
- 元组 `IN` 不放在本场景，后续单独确认。

## 场景 3：WHERE / HAVING 中的 EXISTS 与 NOT EXISTS 子查询

目标：判断相关记录是否存在，例如“查询有已支付订单的用户”、“查询没有黑名单记录的用户”。

### WHERE EXISTS

推荐使用 `exists(query)`，子查询统一从 `select()` 进入：

```kotlin
val rows = User()
    .select {
        [
            it.id,
            it.name
        ]
    }
    .where { u ->
        exists(
            Order()
                .select()
                .where { o ->
                    o.userId == u.id &&
                        o.status == PAID
                }
        )
    }
    .queryList()
```

语义：

- `where` 的 `u` 是生成上下文类，包含 `User` 全部列和 select 新增列。
- `Order().select().where { ... }` 表示 `EXISTS` 的子查询来源。
- 用户不需要写 `select { 1 }`；渲染时可以优化成 `EXISTS (SELECT 1 ...)`。
- `exists(...)` 默认表达“是否存在满足条件的行”。

期望 SQL：

```sql
SELECT u.id, u.name
FROM user u
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.user_id = u.id
      AND o.status = ?
)
```

### WHERE NOT EXISTS

否定使用 Kotlin 原生 `!exists(query)`：

```kotlin
val rows = User()
    .select {
        [
            it.id,
            it.name
        ]
    }
    .where { u ->
        !exists(
            UserBlacklist()
                .select()
                .where { b ->
                    b.userId == u.id &&
                        b.enabled == true
                }
        )
    }
    .queryList()
```

期望 SQL：

```sql
SELECT u.id, u.name
FROM user u
WHERE NOT EXISTS (
    SELECT 1
    FROM user_blacklist b
    WHERE b.user_id = u.id
      AND b.enabled = ?
)
```

### HAVING EXISTS

`having` 中也可以使用 `exists`。如果需要引用聚合投影字段，可以通过自动投影属性访问：

```kotlin
val rows = User()
    .select {
        [
            it.groupId,
            f.count(it.id).as_("userCount")
        ]
    }
    .groupBy { it.groupId }
    .having { g ->
        exists(
            GroupLimit()
                .select()
                .where { l ->
                    l.groupId == g.groupId &&
                        l.minCount <= g.userCount
                }
        )
    }
    .queryList()
```

这里：

- `g.groupId` 来自 `u.groupId` 生成的自动投影字段。
- `g.userCount` 来自 `.as_("userCount")` 生成的自动投影字段。
- `exists(...)` 的子查询可以引用外层 lambda 参数字段。

### 带 select 的 query source

`exists(...)` 也允许接收已经带条件的 query source，但仍然必须先调用 `select()`：

```kotlin
val paidOrders = Order()
    .select()
    .where { it.status == PAID }

val rows = User()
    .select {
        [
            it.id,
            it.name
        ]
    }
    .where { u ->
        exists(paidOrders.where { it.userId == u.id })
    }
    .queryList()
```

在 `EXISTS` 语义中，select 列表不重要。主路径不要求手写 `select { 1 }`，但也不设计 `Order().where { ... }` 这种无 `select` 入口。

### 当前确认

- `EXISTS` 使用 `exists(query)`。
- `NOT EXISTS` 使用 `!exists(query)`。
- 不提供 `notExists(...)`。
- `EXISTS` 子查询统一使用 `KPojo().select().where { ... }` 入口。
- 不设计 `KPojo().where { ... }` 这种无 `select` 入口。
- `exists(KPojo().select().where { ... })` 默认渲染为 `EXISTS (SELECT 1 FROM ... WHERE ...)`。
- 如果传入带 `select { ... }` 的 query source，也允许，但 select 列表不影响 `EXISTS` 语义。
- `where` 的 lambda 参数是生成上下文类，包含源 KPojo 全部列和 select 新增列。
- `having` 可以访问聚合投影字段。
- 相关子查询可以引用外层 lambda 参数字段。
## 场景 4：WHERE / HAVING 中的标量比较、ANY / SOME / ALL

目标：让字段或表达式与子查询结果比较，例如“价格大于平均价”、“分数大于任意历史分数”、“价格不高于同类所有价格”。

本场景分两类：

- 标量子查询比较：右侧子查询必须能保证只返回一行。
- 量词比较：`ANY` / `SOME` / `ALL` 本来就是和一组值比较，不需要 `.limit(1)`。

### 标量子查询比较

直接使用 Kotlin 比较运算符：

```kotlin
val rows = User()
    .select { u ->
        [
            u.id,
            u.name,
            u.score
        ]
    }
    .where { u ->
        u.score > Score()
            .select { f.avg(it.score) }
            .where { it.groupId == u.groupId }
    }
    .queryList()
```

这里右侧是聚合子查询，天然只返回一个值，因此不需要 `.limit(1)`。

期望 SQL：

```sql
SELECT u.id, u.name, u.score
FROM user u
WHERE u.score > (
    SELECT AVG(s.score)
    FROM score s
    WHERE s.group_id = u.group_id
)
```

如果右侧不是“聚合且无 `groupBy`”，就必须显式写 `.limit(1)`：

```kotlin
val rows = Product()
    .select { p ->
        [
            p.id,
            p.name,
            p.price
        ]
    }
    .where { p ->
        p.price < ProductPrice()
            .select { it.price }
            .where { it.productId == p.id }
            .orderBy { it.createdAt.desc() }
            .limit(1)
    }
    .queryList()
```

下面这种写法不应该被接受：

```kotlin
p.price < ProductPrice()
    .select { it.price }
    .where { it.productId == p.id }
```

原因：`>`、`>=`、`<`、`<=`、`==`、`!=` 直接接 `KSelectable` 时，语义是“和一个标量值比较”，所以右侧必须能保证单行。

### ANY

`ANY` 使用 `any(query)`：

```kotlin
val rows = Product()
    .select { p ->
        [
            p.id,
            p.name,
            p.price
        ]
    }
    .where { p ->
        p.price > any(
            ProductPrice()
                .select { it.price }
                .where { it.categoryId == p.categoryId }
        )
    }
    .queryList()
```

期望 SQL：

```sql
SELECT p.id, p.name, p.price
FROM product p
WHERE p.price > ANY (
    SELECT h.price
    FROM product_price h
    WHERE h.category_id = p.category_id
)
```

`any(...)` 右侧是集合，不需要 `.limit(1)`。

### SOME

`SOME` 使用 `some(query)`，语义等同 `any(query)`：

```kotlin
val rows = Product()
    .select { p ->
        [
            p.id,
            p.name,
            p.price
        ]
    }
    .where { p ->
        p.price > some(
            ProductPrice()
                .select { it.price }
                .where { it.categoryId == p.categoryId }
        )
    }
    .queryList()
```

### ALL

`ALL` 使用 `all(query)`：

```kotlin
val rows = Product()
    .select { p ->
        [
            p.id,
            p.name,
            p.price
        ]
    }
    .where { p ->
        p.price <= all(
            ProductPrice()
                .select { it.price }
                .where { it.categoryId == p.categoryId }
        )
    }
    .queryList()
```

期望 SQL：

```sql
SELECT p.id, p.name, p.price
FROM product p
WHERE p.price <= ALL (
    SELECT h.price
    FROM product_price h
    WHERE h.category_id = p.category_id
)
```

`all(...)` 右侧是集合，不需要 `.limit(1)`。

### HAVING 中的用法

`having` 中同样可以使用标量比较和量词比较：

```kotlin
val rows = Order()
    .select {
        [
            it.userId,
            f.sum(it.amount).as_("totalAmount")
        ]
    }
    .groupBy { it.userId }
    .having { g ->
        g.totalAmount > UserQuota()
            .select { it.minAmount }
            .where { it.userId == g.userId }
            .limit(1)
    }
    .queryList()
```

如果使用 `any` / `some` / `all`，则不需要 `.limit(1)`：

```kotlin
val rows = Order()
    .select {
        [
            it.userId,
            f.sum(it.amount).as_("totalAmount")
        ]
    }
    .groupBy { it.userId }
    .having { g ->
        g.totalAmount > any(
            UserQuota()
                .select { it.minAmount }
                .where { it.enabled == true }
        )
    }
    .queryList()
```

### 当前确认

- 标量比较直接使用 Kotlin 运算符：`>`、`>=`、`<`、`<=`、`==`、`!=`。
- `field > KSelectable` 这类写法表示标量子查询比较。
- 标量子查询比较右侧必须能保证单行；非聚合标量子查询必须 `.limit(1)`。
- 聚合且无 `groupBy` 的子查询天然单行，不需要 `.limit(1)`。
- `ANY` 使用 `any(query)`。
- `SOME` 使用 `some(query)`，语义等同 `any(query)`。
- `ALL` 使用 `all(query)`。
- `any` / `some` / `all` 右侧是集合量词比较，不需要 `.limit(1)`。
- `any` / `some` / `all` 右侧子查询必须只选择一列。
- `where` 的 lambda 参数是生成上下文类，包含源 KPojo 全部列和 select 新增列；`having` 可以访问聚合投影字段。

## 场景 5：元组 IN

目标：用多个字段组成一个 row-value tuple，与子查询返回的多列结果进行匹配，例如“每个用户最新一笔订单”、“复合键存在性判断”。

普通 `IN` 只允许右侧子查询选择一列；多列匹配使用 `[]` 字面量表达 SQL row-value tuple。

### 基础语法

推荐使用 `[...] in KSelectable`：

```kotlin
val rows = Order()
    .select {
        [
            it.id,
            it.userId,
            it.createTime,
            it.amount
        ]
    }
    .where { o ->
        [o.userId, o.createTime] in Order()
            .select { i ->
                [
                    i.userId,
                    f.max(i.createTime)
                ]
            }
            .groupBy { it.userId }
    }
    .queryList()
```

期望 SQL：

```sql
SELECT o.id, o.user_id, o.create_time, o.amount
FROM orders o
WHERE (o.user_id, o.create_time) IN (
    SELECT i.user_id, MAX(i.create_time)
    FROM orders i
    GROUP BY i.user_id
)
```

语义：

- `[o.userId, o.createTime]` 是左侧 row-value tuple。
- 右侧子查询必须选择两列，且顺序与左侧 tuple 一一对应。
- tuple 元素数量必须与右侧 select 列数量一致。
- tuple 元素类型应与右侧对应列类型兼容。

### NOT IN

否定使用 Kotlin 原生 `!in`：

```kotlin
val rows = Order()
    .select {
        [
            it.id,
            it.userId,
            it.productId
        ]
    }
    .where { o ->
        [o.userId, o.productId] !in UserProductBlock()
            .select { b ->
                [
                    b.userId,
                    b.productId
                ]
            }
            .where { it.enabled == true }
    }
    .queryList()
```

期望 SQL：

```sql
SELECT o.id, o.user_id, o.product_id
FROM orders o
WHERE (o.user_id, o.product_id) NOT IN (
    SELECT b.user_id, b.product_id
    FROM user_product_block b
    WHERE b.enabled = ?
)
```

### HAVING 中的元组 IN

`having` 中也可以使用 `[]` tuple：

```kotlin
val rows = Order()
    .select { o ->
        [
            o.userId,
            f.sum(o.amount).as_("totalAmount"),
            f.count(o.id).as_("orderCount")
        ]
    }
    .groupBy { it.userId }
    .having {
        [it.totalAmount, it.orderCount] in VipRule()
            .select { r ->
                [
                    r.minAmount,
                    r.minOrderCount
                ]
            }
            .where { it.enabled == true }
    }
    .queryList()
```

### 与普通 IN 的边界

单列场景不要使用 `[]` tuple：

```kotlin
u.id in Order()
    .select { it.userId }
```

tuple IN 单元素不允许。下面这种写法不应该被接受：

```kotlin
[u.id] in Order()
    .select { it.userId }
```

多列场景必须使用 `[]`，不能让普通 `in` 右侧选择多列：

```kotlin
[u.id, u.createTime] in Order()
    .select { o ->
        [
            o.userId,
            f.max(o.createTime)
        ]
    }
```

下面这种写法不应该被接受：

```kotlin
u.id in Order()
    .select { o ->
        [
            o.userId,
            f.max(o.createTime)
        ]
    }
```

### 当前确认

- 元组 `IN` 使用 `[...] in KSelectable`。
- 元组 `NOT IN` 使用 `[...] !in KSelectable`。
- 不引入 `tuple(...)`、`tupleIn(...)` 或 `inTuple(...)`。
- 在条件表达式左侧，`[...] in KSelectable` 表示 SQL row-value tuple，不是普通运行时 `List` 比较。
- 左侧 tuple 元素数量必须大于 1；单元素 `[...] in query` 不允许。
- 左侧 tuple 元素数量必须与右侧 select 列数量一致。
- 左侧 tuple 元素类型必须与右侧 select 对应列类型兼容。
- 单列 `IN` 继续使用场景 2 的普通 `field in KSelectable`。
- 普通 `field in KSelectable` 右侧不能选择多列。
## 场景 6：FROM / JOIN 派生查询

目标：让已有查询结果作为派生表参与 `JOIN`，例如“用户关联最近一笔订单时间”、“订单关联聚合后的商品统计”。

本场景保留现有 Kronos join 心智：主表写在前面，join 动词写在主表后面。

不引入：

- `[A, B].select { ... }`
- `from(A, B) { ... }`
- `.asTable("tmp")`

### JOIN 派生查询

派生查询可以直接作为 `join` / `leftJoin` / `rightJoin` 的目标：

```kotlin
val latestOrder = Order()
    .select { o ->
        [
            o.userId,
            f.max(o.createTime).as_("lastOrderAt")
        ]
    }
    .groupBy { it.userId }

val rows = User().leftJoin(latestOrder) { user, order ->
    on { user.id == order.userId }

    select {
        [
            user.id,
            user.name,
            order.lastOrderAt
        ]
    }
}.queryList()
```

语义：

- `User()` 是主表。
- `latestOrder` 是派生查询源。
- `order` 不是 `Order`，而是 `latestOrder` 的自动投影类型。
- `order.userId` 来自 `latestOrder` 的 `o.userId`。
- `order.lastOrderAt` 来自 `.as_("lastOrderAt")`。
- 用户不需要也不允许在主语法里写派生表 alias。

期望 SQL：

```sql
SELECT
    u.id,
    u.name,
    q.lastOrderAt
FROM user u
LEFT JOIN (
    SELECT
        o.user_id,
        MAX(o.create_time) AS lastOrderAt
    FROM orders o
    GROUP BY o.user_id
) q ON u.id = q.user_id
```

实际 SQL alias 由 Kronos 自动生成，例如 `_kq_0`；用户不感知。

### 多表 JOIN

现有多表 join 写法保持不变：

```kotlin
val rows = User().join(UserRelation(), UserRole()) { user, relation, role ->
    on { user.id == relation.userId }
    on { user.id == role.userId }

    select {
        [
            user.id,
            user.name,
            relation.id.as_("relationId"),
            role.role
        ]
    }
}.queryList()
```

如果其中某个 join 目标是派生查询，对应 lambda 参数就是该派生查询的自动投影类型：

```kotlin
val roleCount = UserRole()
    .select { r ->
        [
            r.userId,
            f.count(r.id).as_("roleCount")
        ]
    }
    .groupBy { it.userId }

val rows = User().leftJoin(UserRelation(), roleCount) { user, relation, role ->
    on { user.id == relation.userId }
    on { user.id == role.userId }

    select {
        [
            user.id,
            user.name,
            relation.id.as_("relationId"),
            role.roleCount
        ]
    }
}.queryList()
```

### JOIN 条件字段必须在派生查询投影中

如果 join 条件需要使用派生查询字段，该字段必须出现在派生查询的 `select` 列表中。

正确：

```kotlin
val latestOrder = Order()
    .select { o ->
        [
            o.userId,
            f.max(o.createTime).as_("lastOrderAt")
        ]
    }
    .groupBy { it.userId }

User().leftJoin(latestOrder) { user, order ->
    on { user.id == order.userId }
    select {
        [
            user.id,
            order.lastOrderAt
        ]
    }
}
```

不正确：

```kotlin
val latestOrder = Order()
    .select { o ->
        [
            f.max(o.createTime).as_("lastOrderAt")
        ]
    }
    .groupBy { it.userId }

User().leftJoin(latestOrder) { user, order ->
    on { user.id == order.userId }
    select {
        [
            user.id,
            order.lastOrderAt
        ]
    }
}
```

这里 `order.userId` 没有被 `latestOrder` 投影出来，因此不能在外层 join 条件中访问。

### 对派生查询继续查询

一个已经生成自动投影类型的 query source，可以继续链式查询：

```kotlin
val latestOrder = Order()
    .select { o ->
        [
            o.userId,
            f.max(o.createTime).as_("lastOrderAt")
        ]
    }
    .groupBy { it.userId }

val rows = latestOrder
    .where {
        it.lastOrderAt != null
    }
    .select {
        [
            it.userId,
            it.lastOrderAt
        ]
    }
    .queryList()
```

这里 `latestOrder.where { ... }` 的 `it` 是 `latestOrder` 的自动投影类型。是否需要包一层派生查询由 Kronos 自动决定。

### 派生查询作为主源继续 JOIN

派生查询也可以作为 join 的主源，继续连接普通表或其他派生查询：

```kotlin
val latestOrder = Order()
    .select { o ->
        [
            o.userId,
            f.max(o.createTime).as_("lastOrderAt")
        ]
    }
    .groupBy { it.userId }

val rows = latestOrder.leftJoin(User()) { order, user ->
    on { order.userId == user.id }

    select {
        [
            user.id,
            user.name,
            order.lastOrderAt
        ]
    }
}.queryList()
```

语义：

- `latestOrder` 是主 query source。
- `order` 是 `latestOrder` 的自动投影类型。
- `User()` 是 join 目标。
- `user` 是完整 `User`。
- 用户仍然不需要写派生表 alias。

期望 SQL：

```sql
SELECT
    u.id,
    u.name,
    q.lastOrderAt
FROM (
    SELECT
        o.user_id,
        MAX(o.create_time) AS lastOrderAt
    FROM orders o
    GROUP BY o.user_id
) q
LEFT JOIN user u ON q.user_id = u.id
```

派生查询也可以连接另一个派生查询：

```kotlin
val latestOrder = Order()
    .select { o ->
        [
            o.userId,
            f.max(o.createTime).as_("lastOrderAt")
        ]
    }
    .groupBy { it.userId }

val roleCount = UserRole()
    .select { r ->
        [
            r.userId,
            f.count(r.id).as_("roleCount")
        ]
    }
    .groupBy { it.userId }

val rows = latestOrder.leftJoin(roleCount) { order, role ->
    on { order.userId == role.userId }

    select {
        [
            order.userId,
            order.lastOrderAt,
            role.roleCount
        ]
    }
}.queryList()
```

这里 `order` 和 `role` 都是各自派生查询的自动投影类型。

### 当前确认

- 保留现有 `KPojo().join(...)` / `leftJoin(...)` / `rightJoin(...)` 写法。
- `KSelectable` 派生查询也可以作为 join 主源继续调用 `join(...)` / `leftJoin(...)` / `rightJoin(...)`。
- `join` 目标可以是 `KPojo()`，也可以是 `KSelectable` 派生查询。
- 如果 join 主源是 `KSelectable`，lambda 的第一个参数是该主源的自动投影类型。
- 如果 join 目标是 `KPojo()`，lambda 参数是该 KPojo 类型。
- 如果 join 目标是 `KSelectable`，lambda 参数是自动投影类型。
- 派生查询字段只能访问它 select 出来的投影字段。
- join 条件要用的派生查询字段必须出现在派生查询 select 列表里。
- 不引入 `.asTable(...)`。
- 不要求用户命名 SQL 表 alias。
- SQL 表 alias 由 Kronos 自动生成。

## 场景 7：窗口函数与自动 SQL 分层

目标：支持 `ROW_NUMBER()`、聚合窗口、窗口 frame 等能力，并允许后续 `where` / `having` / `orderBy` 使用窗口投影字段。

窗口能力不设计成 `rowNumber` 的特例，而设计成通用表达式修饰器机制：

```kotlin
表达式.over(...).as_("name")
```

也就是说，`f.rowNumber()`、`f.sum(o.amount)`、`f.count(o.id)` 都先是表达式；`.over(...)` 只是给表达式追加窗口修饰。

### ROW_NUMBER

推荐语法：

```kotlin
val rows = Order()
    .select { o ->
        [
            o.id,
            o.userId,
            o.amount,
            o.createTime,
            f.rowNumber()
                .over(
                    partitionBy = [o.userId],
                    orderBy = [o.createTime.desc()]
                )
                .as_("rn")
        ]
    }
    .where {
        it.rn <= 2
    }
    .queryList()
```

语义：

- `f.rowNumber()` 是普通函数表达式。
- `.over(...)` 是表达式修饰器，不是 `rowNumber` 专属 API。
- `partitionBy = [...]` 表示窗口分区字段列表。
- `orderBy = [...]` 表示窗口排序字段列表。
- `.as_("rn")` 生成自动投影属性 `it.rn`。
- `select { ... }` 后的 `where { it.rn <= 2 }` 使用自动投影类型。
- 用户不需要声明 `val rn = alias<Int>("rn")`。
- 用户不需要手写外层查询。

期望 SQL：

```sql
SELECT q.id, q.user_id, q.amount, q.create_time, q.rn
FROM (
    SELECT
        o.id,
        o.user_id,
        o.amount,
        o.create_time,
        ROW_NUMBER() OVER (
            PARTITION BY o.user_id
            ORDER BY o.create_time DESC
        ) AS rn
    FROM orders o
) q
WHERE q.rn <= 2
```

如果当前数据库不能在同层 `WHERE` 引用窗口投影，Kronos 自动包一层派生查询；用户语法不变。

### 先过滤源表，再过滤窗口结果

如果要过滤源表字段，当前阶段需要把该字段放进投影：

```kotlin
val rows = Order()
    .select { o ->
        [
            o.id,
            o.userId,
            o.status,
            o.amount,
            o.createTime,
            f.rowNumber()
                .over(
                    partitionBy = [o.userId],
                    orderBy = [o.createTime.desc()]
                )
                .as_("rn")
        ]
    }
    .where {
        it.status == PAID &&
            it.rn <= 2
    }
    .queryList()
```

如果后续还要在自动投影上使用源字段，该字段必须被 select 出来：

```kotlin
val rows = Order()
    .select { o ->
        [
            o.id,
            o.userId,
            o.status,
            o.amount,
            f.rowNumber()
                .over(
                    partitionBy = [o.userId],
                    orderBy = [o.createTime.desc()]
                )
                .as_("rn")
        ]
    }
    .where {
        it.status == PAID &&
            it.rn <= 2
    }
    .queryList()
```

### 聚合窗口

聚合函数也通过同一套 `.over(...)` 机制窗口化：

```kotlin
val rows = Order()
    .select { o ->
        [
            o.id,
            o.userId,
            o.amount,
            f.sum(o.amount)
                .over(
                    partitionBy = [o.userId]
                )
                .as_("userTotal")
        ]
    }
    .where {
        it.userTotal > 1000
    }
    .queryList()
```

这里 `.over(...)` 不是 `sum` 特例，而是表达式修饰器。未来 `f.avg(...)`、`f.count(...)` 等聚合表达式也应复用同一机制。

### Window frame

带 frame 的窗口也使用 `.over(...)` 参数表达：

```kotlin
val rows = Order()
    .select { o ->
        [
            o.id,
            o.userId,
            o.amount,
            f.sum(o.amount)
                .over(
                    partitionBy = [o.userId],
                    orderBy = [o.createTime.asc()],
                    rows = between(2.preceding, currentRow)
                )
                .as_("rollingAmount")
        ]
    }
    .where {
        it.rollingAmount > 500
    }
    .queryList()
```

### ORDER BY 使用窗口投影

窗口投影字段可以继续用于 `orderBy`：

```kotlin
val rows = Order()
    .select { o ->
        [
            o.id,
            o.userId,
            f.rowNumber()
                .over(
                    partitionBy = [o.userId],
                    orderBy = [o.createTime.desc()]
                )
                .as_("rn")
        ]
    }
    .orderBy {
        it.rn.asc()
    }
    .queryList()
```

### 表达式修饰器机制

`.over(...)` 应作为通用表达式修饰器，而不是窗口函数专属特例。目标是让后续能力复用同一套表达式链：

```kotlin
f.count(o.id)
    .filter { it.status == PAID }
    .as_("paidCount")
```

```kotlin
f.sum(o.amount)
    .filter { it.status == PAID }
    .over(
        partitionBy = [o.userId]
    )
    .as_("paidUserTotal")
```

本场景只确认 `.over(...)` 语法；`.filter(...)`、`.withinGroup(...)` 等作为未来表达式修饰器方向，不在本场景展开。

### 当前确认

- 窗口函数通过表达式链表达：`f.xxx(...).over(...).as_("name")`。
- `.over(...)` 是通用表达式修饰器，不是 `rowNumber` 特例。
- `partitionBy = [...]` 使用字段列表。
- `orderBy = [...]` 使用排序表达式列表。
- `.as_("xxx")` 生成自动投影属性 `it.xxx`。
- 后续 `where` / `having` / `orderBy` 通过自动投影类型访问窗口结果。
- 过滤窗口结果时，Kronos 自动决定是否包外层派生查询。
- 不要求用户手写外层查询。
- 不要求用户声明 alias token。
- 如果后续要使用源字段，该字段必须在投影中出现，或者在 `select` 前完成过滤。
- 表达式修饰器机制后续可扩展到 `.filter(...)`、`.withinGroup(...)` 等能力。

## 场景 8：ORDER BY 中的子查询或表达式排序

目标：支持按自动投影字段、标量子查询、函数表达式排序，同时保持现有 `orderBy` DSL 风格。

排序项规则：

- 单个排序项可以直接返回。
- 多个排序项使用 `[]` 字面量。
- 不使用 `(a + b).asc()` 表示多字段排序；多字段排序写成 `[a.asc(), b.desc()]`。
- `+` 只表示真实表达式运算或函数表达式，不表示排序项组合。

### 使用自动投影字段排序

如果排序字段已经在 `select` 投影中，推荐直接使用自动投影字段：

```kotlin
val rows = User()
    .select { u ->
        [
            u.id,
            u.name,
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .as_("lastOrderAmount")
        ]
    }
    .orderBy {
        it.lastOrderAmount.desc()
    }
    .queryList()
```

语义：

- `.as_("lastOrderAmount")` 生成自动投影属性 `it.lastOrderAmount`。
- `select { ... }` 后的 `orderBy` 使用自动投影类型。
- 单个排序项可以省略 `[]`。

多个排序项使用 `[]`：

```kotlin
val rows = User()
    .select {
        [
            it.id,
            it.firstName,
            it.lastName
        ]
    }
    .orderBy {
        [
            it.lastName.asc(),
            it.firstName.asc()
        ]
    }
    .queryList()
```

### 标量子查询排序

标量子查询排序推荐先作为投影表达式命名，再通过自动投影字段排序：

```kotlin
val rows = User()
    .select { u ->
        [
            u.id,
            u.name,
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .as_("lastOrderAmount")
        ]
    }
    .orderBy {
        it.lastOrderAmount.desc()
    }
    .queryList()
```

语义：

- `Order().select { it.amount }...limit(1)` 是标量子查询投影。
- `.as_("lastOrderAmount")` 生成自动投影字段。
- `orderBy` 使用自动投影字段排序。
- 标量子查询排序仍然遵守单行规则；非聚合标量子查询必须 `.limit(1)`。
- 单个排序项可以省略 `[]`。

多个排序项可以混合普通字段、函数表达式和标量子查询投影：

```kotlin
val rows = User()
    .select { u ->
        [
            u.id,
            u.name,
            f.length(u.name).as_("nameLength"),
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .as_("lastOrderAmount")
        ]
    }
    .orderBy {
        [
            it.name.asc(),
            it.nameLength.desc(),
            it.lastOrderAmount.desc()
        ]
    }
    .queryList()
```

### 函数表达式排序

函数表达式也可以作为排序项：

```kotlin
val rows = User()
    .select {
        [
            it.id,
            it.name,
            f.length(it.name).as_("nameLength")
        ]
    }
    .orderBy {
        it.nameLength.desc()
    }
    .queryList()
```

多个函数或字段排序项使用 `[]`：

```kotlin
val rows = User()
    .select {
        [
            it.id,
            it.name,
            f.length(it.name).as_("nameLength")
        ]
    }
    .orderBy {
        [
            it.nameLength.desc(),
            it.id.asc()
        ]
    }
    .queryList()
```

### 窗口投影排序

窗口函数投影排序复用场景 7 的自动投影字段：

```kotlin
val rows = Order()
    .select { o ->
        [
            o.id,
            o.userId,
            f.rowNumber()
                .over(
                    partitionBy = [o.userId],
                    orderBy = [o.createTime.desc()]
                )
                .as_("rn")
        ]
    }
    .orderBy {
        it.rn.asc()
    }
    .queryList()
```

### 当前确认

- `orderBy { item }` 表示单个排序项。
- `orderBy { [item1, item2] }` 表示多个排序项。
- 单个排序项允许省略 `[]`。
- 多个排序项必须使用 `[]`。
- 多字段排序不使用 `(a + b).asc()`。
- `+` 只表示真实表达式运算或函数表达式。
- 带投影的 `select { ... }` 后的 `orderBy` 使用自动投影类型。
- 标量子查询可以作为排序项。
- 标量子查询排序必须满足单行规则；非聚合标量子查询必须 `.limit(1)`。
- 函数表达式可以作为排序项。
- 窗口投影排序通过自动投影字段完成。

## 场景 9：UPDATE SET 标量子查询

目标：在 `UPDATE SET` 中使用标量子查询给字段赋值，例如同步最近订单金额、统计订单数、写入最近登录时间。

本场景不引入 `setSubquery(...)` 等新 API。`set { field = KSelectable }` 直接表示 `SET field = (SELECT ...)`。

除类型安全的 `set { ... }` 外，`patch(...)` 也保留为动态字段入口。它适合字段名来自配置、接口参数、代码生成结果，或需要批量组装更新值的场景；当字段在源码中已知时，仍优先使用 `set { ... }`。

### 基础语法

```kotlin
User()
    .update()
    .set { u ->
        u.lastOrderAmount = Order()
            .select { it.amount }
            .where { it.userId == u.id }
            .orderBy { it.createTime.desc() }
            .limit(1)
    }
    .where { it.status == ACTIVE }
    .execute()
```

语义：

- `set` lambda 参数 `u` 是正在更新的 `User`。
- `Order().select { it.amount }...limit(1)` 是右侧标量子查询。
- 右侧子查询可以引用被更新表字段，例如 `u.id`。
- `where` 限制被更新的行。
- 非聚合右侧子查询必须 `.limit(1)`。

期望 SQL：

```sql
UPDATE user u
SET last_order_amount = (
    SELECT o.amount
    FROM orders o
    WHERE o.user_id = u.id
    ORDER BY o.create_time DESC
    LIMIT 1
)
WHERE u.status = ?
```

### 聚合标量子查询

聚合且无 `groupBy` 的子查询天然单行，不需要 `.limit(1)`：

```kotlin
UserStats()
    .update()
    .set { s ->
        s.orderCount = Order()
            .select { f.count(it.id) }
            .where { it.userId == s.userId }
    }
    .where { it.enabled == true }
    .execute()
```

期望 SQL：

```sql
UPDATE user_stats s
SET order_count = (
    SELECT COUNT(o.id)
    FROM orders o
    WHERE o.user_id = s.user_id
)
WHERE s.enabled = ?
```

### 多字段赋值

同一个 `set` block 中可以给多个字段赋值，其中每个右侧子查询独立遵守标量子查询规则：

```kotlin
UserStats()
    .update()
    .set { s ->
        s.orderCount = Order()
            .select { f.count(it.id) }
            .where { it.userId == s.userId }

        s.lastOrderAmount = Order()
            .select { it.amount }
            .where { it.userId == s.userId }
            .orderBy { it.createTime.desc() }
            .limit(1)
    }
    .where { it.enabled == true }
    .execute()
```

### 动态 patch 赋值

当更新字段需要动态构造时，可以使用 `patch(...)`。右侧值允许是普通值、`null`、函数表达式或标量子查询：

```kotlin
User()
    .update()
    .patch(
        "lastOrderAmount" to Order()
            .select { it.amount }
            .where { it.userId == userId }
            .orderBy { it.createTime.desc() }
            .limit(1),
        "updatedBy" to operatorId
    )
    .where { it.id == userId }
    .execute()
```

语义：

- `patch("field" to KSelectable)` 表示 `SET field = (SELECT ...)`。
- `patch` 使用字段名或字段引用承接动态更新，不提供 `set` lambda receiver。
- 右侧标量子查询仍必须只选择一列。
- 右侧非聚合标量子查询仍必须 `.limit(1)`。
- 如果需要在子查询中引用被更新行字段，优先使用 `set { u -> ... }`，因为它能给相关子查询提供明确的源对象。

### 可选类型提示

通常不需要显式类型提示；如果右侧表达式类型无法推断，可以使用 Kotlin cast：

```kotlin
User()
    .update()
    .set { u ->
        u.lastOrderAmount = (
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1) as BigDecimal
        )
    }
    .where { it.status == ACTIVE }
    .execute()
```

`limit(1) as T` 只是类型提示，不是必需语法，不改变 SQL，也不能绕过单列/单行校验。

### 不在本场景处理的 joined update

`UPDATE ... FROM`、`UPDATE ... JOIN` 方言差异较大，不作为本场景主语法。

如果能用相关标量子查询表达，优先使用：

```kotlin
User()
    .update()
    .set { u ->
        u.lastOrderAmount = Order()
            .select { it.amount }
            .where { it.userId == u.id }
            .orderBy { it.createTime.desc() }
            .limit(1)
    }
    .where { it.status == ACTIVE }
    .execute()
```

### 当前确认

- `set { field = KSelectable }` 表示 `SET field = (SELECT ...)`。
- 右侧子查询必须只选择一列。
- 右侧非聚合标量子查询必须 `.limit(1)`；唯一键证明后续增强，不作为第一版主规则。
- 聚合且无 `groupBy` 的子查询天然单行，不需要 `.limit(1)`。
- `set` lambda 参数是被更新的源 KPojo。
- 右侧子查询可以引用 `set` lambda 参数字段，表示相关更新。
- `patch(...)` 是保留的动态字段入口，也可以承接标量子查询；字段已知时推荐 `set { ... }`。
- `where` 用于限制被更新行。
- 类型提示使用 `limit(1) as T`，只作为类型提示。
- 不引入 `setSubquery(...)`。
- `UPDATE FROM` / joined update 不作为本场景主语法。

## 场景 10：UPDATE / DELETE WHERE 子查询

目标：在 `UPDATE` / `DELETE` 的 `where` 条件中使用子查询，例如批量禁用黑名单用户、删除无效会话、按统计条件更新状态。

本场景不新增 API，直接复用前面已经确认的条件表达式：

- `in` / `!in`
- `exists(...)` / `!exists(...)`
- 标量比较
- `any(...)` / `some(...)` / `all(...)`
- `[...] in KSelectable` 元组 IN

### UPDATE WHERE IN

```kotlin
User()
    .update()
    .set { u ->
        u.status = INACTIVE
    }
    .where { u ->
        u.id in UserBlacklist()
            .select { it.userId }
            .where { it.enabled == true }
    }
    .execute()
```

期望 SQL：

```sql
UPDATE user u
SET status = ?
WHERE u.id IN (
    SELECT b.user_id
    FROM user_blacklist b
    WHERE b.enabled = ?
)
```

### UPDATE WHERE EXISTS

```kotlin
User()
    .update()
    .set { u ->
        u.status = VIP
    }
    .where { u ->
        exists(
            Order()
                .select { it.id }
                .where { o ->
                    o.userId == u.id &&
                        o.amount > 1000
                }
        )
    }
    .execute()
```

期望 SQL：

```sql
UPDATE user u
SET status = ?
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.user_id = u.id
      AND o.amount > ?
)
```

### UPDATE WHERE 标量比较

```kotlin
Product()
    .update()
    .set { p ->
        p.hot = true
    }
    .where { p ->
        p.price > ProductPrice()
            .select { f.avg(it.price) }
            .where { it.categoryId == p.categoryId }
    }
    .execute()
```

这里右侧是聚合子查询，天然单行，不需要 `.limit(1)`。

非聚合标量子查询仍然必须满足单行规则：

```kotlin
Product()
    .update()
    .set { p ->
        p.hot = true
    }
    .where { p ->
        p.price > ProductPrice()
            .select { it.price }
            .where { it.productId == p.id }
            .orderBy { it.createdAt.desc() }
            .limit(1)
    }
    .execute()
```

### DELETE WHERE EXISTS

```kotlin
UserSession()
    .delete()
    .where { s ->
        exists(
            User()
                .select { it.id }
                .where { u ->
                    u.id == s.userId &&
                        u.status == INACTIVE
                }
        )
    }
    .execute()
```

期望 SQL：

```sql
DELETE FROM user_session s
WHERE EXISTS (
    SELECT 1
    FROM user u
    WHERE u.id = s.user_id
      AND u.status = ?
)
```

### DELETE WHERE NOT IN

```kotlin
OrderDraft()
    .delete()
    .where { d ->
        d.userId !in User()
            .select { it.id }
            .where { it.status == ACTIVE }
    }
    .execute()
```

### DELETE WHERE 元组 IN

```kotlin
OrderDraft()
    .delete()
    .where { d ->
        [d.userId, d.productId] in UserProductBlock()
            .select { b ->
                [
                    b.userId,
                    b.productId
                ]
            }
            .where { it.enabled == true }
    }
    .execute()
```

### 当前确认

- `update().where { ... }` 的 lambda 参数是被更新的源 KPojo。
- `delete().where { ... }` 的 lambda 参数是被删除的源 KPojo。
- 条件子查询可以引用该源 KPojo 字段，形成相关子查询。
- `IN` / `NOT IN` 复用 `in` / `!in`。
- `EXISTS` / `NOT EXISTS` 复用 `exists(...)` / `!exists(...)`。
- 标量比较复用 `field > KSelectable` 等比较表达式。
- `ANY` / `SOME` / `ALL` 复用 `any(...)` / `some(...)` / `all(...)`。
- 元组 IN 复用 `[...] in KSelectable`。
- 标量子查询仍然必须满足单行规则；非聚合标量子查询必须 `.limit(1)`。
- 不设计 `deleteJoin` / multi-table delete 作为主语法。
- 不设计 `updateFrom` / joined update 作为本场景主语法。

## 场景 11：INSERT SELECT

目标：把一个 query source 的结果插入到目标表，例如归档用户、生成快照表、把统计结果写入汇总表。

本场景使用 query source 消费式语法：

```kotlin
KSelectable.insert<Target> { ... }
```

不使用 `Target().insert { ... }.select(query)`，因为源查询通常需要先完整构造，再作为数据源插入目标表。

### 基础语法

```kotlin
User()
    .select { u ->
        [
            u.id,
            u.name,
            u.status,
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .as_("lastOrderAmount")
        ]
    }
    .where {
        it.status == ACTIVE
    }
    .insert<UserArchive> {
        [
            it.id,
            it.name,
            it.lastOrderAmount,
            null
        ]
    }
    .execute()
```

语义：

- `User().select { ... }.where { ... }` 是源 query。
- `insert<UserArchive>` 指定目标表。
- `insert` lambda 的 `it` 是源 query 的自动投影类型。
- lambda 返回插入值列表。
- 插入值按顺序写入 `UserArchive` 的可插入字段序列。
- `null` 是合法插入值。
- `status` 可以只用于过滤，不必出现在 `insert` 值列表中。

期望 SQL：

```sql
INSERT INTO user_archive (id, name, last_order_amount, archived_by)
SELECT
    q.id,
    q.name,
    q.lastOrderAmount,
    NULL
FROM (
    SELECT
        u.id,
        u.name,
        u.status,
        (
            SELECT o.amount
            FROM orders o
            WHERE o.user_id = u.id
            ORDER BY o.create_time DESC
            LIMIT 1
        ) AS lastOrderAmount
    FROM user u
) q
WHERE q.status = ?
```

实际是否包派生查询由 Kronos 根据投影过滤、方言能力和渲染需要决定；用户语法不变。

### 按顺序映射

`insert<Target> { [...] }` 按目标表可插入字段顺序映射，不按 alias 或字段名匹配。

```kotlin
User()
    .select { u ->
        [
            u.id,
            u.name,
            u.email
        ]
    }
    .insert<UserArchive> {
        [
            it.id,
            it.name,
            it.email
        ]
    }
    .execute()
```

如果目标表可插入字段顺序是：

```text
id, name, email
```

则上面的值按顺序写入：

```text
it.id    -> id
it.name  -> name
it.email -> email
```

`.as_("xxx")` 不作为目标字段映射规则。按顺序映射时，通常不需要为了 insert 目标字段重命名表达式。

### 目标字段序列与校验

`insert<Target> { [...] }` 的目标字段序列来自 Kronos 对 `Target` 的可插入字段规则，而不是源 query 的字段名或 alias。

第一版按以下规则校验：

- 忽略字段、非数据库字段、不参与 insert 的字段不进入目标字段序列。
- 数据库自增主键默认不进入目标字段序列，除非 Kronos 现有 insert 规则允许显式插入。
- 创建时间、更新时间、逻辑删除、乐观锁等策略字段是否进入序列，以 Kronos 现有 insertable 字段规则为准。
- 带数据库默认值的字段如果不进入目标字段序列，则由数据库或 Kronos 策略处理；如果进入目标字段序列，用户必须在 `[...]` 中提供对应值，可以显式写 `null`。
- `[...]` 的值数量必须与目标字段序列数量完全一致。
- 每个值的类型必须与同位置目标字段兼容。

这意味着顺序映射是强校验，不做“少给几个字段自动跳过默认值字段”的隐式匹配。

### 插入 null

`null` 可以作为插入值：

```kotlin
User()
    .select { u ->
        [
            u.id,
            u.name
        ]
    }
    .insert<UserArchive> {
        [
            it.id,
            it.name,
            null
        ]
    }
    .execute()
```

这表示第三个目标可插入字段写入 `NULL`。

### 插入函数表达式

插入值可以是源投影字段，也可以是函数表达式：

```kotlin
User()
    .select { u ->
        [
            u.id,
            u.name
        ]
    }
    .insert<UserArchive> {
        [
            it.id,
            it.name,
            f.now()
        ]
    }
    .execute()
```

### 插入聚合查询结果

聚合查询结果也可以作为源：

```kotlin
Order()
    .select { o ->
        [
            o.userId,
            f.count(o.id).as_("orderCount"),
            f.sum(o.amount).as_("totalAmount")
        ]
    }
    .groupBy { it.userId }
    .insert<UserOrderSummary> {
        [
            it.userId,
            it.orderCount,
            it.totalAmount
        ]
    }
    .execute()
```

### 插入 JOIN 查询结果

join 查询结果也可以作为源，只要它实现 `KSelectable`：

```kotlin
User().join(Order()) { user, order ->
    on { user.id == order.userId }

    select {
        [
            user.id,
            user.name,
            order.id.as_("orderId"),
            order.amount
        ]
    }
}
    .insert<UserOrderArchive> {
        [
            it.id,
            it.name,
            it.orderId,
            it.amount
        ]
    }
    .execute()
```

SQL 层面等价于：

```sql
INSERT INTO user_order_archive (...)
SELECT u.id, u.name, o.id, o.amount
FROM user u
JOIN orders o ON u.id = o.user_id
```

join 查询的最终投影就是 insert 的源投影。

### 当前确认

- `KSelectable.insert<Target> { ... }` 表示 `INSERT INTO Target SELECT ...`。
- `Target` 必须是 KPojo。
- `insert` lambda 的 `it` 是源 query 的自动投影类型。
- lambda 返回插入值列表。
- 插入值按目标表可插入字段顺序映射。
- 不按 alias 或字段名映射。
- 目标字段序列来自 Kronos 现有可插入字段规则。
- 忽略字段、非数据库字段、不参与 insert 的字段不进入目标字段序列。
- 策略字段、默认值字段是否进入序列，以 Kronos 现有 insertable 字段规则为准。
- `null` 可以作为插入值。
- 插入值可以是源投影字段、常量、`null`、函数表达式、标量子查询表达式。
- 插入值数量必须与目标表可插入字段数量一致。
- 插入值类型必须按顺序与目标字段兼容。
- 源 query 可以保留用于 `where` / `orderBy` 的字段，不必全部插入。
- 普通 select、join select、union 等只要实现 `KSelectable`，都可以作为 insert source。
- 不引入 `insertSelect(...)`。
- 暂不设计显式目标字段列表语法；如果字段顺序风险需要收敛，后续单独讨论。

## 场景 12：UPSERT 中的子查询表达式

目标：在 upsert 冲突更新阶段使用子查询表达式，例如更新统计字段、最近订单金额、最近登录时间。

本场景不引入 upsert 专属的 `setSubquery(...)`。`upsert().set { field = KSelectable }` 直接表示冲突更新时使用标量子查询赋值。

`upsert().patch(...)` 同样保留为动态字段入口，用于运行期组装冲突更新字段；字段在源码中已知时，推荐使用类型安全的 `set { ... }`。

### 基础语法

```kotlin
UserStats(userId = userId)
    .upsert()
    .on { it.userId }
    .set { s ->
        s.orderCount = Order()
            .select { f.count(it.id) }
            .where { it.userId == s.userId }

        s.lastOrderAmount = Order()
            .select { it.amount }
            .where { it.userId == s.userId }
            .orderBy { it.createTime.desc() }
            .limit(1)
    }
    .execute()
```

语义：

- `on { it.userId }` 表示冲突键。
- `set { ... }` 表示冲突后更新哪些字段。
- `set` 右侧可以是标量子查询。
- `set` lambda 参数 `s` 是目标 KPojo。
- 右侧子查询可以引用目标 KPojo 字段，例如 `s.userId`。
- 聚合且无 `groupBy` 的子查询天然单行，不需要 `.limit(1)`。
- 非聚合标量子查询必须 `.limit(1)`。

SQL 形态由方言决定：

```sql
-- PostgreSQL
INSERT INTO user_stats (...)
VALUES (...)
ON CONFLICT (user_id)
DO UPDATE SET
    order_count = (
        SELECT COUNT(o.id)
        FROM orders o
        WHERE o.user_id = user_stats.user_id
    ),
    last_order_amount = (
        SELECT o.amount
        FROM orders o
        WHERE o.user_id = user_stats.user_id
        ORDER BY o.create_time DESC
        LIMIT 1
    )
```

```sql
-- MySQL
INSERT INTO user_stats (...)
VALUES (...)
ON DUPLICATE KEY UPDATE
    order_count = (
        SELECT COUNT(o.id)
        FROM orders o
        WHERE o.user_id = user_stats.user_id
    ),
    last_order_amount = (
        SELECT o.amount
        FROM orders o
        WHERE o.user_id = user_stats.user_id
        ORDER BY o.create_time DESC
        LIMIT 1
    )
```

用户 DSL 不暴露这些方言差异。

### 插入值与冲突更新值不同

插入初始值和冲突更新值可以不同：

```kotlin
UserStats(
    userId = userId,
    orderCount = 0,
    lastOrderAmount = null
)
    .upsert()
    .on { it.userId }
    .set { s ->
        s.orderCount = Order()
            .select { f.count(it.id) }
            .where { it.userId == s.userId }

        s.lastOrderAmount = Order()
            .select { it.amount }
            .where { it.userId == s.userId }
            .orderBy { it.createTime.desc() }
            .limit(1)
    }
    .execute()
```

这里对象属性提供插入时的初始值，`set` block 提供冲突更新时的值。

### 动态 patch 冲突更新

当冲突更新字段来自动态配置或需要批量组装时，可以使用 `patch(...)`：

```kotlin
UserStats(userId = userId)
    .upsert()
    .on { it.userId }
    .patch(
        "orderCount" to Order()
            .select { f.count(it.id) }
            .where { it.userId == userId },
        "lastOrderAmount" to Order()
            .select { it.amount }
            .where { it.userId == userId }
            .orderBy { it.createTime.desc() }
            .limit(1)
    )
    .onConflict()
    .execute()
```

语义：

- `upsert().patch("field" to KSelectable)` 表示冲突更新阶段的 `field = (SELECT ...)`。
- `patch` 的普通值进入对应字段的冲突更新值；`KSelectable` / 表达式值进入 SQL 表达式赋值。
- 右侧标量子查询仍遵守单列/单行规则。
- 需要相关子查询引用目标 KPojo 字段时，优先使用 `set { s -> ... }`。

### 可选类型提示

与其他标量子查询场景一致，可以使用 Kotlin cast 作为可选类型提示：

```kotlin
UserStats(userId = userId)
    .upsert()
    .on { it.userId }
    .set { s ->
        s.lastOrderAmount = (
            Order()
                .select { it.amount }
                .where { it.userId == s.userId }
                .orderBy { it.createTime.desc() }
                .limit(1) as BigDecimal
        )
    }
    .execute()
```

### 当前确认

- `upsert().set { field = KSelectable }` 表示冲突更新时使用标量子查询赋值。
- 右侧子查询必须只选择一列。
- 右侧非聚合标量子查询必须 `.limit(1)`；唯一键证明后续增强，不作为第一版主规则。
- 聚合且无 `groupBy` 的子查询天然单行，不需要 `.limit(1)`。
- `set` lambda 参数是目标 KPojo。
- 子查询可以引用目标 KPojo 字段，表示相关子查询。
- `patch(...)` 是保留的动态字段入口，也可以承接冲突更新阶段的标量子查询；字段已知时推荐 `set { ... }`。
- `on { ... }` 只表示冲突键，不承载子查询条件。
- 类型提示使用 `limit(1) as T`，只作为类型提示。
- 方言差异由 Kronos 处理，DSL 不暴露。
- 不引入 upsert 专属的 `setSubquery(...)`。

## 场景 13：CREATE TABLE AS SELECT

目标：基于一个 `KSelectable` 查询结果创建目标表，即 `CREATE TABLE ... AS SELECT ...`。

本场景不新增 `createTableAs(...)`，也不提供 `createView(...)`。它扩展现有 DDL 入口：

```kotlin
dataSource.table.createTable(target)
dataSource.table.createTable(target, query)
```

其中：

- `createTable(target)` 保持现有语义：按 KPojo 创建表。
- `createTable(target, query)` 表示基于 query 执行 CTAS。

### 基础语法

```kotlin
val activeUsers = User()
    .select { u ->
        [
            u.id,
            u.name,
            u.status
        ]
    }
    .where {
        it.status == ACTIVE
    }

dataSource.table.createTable(
    UserArchive(),
    activeUsers
)
```

语义：

- `UserArchive()` 是目标表 KPojo。
- `activeUsers` 是源 `KSelectable`。
- 目标表名来自 `UserArchive()`。
- 源 query 的最终投影提供 CTAS 的 `SELECT` 数据来源。
- 不需要用户写 SQL 表 alias。

期望 SQL：

```sql
CREATE TABLE user_archive AS
SELECT q.id, q.name, q.status
FROM (
    SELECT u.id, u.name, u.status
    FROM user u
) q
WHERE q.status = ?
```

实际是否需要包派生查询由 Kronos 根据投影过滤和方言能力决定。

### 复杂查询源

源 query 可以使用前面所有 select/subquery 能力：

```kotlin
val userOrderSnapshot = User()
    .select { u ->
        [
            u.id,
            u.name,
            u.status,
            Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1)
                .as_("lastOrderAmount")
        ]
    }
    .where {
        it.status == ACTIVE
    }

dataSource.table.createTable(
    UserOrderSnapshot(),
    userOrderSnapshot
)
```

### JOIN 查询源

join 查询也可以作为 CTAS 源：

```kotlin
val userOrders = User().join(Order()) { user, order ->
    on { user.id == order.userId }

    select {
        [
            user.id,
            user.name,
            order.id.as_("orderId"),
            order.amount
        ]
    }
}

dataSource.table.createTable(
    UserOrderArchive(),
    userOrders
)
```

因为 join 查询实现 `KSelectable`，所以可以直接作为 `createTable(target, query)` 的第二个参数。

### 与普通建表的边界

普通建表保持原 API：

```kotlin
dataSource.table.createTable(UserArchive())
```

CTAS 使用新增重载：

```kotlin
dataSource.table.createTable(UserArchive(), query)
```

注意：不同数据库对 `CREATE TABLE AS SELECT` 是否保留字段类型、主键、索引、默认值等 schema 信息支持不同。若需要完整保留 KPojo 定义的表结构，推荐分两步：

```kotlin
dataSource.table.createTable(UserArchive())

query
    .insert<UserArchive> {
        [
            it.id,
            it.name,
            it.lastOrderAmount
        ]
    }
    .execute()
```

### 当前确认

- 不提供 `createView(...)`。
- 不新增 `createTableAs(...)`。
- 保留现有 `createTable(KPojo)`。
- 新增 `createTable(KPojo, KSelectable)` 表示 CTAS。
- 第一个参数是目标表 KPojo。
- 第二个参数是源 query。
- 普通 select、join select、union 等只要实现 `KSelectable`，都可以作为 CTAS 源。
- 目标表名来自目标 KPojo。
- 源 query 的最终投影提供 `SELECT` 数据来源。
- CTAS 是否保留完整 KPojo schema 取决于方言能力。
- 如需完整 schema，使用 `createTable(KPojo)` + `KSelectable.insert<Target> { ... }`。
