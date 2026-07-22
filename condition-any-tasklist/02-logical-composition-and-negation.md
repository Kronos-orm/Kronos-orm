# 任务 2：保持逻辑组合与取反

进度：100%
状态：已完成（非空集合路径）

## 目标

使已降低的集合 `any` 像普通条件表达式一样参与既有 `||`、`&&`、括号和 `!`。

## 完成内容

- 集合 `any` 返回的 bridge 结果进入现有 `andExpr`、`orExpr` 和 `IrWhen` lowering 路径。
- 正向 `any` 聚合为 OR；取反路径将子谓词按既有德摩根规则重写，再聚合为 AND。
- `SqlBinaryOperator` 和 `StandardSqlRenderer` 继续负责优先级与括号。

## 已验证

- `iterableAnyLogicalComposition.kt` 覆盖以下 lowering：

```kotlin
where { keywords.any { keyword -> it.name.contains(keyword) } || it.status == 1 }
where { it.active == true && keywords.any { keyword -> it.name.contains(keyword) } }
where { keywords.any { keyword -> it.name.contains(keyword) || it.email.contains(keyword) } }
```

- 用例断言外层 `||`、外层 `&&`、内层 `||` 和 `!any` 的 `SqlExpr.Binary` 结构。
- 外层 `&&` 与集合 OR 的渲染结果精确断言为带括号的 SQL 谓词。
- `!keywords.any { keyword -> it.name.contains(keyword) }` 生成两个带 `withNot = true` 的 LIKE 子谓词和一个 AND 根节点。
- 完整 `ConditionBoxTest` 保持既有独立 `&&`、`||` 与取反用例通过。

## 验证记录

- 2026-07-22：`./gradlew.bat :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ConditionBoxTest --no-daemon --console=plain` 通过。
- 空集合取反语义由任务 3 定义与验证。
