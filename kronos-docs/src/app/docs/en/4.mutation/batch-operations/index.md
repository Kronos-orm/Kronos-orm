{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Execute one SQL statement with many parameter sets

Use `SqlHandler.batchExecute` when the same SQL statement should run for multiple rows. It creates a `KronosAtomicBatchTask` and calls the active `KronosDataSourceWrapper.batchUpdate(...)`.

```kotlin group="Batch execute" name="kotlin" icon="kotlin"
import com.kotlinorm.database.SqlHandler

val affectedRows: IntArray = with(SqlHandler) {
    wrapper.batchExecute(
        "UPDATE user SET name = :name WHERE id = :id",
        arrayOf(
            mapOf("name" to "Alice", "id" to 1),
            mapOf("name" to "Bob", "id" to 2)
        )
    )
}
```

```sql group="Batch execute" name="SQL"
UPDATE user SET name = :name WHERE id = :id
```

```text group="Batch execute" name="result"
affectedRows == intArrayOf(1, 1)
```

Each map in the array is bound to the named parameters in the SQL string. The returned `IntArray` is the per-row result returned by the JDBC driver.

## Build a batch task directly

Use `KronosAtomicBatchTask` when you need to pass operation metadata or when your code already works with wrapper tasks.

```kotlin group="Batch task" name="kotlin" icon="kotlin"
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.enums.KOperationType

val task = KronosAtomicBatchTask(
    sql = "INSERT INTO user (name, age) VALUES (:name, :age)",
    paramMapArr = arrayOf(
        mapOf("name" to "Alice", "age" to 18),
        mapOf("name" to "Bob", "age" to 20)
    ),
    operationType = KOperationType.INSERT
)

val affectedRows = wrapper.batchUpdate(task)
```

```sql group="Batch task" name="SQL"
INSERT INTO user (name, age) VALUES (:name, :age)
```

```text group="Batch task" name="result"
affectedRows == intArrayOf(1, 1)
```

`KronosAtomicBatchTask.parsedArr()` converts the named-parameter SQL into one JDBC SQL string and one JDBC parameter list per row.

```kotlin group="Parsed batch" name="kotlin" icon="kotlin"
val (jdbcSql, jdbcParams) = task.parsedArr()

jdbcSql == "INSERT INTO user (name, age) VALUES (?, ?)"
jdbcParams.map { it.toList() } == listOf(
    listOf("Alice", 18),
    listOf("Bob", 20)
)
```

## Batch insert shape

Batch insert uses the same batch task shape. Keep the column list fixed and pass one parameter map per row.

```kotlin group="Batch insert" name="kotlin" icon="kotlin"
with(SqlHandler) {
    wrapper.batchExecute(
        "INSERT INTO user (name, age) VALUES (:name, :age)",
        arrayOf(
            mapOf("name" to "Alice", "age" to 18),
            mapOf("name" to "Bob", "age" to 20)
        )
    )
}
```

For single-row KPojo mutation DSL, use {{ $.keyword("mutation/insert", ["Insert"]) }}, {{ $.keyword("mutation/update", ["Update"]) }}, {{ $.keyword("mutation/delete", ["Delete"]) }}, and {{ $.keyword("mutation/upsert", ["Upsert"]) }}.
