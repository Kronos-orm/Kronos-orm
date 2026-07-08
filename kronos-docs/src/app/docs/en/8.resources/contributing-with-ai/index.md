{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Choose the contributor skill

Use {{ $.code("kronos-dev-guide") }} when the AI assistant is changing the Kronos repository itself. It is stored under {{ $.code(".agents/skills/kronos-dev-guide/") }} and gives the assistant repository context for modules, tests, build commands, release flow, and maintainer workflows.

Use {{ $.keyword("resources/using-kronos-with-ai", ["Using Kronos with AI"]) }} for application code that only consumes Kronos ORM.

The contributor skill is useful for tasks such as:

- Adding database dialect behavior
- Adding or adjusting DSL operations
- Updating codegen, logging, JDBC wrapper, Gradle plugin, or Maven plugin behavior
- Writing unit tests, compiler plugin tests, and integration tests
- Checking CI, versioning, and publishing requirements

## Skill structure

The skill uses a hub-and-reference pattern. {{ $.code("SKILL.md") }} is the entry point and directs the assistant to the right reference file for the task.

| Reference file | When to read |
|----------------|--------------|
| {{ $.code("references/compiler-plugin.md") }} | Modifying the compiler plugin, KPojo augmentation, and DSL lambda transformers |
| {{ $.code("references/ast-and-rendering.md") }} | Working with AST nodes, SQL rendering, dialect support, and functions |
| {{ $.code("references/orm-and-dsl.md") }} | ORM clauses, DSL beans, transactions, cascade, joins, and plugin hooks |
| {{ $.code("references/modules.md") }} | Codegen, logging, jdbc-wrapper, Gradle plugin, and Maven plugin module boundaries |
| {{ $.code("references/testing-and-ci.md") }} | Tests, coverage, CI workflows, version management, and publishing |
| {{ $.code("references/cookbook.md") }} | Step-by-step guides for common development tasks |

## Prepare repository context

The skill is already included in the repository. When working on Kronos itself, point your assistant's context to {{ $.code(".agents/skills/kronos-dev-guide/") }} and ask it to read only the reference files needed for the task.

For other AI tools, include {{ $.code("SKILL.md") }} and the relevant reference files in the assistant context. Add the task document, module README, and tests that define the behavior.

```text group="Context" name="files"
.agents/skills/kronos-dev-guide/SKILL.md
.agents/skills/kronos-dev-guide/references/<needed-topic>.md
DOCS_REFACTOR_TASK_LIST/<task>.md or another active task list
<module>/README.md
<module>/src/test/...
```

Ask the assistant to report which files it read before editing. This keeps broad repository work tied to a visible source of truth.

## Example tasks

With the skill active, you can ask your assistant to help with repository work:

- "Add Oracle support for the MERGE upsert syntax"
- "Write a compiler plugin test for a new condition operator"
- "Add a new built-in SQL function for string concatenation"
- "Set up integration tests for PostgreSQL in kronos-testing"
- "How do I publish a new SNAPSHOT version?"

## Add a database dialect with AI

For a database dialect task, start from the user-facing checklist and then let the dev skill route to the implementation references.

```text group="Prompt" name="dialect"
Use kronos-dev-guide. Add support for a new database dialect.
First read the Create Database Dialect docs page and the relevant dev-guide reference.
Then list the files and tests you plan to touch before editing.
```

The docs entry for that workflow is {{ $.keyword("database/create-database-dialect", ["Create Database Dialect"]) }}.

## Validate AI changes

Ask the assistant to run the smallest meaningful command first, then broaden only when the touched module needs it.

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

For docs-only changes, keep implementation code untouched and verify links, route names, and macros in the changed pages.

## Cookbook entry points

The cookbook covers recurring maintainer tasks:

- Adding a new database dialect
- Adding a new DSL operation
- Adding subquery syntax
- Adding a SQL function
- Adding an annotation
- Writing compiler plugin tests
- Writing integration tests
- Releasing a new version
