{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Transaction

Use `Kronos.transact` to run multiple database operations in one transaction. The block returns its last expression, commits when it completes, and rolls back when an exception leaves the block.

## {{ $.title("Kronos.transact")}} transaction entry

`Kronos.transact` uses the default `Kronos.dataSource` when no wrapper is passed. Pass a `KronosDataSourceWrapper` as the first argument when the transaction should run on a specific data source.

```kotlin group="Transaction 1" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos.transact

val result = transact {
    User(name = "Ada").insert().execute()
    User(id = 1).update().set { it.name = "Ada Lovelace" }.where().execute()
    "committed"
}

val customResult = transact(wrapper) {
    User(name = "Grace").insert().execute()
    "committed on wrapper"
}
```

```kotlin group="Transaction 1" name="signature" icon="kotlin"
fun transact(
    wrapper: KronosDataSourceWrapper? = null,
    isolation: TransactionIsolation? = null,
    timeout: Int? = null,
    block: TransactionScope.() -> Any?
): Any?
```

Return a typed value by returning the value from the block.

```kotlin group="Transaction 2" name="return value" icon="kotlin"
fun createUser(): String {
    return transact {
        User(name = "Ada").insert().execute()
        "ok"
    } as String
}
```

## Roll back on exception

When the block throws, the JDBC wrapper rolls back the transaction and rethrows the exception.

```kotlin group="Rollback" name="kotlin" icon="kotlin"
Kronos.transact {
    User(name = "Ada").insert().execute()
    error("stop this transaction")
}
```

## Nested transactions reuse the connection

Nested `Kronos.transact` blocks on `KronosJdbcWrapper` reuse the active transaction connection. The outer transaction owns the final commit or rollback.

```kotlin group="Nested" name="kotlin" icon="kotlin"
Kronos.transact {
    User(name = "Ada").insert().execute()

    Kronos.transact {
        User(id = 1).update().set { it.name = "Ada Lovelace" }.where().execute()
    }
}
```

## Isolation and timeout

Pass `TransactionIsolation` and `timeout` to configure the transaction. The timeout value is in seconds.

```kotlin group="Options" name="kotlin" icon="kotlin"
import com.kotlinorm.enums.TransactionIsolation

Kronos.transact(
    isolation = TransactionIsolation.READ_COMMITTED,
    timeout = 30
) {
    User(name = "Ada").insert().execute()
}
```

Available isolation values are `READ_UNCOMMITTED`, `READ_COMMITTED`, `REPEATABLE_READ`, and `SERIALIZABLE`.

> **Note**
> Android/JVM SQLite transaction behavior is documented in {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }}.

## Savepoints

`TransactionScope` is the transaction block receiver. JDBC wrappers can expose the active connection to `TransactionScope`, which enables `savepoint`, `rollbackToSavepoint`, and `releaseSavepoint`.

```kotlin group="Savepoint" name="kotlin" icon="kotlin"
Kronos.transact {
    User(name = "Ada").insert().execute()
    val point = savepoint("before_status_update")

    try {
        User(id = 1).update().set { it.status = "ACTIVE" }.where().execute()
        releaseSavepoint(point)
    } catch (e: Exception) {
        rollbackToSavepoint(point)
        throw e
    }
}
```

> **Note**
> Savepoints require a `TransactionScope` backed by a JDBC connection. `KronosJdbcWrapper` provides that connection; custom wrappers should pass their transaction connection when they run the block.
