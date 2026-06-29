# 演进记录

这个文件记录 Kronos KCP/FIR/IR 开发中反复出现的坑、原因和已经验证过的处理方式。

## 使用方式

- 修任何 KCP、FIR、IR 编译错误前，先读这个文件。
- 优先按错误症状、编译阶段、关键栈信息匹配已有记录。
- 修复成功后，把新确认的问题、原因、方案和预防经验追加到这里。
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
