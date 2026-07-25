{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 内置数据库方言

Kronos 会通过 `KronosDataSourceWrapper.dbType` 选择 SQL 方言和表结构语句。

```kotlin
val wrapper = KronosJdbcWrapper(dataSource)

Kronos.dataSource = { wrapper }

println(wrapper.dbType)
println(wrapper.sqlDialect)
```

内置完整方言对应以下 `DBType`：

| 数据库 | `DBType` |
|--------|----------|
| MySQL | `DBType.Mysql` |
| PostgreSQL | `DBType.Postgres` |
| SQLite | `DBType.SQLite` |
| H2 | `DBType.H2` |
| SQL Server | `DBType.Mssql` |
| Oracle | `DBType.Oracle` |
| DM8（达梦） | `DBType.DM8` |

> **Note**
> 连接数据库和 JDBC driver 示例请参考{{ $.keyword("database/connect-to-db", ["连接到数据库"]) }}。
> 表 DDL 工作流请参考{{ $.keyword("database/schema-sync", ["表结构同步"]) }}和{{ $.keyword("database/create-table-as-select", ["基于查询创建表"]) }}。

## 方言行为矩阵

本页作为方言行为矩阵入口。每一行都指向包含 Kotlin 入口和内置方言 SQL 形态的章节或页面。

| 行为 | 查阅位置 |
|------|----------|
| 分页 | 见[分页](#分页)，查看 `page(pageIndex, pageSize)` 生成的 SQL。 |
| Upsert 和 `onConflict()` | 见 [Upsert](#upsert) 与 {{ $.keyword("mutation/upsert", ["Upsert"]) }}，查看按匹配字段 upsert、唯一约束冲突 upsert 和动态冲突赋值。 |
| 自增主键读取 | 见 [Last Insert Id](#last-insert-id) 与 {{ $.keyword("mutation/last-insert-id", ["Last Insert Id"]) }}。 |
| H2 和 DM8 | 见下方独立章节，了解 H2 `MERGE` upsert、DM8 自增列和 JDBC generated key。 |
| DDL 和表结构同步 | 见[表结构操作](#表结构操作)、{{ $.keyword("database/table-operations", ["表操作"]) }}和{{ $.keyword("database/schema-sync", ["表结构同步"]) }}。 |
| 字段类型渲染 | 见 {{ $.keyword("mapping/column-types", ["字段类型"]) }} 中的 `KColumnType` 示例和方言输出。 |
| 标识符引用 | 见[标识符引用](#标识符引用)，查看表名和字段名引用规则。 |
| 函数和窗口函数 | 见[内置函数](#内置函数)、{{ $.keyword("query/functions", ["函数"]) }}和{{ $.keyword("query/sorting-pagination-aggregation", ["排序、分页和聚合"]) }}。 |

## 标识符引用

同一段查询会按当前方言引用表名和字段名。

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

## 分页

`page(pageIndex, pageSize)` 的页码从 1 开始。需要总数元数据时，在 `page(...)` 后调用 `withTotal()`，得到命名的 `PageResult`。Kronos 会把相同 DSL 渲染为当前数据库的分页语法。

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

`onConflict()` 按数据库唯一约束冲突处理更新，并按当前方言渲染对应 SQL。省略 `on { ... }` 时，Kronos 会根据 KPojo 唯一性元数据推导冲突目标，包括主键值和已声明的唯一索引。

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

自增主键 insert 调用 `.withId()` 后可以读取 `lastInsertId`。内置 `KronosJdbcWrapper` 会在 insert 执行时读取 JDBC generated keys；使用后续查询读取生成 ID 的 wrapper 会使用下面的方言 SQL。

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

## 表结构操作

`table.createTable(...)`、`table.syncTable(...)`、`table.dropTable(...)` 和 `table.truncateTable(...)` 会使用当前数据库的 `DatabaseStatements`。

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

表结构同步会先读取当前表结构，再生成需要执行的 DDL。

```kotlin group="Schema Sync" name="kotlin" icon="kotlin"
val changed = Kronos.dataSource.table.syncTable(User())
```

```text group="Schema Sync" name="result"
changed == false  // Kronos 创建目标表
changed == true   // Kronos 按差异同步已有表
```

## 内置函数

函数 DSL 保持相同的 Kotlin 调用方式，由当前方言选择对应 SQL 形式。函数可用范围和参数语义见{{ $.keyword("query/functions", ["函数"]) }}。

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
SELECT (RANDOM() / 9223372036854775808.0 + 0.5) AS rand
FROM "tb_user"
```

```sql group="Functions" name="H2" icon="h2"
SELECT RAND() AS rand
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

```sql group="Functions" name="DM8"
SELECT DBMS_RANDOM.VALUE AS RAND
FROM "TB_USER"
```

## H2

H2 使用双引号引用标识符，并通过 H2 `MERGE` 语法处理唯一约束冲突的 upsert。内存库和服务端连接配置见{{ $.keyword("database/connect-to-db", ["连接到数据库"]) }}。

```kotlin group="H2 Upsert" name="kotlin" icon="kotlin"
User(id = 1, name = "Ada")
    .upsert { it.name }
    .onConflict()
    .execute()
```

```sql group="H2 Upsert" name="H2" icon="h2"
MERGE INTO "user" AS "t1"
USING (VALUES (:id, :name)) AS "t2" ("id", "name")
ON ("t1"."id" = "t2"."id")
WHEN MATCHED THEN UPDATE SET "name" = "t2"."name"
WHEN NOT MATCHED THEN INSERT ("id", "name") VALUES ("t2"."id", "t2"."name")
```

H2 自增字段使用 H2 的 identity 列定义。带 `.withId()` 的插入任务会返回 JDBC generated key。

## DM8（达梦）

DM8 的查询使用 Oracle 兼容语法，表结构使用 DM8 原生自增列。JDBC Driver 配置见{{ $.keyword("database/connect-to-db", ["连接到数据库"]) }}。

```kotlin group="DM8 Identity" name="kotlin" icon="kotlin"
@PrimaryKey(identity = true)
var id: Int? = null
```

Kronos 会将该字段渲染为 DM8 DDL 中的 `INT IDENTITY(1,1)`。带 `.withId()` 的插入任务会返回 JDBC generated key。

## 数据库枚举与扩展

`DBType` 还包含 DB2、Sybase、OceanBase、GaussDB 等枚举值，用于连接识别和方言扩展入口。

```kotlin
val dbType = DBType.fromName(connection.metaData.databaseProductName)
```

添加新的数据库方言请参考{{ $.keyword("database/create-database-dialect", ["创建数据库方言"]) }}。

自定义数据库连接请参考{{ $.keyword("database/datasource-wrapper", ["概念", "数据源包装器"]) }}。
