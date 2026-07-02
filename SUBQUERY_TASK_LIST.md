# Kronos 子查询实现任务清单

更新时间：2026-07-02

本文档按当前 `SUBQUERY_DSL_SPEC.md` 刷新，是实现计划和验收清单，不再沿用旧版“同层 `where/having` 可访问 `Selected` alias 并自动分层”的设计。

## 设计锁定点

- `Source`：当前查询层 FROM / JOIN 暴露给 DSL 的输入类型。
- `Selected`：当前查询层 `select { ... }` 生成的结果类型。
- `Context`：`Source + Selected` 合成上下文，只用于当前层 `orderBy`。
- `KSelectable<Selected>`：查询对象，被下一层消费时，上一层 `Selected` 成为下一层 `Source`。
- `KPojo.where { ... }` 是语法糖，等价于 `KPojo.select().where { ... }`，结果仍是 `KSelectable<Source>`。
- 当前层 `where/groupBy/having` 的 receiver 是 `Source`，不能访问当前层 `Selected` alias。
- 当前层 `orderBy` 的 receiver 是 `Context`，可以访问 `Source` 字段和当前层 `Selected` 字段。
- 需要过滤窗口字段、聚合 alias、标量子查询 alias 时，必须进入下一层查询。
- `.alias("name")` 是新的命名 API，旧命名 API 不保留兼容。
- `select { ... }` 中直接字段投影可继承字段名，非直接字段投影必须显式 `.alias("name")`。
- 同一层 `Selected` 最终字段名必须唯一；新增 `Selected` 字段与 `Source` 字段同名导致 `Context` 生成失败。
- `[]` 是用户侧统一列表语法，由编译器按上下文解释为投影列表、排序列表、窗口字段列表或 row-value tuple。

## 总览

| 进度 | 任务                                     | 状态       | 说明                                                                                                                                                                                                                                          |
|------|------------------------------------------|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 90%  | 任务 1：旧实现和旧测试清理               | 进行中     | 旧命名 API 正向入口、旧同层 alias box、默认自动分层正向测试已清理；core 函数/原生 SQL select 测试已补 `.alias(...)`；源码/docs/guide/testData 范围静态扫描已确认无旧 `as_` 正向残留。                                                     |
| 75%  | 任务 2：查询层类型模型重置               | 进行中     | core `where/having` 已改回 `Source`，`orderBy` 保持 `Context`；compiler box/diagnostics 已验证 Source receiver、orderBy Context、同层 alias 负例。                                                                                             |
| 100% | 任务 3：`KPojo.where` 语法糖             | 已完成     | 已实现 `KPojo.where { ... } = select().where(...)`，core 等价测试和 compiler box 返回类型测试通过。                                                                                                                                           |
| 92%  | 任务 4：`KSelectable` 作为下一层 Source  | 进行中     | `KSelectable<S>.select {}` 与 `join(KSelectable<S>)` 竖切已打通，derived table 在 lowering 阶段 materialize，字段 remap 到 `q` 且参数可共享；`DslIntegrationBoxTest` 已重验下一层 Source 与 join selectable。                                 |
| 75%  | 任务 5：alias API 与命名诊断             | 进行中     | `.alias` 正向 API/编译器识别已迁移，旧命名 API 正向入口已删除；core 函数/聚合/算术投影测试已显式 alias；FIR checker 已覆盖 `[]` collection literal 与 FIR `listOf(...)` 降级形态。                                                           |
| 75%  | 任务 6：receiver 签名与 compiler refine  | 进行中     | `where/having=Source`、`orderBy=Context`、下一层 `KSelectable<S>.select` 已过 compiler box/diagnostics；窗口 alias orderBy 和同层 where 负例已补，更多 scalar/aggregate 边界仍待补。                                                          |
| 75%  | 任务 7：同层 where/having alias 能力删除 | 进行中     | 旧正向 box 和默认自动分层正向测试已删除；同层 `where` / `having` 访问当前层 alias 的首批 diagnostics 已通过。                                                                                                                                 |
| 85%  | 任务 8：orderBy Context                  | 进行中     | `.alias` 后的 selected/function/scalar/window alias orderBy 正向已过 compiler box；Context 冲突诊断已覆盖首批 Source 字段冲突，distinct/group 方言边界仍待补。                                                                                |
| 75%  | 任务 9：标量子查询                       | 进行中     | AST/lowering 多数可用，select item / where / orderBy scalar subquery core 路径已随 targeted core 测试重验；select item alias、单列、`.limit(1)` compiler diagnostics 已有首批覆盖，聚合无 `groupBy` 免 `.limit(1)` 已按表达式层规则处理。 |
| 80%  | 任务 10：谓词子查询                      | 进行中     | `IN/EXISTS/ANY/ALL/tuple IN` 地基可用；单值 RHS 多列、tuple 左右列数不一致、单元素 tuple、`ANY/SOME/ALL` RHS 多列已有 compiler diagnostics；core renderer 继续覆盖 build/runtime validator。                                               |
| 95%  | 任务 11：窗口函数与下一层过滤            | 进行中     | `over { partitionBy/orderBy }` 已能 lowered 为 `FunctionField.over`；窗口 alias 同层 where 负例、同层 orderBy 正向、下一层过滤 core SQL、where 直接窗口函数非法位置 diagnostics 均已覆盖。                                                  |
| 78%  | 任务 12：DML 子查询                      | 进行中     | MySQL core DSL 已覆盖 update/delete 的 IN、tuple IN、EXISTS、ANY/ALL、scalar comparison，以及 update/upsert scalar assignment；typed assignment 语法、更多方言和 compiler handoff 仍待补。                                                    |
| 75%  | 任务 13：INSERT SELECT 与 CTAS           | 进行中     | MySQL core DSL 已覆盖普通/derived/join/union source 的 insert-select 与 CTAS，以及显式值/函数/标量子查询值；字段类型兼容、更多方言和 compiler handoff 仍待补。                                                                                |
| 60%  | 任务 14：测试矩阵重建                    | 进行中     | core/compiler/integration 的职责边界已开始拆分；projection diagnostics、projection/select box、condition box、dslIntegration、integration 测试源码编译、core 测试源码编译，以及 targeted core renderer/DSL/function 测试已验证。            |

最近验证：

- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionBoxTest --tests com.kotlinorm.compiler.SelectBoxTest --no-daemon --console=plain`
- `./gradlew :kronos-compiler-plugin:compileKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 scalar subquery 单列/limit diagnostics 的 FIR checker 编译）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionDiagnosticsTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 scalar subquery 缺 alias、缺 `limit(1)`、多列投影、聚合无 `groupBy` 免 limit、`groupBy` 聚合仍需 limit，以及 projection alias diagnostics）
- `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证新增 FIR diagnostics 未误伤 core 测试源码）
- `./gradlew :kronos-compiler-plugin:compileKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 predicate subquery arity diagnostics 的 FIR checker 编译）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionDiagnosticsTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 `IN`/tuple/`ANY` 谓词子查询列数 diagnostics）
- `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证谓词子查询 diagnostics 后的 core 测试源码编译；移除已迁到 compiler diagnostics 的用户 DSL 负例）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ConditionBoxTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证有效 `field in`、tuple IN、quantified comparison、EXISTS 的 compiler handoff）
- `./gradlew :kronos-core:test --tests com.kotlinorm.ast.SubqueryRendererTest --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 core subquery renderer/DSL 正向路径与 runtime validator 分层）
- `./gradlew :kronos-core:test --tests com.kotlinorm.orm.select.MysqlSelectTest --tests com.kotlinorm.orm.select.SelectClauseAstTest --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --tests com.kotlinorm.ast.SubqueryRendererTest --tests com.kotlinorm.functions.FunctionHandlerTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.DslIntegrationBoxTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`
- `./gradlew :kronos-compiler-plugin:clean :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（用于清理损坏的 kapt/incremental 缓存后重验）
- `./gradlew :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.DslIntegrationBoxTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`
- `./gradlew :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 derived source `q` 前缀/remap 与参数透传）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.DslIntegrationBoxTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 `KPojo.where().select {}` FIR/IR handoff）
- `./gradlew :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 `join(KSelectable)` 右侧 derived table、ON remap 与参数透传）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.DslIntegrationBoxTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 `join(KSelectable)` 右侧 lambda 暴露上一层 `Selected` alias）
- `./gradlew :kronos-core:test --tests com.kotlinorm.orm.join.SelectFromAstTest --tests com.kotlinorm.orm.join.MysqlJoinTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`
- `./gradlew :kronos-compiler-plugin:compileKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 FIR diagnostics 注册和 compiler plugin 主源码编译）
- `./gradlew :kronos-compiler-plugin:compileKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（再次验证 Selected/Source 冲突诊断实现）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionDiagnosticsTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证首批 projection FIR diagnostics，且旧命名 API 不再单独作为 Kronos 诊断用例）
- `./gradlew :kronos-testing:compileTestKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 integration 测试源码中的 alias 替换和投影字段读取）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.DslIntegrationBoxTest.kpojoWhereSugar --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（回归 `KPojo.where().select { [it.id] }` 的 FIR `listOf(...)` 降级形态）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionDiagnosticsTest --tests com.kotlinorm.compiler.DslIntegrationBoxTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 diagnostics 与 Source/Selected/Context 集成）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionBoxTest --tests com.kotlinorm.compiler.SelectBoxTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 projection/orderBy/select 正向 box）
- `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 core 测试源码已适配非直接 select item 必须 `.alias(...)` 的 compiler 规则）
- `./gradlew :kronos-core:clean :kronos-core:test --tests com.kotlinorm.ast.SubqueryRendererTest --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --tests com.kotlinorm.orm.select.MysqlSelectTest --tests com.kotlinorm.orm.select.SelectClauseAstTest --tests com.kotlinorm.functions.MysqlFunctionTest --tests com.kotlinorm.functions.PostgresFunctionTest --tests com.kotlinorm.functions.OracleFunctionTest --tests com.kotlinorm.functions.MssqlFunctionTest --tests com.kotlinorm.functions.SqliteFunctionTest --no-daemon --no-build-cache --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 subquery renderer/DSL、select AST、MySQL select、五方言函数投影；clean 用于刷新 shared `Kronos.init` 的 generated projection/context `kClassCreator` 映射）
- `./gradlew :kronos-compiler-plugin:compileKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 generated projection 作为下一层 Source 时，backend materializer 不再展开 FIR lazy class）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.SelectBoxTest.windowFunctionOver --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证窗口函数 `over { partitionBy/orderBy }` lower 到 `FunctionField.over`）
- `./gradlew :kronos-core:test --tests 'com.kotlinorm.orm.subquery.MysqlSubqueryDslTest.window*' --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证窗口 alias 通过下一层 derived query 过滤，外层引用上一层输出列）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionDiagnosticsTest.sameLayerWhereWindowAlias --tests com.kotlinorm.compiler.ProjectionBoxTest.windowAliasContextOrderBy --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证同层 where 不能访问窗口 alias、同层 orderBy Context 可以访问窗口 alias）
- `./gradlew :kronos-core:clean :kronos-core:test --tests 'com.kotlinorm.orm.subquery.MysqlSubqueryDslTest.window*' --no-daemon --no-build-cache --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（clean 后验证窗口 alias 下一层过滤和当前层 orderBy SQL；clean 用于刷新 shared `Kronos.init` 的 generated context `kClassCreator` 映射）
- `./gradlew :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 MySQL core subquery DSL 全量用例，包括 DML、insert-select、CTAS 与窗口新增用例）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionDiagnosticsTest --tests com.kotlinorm.compiler.ProjectionBoxTest --tests com.kotlinorm.compiler.SelectBoxTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 projection diagnostics、projection box、select box 相关矩阵）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionDiagnosticsTest.windowFunctionInvalidClausePosition --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（验证 `where` scope 不暴露窗口 `over` DSL）
- `./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ProjectionDiagnosticsTest --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1`（重验完整 projection diagnostics suite）
- `rg -n "as_\\(|\\.as_|as_ \\\"| as_" README.MD README-zh_CN.MD kronos-docs/src .agents/skills/kronos-dev-guide/references SUBQUERY_DSL_SPEC.md kronos-core/src kronos-compiler-plugin/src kronos-compiler-plugin/testData`（无结果，验证源码/docs/guide/testData 中无旧命名 API 正向残留；历史 Evolution 和构建产物不纳入该结论）
- `rg -n "selectAliasContextWhere|selectAliasContextOrderByHaving|scalarSubqueryAliasContextWhereOrderBy|legacyAsAlias" kronos-compiler-plugin/src/test/kotlin kronos-compiler-plugin/testData kronos-core/src/test/kotlin`（无结果，验证旧同层 alias 正向 runner/testData 已清理）

当前验证缺口：

- 本轮已通过 projection diagnostics、dslIntegration、projection/select box、`kronos-testing` 测试源码编译、core 测试源码编译和 targeted core renderer/DSL/function 测试；尚未重跑完整 `kronos-core:test`、完整 `kronos-compiler-plugin:test` 和真实数据库 integration tests。
- 非 clean 的 targeted core 测试曾因 shared `Kronos.init` 里的 generated projection/context `kClassCreator` 映射未刷新而失败；clean 后通过，符合已记录的增量/旧映射风险。

## 任务 1：旧实现和旧测试清理

目标：

- 先清掉所有与当前 spec 冲突的旧正向路径。
- 后续实现只面向一个目标：`.alias`、`where/having = Source`、`orderBy = Context`、过滤当前层 `Selected` 必须进入下一层查询。
- 可复用底层能力可以保留，但不能继续作为“同层 where/having alias 可用”的用户语义。

### 1.1 Compiler 清理

| 子项 | 文件或测试 | 当前问题 | 动作 |
|------|------------|----------|------|
| 1.1.1 | `kronos-compiler-plugin/src/main/kotlin/com/kotlinorm/compiler/utils/Constants.kt` | alias 函数名常量仍指向旧命名 API | 改为 `alias`；不为旧命名 API 添加 Kronos 自定义诊断。 |
| 1.1.2 | `KronosProjectionCallRefinementExtension.kt` | `toAliasProjectionField`、`toAliasCallProjectionField`、`recordAliasedExpressionType`、`toAliasLiteralProjectionField` 都围绕旧命名 API | 迁移到 `.alias()`；旧命名 API 不作为插件诊断目标。 |
| 1.1.3 | `KronosProjectionCallRefinementExtension.kt` | 注释和模型仍写 Context 用于 `where/having/orderBy` | 改为 Context 仅供同层 `orderBy`；`where/having` 使用 `Source`。 |
| 1.1.4 | `KronosProjectionCallRefinementExtension.kt` | `mergeContextFields` 对同名字段可能覆盖 | 改为冲突检测；原 Source 字段按原名直接投影放行。 |
| 1.1.5 | `FieldAnalysis.kt` / `SelectTransformer.kt` | 识别旧命名 API 作为 alias；注释仍用旧示例 | 迁移到 `alias`；非直接投影缺 alias 进入 diagnostics。 |
| 1.1.6 | `KronosProjectionRegistry.kt` / `KronosProjectionIrTransformer.kt` | Context runtime/class 可被旧 where/having 路径使用 | 保留给 `orderBy` 和 derived metadata；不得作为 where/having receiver。 |

需要删除、拆分或迁移的 compiler 正向测试：

| 测试入口 | testData | 动作 |
|----------|----------|------|
| `ProjectionBoxTest.selectAliasContextWhere()` | `testData/box/projection/selectAliasContextWhere.kt` | 改 diagnostics：同层 `where` 引用当前层 alias 报错；另补下一层正向测试。 |
| `ProjectionBoxTest.selectAliasContextOrderByHaving()` | `testData/box/projection/selectAliasContextOrderByHaving.kt` | 拆分：`having { it.xx }` 改 diagnostics；`orderBy { it.xx }` 改 `.alias` 后保留正向。 |
| `ProjectionBoxTest.scalarSubqueryAliasContextWhereOrderBy()` | `testData/box/projection/scalarSubqueryAliasContextWhereOrderBy.kt` | 拆分：同层 where 改 diagnostics 或下一层正向；同层 orderBy 改 `.alias` 后保留。 |
| `ProjectionBoxTest.functionAliasContext()` | `testData/box/projection/functionAliasContext.kt` | `having { it.totalCount }` 改 diagnostics；`orderBy` / `Selected` 类型正向改 `.alias`。 |
| `ProjectionBoxTest.generatedSelectProjection()` | `testData/box/projection/generatedSelectProjection.kt` | 旧命名写法改 `.alias("xx")` 后保留。 |
| select box | `testData/box/select/projectionFields.kt` | 旧命名写法改 `.alias("mobile")`。 |
| select box | `testData/box/select/functionFields.kt` | 旧命名写法改 `.alias("cnt")`；未 alias 的非直接投影按目标语义改 diagnostics。 |
| select box | `testData/box/select/scalarSubquerySelectItem.kt` | `KSelectable` 旧命名写法改 `.alias("lastAmount")`。 |

### 1.2 Core 清理

| 子项 | 文件或测试 | 当前问题 | 动作 |
|------|------------|----------|------|
| 1.2.1 | `kronos-core/src/main/kotlin/com/kotlinorm/orm/select/SelectClause.kt` | `where(selectCondition: ToFilter<Context, ...>)` | 改为 `ToFilter<Source, ...>`，使用 `pojo.afterFilter`。 |
| 1.2.2 | `SelectClause.kt` | `having(selectCondition: ToFilter<Context, ...>)` | 改为 `ToFilter<Source, ...>`，聚合过滤写表达式而不是 alias。 |
| 1.2.3 | `SelectClause.kt` iterable extension | 批量 `where` 仍用 `ToFilter<Context, ...>` | 同步改为 `Source`。 |
| 1.2.4 | `SelectClause.kt` | `toStatement()` 默认 `applyAutomaticLayering("q")` | 从默认主路径移除或降级；同层 where/having 不再触发 alias 外包。 |
| 1.2.5 | `SelectConditionLayering.kt` | `whereParts.outer` / `havingParts.outer` 服务旧自动分层 | 保留为内部工具或重命名；不能作为用户 DSL 正向验收。 |
| 1.2.6 | `SelectStatementDerivation.kt` | `wrapWithOuterFilter` 名称和测试容易绑定旧主路径 | 保留给 `KSelectable` 下一层、derived source、方言内部改写。 |
| 1.2.7 | `KTableForSelect.kt` | `Field`、`FunctionField`、`Expression`、generic `R` 仍有旧命名入口 | 新增/迁移 `.alias`；删除旧正向入口；收紧 generic alias。 |
| 1.2.8 | `KSelectable.kt` | `KSelectable` 仍有旧命名入口 | 改为 `.alias`；旧 API 入口删除即可。 |
| 1.2.9 | `FunctionHandler.kt` | `FunctionHandler` 仍有旧命名入口 | 改为/新增 `alias`，旧入口移除即可。 |

需要删除、拆分或迁移的 core 正向测试：

| 测试 | 当前问题 | 动作 |
|------|----------|------|
| `SubqueryRendererTest` 的 `automatic layering moves selected alias predicates to outer query` | 明确把 selected alias predicate 自动搬外层作为正向目标 | 删除或改成内部工具测试，不能作为用户 DSL 验收。 |
| `SubqueryRendererTest` 的 `wrap select statement with outer filter` | 工具可用，但测试语义要转为 explicit derived query / next-layer source | 改名和断言说明。 |
| `MysqlSelectTest.testAsSql` | 使用旧命名写法 | 改 `.alias`。 |
| `MysqlSelectTest.testAlias` | 使用旧命名写法 | 改 `.alias`。 |
| `MysqlSelectTest.testSetDbName` | 使用旧命名写法 | 改 `.alias`。 |
| `MysqlSubqueryDslTest.select scalar subquery item renders sql and params` | 标量子查询 select item 用旧命名写法 | 改 `.alias`，另补缺 alias 负例。 |
| `SelectClauseAstTest` 中 alias 断言 | 使用旧命名写法 | 改 `.alias`。 |

不应删除：

- `orderBy` 访问 selected alias 的 core 能力。
- 普通 `select { [it.id] }.where { it.gender == 0 }`，这是过滤未投影 Source 字段，符合新 spec。
- derived wrapper、subquery lowering、参数透传、alias metadata，只要不作为同层 where/having alias 正向语义。

### 1.3 Docs / Skills 清理

| 文件 | 当前问题 | 动作 |
|------|----------|------|
| `README.MD` / `README-zh_CN.MD` | 旧命名写法 | 改 `.alias(...)`。 |
| `kronos-docs/src/app/docs/en/3.database/8.select-records/index.md` | 正文说 `+` 连接字段、旧命名 alias | 改为 `[]` 字段列表和 `.alias(...)`。 |
| `kronos-docs/src/app/docs/zh-CN/3.database/8.select-records/index.md` | 同上 | 同步中文。 |
| `kronos-docs/src/app/docs/en/3.database/9.select-join-tables/index.md` | 正文和示例仍有 `+` 字段列表、旧命名 alias | 改为 `[]` 和 `.alias(...)`。 |
| `kronos-docs/src/app/docs/zh-CN/3.database/9.select-join-tables/index.md` | 同上 | 同步中文。 |
| `.agents/skills/kronos-dev-guide/references/api-design.md` | 写旧命名 alias，并称 `where/orderBy/having` 都操作 generated context | 改为 `.alias`；明确同层 `where/having = Source`，`orderBy = Context`。 |
| `.agents/skills/kronos-dev-guide/references/api-design.md` | 写“Filtering selected aliases ... where { it.alias }” | 改为进入下一层 `KSelectable.select {}` 后再过滤。 |
| `.agents/skills/kronos-dev-guide/references/orm-and-dsl.md` | `it.field1 + it.field2` 和旧中缀 alias 示例 | 改为 `select { [it.field1, it.field2] }` 和 `.alias(...)`。 |
| `.agents/skills/kronos-dev-guide/references/compiler-plugin.md` | `it.name + it.age` 字段列表和旧中缀 alias 示例 | 改为 `[]` 列表解析和 `.alias("n")`。 |
| `.agents/skills/kronos-dev-kcp/Evolution.md` | 历史记录含 alias where / 自动分层旧目标 | 历史保留；DSL 语义取舍不写入 Evolution，只有确认过的代码/编译器坑再记录。 |

### 1.4 测试迁移规则

| 旧正向测试 | 新处理 |
|------------|--------|
| 旧命名 alias | 正向用法替换为 `.alias("x")`；不为旧名字增加 Kronos 自定义诊断。 |
| 同层 `where { it.alias }` | 改 diagnostics。 |
| 同层 `having { it.aggregateAlias }` | 改 diagnostics。 |
| 同层 `where { it.windowAlias }` | 改 diagnostics。 |
| scalar alias same-level where | 改 diagnostics 或改为下一层正向。 |
| derived query 外层 where | 保留或新增正向。 |
| orderBy alias | 保留正向，迁到 `.alias`。 |
| `having { f.sum(it.amount) > ... }` | 保留正向。 |

### 1.5 清理验收

静态扫描：

```powershell
rg -n "旧命名|where.*Context|having.*Context|自动分层|Selected alias|selectAliasContext|scalarSubqueryAliasContext" SUBQUERY_TASK_LIST.md SUBQUERY_DSL_SPEC.md .agents kronos-docs README.MD README-zh_CN.MD kronos-core kronos-compiler-plugin
```

期望：

- spec/tasklist/docs/skills 中不再有与当前 spec 冲突的旧命名 alias 正向描述。
- compiler/core 正向测试中不再有旧命名 alias。
- `where/having Context` 只允许出现在历史说明或待删除代码注释中，不能作为目标描述。
- 旧 box runner 不再引用已删除 testData。
- 新 diagnostics 覆盖旧正向行为。

测试命令：

```powershell
.\gradlew.bat :kronos-compiler-plugin:test --no-daemon --console=plain
.\gradlew.bat :kronos-core:test --no-daemon --console=plain
```

任务 1 完成判定：

- 旧正向能力不再被测试、文档、skill 鼓励。
- 可复用底层能力没有被误删。
- 后续任务可以从“查询层类型模型重置”开始，不需要再兼容旧 DSL 目标。

## 任务 2：查询层类型模型重置

目标：

- 保留 `SelectClause<Source, Selected, Context>` 概念模型。
- `select { ... }` 使用 `Source`。
- `where { ... }` 使用 `Source`。
- `groupBy { ... }` 使用 `Source`。
- `having { ... }` 使用 `Source`。
- `orderBy { ... }` 使用 `Context`。
- `queryList/queryOne/queryOneOrNull` 返回 `Selected`。

需要重做：

- core 中 `SelectClause.where` / `having` 的目标签名不能再是 `ToFilter<Context, ...>`，应回到 `ToFilter<Source, ...>`。
- iterable 扩展、cascade、join select 等链路同步 Source/Selected/Context 语义。
- compiler FIR call refinement 不得因为存在 `Context` 而让 `where/having` 解析到当前层 alias。

验收：

- `User().select { [it.id] }.where { it.status == 1 }` 合法，即使 `status` 未投影。
- `User().select { [it.id, f.length(it.name).alias("nameLength")] }.where { it.nameLength > 8 }` 编译期错误。
- `queryList()` 返回当前层 `Selected`。

## 任务 3：`KPojo.where` 语法糖

状态：已完成。

目标：

- 支持 `User().where { it.id > 1 }`。
- 语义完全等价 `User().select().where { it.id > 1 }`。
- 结果类型是 `KSelectable<User>`，不是独立的前置 where 查询模型。

验收：

- core DSL build 输出与 `select().where()` 一致：已由 `MysqlSelectTest.testWhereSugarMatchesSelectWhere` 覆盖。
- compiler official box 验证 `User().where { ... }.queryList()` 返回 `List<User>`：已由 `DslIntegrationBoxTest.kpojoWhereSugar` 覆盖。
- `KPojo.where().select { ... }` 进入下一层时，`Source` 仍按 `select()` 的 `Selected = Source` 处理：留到任务 4 的 `KSelectable<S>.select { ... }` 统一验收。

## 任务 4：`KSelectable` 作为下一层 Source

目标：

- `KSelectable<S>.select { ... }` 的 receiver 是上一层 `Selected`。
- derived query、join query、union consumer 都能稳定暴露 `Selected` 输出列。
- 过滤当前层 `Selected` 字段时，通过下一层查询表达。

示例目标：

```kotlin
val q = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

q.select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .queryList()
```

需要重做：

- derived table/source wrapper 不再作为“同层 where alias 自动分层”的补丁，而是正式查询层边界。
- `join(KSelectable)` 右侧 receiver 必须是右侧 query 的 `Selected`。
- `SelectFrom`、分页、union、insert-select、CTAS 消费 `KSelectable` 时统一使用 `Selected`。

当前进展：

- `KSelectable<S>.select { ... }` 已新增 core 入口，receiver 以上一层 `Selected` 作为新 `Source`。
- compiler FIR refinement 已能把 `SelectClause` / `KSelectable` receiver 的 `Selected` 类型作为下一层 `select` 的 Source。
- compiler IR rewrite 已能把 selectable receiver 的 generated projection select 重写到 `KSelectable.selectGeneratedProjection(...)`。
- compiler checker/refinement 已兼容 `[]` 在 FIR 中保留为 `FirCollectionLiteral` 或降级成 `listOf(vararg)` 的两种形态。
- derived table 由 `DeferredSubqueryTable` 延迟到 `SubqueryLowering` 阶段 materialize，内层 `where` 参数会写入外层 build 的共享 parameter map。
- 外层 select/where/having/groupBy/orderBy 的未限定 source 字段会 remap 到 derived table alias `q`。
- 已有 core 正向覆盖：`where().select { ... }.where { ... }` 渲染为 derived query，外层字段带 `q` 前缀，参数包含内外层。
- 已有 compiler box 覆盖：`KPojo.where().select { ... }` 能用上一层输出继续投影，lowering 后保留内层 source 和参数。
- `join(KSelectable)` 已新增 core 入口，右侧表以 deferred derived table 参与 join，ON/select 字段 remap 到 `q`，内层参数在 join build lowering 时透传。
- 已有 compiler box 覆盖：`join(KSelectable)` 右侧 lambda 可访问上一层 `Selected` alias。
- insert-select/CTAS 已补 derived source consumer 覆盖，后续仍需完整 core/compiler 矩阵重跑。
- union consumer 已有普通 source 覆盖；把 union 本身作为下一层 typed `Source` 仍需后续 API 设计和验收。

验收：

- derived select where 可生成 `FROM (SELECT ...) q WHERE q.alias ...`，且内外层参数可透传。
- `join(KSelectable)` 的 right lambda receiver 只暴露右侧 `Selected`。
- 内层参数能透传并在外层安全重命名。

## 任务 5：alias API 与命名诊断

目标：

- `.alias("name")` 命名当前层 `Selected` 字段。
- 旧命名 API 删除；正向用法统一替换为 `.alias("name")`，不额外增加 Kronos 自定义诊断。
- 直接字段投影可继承字段名。
- 函数、聚合、窗口函数、标量子查询、计算表达式作为 select item 时必须显式 alias。
- 同一层 `Selected` 最终字段名必须唯一。
- 新增 `Selected` 字段与 `Source` 字段同名时，`Context` 生成失败。
- 原 `Source` 字段按原名直接投影不算冲突。

诊断验收：

- `select { [f.length(it.name)] }` 报错：已新增 FIR checker 与 diagnostics testData，并通过 `ProjectionDiagnosticsTest`；checker 已兼容 FIR `listOf(vararg)` 形态。
- `select { [f.sum(it.amount)] }` 报错：聚合函数路径同非直接字段规则，后续可补专门 diagnostics case。
- `select { [Order().select { it.amount }.limit(1)] }` 报错：已新增 scalar subquery 缺 alias diagnostics testData，并通过 `ProjectionDiagnosticsTest`。
- `select { [it.id, f.length(it.name).alias("id")] }` 报错。
- `select { [it.id, it.id] }` 报错：已新增 FIR checker 与 diagnostics testData，并通过 `ProjectionDiagnosticsTest`。
- `select { [it.id, f.length(it.name).alias("status")] }` 在 `Source` 存在 `status` 时因 `Context` 冲突报错：已新增 FIR checker 与 diagnostics testData，并通过 `ProjectionDiagnosticsTest`。

## 任务 6：receiver 签名与 compiler refine

目标：

- FIR/IR 生成 `Selected` projection class。
- FIR/IR 生成 `Context = Source + Selected`，但仅供 `orderBy` 使用。
- `select()` 无投影时，`Selected = Source`。
- `select { ... }` 有投影时，`Selected` 由投影列表生成。
- `KSelectable<S>.select { ... }` 使用 `S` 作为新层 `Source`。

需要重做：

- 旧的 “where/having/orderBy 都吃 Context” refine 删除。
- `where/having` 的 generated receiver 改为 `Source`。
- `orderBy` 的 generated receiver 保持 `Context`。
- `.alias` 字段进入 `Selected` 和 `Context`。

验收：

- `select { [it.id] }.where { it.name != null }` 可解析：`DslIntegrationBoxTest.selectClauseStatement` / `kpojoWhereSugar` 已覆盖同层 where 使用未投影 Source 字段。
- `select { [it.id, f.length(it.name).alias("len")] }.orderBy { it.len.desc() }` 可解析：`ProjectionBoxTest.functionAliasContext` 已覆盖 function alias orderBy。
- 同层 `where { it.len > 1 }` 不可解析或报自定义诊断：`ProjectionDiagnosticsTest.sameLayerWhereSelectedAlias` 已覆盖。

## 任务 7：同层 where/having alias 能力删除

目标：

- 当前层 `where` 只能过滤输入行，即 `Source`。
- 当前层 `having` 只能使用基于 `Source` 的分组后表达式。
- 当前层 `where/having` 不允许访问当前层 `Selected` alias、聚合 alias、窗口 alias、标量子查询 alias。
- 旧自动分层能力降级为内部工具或删除，不能作为新 DSL 的正向语义。

需要删除或改写：

- `select { [it.id, it.name.alias("xx")] }.where { it.xx == "Ada" }` 正向测试。
- `select { [it.id, f.sum(...).alias("total")] }.having { it.total > 1 }` 正向测试。
- scalar subquery alias same-level where 正向测试。
- window alias same-level where 正向测试。

验收：

- 上述场景全部变成 diagnostics。
- 下一层过滤写法作为正向路径通过。

## 任务 8：orderBy Context

目标：

- `orderBy` 可以访问 `Source` 字段。
- `orderBy` 可以访问当前层 `Selected` 字段。
- `orderBy { it.alias.desc() }` 渲染为同层 `ORDER BY alias`，必要时由方言层处理合法性。
- `DISTINCT`、`GROUP BY` 后排序字段的 SQL 合法性按方言约束处理。

验收：

- `User().select { [it.id, f.length(it.name).alias("nameLength")] }.orderBy { it.nameLength.desc() }` 可用。
- `User().select { [it.id] }.orderBy { it.name.asc() }` 可用。
- window alias 可用于同层 `orderBy`。
- 非法 distinct/group 排序按生成期或方言诊断处理。

## 任务 9：标量子查询

目标：

- 标量子查询可出现在 select item、where 比较、orderBy、update set、upsert set。
- 标量子查询必须单列。
- 非聚合标量子查询必须显式 `.limit(1)`。
- `query.limit(1) as T` 是类型提示，不改变 SQL。
- 标量子查询作为 select item 时必须 `.alias("name")`。

需要补：

- `.alias` 替代旧命名 API 的 compiler handoff 已通过首批 box/diagnostics；core 函数/聚合/原生 SQL 测试源码已补显式 alias。
- scalar shape 公共校验：单列和 `.limit(1)` 已有 compiler diagnostics；聚合无 `groupBy` 可不写 `.limit(1)` 已有 diagnostics 正向覆盖；core `SubqueryValidator` 仍保留 SQL build 兜底。
- 仍待补：类型提示 `query.limit(1) as T` 的 compiler/core 验收，更多 DML/upsert scalar 位置的 typed set 验收。
- 错误定位尽量放在 compiler diagnostics，runtime builder 保留兜底校验。

验收：

- `select { [it.id, query.limit(1).alias("lastAmount")] }` 正向。
- `where { it.price > query.limit(1) }` 正向。
- 多列 scalar、缺 limit、select item 缺 alias 均报错；首批 compiler diagnostics 已覆盖。

## 任务 10：谓词子查询

目标：

- 支持 `field in query` / `field !in query`。
- 支持 `exists(query)` / `!exists(query)`。
- 支持 `any(query)` / `some(query)` / `all(query)`。
- 支持 `[a, b] in query` / `[a, b] !in query` row-value tuple。
- 单元素 tuple 不允许，单列必须写 `field in query`。

需要补：

- 单值谓词右侧多列诊断：已新增 FIR checker 与 diagnostics testData，并通过 `ProjectionDiagnosticsTest`。
- tuple 左右列数不一致诊断：已新增 FIR checker 与 diagnostics testData，并通过 `ProjectionDiagnosticsTest`。
- 单元素 tuple `[it.id] in query` 诊断：已新增 FIR checker 与 diagnostics testData，并通过 `ProjectionDiagnosticsTest`。
- `ANY/SOME/ALL` 右侧多列诊断：已新增 FIR checker 与 diagnostics testData，并通过 `ProjectionDiagnosticsTest`。
- SQLite / SQL Server 等方言不支持能力的生成期报错或改写策略。

验收：

- select/update/delete where 均可使用谓词子查询。
- 相关子查询可引用外层 lambda receiver。
- 五方言 renderer 矩阵覆盖 `ANY/SOME/ALL`、tuple IN、EXISTS。

## 任务 11：窗口函数与下一层过滤

目标：

- 窗口函数只允许出现在当前层 `select` 或 `orderBy` 表达式中。
- 窗口函数作为 select item 必须 `.alias("rn")`。
- 当前层 `where/groupBy/having` 不可访问窗口 alias。
- top-N / rn 过滤通过下一层查询完成。

当前进展：

- compiler IR 已能识别 `f.rowNumber().over { partitionBy(...); orderBy(...) }`，并构造 `FunctionField.over = WindowClause(...)`。
- `SelectBoxTest.windowFunctionOver` 已覆盖 `partitionBy` / `orderBy` 字段 lowering 和 `.alias("rn")`。
- `ProjectionDiagnosticsTest.sameLayerWhereWindowAlias` 已覆盖同层 `where { it.rn == 1 }` 不可访问当前层窗口 alias。
- `ProjectionBoxTest.windowAliasContextOrderBy` 已覆盖同层 `orderBy { it.rn.asc() }` 的 compiler handoff。
- core DSL 测试已覆盖窗口 alias 下一层过滤和当前层 orderBy SQL；下一层 SQL 形态为外层从 derived query 读取上一层输出列并过滤 `q.rn`。
- 修复了 generated projection 作为下一层 `Source` 时 backend 物化再次展开 FIR lazy class 的崩溃。

仍待补：

- `where` 中直接写窗口 `over` 已由普通 Kotlin unresolved diagnostics 覆盖；`groupBy/having` 可后续补重复矩阵。
- `SUBQUERY_DSL_SPEC.md` 已回到 block 写法，不新增参数式 `over(partitionBy = ..., orderBy = ...)` 语法。

目标写法：

```kotlin
val ranked = Order()
    .select {
        [
            it.id,
            it.userId,
            f.rowNumber()
                .over {
                    partitionBy(it.userId)
                    orderBy(it.createTime.desc())
                }
                .alias("rn")
        ]
    }

ranked
    .select { [it.id, it.userId] }
    .where { it.rn == 1 }
    .queryList()
```

验收：

- 同层 `where { it.rn == 1 }` 报错。
- 下一层过滤渲染为派生表外层 `WHERE rn = ?`。
- `orderBy { it.rn.asc() }` 可在当前层使用。

## 任务 12：DML 子查询

目标：

- `update().set` 可承接标量子查询。
- `update().where` / `delete().where` 可承接所有谓词子查询和标量比较。
- `upsert().set` / `patch` 可承接标量子查询。
- DML lambda receiver 均为目标表 `Source`。
- upsert `set { s -> ... }` 中的 `s` 表示冲突更新的目标行；incoming/excluded DSL 不在当前 spec 中定义。
- `.patch` 作为动态字段入口保留。

当前进展：

- MySQL core DSL 已覆盖 `update/delete where` 中的 `field IN`、row-value tuple IN、`EXISTS/NOT EXISTS`、`ANY/ALL` 和 scalar comparison。
- MySQL core DSL 已覆盖 `update.patch`、`update.setValue`、`upsert.patch`、`upsert.setValue` 承接 scalar subquery。
- `MysqlSubqueryDslTest` 全量通过，确认 DML 子查询与当前 subquery lowering/rendering 可一起工作。

需要补：

- 最终 typed assignment 语法 `field = query` 的 FIR/类型系统承接。
- PostgreSQL/SQLite/MySQL upsert expression assignment 测试。
- Oracle/SQL Server native upsert 不支持或 fallback 策略明确化。
- 相关 update/upsert 子查询引用目标行字段。

验收：

- `UPDATE ... SET col = (SELECT ... LIMIT 1)` 可渲染。
- `UPDATE/DELETE ... WHERE EXISTS/IN/ANY/tuple` 可渲染。
- `UPSERT ... DO UPDATE SET col = (SELECT ...)` 在支持方言可渲染，不支持方言明确报错或走 fallback。

## 任务 13：INSERT SELECT 与 CTAS

目标：

- `KSelectable<Selected>.insert<Target> { ... }` 的 receiver 是源 query 的 `Selected`。
- `insert<Target> { [...] }` 按目标表可插入字段顺序映射。
- `null`、常量、函数表达式、源投影字段、标量子查询都可作为插入值。
- CTAS 使用 `dataSource.table.createTable(target, query)`，消费源 query 的最终 `Selected`。

当前进展：

- MySQL core DSL 已覆盖普通 select、derived select、join select、union source 作为 insert-select source。
- insert-select 已覆盖默认字段序、显式值列表、`null`、常量、函数表达式和标量子查询值。
- CTAS 已覆盖普通 select、derived select、join select、union source。
- `MysqlSubqueryDslTest` 全量通过，确认 source query consumer 与当前 subquery lowering/rendering 可一起工作。

当前明确缺口：

- 现有 `KSelectable<*>.insert<Target> { ... }` 的 lambda 参数是目标表 `List<Field>`，不是源 query 的 `Selected`；与目标设计“receiver 是源 `Selected`”不一致。
- 如果要把 `insert<Target> { ... }` 改成源 `Selected` receiver，需要先处理与现有目标字段列表 lambda 的重载/迁移关系，不能直接新增同名 `Function1` overload。

需要补：

- 字段数量静态或 build 阶段校验。
- 字段类型兼容校验。
- 目标表可插入字段序列与策略字段、忽略字段、默认值字段规则对齐。
- CTAS 参数化按方言支持处理。
- join select / union source / derived query source 全部可作为 consumer 输入。

验收：

- 普通 select、join select、union source 均可 insert-select。
- `insert<Target> { [it.id, null, it.amount] }` 按顺序映射。
- CTAS 五方言 renderer 有明确输出或明确不支持原因。

## 任务 14：测试矩阵重建

测试职责：

- core unit / renderer tests：验证 AST、lowering、SQL、参数、方言边界。
- compiler official box：验证 FIR/IR/DSL handoff、生成类型、lambda receiver、KClass、运行期最小行为。
- compiler diagnostics：验证非法 DSL 在编译期失败。
- integration tests：抽样验证真实数据库语义，不替代 core renderer 矩阵。

必须补的 compiler diagnostics：

- 非直接 select item 缺 alias。
- 标量子查询 select item 缺 alias。
- 当前层 `where` 引用当前层 `Selected` alias。
- 当前层 `having` 引用当前层聚合 alias。
- 当前层 `where` 引用窗口 alias。
- 标量子查询多列或缺 `.limit(1)`。
- 单值 `IN/ANY/SOME/ALL` 右侧多列。
- tuple IN 左右列数不一致。
- 单元素 tuple `[it.id] in query`。
- `Selected` 最终字段名重复。
- 新增 `Selected` 字段与 `Source` 字段冲突导致 `Context` 失败。

必须补的 compiler box：

- `KPojo.where { ... }` 返回 `List<Source>`。
- `select().queryList()` 返回 `Source`。
- `select { [it.id] }.queryList()` 返回 generated `Selected`。
- `where` 可访问未投影的 `Source` 字段。
- `orderBy` 可访问当前层 alias。
- `KSelectable<S>.select { ... }` 以上一层 `Selected` 为 `Source`。
- 聚合 alias 下一层过滤。
- 窗口 alias 下一层过滤。
- `join(KSelectable)` 右侧 receiver 是右侧 `Selected`。
- `insert<Target> { ... }` receiver 是源 `Selected`。
- `[]` 在 select、orderBy、window、tuple IN 四种上下文被正确区分。

必须补的 core tests：

- derived query 外层 where / join / 参数透传。
- select-list scalar subquery、where scalar comparison、orderBy scalar subquery。
- IN / NOT IN / EXISTS / NOT EXISTS / ANY / SOME / ALL / tuple IN。
- update set scalar、update/delete where 子查询、upsert scalar。
- insert-select 普通 select / join / union / derived source。
- CTAS 普通 select / join / union source。
- 五方言 renderer 对方言边界给出稳定输出或稳定错误。

集成测试优先级：

1. 相关标量子查询。
2. derived query 外层过滤。
3. tuple IN 与 quantified comparison 的方言差异。
4. DML 子查询。
5. CTAS 参数化。

## 推荐实施顺序

1. 先完成任务 1：清理旧实现、旧正向测试、旧 docs/skills 说明。
2. 改 `SelectClause` 与 compiler receiver：`where/having = Source`，`orderBy = Context`。
3. 加 `KPojo.where` 语法糖。
4. 删除旧命名 API 正向路径，补 `.alias` API 与诊断。
5. 把同层 where/having alias 正向测试改成 diagnostics。
6. 打通 `KSelectable<S>.select { ... }` 下一层 Source。
7. 重验 orderBy Context。
8. 重验标量/谓词/tuple 子查询公共校验。
9. 重做窗口函数示例和测试：下一层过滤。
10. 重验 DML、insert-select、CTAS。
11. 同步 docs、README、skills 中旧语法和旧 receiver 说明。

## 暂不做

- 不引入用户可见 `inSubquery` / `setSubquery` / `asTable`。
- 不引入 CTE。
- 不设计 incoming / excluded upsert DSL。
- 不把同层 `where/having` 访问当前层 `Selected` alias 作为新 DSL 能力。
