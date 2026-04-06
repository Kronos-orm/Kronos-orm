# Module kronos-jdbc-wrapper

Default JDBC-based `KronosDataSourceWrapper` implementation for Kronos ORM.

## What It Provides

- Wraps any `javax.sql.DataSource` into `KronosBasicWrapper`
- Named parameter binding (`:name` → `?` conversion)
- Query execution: `forList`, `forMap`, `forObject` with type-safe result mapping
- DML execution: `update`, `batchUpdate` with last-insert-ID capture
- Transaction management: thread-local connection propagation, savepoints, nested transaction join semantics
- Type-safe JDBC value conversion

## Usage

```kotlin
val wrapper = KronosBasicWrapper(dataSource)
Kronos.init {
    dataSource = { wrapper }
}
```

## Dependencies

- `compileOnly`: kronos-core (no external deps beyond JDK `javax.sql`)
