# Kronos Projection Work Status

更新日期：2026-07-02

## 当前目标

基础 `select` 投影链路已经打通并进入稳定期。当前目标从“生成投影类并让查询返回它”进一步转为“让投影结果、后续子句 Context、子查询和窗口函数在同一条 DSL 管线内闭合”。

当前可用的基础形态是：

```kotlin
val idRows = user.select { it.id }.queryList()
idRows.firstOrNull()?.id

val rows = user.select { [it.id, it.name] }.queryList()
rows.firstOrNull()?.name
```

`select` lambda 的 `it` 仍然是完整源 KPojo，`queryList/queryOne/queryOneOrNull` 返回生成的投影 KPojo 类型。当前 FIR refined 类型已经可以生成 `SelectClause<Source, Selected, Context>`，其中 `Context` 是“Source 全字段 + Selected 投影字段”的生成类型；基础 `.alias("xx")` alias 已能进入 `Selected` 和 `Context`。按当前 spec，当前层 `where` / `having` 只接收 `Source`，只有当前层 `orderBy` 使用 `Context`；过滤投影 alias 必须进入下一层查询。

## 已完成的方向性改动

- FIR 侧已负责生成稳定的顶层 projection class，带 data-class 形态、`KPojo` 继承、可见属性和稳定命名。
- backend 侧已负责 materialize 真实 IR class，并补齐 `KPojo` fake overrides + body。
- `SelectClause<Source, Selected, Context>` 已在 core 落地，`queryList/queryOne/queryOneOrNull` 返回 `Selected`。
- `Patch.kt` 保留裸 `select { ... }`，并新增内部四参数 `selectGeneratedProjection(projectionClass, contextClass, fields)` 给编译器插件改写使用。
- `KronosProjectionIrTransformer` 已接入 `ErrorReporter`，并在 materialize 后补 fake overrides，再复用 `KronosIrClassTransformer` 生成 projection/context 方法体。
- `ProjectionBoxTest.generatedSelectProjection` 已通过，说明基础 projection 生成链路已经可用；生成类名使用稳定 mangle，不再每次变化。
- 旧的同层 `where/having` 访问当前层 alias 的正向 box 已删除或拆分；`ProjectionBoxTest.selectAliasContextOrderBy` 保留为当前层 `orderBy` Context 正向测试。
- `SelectClause.toStatement()` 已不再把同层 `where/having` alias 自动分层作为用户 DSL 主路径；SQL 字符串行为必须在 core 普通测试验证，compiler box 只验证 FIR/IR/AST/参数 handoff。
- core 子查询地基已有明显推进：`KSelectable<Selected>`、deferred subquery、lowering/validation、alias metadata、derived wrapper、IN/EXISTS/scalar/tuple/insert-select/upsert/CTAS 等底层能力已存在。
- compiler plugin 已补一批底层 handoff box：select-list scalar subquery、condition 中 `in/!in`、tuple IN、`exists/!exists`、scalar comparison、`any/some/all`、order-by selectable、`setValue(field, selectable)`。
- `.agents/skills/kronos-dev-kcp/Evolution.md` 已记录 FIR/IR 过程中遇到的主要坑。

## 当前最新状态

基础投影链路和字段 alias 的 `Selected` / `orderBy Context` 竖切已验收通过，不再停留在“找不到 projection class”或“orderBy 看不到基础 select alias”这类前端可见性问题上。当前最大缺口变成把同一套机制扩展到函数 alias、标量子查询 alias、聚合 alias、窗口 alias，以及 derived/update/upsert 等后续场景。

2026-07-02 最新补充：

- core 函数/聚合/算术/原生 SQL select 测试源码已适配“非直接 select item 必须显式 `.alias(...)`”的 compiler 规则；CUSTOM_CRITERIA_SQL 仍按 core 现有语义渲染原生片段本身，不进入用户可引用 alias registry。
- 已通过 `:kronos-core:compileTestKotlin`，并在 clean 后通过 targeted core renderer/DSL/function 测试：`SubqueryRendererTest`、`MysqlSubqueryDslTest`、`MysqlSelectTest`、`SelectClauseAstTest`、五方言 `*FunctionTest`。
- 非 clean 的 targeted core 测试曾因 shared `Kronos.init` 的 generated projection/context `kClassCreator` 映射未刷新而找不到 `KronosSelectContext_*`；对应 class 文件本身存在且有无参构造，clean 后通过，符合已记录的旧映射风险。
- scalar subquery diagnostics 已覆盖 select item 缺 alias、缺 `limit(1)`、多列投影，以及聚合无 `groupBy` 免 `limit(1)` 的规则；predicate subquery diagnostics 已覆盖单值 RHS 多列、tuple arity mismatch、单元素 tuple 和 `ANY/SOME/ALL` RHS 多列。
- 窗口函数第一条竖切已打通：`f.rowNumber().over { partitionBy(...); orderBy(...) }.alias("rn")` 可 lower 为 `FunctionField.over`，同层 `where` 访问 `rn` 为负例，同层 `orderBy { it.rn.asc() }` 正向通过，下一层 `where { it.rn == 1 }` core SQL 通过；`where` scope 直接写窗口 `over` 已由普通 Kotlin unresolved diagnostics 覆盖。
- 修复了 generated projection 作为下一层 `Source` 时 backend materializer 再次展开 FIR lazy class 导致 `IrConstructorSymbolImpl is unbound` 的崩溃；修复记录已写入 `.agents/skills/kronos-dev-kcp/Evolution.md`。
- MySQL core subquery DSL 全量用例已通过，覆盖 DML 子查询、insert-select、CTAS、derived source、join/union source 与窗口新增场景。

当前已经确认的关键结论：

- 不能只靠 backend IR 修补，FIR 必须先把 projection 类型声明出来，IDE/补全才看得到。
- 投影类不能再做成普通 class；需要 data-class 形态，并且带可无参构造的默认值路径。
- `SelectClause` 的结果类型和后续子句 scope 不是同一个概念，后续子查询和窗口函数会继续依赖这条分层；`Selected` 和 `Context` 需要分别生成。
- 现有 `SubqueryExpression` / `SubqueryTable` / `InSubqueryExpression` AST 已经在 core 里准备好，下一阶段应直接接入现有 `select` / `where` / `having` 管线，而不是新造一套割裂 DSL。
- 同层 `where/having` 不再消费 `Context`，窗口/聚合/标量子查询 alias 的过滤必须通过下一层 `KSelectable`。
- Window AST/rendering 和 block DSL 已有首个完整竖切；当前不新增参数式 `over(partitionBy = ..., orderBy = ...)` 语法，spec 已回到 `over { partitionBy(...); orderBy(...) }` 写法。

当前更适合接手的下一步不是继续补基础投影，而是：

1. 继续补 insert-select / CTAS 的字段类型兼容、更多方言 renderer 边界和真实数据库 integration 抽样。
2. 继续设计或实现 `set { field = query }` / `upsert().set { field = query }` 的最终类型安全语法；当前 `.patch` / `setValue` 动态入口已可工作。
3. 可补 `groupBy/having` 直接窗口函数非法位置重复矩阵；当前 `where` scope 已覆盖同类 DSL surface 限制。

## 重要注意事项

- 不要回到解析源码文本的方向。
- 不要仅靠 backend IR 手造用户可见类型；用户源码需要 `.id/.name` 在 FIR 前端可解析。
- 生成投影类应保持 data-class 形态，并保留默认值以支持 KPojo 无参构造路径。
- `SelectClause` 结果投影和子句 scope 必须分开，不要把它们重新揉成同一个模型。
- 不要用 backend 补丁绕过 `it.lastOrderAmount` / `it.rn` 的前端解析问题；这些属性必须由 FIR 生成。
- 子查询和窗口函数 DSL 应复用现有 `select/where/having/orderBy/set/insert/ddl` 管线，不设计另一套用户语法。
- `.tmp-kotlin-src/` 是为查看 Kotlin compiler sources jar 解压出的本地临时目录，不应提交。

## 下一步建议

1. 补 insert-select / CTAS 映射类型校验和更多方言 renderer 边界。
2. 补 DML typed assignment 语法方案；当前不要引入割裂的新用户 DSL。
3. 继续扩大 `kronos-testing` 真实数据库抽样，优先 derived query 外层过滤、DML 子查询、insert-select/CTAS。
4. 可补 `groupBy/having` 直接窗口函数非法位置 diagnostics 作为低风险重复矩阵。
