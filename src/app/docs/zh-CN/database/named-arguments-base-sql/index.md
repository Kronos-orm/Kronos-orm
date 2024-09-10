{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos提供了一个基础的SQL构建器，它允许您使用命名参数来构建SQL操作，本章将介绍如何使用该构建器执行SQL查询等操作。

## {{ $.title("query") }} 查询Map列表

`query`方法用于执行查询多行记录，返回一个`List<Map<String, Any>>`对象。

**参数**：

{{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
])}}

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: List<Map<String, Any>> = dataSource.query(sql, params)
```

## {{ $.title("queryList") }} 查询指定类型列表

`queryList`方法用于执行查询并返回指定类型列表，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryList<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryList<User>()`。

**参数**：

{{$.params([
    ['T', '<b>泛型参数</b>，查询结果类型', 'String'],
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
])}}

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: List<YourType> = dataSource.queryList(sql, params)
```

## {{ $.title("queryMap") }} 查询单行记录Map

用于查询单行记录，返回一个`Map<String, Any>`对象，若查询结果为空，返回`null`。

**参数**：

{{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
])}}

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: Map<String, Any>? = dataSource.queryMap(sql, params)
```

## {{ $.title("queryOne") }} 查询指定类型单行记录

`queryOne`方法用于执行查询并返回单条记录，当查询结果为空时，抛出异常，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOne<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOne<User>()`。

**参数**：

{{$.params([
    ['T', '<b>泛型参数</b>，查询结果类型', 'String'],
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
])}}

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: YourType = dataSource.queryOne(sql, params)
```

## {{ $.title("queryOneOrNull") }} 查询指定类型单行记录（可空）

`queryOneOrNull`方法用于执行查询并返回单条记录，当查询结果为空时，返回`null`，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOneOrNull<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOneOrNull<User>()`。

**参数**：

{{$.params([
    ['T', '<b>泛型参数</b>，查询结果类型', 'String'],
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
])}}

- `T` 查询结果类型
- `sql` `String` SQL查询语句
- `params` `Map<String, Any?>` 命名参数(可选)

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: YourType? = dataSource.queryOneOrNull(sql, params)
```

## {{ $.title("execute") }} 执行SQL

`execute`方法用于执行SQL语句，返回受影响的行数。

**参数**：

{{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数', 'Map<String, Any?>', 'emptyMap()']
])}}

```kotlin
val sql = "UPDATE table SET column = :value WHERE id = :id"
val params = mapOf("value" to "some value", "id" to 1)
val result: Int = dataSource.execute(sql, params)
```

## {{ $.title("batchExecute") }} 批量执行SQL

`batchExecute`方法用于批量执行SQL语句，返回受影响的行数。

**参数**：

{{$.params([
    ['sql', 'SQL查询语句', 'String'],
    ['params', '命名参数列表', 'Array<Map<String, Any?>>']
])}}

```kotlin
val sql = "UPDATE table SET column = :value WHERE id = :id"
val params = arrayOf(
    mapOf("value" to "some value", "id" to 1),
    mapOf("value" to "another value", "id" to 2)
)
val result: Array<Int> = dataSource.batchExecute(sql, params)
```
