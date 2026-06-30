# Kronos 子查询 Core 任务清单

状态：按任务分状态跟踪

本文档只记录 core 侧为了承接子查询 DSL 必须完成的工作。compiler plugin 适配放到 core 地基稳定之后再展开；当前 compiler 三泛型适配和 operator function 已通过 BoxTest，仅作为备注，不计入 core 完成度。

本文档必须与 `SUBQUERY_DSL_SPEC.md` 的核心语义保持一致：

- 概念目标是 `SelectClause<Source, Selected, Context>`。
- `select { ... }` 的 receiver 是 `Source`。
- `where` / `having` / `orderBy` 的 receiver 是 `Context`。
- `queryList` / `queryOne` / `queryOneOrNull` 的返回类型是 `Selected`。
- `where` / `having` 引用 select alias、聚合 alias、窗口 alias 时，core 负责提供自动派生表分层的承载结构，不能依赖数据库同层 alias 支持。

## 总览

| 完成度 | 任务 | 状态 | 当前判断 |
|--------|------|------|----------|
| 70% | 任务 1：建立 `SelectClause<Source, Selected, Context>` core 类型地基 | 部分完成 | 三泛型、`Selected` 返回、`Context` receiver 签名和 `contextPojo` 承载点已落地；真正的 Context 生成/加载仍等 compiler plugin。 |
| 75% | 任务 2：收敛 `KSelectable` 语义 | 部分完成 | `KSelectable<Selected>` 和 `selectedKClass` 已落地，pagination / union / join 已适配；join/union/derived 的专项 shape 兼容测试仍需补齐。 |
| 65% | 任务 3：引入延迟 materialize 的 query expression | 部分完成 | `SelectQueryRef`、`KSelectableQueryRef`、`DeferredSubqueryExpression` 已有，`parameterValues` 可透传；外层 alias、相关子查询协调仍未完整。 |
| 80% | 任务 4：补 deferred subquery lowering pass | 部分完成 | lowering 已覆盖 select/update/delete/insert/upsert/union/DDL 主要表达式位置，并在 renderer 前自动执行；跨 statement 参数命名仍需增强。 |
| 63% | 任务 5：前移 alias registry 与字段来源 metadata | 部分完成 | alias/source metadata、alias registry、投影输出名推导和条件来源分析地基已落地；函数 select item scope 已改由 FunctionManager/FunctionBuilder 动态提供；所有生成路径和 compiler Context 字段解析仍需补齐。 |
| 60% | 任务 6：支持 select item 标量子查询 | 部分完成 | `KTableForSelect` 已能按顺序收集 aliased `KSelectable` scalar select item，compiler box 覆盖 `KSelectable.as_` 收集，core DSL 测试覆盖 SQL/参数；自动 Selected/Context 投影字段仍未完整接入。 |
| 96% | 任务 7：补 `IN` / `NOT IN` 子查询构造入口 | 部分完成 | compiler 底层测试已覆盖 `field in/!in SelectClause` 和 tuple `in/!in` 到 deferred subquery Criteria；core 测试已覆盖 select/update/delete build SQL/参数以及 select/update/delete tuple IN/NOT IN；左右列数匹配校验已落地并覆盖 AST/DSL 错误测试。 |
| 100% | 任务 8：补 row-value tuple 表达式 | 已完成 | `RowValueExpression` 已有，单元素拒绝，renderer/test 覆盖 tuple IN/NOT IN，用户侧 `[field1, field2] in/!in query` 已有 compiler handoff 与 core SQL/参数竖切。 |
| 50% | 任务 9：派生表包装与外层过滤 | 部分完成 | wrapper 工具、outer where/orderBy 和 automatic layering 工具测试已落地；SelectClause build 自动调用与隐藏支撑投影仍未完成。 |
| 50% | 任务 10：条件来源与自动分层执行 | 部分完成 | `SelectConditionLayering` 可基于 alias/source metadata 拆分 source 与 selected/aggregate/window alias；尚未接入所有 builder 流程。 |
| 74% | 任务 11：补标量子查询公共校验 | 部分完成 | `SubqueryValidator.validateScalar` 已由统一 lowering 覆盖 select/update/delete/insert/upsert/DDL 表达式位置；`validateInSubquery` 已覆盖 IN 子查询左右列数校验；错误定位和唯一键证明仍未做。 |
| 88% | 任务 12：扩展 `CriteriaToAstConverter` | 部分完成 | scalar RHS、exists、IN/NOT IN query、row tuple IN/NOT IN、quantified comparison 的结构化承接已落地；compiler 已覆盖 scalar RHS、`exists/!exists`、`in/!in`、tuple IN/NOT IN 和 `any/some/all` quantified comparison 的底层 Criteria 生成。 |
| 70% | 任务 13：补 `ORDER BY` 子查询和表达式排序承接 | 部分完成 | AST/render/lowering 已支持 order by scalar subquery 和 selected alias；`SelectClause.orderBy` 已可承接 expression/scalar subquery sort item，compiler sort box 已覆盖 selectable sort handoff；selected/aggregate/window alias 的完整 builder/compiler 接入仍未完成。 |
| 72% | 任务 14：补 `UPDATE SET` 标量子查询承接 | 部分完成 | AST/renderer/lowering 和 `UpdateClause` builder 已可承接 scalar subquery/expression assignment；core DSL 已覆盖动态 `patch` 与 `.set { setValue(field, query) }` scalar subquery SQL/参数，spec 已确认 `patch` 可保留；最终推荐语法 `.set { field = query }` 的 FIR/类型安全入口仍需补。 |
| 91% | 任务 15：补 `UPDATE` / `DELETE WHERE` 子查询承接 | 部分完成 | AST/renderer/lowering 可表达 update/delete where exists/in/scalar/quantified；core DSL 已覆盖 update/delete where `in/!in`、tuple IN/NOT IN、scalar comparison、quantified comparison 与 `exists/!exists` 的 SQL/参数。 |
| 87% | 任务 16：补 `INSERT SELECT` | 部分完成 | `InsertStatement.source`、renderer/lowering、`KSelectable.insert<Target>` 和 `UnionClause.insert<Target>` builder 入口已落地；普通 select、join source、union source、显式 values 映射和默认列数校验的 core DSL SQL/参数测试已覆盖，类型兼容校验仍需补。 |
| 76% | 任务 17：补 `UPSERT` 子查询表达式 | 部分完成 | `conflictAssignments`、Upsert builder、fallback upsert update 分支和 MySQL/Postgres/SQLite expression upsert renderer 已有；MySQL core DSL 已覆盖动态 `patch` 与 `.set { setValue(field, query) }` conflict assignment scalar subquery SQL/参数，spec 已确认 `patch` 可保留；最终推荐语法 `.set { field = query }` 的 FIR/类型安全入口仍需补。 |
| 90% | 任务 18：补 `CREATE TABLE AS SELECT` | 部分完成 | `CreateTableAsSelectStatement`、`TableOperation.createTable(target, query)`、CTAS task 参数透传和五方言 CTAS 渲染策略已落地；普通 select、join source、union source 的 core DSL SQL/参数测试已覆盖，schema 保留策略和 Oracle `ifNotExists` 入口仍需补。 |
| 97% | 任务 19：补 core AST / renderer 测试 | 部分完成 | `SubqueryRendererTest` 已覆盖 nested scalar、quantified、order by scalar/alias、criteria converter、layering、update/delete/insert/upsert/CTAS 和五方言矩阵；core DSL 已补 select/update/delete 的 exists/in/scalar/quantified comparison、select/update/delete tuple IN/NOT IN、order by scalar subquery、update-set scalar、insert-select select/join/union source、insert-select 显式 values 映射与列数校验、upsert scalar、CTAS select/join/union source SQL/参数竖切，更深 builder/compiler 测试仍不完整。 |

## 当前事实

- `SelectClause<Source, Selected, Context>` 已存在，`queryList` / `queryOne` / `queryOneOrNull` 返回 `Selected`。
- `select { ... }` 仍以 `Source` 为 receiver；`where` / `having` / `orderBy` 的 core 签名已切到 `Context`，当前默认入口仍让 `Context = Source`。
- `KSelectable` 当前已收敛为 `KSelectable<Selected>`，包含 `selectedKClass: KClass<Selected>`。
- `PagedClause<Source, Selected, Clause : KSelectable<Selected>>` 已使用 `selectedKClass` 返回 `Selected`。
- core 中已经存在一批子查询 AST：
  - `SubqueryExpression.ExistsExpression`
  - `SubqueryExpression.ScalarSubquery`
  - `SubqueryExpression.QuantifiedComparison`
  - `SpecialExpression.InSubqueryExpression`
  - `SubqueryTable`
- core 已新增 deferred subquery / lowering / row value / derived wrapper / scalar validation / criteria converter 承接 / DML DDL AST 地基 / `SubqueryRendererTest` 等地基，`:kronos-core:test` 已通过。
- select item alias/source metadata 地基已部分落地：select item 可推导 metadata，`SelectStatement` 已维护 alias registry，并提供 select item metadata / 输出名查询能力；`SelectConditionLayering` 已能消费这些 metadata。
- `select { [it.id, query.limit(1).as_("alias"), it.name] }` 已有最小竖切：compiler 会把 aliased `KSelectable` 注入为 scalar subquery select item，core build 可渲染 SQL/参数并保持 select item 顺序。
- `where { exists(query) }` / `where { !exists(query) }` 已有最小竖切：compiler 会生成 `CriteriaSubqueryValue.Exists(KSelectableQueryRef(...))`，core DSL build 已覆盖 select/update/delete 的 SQL/参数。
- `where { it.field > any(query) }` / `some(query)` / `all(query)` 已有最小竖切：compiler 会生成 `CriteriaSubqueryValue.QuantifiedComparison(KSelectableQueryRef(...), quantifier)`，core DSL build 已覆盖 update/delete quantified comparison 的 SQL/参数。
- `where { [it.a, it.b] in query }` / `where { [it.a, it.b] !in query }` 已有最小竖切：compiler 会生成 `CriteriaSubqueryValue.In(value = List<Field>, not = ...)`，core converter 会 lower 为 `RowValueExpression`，core DSL build 已覆盖 select tuple IN/NOT IN 的 SQL/参数。
- `update().patch("field" to query.limit(1))` 已有最小竖切：core build 可渲染 `SET field = (SELECT ... LIMIT 1)` 并保留子查询参数。`patch` 已纳入 spec，定位为动态字段入口；`.set { setValue(field, query) }` 也已可渲染 SQL/参数。字段已知时仍推荐后续补齐最终语法 `.set { field = query }`。
- `KSelectable.insert<Target>()` / `UnionClause.insert<Target>()` 已有最小竖切：普通 select、join source 与 union source 均可渲染 `INSERT INTO target (...) SELECT ...` 并透传 source query 参数。
- `upsert().patch("field" to query.limit(1)).onConflict()` 已有 MySQL 最小竖切：core build 可渲染 `ON DUPLICATE KEY UPDATE field = (SELECT ... LIMIT 1)` 并透传子查询参数。`patch` 已纳入 spec，定位为动态字段入口；`.set { setValue(field, query) }` 也已可渲染 SQL/参数。字段已知时仍推荐后续补齐最终语法 `.set { field = query }`。
- `dataSource.table.buildCreateTableAsSelectTask(target, query)` 已有 MySQL 最小竖切：普通 select、join source 与 union source 均可渲染 `CREATE TABLE ... AS SELECT ...` 并透传 source query 参数。
- renderer 已具备渲染这些 AST 的基础能力；`AbstractSqlRenderer` 不直接渲染 deferred node，会在 `render(statement)` 前统一 lowering。
- 当前阶段不要无计划地破坏已跑通的 projection 链路，但 core 类型地基必须朝 `SelectClause<Source, Selected, Context>` 收敛。

## 目标

让 core 能稳定表达并渲染以下能力：

- select list 中的标量子查询；
- `EXISTS` / `NOT EXISTS`；
- `IN` / `NOT IN` 子查询；
- row-value tuple `IN` 子查询；
- 嵌套子查询；
- 派生表包装与外层过滤；
- `where` / `having` / `orderBy` 使用 `Context` 时所需的类型承载点；
- select item alias registry、字段来源 metadata、自动 SQL 分层基础；
- `ORDER BY` 子查询或表达式排序；
- `UPDATE SET` 标量子查询；
- `UPDATE` / `DELETE WHERE` 子查询；
- `INSERT SELECT`；
- `UPSERT` 子查询表达式；
- `CREATE TABLE AS SELECT`。

## 任务 1（70%）：建立 `SelectClause<Source, Selected, Context>` core 类型地基（部分完成）

`SUBQUERY_DSL_SPEC.md` 已经确认三种类型：

- `Source`：原始查询源 KPojo。
- `Selected`：最终查询结果投影。
- `Context`：`where` / `having` / `orderBy` 的 receiver，包含 `Source` 全字段 + `Selected` 投影字段。

core 不能继续只把 Context 视为 compiler plugin 的后置事项。即使第一阶段暂时让 `Context = Source` 或 `Context = Selected` 的某个兼容形态存在，`SelectClause` 的目标类型也必须明确为：

```kotlin
class SelectClause<Source : KPojo, Selected : KPojo, Context : KPojo>(
    ...
)
```

目标方法签名方向：

```kotlin
fun where(condition: ToFilter<Context, Boolean?> = null): SelectClause<Source, Selected, Context>

fun having(condition: ToFilter<Context, Boolean?> = null): SelectClause<Source, Selected, Context>

fun orderBy(fields: ToSort<Context, Any?>): SelectClause<Source, Selected, Context>

fun queryList(wrapper: KronosDataSourceWrapper? = null): List<Selected>
```

当前落地情况：

- `SelectClause<Source, Selected, Context>` 已存在。
- `queryList` / `queryOne` / `queryOneOrNull` 的非 reified 版本返回 `Selected`。
- `withTotal()` 返回的 `PagedClause` 已携带 `Selected`。
- `where` / `having` / `orderBy` 的 core 签名已切到 `Context` receiver。
- `SelectClause` 已新增 `contextPojo` 承载点，当前默认入口仍以 `Source` 作为 Context。
- `select` / `groupBy` / `by` / `cascade` 等仍以 `Source` 为 receiver，这与 `select {}` 使用 `Source` 的目标一致。

剩余工作：

- 需要由 compiler plugin 生成/加载真实 Context 类型，并把它传入 `SelectClause`。
- 需要继续保证现有投影链路和 join/union 入口在真实 Context 接入后不回归。

验收：

- core API 能表达 `Source` / `Selected` / `Context` 三种类型。
- `queryList` / `queryOne` / `queryOneOrNull` 仍然返回 `Selected`。
- 后续 compiler plugin 可以把生成的 Context 类型加载到 SelectClause 上。

## 任务 2（75%）：收敛 `KSelectable` 语义（部分完成）

当前 `KSelectable<T : KPojo>` 的 `T` 更像 source pojo。子查询消费侧更关心 query 的结果投影类型。

目标方向：

```kotlin
abstract class KSelectable<R : KPojo>(
    internal open val pojo: KPojo,
    open val selectedKClass: KClass<R>
)
```

`SelectClause<Source, Selected, Context>` 应实现：

```kotlin
class SelectClause<Source : KPojo, Selected : KPojo, Context : KPojo>(
    ...
) : KSelectable<Selected>(pojo, selectedKClass)
```

注意：

- `KSelectable<R>` 的 `R` 是 query 的最终投影类型，不是 source 类型。
- `Context` 不属于 `KSelectable` 的泛型核心，但属于 `SelectClause` 的子句 receiver 类型。
- 保留 `toStatement(wrapper)` 作为 `KSelectable` 的核心能力。

当前落地情况：

- `KSelectable` 已收敛为 `KSelectable<Selected>`，并包含 `selectedKClass: KClass<Selected>`。
- `SelectClause` 以 `projectionClass` 传入 `selectedKClass`。
- `PagedClause` 使用 `selectClause.selectedKClass` 查询并返回 `Selected`。
- pagination / union / join 代码已适配单泛型 `KSelectable<Selected>`。

剩余工作：

- join / union / derived query 的 `selectedKClass` 规则需要系统核实和补测试，不能仅凭字段存在视为完成。

验收：

- `SelectClause<Source, Selected, Context>` 可作为 `KSelectable<Selected>` 被消费。
- union / join / select 现有对 `KSelectable` 的使用不破坏。
- join / union / derived query 都必须能稳定提供 `selectedKClass`：
  - 显式 DTO 投影使用用户传入的 `KClass`。
  - 自动 DTO 投影使用 FIR 生成的 projection `KClass`。
  - join 查询的投影类型来自 join `select { ... }` 的 `Selected`。
  - union 查询要求所有分支投影 shape 兼容，并以统一的 `Selected` 类型暴露。
  - derived query 的 `selectedKClass` 来自被包装 query 的 `Selected`。
- 现有 projection 回归测试仍能通过。

## 任务 3（65%）：引入延迟 materialize 的 query expression（部分完成）

已有 AST 直接吃 `SelectStatement`，但 DSL/compiler 更自然地产生 `KSelectable`。不能在 helper 调用时过早执行 `toStatement(wrapper)`，否则相关子查询中的外层字段引用、参数命名、派生表包装会被提前冻结。

目标是引入延迟 materialize 的 core 表达能力，例如：

```kotlin
interface SelectQueryRef {
    fun materialize(context: QueryMaterializeContext): SelectStatement
}

data class KSelectableQueryRef(
    val query: KSelectable<*>
) : SelectQueryRef
```

表达式层不要急着把 query 变成 `SelectStatement`：

```kotlin
sealed class DeferredSubqueryExpression : Expression {
    data class Scalar(val query: SelectQueryRef) : DeferredSubqueryExpression()
    data class Exists(val query: SelectQueryRef, val not: Boolean = false) : DeferredSubqueryExpression()
    data class In(
        val value: Expression,
        val query: SelectQueryRef,
        val not: Boolean = false
    ) : DeferredSubqueryExpression()
    data class QuantifiedComparison(
        val expression: Expression,
        val operator: SqlOperator,
        val quantifier: SubqueryExpression.Quantifier,
        val query: SelectQueryRef
    ) : DeferredSubqueryExpression()
}
```

最终在 outer query 的 `toStatement` / statement build 阶段统一 materialize：

- 统一分配 table alias。
- 统一收集参数。
- 统一处理相关子查询中的外层字段引用。
- 统一决定内外层 SQL 分层。

当前落地情况：

- 已有 `QueryMaterializeContext`、`SelectQueryRef`、`KSelectableQueryRef`。
- 已有 `DeferredSubqueryExpression.Scalar` / `Exists` / `In` / `QuantifiedComparison`。
- `KSelectableQueryRef.materialize` 延迟调用 `query.toStatement(context.wrapper, context.parameterValues)`，可透传参数 map。
- 已新增 internal deferred subquery builders，避免 helper 立即 render 或绑定参数。

剩余工作：

- 外层 alias 分配、相关子查询外层字段引用协调、自动分层决策尚未完整落地。
- 用户侧 DSL/compiler 仍未系统接入 deferred query ref。

验收：

- helper 不立即 render 或绑定参数。
- 相关子查询可以安全引用外层 query 字段。
- 最终 materialize 后仍能落到已有 `SubqueryExpression.*` / `SpecialExpression.InSubqueryExpression` AST。

## 任务 4（80%）：补 deferred subquery lowering pass（部分完成）

renderer 应继续只吃 concrete AST，不直接认识 deferred node。所有 `DeferredSubqueryExpression` 必须在 renderer 前统一 lower。

lowering 目标：

- `DeferredSubqueryExpression.Scalar` -> `SubqueryExpression.ScalarSubquery`
- `DeferredSubqueryExpression.Exists` -> `SubqueryExpression.ExistsExpression`
- `DeferredSubqueryExpression.In` -> `SpecialExpression.InSubqueryExpression`
- `DeferredSubqueryExpression.QuantifiedComparison` -> `SubqueryExpression.QuantifiedComparison`

lowering pass 负责：

- 调用 `SelectQueryRef.materialize(context)`。
- 收集并合并子查询参数。
- 分配或传递相关子查询需要的外层上下文。
- 在 materialize 之后执行公共子查询校验。

当前落地情况：

- `SubqueryLowering.lower(statement: Statement)` 已覆盖 `SelectStatement`、`UpdateStatement`、`DeleteStatement`、`InsertStatement`、`UnionStatement` 和 DDL。
- `SubqueryLowering.lower(select)` 已覆盖 select/from/where/groupBy/having/orderBy。
- `lowerExpression` 已能 lower scalar、exists、in、quantified comparison。
- scalar lowering 会调用 `SubqueryValidator.validateScalar`。
- `AbstractSqlRenderer.renderExpression` 遇到 deferred node 会报错，保持 renderer 不直接消费 deferred。
- `AbstractSqlRenderer.render(statement)` 会在渲染前统一执行 lowering。

剩余工作：

- 参数合并、外层上下文传递、相关子查询 alias 协调仍未完整。
- 需要确认所有非 renderer 的 statement 消费入口也不会绕过 lowering。

验收：

- renderer 不需要新增 deferred expression 分支。
- concrete AST 与现有 renderer 兼容。
- delayed query ref 在 lowering 前不会冻结参数或 wrapper。

## 任务 5（63%）：前移 alias registry 与字段来源 metadata（部分完成）

自动分层不能等到最后才猜。select item 生成时就需要记录字段名和来源。

建议在 `SelectStatement` 或 builder 上维护 alias registry：

```kotlin
data class SelectAliasInfo(
    val outputName: String,
    val expression: Expression,
    val scope: ExpressionScope,
    val sourceField: Field? = null
)

enum class ExpressionScope {
    SOURCE,
    SELECTED,
    AGGREGATE,
    WINDOW,
    UNKNOWN
}
```

规则：

- 普通 source 字段：`SOURCE`。
- `.as_("xxx")` 的计算表达式或标量子查询：`SELECTED`。
- 聚合表达式 alias：`AGGREGATE`。
- 窗口函数 alias：`WINDOW`。
- 无法判断来源：`UNKNOWN`，需要保守处理或报错。

Context 字段解析时必须能查询 alias registry，以便把条件拆到 inner where / outer where / inner having / outer filter。

当前落地情况：

- `SelectItem.kt` 已新增 `SelectItemAliasMetadata` 和 `SelectItemSourceScope`，select item 可推导输出 alias/source metadata。
- `SelectStatement.kt` 已新增 `aliasRegistry`、`selectItemMetadata()`、`findSelectOutput()`，开始承载 select item 输出名与来源查询。
- `SelectStatementDerivation.kt` 的 derived wrapper 投影输出名已优先使用 metadata。
- `SelectClause.kt` 的 `fieldsToSelectItems` 已同步记录普通字段、字段 alias、`FunctionField`、无 alias expression 的 metadata。
- 函数 select item 的 scope 不再在 `SelectClause` 或 `SelectItem` 中按函数名写死；`FunctionManager.getSelectItemScope()` 会从已注册的 `FunctionBuilder.selectItemScope()` 动态获取，内置聚合 builder 返回 `AGGREGATE`，自定义 builder 可返回 `WINDOW` / `SELECTED` 等 scope。
- 相关 `SelectClauseAstTest` / `SubqueryRendererTest` 已通过。

剩余工作：

- 补齐 registry 覆盖面，确认所有 select item 生成路径都登记输出名、表达式、来源 scope、source field。
- 明确 `SelectItemSourceScope` 与任务 10 所需 `SOURCE` / `SELECTED` / `AGGREGATE` / `WINDOW` / `UNKNOWN` 分层语义的映射。
- 让 Context 字段解析和自动分层使用 registry，而不是事后猜测。
- 补更多 alias/source metadata 的 builder 与派生表场景测试。

验收：

- select item 产生字段名时同步登记来源。
- `where` / `having` / `orderBy` 引用字段时能知道它来自 Source 还是投影 alias。
- 没有 alias 的 expression select item 如需被 Context 引用，必须报错或生成稳定内部 alias。

## 任务 6（60%）：支持 select item 标量子查询（部分完成）

目标语法最终形态：

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
                .as_("lastOrderAmount")
        ]
    }
```

core 侧要能表达为：

```kotlin
SelectItem.ExpressionSelectItem(
    expression = DeferredSubqueryExpression.Scalar(KSelectableQueryRef(orderSelect)),
    alias = "lastOrderAmount"
)
```

必须确认并实现：

- `KSelectable` 可以作为 scalar select item 输入。
- selectable / scalar subquery expression 必须可 alias 化，支持 `.as_("xxx")` 对应 spec 中的 `lastOrderAmount`。
- `.as_("xxx")` 生成 `Selected` 字段，也生成 `Context` 字段。
- 没有 alias 的 scalar select item 不适合作为 Context 字段引用，应报错或由 builder 生成稳定内部 alias。

当前落地情况：

- `SelectItem.ExpressionSelectItem(SubqueryExpression.ScalarSubquery(...), alias)` 可由 renderer 渲染。
- `DeferredSubqueryExpression.Scalar` 可 lower 成 `ScalarSubquery`。
- `SubqueryRendererTest` 覆盖了 select list scalar subquery 和 deferred scalar lowering。
- `KTableForSelect` 已新增保序 projection item 通道，字段和 scalar subquery select item 可以按用户 `[]` 顺序进入 `SelectClause.statement.selectList`。
- compiler `SelectTransformer` 已能识别 `KSelectable.as_("alias")`，并注入 `addScalarSubquery(query, alias)`，官方 box `select/scalarSubquerySelectItem.kt` 覆盖该底层转换契约。
- core 普通测试 `MysqlSubqueryDslTest` 已覆盖 `select { [it.id, query.limit(1).as_("alias"), it.name] }` 的 SQL/参数输出。

剩余工作：

- `.as_("xxx")` 到 `Selected` / `Context` 自动投影字段的完整承接未完成。
- scalar select item 的 Context 引用、自动分层和 FIR projection 类型推导仍未完整完成。
- 当前只开放 aliased `KSelectable` 作为 select item；未 alias 的 scalar subquery 仍应保持拒绝或不可引用，避免输出名不稳定。

验收：

- AST renderer 可以渲染 select list 中的 scalar subquery。
- 支持嵌套 scalar subquery。
- alias registry 记录 `lastOrderAmount`。
- 用户 DSL `select { [it.id, query.limit(1).as_("lastOrderAmount")] }` 可 build 出正确 SQL/参数。

## 任务 7（96%）：补 `IN` / `NOT IN` 子查询构造入口（部分完成）

已有：

```kotlin
SpecialExpression.InSubqueryExpression(
    value: Expression,
    subquery: SelectStatement,
    not: Boolean = false
)
```

需要补 helper：

```kotlin
internal fun Expression.toInSubqueryExpression(query: SelectQueryRef): DeferredSubqueryExpression.In

internal fun Expression.toNotInSubqueryExpression(query: SelectQueryRef): DeferredSubqueryExpression.In
```

实际结构应归入任务 3 的延迟模型：

```kotlin
DeferredSubqueryExpression.In(
    value = expression,
    query = query,
    not = false
)
```

后续 compiler/DSL 再把：

```kotlin
it.id in Order().select { it.userId }
it.id !in Order().select { it.userId }
```

转成上述 AST。

这些 helper 只能作为 internal/core builder 使用，不能放到用户可见 DSL extension 包中，避免和 spec 中“不引入 `inSubquery` 用户 API”的结论冲突。

当前落地情况：

- `SpecialExpression.InSubqueryExpression` 已存在。
- `DeferredSubqueryExpression.In` 已存在并可 lower 到 `InSubqueryExpression`。
- 已新增 internal `Expression.toInSubqueryExpression(...)` / `toNotInSubqueryExpression(...)` 及 `KSelectable` / `SelectQueryRef` 变体 helper。
- `CriteriaToAstConverter` 已能承接结构化 query IN / NOT IN。
- compiler plugin 已支持 `field in KSelectable` / `field !in KSelectable`。
- compiler official box 只覆盖底层转换契约：`field in/!in SelectClause` 生成 `CriteriaSubqueryValue.In`，并携带 `KSelectableQueryRef`，不在 compiler 模块断言 SQL。
- compiler official box 已覆盖 `[field1, field2] in/!in SelectClause` 生成 `CriteriaSubqueryValue.In(value = List<Field>, not = ...)`。
- core 普通测试覆盖 `select().where { field in/!in SelectClause }`、`update().where { field in/!in SelectClause }` 和 `delete().where { field in/!in SelectClause }` 的 `build()` SQL/参数输出。
- core 普通测试覆盖 `select/update/delete.where { [field1, field2] in/!in SelectClause }` 的 `build()` SQL/参数输出。
- core 普通测试覆盖逻辑删除转 `UPDATE` 分支中的 `delete().where { field in SelectClause }`，确认子查询参数不会在手写 update 渲染路径丢失。
- `DeleteClause.filterEmptyCriteria` 已保留所有 `CriteriaSubqueryValue` fieldless criteria，避免普通 delete 丢弃 tuple IN、EXISTS、后续 scalar/quantified 等结构化子查询条件。
- `SelectClause.renderStatement`、`UpdateClause.renderStatement`、`DeleteClause.renderStatement` 和 `DeleteClause` 逻辑删除 update 分支已在渲染前用同一份 parameter map lower deferred subquery，子查询 where 参数能透传到外层 query/action task。
- `SqlRendererTest.testInSubquery` 和 `SubqueryRendererTest` 覆盖了部分 IN subquery 渲染。
- `SubqueryValidator.validateInSubquery` 已在 lowering 阶段校验左侧表达式列数与子查询 select item 数量一致。
- `SubqueryRendererTest` 已覆盖 scalar/tuple IN 子查询列数不匹配错误。
- `MysqlSubqueryDslTest` 已覆盖用户 DSL tuple IN 子查询列数不匹配错误。

剩余工作：

- 更多错误形态诊断仍可增强，例如错误信息关联 DSL/source 位置。

验收：

- 单列 `IN (SELECT ...)` / `NOT IN (SELECT ...)` renderer 单测通过。
- row-value tuple 左右列数不匹配时报错；普通单列 IN 右侧多列也会报错。

## 任务 8（100%）：补 row-value tuple 表达式（已完成）

目标支持：

```kotlin
[it.userId, it.createTime] in Order()
    .select { [it.userId, f.max(it.createTime)] }
    .groupBy { it.userId }
```

core AST 建议新增：

```kotlin
data class RowValueExpression(
    val values: List<Expression>
) : Expression
```

renderer 渲染为：

```sql
(col1, col2)
```

注意：

- 单元素 tuple 不允许；单列应使用普通 `field in query`。
- row-value tuple 右侧 query 的 select item 数量必须匹配。
- 数量校验可以先在 builder/helper 层做，compiler 后续补类型和诊断。

当前落地情况：

- `RowValueExpression` 已存在并要求至少两个 expression。
- `AbstractSqlRenderer` 已支持 row value 渲染。
- `SubqueryLowering` 已递归 lower row value 内部表达式。
- `SubqueryRendererTest` 覆盖 row-value tuple IN 和单元素拒绝。
- compiler/core 已支持用户侧 `[field1, field2] in query` 的最小竖切：compiler handoff 为 `List<Field>`，core converter 生成 `RowValueExpression`。

剩余工作：

- 右侧 query select item 数量匹配校验仍需在 builder/helper 或 compiler 层补齐。

验收：

- `(a, b) IN (SELECT x, y FROM ...)` 渲染通过。
- 单元素 row-value tuple helper 报错或拒绝创建。

## 任务 9（50%）：派生表包装与外层过滤（部分完成）

目标让 core 能表达：

```sql
SELECT q.id, q.name, q.lastOrderAmount
FROM (
    SELECT u.id, u.name, (...) AS lastOrderAmount
    FROM user u
    WHERE u.status = ?
) q
WHERE q.lastOrderAmount > ?
```

需要补工具：

```kotlin
fun SelectStatement.asDerivedTable(alias: String): SubqueryTable

fun SelectStatement.wrapWithOuterFilter(
    alias: String,
    outerWhere: Expression? = null,
    outerHaving: Expression? = null,
    outerOrderBy: MutableList<OrderByItem>? = null
): SelectStatement
```

还需要补外层 select list 生成：

```kotlin
fun SelectStatement.projectFromAlias(alias: String): MutableList<SelectItem>
```

字段名规则：

- `ColumnSelectItem(alias = null)`：外层字段名取原 column name。
- `ColumnSelectItem(alias = "x")`：外层字段名取 `x`。
- `ExpressionSelectItem(alias = "x")`：外层字段名取 `x`。
- 没有 alias 的 expression select item 不适合作为外层字段引用，应报错或由 builder 生成内部 alias。

当前落地情况：

- 已有 `SelectStatement.asDerivedTable(alias)`。
- 已有 `SelectStatement.projectFromAlias(alias)`。
- 已有 `SelectStatement.wrapWithOuterFilter(...)`。
- 无 alias 的 expression select item 会报错； all-columns select item 不展开时会报错。
- `SubqueryRendererTest` 覆盖外层 where/orderBy 渲染。
- `SelectConditionLayering.applyAutomaticLayering(...)` 已能复用 wrapper 将 selected/aggregate/window alias 条件和排序移到外层。

剩余工作：

- 该能力尚未接入全部 SelectClause build 流程。
- 方言矩阵已覆盖主要 CTAS / insert-select / subquery order/update/delete / expression upsert 分支；更多 builder 组合仍需补。
- 外层支撑投影和隐藏字段策略尚未实现。

验收：

- 可以把任意 `SelectStatement` 包成派生表外层 select。
- outer where / outer order by 能正常渲染。
- MySQL、PostgreSQL、SQLite、SQL Server、Oracle renderer 都不依赖同层 select alias。

## 任务 10（50%）：条件来源与自动分层执行（部分完成）

任务 5 已部分前移 alias registry 和字段来源 metadata。本任务负责把 metadata 用到 statement build 中，形成实际分层。

第一版分层规则：

- `SOURCE` 条件可以下推到 inner where / inner having。
- `SELECTED`、`AGGREGATE`、`WINDOW` 条件默认进入 outer where。
- `UNKNOWN` 先保守留在原位置或要求调用方明确来源。

当前落地情况：

- 已新增 `SelectConditionLayering`，可基于 `SelectStatement.aliasRegistry` / metadata 分析字段来源。
- `SOURCE` 条件可保留在 inner where；`SELECTED`、`AGGREGATE`、`WINDOW` alias 条件和排序可包装为 outer query。
- `SubqueryRendererTest` 已覆盖 selected/aggregate alias 自动分层示例。
- `where` / `having` 在 `SelectClause` 中仍主要直接写入 `statement.where` / `statement.having`，尚未全面自动调用分层工具。

剩余工作：

- 在 SelectClause statement build 中基于 metadata 自动调用分层。
- 补齐复杂条件树、having、窗口函数、隐藏支撑投影的覆盖。

验收：

- builder 能把一组条件拆成 inner where / outer where。
- `having` 中引用聚合 alias 时不依赖数据库同层 alias 支持。

## 任务 11（74%）：补标量子查询公共校验（部分完成）

标量子查询的规则是全局规则，不只属于 `UPDATE SET`：

- select item 标量子查询；
- where / having 中的 scalar comparison；
- update set scalar subquery；
- upsert set scalar subquery；
- insert value scalar subquery。

公共校验规则：

- 标量子查询必须只选择一列。
- 聚合且无 `groupBy` 的标量子查询可以不写 `.limit(1)`。
- 其他非聚合标量子查询必须显式 `.limit(1)`。
- 唯一键证明可以后续增强，不作为第一版主规则。
- `limit(1) as T` 只是类型提示，不改变 SQL，也不能绕过单列/单行校验。

当前落地情况：

- 已有 `SubqueryValidator.validateScalar`。
- 已校验 select item 数量必须为 1。
- 聚合且无 groupBy 可免 `limit(1)`；其他非聚合要求 `limit(1)`。
- `SubqueryLowering` 对 deferred scalar 和 concrete scalar subquery 都会调用公共校验。
- 已有 `SubqueryValidator.validateInSubquery`。
- `SubqueryLowering` 对 deferred/concrete IN subquery 都会校验左右列数。

剩余工作：

- 错误报告目前是 `require` 异常文本，尚不能指向 DSL/source 对应 query expression。
- update/upsert/insert 等 scalar 使用场景还需继续核验是否都经过统一 lowering/validation。
- 唯一键证明和类型提示规则尚未增强。

验收：

- 所有 scalar subquery lowering 统一经过同一套校验。
- 所有 IN subquery lowering 统一经过左右列数校验。
- 错误报告位置能指向对应 query expression。
- 不同使用场景不会各自复制一套 limit 规则。

## 任务 12（88%）：扩展 `CriteriaToAstConverter`（部分完成）

后续条件表达式需要能承载：

- scalar subquery RHS；
- `exists(query)`；
- `field in query`；
- `field !in query`；
- quantified comparison：`any(query)` / `some(query)` / `all(query)`；
- row-value tuple `IN`。

core 侧需要为 converter 预留并补充对应 branch。compiler/plugin 后续负责把 DSL 表达式转成这些 Criteria/AST 输入。

当前落地情况：

- `CriteriaToAstConverter` 能处理普通比较、普通集合 `IN`、`BETWEEN`、`LIKE`、`IS NULL`、raw SQL 等。
- 已新增内部 `CriteriaSubqueryValue`，可承接 scalar RHS、exists、query IN / NOT IN、row tuple IN、quantified comparison。
- `CriteriaToAstConverter` 已能把 tuple IN 的 `List<Field>` handoff 转成 `RowValueExpression`。
- `ConditionType.SQL` 仍可透传已有 `Expression`，也可承接 structured exists。
- compiler official box 已覆盖 `field in/!in SelectClause` 生成 `CriteriaSubqueryValue.In(KSelectableQueryRef)`。
- compiler official box 已覆盖 `[field1, field2] in/!in SelectClause` 生成 `CriteriaSubqueryValue.In(value = List<Field>, not = ...)`。
- compiler official box 已覆盖 `exists(query)` / `!exists(query)` 生成 `CriteriaSubqueryValue.Exists(KSelectableQueryRef)`，并正确记录 `not` 标记。
- compiler official box 已覆盖 `field > SelectClause.limit(1)` 生成 `CriteriaSubqueryValue.Scalar(KSelectableQueryRef)`。
- compiler official box 已覆盖 `field > any(query)` 生成 `CriteriaSubqueryValue.QuantifiedComparison(KSelectableQueryRef, ANY)`。

剩余工作：

- 保持现有普通条件转换不回归。
- 补 `some(query)`、`all(query)`、更多比较操作符和错误形态诊断的专项测试。

验收：

- converter 能处理已有普通条件，不发生回归。
- 新增 subquery 条件 AST 能被 converter 或 helper 生成。

## 任务 13（70%）：补 `ORDER BY` 子查询和表达式排序承接（部分完成）

覆盖 spec 场景 8。

core 需要支持：

- `orderBy` receiver 使用 `Context`。
- `orderBy` 可以引用 source 字段。
- `orderBy` 可以引用 selected alias / 计算字段 / 聚合 alias / 窗口 alias。
- `orderBy` 可以承接标量子查询或函数表达式。
- 当目标数据库不能稳定在同层排序某个投影字段时，builder 可以复用派生表 wrapper。

当前落地情况：

- AST 层 `OrderByItem.expression` 可承载任意 `Expression`，`SubqueryLowering` 也会 lower orderBy item。
- `SelectStatement.wrapWithOuterFilter` 已支持传入 `outerOrderBy`。
- `SubqueryRendererTest` 已覆盖 order by scalar subquery 和 selected alias。
- `SelectConditionLayering` 可把 selected/aggregate/window alias 排序移动到 outer order by。
- `KTableForSort` 已新增 expression/scalar subquery sort item 承载，保留 `sortedFields` 兼容旧字段排序路径。
- `SelectClause.orderBy` 已能把 expression/scalar subquery sort item 转成 `OrderByItem(Expression, SortType)`。
- compiler official box `sort/scalarSubquerySortItem.kt` 已覆盖 `KSelectable.desc()` 进入 expression sort item 的底层 handoff。
- core 普通测试 `MysqlSubqueryDslTest` 已覆盖 `orderBy { addSortSubquery(query.limit(1), DESC) }` 的 SQL/参数输出。

剩余工作：

- 聚合 alias、窗口 alias 和 selected alias 的用户侧 builder/compiler 接入仍不完整。
- 计算字段排序还需要和 operator/function expression handoff 统一。
- 需要结合自动分层决定同层或外层排序。

验收：

- `ORDER BY q.alias DESC` 外层排序可渲染。
- 标量子查询 order item 可渲染。
- source 字段排序不要求该字段进入最终 `Selected`，但如果需要外层排序，builder 必须保证外层可引用字段存在或生成内部支撑投影。

## 任务 14（65%）：补 `UPDATE SET` 标量子查询承接（部分完成）

覆盖 spec 场景 9。

core 需要支持：

- `UpdateStatement` 的 assignment value 可以是 scalar subquery expression。
- scalar subquery 仍使用延迟 query ref，最终在 update statement build 阶段 materialize。
- 右侧 query 可以引用被更新行的字段，形成相关 update。
- 非聚合 scalar subquery 的 `.limit(1)` 校验复用任务 11 的公共规则。

当前落地情况：

- AST `Assignment.value` 是 `Expression`，renderer 可渲染表达式值，这提供了底层承载可能。
- `SubqueryLowering` 已 lower update assignments。
- `SubqueryRendererTest` 已覆盖 `UPDATE ... SET col = (SELECT ... LIMIT 1)`。
- `UpdateClause.set {}` / `patch(...)` 已可把 `KSelectable` / `SelectQueryRef` / `Expression` RHS 转成 builder expression，用于 `UPDATE SET col = (SELECT ...)`。
- core 普通测试已覆盖 `update().patch("field" to selectable.limit(1)).where { ... }.build()` 和 `.set { setValue(field, selectable.limit(1)) }` 的 SQL/参数输出。

剩余工作：

- 相关 update 外层字段引用未处理。
- compiler plugin 低层 set box 已覆盖 `setValue(field, selectable)` 会把 `KSelectable` RHS 原样收集到 `fieldParamMap`；最终 `it.field = selectable` 语法仍需 FIR/类型系统承接。

验收：

- `SET col = (SELECT ... LIMIT 1)` 可渲染。
- MySQL、PostgreSQL、SQLite、SQL Server、Oracle 方言不暴露给用户 DSL。

## 任务 15（91%）：补 `UPDATE` / `DELETE WHERE` 子查询承接（部分完成）

覆盖 spec 场景 10。

core 需要在 update/delete 条件中复用：

- `IN` / `NOT IN` 子查询；
- `EXISTS` / `!EXISTS`；
- scalar comparison；
- quantified comparison；
- row-value tuple `IN`。

当前落地情况：

- select 侧已有部分 subquery AST/render/lowering 地基。
- update/delete 条件可通过 AST 表达 exists/in/scalar，并由 renderer 前 lowering 处理。
- `CriteriaToAstConverter` 已支持结构化 subquery 条件输入。
- `SubqueryRendererTest` 已覆盖 update/delete where subquery 渲染。
- `field in query` / `field !in query` 的 compiler condition box 已覆盖底层 Criteria 转换；core DSL 测试已覆盖 update/delete where 的 SQL/参数输出，包含普通 delete 和逻辑删除转 update 分支。
- `exists(query)` / `!exists(query)` 的 compiler condition box 已覆盖底层 Criteria 转换；core DSL 测试已覆盖 update/delete where 的 SQL/参数输出。
- tuple IN/NOT IN 的 compiler handoff 已通，core DSL 已覆盖 select/update/delete where tuple IN/NOT IN 的 SQL/参数。
- scalar comparison 的 compiler handoff 已通，core DSL 已覆盖 update/delete where `field > query.limit(1)` 的 SQL/参数。
- quantified comparison 的 compiler handoff 已通，core DSL 已覆盖 update/delete where `field > any(query)` / `field <= all(query)` 的 SQL/参数。
- `DeleteClause.filterEmptyCriteria` 已保留所有 `CriteriaSubqueryValue` fieldless criteria，普通 delete 不再误删 row tuple IN/NOT IN 条件。

剩余工作：

- `UpdateClause` / `DeleteClause` 的 query ref、deferred lowering 已有基本竖切，相关 source 字段上下文仍未完整处理。
- 仍需补 `some(query)`、更多比较操作符和错误形态诊断。

验收：

- `UPDATE ... WHERE EXISTS (...)` 可渲染。
- `DELETE ... WHERE id IN (SELECT ...)` 可渲染。
- 条件子查询可以引用 update/delete 的 source 字段。

## 任务 16（87%）：补 `INSERT SELECT`（部分完成）

覆盖 spec 场景 11。

core 需要提供 `KSelectable` 作为 insert source 的承接能力：

```kotlin
KSelectable<SourceProjection>.insert<Target> { ... }
```

核心规则：

- `insert<Target>` 的 lambda receiver 是源 query 的 `Selected`。
- 插入值按目标表可插入字段顺序映射。
- `null`、常量、函数表达式、源投影字段、标量子查询都可以作为插入值。
- 源 query 可以是普通 select、join select、union 或派生查询；普通 select / join select 使用 `KSelectable`，union 使用 `UnionClause`。

当前落地情况：

- `InsertStatement` 已新增 `source: Statement?` 承载 `INSERT ... SELECT`，renderer/lowering 支持 `SelectStatement` 与 `UnionStatement`。
- `AbstractSqlRenderer.renderInsertStatement` 已支持 source query。
- `SubqueryLowering` 已 lower insert source。
- `SubqueryRendererTest` 已覆盖 `INSERT INTO ... SELECT ...`。
- 已新增 `KSelectable<*>.insert<Target>()` core builder 入口，生成 `InsertStatement.source`。
- 已新增 `UnionClause.insert<Target>()` core builder 入口，生成 union source 的 `InsertStatement.source`。
- `InsertClause` materialize source query 时会复用外层 parameter map，source `where` 参数可进入最终 action task。
- core 普通测试已覆盖普通 select source、join source、union source 的 `insert<Target>().build()` SQL/参数输出，并验证 union 分支参数重命名。
- `insert<Target> { [...] }` 的显式 values 会按目标可插入字段顺序重写 source query 的 select list，已覆盖源字段、`NULL`、常量参数、函数表达式和标量子查询参数输出。
- 默认 `insert<Target>()` 会校验 source select item 数量与目标可插入字段数量一致，避免生成必然失败的 `INSERT ... SELECT`。

剩余工作：

- 补目标字段类型兼容校验。

验收：

- `INSERT INTO target (...) SELECT ...` 可由 AST 表达并渲染。
- `INSERT INTO target (...) (SELECT ...) UNION (SELECT ...)` 可由 AST 表达并渲染。
- 源 query 如因投影过滤需要派生表，insert source 能消费该派生结果。

## 任务 17（70%）：补 `UPSERT` 子查询表达式（部分完成）

覆盖 spec 场景 12。

core 需要支持 upsert conflict update 阶段的 assignment value 为 scalar subquery：

- `upsert().set { field = KSelectable }` 的 core 表达能力。
- 子查询可以引用 upsert target/source 字段。
- 方言差异由现有 upsert renderer 或 support 层处理。

当前落地情况：

- `UpsertClause` 当前仍围绕字段选择、参数和现有 insert/update 任务构建。
- `InsertStatement` 已新增 `conflictAssignments: List<Assignment>` 承载 expression-based upsert assignment。
- `AbstractSqlRenderer.renderConflictAssignments` 已支持 MySQL、PostgreSQL、SQLite 的基础渲染。
- `SubqueryRendererTest` 已覆盖 MySQL upsert scalar subquery assignment。
- `UpsertClause.set {}` 已新增与 `UpdateClause.set {}` 对齐的 ToSet 入口，能把 `KSelectable` / `Expression` RHS 收集为 conflict assignment。
- `UpsertClause.patch(...)` 已可收集 `KSelectable` / `Expression` conflict assignment，并在 expression assignment 场景走 AST upsert 渲染。
- expression upsert 分支已在渲染前用共享 `QueryMaterializeContext` lower statement，scalar subquery 参数能进入最终 action task。
- core 普通测试已覆盖 MySQL `upsert().patch("field" to selectable.limit(1)).onConflict().build()` 和 `.set { setValue(field, selectable.limit(1)) }` 的 SQL/参数输出。
- 非 `onConflict()` 的先查后改 fallback upsert 已保留 `patch` 中的 `KSelectable` / `Expression` RHS，并在更新分支生成 scalar subquery assignment。
- MSSQL / Oracle native expression upsert 当前明确不支持并抛错；这不影响既有 fallback upsert 机制。

剩余工作：

- 补 PostgreSQL/SQLite builder 级测试，并为 MSSQL/Oracle 单独设计或保持明确不支持。
- compiler plugin 低层 set box 已覆盖 `setValue(field, selectable)` 会把 `KSelectable` RHS 原样收集到 `fieldParamMap`；最终 `it.field = selectable` 语法仍需 FIR/类型系统承接。

验收：

- PostgreSQL `ON CONFLICT DO UPDATE SET col = (SELECT ...)` 可表达。
- MySQL `ON DUPLICATE KEY UPDATE col = (SELECT ...)` 可表达。
- 不引入用户可见 `setSubquery(...)`。

## 任务 18（90%）：补 `CREATE TABLE AS SELECT`（部分完成）

覆盖 spec 场景 13。

core 需要扩展现有 DDL 入口，让 `createTable(target, query)` 可表达 CTAS：

```kotlin
dataSource.table.createTable(target)
dataSource.table.createTable(target, query)
```

核心规则：

- 第一个参数是目标 KPojo。
- 第二个参数是 query source：普通 select / join select 使用 `KSelectable`，union 使用 `UnionClause`。
- 源 query 的最终 `Selected` 作为 CTAS select list。
- 是否完整保留 schema 取决于方言；如需完整 schema，推荐 `createTable(target)` + `query.insert<Target> { ... }`。

当前落地情况：

- `TableOperation.createTable` / `buildCreateTableStatement` 当前是普通 schema create。
- `DdlStatement.CreateTableAsSelectStatement` 已新增，携带 `SelectStatement` source。
- `AbstractSqlRenderer` 已提供默认 CTAS 渲染。
- `SubqueryLowering` 已 lower CTAS query。
- `SubqueryRendererTest` 已覆盖 MySQL 风格 CTAS。
- `TableOperation.createTable(target, query)` 和 `buildCreateTableAsSelectStatement(target, query)` 已新增。
- `buildCreateTableAsSelectTask(target, query)` 已新增，CTAS source query 使用共享 parameter map materialize，并把 source `where` 参数合入最终 action task。
- core 普通测试已覆盖 MySQL 普通 select source 的 `CREATE TABLE ... AS SELECT ... WHERE ...` SQL/参数输出。
- core 普通测试已覆盖 MySQL join source 的 `CREATE TABLE ... AS SELECT ... LEFT JOIN ... WHERE ...` SQL/参数输出。
- core 普通测试已覆盖 MySQL union source 的 `CREATE TABLE ... AS (SELECT ...) UNION (SELECT ...)` SQL/参数输出，并验证 union 分支参数重命名。
- `SelectFrom` 实现了 `KSelectable`，因此 join source 可直接作为 CTAS source 消费。
- `CreateTableAsSelectStatement.query` 已放宽为 query statement，renderer/lowering 支持 `SelectStatement` 与 `UnionStatement`；`TableOperation` 已提供 `UnionClause` CTAS builder 入口。
- MSSQL 使用 `SELECT ... INTO [dbo].[table]`，Oracle 使用 `CREATE TABLE NAME AS SELECT ...` 且拒绝 `IF NOT EXISTS`。

剩余工作：

- 补 schema 保留策略说明和更多 builder 测试。
- SQL Server 的 `SELECT INTO` CTAS 暂只支持单个 `SelectStatement` source；union source 需要单独设计或保持明确不支持。
- Oracle `ifNotExists = false` 的用户入口或调用策略需要在上层明确。

验收：

- `CREATE TABLE target AS SELECT ...` 可渲染。
- 普通 select、join select、union 等 query source 都可作为 CTAS source。

## 任务 19（95%）：补 core AST / renderer 测试（部分完成）

先用 core 单测验证，不等 compiler plugin。

建议测试：

- scalar subquery in select list；
- nested scalar subquery；
- exists；
- not exists；
- in subquery；
- not in subquery；
- row tuple in subquery；
- derived table outer where；
- derived table outer order by；
- quantified comparison：`ANY` / `SOME` / `ALL`。
- order by scalar subquery / selected alias；
- update set scalar subquery；
- update/delete where subquery；
- insert select；
- upsert scalar subquery；
- create table as select。

五种数据库 renderer 至少覆盖：

- MySQL
- PostgreSQL
- SQLite
- SQL Server
- Oracle

当前落地情况：

- 已有 `kronos-core/src/test/kotlin/com/kotlinorm/ast/SubqueryRendererTest.kt`。
- 已覆盖 scalar subquery in select list、nested scalar、exists/not exists、IN/NOT IN、row tuple in subquery、derived table outer where/orderBy、deferred scalar lowering、quantified comparison、order by scalar/alias、criteria converter、automatic layering、update/delete where、update set scalar、insert select、upsert scalar、CTAS。
- 已补五方言 renderer 矩阵，覆盖 CTAS、insert-select、update/delete/order subquery，以及 MySQL/PostgreSQL/SQLite expression upsert。
- 不再以零散 builder shape 测试作为完成依据；子查询场景验收应优先使用“用户 DSL -> clause -> SQL/参数”的完整竖切测试。
- `SqlRendererTest` 中有基础 `InSubqueryExpression` 渲染测试。
- `MysqlSubqueryDslTest` 已覆盖 `select/update/delete where field in/!in SelectClause`、select/update/delete where tuple IN/NOT IN、update/delete where scalar comparison、update/delete where quantified comparison、逻辑删除转 update 的 IN 子查询、select-list scalar subquery、order-by scalar subquery、`select/update/delete where exists/!exists`、update-set scalar subquery（patch 与 setValue 路径）、insert-select 普通 select / join source / union source、insert-select 显式 values 映射与列数校验、upsert scalar subquery（patch 与 setValue 路径），以及 CTAS 普通 select / join source / union source 的 SQL/参数输出。
- `FunctionBuildersTest` 已覆盖自定义 `FunctionBuilder` 可通过 `selectItemScope` 动态声明函数 select item scope，避免把聚合/窗口函数识别写死在 select builder 中。
- 已知 `:kronos-core:test` 已通过。

剩余工作：

- compiler official box 测试目前覆盖了 scalar comparison、`field in query` / `field !in query`、tuple IN/NOT IN、`exists` / `!exists` 的 Criteria 转换契约，`KSelectable.as_` 作为 scalar select item 的收集契约，`KSelectable.desc()` 作为 scalar sort item 的收集契约，以及 `setValue(field, selectable)` 的 KSelectable RHS 收集契约；core 普通测试覆盖了 select/update/delete where、select/update/delete tuple IN/NOT IN、update/delete scalar comparison、select-list scalar subquery、order-by scalar subquery、update/upsert setValue scalar subquery 的 SQL/参数输出。其他 DSL 场景仍需按同样边界补测试。
- insert-select 字段类型兼容校验、真实 Context 接入后的 builder 行为仍需补测试。

验收：

```powershell
.\gradlew.bat :kronos-core:test --no-daemon --console=plain
```

在当前 compiler plugin 未完成时，如果 `:kronos-core:test` 被跨模块编译阻断，应至少跑 core AST 测试所在的最小 task，并记录阻断原因。

## 暂不做

- 暂不实现 compiler plugin 对 `Context` 的生成与 receiver 替换。
- 暂不实现 window function DSL。
- 暂不设计用户可见的 `inSubquery` / `setSubquery` / `asTable`。
- 暂不引入 CTE。

## 推荐实施顺序

1. 补齐 `where` / `having` / `orderBy` 使用 `Context` 的 core receiver。
2. 视兼容风险收敛 `KSelectable` 为结果投影语义，或先完成所有 `selectedKClass` 规则测试。
3. 前移 alias registry 与字段来源 metadata。
4. 把派生表包装工具接入自动分层。
5. 接 select item scalar subquery 的 core builder 和 alias 化。
6. 补 `IN` / `EXISTS` / quantified comparison / row tuple 的 helper 与 criteria converter。
7. 扩展 lowering 到 update/insert/upsert/DDL 等非 select statement。
8. 补 spec 场景 8-13 对应 core 承接能力。
9. 补完整 core renderer 和 builder 单测。
10. 最后再让 compiler plugin 生成对应调用。
