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

For every error, failure, or unexpected behavior, consult `Evolution.index.md` first before attempting a fix. The index is the highest-priority troubleshooting memory and keeps the context cost low. Do not browse the full evolution history by default; open only the specific `evolution/*.md` file linked by the index.

Priority order for error handling:

1. Read `Evolution.index.md` and look for a matching symptom, module, command, or error message.
2. If the index matches, read only the linked `evolution/*.md` entry file.
3. Reuse any documented solution or prevention rule that applies.
4. If no matching index record exists, continue to the relevant reference file below or search code/tests normally without opening the full evolution log.
5. After a successful build or verified fix, create one reusable, confirmed pitfall file under `evolution/`, then add one concise row to `Evolution.index.md`.
6. Periodically remove, archive, or rewrite obsolete evolution records when the guidance is no longer true.

Use this protocol to preserve discovered project pitfalls and avoid repeating the same mistakes.

## Architecture at a Glance

```
User DSL code (select/insert/update/delete/upsert/join/union)
  ↓ compile time
Compiler Plugin (kronos-compiler-plugin)
  → KPojo class augmentation (toDataMap, fromMapData, __columns, get/set, strategies)
  → DSL lambda transformation (KTableForCondition/Select/Set/Sort/Reference → Criteria/Field IR)
  ↓ runtime
ORM Clause classes (SelectClause, InsertClause, etc.)
  → Build AST nodes (SelectStatement, InsertStatement, etc.)
  → SqlManager routes to dialect-specific SqlRenderer
  → SqlRenderer renders AST → SQL string + named parameters
  → NamedParameterUtils converts :name → ? positional params
  → KronosDataSourceWrapper executes through JDBC or platform-specific wrappers
```

## Module Map

| Module | Role | Key Entry Points |
|--------|------|-----------------|
| `kronos-core` | Shared ORM runtime, models, execution contracts, strategies and codecs | `src/`, `Kronos.kt`, `database/`, `orm/`, `wrappers/KronosExecutionWrapper.kt` |
| `kronos-syntax` | Multiplatform SQL AST and dialect renderers | `src/`, `statement/`, `render/` |
| `kronos-compiler-plugin` | K2 FIR diagnostics and projections, IR transformations | `KronosFirExtensionRegistrar`, `KronosParserTransformer`, `KronosIrClassTransformer` |
| `kronos-gradle-plugin` | Gradle compiler integration, JVM provider resources and Native metadata manifests | `KronosGradlePlugin.kt`, `GenerateKronosGeneratedTypeService.kt` |
| `kronos-maven-plugin` | Maven build integration | `KronosMavenPlugin.kt` |
| `kronos-logging` | Pluggable logging (SLF4J/JUL/Commons/Android) | `KronosLoggerApp.kt` |
| `kronos-jdbc-wrapper` | Default JDBC DataSource wrapper | `KronosJdbcWrapper.kt` |
| `kronos-codegen` | DB schema → KPojo Kotlin files | `ConfigReader.kt`, `TemplateConfig.kt` |
| `kronos-testing` | Integration tests (real and in-process DBs) | MySQL/Postgres/SQLite/H2/Oracle/MSSQL/DM8 tests |
| `kronos-testing/ide` | Real IDEA and KEFS integration tests, compiler compatibility artifact staging | `KefsProjectionFixtureTest`, `scripts/ide.main.kts` |
| `kronos-docs` | Documentation website (Angular/ng-doc) | `pnpm install && ng build` |
| `templates` | Shared Kotlin Toolchain configuration | `common.module-template.yaml`, `kronos-runtime.module-template.yaml` |
| `plugins/kronos-native` | Local Toolchain plugin for Native metadata and publication | `module.yaml`, `plugin.yaml`, `src/`, `test/` |
| `scripts/quality.main.kts` | JOIN generation, coverage and API documentation | Official Kover and Dokka CLIs |

## Multiplatform Source Sets

`kronos-core/src/` contains models, annotations, ORM operations, table statements, generated type registries, codecs and the execution wrapper. `src@jvm/` contains Java/JDBC temporal conversion, platform implementations and JVM compatibility code. Native implementations live in `src@native/`, `src@apple/`, `src@linux/` and `src@mingwX64/`. Shared tests live in `test/`; platform tests use `test@jvm/` and `test@native/`. JVM-only modules retain the Maven-like `src/main/kotlin` and `src/test/kotlin` layout. Production core sources do not import `kotlin.reflect.full` or `kotlin.reflect.jvm`; JVM reflection helpers are test dependencies.

Use `initializeKronosMetadata()` from the final application/framework module. The compiler rewrites Native calls to the application aggregate; the Gradle plugin exports dependency manifests and source-visible provider declarations. Do not call hash-named aggregate internals or freeze a partially populated graph. Platform driver adapters implement `KronosExecutionDataSource`; `KronosExecutionWrapper` bridges the contract to shared ORM tasks. SQLite driver implementations belong to the reference applications.

## Testing Frameworks At A Glance

Pick the narrowest test infrastructure that proves the behavior:

| Test kind | Location | Use for | Do not use for |
|-----------|----------|---------|----------------|
| Ordinary unit tests | MPP `test/` and `test@platform/`; JVM-only `src/test/kotlin` | Pure functions, AST rendering, utility classes, core runtime behavior that does not need a fresh compiler pipeline | Compiler-plugin generated declarations or IR rewrites |
| Official compiler tests | `kronos-compiler-plugin/testData` plus runners in `src/test/kotlin/com/kotlinorm/compiler` | KPojo generated bodies, DSL lambda transformations, FIR diagnostics, IR verifier regressions, compiler-plugin behavior through Kotlin's real FIR/IR pipeline | Broad smoke tests, pure utilities, external database integration |
| IDE integration tests | `kronos-testing/ide/src/test` | Actual KEFS loading, inferred types, stock Kotlin completion, syntax highlighting and diagnostics | Source-string checks in place of editor behavior |

Assertion rule: positive tests must assert the complete expected value, for example with `assertEquals(expectedSql, actualSql)`. Do not use `contains` for positive SQL, code, or text checks. Substring assertions are only acceptable for negative checks, such as proving forbidden text is absent with `assertFalse(actual.contains(forbidden))`.

Historical note: kctfork / kotlin-compile-testing tests and helpers such as `KotlinSourceDynamicCompiler` and `IrTestFramework` were removed from `kronos-compiler-plugin`. Do not add new tests using kctfork; migrate compiler-plugin behavior into official `testData` tests instead. Keep ordinary utility tests when they do not require compiler execution.

When adding or migrating compiler-plugin tests:

1. Read `.agents/skills/kronos-dev-kcp/SKILL.md`, especially the Kronos official compiler testData style section.
2. If the behavior depends on generated KPojo members or DSL IR rewrites, add a `testData/box/.../*.kt` file and a thin official runner method.
3. If invalid source should fail, use `testData/diagnostics`, not a box test.
4. If a removed kctfork behavior is rediscovered as uncovered, add a precise official `testData` file that asserts the same compiler-plugin contract and important values.
5. Keep pure utility coverage as ordinary unit tests; do not force utility behavior through compiler testData.

## Build And Test Commands

Use JDK 21 and the checked-in Kotlin Toolchain wrapper through the Kotlin script entry point. `scripts/run` downloads and verifies the catalogued official Kotlin SDK; Windows uses `scripts\run.cmd`:

```bash
scripts/run build bootstrap
scripts/run build build -m kronos-core -p jvm
scripts/run build test -m kronos-core -p jvm
scripts/run build test -m kronos-compiler-plugin -p jvm --include-classes='*KPojoGeneratedBodiesBoxTest'
scripts/run build test -m kronos-core -p macosX64
```

Repeat `--include-classes` for multiple class globs. Use one module and platform before broadening the run. The bootstrap publishes the source-built compiler to `build/repository/bootstrap`, then stages its KEFS compatibility coordinate without changing the JAR. Root configuration lives in `project.yaml`, module configuration in `module.yaml`, and dependencies in `libs.versions.toml`.

For source development, install the official Kotlin Toolchain IDE plugin and KEFS by Mr3zee. Open the directory containing `project.yaml` and use the shared `.idea/kotlin-plugins.xml`. The standalone editor fixture commands are documented in `kronos-testing/ide/README.md`.

## Coverage Workflow

```bash
scripts/run quality coverage --module kronos-compiler-plugin
scripts/run quality coverage --baseline-dir .coverage-baseline
scripts/run quality compare --baseline-dir .coverage-baseline
```

The coverage command runs instrumented JVM tests and writes `<module>/build/reports/kover/report.xml`, `html/index.html` and `summary.json`. Omitting `--module` covers core, syntax, compiler-plugin and codegen. The comparison writes `coverage/comparison.json` and `coverage/comparison.md`, and fails when the baseline policy is violated. Use these reports to identify meaningful missing behavior before adding tests.

## Reference Files

Read these for deep implementation details:

| File | When to Read |
|------|-------------|
| `Evolution.index.md` | Required first read before fixing any error; maps symptoms and keywords to targeted `evolution/*.md` entries |
| `Evolution.md` | Short protocol stub; do not use it as a full history store |
| `evolution/*.md` | One file per historical pitfall; open only the indexed matching file |
| `references/compiler-plugin.md` | Modifying compiler plugin, adding/changing IR transforms, debugging IR output. Before compiler plugin architecture, FIR/frontend, IR/backend, diagnostics, or compiler-test work, use `.agents/skills/kronos-dev-kcp` first. |
| `references/ast-and-rendering.md` | Working with AST nodes, SQL rendering, adding/modifying database dialects, functions system |
| `references/orm-and-dsl.md` | ORM clause classes, DSL beans, plugin/hook system, task execution, transactions, value transformers |
| `references/api-design.md` | Designing or reviewing Kronos user-facing DSL syntax, API shape, subquery syntax, projection syntax, or documentation specs before implementation |
| `references/modules.md` | Working on codegen, logging, jdbc-wrapper, gradle/maven plugins |
| `references/testing-and-ci.md` | Writing tests, coverage, CI workflows, version management, publishing, documentation |
| `references/cookbook.md` | Step-by-step: add new DB dialect, add new DSL operation, add subquery syntax, write tests |
