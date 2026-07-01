# Kronos 子查询实现任务清单

更新时间：2026-07-01

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
- `.alias("name")` 是新的命名 API，旧 `.as_("name")` 不保留兼容。
- `select { ... }` 中直接字段投影可继承字段名，非直接字段投影必须显式 `.alias("name")`。
- 同一层 `Selected` 最终字段名必须唯一；新增 `Selected` 字段与 `Source` 字段同名导致 `Context` 生成失败。
- `[]` 是用户侧统一列表语法，由编译器按上下文解释为投影列表、排序列表、窗口字段列表或 row-value tuple。

## 总览

| 进度 | 任务 | 状态 | 说明 |
|------|------|------|------|
| 0% | 任务 1：旧实现和旧测试清理 | 未开始 | 先删除/改写与新 spec 冲突的正向路径，避免后续任务继续兼容旧目标。 |
| 15% | 任务 2：查询层类型模型重置 | 需重做 | 当前实现有三泛型地基，但 `where/having` 仍按旧 Context 方向，需要改回 Source。 |
| 5% | 任务 3：`KPojo.where` 语法糖 | 未开始 | 新 spec 增加的入口，等价 `select().where()`。 |
| 25% | 任务 4：`KSelectable` 作为下一层 Source | 部分可复用 | derived/lowering 可复用，但类型语义和测试要按新规则重验。 |
| 10% | 任务 5：alias API 与命名诊断 | 需重做 | `.as_` 正向路径要删除，`.alias` 与强制 alias 诊断要补齐。 |
| 10% | 任务 6：receiver 签名与 compiler refine | 需重做 | `select/where/groupBy/having/orderBy/queryList` 的类型承载要按新 receiver 表重刷。 |
| 20% | 任务 7：同层 where/having alias 能力删除 | 需重做 | 旧正向能力变成负向诊断，自动分层不再是用户语义主路径。 |
| 35% | 任务 8：orderBy Context | 部分可复用 | selected alias 排序地基可复用，但要迁到 `.alias` 并补 Context 冲突诊断。 |
| 60% | 任务 9：标量子查询 | 部分可复用 | AST/lowering 多数可用，select item alias 规则和 `.limit(1)` 诊断要补。 |
| 70% | 任务 10：谓词子查询 | 部分可复用 | `IN/EXISTS/ANY/ALL/tuple IN` 地基可用，诊断和新语法验收要补。 |
| 25% | 任务 11：窗口函数与下一层过滤 | 需重做 | 旧 `select(...rn).where { it.rn }` 目标删除，改为下一层过滤。 |
| 55% | 任务 12：DML 子查询 | 部分可复用 | update/delete/upsert AST 可复用，最终 typed set 语法和方言验收要补。 |
| 60% | 任务 13：INSERT SELECT 与 CTAS | 部分可复用 | source query 消费已存在，receiver/类型兼容/方言边界要重验。 |
| 10% | 任务 14：测试矩阵重建 | 需重做 | core/compiler/integration 的职责边界要重新分配。 |

## 任务 1：旧实现和旧测试清理

目标：

- 先清掉所有与当前 spec 冲突的旧正向路径。
- 后续实现只面向一个目标：`.alias`、`where/having = Source`、`orderBy = Context`、过滤当前层 `Selected` 必须进入下一层查询。
- 可复用底层能力可以保留，但不能继续作为“同层 where/having alias 可用”的用户语义。

### 1.1 Compiler 清理

| 子项 | 文件或测试 | 当前问题 | 动作 |
|------|------------|----------|------|
| 1.1.1 | `kronos-compiler-plugin/src/main/kotlin/com/kotlinorm/compiler/utils/Constants.kt` | `SelectAliasFunctionName = "as_"` | 改为 `alias`；旧 `as_` 只允许作为 diagnostics 负例。 |
| 1.1.2 | `KronosProjectionCallRefinementExtension.kt` | `toAliasProjectionField`、`toAliasCallProjectionField`、`recordAliasedExpressionType`、`toAliasLiteralProjectionField` 都围绕 `as_` | 迁移到 `.alias()`；补 `.as_()` 使用错误。 |
| 1.1.3 | `KronosProjectionCallRefinementExtension.kt` | 注释和模型仍写 Context 用于 `where/having/orderBy` | 改为 Context 仅供同层 `orderBy`；`where/having` 使用 `Source`。 |
| 1.1.4 | `KronosProjectionCallRefinementExtension.kt` | `mergeContextFields` 对同名字段可能覆盖 | 改为冲突检测；原 Source 字段按原名直接投影放行。 |
| 1.1.5 | `FieldAnalysis.kt` / `SelectTransformer.kt` | 识别 `as_` 作为 alias；注释仍用旧示例 | 迁移到 `alias`；非直接投影缺 alias 进入 diagnostics。 |
| 1.1.6 | `KronosProjectionRegistry.kt` / `KronosProjectionIrTransformer.kt` | Context runtime/class 可被旧 where/having 路径使用 | 保留给 `orderBy` 和 derived metadata；不得作为 where/having receiver。 |

需要删除、拆分或迁移的 compiler 正向测试：

| 测试入口 | testData | 动作 |
|----------|----------|------|
| `ProjectionBoxTest.selectAliasContextWhere()` | `testData/box/projection/selectAliasContextWhere.kt` | 改 diagnostics：同层 `where` 引用当前层 alias 报错；另补下一层正向测试。 |
| `ProjectionBoxTest.selectAliasContextOrderByHaving()` | `testData/box/projection/selectAliasContextOrderByHaving.kt` | 拆分：`having { it.xx }` 改 diagnostics；`orderBy { it.xx }` 改 `.alias` 后保留正向。 |
| `ProjectionBoxTest.scalarSubqueryAliasContextWhereOrderBy()` | `testData/box/projection/scalarSubqueryAliasContextWhereOrderBy.kt` | 拆分：同层 where 改 diagnostics 或下一层正向；同层 orderBy 改 `.alias` 后保留。 |
| `ProjectionBoxTest.functionAliasContext()` | `testData/box/projection/functionAliasContext.kt` | `having { it.totalCount }` 改 diagnostics；`orderBy` / `Selected` 类型正向改 `.alias`。 |
| `ProjectionBoxTest.generatedSelectProjection()` | `testData/box/projection/generatedSelectProjection.kt` | `.as_("xx")` 改 `.alias("xx")` 后保留。 |
| select box | `testData/box/select/projectionFields.kt` | `.as_("mobile")` 改 `.alias("mobile")`。 |
| select box | `testData/box/select/functionFields.kt` | `.as_("cnt")` 改 `.alias("cnt")`；未 alias 的非直接投影按目标语义改 diagnostics。 |
| select box | `testData/box/select/scalarSubquerySelectItem.kt` | `KSelectable.as_("lastAmount")` 改 `.alias("lastAmount")`。 |

### 1.2 Core 清理

| 子项 | 文件或测试 | 当前问题 | 动作 |
|------|------------|----------|------|
| 1.2.1 | `kronos-core/src/main/kotlin/com/kotlinorm/orm/select/SelectClause.kt` | `where(selectCondition: ToFilter<Context, ...>)` | 改为 `ToFilter<Source, ...>`，使用 `pojo.afterFilter`。 |
| 1.2.2 | `SelectClause.kt` | `having(selectCondition: ToFilter<Context, ...>)` | 改为 `ToFilter<Source, ...>`，聚合过滤写表达式而不是 alias。 |
| 1.2.3 | `SelectClause.kt` iterable extension | 批量 `where` 仍用 `ToFilter<Context, ...>` | 同步改为 `Source`。 |
| 1.2.4 | `SelectClause.kt` | `toStatement()` 默认 `applyAutomaticLayering("q")` | 从默认主路径移除或降级；同层 where/having 不再触发 alias 外包。 |
| 1.2.5 | `SelectConditionLayering.kt` | `whereParts.outer` / `havingParts.outer` 服务旧自动分层 | 保留为内部工具或重命名；不能作为用户 DSL 正向验收。 |
| 1.2.6 | `SelectStatementDerivation.kt` | `wrapWithOuterFilter` 名称和测试容易绑定旧主路径 | 保留给 `KSelectable` 下一层、derived source、方言内部改写。 |
| 1.2.7 | `KTableForSelect.kt` | `Field.as_`、`FunctionField.as_`、`Expression.as_`、generic `R.as_` | 新增/迁移 `.alias`；删除或诊断 `.as_`；收紧 generic alias。 |
| 1.2.8 | `KSelectable.kt` | `KSelectable.as_` | 改为 `.alias`；旧 API 删除或负例。 |
| 1.2.9 | `FunctionHandler.kt` | `FunctionHandler.as_` | 改为/新增 `alias`，旧入口移除或负例。 |

需要删除、拆分或迁移的 core 正向测试：

| 测试 | 当前问题 | 动作 |
|------|----------|------|
| `SubqueryRendererTest` 的 `automatic layering moves selected alias predicates to outer query` | 明确把 selected alias predicate 自动搬外层作为正向目标 | 删除或改成内部工具测试，不能作为用户 DSL 验收。 |
| `SubqueryRendererTest` 的 `wrap select statement with outer filter` | 工具可用，但测试语义要转为 explicit derived query / next-layer source | 改名和断言说明。 |
| `MysqlSelectTest.testAsSql` | 使用 `.as_` | 改 `.alias` 或迁负例。 |
| `MysqlSelectTest.testAlias` | 使用 `.as_` | 改 `.alias`。 |
| `MysqlSelectTest.testSetDbName` | 使用 `.as_` | 改 `.alias`。 |
| `MysqlSubqueryDslTest.select scalar subquery item renders sql and params` | 标量子查询 select item 用 `.as_` | 改 `.alias`，另补缺 alias 负例。 |
| `SelectClauseAstTest` 中 alias 断言 | 使用 `.as_` | 改 `.alias`。 |

不应删除：

- `orderBy` 访问 selected alias 的 core 能力。
- 普通 `select { [it.id] }.where { it.gender == 0 }`，这是过滤未投影 Source 字段，符合新 spec。
- derived wrapper、subquery lowering、参数透传、alias metadata，只要不作为同层 where/having alias 正向语义。

### 1.3 Docs / Skills 清理

| 文件 | 当前问题 | 动作 |
|------|----------|------|
| `README.MD` / `README-zh_CN.MD` | `relation.id.as_("relationId")`、`f.count(1).as_("count")` | 改 `.alias(...)`。 |
| `kronos-docs/src/app/docs/en/3.database/8.select-records/index.md` | 正文说 `+` 连接字段、`as_` alias | 改为 `[]` 字段列表和 `.alias(...)`。 |
| `kronos-docs/src/app/docs/zh-CN/3.database/8.select-records/index.md` | 同上 | 同步中文。 |
| `kronos-docs/src/app/docs/en/3.database/9.select-join-tables/index.md` | 正文和示例仍有 `+` 字段列表、`.as_` | 改为 `[]` 和 `.alias(...)`。 |
| `kronos-docs/src/app/docs/zh-CN/3.database/9.select-join-tables/index.md` | 同上 | 同步中文。 |
| `.agents/skills/kronos-dev-guide/references/api-design.md` | 写 `.as_`，并称 `where/orderBy/having` 都操作 generated context | 改为 `.alias`；明确同层 `where/having = Source`，`orderBy = Context`。 |
| `.agents/skills/kronos-dev-guide/references/api-design.md` | 写“Filtering selected aliases ... where { it.alias }” | 改为进入下一层 `KSelectable.select {}` 后再过滤。 |
| `.agents/skills/kronos-dev-guide/references/orm-and-dsl.md` | `it.field1 + it.field2`、`it.field1 as_ "alias"` | 改为 `select { [it.field1, it.field2] }` 和 `.alias(...)`。 |
| `.agents/skills/kronos-dev-guide/references/compiler-plugin.md` | `it.name + it.age` 字段列表、`it.name as_ "n"` | 改为 `[]` 列表解析和 `.alias("n")`。 |
| `.agents/skills/kronos-dev-kcp/Evolution.md` | 历史记录含 alias where / 自动分层旧目标 | 历史可保留，但追加“已被新版 spec 废弃”的索引或注记。 |

### 1.4 测试迁移规则

| 旧正向测试 | 新处理 |
|------------|--------|
| `.as_("x")` alias | 改 diagnostics：`.as_` 不可解析或报旧 API 已删除。 |
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
rg -n "as_\(|\.as_|where.*Context|having.*Context|自动分层|Selected alias|selectAliasContext|scalarSubqueryAliasContext" SUBQUERY_TASK_LIST.md SUBQUERY_DSL_SPEC.md .agents kronos-docs README.MD README-zh_CN.MD kronos-core kronos-compiler-plugin
```

期望：

- spec/tasklist/docs/skills 中不再有与当前 spec 冲突的 `.as_` 正向描述。
- compiler/core 正向测试中不再有 `.as_` alias。
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

目标：

- 支持 `User().where { it.id > 1 }`。
- 语义完全等价 `User().select().where { it.id > 1 }`。
- 结果类型是 `KSelectable<User>`，不是独立的前置 where 查询模型。

验收：

- core DSL build 输出与 `select().where()` 一致。
- compiler official box 验证 `User().where { ... }.queryList()` 返回 `List<User>`。
- `KPojo.where().select { ... }` 进入下一层时，`Source` 仍按 `select()` 的 `Selected = Source` 处理。

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

验收：

- derived select where 可生成 `FROM (SELECT ...) q WHERE q.alias ...`。
- `join(KSelectable)` 的 right lambda receiver 只暴露右侧 `Selected`。
- 内层参数能透传并在外层安全重命名。

## 任务 5：alias API 与命名诊断

目标：

- `.alias("name")` 命名当前层 `Selected` 字段。
- `.as_("name")` 删除、不可解析或报编译期错误。
- 直接字段投影可继承字段名。
- 函数、聚合、窗口函数、标量子查询、计算表达式作为 select item 时必须显式 alias。
- 同一层 `Selected` 最终字段名必须唯一。
- 新增 `Selected` 字段与 `Source` 字段同名时，`Context` 生成失败。
- 原 `Source` 字段按原名直接投影不算冲突。

诊断验收：

- `select { [f.length(it.name)] }` 报错。
- `select { [f.sum(it.amount)] }` 报错。
- `select { [Order().select { it.amount }.limit(1)] }` 报错。
- `select { [it.id, f.length(it.name).alias("id")] }` 报错。
- `select { [it.id, it.id] }` 报错。
- `select { [it.id, f.length(it.name).alias("status")] }` 在 `Source` 存在 `status` 时因 `Context` 冲突报错。

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

- `select { [it.id] }.where { it.name != null }` 可解析。
- `select { [it.id, f.length(it.name).alias("len")] }.orderBy { it.len.desc() }` 可解析。
- 同层 `where { it.len > 1 }` 不可解析或报自定义诊断。

## 任务 7：同层 where/having alias 能力删除

目标：

- 当前层 `where` 只能过滤输入行，即 `Source`。
- 当前层 `having` 只能使用基于 `Source` 的分组后表达式。
- 当前层 `where/having` 不允许访问当前层 `Selected` alias、聚合 alias、窗口 alias、标量子查询 alias。
- 旧自动分层能力降级为内部工具或删除，不能作为新 DSL 的正向语义。

需要删除或改写：

- `select { [it.id, it.name.as_("xx")] }.where { it.xx == "Ada" }` 正向测试。
- `select { [it.id, f.sum(...).as_("total")] }.having { it.total > 1 }` 正向测试。
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

- `.alias` 替代 `.as_` 的 core/compiler handoff。
- scalar shape 公共校验：单列、单行、limit、类型提示。
- 错误定位尽量放在 compiler diagnostics，runtime builder 保留兜底校验。

验收：

- `select { [it.id, query.limit(1).alias("lastAmount")] }` 正向。
- `where { it.price > query.limit(1) }` 正向。
- 多列 scalar、缺 limit、select item 缺 alias 均报错。

## 任务 10：谓词子查询

目标：

- 支持 `field in query` / `field !in query`。
- 支持 `exists(query)` / `!exists(query)`。
- 支持 `any(query)` / `some(query)` / `all(query)`。
- 支持 `[a, b] in query` / `[a, b] !in query` row-value tuple。
- 单元素 tuple 不允许，单列必须写 `field in query`。

需要补：

- 单值谓词右侧多列诊断。
- tuple 左右列数不一致诊断。
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

目标写法：

```kotlin
val ranked = Order()
    .select {
        [
            it.id,
            it.userId,
            f.rowNumber()
                .over(partitionBy = [it.userId], orderBy = [it.createTime.desc()])
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

- `.as_("x")` 使用报错或不可解析。
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
4. 删除 `.as_` 正向路径，补 `.alias` API 与诊断。
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
