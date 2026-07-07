{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 用多组参数执行同一条 SQL

同一条 SQL 需要写入多行时，使用 `SqlHandler.batchExecute`。它会创建 `KronosAtomicBatchTask`，并调用当前 `KronosDataSourceWrapper.batchUpdate(...)`。

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

数组中的每个 map 都会绑定到 SQL 中的命名参数。返回的 `IntArray` 是 JDBC driver 返回的逐行执行结果。

## 直接创建批量任务

需要传递操作类型等元数据，或代码已经在处理 wrapper task 时，可以直接使用 `KronosAtomicBatchTask`。

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

`KronosAtomicBatchTask.parsedArr()` 会把命名参数 SQL 转成一条 JDBC SQL，并为每行生成一组 JDBC 参数。

```kotlin group="Parsed batch" name="kotlin" icon="kotlin"
val (jdbcSql, jdbcParams) = task.parsedArr()

jdbcSql == "INSERT INTO user (name, age) VALUES (?, ?)"
jdbcParams.map { it.toList() } == listOf(
    listOf("Alice", 18),
    listOf("Bob", 20)
)
```

## 批量插入形态

批量插入使用同样的批量任务形态。保持固定列列表，并为每行传入一个参数 map。

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

单行 KPojo mutation DSL 见 {{ $.keyword("mutation/insert", ["插入"]) }}、{{ $.keyword("mutation/update", ["更新"]) }}、{{ $.keyword("mutation/delete", ["删除"]) }} 和 {{ $.keyword("mutation/upsert", ["更新插入"]) }}。
