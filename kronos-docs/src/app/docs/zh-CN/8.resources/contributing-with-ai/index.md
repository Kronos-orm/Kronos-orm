{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 选择贡献者 skill

开发 Kronos 仓库本身时，使用 {{ $.code("kronos-dev-guide") }}。它位于 {{ $.code(".agents/skills/kronos-dev-guide/") }}，为 AI 助手提供模块、测试、构建命令、发布流程和维护工作流上下文。

只是在业务项目中使用 Kronos ORM 时，使用 {{ $.keyword("resources/using-kronos-with-ai", ["使用 AI 编写 Kronos 代码"]) }}。

贡献者 skill 适合这些任务：

- 添加数据库方言行为
- 添加或调整 DSL 操作
- 更新 codegen、logging、JDBC wrapper、Gradle 插件或 Maven 插件行为
- 编写单元测试、编译器插件测试和集成测试
- 检查 CI、版本和发布要求

## 技能结构

该技能采用导航中心加参考文件的模式。{{ $.code("SKILL.md") }} 是入口，会根据任务指引 AI 阅读对应的参考文件。

| 参考文件 | 适用场景 |
|----------|---------|
| {{ $.code("references/compiler-plugin.md") }} | 修改编译器插件、KPojo 增强和 DSL lambda 转换器 |
| {{ $.code("references/ast-and-rendering.md") }} | AST 节点、SQL 渲染、方言支持和函数系统 |
| {{ $.code("references/orm-and-dsl.md") }} | ORM 子句、DSL beans、事务、级联、联表和插件钩子 |
| {{ $.code("references/modules.md") }} | Codegen、日志、jdbc-wrapper、Gradle 插件和 Maven 插件模块边界 |
| {{ $.code("references/testing-and-ci.md") }} | 测试、覆盖率、CI 工作流、版本管理和发布 |
| {{ $.code("references/cookbook.md") }} | 常见开发任务的分步指南 |

## 准备仓库上下文

该技能已经包含在仓库中。开发 Kronos 本身时，可以将 {{ $.code(".agents/skills/kronos-dev-guide/") }} 加入 AI 助手上下文，并要求它只读取当前任务需要的参考文件。

其他 AI 工具可将 {{ $.code("SKILL.md") }} 及相关参考文件加入助手上下文。再补充任务文档、模块 README 和定义行为的测试。

```text group="Context" name="files"
.agents/skills/kronos-dev-guide/SKILL.md
.agents/skills/kronos-dev-guide/references/<needed-topic>.md
DOCS_REFACTOR_TASK_LIST/<task>.md 或其他当前任务清单
<module>/README.md
<module>/src/test/...
```

让 AI 在编辑前说明已读取哪些文件，这样跨模块修改可以绑定到明确依据。

## 示例任务

技能激活后，可以让 AI 助手协助仓库开发任务：

- "为 Oracle 的 MERGE 语法添加 upsert 支持"
- "为新的条件运算符编写编译器插件测试"
- "添加一个字符串拼接的内置 SQL 函数"
- "在 kronos-testing 中为 PostgreSQL 设置集成测试"
- "如何发布一个新的 SNAPSHOT 版本？"

## 使用 AI 添加数据库方言

数据库方言任务先从用户可见的检查清单开始，再让 dev skill 路由到实现参考。

```text group="Prompt" name="dialect"
使用 kronos-dev-guide。添加新的数据库方言支持。
先阅读“创建数据库方言”文档页和相关 dev-guide 参考文件。
编辑前列出计划修改的文件和测试。
```

该工作流的文档入口是 {{ $.keyword("database/create-database-dialect", ["创建数据库方言"]) }}。

## 验证 AI 修改

先让 AI 运行最小有效命令；触及的模块需要更宽验证时，再扩大范围。

```bash group="Validate" name="compiler plugin" icon="terminal"
./gradlew :kronos-compiler-plugin:test
```

```bash group="Validate" name="core" icon="terminal"
./gradlew :kronos-core:test
```

```bash group="Validate" name="docs" icon="terminal"
cd kronos-docs
pnpm build
```

只改文档时，不修改实现代码，并检查改动页面里的链接、route 名和宏。

## 开发手册入口

开发手册覆盖常见维护任务：

- 添加新的数据库方言
- 添加新的 DSL 操作
- 添加子查询语法
- 添加 SQL 函数
- 添加注解
- 编写编译器插件测试
- 编写集成测试
- 发布新版本
