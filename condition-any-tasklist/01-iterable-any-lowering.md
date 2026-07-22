# 任务 1：降低非空 Iterable.any 谓词

进度：100%
状态：已完成（非空集合路径）

## 目标

在条件 lambda 中为 Kotlin `Iterable<T>.any(predicate)` 增加一条窄范围的编译器插件 lowering，使非空运行时集合生成 syntax OR 树。

## 完成内容

- `ConditionAnalysis` 使用 Kotlin stdlib package、`Iterable` receiver、单个谓词参数共同确定 `Iterable.any(predicate)` 的 callable identity。
- 谓词 lambda 的返回值被重写为 `SqlExpr?`，运行时集合元素按原有迭代顺序执行该 lambda。
- `KTableForCondition.iterableAnyConditionExpr` 是 `@PublishedApi internal` 的 AST 聚合 bridge，正向路径复用 `orExpr`。
- 集合 receiver 作为 bridge 的单个实参进入生成 IR，`loadKeywords()` 一类表达式只求值一次。
- `KTableForCondition.any(query)` 保持量化子查询路径；PostgreSQL `f.any(...)` 保持 SQL 函数路径。

## 已验证

- `iterableAnyContains.kt` 断言两个关键字生成两个 `SqlExpr.Like` 子节点和一个 `SqlBinaryOperator.Or`，包含通配符转义、独立参数和迭代顺序。
- 用例断言 collection receiver 恰好求值一次。
- `iterableAnyCallableBoundaries.kt` 断言 PostgreSQL `f.any(...)` 仍生成 `SqlExpr.Function("ANY")`。
- 完整 `ConditionBoxTest` 覆盖既有的 `KTableForCondition.any(query)` quantified-subquery 用例。
- 实现复用既有 syntax builder 和参数绑定，不引入 raw SQL 或字符串拼接路径。

## 验证记录

- 2026-07-22：`./gradlew.bat :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ConditionBoxTest --no-daemon --console=plain` 通过。
- 2026-07-22：`./gradlew.bat :kronos-core:test --no-daemon --console=plain` 通过。
- 空集合和子表达式无值策略由任务 3 定义与验证。
