{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 什么是 {{ $.title("kronos-orm-guide") }}

{{ $.code("kronos-orm-guide") }} 是从 Kronos 仓库 {{ $.code("release/llm") }} 分支发布的 AI 技能。它帮助 AI 编程助手理解 {{ $.title("Kronos ORM") }} API，并为你的项目生成正确的 ORM 代码。

该技能会教会 AI 助手：

- 使用合适的注解定义 {{ $.keyword("concept/code-first", ["KPojo"]) }} 数据类
- 编写 CRUD 操作：{{ $.code("select") }}、{{ $.code("insert") }}、{{ $.code("update") }}、{{ $.code("delete") }}、{{ $.code("upsert") }}
- 使用条件 DSL：{{ $.code("where") }}、{{ $.code("having") }}、{{ $.code("on") }}
- 编写联表查询、联合查询和子查询
- 使用 {{ $.keyword("database/transact", ["事务"]) }}、级联操作和 DDL 表操作
- 使用聚合、字符串、日期、数学等内置函数
- 配置命名策略、逻辑删除和乐观锁

## 一键安装

将 {{ $.code("release/llm") }} 分支克隆到 AI 工具约定的技能目录：

| 工具 | 命令 |
|------|------|
| {{ $.title("Cursor") }} | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .cursor/skills/kronos-orm-guide && rm -rf .cursor/skills/kronos-orm-guide/.git` |
| {{ $.title("默认 / 通用") }} | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .agents/skills/kronos-orm-guide && rm -rf .agents/skills/kronos-orm-guide/.git` |

Windsurf 和其他可读取 {{ $.code(".agents/skills/") }} 的工具使用默认命令。

## 技能内容

| 文件 | 内容 |
|------|------|
| {{ $.code("SKILL.md") }} | 核心指南，包含项目配置、KPojo 定义、CRUD、条件 DSL、联表、事务和 DDL |
| {{ $.code("references/advanced.md") }} | 级联操作、内置/自定义函数、原生 SQL、跨库联表和序列化 |
| {{ $.code("references/annotations.md") }} | `@Table`、`@PrimaryKey`、`@Column`、`@CreateTime` 等注解参考 |

## 示例提示词

技能激活后，可以向 AI 助手提出 Kronos 相关任务：

- "创建一个包含 id、name、email 和时间戳的 User 实体"
- "写一个 User 和 Order 联表查询，带分页"
- "添加一个事务，插入用户及其订单"
- "如何为这个实体配置逻辑删除？"
- "写一个按 email 冲突时更新的 upsert"

AI 会根据技能中的 API 知识生成符合 Kronos DSL 习惯的代码，而不是泛泛的 ORM 写法。
