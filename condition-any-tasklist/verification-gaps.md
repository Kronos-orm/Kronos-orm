# 当前验证缺口

更新日期：2026-07-22

- 尚无官方 compiler test 证明 `Iterable.any(predicate)` lowering，因为实现尚不存在。任务 1 负责关闭基础路径缺口。
- 尚无测试定义空集合 `any` 的 AST/SQL 行为；任务 3 必须证明它为 false 及其取反结果。
- “集合非空但每个子表达式都被既有无值规则省略”的行为尚未确认；任务 3 必须记录选择的兼容策略。
- Kotlin stdlib、`KTableForCondition.any(query)` 与 PostgreSQL `f.any(...)` 的 callable identity 尚未被测试；任务 1 必须补充覆盖。
- 尚无面向用户的文档；任务 4 在有实现证据后更新中英文文档与 ORM guide。

## 必跑验证

- `./gradlew.bat :kronos-compiler-plugin:test --tests com.kotlinorm.compiler.ConditionBoxTest --no-daemon --console=plain`
- `./gradlew.bat :kronos-core:test --tests com.kotlinorm.beans.dsl.KTableForConditionBehaviorTest --no-daemon --console=plain`
- `./gradlew.bat :kronos-core:test :kronos-compiler-plugin:test --no-daemon --console=plain`
- 用户文档修改后，在 `kronos-docs` 中执行 `pnpm build`。
