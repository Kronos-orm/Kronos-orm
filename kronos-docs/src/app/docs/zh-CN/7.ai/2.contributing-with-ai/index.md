# 使用 AI 参与开发

Kronos ORM 包含一个面向开发者的 AI 技能 **kronos-dev-guide**，为 AI 编程助手提供项目内部的深度知识 — 编译器插件架构、AST 渲染管线、模块结构、测试模式和 CI/CD 流程。

## 什么是 kronos-dev-guide？

`kronos-dev-guide` 位于仓库的 `.claude/skills/kronos-dev-guide/` 目录下，专为贡献者和维护者设计，在开发 Kronos 代码库时提供 AI 辅助。

该技能帮助 AI 助手：

- 理解编译器插件的 IR 变换流程
- 理解基于 AST 的 SQL 生成和多方言渲染
- 添加新的数据库方言支持
- 添加新的 DSL 操作或子查询语法
- 编写单元测试、编译器插件测试和集成测试
- 理解 CI/CD 流程、覆盖率和发布流程
- 处理各模块（codegen、logging、jdbc-wrapper、gradle/maven 插件）

## 技能结构

技能采用导航中心 + 参考文件的模式。`SKILL.md` 是入口，根据任务指引 AI 阅读对应的参考文件：

| 参考文件 | 适用场景 |
|----------|---------|
| `references/compiler-plugin.md` | 修改编译器插件、KPojo 增强、DSL lambda 转换器 |
| `references/ast-and-rendering.md` | AST 节点、SQL 渲染、添加方言支持、函数系统 |
| `references/orm-and-dsl.md` | ORM 子句类、DSL beans、事务、级联、联表、插件/钩子系统 |
| `references/modules.md` | Codegen、日志、jdbc-wrapper、gradle/maven 插件内部实现 |
| `references/testing-and-ci.md` | 编写测试、覆盖率、CI 工作流、版本管理、发布 |
| `references/cookbook.md` | 常见开发任务的分步指南 |

## 配置

技能已包含在仓库的 `.claude/skills/kronos-dev-guide/` 目录中。使用 Claude Code 打开项目时会自动检测。

其他 AI 工具可将 `SKILL.md` 及相关参考文件添加到助手的上下文中。

## 示例任务

技能激活后，你可以让 AI 助手协助以下任务：

- "为 Oracle 的 MERGE 语法添加 upsert 支持"
- "为新的条件运算符编写编译器插件测试"
- "KronosParserTransformer 是如何分发 DSL lambda 的？"
- "添加一个字符串拼接的内置 SQL 函数"
- "在 kronos-testing 中为 PostgreSQL 设置集成测试"
- "如何发布一个新的 SNAPSHOT 版本？"

## 开发手册

技能包含一份开发手册（`references/cookbook.md`），提供常见开发任务的分步指南：

- 添加新的数据库方言
- 添加新的 DSL 操作
- 添加子查询语法
- 添加 SQL 函数
- 添加注解
- 编写编译器插件测试
- 编写集成测试
- 发布新版本
- 调试 IR 输出
