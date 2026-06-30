# Evolution

This file records recurring Kronos-ORM development pitfalls and their proven fixes.

## 使用说明

- 修复报错、构建失败或异常行为前，先读 `Evolution.index.md`，不要默认读取本文档全量内容；索引未命中时不要打开全文。
- 只有索引命中时，才用定向搜索读取本文档中的对应条目，例如 `Select-String -Path .agents/skills/kronos-dev-guide/Evolution.md -Pattern "问题描述" -Context 0,20`。
- 如果索引没有命中，继续按 skill 里的相关 reference 或代码搜索排查。
- 每次构建成功或问题修复后，如果遇到值得复用、已经验证的重要问题与解决方案，将完整记录追加到本文档，并同步给 `Evolution.index.md` 增加一条简短索引。
- 遵循已有的最佳实践，避免重复犯错。

## 记录格式

```markdown
## [日期] - [问题描述]

### 问题症状
[描述问题的具体表现和错误信息]

### 问题原因
[分析问题的根本原因]

### 解决方案
[列出解决该问题的具体步骤]

### 预防措施
[列出如何避免此类问题的最佳实践]
```

## 2026-06-27 - Collection literal tests require the explicit compiler flag

### 问题症状
在 compile-testing 动态编译测试中使用 `select { [it.id, it.name] }`、`reference { [it::id, it::name] }` 或类似 collection literal 语法时，如果没有为动态编译器传入 `-Xcollection-literals`，测试源码无法按 Kotlin 2.4 集合字面量语法解析。

### 问题原因
Kronos 的主 Gradle 编译参数和 compile-testing 动态编译参数是两套配置。根项目 `freeCompilerArgs` 不会自动传递给 `KotlinCompilation.kotlincArguments`。

### 解决方案
在 compile-testing 工具入口中显式设置 `kotlincArguments = ["-Xcollection-literals"]`。Kotlin 2.4.0 正式版已验证 `reference { [it::id, it::name] }` 可以通过动态编译测试。

### 预防措施
新增或修改 compile-testing 用例时，如果源码包含 `[]` 集合字面量，先确认对应测试工具入口传入了 `-Xcollection-literals`；不要把主工程编译参数等同于动态编译参数。

## 2026-06-27 - Field projection plus must not be reused for arithmetic

### 问题症状
将字段列表从 `select { it.a + it.b }` 迁移到 `select { [it.a, it.b] }` 后，如果测试或文档仍保留旧字段列表写法，SQL 会变为 `(... + ...) AS add` 这类真实加法表达式，导致断言失败。

### 问题原因
`+` 已改为真实运算符函数表达式，不再表示字段投影列表。旧测试输入会被正确解析为 `FunctionField("add", ...)`。

### 解决方案
字段投影列表统一迁移为 `[]`、`listOf(...)` 或投影调用；只在真正的算术或字符串拼接场景保留 `+`。

### 预防措施
修改 select/orderBy/groupBy/by/update/upsert/reference/cascade 示例或测试时，先扫描旧字段列表模式，确保 `+` 只表示函数/算术语义。

## 2026-06-27 - Typed select projection must preserve source DTO as lambda receiver

### 问题症状
为 `select` 增加投影 DTO 类型时，如果尝试只暴露 `select<R> { ... }` 或把 receiver 擦成 `KPojo`，会导致 `select` lambda 内的 `it` 不再是源表完整 DTO，或者普通 `select { ... }` 与投影重载发生 overload ambiguity。

### 问题原因
Kotlin 扩展函数里 `select` 同时需要源 DTO 类型 `T` 和投影 DTO 类型 `R`。`T` 必须用于 `ToSelect<T, Any?>`，否则字段选择、where/orderBy/groupBy/cascade 都会丢失源表字段信息。当前阶段若用同名 `select<T, R>`，还需要避免无显式类型参数调用时参与普通 `select {}` 的重载竞争。

### 解决方案
当前阶段使用显式双类型参数：`source.select<Source, Projection> { [it.id, it.name] }`，其中 lambda 的 `it` 仍是 `Source`，返回值为 `SelectClause<Source, Projection>`。投影重载增加默认 `projectionClass: KClass<R> = R::class` 参数，并将 `fields` 保持为最后一个参数，从而保留 trailing lambda 且避免普通 `select {}` 歧义。

### 预防措施
不要实现会让 `it` 变成投影 DTO `R` 的 `select` API；投影类型只应影响 `SelectClause<T, R>` 的返回类型、子查询类型和最终 KPojo 映射。后续若由编译器插件省略类型参数，应同时推断 `T` 和 `R`，而不是支持 `select<R>` 这种中间态。

## 2026-06-27 - Generated IR nodes must not be reused across parents

### 问题症状
Kotlin 官方 compiler test framework 运行 KPojo box 测试时，IR verifier 报错：
`the compiler plugin 'com.kotlinorm.compiler.plugin.KronosIrGenerationExtension' generated invalid IR`
并指出 `Duplicate IR node: GET_VAR ...`、`Duplicate IR node: CONST String ...` 等。

### 问题原因
生成 KPojo 成员体时缓存并重复使用了同一个 `IrExpression` 节点，例如 `irGet(dispatchReceiver)`、`irGet(name)`、`irGet(map)`、注解参数里的 `IrConst` / enum / vararg，以及 `@Table` 注解参数。IR 树要求表达式节点只能有一个父节点，不能在多个 branch、constructor argument 或 return body 中共享。

### 解决方案
把可复用值改为 factory：每次使用时重新调用 `irGet(...)`、`irString(...)`、`irBoolean(...)` 等创建新节点。不要把 annotation argument 原节点直接塞进生成代码，必须按值重建 String/Int/Boolean/enum/vararg。动态 setter 只为有 setter 的 `var` 属性生成分支；`toDataMap` / `fromMapData` 分别遵守 `IgnoreAction.TO_MAP` / `IgnoreAction.FROM_MAP`。

### 预防措施
写 IR 生成代码时不要缓存 `IrExpression` 后多处插入；可以缓存 symbol、type、字符串值、布尔值等普通数据。官方 compiler test framework 的 IR verifier 应作为 KPojo 生成逻辑的主要回归防线。

## 2026-06-27 - Set transformer must clone RHS expressions when adding setValue/setAssign calls

### 问题症状
官方 compiler test framework 运行 `set` DSL box 测试时，IR verifier 报错：
`Duplicate IR node: CONST Int ...`、`Duplicate IR node: CONST String ...`、`Duplicate IR node: GET_VAR '$this$...'`。
错误位置在生成的 `KTableForSet.setValue(...)` / `setAssign(...)` 调用内部。

### 问题原因
`SetTransformer` 生成 `setValue` / `setAssign` 调用时，把原赋值表达式的 RHS 节点直接作为生成调用参数，同时原赋值语句仍保留在 IR 树中；同一个 `irGet(extensionReceiver)` 也被多个生成调用共享。IR 表达式节点只能有一个父节点，因此 verifier 判定为 invalid IR。

### 解决方案
生成每个 `setValue` / `setAssign` 调用时重新创建 dispatch receiver：`builder.irGet(irFunction.parameters.extensionReceiver!!)`;
对原 RHS 使用 `deepCopyWithSymbols()` 后再放入生成调用参数，避免和原赋值语句共享节点。

### 预防措施
Transformer 在“保留原语句 + 额外插入派生调用”时，不能复用原语句里的 `IrExpression`。RHS、receiver、常量、临时取值都要重新构造或 deep-copy。

## 2026-06-30 - Cascade projection rows must keep source table metadata and local keys

### 问题症状
`Book(1).select { it.chapters }.queryList().flatMap { it.chapters }` 返回的 projection row 中 `chapters` 为 null，早期还会因为父行 `toDataMap()` 只有 `{chapters=null}` 而无法构造子表查询条件。修正方向后又出现 `Book.chapters -> Chapter.book -> Book.chapters` 的递归查询导致 `StackOverflowError`。

### 问题原因
select projection 生成类默认以自身类名生成 `__tableName` 和 `Field.tableName`，而 cascade runtime 需要它作为 source row 参与 `id -> bookId` 这类关联键推导。只选择 cascade 字段时，projection 结果类型还需要隐式保留本地关联键，否则后置 cascade 查询拿不到父行 id。递归查询时子查询默认继续 cascade，会反向展开关联。

### 解决方案
projection class 的 KPojo 元数据使用 source class 作为 metadata owner：`__tableName` 和 `kronosColumns().tableName` 与 source 表一致。物化 projection class 时，对选中的 `@Cascade` 字段追加 `@Cascade.properties` 中声明的本地键作为隐藏 projection property。Cascade setValues 的映射方向使用 `ValidCascade.mapperByThis`，不要用 projection row 的运行时表名猜方向。SELECT 的 cascade 子查询禁用继续 cascade，避免默认反向无限展开。

### 预防措施
新增/修改 projection + cascade 行为时，必须同时覆盖：
- 只选 cascade 字段时 projection row 能保留本地键；
- projection row 的 `__tableName` 和 Field 表名等于 source 表；
- cascade after-query 不会沿反向关系无限递归。

## 2026-06-30 - Reverse cascade annotations must also provide projection local keys

### 问题症状
当只有子端属性声明 `@Cascade(["bookId"], ["id"]) val book: Book?`，父端 `Book.chapters` 不重复声明 `@Cascade` 时，`Book().select { it.chapters }.queryList()` 的 projection row 仍可能缺少隐藏的 `id` 本地键，导致后置 cascade 查询不执行或 `chapters` 为 null。

### 问题原因
runtime 的 cascade 发现逻辑支持从目标类反向扫描有效关系，但 projection materializer 只按被选中源属性上的直接 `@Cascade` 补隐藏本地键。对于 `Book.chapters` 这种未直接标注、由 `Chapter.book` 反向声明的关系，生成 projection class 时没有把反向 cascade 的 target properties 追加进 projection。

### 解决方案
projection materializer 在处理选中字段时同时收集：
- 源属性自身 `@Cascade.properties` 声明的本地键；
- 源属性目标类中指回 source class 的反向 `@Cascade.targetProperties`。

生成的隐藏字段继续使用 source class 的字段类型和 metadata owner，避免 projection 自身表名污染 cascade 映射。

### 预防措施
projection + cascade 测试必须覆盖“父端不标注、子端反向标注”的场景；不要通过在父端重复加 `@Cascade` 掩盖反向扫描缺陷。

## 2026-06-30 - Test-local private KPojo can poison global KClassCreator discovery

### 问题症状
运行 `:kronos-core:test --tests com.kotlinorm.ast.SubqueryRendererTest --tests com.kotlinorm.orm.select.SelectClauseAstTest` 时，`SelectClauseAstTest` 全部失败：

```text
IllegalAccessError: failed to access class ...SubqueryRendererTest$ParameterCollectingPojo
NoClassDefFoundError: com/kotlinorm/ast/SubqueryRendererTest$ParameterCollectingPojo
```

失败发生在 `KronosTestBase.ensureInitialized` 初始化 `kClassCreator` 后，后续测试构造任意 `SelectClause` 时触发。

### 问题原因
测试文件中新增了 `private` nested `KPojo` 辅助类。全局测试初始化会扫描/缓存测试 classpath 中的 `KPojo` 类型，private nested class 不能被其他测试辅助类访问。把它移到顶层后，如果没有清掉旧测试 class，旧的 `SubqueryRendererTest$ParameterCollectingPojo` 仍可能残留在 `build/classes/kotlin/test` 并继续被扫描。

### 解决方案
测试专用 `KPojo` 不要定义为 private nested class；改为文件级 `internal` class 或放入专用公开测试 fixture。修复后需要运行：

```powershell
.\gradlew.bat :kronos-core:clean :kronos-core:test --tests com.kotlinorm.ast.SubqueryRendererTest --tests com.kotlinorm.orm.select.SelectClauseAstTest --no-daemon --no-build-cache --console=plain
```

仅 `cleanTest` 不够，因为它不一定清理 Kotlin test classes。

### 预防措施
新增 core 测试 KPojo 时，避免 private/local/nested class 形态。凡是失败栈里出现 `KronosTestBase.ensureInitialized`、`kClassCreator`、`NoClassDefFoundError` 指向旧测试类名，先执行 module `clean` 清理 stale class，再判断是否为真实业务回归。

## 2026-06-30 - Shared Kronos.init can hold a stale kClassCreator snapshot under incremental test compilation

### 问题症状
新增 core 普通测试文件里的顶层 `KPojo`，例如 `Scene2User`，类本身已经被 compiler plugin 增强并生成无参构造，但运行：

```powershell
.\gradlew.bat :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --console=plain
```

仍失败：

```text
NullPointerException: KClass Scene2User instantiation failed
```

用 `javap` 检查可见 `Scene2User.class` 已有无参构造和 KPojo 生成方法，但 `KronosTestBase$Companion.ensureInitialized` 内生成的 `kClassCreator` lambda 没有 `Scene2User` 分支。

### 问题原因
`kClassCreator` 是由 compiler plugin 在 `Kronos.init {}` 调用点生成的 KPojo 类快照。core 测试复用 `KronosTestBase.ensureInitialized` 作为共享 init 入口；当只新增/修改某个测试文件时，增量编译可能不会重新编译 `KronosTestBase.kt`，导致该调用点里的 `kClassCreator` 仍是旧快照，缺少新测试文件里的 KPojo。

### 解决方案
先用干净编译确认是否为 stale snapshot：

```powershell
.\gradlew.bat "-Dkotlin.incremental=false" :kronos-core:clean :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --no-build-cache --console=plain
```

如果 clean 后通过，说明实体增强和无参构造没有问题，失败来自增量编译下共享 init 的旧 `kClassCreator`。短期测试排查不要直接改用 sample bean 掩盖问题；应先确认 `javap` 中 init lambda 是否包含目标 KPojo 分支。

### 预防措施
新增 core test-local KPojo 后，如果失败指向 `KClassCreatorKt.createInstance`，先检查：

- 目标 KPojo class 是否已生成无参构造；
- 共享 `Kronos.init` 调用点的 `kClassCreator` lambda 是否包含该类；
- 干净编译是否通过。

这是一个需要后续从 compiler/Gradle 增量编译依赖侧处理的风险：`@KronosInit` / `Kronos.init` 调用点生成的 KPojo map 依赖全模块 KPojo 集合，增量编译必须让 init 调用点在 KPojo 集合变化时失效。

## 2026-06-30 - DeleteClause must preserve fieldless SQL criteria such as EXISTS

### 问题症状
`Scene2User().delete().logic(false).where { exists(query) }.build()` 渲染成：

```text
DELETE FROM `tb_scene2_user`
```

预期的 `WHERE EXISTS (...)` 被整个丢弃。同一个 `exists(query)` 在 select/update 场景可以正常渲染。

### 问题原因
`DeleteClause.toStatement()` 在转换前会调用 `filterEmptyCriteria`。旧逻辑只保留 `criteria.field.name` 非空的叶子条件；而 `exists(query)` 由 compiler plugin 生成的是 `ConditionType.SQL`，field 是空占位，value 是 `CriteriaSubqueryValue.Exists`。因此 delete 专属过滤把有效的 fieldless SQL criteria 当空条件删除了。

### 解决方案
`DeleteClause.filterEmptyCriteria` 必须保留 `criteria.type == ConditionType.SQL && criteria.value != null` 的叶子条件。修复后用以下命令验证：

```powershell
.\gradlew.bat "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.incremental=false" :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --no-build-cache --console=plain
```

### 预防措施
新增 fieldless criteria 类型时，例如 `EXISTS`、raw SQL、后续 scalar/quantified wrapper，不要用“field name 非空”作为唯一有效性判断。select/update/delete 三条 clause 路径都要覆盖 core DSL -> SQL/参数测试，因为 delete 有额外的 empty criteria 过滤。

## 2026-06-30 - INSERT SELECT must share source query parameter values

### 问题症状
`Scene2Order().select { ... }.where { it.status == 13 }.insert<Scene2OrderArchive>().build()` 生成的 SQL 正确包含：

```text
WHERE `status` = :status
```

但最终参数是：

```text
{status=null}
```

预期是 `{status=13}`。

### 问题原因
`InsertClause.toStatement()` 之前用 `sourceQuery?.toStatement(wrapper)` materialize source select，没有把外层 insert 的参数 map 传进去。source query 自己生成了 `:status` 占位符，但对应的 criteria 参数值没有进入 insert action task。后续 `renderStatement` 又从目标表字段/默认值里补参数，导致同名 `status` 被目标实例默认 null 填上。

### 解决方案
`InsertClause.build()` 创建共享的 `sourceParameterValues`，调用 `toStatement(wrapper, sourceParameterValues)`；`InsertClause.toStatement` 内部调用 `sourceQuery.toStatement(wrapper, parameterValues)`；`renderStatement` 再把 `sourceParameterValues` 中 SQL 实际使用的参数合并到最终参数 map。

验证命令：

```powershell
.\gradlew.bat "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.incremental=false" :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --no-build-cache --console=plain
```

### 预防措施
凡是 builder 把一个 `KSelectable` 嵌入另一个 statement，例如 insert-select、CTAS、upsert assignment、derived table、scalar subquery，都必须复用或显式传递同一个 parameter map。不要只检查 SQL 字符串，必须断言最终 action task 的参数值。

## 2026-06-30 - UPSERT expression assignments must not overwrite insert parameters

### 问题症状
`Scene2User(id = 1, name = "seed").upsert().patch("name" to query.limit(1)).on { it.id }.onConflict().build()` 能生成正确的 SQL：

```text
ON DUPLICATE KEY UPDATE `name` = (SELECT ... WHERE `status` = :status LIMIT 1)
```

但最终参数一开始是：

```text
{id=1, name=com.kotlinorm.orm.select.SelectClause@...}
```

修掉覆盖后又变成：

```text
{id=1, name=seed}
```

缺少子查询参数 `status=15`。

### 问题原因
`UpsertClause.patch` 同时服务普通参数 assignment 和 expression assignment。旧逻辑把所有 patch 值都写回 `paramMap`，导致 `KSelectable` 覆盖 insert values 中同名字段的普通值。expression upsert 分支随后直接调用 renderer，renderer 默认 lowering 没有共享外层 parameter map，子查询 criteria 参数不会进入最终 action task。

### 解决方案
`UpsertClause.build` 合并 `paramMapNew` 时跳过 `!value.requiresBuilderParameter()` 的 expression/selectable 值，保留 insert 参数中的普通字段值。expression upsert 分支在渲染前显式调用：

```kotlin
SubqueryLowering.lower(statement, QueryMaterializeContext(wrapper, subqueryParameterValues))
```

再把 `subqueryParameterValues` 中 SQL 实际使用的参数合并到 rendered params。

验证命令：

```powershell
.\gradlew.bat "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.incremental=false" :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --no-build-cache --console=plain
```

### 预防措施
任何 patch/set API 一旦同时支持普通参数和 expression/selectable assignment，都必须分开处理：普通值进入 DML 参数 map，expression/selectable 只进入 AST assignment。renderer 默认 lowering 不足以收集外层 action task 需要的参数；需要共享 `QueryMaterializeContext.parameterValues`。

## 2026-06-30 - CTAS must share source query parameter values

### 问题症状
`dataSource.table.createTable(target, sourceQuery.where { ... })` 能渲染出带 `WHERE status = :status` 的 CTAS SQL，但执行任务最初只携带 `{tableName=...}`，source query 的 `status` 参数没有进入 `KronosAtomicActionTask.paramMap`。

### 问题原因
`TableOperation.buildCreateTableAsSelectStatement` 直接调用 `query.toStatement(dataSource)`，没有向 source query 传入共享参数 map；`createTable(target, query)` 渲染 DDL 后也没有合并 source query materialize 阶段收集到的参数。

### 解决方案
CTAS task 构建时创建共享 `parameterValues`，调用 `query.toStatement(dataSource, parameterValues)`；渲染前再用同一个 `QueryMaterializeContext` lowering。最终 action task 参数从 renderer 参数和 SQL 实际引用到的 `parameterValues` 合并得到。

### 预防措施
任何 DDL/DML builder 只要嵌入 `KSelectable`，包括 CTAS、insert-select、upsert assignment、derived table，都必须用共享 parameter map materialize source query，并在普通测试中同时断言 SQL 与最终 task 参数。

## 2026-06-30 - Tuple IN compiler handoff should lower List<Field> in core

### 问题症状
实现 `[it.userId, it.status] in query` 时，如果 compiler plugin 直接尝试产出 `RowValueExpression`，容易把 `Field` 当成 `Expression` 写进错误的运行时结构；而 core SQL 渲染真正需要的是 `(user_id, status) IN (SELECT ...)`。

### 问题原因
compiler condition transformer 的职责是把用户 DSL lambda 转成 `Criteria` handoff，不应该越界到 SQL AST 生成。row-value tuple 的 SQL 形态依赖 renderer/criteria converter 对字段、表别名、数据库名的统一处理。

### 解决方案
compiler 侧生成 `CriteriaSubqueryValue.In(query = KSelectableQueryRef(...), value = List<Field>)`。core 的 `CriteriaToAstConverter` 识别 `List<Field>`，在转换阶段生成 `RowValueExpression(ColumnReference...)`，再走统一 deferred subquery lowering/rendering。

### 预防措施
新增 compiler DSL 能力时先明确 handoff 类型：compiler 测 Criteria 结构，core 普通测试测 SQL/参数。不要在 compiler 测试里承担 SQL rendering，也不要让 core 假设 compiler 已经生成最终 AST。

## 2026-06-30 - DeleteClause must preserve all fieldless structured subquery criteria

### 问题症状
`Scene2Order().delete().logic(false).where { [it.userId, it.status] in query }.build()` 渲染成：

```text
DELETE FROM `tb_scene2_order`
```

预期的 `WHERE (user_id, status) IN (SELECT ...)` 被删除。`select` / `update` 的相同 tuple IN 条件可以正常渲染。

### 问题原因
`DeleteClause.filterEmptyCriteria` 之前只保留有字段名的 leaf criteria，以及 `ConditionType.SQL && value != null` 的 fieldless criteria。tuple IN 由 compiler plugin 生成的是 `ConditionType.IN`，field 是空占位，value 是 `CriteriaSubqueryValue.In(value = List<Field>, query = ...)`，因此被 delete 专属过滤当成空条件丢弃。

### 解决方案
`DeleteClause.filterEmptyCriteria` 必须保留所有 `criteria.value is CriteriaSubqueryValue` 的 fieldless structured criteria，而不是只特判 `ConditionType.SQL`。修复后用以下命令验证：

```powershell
.\gradlew.bat "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.incremental=false" :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --no-build-cache --console=plain
```

### 预防措施
新增 fieldless criteria 类型时，例如 tuple IN、EXISTS、scalar subquery、quantified comparison，不要用 field name 非空判断有效性。delete 普通删除和逻辑删除路径都要覆盖 core DSL -> SQL/参数测试。

## 2026-06-30 - CriteriaSubqueryValue.In NOT flags must be merged with OR, not XOR

### 问题症状
修 `CriteriaToAstConverter` 时，为了让手写 `Criteria(Field("id"), ConditionType.IN, not = true, value = CriteriaSubqueryValue.In(query))` 生成 `NOT IN`，如果把外层 `criteria.not` 与 `CriteriaSubqueryValue.In.not` 用 XOR 合并，会导致用户 DSL 路径里的 `field !in query` 反而渲染成普通 `IN`。

### 问题原因
compiler DSL handoff 路径里，`!in` 可能同时让外层 `Criteria.not` 和结构化 `CriteriaSubqueryValue.In.not` 都为 true。两者不是互斥标记，而是两个来源的同一语义标记。`applyNot` 对 `DeferredSubqueryExpression.In` 会跳过二次包裹，因此结构化分支必须自己把 NOT 保留下来。

### 解决方案
`CriteriaToAstConverter` 中结构化 IN 分支使用：

```kotlin
not = value.not || criteria.not
```

普通 `SelectQueryRef` IN 分支则使用：

```kotlin
not = criteria.not
```

用 `SubqueryRendererTest.criteria converter accepts structured subquery predicates` 覆盖手写 Criteria，用 `MysqlSubqueryDslTest` 覆盖用户 DSL 的 `in` / `!in`、tuple IN / NOT IN。

### 预防措施
新增结构化 Criteria handoff 时，区分“语义标记的多来源合并”和“逻辑反转”。对于 NOT IN / NOT EXISTS 这类结构化表达式，优先用普通测试同时覆盖手写 core Criteria 和用户 DSL 生成路径，避免只修一边。
