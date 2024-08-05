# {{ NgDocPage.title }}

Kronos提供了一个基础的SQL查询构建器，它允许您使用命名参数来构建SQL查询。这个构建器是一个简单的构建器，它允许您构建一个SQL查询，然后使用命名参数来填充查询中的参数。

## 使用<span style="color: #DD6666">query</span>查询Map列表

`KronosDataSourceWrapper.query()`

- `sql` `String` SQL查询语句
- `params` `Map<String, Any?>` 命名参数(可选)

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: List<Map<String, Any>> = dataSource.query(sql, params)
```

## 使用<span style="color: #DD6666">queryList</span>查询List

`queryList`方法用于执行查询并返回指定类型列表，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryList<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryList<User>()`。

`KronosDataSourceWrapper.queryList()`

- `T` 查询结果类型
- `sql` `String` SQL查询语句
- `params` `Map<String, Any?>` 命名参数(可选)

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: List<YourType> = dataSource.queryList(sql, params)
```

## 使用<span style="color: #DD6666">queryMap</span>查询Map

若查询结果只有一行，可以使用`queryMap`方法，返回一个`Map<String, Any>`对象。

若查询结果为空，返回`null`。

`KronosDataSourceWrapper.queryMap()`

- `sql` `String` SQL查询语句
- `params` `Map<String, Any?>` 命名参数(可选)

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: Map<String, Any>? = dataSource.queryMap(sql, params)
```

## 使用<span style="color: #DD6666">queryOne</span>查询单条记录

`queryOne`方法用于执行查询并返回单条记录，当查询结果为空时，抛出异常，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOne<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOne<User>()`。

`KronosDataSourceWrapper.queryOne()`

- `T` 查询结果类型
- `sql` `String` SQL查询语句
- `params` `Map<String, Any?>` 命名参数(可选)

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: YourType = dataSource.queryOne(sql, params)
```

## 使用<span style="color: #DD6666">queryOneOrNull</span>查询单条记录(可空)

`queryOneOrNull`方法用于执行查询并返回单条记录，当查询结果为空时，返回`null`，可以接收泛型参数。

当查询单列时，可以直接将泛型参数设置为列的类型，例如：`queryOneOrNull<Int>()`。

查询多列时，可以将泛型参数设置为KPojo的子类，例如：`queryOneOrNull<User>()`。

`KronosDataSourceWrapper.queryOneOrNull()`

- `T` 查询结果类型
- `sql` `String` SQL查询语句
- `params` `Map<String, Any?>` 命名参数(可选)

```kotlin
val sql = "SELECT * FROM table WHERE column = :value"
val params = mapOf("value" to "some value")
val result: YourType? = dataSource.queryOneOrNull(sql, params)
```

## 使用<span style="color: #DD6666">execute</span>执行SQL

`execute`方法用于执行SQL语句，返回受影响的行数。

`KronosDataSourceWrapper.execute()`

- `sql` `String` SQL查询语句
- `params` `Map<String, Any?>` 命名参数(可选)

```kotlin
val sql = "UPDATE table SET column = :value WHERE id = :id"
val params = mapOf("value" to "some value", "id" to 1)
val result: Int = dataSource.execute(sql, params)
```

## 使用<span style="color: #DD6666">batchExecute</span>批量执行SQL

`batchExecute`方法用于批量执行SQL语句，返回受影响的行数。

`KronosDataSourceWrapper.batchExecute()`

- `sql` `String` SQL查询语句
- `params` `Array<Map<String, Any?>>` 命名参数列表

```kotlin
val sql = "UPDATE table SET column = :value WHERE id = :id"
val params = arrayOf(
    mapOf("value" to "some value", "id" to 1),
    mapOf("value" to "another value", "id" to 2)
)
val result: Array<Int> = dataSource.batchExecute(sql, params)
```
