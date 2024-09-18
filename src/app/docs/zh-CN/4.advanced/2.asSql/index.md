{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`AsSql`是Kronos定义的SQL转换器，用于将Kotlin原生表达式转换为SQL语句。

## 基础用法

在`where`、`having`、`on`等语句中，可以使用`asSql`方法将`String`类型的自定义SQL语句插入到Kronos的条件语句构建中：

具体使用方法可参考 {{ 
$.keyword("database/where-having-on-clause", ["Criteria", "(String/Boolean).asSql 自定义SQL查询条件"]) }}

通过`asSql()`，可以实现复杂条件查询的自定义SQL语句。

## 进阶用法

以下是一个简单的示例，用于解释`asSql`可以带来的查询性能提升：

```kotlin
User(id = 1).select().where { it.id == 1 }.queryOne()
```
这将会生成如下的SQL语句：

```sql group="Case 1" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age`
FROM `user`
WHERE `id` = 1
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = 1
```

```sql group="Case 1" name="SQLite" icon="sqlite"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = 1
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age]
FROM [user]
WHERE [id] = 1
```

```sql group="Case 1" name="Oracle" icon="oracle"
SELECT "id", "name", "age"
FROM "user"
WHERE "id" = 1
```

实际上，我们可以为`it.id == 1`传入`asSql()`方法，从而大大提升查询性能。

```kotlin
val search = User(id = 100)
```