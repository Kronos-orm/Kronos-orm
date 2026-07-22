# 任务 1：降低非空 Iterable.any 谓词

进度：0%
状态：待处理

## 目标

在条件 lambda 中为 Kotlin `Iterable<T>.any(predicate)` 增加一条窄范围的编译器插件 lowering，使非空运行时集合生成 syntax OR 树。

## 当前状态

- `ConditionAnalysis.analyzeMethodSqlExpr` 按名称分发已有条件调用，但没有集合 `any` 分支。
- `containsConditionExpr` 已能把一个动态关键字转换为带命名参数、带通配符转义的 `SqlExpr.Like`。
- `orExpr` 已能把非空 `List<SqlExpr?>` 折叠为所需的 `SqlExpr.Binary(... Or ...)` 结构。
- `KTableForCondition.any(query)` 与 `FunctionHandler.any(...)` 同名，因此按名字实现一定不正确。

## 后续工作

- 增加 Kotlin `Iterable.any(predicate)` 重载的已解析符号判定，检查 stdlib owner、receiver 类型、参数数量和谓词函数形状。
- 在 `ConditionAnalysis` 中重建返回 syntax 的谓词 lambda，对每个集合元素按迭代顺序执行一次。
- 通过现有 `orExpr` 聚合子表达式；若需要内部辅助方法，它必须保持相同的 AST-only 契约且不暴露为用户 API。
- 在生成的迭代和聚合逻辑前保存集合 receiver，确保它只求值一次。
- 新增官方 compiler box 测试，例如 `condition/iterableAnyContains.kt`，并在 `ConditionBoxTest` 中增加 runner。

## 验收

- `where { keywords.any { keyword -> it.name.contains(keyword) } }` 在两个元素时生成两个 `SqlExpr.Like` 子节点和一个 `SqlBinaryOperator.Or`。
- 参数 map 中包含各自独立、按集合顺序分配且正确加通配符转义的值。
- 实现中不出现 `SqlExpr.UnsafeRaw`、`asSql`、`patch` 或手工拼接 SQL。
- 测试证明 `KTableForCondition.any(query)` 与 `f.any(...)` 维持既有行为，不会被识别为集合谓词。
- 目标官方 compiler box suite 通过。

## 验证记录

- 当前静态检查：尚未实现；`ConditionAnalysis` 没有 `Iterable.any` lowering 分支。
- 目标命令：`./gradlew.bat :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ConditionBoxTest --no-daemon --console=plain`。
