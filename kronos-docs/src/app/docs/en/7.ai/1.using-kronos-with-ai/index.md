{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## What is {{ $.title("kronos-orm-guide") }}

{{ $.code("kronos-orm-guide") }} is an AI skill published from the Kronos repository's {{ $.code("release/llm") }} branch. It helps AI coding assistants understand {{ $.title("Kronos ORM") }} APIs and generate correct ORM code for your project.

The skill teaches assistants how to:

- Define {{ $.keyword("concept/code-first", ["KPojo"]) }} data classes with proper annotations
- Write CRUD operations: {{ $.code("select") }}, {{ $.code("insert") }}, {{ $.code("update") }}, {{ $.code("delete") }}, and {{ $.code("upsert") }}
- Use the condition DSL: {{ $.code("where") }}, {{ $.code("having") }}, and {{ $.code("on") }}
- Perform join queries, union queries, and subqueries
- Work with {{ $.keyword("database/transact", ["transactions"]) }}, cascade operations, and DDL
- Use built-in functions for aggregate, string, date, and math expressions
- Configure naming strategies, logical deletion, and optimistic locking

## Install with one command

Clone the {{ $.code("release/llm") }} branch into the location expected by your AI tool:

| Tool | Command |
|------|---------|
| {{ $.title("Cursor") }} | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .cursor/skills/kronos-orm-guide && rm -rf .cursor/skills/kronos-orm-guide/.git` |
| {{ $.title("Default / Generic") }} | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .agents/skills/kronos-orm-guide && rm -rf .agents/skills/kronos-orm-guide/.git` |

Use the default command for Windsurf and other tools that can read agent skill files from {{ $.code(".agents/skills/") }}.

## Skill contents

| File | Content |
|------|---------|
| {{ $.code("SKILL.md") }} | Core guide for project setup, KPojo definitions, CRUD, condition DSL, joins, transactions, and DDL |
| {{ $.code("references/advanced.md") }} | Cascade operations, built-in and custom functions, raw SQL, cross-database joins, and serialization |
| {{ $.code("references/annotations.md") }} | Complete annotation reference for `@Table`, `@PrimaryKey`, `@Column`, `@CreateTime`, and related annotations |

## Example prompts

Once the skill is active, ask your AI assistant for Kronos-specific work:

- "Create a User entity with id, name, email, and timestamps"
- "Write a query that joins User and Order tables with pagination"
- "Add a transaction that inserts a user and their orders"
- "How do I configure logical deletion for this entity?"
- "Write an upsert that updates on conflict by email"

The assistant can then produce code that follows Kronos DSL conventions instead of generic ORM patterns.
