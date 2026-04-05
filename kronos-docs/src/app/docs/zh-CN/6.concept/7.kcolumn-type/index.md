各数据库类型与常用类型对应关系一览表：

## **BIT**

存储 0/1

**KotlinType**: `Boolean`

| Mysql      | Oracle    | SQL Server | PostgreSQL | SQLite  |
|------------|-----------|------------|------------|---------|
| TINYINT(1) | NUMBER(1) | BIT        | BOOLEAN    | INTEGER |

## **TINYINT**

存储 -128~127

**KotlinType**: `Byte`

| Mysql   | Oracle    | SQL Server | PostgreSQL | SQLite  |
|---------|-----------|------------|------------|---------|
| TINYINT | NUMBER(3) | TINYINT    | SMALLINT   | INTEGER |

## **SMALLINT**

存储 -32768~32767

**KotlinType**: `Short`

| Mysql    | Oracle    | SQL Server | PostgreSQL | SQLite  |
|----------|-----------|------------|------------|---------|
| SMALLINT | NUMBER(5) | SMALLINT   | SMALLINT   | INTEGER |

## **MEDIUMINT**

存储 -8388608~8388607

**KotlinType**: `Int`

| Mysql     | Oracle    | SQL Server | PostgreSQL | SQLite  |
|-----------|-----------|------------|------------|---------|
| MEDIUMINT | NUMBER(7) | INT        | INTEGER    | INTEGER |

## **INT**

存储 -2147483648~2147483647

**KotlinType**: `Int`

| Mysql | Oracle     | SQL Server | PostgreSQL | SQLite  |
|-------|------------|------------|------------|---------|
| INT   | NUMBER(10) | INT        | INTEGER    | INTEGER |

## **BIGINT**

存储 -9223372036854775808~9223372036854775807

**KotlinType**: `Long`

| Mysql  | Oracle     | SQL Server | PostgreSQL | SQLite  |
|--------|------------|------------|------------|---------|
| BIGINT | NUMBER(19) | BIGINT     | BIGINT     | INTEGER |

## **FLOAT**

存储 -3.4028235E+38~3.4028235E+38

**KotlinType**: `Float`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| FLOAT | FLOAT  | FLOAT      | REAL       | REAL   |

## **DOUBLE**

存储 -1.7976931348623157E+308~1.7976931348623157E+308

**KotlinType**: `Double`

| Mysql  | Oracle | SQL Server | PostgreSQL | SQLite |
|--------|--------|------------|------------|--------|
| DOUBLE | DOUBLE | FLOAT      | DOUBLE     | REAL   |

## **DECIMAL**

存储 -10^38+1~10^38-1

**KotlinType**: `BigDecimal`

| Mysql   | Oracle | SQL Server | PostgreSQL | SQLite  |
|---------|--------|------------|------------|---------|
| DECIMAL | NUMBER | DECIMAL    | DECIMAL    | NUMERIC |

## **SERIAL**

存储序列号

**KotlinType**: `Int`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite  |
|-------|--------|------------|------------|---------|
| INT   | NUMBER | INT        | SERIAL     | INTEGER |

## **NUMERIC**

存储 -10^38+1~10^38-1

**KotlinType**: `BigDecimal`

| Mysql   | Oracle | SQL Server | PostgreSQL | SQLite  |
|---------|--------|------------|------------|---------|
| NUMERIC | NUMBER | DECIMAL    | DECIMAL    | NUMERIC |

## **CHAR**

存储固定长度字符串

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| CHAR  | CHAR   | CHAR       | CHAR       | TEXT   |

## **VARCHAR**

存储可变长度字符串

**KotlinType**: `String`

| Mysql   | Oracle  | SQL Server | PostgreSQL | SQLite |
|---------|---------|------------|------------|--------|
| VARCHAR | VARCHAR | VARCHAR    | VARCHAR    | TEXT   |

## **TEXT**

存储文本

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| TEXT  | CLOB   | TEXT       | TEXT       | TEXT   |

## **MEDIUMTEXT**

存储中等文本

**KotlinType**: `String`

| Mysql      | Oracle | SQL Server | PostgreSQL | SQLite |
|------------|--------|------------|------------|--------|
| MEDIUMTEXT | CLOB   | TEXT       | TEXT       | TEXT   |

## **LONGTEXT**

存储长文本

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| TEXT  | CLOB   | TEXT       | TEXT       | TEXT   |

## **DATE**

存储日期

**KotlinType**: `LocalDate`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| DATE  | DATE   | DATE       | DATE       | TEXT   |

## **TIME**

存储时间

**KotlinType**: `LocalTime`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| TIME  | DATE   | TIME       | TIME       | TEXT   |

## **DATETIME**

存储日期时间

**KotlinType**: `LocalDateTime`

| Mysql    | Oracle | SQL Server | PostgreSQL | SQLite |
|----------|--------|------------|------------|--------|
| DATETIME | DATE   | DATETIME   | TIMESTAMP  | TEXT   |

## **TIMESTAMP**

存储时间戳（时间）

**KotlinType**: `String`

| Mysql     | Oracle | SQL Server | PostgreSQL | SQLite |
|-----------|--------|------------|------------|--------|
| TIMESTAMP | DATE   | DATETIME   | TIMESTAMP  | TEXT   |

## **Year**

存储年份

**KotlinType**: `Int`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite  |
|-------|--------|------------|------------|---------|
| YEAR  | NUMBER | INT        | INT        | INTEGER |

## **BINARY**

存储二进制

**KotlinType**: `ByteArray`

| Mysql  | Oracle | SQL Server | PostgreSQL | SQLite |
|--------|--------|------------|------------|--------|
| BINARY | BLOB   | BINARY     | BYTEA      | BLOB   |

## **VARBINARY**

存储可变二进制

**KotlinType**: `ByteArray`

| Mysql     | Oracle | SQL Server | PostgreSQL | SQLite |
|-----------|--------|------------|------------|--------|
| VARBINARY | BLOB   | VARBINARY  | BYTEA      | BLOB   |

## **LONGVARBINARY**

存储长二进制

**KotlinType**: `ByteArray`

| Mysql    | Oracle | SQL Server | PostgreSQL | SQLite |
|----------|--------|------------|------------|--------|
| LONGBLOB | BLOB   | IMAGE      | BYTEA      | BLOB   |

## **BLOB**

存储二进制大对象

**KotlinType**: `ByteArray`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| BLOB  | BLOB   | IMAGE      | BYTEA      | BLOB   |

## **MEDIUMBLOB**

存储中等二进制大对象

**KotlinType**: `ByteArray`

| Mysql      | Oracle | SQL Server | PostgreSQL | SQLite |
|------------|--------|------------|------------|--------|
| MEDIUMBLOB | BLOB   | IMAGE      | BYTEA      | BLOB   |

## **LONGBLOB**

存储长二进制大对象

**KotlinType**: `ByteArray`

| Mysql    | Oracle | SQL Server | PostgreSQL | SQLite |
|----------|--------|------------|------------|--------|
| LONGBLOB | BLOB   | IMAGE      | BYTEA      | BLOB   |

## **CLOB**

存储字符大对象

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| CLOB  | CLOB   | TEXT       | TEXT       | TEXT   |

## **JSON**

存储 JSON

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| JSON  | JSON   | JSON       | JSON       | TEXT   |

## **ENUM**

存储枚举

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| ENUM  | ENUM   | ENUM       | TEXT       | TEXT   |

## **NVARCHAR**

存储包含n个字符的可变长度Unicode字符数据

**KotlinType**: `String`

| Mysql    | Oracle   | SQL Server | PostgreSQL | SQLite |
|----------|----------|------------|------------|--------|
| NVARCHAR | NVARCHAR | NVARCHAR   | TEXT       | TEXT   |

## **NCHAR**

存储包含n个字符的固定长度Unicode字符数据

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| NCHAR | NCHAR  | NCHAR      | TEXT       | TEXT   |

## **NCLOB**

存储包含n个字符的Unicode字符数据

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| NCLOB | NCLOB  | NTEXT      | TEXT       | TEXT   |

## **UUID**

存储 UUID（通用唯一标识符）

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| CHAR  | CHAR   | CHAR       | UUID       | TEXT   |

## **SET**

存储 SET

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| SET   | SET    | SET        | TEXT       | TEXT   |

## **GEOMETRY**

存储几何

**KotlinType**: `String`

| Mysql    | Oracle   | SQL Server | PostgreSQL | SQLite |
|----------|----------|------------|------------|--------|
| GEOMETRY | GEOMETRY | GEOMETRY   | GEOMETRY   | TEXT   |

## **POINT**

存储点

**KotlinType**: `String`

| Mysql | Oracle | SQL Server | PostgreSQL | SQLite |
|-------|--------|------------|------------|--------|
| POINT | POINT  | POINT      | POINT      | TEXT   |

## **LINESTRING**

存储线

**KotlinType**: `String`

| Mysql      | Oracle     | SQL Server | PostgreSQL | SQLite |
|------------|------------|------------|------------|--------|
| LINESTRING | LINESTRING | LINESTRING | LINESTRING | TEXT   |
