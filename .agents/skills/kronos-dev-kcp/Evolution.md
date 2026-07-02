# 演进记录

这个文件记录 Kronos KCP/FIR/IR 开发中反复出现的坑、原因和已经验证过的处理方式。

## 使用方式

- 修任何 KCP、FIR、IR 编译错误前，先读 `Evolution.index.md`，不要默认读取本文档全量内容；索引未命中时不要打开全文。
- 优先在索引里按错误症状、编译阶段、关键栈信息匹配已有记录。
- 只有索引命中时，才用定向搜索读取本文档中的对应条目，例如 `Select-String -Path .agents/skills/kronos-dev-kcp/Evolution.md -Pattern "问题描述" -Context 0,22`。
- 如果索引没有命中，继续读取相关 KCP reference 或代码搜索排查。
- 修复成功后，把新确认的问题、原因、方案和预防经验追加到这里，并同步给 `Evolution.index.md` 增加一条简短索引。
- 只记录已经验证过或高度确定的经验，不把猜测写成结论。

## 记录格式

```markdown
## [日期] - [问题描述]

### 问题症状
[准确错误、编译阶段、关键栈]

### 问题原因
[根因]

### 解决方案
[已验证修复方式]

### 预防措施
[后续如何避免]
```

## 2026-06-29 - FIR select 投影字段知道得太晚，不能普通插入源码作用域 class

### 问题症状
对于 `user.select { it.id }.queryList()`，选中的字段要到 `select` call refinement / body resolve 时才知道，但 `rows.firstOrNull()?.id` 又必须能在外层表达式里解析到一个可见的投影类型。

### 问题原因
call refinement / body resolve 晚于普通 block 声明收集。此时如果直接修改 parent FIR block，把 class prepend 到当前语句前，容易在 FIR 遍历 statements 时触发 concurrent modification。若改成 `run { class Projection; queryList() }`，local class 类型逃出 `run` 后又可能被近似，外层拿不到具体属性。

### 解决方案
优先用 FIR declaration generation 生成稳定 synthetic class，让前端解析能看到这个类型。call refinement 只负责把 `SelectClause<T, R>` 和 query 返回类型指向这个 synthetic projection class。

### 预防措施
不要在 call refinement 里修改 parent block 来插入 local class。把“前端可见声明”和“后端可执行实现”分成两个职责处理。

## 2026-06-29 - `select` 必须保留源 KPojo 作为 lambda receiver，同时 refine 结果投影

### 问题症状
`user.select { it.id }` 里的 `it` 必须仍然是完整源 KPojo，但 `queryList()` 希望返回 `List<GeneratedProjection>`。

### 问题原因
投影结果类型和源 receiver 类型不是同一个概念。如果把 `select` lambda receiver 改成投影类型，未选中的源表字段会在 select/where/orderBy/groupBy 等后续语义中丢失。

### 解决方案
runtime 仍保留裸 API：`T.select(fields: ToSelect<T, Any?>): SelectClause<T, T>`。FIR call refinement 将返回类型解释成 `SelectClause<T, GeneratedProjection>`，但不改变 lambda receiver。

### 预防措施
设计投影 API 时始终区分 source row scope 和 result projection type。projection class 只表示最终查询返回字段。

## 2026-06-29 - 投影结果模型和后续子句上下文模型不是同一个东西

### 问题症状
select/subquery alias 可能需要在后续 where/having/orderBy 里可见，但最终 query row 应该只包含 select 出来的字段。

### 问题原因
结果投影和子句上下文职责不同。结果投影是 `queryList()` 暴露给用户的元素类型；子句上下文可能需要“原 DTO 全部字段 + select 新增 alias”。

### 解决方案
模型上拆开。第一版可以只生成 `KronosSelectResult_*` 作为 query result。后续再生成 `KronosSelectScope_*`，或用等价的 synthetic receiver model 支撑 where/having/orderBy/groupBy。

### 预防措施
不要让一个“projection”模型同时承担结果行和子句 scope 两种语义，否则后续子查询和 alias 会混乱。

## 2026-06-29 - 生成投影 KPojo 必须是 data-class 形态，并且可无参构造

### 问题症状
Kronos KPojo runtime 需要 KPojo 类型具备无参构造路径。期望的源码等价形态是：

```kotlin
data class KronosSelectResult_x(
    var id: Int? = null,
    var name: String? = null,
) : KPojo
```

### 问题原因
投影 row 是结果映射目标。它既要像 Kotlin data class 一样暴露主构造属性，又要满足 Kronos KPojo 的实例化要求。

### 解决方案
FIR 生成 projection class 时应设置 data-class status，实现 `KPojo`，带 primary constructor，并给构造参数 null 默认值。backend IR 仍可能需要补可执行 body/default body 和 KPojo 方法。

### 预防措施
不要把最终投影类生成成普通 class。测试里要保留 constructor/default/no-arg 相关约束。

## 2026-06-29 - 可以用 suppress 直接调用 internal FIR API，但 `-Werror` 会卡住

### 问题症状
使用 `org.jetbrains.kotlin.fir.ownerGenerator` 等 internal API 时需要：

```kotlin
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
```

Kotlin 2.4 会对 `INVISIBLE_REFERENCE` suppress 本身报 warning。开启 warnings-as-errors 时，插件编译失败。

### 问题原因
这个 suppress 会产生“行为不保证稳定”的编译器 warning，而 `-Werror` 把 warning 提升为 error。

### 解决方案
在 compiler plugin module 内关闭 `allWarningsAsErrors`，至少在当前 internal API 路线未替换前如此。

### 预防措施
尽量不用反射，优先明确使用 internal API + suppress；但要把 internal API 使用点隔离，并确认该 module 不因 `-Werror` 被 warning 卡死。

## 2026-06-29 - 从 IR type argument 里 transform lazy FIR-backed projection class 会导致 property symbol unbound

### 问题症状
`ProjectionBoxTest.generatedSelectProjection` 在 IR generation 阶段失败：

```text
IrGenerationExtensionException: IrPropertySymbolImpl is unbound. Signature: null
...
Fir2IrLazyClass.getDeclarations
KronosParserTransformer.processKPojoClass
KronosParserTransformer.visitCall
```

### 问题原因
`visitCall` 从 query type argument 发现生成的 projection class 后，立即调用 `irClass.transform(KronosIrClassTransformer(...))`。这个 class 是 Fir2Ir lazy class，在 call 遍历中展开 declarations，会让 member scope 提前拉取还没绑定好的 generated property symbol。

### 解决方案
不要在 type-argument discovery 路径里立即 transform generated projection lazy class。可以收集它，但不要在那里对 lazy class 做 `transformChildren`。普通源码 KPojo class 仍可正常处理。

### 预防措施
通过 `IrType.getClass()` 间接拿到的 FIR-generated declaration 很可能是 lazy wrapper，不要默认它和源码 IR class 一样可展开、可 mutate。

## 2026-06-29 - FIR-generated projection property access 可能以 fake getter 进入 JVM 后端

### 问题症状
避开 lazy class 展开后，JVM codegen 失败：

```text
Fake override should have at least one overridden descriptor:
FUN FAKE_OVERRIDE name:<get-id> ... declared in com.kotlinorm.generated.projection.KronosSelectResult_*
```

前端已经能解析 `.id`，IR 里也有 `<get-id>` 调用，但 getter 仍是没有真实 overridden member 的 fake override。

### 问题原因
synthetic projection property 已经足够让源码解析通过，但它还没有被物化为 JVM codegen 可以发射的真实 backend declaration/body。

### 解决方案
需要在 backend IR 中把 projection member fake override 转成真实声明，思路类似现有 `KronosIrClassTransformer.replaceFakeBody`。但这个修复必须作用在可变 IR declaration 上，不能直接改 Fir2Ir lazy declaration。

### 预防措施
FIR resolution 通过不代表 JVM backend 能 codegen。凡是 frontend synthetic property，都要验证后端 accessor 是否是真实可发射声明。

## 2026-06-29 - 手动给 FIR property 加 accessor 可能触发 fake-override substitution 崩溃

### 问题症状
给 synthetic FIR property 手动补 `buildPropertyAccessor` getter/setter 后，Fir2Ir 在转换 property access 时失败：

```text
KotlinIllegalArgumentExceptionWithAttachments:
Exception was thrown during transformation of class FirPropertyAccessExpressionImpl
Caused by: java.util.NoSuchElementException: List is empty.
FirFakeOverrideGenerator.buildCopy
```

### 问题原因
手写的 FIR property/accessor 形态不符合当前编译器对 generated property、accessor、fake override 的内部预期。Fir2Ir 尝试构造 substitution override copy 时找不到必要 override 信息。

### 解决方案
除非已经对等价手写 Kotlin 的 FIR dump 做过对照，并确认 constructor-property/accessor 的完整形态，否则不要靠临时手补 FIR accessor 修这个问题。优先转到 backend IR fake member 修复，或改成完整受支持的 declaration generation 路线。

### 预防措施
遇到 `FirFakeOverrideGenerator` 相关崩溃，先撤销 ad hoc FIR accessor，和等价手写 Kotlin FIR 结构对比后再继续。

## 2026-06-29 - Fir2Ir lazy declaration 不能在 IR extension 中 mutate

### 问题症状
尝试在 `IrCall` 上拿到 projection getter 后直接修改 getter body，失败：

```text
IrGenerationExtensionException: Mutation of Fir2Ir lazy elements is not possible
Caused by: AbstractFir2IrLazyFunction.setBody
KronosProjectionIrTransformer.replaceProjectionGetterBody
```

### 问题原因
从 call symbol 拿到的 getter 是 `AbstractFir2IrLazyFunction`。Fir2Ir lazy declaration 在 IR generation extension 中不可变，不能设置 `body`、`isFakeOverride`、receiver 等。

### 解决方案
不要直接 mutate lazy Fir2Ir function/property。要么让 FIR 生成路径产出非 lazy、可变的 IR declaration；要么在 owner/file 层生成 concrete IR declaration，并把调用重定向到这个 concrete symbol。

### 预防措施
从 symbol 间接拿到 IR declaration 后，先判断它是不是 Fir2Ir lazy element。`replaceFakeBody` 适合 mutable source IR fake override，不适合 lazy FIR-backed declaration。

## 2026-06-29 - backend IR 可以修 fake override，但只能修可变 declaration

### 问题症状
仓库里已有可参考模式在 `KronosIrClassTransformer`：

```kotlin
declaration.isFakeOverride = false
declaration[IrParameterKind.DispatchReceiver] = irClass.thisReceiver
declaration.body = DeclarationIrBuilder(...).irBlockBody { ... }
```

属性也有类似模式：

```kotlin
declaration.isFakeOverride = false
declaration.addBackingField { ... }
declaration.addDefaultGetter(...)
declaration.addDefaultSetter(...)
```

### 问题原因
这些例子是在遍历 mutable IR class body 时操作真实 IR declaration。模式本身没问题，但前提是 declaration 不是 Fir2Ir lazy wrapper。

### 解决方案
当 projection declaration 以可变 IR member 出现时，复用 `replaceFakeBody` / `replaceFakeProp` 风格修 fake override。如果 projection class 仍是 lazy，必须先改变生成路径，让 class/member 变成真实 IR declaration，或把调用重定向到新生成的 concrete declaration。

### 预防措施
不要因为对象实现了 `IrSimpleFunction` / `IrProperty` 接口，就默认可以 mutate。关键要看 owner 的实际实现是不是 lazy。

## 2026-06-29 - official compiler test 的真实失败要看 XML 报告

### 问题症状
Gradle 控制台只显示：

```text
ProjectionBoxTest > generatedSelectProjection() FAILED
org.gradle.internal.exceptions.DefaultMultiCauseException
```

看不到 FIR/backend 的真实失败点。

### 问题原因
Kotlin official compiler test framework 会把多个 handler 的失败聚合到 JUnit failure 中。Gradle plain console 经常只显示外层异常，具体的 `NoFirCompilationErrorsHandler`、metadata compare、backend crash 等信息被折叠。

### 解决方案
失败后直接打开：

```text
kronos-compiler-plugin/build/test-results/test/TEST-*.xml
```

例如 `ProjectionBoxTest.generatedSelectProjection` 的真实失败是：

```text
MISSING_DEPENDENCY_CLASS: Cannot access class 'com.kotlinorm.generated.projection.KronosSelectResult_6ec22c25'
```

发生在 `NoFirCompilationErrorsHandler`。

### 预防措施
以后 official compiler test 失败时，先读 XML，再决定是 FIR diagnostic、metadata、Fir2Ir、backend codegen 还是 runtime box 问题。不要只根据 Gradle 控制台外层异常改代码。

## 2026-06-29 - `FirFunctionCallRefinementExtension.transform` 返回值必须仍是 `FirFunctionCall`

### 问题症状
想按文档把：

```kotlin
call()
```

改成：

```kotlin
run { class Projection; call() as Container<Projection> }
```

但 `transform(call, originalSymbol)` 的签名是：

```kotlin
FirFunctionCall
```

不能直接返回 `FirBlock`。

### 问题原因
body resolve 在完成 refined call 后直接执行：

```kotlin
result = callData.extension.transform(result, callData.originalSymbol)
```

因此插件必须返回一个表达式形态仍为 `FirFunctionCall` 的外壳调用，通常是 resolved `kotlin.run`/`let` call。local class 应放进这个外壳 call 的 lambda body。

### 解决方案
实验方向是用 `session.symbolProvider.getTopLevelFunctionSymbols(kotlin, run)` 找到 `kotlin.run`，构造 resolved `run` call，lambda body 中放 plugin-generated projection class 和 `select` call 的 cast 表达式。

### 预防措施
不要试图让 `transform` 直接返回 block。实现 local declaration wrapper 时，优先检查 Kotlin 当前版本的 FIR builder 和 body resolve 调用点。

## 2026-06-30 - 生成投影类要在后端物化后再进入 KClassCreator 映射

### 问题症状
`ProjectionBoxTest.generatedSelectProjection` 前端类型和 bytecode 物化后，运行期仍可能报：

```text
KClass GeneratedProjectionUser instantiation failed
```

或如果把 FIR-generated projection lazy class 直接纳入普通 KPojo 收集器，则容易再次触发 lazy declaration 展开和 unbound symbol 问题。

### 问题原因
`kClassCreator` 映射生成发生在 `@KronosInit` / `Kronos.init {}` 处理阶段。自动 select projection 的真实可调用 IR class 是后端 materializer 生成的 concrete class，不是 FIR2IR 暴露出来的 lazy projection class。若 map 生成早于 projection materialization，或把 lazy projection class 当成普通源码 KPojo 展开，会导致实例化路径缺失或 lazy symbol 失败。

### 解决方案
`KronosParserTransformer` 只收集源码 KPojo 和 init 入口，跳过 generated projection lazy class。`KronosProjectionIrTransformer` materialize 同名顶层 concrete projection class，生成无参构造并暴露 concrete `IrClass` 集合。`KronosIrGenerationExtension` 在 projection materialization 之后统一把源码 KPojo 与 materialized projection classes 合并传给 `KClassMapGenerator`。

### 预防措施
FIR 负责声明形状和 IDE/前端可见类型；backend IR 负责生成 projection class body、无参构造和实例化映射。不要把 FIR lazy projection class 传给普通 KPojo body transformer 或提前放进 `kClassCreator`。

## 2026-06-30 - KPojo val 属性写入和 projection 元数据需要官方 box 测试覆盖

### 问题症状
集成测试中 JDBC 映射 `Order(id, userId, orderDate)` 时，如果测试实体保持源码 `val`，动态 `set(name, value)` 没有写入任何字段；cascade projection 中生成类虽然能映射 projection 字段，但 source 表名和本地关联键不足会导致后置查询失败。

### 问题原因
后端把 KPojo 属性标记为 var 不等于已有生成逻辑会写入这些属性。`createPropertySetter`、`fromMapData`、`safeFromMapData` 之前按 `prop.setter != null` 过滤，源码 `val` 没有 setter 时不会生成写入分支。projection class 的 KPojo 元数据默认按生成类自身计算，不能表达 source table。

### 解决方案
KPojo 生成的动态 setter 和 map 写入逻辑按 backing field 生成写入分支，不再要求源码 setter。新增 official compiler box 测试覆盖源码 `val` 的 `set/fromMapData/safeFromMapData` 可写性，由官方 runner/IR verifier 验证生成 IR。projection materializer 对生成类传入 source metadata class，并新增 box 测试验证 projection 继承 source 表名、cascade-only projection 隐式保留本地键。

### 预防措施
凡是修改 KPojo body generation、projection materialization、cascade projection 行为，都要优先加 official `testData/box` 测试，不能只靠数据库集成测试发现运行期缺字段或无效 IR。

## 2026-06-30 - 二元运算符字段表达式不能再被旧字段列表诊断误拦截

### 问题症状
`select { it.score + 10 + 20 }`、`select { it.score % 2 }`、`where { it.score + 10 > it.score - 10 }` 等用户侧合法计算表达式在 `:kronos-core:test` 编译测试阶段失败，插件报：

```text
[Kronos] Operator expressions are no longer supported in Kronos field DSL.
[Kronos] Missing operand for 'GT' comparison
```

### 问题原因
字段列表已经从旧的 `it.a + it.b` 迁移到 `[]`，但 compiler plugin 的 `FieldAnalysis` 仍把所有 `plus/times/div/rem` 等 operator call 当成旧字段列表误用处理。给 core DSL 增加 `Any?.plus` 只能让 Kotlin nullable receiver 解析通过，不能让插件生成 SQL 表达式。

### 解决方案
保留旧字段列表的禁用语义，但允许二元 operator expression 作为计算字段或条件字段：

- `+` 根据操作数类型映射为 `add` 或 `concat`
- `-` 映射为 `sub`
- `*` 映射为 `mul`
- `/` 映射为 `div`
- `%` 映射为 `mod`
- `unaryPlus` 继续报错，不能静默忽略或支持

`ConditionAnalysis.extractFieldExpression` 也要识别这些 operator expression，否则比较表达式左侧/右侧是计算字段时会报 missing operand。

### 预防措施
修改字段列表语法诊断时，区分“旧投影列表写法”与“合法计算表达式”。新增运算符 DSL 能力时同时验证 select 字段、where/having 比较、字符串 concat 三类场景，不能只改 core 的占位 DSL 函数签名。

## 2026-06-30 - `Kronos.init` 调用点生成的 kClassCreator 在增量编译下可能变成旧快照

### 问题症状
core 普通测试新增顶层 `KPojo` 后，测试运行时报：

```text
NullPointerException: KClass Scene2User instantiation failed
```

但 `javap` 检查目标 class 已被插件增强，且有无参构造。进一步检查共享初始化入口 `KronosTestBase$Companion.ensureInitialized`，发现生成的 `kClassCreator` lambda 没有新 KPojo 分支。

### 问题原因
`KClassMapGenerator.generateForCallSite` 把当前编译看到的 `kPojoClasses` 快照写进 `Kronos.init {}` 调用点。如果该调用点在另一个文件中，增量编译新增 KPojo 文件时可能不重新编译 init 调用点，于是运行期 map 仍是旧快照。

### 解决方案
先用 module clean 验证是否为增量 stale：

```powershell
.\gradlew.bat "-Dkotlin.incremental=false" :kronos-core:clean :kronos-core:test --tests com.kotlinorm.orm.subquery.MysqlSubqueryDslTest --no-daemon --no-build-cache --console=plain
```

clean 后通过说明 KPojo 生成本身没坏，问题在 init 调用点快照失效。

### 预防措施
不要只根据 `KClass ... instantiation failed` 判断为无参构造缺失。对 `KClassMapGenerator` 相关问题要同时检查目标 KPojo class 和 init 调用点字节码。后续需要从 compiler/Gradle 集成层让 `Kronos.init` 调用点依赖全模块 KPojo 集合变化，或改造 map 生成机制避免旧快照。

## 2026-06-30 - 移动嵌套 selectable 调用时不要 deep-copy lambda local function

### 问题症状
为 `select { [query.as_("alias")] }` 生成 `addScalarSubquery(query, alias)` 调用时，官方 compiler box 在 IR verifier 阶段失败：

```text
Declaration with wrong parent:
FUN LOCAL_FUNCTION_FOR_LAMBDA name:<anonymous> ...
expectedParent: null
actualParent: FUN LOCAL_FUNCTION_FOR_LAMBDA ...
```

### 问题原因
`query` 表达式本身可能是 `Order().select { ... }`，内部携带 nested lambda local function。对整个 query expression 使用 `deepCopyWithSymbols()` 会复制 local function / lambda 节点，但 parent 没有按 FIR/IR 树重新归属，导致 verifier 认为插件生成了 invalid IR。

### 解决方案
当原始 return/list 表达式不会继续保留时，直接把原 `query` expression 移入生成的 `addScalarSubquery(...)` 调用，不要 deep-copy 整个 nested selectable call。只有在“原语句保留 + 额外插入派生调用”时才 deep-copy RHS，避免节点复用。

### 预防措施
IR transformer 处理嵌套 `select { ... }`、lambda、local function、function expression 时，不要机械套用 `deepCopyWithSymbols()`。先判断原节点是否还会留在树中：如果原节点被替换或搬移，优先 move；如果原节点仍保留，再对无 local declaration 风险的表达式做 deep-copy，并用官方 compiler test 的 IR verifier 验证。

## 2026-06-30 - Condition tuple IN 不要把 Field 塞进用户侧 Array<T>

### 问题症状
新增 official box 测试 `[it.id, it.status] in query` 时，编译通过但 box 运行失败：

```text
java.lang.ArrayStoreException: com.kotlinorm.beans.dsl.Field
at TupleInSelectableSubqueryKt.box$lambda$1(...)
```

### 问题原因
Kotlin 2.4 collection literal 在该条件表达式里会推断成用户字段类型数组，例如 `Array<Int?>`。如果 compiler plugin 试图直接把每个元素替换成 `Field` 或把 `Field` 当成 AST `Expression` 塞回这个数组，运行时就会向 `Integer[]` 写入 `Field`，触发 `ArrayStoreException`。

### 解决方案
Condition transformer 不要把 tuple 左值改写为用户侧数组结果。对 `KSelectable.contains(tuple)` 只构造 Criteria handoff：`CriteriaSubqueryValue.In(value = List<Field>, query = KSelectableQueryRef(...))`。core 的 `CriteriaToAstConverter` 再把 `List<Field>` lowering 成 `RowValueExpression(ColumnReference...)`。

### 预防措施
compiler plugin 的 condition 测试应验证 Criteria 结构，不要在 compiler 层直接构造 SQL AST。凡是处理 `[]` collection literal，都要注意源表达式的静态数组元素类型，不能把 `Field`/AST 节点写回用户数组。

## 2026-06-30 - Scalar subquery comparison RHS must be wrapped as CriteriaSubqueryValue.Scalar

### 问题症状
新增 official box 测试 `it.status > Order().select { it.status }.limit(1)` 时，Criteria 能生成 `ConditionType.GT`，但 `criteria.value` 是运行时 `SelectClause` 对象：

```text
Fail: value was com.kotlinorm.orm.select.SelectClause
```

core 的 `CriteriaToAstConverter` 因此会把 RHS 当普通值/字符串处理，而不是渲染成 `(SELECT ... LIMIT 1)`。

### 问题原因
`ConditionAnalysis.resolveValueExpression` 只会把字段/函数表达式转成 `Field` / `FunctionField`，其他表达式原样作为 Criteria value。`KSelectable` RHS 没有被包装成结构化 handoff，core converter 无法区分“普通参数值”和“标量子查询”。

### 解决方案
当 comparison RHS 的 IR type 是 `KSelectable` 或其子类型时，compiler 侧生成：

```kotlin
CriteriaSubqueryValue.Scalar(KSelectableQueryRef(query))
```

core 的 `CriteriaToAstConverter.buildComparisonExpression` 再把它 lowering 成 `DeferredSubqueryExpression.Scalar`，最终由 renderer 输出 scalar subquery SQL。

### 预防措施
新增任何子查询条件 DSL 时，compiler box 要断言 Criteria 的结构化 value 类型，而不是只看 ConditionType。不要把 `SelectClause` / `KSelectable` 运行时对象直接塞进普通 Criteria value。

## 2026-07-01 - FIR 成员过滤不要用名字字符串启发式

### 问题症状
读取 FIR class declarations 生成 projection/context 字段时，曾出现类似下面的过滤逻辑：

```kotlin
if (name.asString().startsWith("<")) return@forEach
```

这种写法看起来能跳过特殊成员，但它把 FIR/Name 的内部展示格式当成语义契约，容易在 Kotlin 版本、不同 declaration origin、local/synthetic declaration 形态变化时误判。

### 问题原因
`Name.asString()` 是名字文本，不适合作为判断 declaration 种类、来源或可见性的依据。用 `"<"` 这类字符串前缀推断 synthetic/local/internal 成员，会把编译器实现细节泄漏进插件逻辑，也会让 IDE/FIR 行为和 backend 行为难以稳定复现。

### 解决方案
过滤 FIR 成员时只使用结构化信息：

- 先按 declaration 类型筛选，例如只处理 `FirProperty`，不要从名字推断 constructor/accessor/function。
- 需要区分来源时检查 `origin`、`source`、`status.visibility`、`isLocal`、containing class/scope 等 FIR 元数据。
- 需要判断属性是否可用于 DSL context 时，检查 resolved return type、receiver/context、symbol 所属 class，以及是否是源码 KPojo 或插件明确生成的字段。
- 如果当前 FIR API 暂时拿不到可靠信息，宁可保守跳过并补测试，不要引入名字字符串启发式。

### 预防措施
后续修改 FIR declaration collection、projection context field merge、KPojo/generated class 收集器时，不要使用 `name.asString().startsWith("<")`、`contains("$")` 等名字格式判断。新增过滤规则必须能说明对应的 FIR 结构化依据，并用 official `testData` 覆盖至少一个会误判的场景。

## 2026-07-01 - compiler plugin 测试不要断言最终 SQL 字符串

### 问题症状
`projection/selectAliasContextWhere.kt` 最初在 official compiler box 中调用 `build(wrapper)` 并断言 SQL 包含 `FROM (SELECT ...)`、``WHERE `q`.`xx` = :xx`` 等渲染结果。这样会把 renderer、方言 quoting、自动分层策略的责任混进 compiler-plugin 测试。

### 问题原因
compiler plugin 的职责是让源码通过 FIR/IR 解析和改写，并把 DSL 表达式 handoff 到 core 可理解的结构。最终 SQL 字符串属于 core AST/rendering 层；真实数据库行为属于 testing 模块。compiler box 断 SQL 会让测试边界变模糊，后续 core renderer 调整也会误伤 compiler 测试。

### 解决方案
compiler official box 只验证最小 compiler 契约：

- FIR 能解析 generated projection/context 字段，例如 `where { it.xx == "Ada" }`。
- IR 能构造 runtime projection/context class，并把调用改写到对应 core API。
- 运行时能检查 Criteria / AST /参数 handoff，例如 `SelectStatement.where` 包含 alias column 和 named parameter，`params["xx"] == "Ada"`。

SQL 字符串、自动派生表分层、方言 quoting 和参数渲染放到 core 普通测试，例如 `SubqueryRendererTest` 或具体方言 DSL test。真实连接数据库的行为放 `kronos-testing`。

### 预防措施
新增 compiler-plugin testData 时，避免读取 `task.sql`、断言 `SELECT` / `WHERE` 字符串或依赖方言 quoting。需要验证 SQL 时，新增 core 单测；需要验证真实数据库语义时，新增 testing 集成测试。compiler 层优先断言 FIR 可见性、IR verifier、生成类映射、Criteria/AST handoff 和参数 map。

## 2026-07-02 - generated projection 作为下一层 Source 时不要展开 FIR lazy class

### 问题症状
新增窗口函数下一层过滤 core 测试时，`ranked.select { ... }` 触发 core 测试源码编译阶段崩溃：

```text
IrGenerationExtensionException: IrConstructorSymbolImpl is unbound. Signature: null
at KronosProjectionIrTransformer.materializeGeneratedProjectionClass
```

### 问题原因
外层 `KSelectable<S>.select { ... }` 的 `Source` 可能是上一层 FIR-generated projection class。backend materializer 在创建外层 generated projection 时，如果直接读取 `sourceType.classOrNull.owner.properties`，就会展开 FIR2IR lazy class 的 constructor/property symbols。上一层 projection 已经有 backend materialized concrete class，但旧逻辑没有优先使用它。

### 解决方案
`KronosProjectionIrTransformer` 对 generated projection source 分两类处理：

- 字段类型来源优先使用 `materializedProjectionClasses[sourceFqName]` 的 concrete class。
- 表元数据来源使用上一层记录的 metadata class，避免外层 projection 把表名退化成 generated class 名。
- 如果 generated source 尚未 materialize，则保守不展开 lazy class。

已用以下命令验证：

```text
./gradlew :kronos-compiler-plugin:compileKotlin --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1
./gradlew :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.SelectBoxTest.windowFunctionOver --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1
./gradlew :kronos-core:test --tests 'com.kotlinorm.orm.subquery.MysqlSubqueryDslTest.window*' --no-daemon --console=plain -Dkotlin.incremental=false -Dorg.gradle.workers.max=1
```

### 预防措施
任何从 `IrType.classOrNull.owner` 得到的 generated projection class 都应先判断是否已有 backend materialized class。不要在下一层 select、join selectable、insert-select、CTAS 等消费 generated `Selected` 的路径中直接展开 FIR lazy class declarations。
