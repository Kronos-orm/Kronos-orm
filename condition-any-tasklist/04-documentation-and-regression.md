# 任务 4：文档与回归验证

进度：0%
状态：待处理

## 目标

在实现完成后发布经过验证的 Kotlin 原生写法，并留下可复现的验证记录。

## 当前状态

- 中英文查询条件页已记录普通 `contains`、`&&`、`||`、取反、动态条件和原生 SQL，但没有集合 `any` 谓词。
- 面向用户的 ORM guide 也没有集合 `any` 条件示例。
- 这是编译器插件行为，除 core unit test 外还需要 `testData/box/condition` 的官方测试。

## 后续工作

- 更新 `kronos-docs/src/app/docs/en/3.query/conditions/index.md` 与 `kronos-docs/src/app/docs/zh-CN/3.query/conditions/index.md`，增加简洁且技术等价的示例。
- 展示 `any` 谓词与普通 `||` 或 `&&` 的组合，并说明空集合行为；不得把它写成 SQL 量化 `ANY`。
- 如修改公开文档，同步 `.agents/skills/kronos-orm-guide/SKILL.md` 中对应的条件 DSL 部分。
- 新增或更新官方 compiler box tests 及 JUnit runner。正向断言应比较完整的 syntax 结构或渲染输出，不能只做子串断言。
- 先运行有针对性的 compiler/core 测试，再运行相关模块测试。文档修改后，在 `kronos-docs` 中运行 `pnpm build`。
- 将命令、结果和剩余未验证范围记录到本任务清单的验证文件。

## 验收

- 中英文文档描述相同的公开写法、支持范围和空集合行为。
- 文档只使用 `collection.any { element -> ... }` 与普通 Kotlin 布尔运算符，不推荐 raw SQL。
- 所有新增 compiler 测试均通过官方 Kotlin FIR/IR 管线。
- 相关 core、compiler-plugin、文档验证结果均记录了确切范围。
- ORM guide 与面向用户的条件文档保持一致。

## 验证记录

- 当前静态检查：文档和 ORM guide 尚未包含该特性。
- 目标命令：

  - `./gradlew.bat :kronos-core:test :kronos-compiler-plugin:test --no-daemon --console=plain`
  - 文档修改后，在 `kronos-docs` 中执行 `pnpm build`。
