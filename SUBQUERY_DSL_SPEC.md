# Kronos 子查询 DSL 提案

## 1. 目标与范围

本文定义 Kronos 子查询与查询作为数据源的用户 DSL、SQL 语义和边界规则。

覆盖范围：

- 标量子查询；
- `IN` / `NOT IN` / `EXISTS` / `NOT EXISTS`；
- `ANY` / `SOME` / `ALL`；
- row-value tuple `IN`；
- `KSelectable` 作为下一层 `Source`；
- 窗口函数结果过滤；
- UPDATE / DELETE / UPSERT 中的子查询；
- INSERT SELECT；
- CREATE TABLE AS SELECT。

## 2. 核心模型

### 2.1 查询层

| 概念 | 含义 |
|------|------|
| `Source` | 当前查询层 FROM / JOIN 暴露给 DSL 的输入类型。 |
| `Selected` | 当前查询层 `select { ... }` 查出来的结果类型。 |
| `Context` | `Source` 和 `Selected` 合并后的上下文。 |
| `KSelectable<Selected>` | 查询对象。被下一层查询消费时，`Selected` 成为下一层 `Source`。 |

`select()` 不指定投影时，当前层 `Selected = Source`。

`select { ... }` 指定投影时，当前层 `Selected` 由投影项生成。

KPojo 可以直接接查询子句。`User().where { it.id > 1 }` 等价于 `User().select().where { it.id > 1 }`。

这种写法是 KPojo 查询入口的语法糖，结果仍然是 `KSelectable<Source>`。

`queryList()`、`queryOne()`、`queryOneOrNull()` 返回当前层 `Selected`。

```kotlin
User()
    .where { it.id > 1 }
    .queryOne()
```

新增 `Selected` 字段与 `Source` 字段同名时，`Context` 生成失败，报编译期错误。

原 `Source` 字段按原名投影时，不产生 `Context` 冲突。

```kotlin
val q = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
```

在这个查询层中：

- `Source` 来自 `User`；
- `Selected` 包含 `id` 和 `nameLength`；
- `Context` 可以访问 `Source` 字段和 `Selected.nameLength`；
- `q.select { ... }` 进入下一层后，下一层 `Source` 包含 `id` 和 `nameLength`。

### 2.2 SQL 语义阶段

```text
FROM / JOIN
WHERE
GROUP BY
HAVING
SELECT
DISTINCT
ORDER BY
LIMIT / OFFSET
```

Kronos 的 receiver 设计按这个顺序约束字段可见性。

窗口函数只能出现在当前层 `select` 或 `orderBy` 表达式中。窗口函数结果进入当前层 `Selected`，因此同层 `where`、`groupBy`、`having` 不能访问窗口函数结果。

## 3. DSL 通用规则

### 3.1 同层查询 receiver

| 位置 | receiver |
|------|----------|
| `select { ... }` | `Source` |
| `where { ... }` | `Source` |
| `groupBy { ... }` | `Source` |
| `having { ... }` | `Source` |
| `orderBy { ... }` | `Context` |
| `KPojo.where { ... }` | 等价于 `KPojo.select().where { ... }` |

### 3.2 跨层与写入 receiver

| 位置 | receiver |
|------|----------|
| `KSelectable<S>.select { ... }` | `S` |
| `join(KSelectable) { left, right -> ... }` | `left` 是左侧 `Source`，`right` 是右侧 `Selected` |
| `insert<Target> { ... }` | 源 query 的 `Selected` |
| `update().set { ... }` | 被更新表的 `Source` |
| `update().where { ... }` / `delete().where { ... }` | 被更新表或删除表的 `Source` |

### 3.3 字段命名

`.alias("name")` 命名当前层 `Selected` 字段。

`select { ... }` 中，直接字段投影可以继承字段名；非直接字段投影必须显式 `.alias("name")`。

| 投影项 | alias 要求 |
|------|------------|
| `it.id` | 可省略 |
| `f.length(it.name)` | 必须写 |
| `f.sum(it.amount)` | 必须写 |
| 标量子查询 | 作为 `select` 投影项时必须写 |
| 窗口函数 | 必须写 |

只要表达式进入 `select { ... }` 的投影列表，非直接字段投影就必须 alias；消费方是否使用字段名不影响这条规则。

同一层 `Selected` 的最终字段名必须唯一。

这是对原 `.as_("name")` 的破坏性改名。原 API 不保留兼容，使用 `.as_("name")` 应报编译期错误或不可解析。

DSL：

```kotlin
User().select { [it.id, f.length(it.name).alias("nameLength")] }
```

语义：当前层 `Selected` 包含 `id` 和 `nameLength`。

等价 SQL：

```sql
SELECT u.id, LENGTH(u.name) AS nameLength
FROM user u
```

### 3.4 列表语法

`[]` 按所在位置解释。

| 位置 | 含义 |
|------|------|
| `select { [a, b] }` | 投影列表 |
| `orderBy { [a.asc(), b.desc()] }` | 排序列表 |
| `over(partitionBy = [a], orderBy = [b.asc()])` | 窗口字段列表 |
| `[a, b] in query` | row-value tuple |

### 3.5 SQL 示例 alias

SQL 示例中的 `u`、`o`、`q` 表示 Kronos 内部生成的 SQL 表标识。

## 4. 子查询形式

### 4.1 子查询形态总表

| 形态 | 典型位置 | 右侧查询要求 | 结果语义 |
|------|----------|--------------|----------|
| 表查询 | `KSelectable.select { ... }`、`join(query)` | 任意投影 | `Selected` 成为下一层 `Source` |
| 标量子查询 | `select` 表达式、`where` 比较、`update set` | 一列，显式 `.limit(1)` | 单个表达式值 |
| 谓词子查询 | `where`、`having`、`update where`、`delete where` | 按谓词决定 | SQL boolean 条件 |
| row-value tuple `IN` | `where`、`having` | 右侧列数等于左侧 tuple 元数 | SQL tuple predicate |
| 查询结果消费者 | `insert<Target>`、CTAS | 按目标能力决定 | 消费当前层 `Selected` |

相关子查询可以引用外层 lambda receiver。

```kotlin
User()
    .select { u ->
        [
            u.id,
            Order().select { it.amount }.where { it.userId == u.id }.limit(1).alias("amount")
        ]
    }
    .queryList()
```

上例中，内层 `where { ... }` 的 `it` 是 `Order` 的 `Source`，`u` 是外层 `User` 的 `Source`。

### 4.2 查询作为 Source

`KSelectable<S>.select { ... }` 会进入下一层查询。

下一层 `Source = S`。

DSL：

```kotlin
val userNameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

userNameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .queryList()
```

SQL 语义：先生成派生表 `q`，再在外层过滤 `q.nameLength`。

等价 SQL：

```sql
SELECT q.id, q.nameLength
FROM (
    SELECT u.id, LENGTH(u.name) AS nameLength
    FROM user u
) q
WHERE q.nameLength > ?
```

参与 join：

```kotlin
val orderAmounts = Order()
    .select { [it.userId, it.amount.alias("orderAmount")] }

User()
    .join(orderAmounts) { user, order ->
        on { user.id == order.userId }
        select { [user.id, user.name, order.orderAmount] }
    }
    .queryList()
```

等价 SQL：

```sql
SELECT u.id, u.name, q.orderAmount
FROM user u
JOIN (
    SELECT o.user_id, o.amount AS orderAmount
    FROM orders o
) q ON u.id = q.user_id
```

规则：

- `KSelectable` 被消费后，投影字段成为下一层 `Source` 字段；
- 过滤当前层 `Selected` 字段时，进入下一层查询；
- 是否使用中间 `val` 只影响 Kotlin 代码组织，不影响 SQL 语义。

### 4.3 标量子查询

标量子查询返回一个表达式值。

规则：

- 只能返回一列；
- 必须显式 `.limit(1)`；
- 默认 Kotlin 类型来自唯一投影项；
- 需要类型提示时，使用 `query.limit(1) as T`；
- `as T` 是编译器识别的 DSL 类型提示，不改变 SQL；
- `as T` 写在 `.limit(1)` 后；
- `.alias("name")` 命名外层 `Selected` 字段。

放在 `select` 中：

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
    .queryList()
```

SQL 语义：为每个用户查询最近一笔订单金额。

等价 SQL：

```sql
SELECT u.id, (
    SELECT o.amount
    FROM orders o
    WHERE o.user_id = u.id
    ORDER BY o.create_time DESC
    LIMIT 1
) AS lastAmount
FROM user u
```

放在 `where` 比较中：

```kotlin
Product()
    .select()
    .where { p ->
        p.price > ProductPrice()
            .select { f.avg(it.price) }
            .where { it.categoryId == p.categoryId }
            .limit(1)
    }
    .queryList()
```

SQL 语义：查询价格高于同分类平均价格的商品。

等价 SQL：

```sql
SELECT *
FROM product p
WHERE p.price > (
    SELECT AVG(pp.price)
    FROM product_price pp
    WHERE pp.category_id = p.category_id
    LIMIT 1
)
```

需要类型提示：

```kotlin
User()
    .select { u ->
        [
            u.id,
            (Order()
                .select { it.amount }
                .where { it.userId == u.id }
                .orderBy { it.createTime.desc() }
                .limit(1) as BigDecimal)
                .alias("lastAmount")
        ]
    }
    .queryList()
```

SQL 与第一个 `select` 示例相同。

### 4.4 谓词子查询

`IN` / `NOT IN`：

```kotlin
User()
    .select()
    .where { it.id in Order().select { it.userId } }
    .queryList()
```

SQL 语义：查询有订单的用户。

等价 SQL：

```sql
SELECT *
FROM user u
WHERE u.id IN (
    SELECT o.user_id
    FROM orders o
)
```

```kotlin
User()
    .select()
    .where { it.id !in UserBlacklist().select { it.userId } }
    .queryList()
```

等价 SQL：

```sql
SELECT *
FROM user u
WHERE u.id NOT IN (
    SELECT b.user_id
    FROM user_blacklist b
)
```

`EXISTS`：

```kotlin
User()
    .select()
    .where { u ->
        exists(Order().select().where { it.userId == u.id })
    }
    .queryList()
```

SQL 语义：查询存在订单的用户。

等价 SQL：

```sql
SELECT *
FROM user u
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.user_id = u.id
)
```

```kotlin
User()
    .select()
    .where { u ->
        !exists(Order().select().where { it.userId == u.id })
    }
    .queryList()
```

等价 SQL：

```sql
SELECT *
FROM user u
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.user_id = u.id
)
```

`ANY` / `SOME` / `ALL`：

```kotlin
Product()
    .select()
    .where { p ->
        p.price > any(ProductPrice()
            .select { it.price }
            .where { it.categoryId == p.categoryId })
    }
    .queryList()
```

SQL 语义：查询价格大于同分类任意一个历史价格的商品。

等价 SQL：

```sql
SELECT *
FROM product p
WHERE p.price > ANY (
    SELECT pp.price
    FROM product_price pp
    WHERE pp.category_id = p.category_id
)
```

```kotlin
Product()
    .select()
    .where { p ->
        p.price > all(ProductPrice()
            .select { it.price }
            .where { it.categoryId == p.categoryId })
    }
    .queryList()
```

等价 SQL：

```sql
SELECT *
FROM product p
WHERE p.price > ALL (
    SELECT pp.price
    FROM product_price pp
    WHERE pp.category_id = p.category_id
)
```

规则：

- 普通单值 `IN`、`NOT IN`、`ANY`、`SOME`、`ALL` 右侧子查询必须返回一列；
- `EXISTS` 忽略右侧子查询的 select list；
- `some(query)` 与 `any(query)` 同义；
- `IN`、`NOT IN`、`ANY`、`SOME`、`ALL` 保留 SQL 原生 NULL 语义。

### 4.5 Row-value tuple IN

DSL：

```kotlin
Order()
    .select()
    .where {
        [it.userId, it.createTime] in OrderArchive()
            .select { [it.userId, f.max(it.createTime).alias("maxCreateTime")] }
            .groupBy { it.userId }
    }
    .queryList()
```

SQL 语义：查询每个用户在归档表中的最新订单记录。

等价 SQL：

```sql
SELECT *
FROM orders o
WHERE (o.user_id, o.create_time) IN (
    SELECT a.user_id, MAX(a.create_time) AS maxCreateTime
    FROM order_archive a
    GROUP BY a.user_id
)
```

规则：

- 多列 tuple 使用 `[a, b] in query` 或 `[a, b] !in query`；
- 单列使用 `a in query` 或 `a !in query`；
- 左右两侧列数必须一致；
- NULL 处理保留 SQL 原生三值逻辑。

## 5. 子句规则

### 5.1 可见性总表

| 位置 | 可见字段 | 主要规则 |
|------|----------|----------|
| `where { ... }` | 当前层 `Source` | 过滤输入行 |
| `groupBy { ... }` | 当前层 `Source` | 生成 SQL `GROUP BY` |
| `having { ... }` | 当前层 `Source` | 生成 SQL `HAVING` |
| `orderBy { ... }` | 当前层 `Context` | 可以使用当前层 `Selected` 字段排序 |
| 下一层 `KSelectable<S>.select { ... }` | `S` | 上一层 `Selected` 成为本层 `Source` |

### 5.2 where 使用 Source

`where { ... }` 的 receiver 是 `Source`。

过滤 `Selected` 字段时，写成下一层查询。

```kotlin
val ranked = Order()
    .select {
        [
            it.id,
            it.userId,
            it.amount,
            f.rowNumber()
                .over(partitionBy = [it.userId], orderBy = [it.createTime.desc()])
                .alias("rn")
        ]
    }

ranked
    .select { [it.id, it.userId, it.amount] }
    .where { it.rn == 1 }
    .queryList()
```

等价 SQL：

```sql
SELECT q.id, q.user_id, q.amount
FROM (
    SELECT
        o.id,
        o.user_id,
        o.amount,
        ROW_NUMBER() OVER (
            PARTITION BY o.user_id
            ORDER BY o.create_time DESC
        ) AS rn
    FROM orders o
) q
WHERE q.rn = ?
```

### 5.3 having 使用 Source

`having { ... }` 的 receiver 是 `Source`。

DSL：

```kotlin
Order()
    .select { [it.userId, f.sum(it.amount).alias("totalAmount")] }
    .groupBy { it.userId }
    .having {
        f.sum(it.amount) in VipLevel()
            .select { it.minAmount }
            .where { it.enabled == true }
    }
    .queryList()
```

SQL 语义：查询订单总额进入有效 VIP 档位的用户。

等价 SQL：

```sql
SELECT o.user_id, SUM(o.amount) AS totalAmount
FROM orders o
GROUP BY o.user_id
HAVING SUM(o.amount) IN (
    SELECT v.min_amount
    FROM vip_level v
    WHERE v.enabled = ?
)
```

规则：

- 直接访问字段时，字段必须符合 SQL `HAVING` 对当前查询层的约束；
- 当前层 `Selected` 字段不进入同层 `having`；
- 需要按当前层 `Selected` 字段过滤时，进入下一层查询。

### 5.4 orderBy 使用 Context

`orderBy { ... }` 的 receiver 是 `Context`。

DSL：

```kotlin
User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .orderBy { it.nameLength.desc() }
    .queryList()
```

SQL 语义：按当前层 `Selected.nameLength` 排序。

等价 SQL：

```sql
SELECT u.id, LENGTH(u.name) AS nameLength
FROM user u
ORDER BY nameLength DESC
```

规则：

- `orderBy` 可以访问当前层 `Source` 字段；
- `orderBy` 可以访问当前层 `Selected` 字段；
- `SELECT DISTINCT` 时，`orderBy` 的字段必须满足对应 SQL 方言约束；
- `GROUP BY` 后排序字段必须满足 SQL 对当前查询层的约束。

## 6. DML 中的子查询

### 6.1 UPDATE SET 标量子查询

DSL：

```kotlin
UserStats()
    .update()
    .set { s ->
        s.lastAmount = Order()
            .select { it.amount }
            .where { it.userId == s.userId }
            .orderBy { it.createTime.desc() }
            .limit(1)
    }
    .where { it.enabled == true }
    .execute()
```

SQL 语义：用子查询结果更新统计表字段。

示意 SQL：

```sql
UPDATE user_stats s
SET last_amount = (
    SELECT o.amount
    FROM orders o
    WHERE o.user_id = s.user_id
    ORDER BY o.create_time DESC
    LIMIT 1
)
WHERE s.enabled = ?
```

### 6.2 UPDATE WHERE 子查询

DSL：

```kotlin
User()
    .update()
    .set { it.status = INACTIVE }
    .where { u -> u.id in UserBlacklist().select { it.userId } }
    .execute()
```

SQL 语义：按子查询结果更新用户状态。

示意 SQL：

```sql
UPDATE user u
SET status = ?
WHERE u.id IN (
    SELECT b.user_id
    FROM user_blacklist b
)
```

### 6.3 DELETE WHERE 子查询

DSL：

```kotlin
UserSession()
    .delete()
    .where { s ->
        exists(User().select().where { it.id == s.userId && it.status == INACTIVE })
    }
    .execute()
```

SQL 语义：删除属于非活跃用户的会话。

示意 SQL：

```sql
DELETE FROM user_session s
WHERE EXISTS (
    SELECT 1
    FROM user u
    WHERE u.id = s.user_id
      AND u.status = ?
)
```

### 6.4 UPSERT SET 标量子查询

DSL：

```kotlin
UserStats(userId = userId)
    .upsert()
    .on { it.userId }
    .set { s ->
        s.lastAmount = Order()
            .select { it.amount }
            .where { it.userId == s.userId }
            .orderBy { it.createTime.desc() }
            .limit(1)
    }
    .execute()
```

SQL 语义：冲突更新阶段使用标量子查询设置字段。

PostgreSQL 示意 SQL：

```sql
INSERT INTO user_stats (...)
VALUES (...)
ON CONFLICT (user_id)
DO UPDATE SET last_amount = (
    SELECT o.amount
    FROM orders o
    WHERE o.user_id = user_stats.user_id
    ORDER BY o.create_time DESC
    LIMIT 1
)
```

规则：`set { s -> ... }` 中的 `s` 表示冲突更新的目标行。引用 incoming / excluded 值的 DSL 在本文中不定义。

## 7. 查询结果消费者

### 7.1 INSERT SELECT

DSL：

```kotlin
Order()
    .select { [it.userId, f.count(it.id).alias("orderCount")] }
    .groupBy { it.userId }
    .insert<UserOrderSummary> { [it.userId, it.orderCount] }
    .execute()
```

SQL 语义：把查询结果插入目标表。

等价 SQL：

```sql
INSERT INTO user_order_summary (user_id, order_count)
SELECT o.user_id, COUNT(o.id) AS orderCount
FROM orders o
GROUP BY o.user_id
```

规则：

- `insert<Target> { [...] }` 的 receiver 是源 query 的 `Selected`；
- lambda 返回的表达式序列按目标表可插入字段顺序映射；
- 本例假设 `UserOrderSummary` 的可插入字段顺序为 `userId, orderCount`。

### 7.2 CREATE TABLE AS SELECT

DSL：

```kotlin
val activeUsers = User()
    .select { [it.id, it.name, it.status] }
    .where { it.status == ACTIVE }

dataSource.table.createTable(UserArchive(), activeUsers)
```

SQL 语义：使用 CTAS 重载，用查询结果创建表。

等价 SQL：

```sql
CREATE TABLE user_archive AS
SELECT u.id, u.name, u.status
FROM user u
WHERE u.status = ?
```

规则：

- `UserArchive()` 提供目标表名；
- CTAS 的列信息由数据库根据查询结果推断；
- `UserArchive` 的字段、索引、默认值和主键定义不参与 CTAS 列定义；
- 带参数的 CTAS 按方言支持处理；
- 需要完整 KPojo schema 时，使用普通建表加 INSERT SELECT。

## 8. 诊断规则

### 8.1 静态规则

| 场景 | 诊断 |
|------|------|
| 标量子查询返回多列 | 编译期错误 |
| 标量子查询缺少 `.limit(1)` | 编译期错误 |
| 普通单值 `IN` / `ANY` / `SOME` / `ALL` 右侧返回多列 | 编译期错误 |
| row-value tuple 左右列数不一致 | 编译期错误 |
| 同层 `where` 引用当前层 `Selected` 字段 | 编译期错误 |
| `select { ... }` 中非直接字段投影缺少 `.alias("name")` | 编译期错误 |
| 同一层 `Selected` 的最终字段名重复 | 编译期错误 |
| 使用 `.as_("name")` | 编译期错误或不可解析 |
| `insert<Target>` 字段数量可静态判断且不匹配 | 编译期错误 |
| DML 目标表字段和子查询字段类型可静态判断且不兼容 | 编译期错误 |

### 8.2 生成期 / 运行期规则

| 场景 | 诊断 |
|------|------|
| 方言不支持当前 SQL 能力 | 生成阶段报错 |
| DML 字段类型无法静态判断且运行期不兼容 | 运行期错误 |
| CTAS 参数化不被当前方言支持 | 生成阶段报错 |

## 9. 方言边界

| 能力 | MySQL | PostgreSQL | SQLite | Oracle | SQL Server |
|------|-------|------------|--------|--------|------------|
| `ANY` / `SOME` / `ALL` | 方言实现 | 方言实现 | 生成阶段报错 | 方言实现 | 方言实现 |
| row-value tuple `IN` | 方言实现 | 方言实现 | 方言实现或报错 | 方言实现 | 生成阶段报错或改写 |
| UPDATE / DELETE 目标表 alias | 方言生成 | 方言生成 | 方言生成 | 方言生成 | 方言生成 |
| UPSERT 中子查询 | 方言生成 | 方言生成 | 方言生成 | 方言生成 | 方言生成 |
| `ORDER BY` / `LIMIT` 标量子查询 | 方言生成 | 方言生成 | 方言生成 | 方言改写 | 方言改写 |
| 参数化 CTAS | 方言支持时生成 | 方言支持时生成 | 方言支持时生成 | 方言支持时生成 | 方言支持时生成 |
