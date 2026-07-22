# 任务 3：定义空集合与无值语义

进度：0%
状态：待处理

## 目标

实现并证明不能直接继承现有 `orExpr` 行为的语义边界。

## 当前状态

- `KTableForCondition.orExpr` 会过滤 `null` 子节点；当没有子节点时返回 `null`。
- 对普通查询条件来说，`null` syntax 表达式表示省略这个条件。
- Kotlin `emptyList<T>().any { ... }` 为 false；如果直接调用 `orExpr(emptyList())`，会错误放宽查询。
- 在现有无值策略下，单个 `contains` 值可能生成无值表达式，尤其在 query 场景中 null 会被忽略。

## 后续工作

- 增加明确的空集合路径，生成 `SqlExpr.BooleanLiteral(false)`，但不修改 `orExpr` 本身。
- 让空集合取反通过选定的取反表示生成 true。
- 测试单元素、重复元素和空集合，确认每个动态关键字都独立绑定。
- 明确并记录“集合非空但所有子谓词因既有无值规则变成 `null`”的行为。推荐的兼容策略是维持既有子谓词省略语义，但完成前必须用聚焦测试确认。
- 测试非平凡集合 receiver 只会求值一次，且按原始迭代顺序消费。
- 仅当引入新的内部聚合 helper 时，才在 `KTableForConditionBehaviorTest` 增加 unit coverage；编译器行为继续放在官方 compiler testData。

## 验收

- 空 `any` 产生 `SqlExpr.BooleanLiteral(false)`，不能令 `where` 条件被省略。
- 空 `any` 取反后为 true。
- 既有 `orExpr(emptyList()) == null` 行为保持不变且有覆盖。
- 已选择的“所有子节点无值”行为有明确测试和条件 DSL 文档说明。
- receiver 求值次数与参数顺序经过验证，而不是仅靠推测。

## 验证记录

- 当前静态检查：`logicalExpr` 对空列表返回 `null`，没有集合 `any` 专用分支。
- 目标命令：

  - `./gradlew.bat :kronos-core:test --tests com.kotlinorm.beans.dsl.KTableForConditionBehaviorTest --no-daemon --console=plain`
  - `./gradlew.bat :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ConditionBoxTest --no-daemon --console=plain`
