# {{ NgDocPage.title }}

## MySQL

### Index Methods

#### BTREE

- `Mysql.KIndexMethod.BTREE`

Binary tree index is the most common index type. No need to specify it, MySQL uses this index by default.

#### HASH

- `Mysql.KIndexMethod.HASH`

Hash indexes are only supported by the Memory engine and do not support range queries.

### Index Type

#### NORMAL

- `Mysql.KIndexType.NORMAL`

Ordinary index, no need to specify, MySQL uses this index by default.

#### UNIQUE

- `Mysql.KIndexType.UNIQUE`

Unique index, used to ensure data uniqueness.

#### FULLTEXT

- `Mysql.KIndexType.FULLTEXT`

Full-text indexing is supported only by the MyISAM engine and is used for full-text search.

#### SPATIAL

- `Mysql.KIndexType.SPATIAL`

Spatial index, used for spatial data types, supports range queries.

## PostgreSQL

### Index Methods

#### BTREE

- `PostgreSQL.KIndexMethod.BTREE`

Binary tree index, no need to specify, PostgreSQL uses this index by default.

#### HASH

- `PostgreSQL.KIndexMethod.HASH`

Hash index is used for equality query and does not support range query.

#### GIST

- `PostgreSQL.KIndexMethod.GIST`

General index, supports range query, and supports spatial index.

#### SPGIST

- `PostgreSQL.KIndexMethod.SPGIST`

Spatial index, supporting range queries.

#### GIN

- `PostgreSQL.KIndexMethod.GIN`

General inverted index for full-text search.

#### BRIN

- `PostgreSQL.KIndexMethod.BRIN`

Range index, used for large tables.

### Index Type

#### UNIQUE

- `PostgreSQL.KIndexType.UNIQUE`

Unique index, used to ensure data uniqueness.

## Oracle

### Index Type

#### NORMAL

- `Oracle.KIndexType.NORMAL`

Ordinary index, no need to specify, Oracle uses this index by default.

#### BITMAP

- `Oracle.KIndexType.BITMAP`

Bitmap index, used for high-concurrency queries and suitable for low-cardinality columns.

#### UNIQUE

- `Oracle.KIndexType.UNIQUE`

Unique index, used to ensure data uniqueness.

## SQL Server

### Index Methods

#### UNIQUE

- `SqlServer.KIndexMethod.UNIQUE`

Unique index, used to ensure data uniqueness.

### Index Type

#### CLUSTERED

- `SqlServer.KIndexType.CLUSTERED`

Clustered index, the physical order of the data in the table is consistent with the logical order of the index.

#### NONCLUSTERED

- `SqlServer.KIndexType.NONCLUSTERED`

For non-clustered indexes, the physical order of the data in the table is inconsistent with the logical order of the index.

#### XML

- `SqlServer.KIndexType.XML`

XML index, for XML data type.

#### SPATIAL

- `SqlServer.KIndexType.SPATIAL`

Spatial index, used for spatial data types.

## SQLite

### Index Methods

#### UNIQUE

- `SQLite.KIndexMethod.UNIQUE`

Unique index, used to ensure data uniqueness.

### Index Type

#### NOCASE

- `SQLite.KIndexType.NOCASE`

Ignore case.

#### RTRIM

- `SQLite.KIndexType.RTRIM`

Trailing spaces are ignored.

#### BINARY

- `SQLite.KIndexType.BINARY`

Binary comparison.


