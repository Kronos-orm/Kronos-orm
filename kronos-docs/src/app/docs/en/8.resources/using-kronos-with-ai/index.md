{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## What is {{ $.title("kronos-orm-guide") }}

{{ $.code("kronos-orm-guide") }} is an AI skill published from the Kronos repository's {{ $.code("main") }} branch. It helps AI coding assistants understand {{ $.title("Kronos ORM") }} APIs and generate correct ORM code for your project.

The skill teaches assistants how to:

- Define {{ $.keyword("mapping/code-first", ["KPojo"]) }} data classes with proper annotations
- Write CRUD operations: {{ $.code("select") }}, {{ $.code("insert") }}, {{ $.code("update") }}, {{ $.code("delete") }}, and {{ $.code("upsert") }}
- Use the condition DSL: {{ $.code("where") }}, {{ $.code("having") }}, and {{ $.code("on") }}
- Perform join queries, union queries, and subqueries
- Work with {{ $.keyword("database/transactions", ["transactions"]) }}, cascade operations, and DDL
- Use built-in functions for aggregate, string, date, and math expressions
- Configure naming strategies, logical deletion, and optimistic locking

## Install with one command

Install the skill from the {{ $.code("main") }} branch into the location expected by your AI tool:

| Tool | Command |
|------|---------|
| {{ $.title("Claude") }} | `npx degit Kronos-orm/Kronos-orm/.agents/skills/kronos-orm-guide#main .claude/skills/kronos-orm-guide` |
| {{ $.title("Codex") }} | `npx degit Kronos-orm/Kronos-orm/.agents/skills/kronos-orm-guide#main .agents/skills/kronos-orm-guide` |
| {{ $.title("Cursor") }} | `npx degit Kronos-orm/Kronos-orm/.agents/skills/kronos-orm-guide#main .cursor/skills/kronos-orm-guide` |
| {{ $.title("Default / Generic") }} | `npx degit Kronos-orm/Kronos-orm/.agents/skills/kronos-orm-guide#main .agents/skills/kronos-orm-guide` |

If the target directory already exists, add {{ $.code("--force") }} after {{ $.code("degit") }}.

Claude Code loads project skills from {{ $.code(".claude/skills/") }}. Codex loads project skills from {{ $.code(".agents/skills/") }}. Use the default command for Windsurf and other tools that can read agent skill files from {{ $.code(".agents/skills/") }}.

## Prepare project context

Ask the assistant to read the skill and the project files that decide the Kronos setup before it writes code.

```text group="Context" name="files"
<skill-dir>/kronos-orm-guide/SKILL.md
<skill-dir>/kronos-orm-guide/references/advanced.md
<skill-dir>/kronos-orm-guide/references/annotations.md
build.gradle.kts or pom.xml
src/main/kotlin/... existing KPojo classes
```

Use {{ $.code(".claude/skills") }} as {{ $.code("<skill-dir>") }} for Claude, {{ $.code(".agents/skills") }} for Codex and generic agents, and {{ $.code(".cursor/skills") }} for Cursor.

For docs-backed tasks, add the relevant page link instead of pasting large examples. These pages cover the most common generation targets:

| Task | Docs page |
|------|-----------|
| Project setup | {{ $.keyword("configuration/compiler-plugins", ["Compiler Plugins"]) }} |
| Entity mapping | {{ $.keyword("mapping/code-first", ["KPojo"]) }} and {{ $.keyword("mapping/annotations", ["Annotation Config"]) }} |
| Conditions and projection | {{ $.keyword("query/conditions", ["Conditions"]) }} and {{ $.keyword("query/projection", ["Projection"]) }} |
| Subqueries and INSERT SELECT | {{ $.keyword("query/subqueries", ["Subqueries"]) }} |
| Database execution | {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }} |

## Ask for current API output

Use prompts that name the build tool, database, and expected Kronos API. This keeps the answer on the current compiler-plugin and DSL path.

```text group="Prompt 1" name="project setup"
Use the Kronos ORM skill. Add Kronos to this JVM Kotlin Gradle project.
Use Kotlin 2.4.0, com.kotlinorm.kronos-gradle-plugin, kronos-core,
and kronos-jdbc-wrapper. Configure Kronos.dataSource with KronosJdbcWrapper.
Use direct property assignment on the Kronos object for global settings.
```

The expected dependency shape is:

```kotlin group="Prompt 2" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
}
```

For ORM code, ask for one entity and one runnable query at a time.

```text group="Prompt 3" name="query"
Using Kronos ORM, create a User KPojo for table tb_user with id, name, email,
deleted, and version fields. Then write a query that selects id and name,
filters active users by email domain, and prints the generated SQL and paramMap
with build().
```

The answer should use the current DSL shape.

```kotlin group="Prompt 4" name="expected kotlin" icon="kotlin"
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var email: String? = null,
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = false,
    @Version
    var version: Int? = null
) : KPojo

val task = User()
    .select { [it.id, it.name] }
    .where { it.email like "%@example.com" }
    .build()

println(task.sql)
println(task.paramMap)
```

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

## Validate the answer

After the assistant edits code, ask it to run the narrow build check and inspect SQL before executing database writes.

```bash group="Validate 1" name="shell" icon="terminal"
./gradlew compileKotlin
```

```text group="Validate 1" name="output"
[Kronos] Kronos compiler plugin K2 initialized
BUILD SUCCESSFUL
```

For queries and mutations, request a `build()` example first so the generated SQL and parameters are visible.

```kotlin group="Validate 2" name="sql" icon="kotlin"
val task = User(id = 7)
    .delete()
    .where()
    .build()

println(task.sql)
println(task.paramMap)
```

For repository changes to Kronos itself, use {{ $.keyword("resources/contributing-with-ai", ["Contributing with AI"]) }} instead of the user-facing ORM skill.
