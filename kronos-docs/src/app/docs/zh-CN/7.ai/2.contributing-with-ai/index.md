{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 什么是 {{ $.title("kronos-dev-guide") }}

{{ $.code("kronos-dev-guide") }} 是仓库内面向开发者的 AI 技能，位于 {{ $.code(".agents/skills/kronos-dev-guide/") }}。它为 AI 编程助手提供 Kronos 内部实现上下文，包括编译器插件架构、AST 渲染、模块边界、测试模式和 CI/CD 流程。

该技能帮助 AI 助手：

- 理解编译器插件的 IR 变换流程
- 理解基于 AST 的 SQL 生成和多方言渲染
- 添加新的数据库方言支持
- 添加新的 DSL 操作或子查询语法
- 编写单元测试、编译器插件测试和集成测试
- 理解覆盖率、版本管理和发布流程
- 处理 codegen、logging、jdbc-wrapper、Gradle 插件和 Maven 插件模块

## 技能结构

该技能采用导航中心加参考文件的模式。{{ $.code("SKILL.md") }} 是入口，会根据任务指引 AI 阅读对应的参考文件。

| 参考文件 | 适用场景 |
|----------|---------|
| {{ $.code("references/compiler-plugin.md") }} | 修改编译器插件、KPojo 增强和 DSL lambda 转换器 |
| {{ $.code("references/ast-and-rendering.md") }} | AST 节点、SQL 渲染、方言支持和函数系统 |
| {{ $.code("references/orm-and-dsl.md") }} | ORM 子句、DSL beans、事务、级联、联表和插件钩子 |
| {{ $.code("references/modules.md") }} | Codegen、日志、jdbc-wrapper、Gradle 插件和 Maven 插件内部实现 |
| {{ $.code("references/testing-and-ci.md") }} | 测试、覆盖率、CI 工作流、版本管理和发布 |
| {{ $.code("references/cookbook.md") }} | 常见开发任务的分步指南 |

## 配置

该技能已经包含在仓库中。开发 Kronos 本身时，可以将 {{ $.code(".agents/skills/kronos-dev-guide/") }} 加入 AI 助手上下文。

其他 AI 工具可将 {{ $.code("SKILL.md") }} 及相关参考文件加入助手上下文。

## 示例任务

技能激活后，可以让 AI 助手协助仓库开发任务：

- "为 Oracle 的 MERGE 语法添加 upsert 支持"
- "为新的条件运算符编写编译器插件测试"
- "KronosParserTransformer 是如何分发 DSL lambda 的？"
- "添加一个字符串拼接的内置 SQL 函数"
- "在 kronos-testing 中为 PostgreSQL 设置集成测试"
- "如何发布一个新的 SNAPSHOT 版本？"

## 开发手册

开发手册覆盖常见维护任务：

- 添加新的数据库方言
- 添加新的 DSL 操作
- 添加子查询语法
- 添加 SQL 函数
- 添加注解
- 编写编译器插件测试
- 编写集成测试
- 发布新版本
- 调试 IR 输出
