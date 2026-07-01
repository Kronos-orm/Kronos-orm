# Kronos Projection Work Status

更新日期：2026-07-01

## 当前目标

基础 `select` 投影链路已经打通并进入稳定期。当前目标从“生成投影类并让查询返回它”进一步转为“让投影结果、后续子句 Context、子查询和窗口函数在同一条 DSL 管线内闭合”。

当前可用的基础形态是：

```kotlin
val idRows = user.select { it.id }.queryList()
idRows.firstOrNull()?.id

val rows = user.select { [it.id, it.name] }.queryList()
rows.firstOrNull()?.name
```

`select` lambda 的 `it` 仍然是完整源 KPojo，`queryList/queryOne/queryOneOrNull` 返回生成的投影 KPojo 类型。当前 FIR refined 类型已经可以生成 `SelectClause<Source, Selected, Context>`，其中 `Context` 是“Source 全字段 + Selected 投影字段”的生成类型；基础 `.as_("xx")` alias 已能在后续 `where` / `having` / `orderBy` 中被 FIR 解析并交给 IR/core。

## 已完成的方向性改动

- FIR 侧已负责生成稳定的顶层 projection class，带 data-class 形态、`KPojo` 继承、可见属性和稳定命名。
- backend 侧已负责 materialize 真实 IR class，并补齐 `KPojo` fake overrides + body。
- `SelectClause<Source, Selected, Context>` 已在 core 落地，`queryList/queryOne/queryOneOrNull` 返回 `Selected`。
- `Patch.kt` 保留裸 `select { ... }`，并新增内部四参数 `selectGeneratedProjection(projectionClass, contextClass, fields)` 给编译器插件改写使用。
- `KronosProjectionIrTransformer` 已接入 `ErrorReporter`，并在 materialize 后补 fake overrides，再复用 `KronosIrClassTransformer` 生成 projection/context 方法体。
- `ProjectionBoxTest.generatedSelectProjection` 已通过，说明基础 projection 生成链路已经可用；生成类名使用稳定 mangle，不再每次变化。
- `ProjectionBoxTest.selectAliasContextWhere` 已通过，说明基础 `Context` FIR 可见性、运行时 context 实例化、alias 条件 AST/参数 handoff 已打通。
- `ProjectionBoxTest.selectAliasContextOrderByHaving` 已通过，说明基础 select alias 可作为 `having` filter 和 `orderBy` sort 的 generated Context 字段使用；compiler 测试只验证 AST/参数 handoff，不断言 SQL。
- `SelectClause.toStatement()` 已接入 `SelectConditionLayering.applyAutomaticLayering("q")`，core 的 `SubqueryRendererTest` 负责验证 SQL 自动分层。
- core 子查询地基已有明显推进：`KSelectable<Selected>`、deferred subquery、lowering/validation、alias metadata、derived wrapper、IN/EXISTS/scalar/tuple/insert-select/upsert/CTAS 等底层能力已存在。
- compiler plugin 已补一批底层 handoff box：select-list scalar subquery、condition 中 `in/!in`、tuple IN、`exists/!exists`、scalar comparison、`any/some/all`、order-by selectable、`setValue(field, selectable)`。
- `.agents/skills/kronos-dev-kcp/Evolution.md` 已记录 FIR/IR 过程中遇到的主要坑。

## 当前最新状态

基础投影链路和字段 alias 的 `Context` 竖切已验收通过，不再停留在“找不到 projection class”或“where/orderBy/having 看不到基础 select alias”这类前端可见性问题上。当前最大缺口变成把同一套机制扩展到函数 alias、标量子查询 alias、聚合 alias、窗口 alias，以及 derived/update/upsert 等后续场景。

当前已经确认的关键结论：

- 不能只靠 backend IR 修补，FIR 必须先把 projection 类型声明出来，IDE/补全才看得到。
- 投影类不能再做成普通 class；需要 data-class 形态，并且带可无参构造的默认值路径。
- `SelectClause` 的结果类型和后续子句 scope 不是同一个概念，后续子查询和窗口函数会继续依赖这条分层；`Selected` 和 `Context` 需要分别生成。
- 现有 `SubqueryExpression` / `SubqueryTable` / `InSubqueryExpression` AST 已经在 core 里准备好，下一阶段应直接接入现有 `select` / `where` / `having` 管线，而不是新造一套割裂 DSL。
- `SelectConditionLayering.applyAutomaticLayering(...)` 已接入 `SelectClause.toStatement()`；SQL 字符串行为必须在 core 普通测试验证，compiler box 只验证 FIR/IR/AST/参数 handoff。
- Window AST/rendering 已有一部分，例如 `WindowClause`、`WindowFrame`、`FunctionCall.over` lowering/rendering；用户 DSL 的 `f.rowNumber().over(...).as_("rn")` 还没完整实现。

当前更适合接手的下一步不是继续补基础投影，而是：

1. 扩展 `Context` 字段模型，让函数 alias、标量子查询 alias、聚合/window alias 都能进入 `Selected` 和 `Context`。
2. 做窗口函数 DSL 的第一条完整竖切：`f.rowNumber().over(...).as_("rn")` + 外层 `where { it.rn == 1 }`。
3. 继续补 derived join、insert-select 映射类型校验、`set { field = query }` / `upsert().set { field = query }` 的最终类型安全语法。

## 重要注意事项

- 不要回到解析源码文本的方向。
- 不要仅靠 backend IR 手造用户可见类型；用户源码需要 `.id/.name` 在 FIR 前端可解析。
- 生成投影类应保持 data-class 形态，并保留默认值以支持 KPojo 无参构造路径。
- `SelectClause` 结果投影和子句 scope 必须分开，不要把它们重新揉成同一个模型。
- 不要用 backend 补丁绕过 `it.lastOrderAmount` / `it.rn` 的前端解析问题；这些属性必须由 FIR 生成。
- 子查询和窗口函数 DSL 应复用现有 `select/where/having/orderBy/set/insert/ddl` 管线，不设计另一套用户语法。
- `.tmp-kotlin-src/` 是为查看 Kotlin compiler sources jar 解压出的本地临时目录，不应提交。

## 下一步建议

1. 把 alias/source metadata 与 `Context` 字段模型继续对齐，重点是函数、聚合、窗口和 scalar subquery select item。
2. 补 aggregate / window / scalar subquery alias 的 `orderBy`、`having` 用户 DSL 竖切测试；compiler 层验证 AST/参数 handoff，SQL 分层留在 core。
3. 做窗口函数 DSL 的第一场景：`ROW_NUMBER() OVER (...) AS rn`，并支持外层 `where { it.rn == 1 }`。
4. 继续补 derived join、insert-select 映射类型校验、`set { field = query }` / `upsert().set { field = query }` 的最终类型安全语法。
