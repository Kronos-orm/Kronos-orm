# Module kronos-core

Core module of the Kronos ORM framework. Zero runtime dependencies.

## What's Inside

- **Syntax-based SQL generation** — Kronos DSL lowers SQL expressions toward `kronos-syntax` trees while legacy AST adapters are being retired
- **ORM operations** — `select/`, `insert/`, `update/`, `delete/`, `upsert/`, `join/`, `union/`, `cascade/`, `ddl/`, `pagination/`
- **DSL beans** — `Field`, `Criteria`, `KronosFunctionExpr`, `KTableForCondition/Select/Set/Sort/Reference`
- **Database dialects** — MySQL, PostgreSQL, SQLite, SQL Server, Oracle (extensible via `RegisteredDBTypeManager`)
- **Interfaces** — `KPojo`, `KronosDataSourceWrapper`, `KLogger`, `KronosNamingStrategy`
- **Annotations** — `@Table`, `@PrimaryKey`, `@Column`, `@ColumnType`, `@CreateTime`, `@UpdateTime`, `@LogicDelete`, `@Version`, `@Cascade`, `@TableIndex`, `@Serialize`, `@KronosFunction`, etc.
- **Built-in strategies** — primary key generation, logical deletion, optimistic lock, timestamps, naming
- **Functions system** — `FunctionHandler` DSL extensions lowered to syntax expressions, with optional `FunctionManager` renderers during migration
- **Task system** — `KronosQueryTask`, `KronosActionTask`, `KronosAtomicBatchTask`, `TransactionScope`
- **Plugins** — `DataGuardPlugin` (prevents full-table UPDATE/DELETE), `LastInsertIdPlugin`
- **Global config** — `Kronos.kt` singleton

## Dependencies

Zero runtime dependencies. Compiler plugin is only used for tests via `kotlinCompilerPluginClasspathTest`.
