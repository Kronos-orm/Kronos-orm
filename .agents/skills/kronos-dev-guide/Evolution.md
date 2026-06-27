# Evolution

This file records recurring Kronos-ORM development pitfalls and their proven fixes.

## 使用说明

- 每次构建成功后，将遇到的重要问题与解决方案记录到此文件
- 每次修复报错前,先阅读此文件了解历史踩坑情况
- 遵循已有的最佳实践,避免重复犯错

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
