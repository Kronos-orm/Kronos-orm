{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Raw SQL methods are extensions declared by `SqlExecutor`. Import the extension you use, for example:

```kotlin group="Native SQL import" name="kotlin" icon="kotlin"
import com.kotlinorm.database.SqlExecutor.query
import com.kotlinorm.database.SqlExecutor.queryList
import com.kotlinorm.database.SqlExecutor.queryMap
import com.kotlinorm.database.SqlExecutor.queryOne
import com.kotlinorm.database.SqlExecutor.queryOneOrNull
```

## 1. {{ $.title("query") }} Query Map List

Execute SQL statements, query multiple records, and return a list of maps.

- **Function Declaration**

    ```kotlin
    fun query(sql: String, params: Map<String, Any?> = emptyMap()): List<Map<String, Any?>>
    ```

- **Usage Example**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: List<Map<String, Any?>> = wrapper.query(sql, params)
    ```

- **Parameters**:

    {{$.params([
    ['sql', 'SQL query statement', 'String'],
    ['params', 'Named parameters', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **Return Value**:

    `List<Map<String, Any?>>` query result list. Selected SQL `NULL` values remain present under their column keys.

{{ $.hr() }}

## 2. {{ $.title("queryList") }} Query Specified Type List

- **Generic Parameters**： `<T>` Query result type

{{ $.hr(50) }}

Execute SQL statements, query multiple records, and return a list of specified types.

> **Note**
> When querying a single column, you can directly set the generic parameter to the type of the column, for example: `queryList<Int>()`
>
> When querying multiple columns, you can set the generic parameter to a subclass of KPojo, for example: `queryList<User>()`
>
> Kronos recognizes `T : KPojo` at runtime, so ordinary calls do not need to pass wrapper mapping flags manually.

- **Function Declaration**

    ```kotlin
    fun <T> queryList(sql: String, params: Map<String, Any?> = emptyMap()): List<T>
    ```

- **Usage Example**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: List<YourType> = wrapper.queryList(sql, params)
    ```

- **Parameters**:

    {{$.params([
    ['sql', 'SQL query statement', 'String'],
    ['params', 'Named parameters', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **Return Value**:

    `List<T>` Query result list

{{ $.hr() }}

## 3. {{ $.title("queryMap") }} Query Single Record Map

Execute SQL statements, query a single record, and return a map.

- **Function Declaration**

    ```kotlin
    fun queryMap(sql: String, params: Map<String, Any?> = emptyMap()): Map<String, Any?>?
    ```

- **Usage Example**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: Map<String, Any?>? = wrapper.queryMap(sql, params)
    ```

- **Parameters**:

    {{$.params([
    ['sql', 'SQL query statement', 'String'],
    ['params', 'Named parameters', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **Return Value**:

    `Map<String, Any?>?` Query result

{{ $.hr() }}

## 4. {{ $.title("queryOne") }} Query Specified Type Single Record

- **Generic Parameters**： `<T>` Query result type

{{ $.hr(50) }}

Execute SQL statements, query a single record, and return a specified type.

> **Note**
> When querying a single column, you can directly set the generic parameter to the type of the column, for example: `queryOne<Int>()`
>
> When querying multiple columns, you can set the generic parameter to a subclass of KPojo, for example: `queryOne<User>()`
>
> Kronos recognizes `T : KPojo` at runtime, so ordinary calls do not need to pass wrapper mapping flags manually.

- **Function Declaration**

    ```kotlin
    fun <T> queryOne(sql: String, params: Map<String, Any?> = emptyMap()): T
    ```

- **Usage Example**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: YourType = wrapper.queryOne(sql, params)
    ```

- **Parameters**:

    {{$.params([
    ['sql', 'SQL query statement', 'String'],
    ['params', 'Named parameters', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **Return Value**:

    `T` Query result

{{ $.hr() }}

## 5. {{ $.title("queryOneOrNull") }} Query Specified Type Single Record (Nullable)

- **Generic Parameters**： `<T>` Query result type

{{ $.hr(50) }}

Execute SQL statements, query a single record, and return a specified type. When the query result is empty, return null.

> **Note**
> When querying a single column, you can directly set the generic parameter to the type of the column, for example: `queryOneOrNull<Int>()`
>
> When querying multiple columns, you can set the generic parameter to a subclass of KPojo, for example: `queryOneOrNull<User>()`

- **Function Declaration**

    ```kotlin
    fun <T> queryOneOrNull(sql: String, params: Map<String, Any?> = emptyMap()): T?
    ```

- **Usage Example**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: YourType? = wrapper.queryOneOrNull(sql, params)
    ```

- **Parameters**:

    {{$.params([
    ['sql', 'SQL query statement', 'String'],
    ['params', 'Named parameters', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **Return Value**:

    `T?` Query result

{{ $.hr() }}

## 6. {{ $.title("execute") }} Execute SQL

Execute SQL statements and return the number of affected rows.

- **Function Declaration**

    ```kotlin
    fun execute(sql: String, params: Map<String, Any?> = emptyMap()): Int
    ```

- **Usage Example**

    ```kotlin
    val sql = "UPDATE table SET column = :value WHERE id = :id"
    val params = mapOf("value" to "some value", "id" to 1)
    val affectedRows: Int = wrapper.execute(sql, params)
    ```

- **Parameters**:

    {{$.params([
    ['sql', 'SQL query statement', 'String'],
    ['params', 'Named parameters', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **Return Value**:

    `Int` Number of affected rows

{{ $.hr() }}

## 7. {{ $.title("batchExecute") }} Batch Execute SQL

- **Function Declaration**

    ```kotlin
    fun batchExecute(sql: String, params: Array<Map<String, Any?>>): IntArray
    ```

- **Usage Example**

    ```kotlin
    val sql = "UPDATE table SET column = :value WHERE id = :id"
    val params = arrayOf(
        mapOf("value" to "some value", "id" to 1),
        mapOf("value" to "another value", "id" to 2)
    )
    val result: IntArray = wrapper.batchExecute(sql, params)
    ```

- **Parameters**:

    {{$.params([
    ['sql', 'SQL query statement', 'String'],
    ['params', 'Named parameters list', 'Array<Map<String, Any?>>']
    ])}}

- **Return Value**:

    `IntArray` Number of affected rows for each parameter set.

For batch task shape, named-parameter parsing, and insert/update examples, see {{ $.keyword("mutation/batch-operations", ["Batch Operations"]) }}.
