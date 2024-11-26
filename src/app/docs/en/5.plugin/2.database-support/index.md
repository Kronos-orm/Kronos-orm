{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Types of databases already supported

Kronos supports the creation of plug-ins to realize the expansion of database types, currently supported database types are as follows:

- [MySQL](https://www.mysql.com/)
- [PostgreSQL](https://www.postgresql.org/)
- [Mssql](https://www.microsoft.com/sql-server)
- [SQLite](https://www.sqlite.org/)
- [Oracle](https://www.oracle.com/database/technologies/)

## Support for extended database types

No official support has been provided yet, but the database type can be implemented by creating a plugin as follows:

- [<span class="code-red">DB2</span>](https://www.ibm.com/db2)
- [<span class="code-red">Sybase</span>](https://www.sap.com/)
- [<span class="code-red">H2</span>](https://www.h2database.com/)
- [<span class="code-red">OceanBase</span>](https://www.oceanbase.com/)
- [<span class="code-red">DM8</span>](https://www.dameng.com/DM8.html)
- [<span class="code-red">GaussDB</span>](https://www.huaweicloud.com/product/gaussdb.html)

If you have other database type requirements, you can submit a PR to us, and we will add the enumeration class for that database as soon as possible.

## How to implement database support

### 1. Get database type enum class

Typically, the database type enumeration class is provided by Kronos, and you can get all supported database types through the `DBType` class.

```kotlin
val mysql = DBType.Mysql
val postgresql = DBType.Postgres
val oceanBase = DBType.OceanBase
```

### 2. Creating Database Type Implementation Classes

Create an `object` or `class` that inherits the [DatabaseSupport](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-core/src/main/kotlin/com/ kotlinorm/interfaces/DatabasesSupport.kt) interface and implement its methods, e.g. `getDBNameFromUrl`, `getColumnCreateSql`, `getIndexCreateSql`, etc.

The following are the officially supported database type implementation classes:

- [MysqlSupport](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-core/src/main/kotlin/com/kotlinorm/database/mysql/MysqlSupport.kt)
- [PostgresqlSupport](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-core/src/main/kotlin/com/kotlinorm/database/postgres/PostgresqlSupport.kt)
- [MssqlSupport](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-core/src/main/kotlin/com/kotlinorm/database/mssql/MssqlSupport.kt)
- [SqLiteSupport](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-core/src/main/kotlin/com/kotlinorm/database/sqlite/SqliteSupport.kt)
- [OracleSupport](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-core/src/main/kotlin/com/kotlinorm/database/oracle/OracleSupport.kt)

### 3. Registering Database Type Implementation Classes

The database type implementation class is registered during `Kronos` initialization through the `registerDBTypeSupport` method of the `SqlManagerCustom` class.

```kotlin
SqlManagerCustom.registerDBTypeSupport(DBType.Mysql, MysqlSupport)
```