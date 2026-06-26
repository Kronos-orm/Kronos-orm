# Using Kronos with AI

Kronos ORM provides a built-in AI skill called **kronos-orm-guide** that helps AI coding assistants understand the Kronos API and generate correct ORM code for your project.

## What is kronos-orm-guide?

`kronos-orm-guide` is a skill file published on the `release/llm` branch of the Kronos repository. It teaches AI assistants how to:

- Define KPojo data classes with proper annotations
- Write CRUD operations (select / insert / update / delete / upsert)
- Use the condition DSL (`where`, `having`, `on`)
- Perform join queries, union queries, and subqueries
- Work with transactions, cascade operations, and DDL
- Use built-in functions (aggregate, string, date, math)
- Configure global settings (naming strategies, logical deletion, optimistic locking)

## Install with One Command

Install the skill into your project with a single command based on your AI tool:

| Tool | Command |
|------|---------|
| **Cursor / Windsurf** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .cursor/skills/kronos-orm-guide && rm -rf .cursor/skills/kronos-orm-guide/.git` |
| **Claude Code** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .claude/skills/kronos-orm-guide && rm -rf .claude/skills/kronos-orm-guide/.git` |
| **Codex** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .codex/kronos-orm-guide && rm -rf .codex/kronos-orm-guide/.git` |
| **OpenCode** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .opencode/skills/kronos-orm-guide && rm -rf .opencode/skills/kronos-orm-guide/.git` |
| **Kiro** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .kiro/skills/kronos-orm-guide && rm -rf .kiro/skills/kronos-orm-guide/.git` |
| **Generic** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .llm/kronos-orm-guide && rm -rf .llm/kronos-orm-guide/.git` |

For tools not listed above, clone the `release/llm` branch and point your AI assistant's context to the skill files.

## What the Skill Covers

The skill is organized into three files:

| File | Content |
|------|---------|
| `SKILL.md` | Core guide — project setup, KPojo definition, CRUD, condition DSL, join, transactions, DDL |
| `references/advanced.md` | Cascade operations, built-in/custom functions, raw SQL, cross-DB joins, serialization |
| `references/annotations.md` | Complete annotation reference (@Table, @PrimaryKey, @Column, @CreateTime, etc.) |

## Example Prompts

Once the skill is active, you can ask your AI assistant things like:

- "Create a User entity with id, name, email, and timestamps"
- "Write a query that joins User and Order tables with pagination"
- "Add a transaction that inserts a user and their orders"
- "How do I configure logical deletion for this entity?"
- "Write an upsert that updates on conflict by email"

The AI will generate correct Kronos DSL code based on the skill's knowledge of the API.
