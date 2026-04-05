---
name: kronos-dev-guide
description: >
  Kronos-ORM internal developer guide for contributors and maintainers.
  Use this skill whenever working on the Kronos-ORM repository internals:
  adding features, fixing bugs, adding database dialect support, writing tests,
  understanding the compiler plugin or AST architecture, modifying DSL operations,
  working with transactions, or navigating the CI/CD pipeline.
  Also trigger when someone asks about module structure, how the compiler plugin
  transforms KPojo classes, how SQL is generated from the AST, how to add
  a new database backend, how to write tests, how coverage works, how to publish
  releases, or how any specific module (codegen, logging, jdbc-wrapper, gradle/maven
  plugin) works internally.
---

# Kronos-ORM Developer Guide

Kotlin compiler-plugin-powered ORM. Zero reflection, AST-based SQL generation, multi-database.

## Architecture at a Glance

```
User DSL code (select/insert/update/delete/upsert/join/union)
  ↓ compile time
Compiler Plugin (kronos-compiler-plugin)
  → KPojo class augmentation (toDataMap, fromMapData, kronosColumns, get/set, strategies)
  → DSL lambda transformation (KTableForCondition/Select/Set/Sort/Reference → Criteria/Field IR)
  ↓ runtime
ORM Clause classes (SelectClause, InsertClause, etc.)
  → Build AST nodes (SelectStatement, InsertStatement, etc.)
  → SqlManager routes to dialect-specific SqlRenderer
  → SqlRenderer renders AST → SQL string + named parameters
  → NamedParameterUtils converts :name → ? positional params
  → KronosDataSourceWrapper executes via JDBC
```

## Module Map

| Module | Role | Key Entry Points |
|--------|------|-----------------|
| `kronos-core` | Contracts, AST, SQL rendering, ORM ops, strategies, DSL beans | `Kronos.kt`, `ast/`, `database/`, `orm/`, `beans/dsl/` |
| `kronos-compiler-plugin` | K2 IR transformations | `KronosParserTransformer`, `KronosIrClassTransformer` |
| `kronos-compiler-plugin-legacy` | Pre-K2 plugin (maintenance only) | Same transform goals, older structure |
| `kronos-gradle-plugin` | Gradle build integration | `KronosGradlePlugin.kt` |
| `kronos-maven-plugin` | Maven build integration | `KronosMavenPlugin.kt` |
| `kronos-logging` | Pluggable logging (SLF4J/JUL/Commons/Android) | `KronosLoggerApp.kt` |
| `kronos-jdbc-wrapper` | Default JDBC DataSource wrapper | `KronosBasicWrapper.kt` |
| `kronos-codegen` | DB schema → KPojo Kotlin files | `ConfigReader.kt`, `TemplateConfig.kt` |
| `kronos-testing` | Integration tests (real DBs) | MySQL/Postgres/SQLite/Oracle/MSSQL tests |
| `kronos-docs` | Documentation website (Angular/ng-doc) | `pnpm install && ng build` |
| `build-logic` | Convention plugins (publishing, dokka) | `publishing.gradle.kts`, `dokka-convention.gradle.kts` |

## Reference Files

Read these for deep implementation details:

| File | When to Read |
|------|-------------|
| `references/compiler-plugin.md` | Modifying compiler plugin, adding/changing IR transforms, debugging IR output |
| `references/ast-and-rendering.md` | Working with AST nodes, SQL rendering, adding/modifying database dialects, functions system |
| `references/orm-and-dsl.md` | ORM clause classes, DSL beans, plugin/hook system, task execution, transactions, value transformers |
| `references/modules.md` | Working on codegen, logging, jdbc-wrapper, gradle/maven plugins |
| `references/testing-and-ci.md` | Writing tests, coverage, CI workflows, version management, publishing, documentation |
| `references/cookbook.md` | Step-by-step: add new DB dialect, add new DSL operation, add subquery syntax, write tests |
