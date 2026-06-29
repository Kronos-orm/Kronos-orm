# kronos-core Projection and Subquery Adaptation Plan

状态：设计清单

本文档记录新版 select 投影、生成上下文类型、子查询 DSL 落地时，`kronos-core` 需要配合修改的 API、运行时结构和 SQL AST 能力。

## 背景

新版语法里，一个 `select` 需要区分两个类型概念：

- **Result Projection**：查询最终返回类型，只包含 `select { ... }` 真正返回的字段。
- **Clause Context**：后续 `where`、`having`、`orderBy` 等子句使用的上下文类型，包含原 DTO 全部列 + `select { ... }` 新增投影列。

示例：

```kotlin
val rows = User()
    .select {
        [
            it.id,
            it.name,
            Order()
                .select { it.amount }
                .where { it.userId == outer.id }
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

这里：

- `queryList()` 返回 `List<ResultProjection>`，字段为 `id`、`name`、`lastOrderAmount`。
- `where { ... }` 的 `it` 是 `ClauseContext`，字段为 `User` 全部列 + `lastOrderAmount`。

## 当前 core 现状

当前 `SelectClause<T, R>` 已经有结果投影类型 `R`：

```kotlin
class SelectClause<T : KPojo, R : KPojo>(
    override val pojo: T,
    setSelectFields: ToSelect<T, Any?> = null,
    private val projectionClass: KClass<R>
) : KSelectable<T>(pojo)
```

并且已有编译器插件可调用的内部入口：

```kotlin
internal fun <T : KPojo, R : KPojo> T.selectGeneratedProjection(
    projectionClass: KClass<R>,
    fields: ToSelect<T, Any?> = null
): SelectClause<T, R>
```

但 core 仍有几个限制：

- `where`、`having`、`orderBy`、`groupBy` 的 lambda 类型仍然基于源 `T`。
- `KSelectable` 只有一个类型参数，不能表达“源类型、结果类型、上下文类型”。
- `selectFields` 只能表达 `Field` 集合，不足以表达标量子查询、函数、窗口函数、tuple、派生查询等表达式。
- `toStatement()` 只能产出单层 `SelectStatement`，还缺少“因为 where/orderBy 引用了 select alias 而自动包外层派生查询”的元数据和渲染策略。

## 目标类型结构

直接把现有 `SelectClause` 改成三泛型类型：

```kotlin
class SelectClause<TSource : KPojo, TResult : KPojo, TContext : KPojo>
```

含义：

- `TSource`：原始 KPojo，例如 `User`。
- `TResult`：最终查询返回投影。
- `TContext`：后续子句上下文，包含源全列 + select 新增列。

这是破坏式重构：不保留二泛型 `SelectClause<T, R>`，不做 `typealias`，不加默认泛型。所有调用点直接迁移到最合理的三泛型 API。

## Select API 修改

编译器插件最终应调用类似以下入口：

```kotlin
internal fun <T : KPojo, R : KPojo, C : KPojo> T.selectGeneratedProjection(
    resultClass: KClass<R>,
    contextClass: KClass<C>,
    fields: ToSelect<T, Any?> = null
): SelectClause<T, R, C>
```

其中：

- `fields` lambda 的接收者仍然是源 `T`。
- `resultClass` 用于 `queryList/queryOne/queryOneOrNull` 映射。
- `contextClass` 用于后续 `where/orderBy/having` 的类型。
- core 不负责生成类，只接收编译器插件传入的 `KClass`。

普通用户入口仍保持：

```kotlin
fun <T : KPojo> T.select(fields: ToSelect<T, Any?> = null): SelectClause<T, T, T>
```

## 后续子句 API 修改

当前 API：

```kotlin
fun where(selectCondition: ToFilter<T, Boolean?> = null): SelectClause<T, R>
fun having(selectCondition: ToFilter<T, Boolean?> = null): SelectClause<T, R>
fun orderBy(someFields: ToSort<T, Any?>): SelectClause<T, R>
fun groupBy(someFields: ToSelect<T, Any?>): SelectClause<T, R>
```

需要改为：

```kotlin
fun where(selectCondition: ToFilter<C, Boolean?> = null): SelectClause<T, R, C>
fun having(selectCondition: ToFilter<C, Boolean?> = null): SelectClause<T, R, C>
fun orderBy(someFields: ToSort<C, Any?>): SelectClause<T, R, C>
```

`groupBy` 是否使用 `T` 还是 `C` 需要再定：

- 如果只允许源字段分组，则使用 `T`。
- 如果允许按 select alias / 函数投影分组，则使用 `C`。

建议第一版保持 `groupBy` 面向源字段，避免 SQL 语义复杂化：

```kotlin
fun groupBy(someFields: ToSelect<T, Any?>): SelectClause<T, R, C>
```

## KSelectable 修改

当前：

```kotlin
abstract class KSelectable<T : KPojo>(internal open val pojo: T)
```

建议演进为：

```kotlin
abstract class KSelectable<TSource : KPojo, TResult : KPojo>(
    internal open val pojo: TSource
)
```

必要时增加 context：

```kotlin
abstract class KSelectable<TSource : KPojo, TResult : KPojo, TContext : KPojo>
```

第一版建议至少引入 `TResult`，因为子查询、insert-select、join derived source、CTAS 都需要知道 query source 的结果投影。

影响点：

- `UnionClause` 入参从 `KSelectable<out KPojo>` 调整为能表达 result projection。
- `SelectFrom` / join 查询需要实现新的 `KSelectable`。
- `insert<Target> { ... }` 的 lambda `it` 应该是 source query 的 `TResult`。
- CTAS 的源 query 使用 `KSelectable<*, TResult>`。

## Select 表达式模型

当前 `selectFields` 是 `LinkedHashSet<Field>`，适合列字段，但新版 select item 需要支持：

- 普通列字段。
- 函数表达式。
- 标量子查询。
- 窗口函数表达式。
- raw SQL 表达式。
- alias。
- collection literal 产生的有序列表。

建议新增 core 内部模型：

```kotlin
sealed interface SelectExpression

data class ColumnSelectExpression(val field: Field) : SelectExpression
data class FunctionSelectExpression(val field: FunctionField, val alias: String?) : SelectExpression
data class ScalarSubquerySelectExpression(val query: KSelectable<*, *>, val alias: String, val typeHint: KClass<*>?) : SelectExpression
data class ModifiedSelectExpression(val expression: SqlExpression, val alias: String?) : SelectExpression
```

`SelectClause` 内部不要只保存 `selectFields`，而应保存有序 `selectItems`：

```kotlin
internal val selectExpressions: MutableList<SelectExpression>
```

再统一转换为 AST `SelectItem`。

## Alias 元数据

为了支持：

```kotlin
.where { it.lastOrderAmount > 100 }
.orderBy { it.rn.asc() }
```

core 需要知道某个 `Field` 来自源列还是 select alias。

建议给 `Field` 或新增 `ResolvedFieldRef` 增加来源：

```kotlin
enum class FieldOrigin {
    SOURCE_COLUMN,
    SELECT_ALIAS,
    GENERATED_CONTEXT_ONLY
}
```

用途：

- `SOURCE_COLUMN` 渲染为源表列。
- `SELECT_ALIAS` 在同层可引用时渲染为 alias；不能同层引用时触发自动外层派生查询。
- `GENERATED_CONTEXT_ONLY` 只用于编译期上下文，不应直接出现在最终 result projection。

## 自动 SQL 分层

当 `where` / `having` / `orderBy` 引用了 select alias 或窗口函数结果时，SQL 可能需要自动包一层：

```sql
SELECT *
FROM (
    SELECT u.id, ..., row_number() over (...) AS rn
    FROM user u
) q
WHERE q.rn <= 2
```

core 需要在 `SelectClause` 里记录：

```kotlin
internal var requiresOuterQueryLayer: Boolean = false
internal val outerCriteria: MutableList<Criteria>
internal val outerOrderBy: MutableList<OrderByItem>
```

转换规则：

- `where` 引用 `SOURCE_COLUMN`：放内层 `WHERE`。
- `where` 引用 `SELECT_ALIAS` / window alias：放外层 `WHERE`。
- `orderBy` 引用 `SELECT_ALIAS`：方言允许时可同层；否则外层。
- `having` 聚合过滤通常保持内层 `HAVING`；若引用窗口结果则外层。

第一版可以先保守：只要后续子句引用 select alias / window alias，就包外层。

## where 多次调用语义

多次调用 `where` 应叠加为 `AND`，不是覆盖。

当前 `SelectClause.where` 已经在 AST 层使用 `AND` 合并，这个行为应保留并写测试。

```kotlin
val paidOrders = Order()
    .select()
    .where { it.status == PAID }

User()
    .select { [it.id, it.name] }
    .where { u ->
        exists(
            paidOrders.where { it.userId == u.id }
        )
    }
```

应渲染为：

```sql
WHERE status = ?
  AND user_id = u.id
```

如果未来需要覆盖语义，必须新增显式 API，例如 `replaceWhere(...)`，不能改变默认 `where`。

## 子查询支持

core 需要让 `KSelectable` 可以出现在以下位置：

- select item 标量子查询。
- `field in query`。
- `[field1, field2] in query`。
- `exists(query)` / `!exists(query)`。
- 标量比较：`field > query.limit(1)`。
- update/upsert set 右侧。

建议新增条件表达式节点或扩展现有 Criteria：

```kotlin
data class SubqueryExpression(val query: KSelectable<*, *>)
data class ExistsExpression(val query: KSelectable<*, *>, val negated: Boolean)
data class InSubqueryExpression(val left: SqlExpression, val query: KSelectable<*, *>, val negated: Boolean)
data class RowValueExpression(val items: List<SqlExpression>)
```

`KSelectable.toStatement()` 已存在，应作为子查询 AST 的入口。

## Scalar 子查询规则

core 需要记录并校验 scalar 子查询：

- 只能选择一列。
- 聚合且无 `groupBy` 可不写 `.limit(1)`。
- 其他非聚合标量子查询必须写 `.limit(1)`。
- `limit(1) as T` 是编译器类型提示，不改变 SQL，也不能绕过校验。

建议在 `KSelectable` 或 `SelectStatement` 上暴露只读能力：

```kotlin
fun isSingleColumnProjection(): Boolean
fun isAggregateWithoutGroupBy(): Boolean
fun hasLimitOne(): Boolean
```

## Insert Select

新增：

```kotlin
fun <Target : KPojo> KSelectable<*, *>.insert(
    values: ToSelect<TResult, Any?>
): InsertSelectClause<Target>
```

注意：

- lambda `it` 是源 query 的 `TResult`。
- 目标字段序列来自 `Target` 的可插入字段规则。
- `values` 数量必须与目标字段序列一致。
- 类型必须兼容。
- `null` 允许作为显式值。

如果 `KSelectable` 第一版无法带 `TResult`，则 insert-select 很难做到类型安全。

## CTAS

DDL 层新增重载：

```kotlin
dataSource.table.createTable(target: KPojo, query: KSelectable<*, *>)
```

行为：

- 不新增 `createTableAs`。
- 不提供 `createView`。
- `target` 决定目标表名。
- `query` 决定 `SELECT` 来源。
- schema 保真度按方言能力处理；需要完整 schema 时使用 `createTable(target)` + `query.insert<Target> { ... }`。

## Join / Derived Query

join 目标需要支持 `KSelectable`：

```kotlin
User().leftJoin(latestOrder) { user, order ->
    on { user.id == order.userId }
    select { [user.id, order.lastOrderAt] }
}
```

core 需要：

- 为 derived query 自动生成表 alias。
- join lambda 参数使用 derived query 的 `TResult`。
- 不暴露 `.asTable(...)`。
- `SelectFrom` 也实现新的 `KSelectable`，以便 join result 可用于 insert-select / CTAS。

## 通用表达式修饰器

core 函数系统需要支持通用表达式修饰器，而不是新增一个专门的 `WindowFunctionExpression`：

```kotlin
f.rowNumber()
    .over(
        partitionBy = [it.userId],
        orderBy = [it.createTime.desc()]
    )
    .as_("rn")
```

建议新增通用 AST 节点：

```kotlin
data class ModifiedExpression(
    val base: SqlExpression,
    val modifiers: List<ExpressionModifier>
)

sealed interface ExpressionModifier

data class OverModifier(
    val partitionBy: List<SqlExpression>,
    val orderBy: List<OrderByItem>
) : ExpressionModifier

data class FilterModifier(
    val condition: SqlExpression
) : ExpressionModifier

data class WithinGroupModifier(
    val orderBy: List<OrderByItem>
) : ExpressionModifier
)
```

`.over(...)` 只是 `ModifiedExpression` 的一种 modifier，后续能力复用同一套机制：

- `.filter { ... }`
- `.withinGroup(...)`
- aggregate order by

渲染层根据 modifier 类型组合 SQL：

```sql
COUNT(*) FILTER (WHERE ...)
ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)
PERCENTILE_CONT(...) WITHIN GROUP (ORDER BY ...)
```

这样函数表达式、聚合函数、窗口函数都走同一套表达式链，不需要为每个 SQL 特性新增一类 select item。

## 重构顺序

建议按以下顺序直接大改 core：

1. 直接把当前 `SelectClause<T, R>` 改成 `SelectClause<TSource, TResult, TContext>`。
2. 引入 `KSelectable<TSource, TResult>`，并调整所有实现和消费方。
3. 将 select API、query API、分页、cascade、批量扩展函数全部改到三泛型 `SelectClause`。
5. 改 `where/having/orderBy` 使用 context 类型。
6. 引入有序 select expression model，替代只依赖 `selectFields` 的 select item 构建。
7. 支持 select alias 元数据和自动外层 SQL 分层。
8. 支持 subquery expression / exists / in / tuple in AST。
9. 支持 insert-select。
10. 支持 CTAS。
11. 支持 derived query join。
12. 支持通用表达式修饰器。

每一步都需要对应 compiler-plugin official testData 和 core SQL rendering 单测。

## 需要重点测试的行为

- `select { [it.id, it.name] }.queryList()` 返回 result projection。
- `select { [it.id] }.where { it.age > 18 }` 中 `age` 不需要投影。
- `select { [it.id, sub.as_("lastOrderAmount")] }.where { it.lastOrderAmount > 100 }` 自动外层分层。
- 多次 `where` 叠加为 `AND`。
- `exists(Order().select().where { ... })` 渲染为 `EXISTS (SELECT 1 ...)`。
- `it.id in Order().select { it.userId }`。
- `[it.userId, it.createTime] in query`，单元素 `[it.id] in query` 不允许。
- 非聚合 scalar 子查询缺少 `.limit(1)` 报错。
- `KSelectable.insert<Target> { [...] }` 顺序映射和数量校验。
- `createTable(target, query)` CTAS。
- join derived query 自动 alias。
- window alias `rn` 过滤自动外层查询。
