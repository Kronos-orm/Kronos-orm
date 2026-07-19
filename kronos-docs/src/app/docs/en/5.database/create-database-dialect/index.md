{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Create Database Dialect

Add a database dialect through five entry points: database type resolution, SQL rendering, table-structure statements, runtime registration, and verification cases.

> **Note**
> For built-in dialects and user-visible behavior examples, see {{ $.keyword("database/dialect-support", ["Database Dialect Support"]) }}.

## 1. Resolve the Database Type

`KronosJdbcWrapper` reads JDBC metadata and resolves the runtime database type through `DBType.fromName(...)`.

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

## 2. Configure the SQL Dialect

When the target database can use the standard renderer shape, configure `SqlDialect` directly.

```kotlin group="SqlDialect 1" name="kotlin" icon="kotlin"
val YourDatabaseDialect = SqlDialect(
    leftQuote = "\"",
    rightQuote = "\"",
    standardEscapeStrings = true,
    limitStyle = SqlLimitStyle.LimitOffset,
    family = SqlDialectFamily.Standard
)
```

Check identifier quoting and pagination with a minimal query.

```kotlin group="SqlDialect 2" name="check" icon="kotlin"
val sql = SqlQuery.Select(
    from = listOf(SqlTable.Ident("user")),
    limit = SqlLimit.limit(limit = 10, offset = 20)
).toSql(YourDatabaseDialect)
```

```sql group="SqlDialect 2" name="sql"
SELECT * FROM "user" LIMIT 10 OFFSET 20
```

## 3. Add a Dedicated Renderer

When the target database needs dedicated syntax, add a `SqlDialectFamily` value and a renderer branch.

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

## 4. Read Table Metadata

`DatabaseStatements` provides table-existence, column, index, and comment metadata statements. The fragment below shows metadata queries and row mapping.

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

## 5. Generate DDL Statements

The same `YourDatabaseStatements` implementation handles table creation, drop, truncate, and schema sync.

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

## 6. Register the Dialect

`SqlManager.registerDatabase(...)` registers the SQL dialect and table-structure statements together.

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

## 7. Connect to the Database

After JDBC metadata maps to `DBType.YourDatabase`, `KronosJdbcWrapper` exposes the matching dialect.

```kotlin group="Connect" name="kotlin" icon="kotlin"
val wrapper = KronosJdbcWrapper(dataSource)

check(wrapper.dbType == DBType.YourDatabase)
check(wrapper.sqlDialect == YourDatabaseDialect)

Kronos.dataSource = { wrapper }
```

```text group="Connect" name="result"
wrapper.dbType == DBType.YourDatabase
wrapper.sqlDialect == YourDatabaseDialect
```

## 8. Verify Select

Start with a minimal select to verify identifier quoting, table names, and column names.

```kotlin group="Verify Select" name="kotlin" icon="kotlin"
val (sql, _) = User()
    .select { [it.id, it.name] }
    .build(wrapper)

check(sql == """SELECT "id", "name" FROM "user"""")
```

## 9. Verify Pagination

Pagination verification covers `limitStyle` and the renderer's `renderLimit(...)`.

```kotlin group="Verify Pagination" name="kotlin" icon="kotlin"
val (_, pageTask) = User()
    .select()
    .orderBy { it.id.asc() }
    .page(2, 20)
    .withTotal()
    .build(wrapper)

val (sql, _) = pageTask

check(sql.contains("LIMIT 20 OFFSET 20"))
```

## 10. Verify Upsert

Upsert verification covers conflict fields and update-field rendering.

```kotlin group="Verify Upsert" name="kotlin" icon="kotlin"
val (sql, _) = User(id = 1, name = "Ada")
    .upsert()
    .on { it.id }
    .set { [it.name] }
    .build(wrapper)

check(sql.contains("ON CONFLICT"))
```

## 11. Verify Schema Sync

Schema sync verification covers metadata queries, column diffs, index diffs, and DDL execution.

```kotlin group="Verify DDL" name="kotlin" icon="kotlin"
val created = wrapper.table.syncTable(User())

if (created) {
    wrapper.table.truncateTable(User())
}
```

```text group="Verify DDL" name="result"
created == false  // the table was missing and has been created
created == true   // the table existed and has been synced
```

## 12. Verify Functions

Function verification covers ordinary function names, database-specific function names, and expression arguments.

```kotlin group="Verify Function" name="kotlin" icon="kotlin"
val (sql, _) = User()
    .select { f.rand().alias("rand") }
    .build(wrapper)

check(sql.contains("RAND()") || sql.contains("RANDOM()"))
```

## 13. Update the Documentation Entry

After the dialect passes build and integration verification, add the database name to {{ $.keyword("database/dialect-support", ["Database Dialect Support"]) }}.

```markdown
| Your Database | `DBType.YourDatabase` |
```
