{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## What is {{ $.title("kronos-dev-guide") }}

{{ $.code("kronos-dev-guide") }} is a developer-facing AI skill stored under {{ $.code(".agents/skills/kronos-dev-guide/") }} in the repository. It gives AI coding assistants project-level context for Kronos internals, including compiler plugin architecture, AST rendering, module boundaries, testing patterns, and CI/CD workflows.

The skill helps assistants:

- Navigate the compiler plugin's IR transformation pipeline
- Understand AST-based SQL generation and per-dialect rendering
- Add new database dialect support
- Add new DSL operations or subquery syntax
- Write unit tests, compiler plugin tests, and integration tests
- Understand coverage, versioning, and publishing workflows
- Work across codegen, logging, jdbc-wrapper, Gradle plugin, and Maven plugin modules

## Skill structure

The skill uses a hub-and-reference pattern. {{ $.code("SKILL.md") }} is the entry point and directs the assistant to the right reference file for the task.

| Reference file | When to read |
|----------------|--------------|
| {{ $.code("references/compiler-plugin.md") }} | Modifying the compiler plugin, KPojo augmentation, and DSL lambda transformers |
| {{ $.code("references/ast-and-rendering.md") }} | Working with AST nodes, SQL rendering, dialect support, and functions |
| {{ $.code("references/orm-and-dsl.md") }} | ORM clauses, DSL beans, transactions, cascade, joins, and plugin hooks |
| {{ $.code("references/modules.md") }} | Codegen, logging, jdbc-wrapper, Gradle plugin, and Maven plugin internals |
| {{ $.code("references/testing-and-ci.md") }} | Tests, coverage, CI workflows, version management, and publishing |
| {{ $.code("references/cookbook.md") }} | Step-by-step guides for common development tasks |

## Setup

The skill is already included in the repository. When working on Kronos itself, point your assistant's context to {{ $.code(".agents/skills/kronos-dev-guide/") }}.

For other AI tools, include {{ $.code("SKILL.md") }} and the relevant reference files in the assistant context.

## Example tasks

With the skill active, you can ask your assistant to help with repository work:

- "Add Oracle support for the MERGE upsert syntax"
- "Write a compiler plugin test for a new condition operator"
- "How does KronosParserTransformer dispatch DSL lambdas?"
- "Add a new built-in SQL function for string concatenation"
- "Set up integration tests for PostgreSQL in kronos-testing"
- "How do I publish a new SNAPSHOT version?"

## Cookbook

The cookbook covers recurring maintainer tasks:

- Adding a new database dialect
- Adding a new DSL operation
- Adding subquery syntax
- Adding a SQL function
- Adding an annotation
- Writing compiler plugin tests
- Writing integration tests
- Releasing a new version
- Debugging IR output
