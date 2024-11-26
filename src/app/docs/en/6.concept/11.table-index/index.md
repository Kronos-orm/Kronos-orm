{% import "../../../macros/macros-en.njk" as $ %}

## Index Type

### MySQL

- **NORMAL** `Mysql.KIndexType.NORMAL` Normal index, no need to specify, MySQL uses this index by default.

- **UNIQUE** `Mysql.KIndexType.UNIQUE` Unique indexes, used to ensure data uniqueness.

- **FULLTEXT** `Mysql.KIndexType.FULLTEXT` Full-text indexing, supported only by the MyISAM engine, is used for full-text searches.

- **SPATIAL** `Mysql.KIndexType.SPATIAL` Spatial indexes for spatial data types that support range queries.

### PostgreSQL

- **UNIQUE** `PostgreSQL.KIndexType.UNIQUE` Unique indexes, used to ensure data uniqueness.

### Oracle

- **NORMAL** `Oracle.KIndexType.NORMAL` Normal index, no need to specify, Oracle uses this index by default.

- **BITMAP** `Oracle.KIndexType.BITMAP` Bitmap indexes for highly concurrent queries for low base columns.

- **UNIQUE** `Oracle.KIndexType.UNIQUE` Unique indexes, used to ensure data uniqueness.

### SQL Server

- **CLUSTERED** `SqlServer.KIndexType.CLUSTERED` Aggregated indexes where the physical order of the data in the table matches the logical order of the index.

- **NONCLUSTERED** `SqlServer.KIndexType.NONCLUSTERED` Non-aggregated indexes where the physical order of the data in the table does not match the logical order of the index.

- **XML** `SqlServer.KIndexType.XML` XML index for XML data types.

- **SPATIAL** `SqlServer.KIndexType.SPATIAL` Spatial index for spatial data types.

### SQLite

- **UNIQUE** `SQLite.KIndexType.UNIQUE` Unique indexes, used to ensure data uniqueness.

## Indexing Methods

### MySQL

- **BTREE** `Mysql.KIndexMethod.BTREE` Binary tree index, the most common type of index, no need to specify, MySQL uses this index by default.

- **HASH**  `Mysql.KIndexMethod.HASH` Hash indexes, which are only supported by the Memory engine, do not support range queries.

### PostgreSQL

- **BTREE** `PostgreSQL.KIndexMethod.BTREE` Binary tree index, no need to specify, PostgreSQL uses this index by default.

- **HASH** `PostgreSQL.KIndexMethod.HASH` Hash indexes, used for equal-value queries, do not support range queries.

- **GIST** `PostgreSQL.KIndexMethod.GIST` Generic indexes with range query support and spatial index support.

- **SPGIST** `PostgreSQL.KIndexMethod.SPGIST` Spatial index with range query support.

- **GIN** `PostgreSQL.KIndexMethod.GIN` Generalized inverted index for full-text searching.

- **BRIN** `PostgreSQL.KIndexMethod.BRIN` Interval indexes for large tables.

### SQL Server

- **UNIQUE** `SqlServer.KIndexMethod.UNIQUE` Unique indexes, used to ensure data uniqueness.