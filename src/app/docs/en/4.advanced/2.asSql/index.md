{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

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

实际上，我们还可以在一个多case的sql场景下使用`asSql`来代替`if`或者`when`，从而可以将多种case整合进一个sql语句中。

比如以下场景：需要根据传入的`case`变量为0-3的不同值，来查询`age`分别位于0-18、19-35、36-60以及60以上的用户。
常规的做法是使用`if`或者`when`来判断`case`的值，然后分别构建不同的sql语句，如：

```kotlin
when (case) {
    0 -> User.select().where { it.age between 0..18 }.queryList()
    1 -> User.select().where { it.age between 19..35 }.queryList()
    2 -> User.select().where { it.age between 36..60 }.queryList()
    3 -> User.select().where { it.age >= 60 }.queryList()
    else -> throw IllegalArgumentException("Invalid case value")
}
```

而使用`asSql`，我们可以将这四种情况整合进一个sql语句中，使用短路逻辑来实现，如：

```kotlin
User(id = 1).select().where { 
    ((0 == case).asSql() && it.age between 0..18) ||
    ((1 == case).asSql() && it.age between 19..35) ||
    ((2 == case).asSql() && it.age between 36..60) ||
    ((3 == case).asSql() && it.age >= 60)
}.queryList()
```