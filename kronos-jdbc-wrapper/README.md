# Module kronos-jdbc-wrapper

Default JDBC-based `KronosDataSourceWrapper` implementation for Kronos ORM.

## What It Provides

- Wraps any `javax.sql.DataSource` into `KronosJdbcWrapper`
- Named parameter binding (`:name` → `?` conversion)
- Query execution: `toList` and `first` with full `KType` result mapping
- DML execution: `update`, `batchUpdate` with last-insert-ID capture
- Transaction management: thread-local connection propagation, savepoints, nested transaction join semantics
- Type-safe JDBC value conversion

## Usage

```kotlin
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper

val wrapper = KronosJdbcWrapper(dataSource)
Kronos.dataSource = { wrapper }
```

Force a database type or tune JDBC execution through the constructor configuration block:

```kotlin
import com.kotlinorm.enums.DBType
import com.kotlinorm.wrappers.KronosJdbcWrapper
import com.kotlinorm.wrappers.KronosSqlWarningPolicy

val wrapper = KronosJdbcWrapper(dataSource, databaseType = DBType.Mysql) {
    statement.fetchSize = 1000
    statement.queryTimeoutSeconds = 30
    warningPolicy = KronosSqlWarningPolicy.THROW
}
```

The configuration block exposes statement and result-set settings, SQL warning policy, exception translation, argument binders, column mappers, Oracle LONG handling, and the loaded plugin list.

## Dependencies

- `compileOnly`: kronos-core (no external deps beyond JDK `javax.sql`)
