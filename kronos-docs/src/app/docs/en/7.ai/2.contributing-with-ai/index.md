# Contributing with AI

Kronos ORM includes a developer-facing AI skill called **kronos-dev-guide** that gives AI coding assistants deep knowledge of the project's internals — compiler plugin architecture, AST rendering pipeline, module structure, testing patterns, and CI/CD workflows.

## What is kronos-dev-guide?

`kronos-dev-guide` is located at `.claude/skills/kronos-dev-guide/` in the repository. It's designed for contributors and maintainers who want AI assistance when working on the Kronos codebase itself.

The skill helps AI assistants:

- Navigate the compiler plugin's IR transformation pipeline
- Understand the AST-based SQL generation and per-dialect rendering
- Add new database dialect support
- Add new DSL operations or subquery syntax
- Write unit tests, compiler plugin tests, and integration tests
- Understand the CI/CD pipeline, coverage, and publishing flow
- Work with any module (codegen, logging, jdbc-wrapper, gradle/maven plugins)

## Skill Structure

The skill uses a hub-and-reference pattern. `SKILL.md` is the entry point that directs the AI to the right reference file based on the task:

| Reference File | When to Read |
|----------------|-------------|
| `references/compiler-plugin.md` | Modifying the compiler plugin, KPojo augmentation, DSL lambda transformers |
| `references/ast-and-rendering.md` | Working with AST nodes, SQL rendering, adding dialect support, functions system |
| `references/orm-and-dsl.md` | ORM clause classes, DSL beans, transactions, cascade, join, plugin/hook system |
| `references/modules.md` | Codegen, logging, jdbc-wrapper, gradle/maven plugin internals |
| `references/testing-and-ci.md` | Writing tests, coverage, CI workflows, version management, publishing |
| `references/cookbook.md` | Step-by-step guides for common development tasks |

## Setup

The skill is already included in the repository under `.claude/skills/kronos-dev-guide/`. If you're using Claude Code, it will be automatically detected when you open the project.

For other AI tools, point your assistant's context to the `SKILL.md` file and the relevant reference files.

## Example Tasks

With the skill active, you can ask your AI assistant to help with tasks like:

- "Add Oracle support for the MERGE upsert syntax"
- "Write a compiler plugin test for a new condition operator"
- "How does KronosParserTransformer dispatch DSL lambdas?"
- "Add a new built-in SQL function for string concatenation"
- "Set up integration tests for PostgreSQL in kronos-testing"
- "How do I publish a new SNAPSHOT version?"

## Cookbook

The skill includes a cookbook (`references/cookbook.md`) with step-by-step guides for the most common development tasks:

- Adding a new database dialect
- Adding a new DSL operation
- Adding subquery syntax
- Adding a SQL function
- Adding an annotation
- Writing compiler plugin tests
- Writing integration tests
- Releasing a new version
- Debugging IR output
