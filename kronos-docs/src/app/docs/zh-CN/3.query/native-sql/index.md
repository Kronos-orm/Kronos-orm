{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

原生 SQL 方法是 `SqlExecutor` 提供的扩展函数。按需导入对应方法，例如：

```kotlin group="Native SQL import" name="kotlin" icon="kotlin"
import com.kotlinorm.database.SqlExecutor.query
import com.kotlinorm.database.SqlExecutor.queryList
import com.kotlinorm.database.SqlExecutor.queryMap
import com.kotlinorm.database.SqlExecutor.queryOne
import com.kotlinorm.database.SqlExecutor.queryOneOrNull
import com.kotlinorm.database.SqlExecutor.toList
import com.kotlinorm.database.SqlExecutor.first
import com.kotlinorm.database.SqlExecutor.firstOrNull
```

## 1. {{ $.title("query") }} 查询Map列表

执行SQL语句，查询多行记录

- **函数声明**

    ```kotlin
    fun query(sql: String, params: Map<String, Any?> = emptyMap()): List<Map<String, Any?>>
    ```

- **使用示例**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: List<Map<String, Any?>> = wrapper.query(sql, params)
    ```

- **接收参数**：

    {{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **返回值**：

    `List<Map<String, Any?>>` 查询结果列表。选中的 SQL `NULL` 会保留对应列名，值为 `null`。

{{ $.hr() }}

## 2. {{ $.title("queryList") }} 查询指定类型列表

- **泛型参数**： `<T>` 查询结果类型

{{ $.hr(50) }}

执行SQL语句，查询多行记录并返回指定类型列表

> **Note**
> 查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryList<Int>()`
>
> 查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryList<User>()`
>
> Kronos 会在运行时识别 `T : KPojo`，普通调用不需要手动传 wrapper 映射标记。

- **函数声明**

    ```kotlin
    fun <T> queryList(sql: String, params: Map<String, Any?> = emptyMap()): List<T>
    ```

- **使用示例**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: List<YourType> = wrapper.queryList(sql, params)
    ```

- **接收参数**：

    {{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **返回值**：

    `List<T>` 查询结果列表

```kotlin group="Native SQL row mapper list" name="kotlin" icon="kotlin"
data class UserRow(
    val id: Int?,
    val displayName: String?
)

val result: List<UserRow> = wrapper.toList(
    "SELECT id, name AS display_name FROM user WHERE age >= :age",
    mapOf("age" to 18)
) { row ->
    UserRow(
        id = row.get<Int?>(1),
        displayName = row.get<String?>("display_name")
    )
}
```

`row.get<T>("label")` 按 JDBC 列名或 alias 读取，`row.get<T>(field)` 使用 `Field` 的投影输出名，名称为空时回退到数据库列名，`row.get<T>(position)` 按 JDBC 列位置读取，位置从 `1` 开始。值按目标 `KType` 经过当前结果映射和 `ValueCodec` 转换，`rowNumber` 从 `0` 开始。`row` 在映射回调执行期间有效；回调结束后使用已构造的结果对象。`KronosJdbcWrapper` 支持该行映射能力。

{{ $.hr() }}

## 3. {{ $.title("queryMap") }} 查询单行记录Map

执行SQL语句，查询单行记录

- **函数声明**

    ```kotlin
    fun queryMap(sql: String, params: Map<String, Any?> = emptyMap()): Map<String, Any?>?
    ```

- **使用示例**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: Map<String, Any?>? = wrapper.queryMap(sql, params)
    ```

- **接收参数**：

    {{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **返回值**：

    `Map<String, Any?>?` 查询结果Map

{{ $.hr() }}

## 4. {{ $.title("queryOne") }} 查询指定类型单行记录

- **泛型参数**： `<T>` 查询结果类型

{{ $.hr(50) }}

执行SQL语句，查询单行记录返回指定类型，当查询结果为空时，抛出异常。

> **Note**
> 查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOne<Int>()`
>
> 查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOne<User>()`
>
> Kronos 会在运行时识别 `T : KPojo`，普通调用不需要手动传 wrapper 映射标记。

- **函数声明**

    ```kotlin
    fun <T> queryOne(sql: String, params: Map<String, Any?> = emptyMap()): T
    ```

- **使用示例**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: YourType = wrapper.queryOne(sql, params)
    ```

- **接收参数**：

    {{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **返回值**：

    `T` 查询结果

```kotlin group="Native SQL row mapper first" name="kotlin" icon="kotlin"
val result: UserRow = wrapper.first(
    "SELECT id, name AS display_name FROM user WHERE id = :id",
    mapOf("id" to 1)
) { row ->
    UserRow(row.get<Int?>("id"), row.get<String?>("display_name"))
}
```

{{ $.hr() }}

## 5. {{ $.title("queryOneOrNull") }} 查询指定类型单行记录（可空）

- **泛型参数**： `<T>` 查询结果类型

{{ $.hr(50) }}

执行SQL语句，查询单行记录返回指定类型。

> **Note**
> 查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOneOrNull<Int>()`
>
> 查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOneOrNull<User>()`

- **函数声明**

    ```kotlin
    fun <T> queryOneOrNull(sql: String, params: Map<String, Any?> = emptyMap()): T?
    ```

- **使用示例**

    ```kotlin
    val sql = "SELECT * FROM table WHERE column = :value"
    val params = mapOf("value" to "some value")
    val result: YourType? = wrapper.queryOneOrNull(sql, params)
    ```

- **接收参数**：

    {{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **返回值**：

    `T?` 查询结果

```kotlin group="Native SQL row mapper first or null" name="kotlin" icon="kotlin"
val result: UserRow? = wrapper.firstOrNull(
    "SELECT id, name AS display_name FROM user WHERE id = :id",
    mapOf("id" to 404)
) { row ->
    UserRow(row.get<Int?>("id"), row.get<String?>("display_name"))
}
```

{{ $.hr() }}

## 6. {{ $.title("execute") }} 执行SQL

用于执行SQL语句，返回受影响的行数。

- **函数声明**

    ```kotlin
    fun execute(sql: String, params: Map<String, Any?> = emptyMap()): Int
    ```

- **使用示例**

    ```kotlin
    val sql = "UPDATE table SET column = :value WHERE id = :id"
    val params = mapOf("value" to "some value", "id" to 1)
    val affectedRows: Int = wrapper.execute(sql, params)
    ```

- **接收参数**：

    {{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
    ])}}

- **返回值**：

    `Int` 受影响的行数

{{ $.hr() }}

## 7. {{ $.title("batchExecute") }} 批量执行SQL

批量执行SQL语句，返回受影响的行数。

- **函数声明**

    ```kotlin
    fun batchExecute(sql: String, params: Array<Map<String, Any?>>): IntArray
    ```

- **使用示例**

    ```kotlin
    val sql = "UPDATE table SET column = :value WHERE id = :id"
    val params = arrayOf(
        mapOf("value" to "some value", "id" to 1),
        mapOf("value" to "another value", "id" to 2)
    )
    val result: IntArray = wrapper.batchExecute(sql, params)
    ```

- **接收参数**：

    {{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数列表', 'Array<Map<String, Any?>>']
    ])}}

- **返回值**：

    `IntArray` 每组参数对应的受影响行数。

批量任务形态、命名参数解析和 insert/update 示例见 {{ $.keyword("mutation/batch-operations", ["批量操作"]) }}。
