{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Transaction

In Kronos, transaction is an important concept that allows you to combine multiple database operations into one atomic operation. That is, either all operations execute successfully or none of them do. This is important to ensure data consistency and integrity.

## {{ $.title("Kronos.transact")}}Use of transaction

Kronos transactions are very simple to use. You just need to use the `Kronos.transaction` method in your code to start a transaction and perform your database operations in the transaction block.

```kotlin
import com.kotlinorm.Kronos.transact

transact {
    // Perform database operations here, using the default data source
}

transact(dataSourceWrapper) {
    // Perform database operations here, using the specified data source
}
```

The return value of the transact method is of type `Any?` and indicates the result of the execution of the transaction.
To add transaction wrappers to your functions with return values, you can do this:

```
// before
fun doRequest(): String {
    // do something
    return "result"
}
// after
fun doRequest(): String {
    return transact {
        // do something
        "result"
        // or return@transact "result"
    } as String
}
```

In a transaction block, you can perform any database operation such as inserts, updates, deletions, and so on. If an exception occurs in a transaction block, Kronos automatically rolls back the transaction to ensure data consistency.

If all operations in the transaction block are executed successfully, Kronos will automatically commit the transaction.

This means that all operations are permanently saved to the database.

> **Note**
> When using transactions, make sure that the KronosDataSourceWrapper you are using and your database support transactional operations.

## Nesting of transactions

Kronos supports nested transactions. This means that you can start another transaction block within a transaction block. kronos automatically manages the commit and rollback of nested transactions.
```kotlin
Kronos.transact {
    // Perform database operations here
    Kronos.transact {
        // Perform database operations here
    }
}
```

In the above code, the outer transaction block commits after the inner transaction block executes successfully. If an exception occurs in the inner transaction block, Kronos automatically rolls back the outer transaction block.

## Transaction isolation levels

Kronos supports multiple transaction isolation levels, including:

- READ_UNCOMMITTED
- READ_COMMITTED
- REPEATABLE_READ
- SERIALIZABLE

You can specify the transaction isolation level when calling `transact` via the `isolation` parameter:

```kotlin
import com.kotlinorm.Kronos.transact
import com.kotlinorm.enums.TransactionIsolation

transact(isolation = TransactionIsolation.READ_COMMITTED) {
    // Perform database operations here
}
```

## Transaction timeout

Kronos supports setting a timeout for transactions (in seconds). You can specify the transaction timeout when calling `transact` via the `timeout` parameter:

```kotlin
transact(timeout = 30) {
    // Perform database operations here, with a 30-second timeout
}
```

You can also specify both isolation level and timeout:

```kotlin
transact(isolation = TransactionIsolation.REPEATABLE_READ, timeout = 30) {
    // Perform database operations here
}
```

## Saving points for transactions

Kronos supports savepoints for transactions. You can use the `savepoint` method in a transaction block to set the savepoint, and then use the `rollbackToSavepoint` method in the transaction block to roll back to the savepoint.

```kotlin
Kronos.transact {
    // Perform database operations here
    val savepoint = savepoint("savepoint1")
    // Perform database operations here
    rollbackToSavepoint(savepoint)
}
```

You can also use the `releaseSavepoint` method to release a savepoint:

```kotlin
Kronos.transact {
    val sp = savepoint("sp1")
    // Perform database operations here
    releaseSavepoint(sp)
}
```
