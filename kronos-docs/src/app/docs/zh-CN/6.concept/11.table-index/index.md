## 索引类型

### MySQL

- **NORMAL** `Mysql.KIndexType.NORMAL` 普通索引，无需指定，MySQL默认使用该索引。

- **UNIQUE** `Mysql.KIndexType.UNIQUE` 唯一索引，用于保证数据唯一性。

- **FULLTEXT** `Mysql.KIndexType.FULLTEXT` 全文索引，只有MyISAM引擎支持，用于全文搜索。

- **SPATIAL** `Mysql.KIndexType.SPATIAL` 空间索引，用于空间数据类型，支持范围查询。

### PostgreSQL

- **UNIQUE** `PostgreSQL.KIndexType.UNIQUE` 唯一索引，用于保证数据唯一性。

### Oracle

- **NORMAL** `Oracle.KIndexType.NORMAL` 普通索引，无需指定，Oracle默认使用该索引。

- **BITMAP** `Oracle.KIndexType.BITMAP` 位图索引，用于高并发查询，适用于低基数列。

- **UNIQUE** `Oracle.KIndexType.UNIQUE` 唯一索引，用于保证数据唯一性。

### SQL Server

- **CLUSTERED** `SqlServer.KIndexType.CLUSTERED` 聚集索引，表中数据的物理顺序与索引的逻辑顺序一致。

- **NONCLUSTERED** `SqlServer.KIndexType.NONCLUSTERED` 非聚集索引，表中数据的物理顺序与索引的逻辑顺序不一致。

- **XML** `SqlServer.KIndexType.XML` XML索引，用于XML数据类型。

- **SPATIAL** `SqlServer.KIndexType.SPATIAL` 空间索引，用于空间数据类型。

### SQLite

- **UNIQUE** `SQLite.KIndexType.UNIQUE` 唯一索引，用于保证数据唯一性。

## 索引方法

### MySQL

- **BTREE** `Mysql.KIndexMethod.BTREE` 二叉树索引，最常见的索引类型，无需指定，MySQL默认使用该索引。

- **HASH**  `Mysql.KIndexMethod.HASH` 哈希索引，只有Memory引擎支持，不支持范围查询。

### PostgreSQL

- **BTREE** `PostgreSQL.KIndexMethod.BTREE` 二叉树索引，无需指定，PostgreSQL默认使用该索引。

- **HASH** `PostgreSQL.KIndexMethod.HASH` 哈希索引，用于等值查询，不支持范围查询。

- **GIST** `PostgreSQL.KIndexMethod.GIST` 通用索引，支持范围查询，支持空间索引。

- **SPGIST** `PostgreSQL.KIndexMethod.SPGIST` 空间索引，支持范围查询。

- **GIN** `PostgreSQL.KIndexMethod.GIN` 通用倒排索引，用于全文搜索。

- **BRIN** `PostgreSQL.KIndexMethod.BRIN` 区间索引，用于大表。

### SQL Server

- **UNIQUE** `SqlServer.KIndexMethod.UNIQUE` 唯一索引，用于保证数据唯一性。