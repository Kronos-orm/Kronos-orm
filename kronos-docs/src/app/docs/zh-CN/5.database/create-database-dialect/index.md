{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 创建数据库方言

添加数据库方言时，从数据库类型、SQL 渲染、表结构语句、运行时注册和验证用例五个入口完成。

> **Note**
> 内置方言和用户可见行为示例请参考{{ $.keyword("database/dialect-support", ["数据库方言支持"]) }}。

## 1. 识别数据库类型

`KronosJdbcWrapper` 会读取 JDBC metadata，并通过 `DBType.fromName(...)` 得到运行时数据库类型。

```kotlin group="DBType" name="kotlin" icon="kotlin"
enum class DBType {
    Mysql,
    Oracle,
    Postgres,
    Mssql,
    SQLite,
    YourDatabase,
    Unknown;

    companion object {
        fun fromName(name: String) =
            YourDatabase.takeIf { name.equals("Your Database", ignoreCase = true) }
                ?: Mssql.takeIf { name == "Microsoft SQL Server" }
                ?: Postgres.takeIf { name == "PostgreSQL" }
                ?: entries.first { it.name.uppercase() == name.uppercase() }
    }
}
```

```text group="DBType" name="result"
JDBC metadata databaseProductName = "Your Database"
DBType.fromName("Your Database") == DBType.YourDatabase
```

## 2. 配置 SQL 方言

目标数据库可以复用标准渲染规则时，直接配置 `SqlDialect`。

```kotlin group="SqlDialect 1" name="kotlin" icon="kotlin"
val YourDatabaseDialect = SqlDialect(
    leftQuote = "\"",
    rightQuote = "\"",
    standardEscapeStrings = true,
    limitStyle = SqlLimitStyle.LimitOffset,
    family = SqlDialectFamily.Standard
)
```

用一个最小查询检查引用符和分页。

```kotlin group="SqlDialect 2" name="check" icon="kotlin"
val sql = SqlQuery.Select(
    from = listOf(SqlTable.Ident("user")),
    limit = SqlLimit.limit(limit = 10, offset = 20)
).toSql(YourDatabaseDialect)
```

```sql group="SqlDialect 2" name="sql"
SELECT * FROM "user" LIMIT 10 OFFSET 20
```

## 3. 添加专属渲染器

目标数据库需要专属语法时，为它添加 `SqlDialectFamily` 和 renderer 分支。

```kotlin group="Renderer" name="family" icon="kotlin"
enum class SqlDialectFamily {
    Standard,
    MySql,
    PostgreSql,
    SQLite,
    Oracle,
    SqlServer,
    YourDatabase
}

val YourDatabaseDialect = SqlDialect(
    leftQuote = "\"",
    rightQuote = "\"",
    limitStyle = SqlLimitStyle.LimitOffset,
    family = SqlDialectFamily.YourDatabase
)
```

```kotlin group="Renderer" name="renderer" icon="kotlin"
class YourDatabaseSqlRenderer : StandardSqlRenderer(YourDatabaseDialect) {
    override fun renderLimit(limit: SqlLimit): String = buildString {
        limit.fetch?.let { append("LIMIT ${renderExpr(it.limit)}") }
        limit.offset?.let { append(" OFFSET ${renderExpr(it)}") }
    }
}

fun sqlRenderer(dialect: SqlDialect = SqlDialect.Standard): SqlRenderer = when (dialect.family) {
    SqlDialectFamily.YourDatabase -> YourDatabaseSqlRenderer()
    SqlDialectFamily.Standard -> StandardSqlRenderer(dialect)
    SqlDialectFamily.MySql -> MysqlSqlRenderer(standardEscapeStrings = dialect.standardEscapeStrings)
    SqlDialectFamily.PostgreSql -> PostgresqlSqlRenderer()
    SqlDialectFamily.SQLite -> SqliteSqlRenderer()
    SqlDialectFamily.Oracle -> OracleSqlRenderer()
    SqlDialectFamily.SqlServer -> SqlServerSqlRenderer()
}
```

```kotlin group="Renderer" name="check" icon="kotlin"
val sql = SqlQuery.Select(
    from = listOf(SqlTable.Ident("user")),
    limit = SqlLimit.limit(limit = 5, offset = 10)
).toSql(YourDatabaseDialect)
```

```sql group="Renderer" name="sql"
SELECT * FROM "user" LIMIT 5 OFFSET 10
```

## 4. 读取表结构

`DatabaseStatements` 提供表存在性、字段、索引和注释读取语句。下面片段展示 metadata 查询和结果映射方式。

```kotlin group="Metadata" name="kotlin" icon="kotlin"
override fun databaseName(wrapper: KronosDataSourceWrapper): String =
    wrapper.url.substringBefore("?").substringAfterLast("/")

override fun tableExists(): SqlQuery = SqlQuery.Select(
    select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("COUNT(*)"))),
    from = listOf(
        SqlTable.Ident(
            "information_schema.tables",
            identifier = SqlIdentifier.of("information_schema", "tables")
        )
    ),
    where = SqlExpr.UnsafeRaw("table_schema = :dbName AND table_name = :tableName")
)

override fun tableColumns(tableName: String): SqlQuery = SqlQuery.Select(
    select = listOf(
        SqlSelectItem.Expr(SqlExpr.UnsafeRaw("column_name AS COLUMN_NAME")),
        SqlSelectItem.Expr(SqlExpr.UnsafeRaw("data_type AS DATA_TYPE")),
        SqlSelectItem.Expr(SqlExpr.UnsafeRaw("character_maximum_length AS LENGTH")),
        SqlSelectItem.Expr(SqlExpr.UnsafeRaw("is_nullable AS IS_NULLABLE"))
    ),
    from = listOf(
        SqlTable.Ident(
            "information_schema.columns",
            identifier = SqlIdentifier.of("information_schema", "columns")
        )
    ),
    where = SqlExpr.UnsafeRaw("table_schema = :dbName AND table_name = :tableName")
)

override fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field> =
    rows.map {
        Field(
            columnName = it.cell("COLUMN_NAME").toString(),
            type = KColumnType.fromString(it.cell("DATA_TYPE").toString()),
            length = it.cell("LENGTH").asInt(),
            tableName = tableName,
            nullable = it.cell("IS_NULLABLE") == "YES"
        )
    }

override fun tableIndexes(tableName: String): SqlQuery = SqlQuery.Select(
    select = listOf(
        SqlSelectItem.Expr(SqlExpr.UnsafeRaw("index_name AS indexName")),
        SqlSelectItem.Expr(SqlExpr.UnsafeRaw("column_name AS columnName"))
    ),
    from = listOf(
        SqlTable.Ident(
            "information_schema.statistics",
            identifier = SqlIdentifier.of("information_schema", "statistics")
        )
    ),
    where = SqlExpr.UnsafeRaw("table_schema = :dbName AND table_name = :tableName")
)

override fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex> =
    rows.groupBy { it.cell("indexName").toString() }.map { (name, columns) ->
        KTableIndex(
            name = name,
            columns = columns.map { it.cell("columnName").toString() }.toTypedArray()
        )
    }
```

```sql group="Metadata" name="sql"
SELECT COUNT(*)
FROM "information_schema"."tables"
WHERE table_schema = :dbName AND table_name = :tableName
```

## 5. 生成 DDL 语句

同一个 `YourDatabaseStatements` 继续负责建表、删表、清表和表结构同步。

```kotlin group="DDL" name="kotlin" icon="kotlin"
override fun createTable(input: DatabaseCreateTable): List<SqlStatement> {
    val table = SqlIdentifier.of(input.tableName)
    return listOf(
        SqlDdlStatement.CreateTable(
            tableName = table,
            columns = input.columns.map { it.toColumnDefinition(::columnType) },
            comment = input.tableComment,
            ifNotExists = true
        )
    ) + input.indexes.map { it.toCreateIndexStatement(table) }
}

override fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement> =
    listOf(SqlDdlStatement.DropTable(SqlIdentifier.of(tableName), ifExists))

override fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement> =
    listOf(SqlDmlStatement.Truncate(SqlTable.Ident(tableName), restartIdentity))

override fun syncTable(input: DatabaseSyncTable): List<SqlStatement> {
    val table = SqlIdentifier.of(input.tableName)
    return buildList {
        addAll(input.columns.toAdd.map { (column, _) ->
            SqlDdlStatement.AlterTable.AddColumn(table, column.toColumnDefinition(::columnType))
        })
        addAll(input.columns.toModified.map { (column, _, _) ->
            SqlDdlStatement.AlterTable.ModifyColumn(table, column.toColumnDefinition(::columnType))
        })
        addAll(input.columns.toDelete.map {
            SqlDdlStatement.AlterTable.DropColumn(table, SqlIdentifier.of(it.columnName))
        })
        addAll(input.indexes.toDelete.map { SqlDdlStatement.DropIndex(SqlIdentifier.of(it.name), table) })
        addAll(input.indexes.toAdd.map { it.toCreateIndexStatement(table) })
    }
}

private fun columnType(type: KColumnType, length: Int, scale: Int): String = when (type) {
    KColumnType.INT -> "INTEGER"
    KColumnType.VARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
    KColumnType.DATETIME -> "TIMESTAMP"
    else -> "VARCHAR(255)"
}
```

```sql group="DDL" name="sql"
CREATE TABLE IF NOT EXISTS "account" (
    "id" INTEGER NOT NULL PRIMARY KEY,
    "name" VARCHAR(64)
)
```

## 6. 注册方言

`SqlManager.registerDatabase(...)` 同时注册 SQL 方言和表结构语句。

```kotlin group="Register" name="kotlin" icon="kotlin"
SqlManager.registerDatabase(
    DBType.YourDatabase,
    YourDatabaseDialect,
    YourDatabaseStatements
)
```

```kotlin group="Register" name="check" icon="kotlin"
check(SqlManager.dialectOf(DBType.YourDatabase) == YourDatabaseDialect)
check(SqlManager.statementsOf(DBType.YourDatabase) == YourDatabaseStatements)
```

## 7. 连接数据库

JDBC metadata 能映射到 `DBType.YourDatabase` 后，`KronosJdbcWrapper` 会带出对应方言。

```kotlin group="Connect" name="kotlin" icon="kotlin"
val wrapper = KronosJdbcWrapper(dataSource)

check(wrapper.dbType == DBType.YourDatabase)
check(wrapper.sqlDialect == YourDatabaseDialect)

with(Kronos) {
    dataSource = { wrapper }
}
```

```text group="Connect" name="result"
wrapper.dbType == DBType.YourDatabase
wrapper.sqlDialect == YourDatabaseDialect
```

## 8. 验证查询

先验证最小 select，确认引用符、表名和字段名正确。

```kotlin group="Verify Select" name="kotlin" icon="kotlin"
val (sql, _) = User()
    .select { [it.id, it.name] }
    .build(wrapper)

check(sql == """SELECT "id", "name" FROM "user"""")
```

## 9. 验证分页

分页验证覆盖 `limitStyle` 和 renderer 的 `renderLimit(...)`。

```kotlin group="Verify Pagination" name="kotlin" icon="kotlin"
val (_, pageTask) = User()
    .select()
    .orderBy { it.id.asc() }
    .withTotal()
    .page(2, 20)
    .build(wrapper)

val (sql, _) = pageTask

check(sql.contains("LIMIT 20 OFFSET 20"))
```

## 10. 验证 Upsert

upsert 验证覆盖冲突字段和更新字段渲染。

```kotlin group="Verify Upsert" name="kotlin" icon="kotlin"
val (sql, _) = User(id = 1, name = "Ada")
    .upsert()
    .on { it.id }
    .set { [it.name] }
    .build(wrapper)

check(sql.contains("ON CONFLICT"))
```

## 11. 验证表结构同步

表结构同步验证覆盖 metadata 查询、字段差异、索引差异和 DDL 执行。

```kotlin group="Verify DDL" name="kotlin" icon="kotlin"
val created = wrapper.table.syncTable(User())

if (created) {
    wrapper.table.truncateTable(User())
}
```

```text group="Verify DDL" name="result"
created == false  // 表不存在，已创建表
created == true   // 表已存在，已执行同步
```

## 12. 验证函数

函数验证覆盖普通函数名、数据库专属函数名和表达式参数。

```kotlin group="Verify Function" name="kotlin" icon="kotlin"
val (sql, _) = User()
    .select { f.rand().alias("rand") }
    .build(wrapper)

check(sql.contains("RAND()") || sql.contains("RANDOM()"))
```

## 13. 更新文档入口

方言通过构建和集成验证后，把数据库名称加入{{ $.keyword("database/dialect-support", ["数据库方言支持"]) }}页面。

```markdown
| Your Database | `DBType.YourDatabase` |
```
