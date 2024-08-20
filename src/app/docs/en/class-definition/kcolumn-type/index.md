# {{ NgDocPage.title }}

List of corresponding relationships between various database types and commonly used types:：

## **BIT**

store 0/1

**KotlinType**: `Boolean`

| Mysql      | Oracle    | SQL Server | PostgreSQL | SQLite  |
|------------|-----------|------------|------------|---------|
| TINYINT(1) | NUMBER(1) | BIT        | BOOLEAN    | INTEGER |

## **TINYINT**

store -128~127

**KotlinType**: `Byte`

| Mysql   | Oracle    | SQL Server | PostgreSQL | SQLite  |
|---------|-----------|------------|------------|---------|
| TINYINT | NUMBER(3) | TINYINT    | SMALLINT   | INTEGER |

## **SMALLINT**

store -32768~32767

**KotlinType**: `Short`

| Mysql    | Oracle    | SQL Server | PostgreSQL | SQLite  |
|----------|-----------|------------|------------|---------|
| SMALLINT | NUMBER(5) | SMALLINT   | SMALLINT   | INTEGER |

## **MEDIUMINT**

store -8388608~8388607

**KotlinType**: `Int`

| Mysql     | Oracle    | SQL Server | PostgreSQL | SQLite  |
|-----------|-----------|------------|------------|---------|
| MEDIUMINT | NUMBER(7) | INT        | INTEGER    | INTEGER |

## **INT**

store -2147483648~2147483647

**KotlinType**: `Int`

| Mysql | Oracle     | SQL Server | PostgreSQL | SQLite  |
|-------|------------|------------|------------|---------|
| INT   | NUMBER(10) | INT        | INTEGER    | INTEGER |

## **BIGINT**

store -9223372036854775808~9223372036854775807

**KotlinType**: `Long`

| Mysql  | Oracle     | SQL Server | PostgreSQL | SQLite  |
|--------|------------|------------|------------|---------|
| BIGINT | NUMBER(19) | BIGINT     | BIGINT     | INTEGER |

## **FLOAT**

store -3.4028235E+38~3.4028235E+38

**KotlinType**: `Float`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| FLOAT | FLOAT  | FLOAT      | REAL       | REAL   |

## **DOUBLE**

store -1.7976931348623157E+308~1.7976931348623157E+308

**KotlinType**: `Double`

| Mysql  | Oracle | SQL Server | PostgreSQL | SQLite |
|--------|--------|------------|------------|--------|
| DOUBLE | DOUBLE | FLOAT      | DOUBLE     | REAL   |

## **DECIMAL**

store -10^38+1~10^38-1

**KotlinType**: `BigDecimal`

| Mysql   | Oracle | SQL Server | PostgreSQL | SQLite  |
|---------|--------|------------|------------|---------|
| DECIMAL | NUMBER | DECIMAL    | DECIMAL    | NUMERIC |

## **SERIAL**

store serial

**KotlinType**: `Int`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite  |
|-------|--------|------------|------------|---------|
| INT   | NUMBER | INT        | SERIAL     | INTEGER |

## **NUMERIC**

store -10^38+1~10^38-1

**KotlinType**: `BigDecimal`

| Mysql   | Oracle | SQL Server | PostgreSQL | SQLite  |
|---------|--------|------------|------------|---------|
| NUMERIC | NUMBER | DECIMAL    | DECIMAL    | NUMERIC |

## **CHAR**

store fixed length string

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| CHAR  | CHAR   | CHAR       | CHAR       | TEXT   |

## **VARCHAR**

store variable length string

**KotlinType**: `String`

| Mysql   | Oracle  | SQL Server | PostgreSQL | SQLite |
|---------|---------|------------|------------|--------|
| VARCHAR | VARCHAR | VARCHAR    | VARCHAR    | TEXT   |

## **TEXT**

store text

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| TEXT  | CLOB   | TEXT       | TEXT       | TEXT   |

## **MEDIUMTEXT**

store medium text

**KotlinType**: `String`

| Mysql      | Oracle | SQL Server | PostgreSQL | SQLite |
|------------|--------|------------|------------|--------|
| MEDIUMTEXT | CLOB   | TEXT       | TEXT       | TEXT   |

## **LONGTEXT**

store long text

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| TEXT  | CLOB   | TEXT       | TEXT       | TEXT   |

## **DATE**

store date

**KotlinType**: `LocalDate`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| DATE  | DATE   | DATE       | DATE       | TEXT   |

## **TIME**

store time

**KotlinType**: `LocalTime`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| TIME  | DATE   | TIME       | TIME       | TEXT   |

## **DATETIME**

store date and time

**KotlinType**: `LocalDateTime`

| Mysql    | Oracle | SQL Server | PostgreSQL | SQLite |
|----------|--------|------------|------------|--------|
| DATETIME | DATE   | DATETIME   | TIMESTAMP  | TEXT   |

## **TIMESTAMP**

store timestamp

**KotlinType**: `String`

| Mysql     | Oracle | SQL Server | PostgreSQL | SQLite |
|-----------|--------|------------|------------|--------|
| TIMESTAMP | DATE   | DATETIME   | TIMESTAMP  | TEXT   |

## **Year**

store year

**KotlinType**: `Int`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite  |
|-------|--------|------------|------------|---------|
| YEAR  | NUMBER | INT        | INT        | INTEGER |

## **BINARY**

store binary

**KotlinType**: `ByteArray`

| Mysql  | Oracle | SQL Server | PostgreSQL | SQLite |
|--------|--------|------------|------------|--------|
| BINARY | BLOB   | BINARY     | BYTEA      | BLOB   |

## **VARBINARY**

store variable length binary

**KotlinType**: `ByteArray`

| Mysql     | Oracle | SQL Server | PostgreSQL | SQLite |
|-----------|--------|------------|------------|--------|
| VARBINARY | BLOB   | VARBINARY  | BYTEA      | BLOB   |

## **LONGVARBINARY**

store long binary

**KotlinType**: `ByteArray`

| Mysql    | Oracle | SQL Server | PostgreSQL | SQLite |
|----------|--------|------------|------------|--------|
| LONGBLOB | BLOB   | IMAGE      | BYTEA      | BLOB   |

## **BLOB**

store blob

**KotlinType**: `ByteArray`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| BLOB  | BLOB   | IMAGE      | BYTEA      | BLOB   |

## **MEDIUMBLOB**

store medium blob

**KotlinType**: `ByteArray`

| Mysql      | Oracle | SQL Server | PostgreSQL | SQLite |
|------------|--------|------------|------------|--------|
| MEDIUMBLOB | BLOB   | IMAGE      | BYTEA      | BLOB   |

## **LONGBLOB**

store long blob

**KotlinType**: `ByteArray`

| Mysql    | Oracle | SQL Server | PostgreSQL | SQLite |
|----------|--------|------------|------------|--------|
| LONGBLOB | BLOB   | IMAGE      | BYTEA      | BLOB   |

## **CLOB**

store clob

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| CLOB  | CLOB   | TEXT       | TEXT       | TEXT   |

## **JSON**

store json

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| JSON  | JSON   | JSON       | JSON       | TEXT   |

## **ENUM**

store enum

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| ENUM  | ENUM   | ENUM       | TEXT       | TEXT   |

## **NVARCHAR**

store nvarchar

**KotlinType**: `String`

| Mysql    | Oracle   | SQL Server | PostgreSQL | SQLite |
|----------|----------|------------|------------|--------|
| NVARCHAR | NVARCHAR | NVARCHAR   | TEXT       | TEXT   |

## **NCHAR**

store nchar

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| NCHAR | NCHAR  | NCHAR      | TEXT       | TEXT   |

## **NCLOB**

store nclob

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| NCLOB | NCLOB  | NTEXT      | TEXT       | TEXT   |

## **UUID**

store uuid

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| CHAR  | CHAR   | CHAR       | UUID       | TEXT   |

## **SET**

store set

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| SET   | SET    | SET        | TEXT       | TEXT   |

## **GEOMETRY**

store geometry

**KotlinType**: `String`

| Mysql    | Oracle   | SQL Server | PostgreSQL | SQLite |
|----------|----------|------------|------------|--------|
| GEOMETRY | GEOMETRY | GEOMETRY   | GEOMETRY   | TEXT   |

## **POINT**

store point

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| POINT | POINT  | POINT      | POINT      | TEXT   |

## **LINESTRING**

store linestring

**KotlinType**: `String`

| Mysql      | Oracle     | SQL Server | PostgreSQL | SQLite |
|------------|------------|------------|------------|--------|
| LINESTRING | LINESTRING | LINESTRING | LINESTRING | TEXT   |
