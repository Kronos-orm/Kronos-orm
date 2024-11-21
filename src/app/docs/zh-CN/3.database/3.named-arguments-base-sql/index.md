{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 1. {{ $.title("query") }} 查询Map列表

执行SQL语句，查询多行记录

- **函数声明**

```kotlin
fun query(sql: String, params: Map<String, Any?> = emptyMap()): List<Map<String, Any>>
```

- **使用示例**

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: List<Map<String, Any>> = wrapper.query(sql, params)
```

- **接收参数**：

{{$.params([
['sql', 'SQL查询语句', 'String'],
['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
])}}

- **返回值**：

`List<Map<String, Any>>` 查询结果列表

{{ $.hr() }}

## 2. {{ $.title("queryList") }} 查询指定类型列表

- **泛型参数**： `<T>` 查询结果类型

{{ $.hr(50) }}

执行SQL语句，查询多行记录并返回指定类型列表

> **Note**
> 查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryList<Int>()`
>
> 查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryList<User>()`

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

{{ $.hr() }}

## 3. {{ $.title("queryMap") }} 查询单行记录Map

执行SQL语句，查询单行记录

- **函数声明**

```kotlin
fun queryMap(sql: String, params: Map<String, Any?> = emptyMap()): Map<String, Any>?
```

- **使用示例**

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: Map<String, Any>? = wrapper.queryMap(sql, params)
```

- **接收参数**：

{{$.params([
['sql', 'SQL查询语句', 'String'],
['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
])}}

- **返回值**：

`Map<String, Any>?` 查询结果Map

{{ $.hr() }}

## 4. {{ $.title("queryOne") }} 查询指定类型单行记录

- **泛型参数**： `<T>` 查询结果类型

{{ $.hr(50) }}

执行SQL语句，查询单行记录返回指定类型，当查询结果为空时，抛出异常。

> **Note**
> 查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOne<Int>()`
>
> 查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOne<User>()`

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
fun batchExecute(sql: String, params: Array<Map<String, Any?>>): Array<Int>
```

- **使用示例**

```kotlin
val sql = "UPDATE table SET column = :value WHERE id = :id"
val params = arrayOf(
    mapOf("value" to "some value", "id" to 1),
    mapOf("value" to "another value", "id" to 2)
)
val result: Array<Int> = wrapper.batchExecute(sql, params)
```

- **接收参数**：

{{$.params([
['sql', 'SQL查询语句', 'String'],
['params', '命名参数列表', 'Array<Map<String, Any?>>']
])}}

- **返回值**：

`Array<Int>` 受影响的行数数组
