# Kronos-ORM

Kotlin compiler-plugin-powered ORM framework. Zero reflection, strong typing, multi-database support.

## Module Structure

| Module | Description |
|--------|-------------|
| `kronos-core` | Core contracts (`KPojo`, `KLogger`, `KronosDataSourceWrapper`), AST-based SQL generation, DSL beans, ORM operations (select/insert/update/delete/upsert/ddl/cascade/join/union), strategies, caching, i18n |
| `kronos-compiler-plugin` | K2 compiler plugin — IR transformations for KPojo class augmentation and KTable DSL parsing (condition/select/set/sort/reference) |
| `kronos-compiler-plugin-legacy` | Legacy (pre-K2) compiler plugin, same transformation goals, older internal structure |
| `kronos-gradle-plugin` | Gradle plugin wiring the compiler plugin into Kotlin compilation (included build) |
| `kronos-maven-plugin` | Maven plugin counterpart |
| `kronos-logging` | Pluggable logging — auto-detects SLF4J, Commons Logging, JUL, Android Log |
| `kronos-jdbc-wrapper` | Default JDBC `KronosDataSourceWrapper` implementation with thread-local transaction support |
| `kronos-codegen` | TOML-config-driven code generator: DB schema → annotated KPojo Kotlin files |
| `kronos-testing` | Integration tests against MySQL, PostgreSQL, SQLite, Oracle, SQL Server |
| `kronos-docs` | Documentation website (Angular) |
| `build-logic` | Shared Gradle convention plugins (`kronos.publishing`, `kronos.dokka-convention`) |

## Build & Test

```bash
# Build everything
./gradlew clean build

# Run all tests (integration + unit, checks DB connectivity first)
./test.sh

# Run unit tests only
./gradlew test

# Single module
./gradlew :kronos-core:test

# Integration tests (requires DB env vars, see below)
./gradlew :kronos-testing:test --info --stacktrace

# Static analysis
./gradlew detekt
```

### Integration Test Environment Variables

| Variable | Example |
|----------|---------|
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | MySQL 8.x credentials |
| `POSTGRES_USERNAME` / `POSTGRES_PASSWORD` | PostgreSQL 17 credentials |

## Code Conventions

- **License header**: Apache 2.0 in every source file
- **Commit messages**: Angular convention — `<type>(<scope>): <subject>` (feat, fix, docs, refactor, test, ci, chore, perf, style, build)
- **Naming**: Kotlin coding conventions. `KPojo` data classes use nullable properties with `null` defaults.
- **Kotlin version**: 2.3.0, JVM target 1.8, build toolchain JDK 17/21
- **No runtime dependencies** in `kronos-core` — everything is self-contained

## Architecture Highlights

- **KPojo interface**: All entity classes implement `KPojo`. The compiler plugin generates method bodies (`toDataMap`, `fromMapData`, `kronosColumns`, `get`/`set`, etc.) at compile time — zero reflection at runtime.
- **AST-based SQL generation**: SQL is built as a typed AST (`SqlNode` → `Expression` / `Statement` / `TableReference`) then rendered per-dialect by `SqlRenderer` implementations (MySQL, PostgreSQL, SQLite, Oracle, MSSQL).
- **Compiler plugin IR transforms**: The K2 plugin (`KronosParserTransformer`) intercepts `KTableForSelect/Set/Condition/Sort/Reference` lambdas and rewrites them into `Field`/`Criteria` IR at compile time. `KronosIrClassTransformer` augments KPojo classes.
- **Transaction DSL**: `Kronos.transact { }` with configurable isolation, timeout, savepoint support (`savepoint`/`rollbackToSavepoint`/`releaseSavepoint`), and nested transaction join semantics (inner reuses outer connection).
- **Built-in strategies**: Logical deletion, optimistic lock, create/update timestamps, primary key generation (UUID, Snowflake) — all configurable per-entity via annotations or global config.
- **Database dialects**: Registered via `RegisteredDBTypeManager`. Each dialect provides a `SqlRenderer` and a `ConflictResolver` for upsert.

## Detailed Dev Guide

For module internals, transformer pipelines, adding new DB support, DSL operations, testing patterns, and CI/CD workflows, see:

→ `.claude/skills/kronos-dev-guide`
