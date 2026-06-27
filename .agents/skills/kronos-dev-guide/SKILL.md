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

## Evolution Memory Protocol

For every error, failure, or unexpected behavior, read `Evolution.md` first before attempting a fix. This file is the highest-priority troubleshooting memory for this skill and must be checked before consulting other reference files, searching the codebase, or changing files.

Priority order for error handling:

1. Read `Evolution.md` and look for a matching symptom, module, command, or error message.
2. Reuse any documented solution or prevention rule that applies.
3. If no matching record exists, continue to the relevant reference file below.
4. After a successful build or verified fix, append important new problems and solutions to `Evolution.md` using the required format.

Use this protocol to preserve discovered project pitfalls and avoid repeating the same mistakes.

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
| `kronos-gradle-plugin` | Gradle build integration | `KronosGradlePlugin.kt` |
| `kronos-maven-plugin` | Maven build integration | `KronosMavenPlugin.kt` |
| `kronos-logging` | Pluggable logging (SLF4J/JUL/Commons/Android) | `KronosLoggerApp.kt` |
| `kronos-jdbc-wrapper` | Default JDBC DataSource wrapper | `KronosBasicWrapper.kt` |
| `kronos-codegen` | DB schema → KPojo Kotlin files | `ConfigReader.kt`, `TemplateConfig.kt` |
| `kronos-testing` | Integration tests (real DBs) | MySQL/Postgres/SQLite/Oracle/MSSQL tests |
| `kronos-docs` | Documentation website (Angular/ng-doc) | `pnpm install && ng build` |
| `build-logic` | Convention plugins (publishing, dokka) | `publishing.gradle.kts`, `dokka-convention.gradle.kts` |

## Testing Frameworks At A Glance

Kronos currently uses three kinds of tests. Pick the narrowest one that proves the behavior:

| Test kind | Location | Use for | Do not use for |
|-----------|----------|---------|----------------|
| Ordinary unit tests | `*/src/test/kotlin` | Pure functions, AST rendering, utility classes, core runtime behavior that does not need a fresh compiler pipeline | Compiler-plugin generated declarations or IR rewrites |
| Official compiler tests | `kronos-compiler-plugin/testData` plus runners in `src/test/kotlin/com/kotlinorm/compiler/official` | KPojo generated bodies, DSL lambda transformations, FIR diagnostics, IR verifier regressions, compiler-plugin behavior through Kotlin's real FIR/IR pipeline | Broad smoke tests, pure utilities, external database integration |
| kctfork / kotlin-compile-testing legacy tests | `kronos-compiler-plugin/src/test/kotlin` helpers such as `KotlinSourceDynamicCompiler` and `IrTestFramework` | Existing coverage that has not yet been mapped to official compiler tests; focused dynamic compilation while migration is incomplete | New replacement coverage when an official compiler test can prove the same contract |

Important migration rule: official compiler tests are the preferred direction for compiler-plugin behavior, but kctfork is not removable until coverage is mapped. At the time this guide was updated, excluding kctfork reduced coverage by about 10.55 percentage points, so kctfork still protects real behavior.

When adding or migrating compiler-plugin tests:

1. Read `kronos-compiler-plugin/src/test/kotlin/com/kotlinorm/compiler/official/TEST_DATA_STYLE_GUIDE.md`.
2. If the behavior depends on generated KPojo members or DSL IR rewrites, add a `testData/box/.../*.kt` file and a thin official runner method.
3. If invalid source should fail, use `testData/diagnostics`, not a box test.
4. If the test replaces a kctfork case, record the mapping as `covered`, `covered by stronger official test`, `intentionally kept as unit test`, `obsolete`, or `missing`.
5. Do not delete old kctfork tests just because a broad official smoke test exists; the official test must assert the same compiler-plugin contract and important values.

## Reference Files

Read these for deep implementation details:

| File | When to Read |
|------|-------------|
| `Evolution.md` | Required first read before fixing any error; stores historical problems, root causes, solutions, and prevention rules |
| `references/compiler-plugin.md` | Modifying compiler plugin, adding/changing IR transforms, debugging IR output. Before compiler plugin architecture, FIR/frontend, IR/backend, diagnostics, or compiler-test work, use `.agents/skills/kronos-dev-kcp` first. |
| `references/ast-and-rendering.md` | Working with AST nodes, SQL rendering, adding/modifying database dialects, functions system |
| `references/orm-and-dsl.md` | ORM clause classes, DSL beans, plugin/hook system, task execution, transactions, value transformers |
| `references/modules.md` | Working on codegen, logging, jdbc-wrapper, gradle/maven plugins |
| `references/testing-and-ci.md` | Writing tests, coverage, CI workflows, version management, publishing, documentation |
| `references/cookbook.md` | Step-by-step: add new DB dialect, add new DSL operation, add subquery syntax, write tests |
