# 使用 AI 编写 Kronos 代码

Kronos ORM 内置了一个 AI 技能文件 **kronos-orm-guide**，帮助 AI 编程助手理解 Kronos API 并为你的项目生成正确的 ORM 代码。

## 什么是 kronos-orm-guide？

`kronos-orm-guide` 发布在 Kronos 仓库的 `release/llm` 分支上。它教会 AI 助手：

- 定义 KPojo 数据类及注解
- 编写 CRUD 操作（select / insert / update / delete / upsert）
- 使用条件 DSL（`where`、`having`、`on`）
- 联表查询、联合查询、子查询
- 事务、级联操作、DDL 表操作
- 内置函数（聚合、字符串、日期、数学）
- 全局配置（命名策略、逻辑删除、乐观锁）

## 一键安装

根据你使用的 AI 工具，执行对应命令即可将技能安装到项目中：

| 工具 | 命令 |
|------|------|
| **Cursor / Windsurf** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .cursor/skills/kronos-orm-guide && rm -rf .cursor/skills/kronos-orm-guide/.git` |
| **Claude Code** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .claude/skills/kronos-orm-guide && rm -rf .claude/skills/kronos-orm-guide/.git` |
| **Codex** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .codex/kronos-orm-guide && rm -rf .codex/kronos-orm-guide/.git` |
| **OpenCode** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .opencode/skills/kronos-orm-guide && rm -rf .opencode/skills/kronos-orm-guide/.git` |
| **Kiro** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .kiro/skills/kronos-orm-guide && rm -rf .kiro/skills/kronos-orm-guide/.git` |
| **通用** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .llm/kronos-orm-guide && rm -rf .llm/kronos-orm-guide/.git` |

未列出的工具可以克隆 `release/llm` 分支，将技能文件添加到 AI 助手的上下文中。

## 技能内容

技能由三个文件组成：

| 文件 | 内容 |
|------|------|
| `SKILL.md` | 核心指南 — 项目配置、KPojo 定义、CRUD、条件 DSL、联表、事务、DDL |
| `references/advanced.md` | 级联操作、内置/自定义函数、原生 SQL、跨库联表、序列化 |
| `references/annotations.md` | 完整注解参考（@Table、@PrimaryKey、@Column、@CreateTime 等） |

## 示例提示词

技能激活后，你可以这样向 AI 助手提问：

- "创建一个包含 id、name、email 和时间戳的 User 实体"
- "写一个 User 和 Order 联表查询，带分页"
- "添加一个事务，插入用户及其订单"
- "如何为这个实体配置逻辑删除？"
- "写一个按 email 冲突时更新的 upsert"

AI 会根据技能中的 API 知识生成正确的 Kronos DSL 代码。
