# 任务 2：保持逻辑组合与取反

进度：0%
状态：待处理

## 目标

使已降低的集合 `any` 像普通条件表达式一样参与既有 `||`、`&&`、括号和 `!`。

## 当前状态

- `ConditionAnalysis` 已将 Kotlin `&&`、`||` 降低为 `andExpr`、`orExpr`，并在取反时处理德摩根变换。
- `SqlBinaryOperator` 保存 AND/OR 优先级，`StandardSqlRenderer` 会为必要的谓词操作数补括号。
- 新 `any` 的结果必须进入同一 `SqlExpr?` 管线，不能独立渲染，也不能作为运行时 Boolean 返回。

## 后续工作

- 覆盖以下 lowering：

```kotlin
where { keywords.any { keyword -> it.name.contains(keyword) } || it.status == 1 }
where { it.active == true && keywords.any { keyword -> it.name.contains(keyword) } }
where { keywords.any { keyword -> it.name.contains(keyword) || it.email.contains(keyword) } }
```

- 为 `!keywords.any { predicate }` 选择一种规范 AST 形状：聚合外层 unary NOT，或对已取反子节点作德摩根 AND 聚合；必须与既有条件取反策略一致。
- 验证动态子节点位于外层条件下时，参数分配仍稳定。
- 增加聚焦 compiler testData，断言生成的 syntax 树与渲染后的谓词结构，包含 AND/OR 混用时的括号。

## 验收

- 集合 `any` 可位于 `||`、`&&` 两侧，且不会退化为 Kotlin 运行时 Boolean。
- 内层 `||` / `&&` 与外层 `||` / `&&` 在渲染谓词中保持正确分组。
- `!keywords.any { keyword -> it.name.contains(keyword) }` 具有 SQL 等价语义且保留绑定参数。
- 既有独立 `&&`、`||`、取反 compiler 测试保持通过。

## 验证记录

- 当前静态检查：已有逻辑 lowering 与 renderer 优先级支持，但当前没有集合聚合进入该路径。
- 目标命令：`./gradlew.bat :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ConditionBoxTest --no-daemon --console=plain`。
