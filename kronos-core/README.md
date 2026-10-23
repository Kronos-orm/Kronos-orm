# Module kronos-core

Multiplatform ORM runtime for JVM, Android and Kotlin/Native. Provides the shared query DSL, entity metadata, SQL planning and database execution contracts.

## What's Inside

- **SQL generation** — Kronos DSL builds SQL syntax trees rendered by `kronos-syntax`
- **ORM operations** — `select/`, `insert/`, `update/`, `delete/`, `upsert/`, `join/`, `union/`, `cascade/`, `ddl/`, `pagination/`
- **DSL beans** — `Field`, `Criteria`, `KronosFunctionExpr`, `KTableForCondition/Select/Set/Sort/Reference`
- **Database dialects** — MySQL, PostgreSQL, SQLite, H2, SQL Server, Oracle, DM8 (extensible via `SqlManager.registerDatabase`)
- **Interfaces** — `KPojo`, `KronosDataSourceWrapper`, `KronosExecutionDataSource`, `KLogger`, `KronosNamingStrategy`
- **Annotations** — `@Table`, `@PrimaryKey`, `@Column`, `@ColumnType`, `@CreateTime`, `@UpdateTime`, `@LogicDelete`, `@Version`, `@Cascade`, `@TableIndex`, `@Serialize`, `@KronosFunction`, etc.
- **Built-in strategies** — primary key generation, logical deletion, optimistic lock, timestamps, naming
- **Functions system** — `FunctionHandler` DSL extensions and dialect-specific SQL rendering
- **Task system** — `KronosQueryTask`, `KronosActionTask`, `KronosAtomicBatchTask`, `TransactionScope`
- **Plugins** — `DataGuardPlugin` prevents full-table UPDATE/DELETE
- **Global config** — `Kronos.kt` singleton

## Dependencies

Exports `kronos-syntax` and uses Kotlinx DateTime for JVM and Native time handling. Database drivers and connection adapters are selected by the application: use `kronos-jdbc-wrapper` for JDBC or implement `KronosExecutionDataSource` and wrap it with `KronosExecutionWrapper`.

The Kronos compiler plugin generates entity methods, typed projections and metadata during compilation. Native applications initialize generated metadata with `initializeKronosMetadata()` in the final application or framework module.
