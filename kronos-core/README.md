# Module kronos-core

Core module of the Kronos ORM framework. Zero runtime dependencies.

## What's Inside

- **AST-based SQL generation** — typed `SqlNode` hierarchy (`Expression`, `Statement`, `TableReference`) rendered per-dialect by `SqlRenderer` implementations
- **ORM operations** — `select/`, `insert/`, `update/`, `delete/`, `upsert/`, `join/`, `union/`, `cascade/`, `ddl/`, `pagination/`
- **DSL beans** — `Field`, `Criteria`, `FunctionField`, `KTableForCondition/Select/Set/Sort/Reference`
- **Database dialects** — MySQL, PostgreSQL, SQLite, SQL Server, Oracle (extensible via `RegisteredDBTypeManager`)
- **Interfaces** — `KPojo`, `KronosDataSourceWrapper`, `KLogger`, `KronosNamingStrategy`
- **Annotations** — `@Table`, `@PrimaryKey`, `@Column`, `@ColumnType`, `@CreateTime`, `@UpdateTime`, `@LogicDelete`, `@Version`, `@Cascade`, `@TableIndex`, `@Serialize`, etc.
- **Built-in strategies** — primary key generation, logical deletion, optimistic lock, timestamps, naming
- **Functions system** — `FunctionManager` with bundled math, string, aggregate, and Postgres-specific builders
- **Task system** — `KronosQueryTask`, `KronosActionTask`, `KronosAtomicBatchTask`, `TransactionScope`
- **Plugins** — `DataGuardPlugin` (prevents full-table UPDATE/DELETE), `LastInsertIdPlugin`
- **Global config** — `Kronos.kt` singleton

## Dependencies

Zero runtime dependencies. Compiler plugin is only used for tests via `kotlinCompilerPluginClasspathTest`.
