{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Built-in Database Dialects

Kronos chooses the SQL dialect and table-structure statements from `KronosDataSourceWrapper.dbType`.

```kotlin
val wrapper = KronosJdbcWrapper(dataSource)

Kronos.dataSource = { wrapper }

println(wrapper.dbType)
println(wrapper.sqlDialect)
```

The complete built-in dialects map to these `DBType` values:

| Database | `DBType` |
|----------|----------|
| MySQL | `DBType.Mysql` |
| PostgreSQL | `DBType.Postgres` |
| SQLite | `DBType.SQLite` |
| SQL Server | `DBType.Mssql` |
| Oracle | `DBType.Oracle` |

> **Note**
> For database connection and JDBC driver examples, see {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }}.
> For table DDL workflows, see {{ $.keyword("database/schema-sync", ["Schema Sync"]) }} and {{ $.keyword("database/create-table-as-select", ["Create Table As Select"]) }}.

## Behavior matrix

Use this page as the dialect behavior matrix. Each row links to the section or page that shows the Kotlin entry point and the SQL shape for the built-in dialects.

| Behavior | Where to check |
|----------|----------------|
| Pagination | See [Pagination](#pagination) for `page(pageIndex, pageSize)` SQL in MySQL, PostgreSQL, SQLite, SQL Server, and Oracle. |
| Upsert and `onConflict()` | See [Upsert](#upsert) and {{ $.keyword("mutation/upsert", ["Upsert"]) }} for match-field upsert, uniqueness-conflict upsert, and dynamic conflict assignments. |
| Last insert id | See [Last Insert Id](#last-insert-id) and {{ $.keyword("mutation/last-insert-id", ["Last Insert Id"]) }} for identity key retrieval. |
| DDL and schema sync | See [Table Operations](#table-operations), {{ $.keyword("database/table-operations", ["Table Operations"]) }}, and {{ $.keyword("database/schema-sync", ["Schema Sync"]) }}. |
| Column type rendering | See {{ $.keyword("mapping/column-types", ["Column Types"]) }} for `KColumnType` examples and dialect output. |
| Identifier quoting | See [Identifier Quoting](#identifier-quoting) for table and column quoting rules. |
| Functions and windows | See [Built-in Functions](#built-in-functions), {{ $.keyword("query/functions", ["Functions"]) }}, and {{ $.keyword("query/sorting-pagination-aggregation", ["Sorting, Pagination, and Aggregation"]) }}. |

## Identifier Quoting

The same query uses the identifier quoting rules of the active dialect.

```kotlin group="Identifier Quote" name="kotlin" icon="kotlin"
val users = User()
    .select { [it.id, it.name] }
    .toList()
```

```sql group="Identifier Quote" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
```

```sql group="Identifier Quote" name="PostgreSQL" icon="postgres"
SELECT "id", "name"
FROM "user"
```

```sql group="Identifier Quote" name="SQLite" icon="sqlite"
SELECT "id", "name"
FROM "user"
```

```sql group="Identifier Quote" name="SQLServer" icon="sqlserver"
SELECT [id], [name]
FROM [user]
```

```sql group="Identifier Quote" name="Oracle" icon="oracle"
SELECT "ID", "NAME"
FROM "USER"
```

## Pagination

`page(pageIndex, pageSize)` starts from page 1. Add `withTotal()` after `page(...)` when a named `PageResult` with total metadata is required. Kronos renders the matching pagination syntax for the active database.

```kotlin group="Pagination" name="kotlin" icon="kotlin"
val page = User()
    .select { [it.id, it.name] }
    .orderBy { it.id.asc() }
    .page(2, 20)
    .withTotal()
    .toList()

val users = page.records
```

```sql group="Pagination" name="Mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
ORDER BY `id` ASC
LIMIT 20 OFFSET 20
```

```sql group="Pagination" name="PostgreSQL" icon="postgres"
SELECT "id", "name"
FROM "user"
ORDER BY "id" ASC
LIMIT 20 OFFSET 20
```

```sql group="Pagination" name="SQLite" icon="sqlite"
SELECT "id", "name"
FROM "user"
ORDER BY "id" ASC
LIMIT 20 OFFSET 20
```

```sql group="Pagination" name="SQLServer" icon="sqlserver"
SELECT [id], [name]
FROM [user]
ORDER BY [id] ASC
OFFSET 20 ROWS FETCH NEXT 20 ROWS ONLY
```

```sql group="Pagination" name="Oracle" icon="oracle"
SELECT "ID", "NAME"
FROM "USER"
ORDER BY "ID" ASC
OFFSET 20 ROWS FETCH NEXT 20 ROWS ONLY
```

## Upsert

`onConflict()` uses database uniqueness conflicts and renders the matching SQL for the active dialect. When `on { ... }` is omitted, Kronos infers the conflict target from KPojo uniqueness metadata: primary-key values and declared unique indexes.

```kotlin group="Upsert" name="kotlin" icon="kotlin"
User(id = 1, name = "Ada")
    .upsert { it.name }
    .onConflict()
    .execute()
```

```sql group="Upsert" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`) VALUES (:id, :name)
ON DUPLICATE KEY UPDATE `name` = :name
```

```sql group="Upsert" name="PostgreSQL" icon="postgres"
INSERT INTO "user" ("id", "name") VALUES (:id, :name)
ON CONFLICT ("id") DO UPDATE SET "name" = :name
```

```sql group="Upsert" name="SQLite" icon="sqlite"
INSERT INTO "user" ("id", "name") VALUES (:id, :name)
ON CONFLICT ("id") DO UPDATE SET "name" = :name
```

```sql group="Upsert" name="SQLServer" icon="sqlserver"
MERGE INTO [user] AS [t1]
USING (SELECT :id AS [id], :name AS [name]) AS [t2]
ON ([t1].[id] = [t2].[id])
WHEN MATCHED THEN UPDATE SET [t1].[name] = :name
WHEN NOT MATCHED THEN INSERT ([id], [name]) VALUES (:id, :name)
```

```sql group="Upsert" name="Oracle" icon="oracle"
MERGE INTO "USER" "T1"
USING (SELECT :id AS "ID", :name AS "NAME") "T2"
ON ("T1"."ID" = "T2"."ID")
WHEN MATCHED THEN UPDATE SET "T1"."NAME" = :name
WHEN NOT MATCHED THEN INSERT ("ID", "NAME") VALUES (:id, :name)
```

## Last Insert Id

An insert on an identity primary key can read `lastInsertId` when the insert uses `.withId()`. The built-in `KronosJdbcWrapper` reads JDBC generated keys during insert execution; wrappers that use a follow-up query use the dialect SQL below.

```kotlin group="Last Insert Id" name="kotlin" icon="kotlin"
val id = User(name = "Kronos")
    .insert()
    .withId()
    .execute()
    .lastInsertId
```

```sql group="Last Insert Id" name="Mysql" icon="mysql"
SELECT LAST_INSERT_ID()
```

```sql group="Last Insert Id" name="PostgreSQL" icon="postgres"
SELECT LASTVAL()
```

```sql group="Last Insert Id" name="SQLite" icon="sqlite"
SELECT last_insert_rowid()
```

```sql group="Last Insert Id" name="SQLServer" icon="sqlserver"
SELECT SCOPE_IDENTITY()
```

```sql group="Last Insert Id" name="Oracle" icon="oracle"
SELECT MAX("ID") FROM "USER"
```

## Table Operations

`table.createTable(...)`, `table.syncTable(...)`, `table.dropTable(...)`, and `table.truncateTable(...)` use the active database's `DatabaseStatements`.

```kotlin group="DDL" name="kotlin" icon="kotlin"
val table = Kronos.dataSource.table

table.createTable(Account())
table.syncTable(Account())
table.truncateTable(Account())
table.dropTable(Account())
```

```sql group="DDL" name="Mysql" icon="mysql"
CREATE TABLE `account` (`id` SIGNED NOT NULL, `name` CHAR(64), `created_at` DATETIME)
```

```sql group="DDL" name="PostgreSQL" icon="postgres"
CREATE TABLE "account" ("id" INTEGER NOT NULL, "name" VARCHAR(64), "created_at" TIMESTAMP)
```

```sql group="DDL" name="SQLite" icon="sqlite"
CREATE TABLE "account" ("id" INTEGER NOT NULL, "name" VARCHAR(64), "created_at" TIMESTAMP)
```

```sql group="DDL" name="SQLServer" icon="sqlserver"
CREATE TABLE [account] ([name] NVARCHAR(32), [created_at] DATETIME2)
```

```sql group="DDL" name="Oracle" icon="oracle"
CREATE TABLE "ACCOUNT" ("NAME" VARCHAR(4000), "SCORE" INT)
```

Schema sync reads the current table structure first, then executes the required DDL.

```kotlin group="Schema Sync" name="kotlin" icon="kotlin"
val changed = Kronos.dataSource.table.syncTable(User())
```

```text group="Schema Sync" name="result"
changed == false  // Kronos creates the target table
changed == true   // Kronos synchronizes the existing table by diff
```

## Built-in Functions

The function DSL keeps the same call shape, and database-specific functions are rendered by the active dialect.

```kotlin group="Functions" name="kotlin" icon="kotlin"
val rows = User()
    .select { f.rand().alias("rand") }
    .toList()
```

```sql group="Functions" name="Mysql" icon="mysql"
SELECT RAND() AS rand
FROM `tb_user`
```

```sql group="Functions" name="PostgreSQL" icon="postgres"
SELECT RANDOM() AS rand
FROM "tb_user"
```

```sql group="Functions" name="SQLite" icon="sqlite"
SELECT RANDOM() AS rand
FROM "tb_user"
```

```sql group="Functions" name="SQLServer" icon="sqlserver"
SELECT RAND() AS rand
FROM [tb_user]
```

```sql group="Functions" name="Oracle" icon="oracle"
SELECT DBMS_RANDOM.VALUE AS RAND
FROM "TB_USER"
```

## Database Enum Values and Extension

`DBType` also contains DB2, Sybase, H2, OceanBase, DM8, and GaussDB for connection identification and dialect extension entry points.

```kotlin
val dbType = DBType.fromName(connection.metaData.databaseProductName)
```

For adding a new database dialect, see {{ $.keyword("database/create-database-dialect", ["Create Database Dialect"]) }}.

For custom database connections, see {{ $.keyword("database/datasource-wrapper", ["concept", "data-source-wrapper"]) }}.
