## 已经支持的数据库类型
Kronos支持创建插件实现数据库类型的扩展，目前支持的数据库类型如下：

- [MySQL](https://www.mysql.com/)
- [PostgreSQL](https://www.postgresql.org/)
- [Mssql](https://www.microsoft.com/sql-server)
- [SQLite](https://www.sqlite.org/)
- [Oracle](https://www.oracle.com/database/technologies/)

## 支持扩展的数据库类型
尚未提供官方支持，但可以通过创建插件实现数据库类型如下：

- [<span class="code-red">DB2</span>](https://www.ibm.com/db2)
- [<span class="code-red">Sybase</span>](https://www.sap.com/)
- [<span class="code-red">H2</span>](https://www.h2database.com/)
- [<span class="code-red">OceanBase</span>](https://www.oceanbase.com/)
- [<span class="code-red">DM8</span>](https://www.dameng.com/DM8.html)
- [<span class="code-red">GaussDB</span>](https://www.huaweicloud.com/product/gaussdb.html)

## 如何实现数据库支持

### 1. 获取数据库类型枚举类

通常情况下，数据库类型枚举类是由Kronos提供的，你可以通过`DBType`类获取到所有支持的数据库类型。

```kotlin
val mysql = DBType.Mysql
val postgresql = DBType.Postgres
val oceanBase = DBType.OceanBase
```

### 2. 创建数据库类型实现类