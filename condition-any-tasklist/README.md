# 集合 `any` 条件 DSL 任务清单

更新日期：2026-07-22

本清单基于当前条件 DSL、syntax AST、编译器插件分析和现有编译器测试结构，目标是在 Kronos 条件 lambda 中支持 Kotlin 集合 `any { ... }`，并保持普通 `||`、`&&`、`!` 组合方式不变。

## 入口

- [设计锁](00-design-locks.md)
- [推荐实施顺序](implementation-order.md)
- [当前验证缺口](verification-gaps.md)
- [验证记录](verification-log.md)
- [非目标](out-of-scope.md)

## 任务文档

- [任务 1：降低非空 Iterable.any 谓词](01-iterable-any-lowering.md)
- [任务 2：保持逻辑组合与取反](02-logical-composition-and-negation.md)
- [任务 3：定义空集合与无值语义](03-empty-and-no-value-semantics.md)
- [任务 4：文档与回归验证](04-documentation-and-regression.md)

## 当前判断

- `KTableForCondition` 已能构造 `SqlExpr.Like`、`SqlExpr.Binary(And/Or)` 和命名参数；syntax 模型不需要新增 `LikeAny` 节点。
- `ConditionAnalysis` 通过 Kotlin stdlib 的已解析 callable、Iterable receiver 和谓词形状识别 `Iterable.any(predicate)`，将子谓词交给同一条 `SqlExpr?` 构造管线。
- `orExpr(emptyList())` 当前返回 `null`，表示省略条件。此既有语义不能全局改变，而空集合 `any` 必须为 false。
- 同名的量化子查询 `any(query)` 与 PostgreSQL 函数 `f.any(...)` 保持各自的 lowering 路径。

## 总览

| 进度 | 任务 | 状态 | 说明 |
|------|------|------|------|
| 100% | 任务 1：降低非空 Iterable.any 谓词 | 已完成 | 已解析 stdlib 调用并生成按迭代顺序聚合的 syntax OR 树。 |
| 100% | 任务 2：保持逻辑组合与取反 | 已完成 | 集合谓词已进入既有 `SqlExpr?` 逻辑组合与取反路径。 |
| 0% | 任务 3：定义空集合与无值语义 | 待处理 | 空 `any` 与现有空 `orExpr` 的省略语义不同。 |
| 0% | 任务 4：文档与回归验证 | 待处理 | 在任务 3 的语义完成后整理用户文档并扩展回归验证。 |
