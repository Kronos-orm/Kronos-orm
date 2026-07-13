{% import "../../../macros/macros-en.njk" as $ %}

## Use `KColumnType`

`KColumnType` is the enum accepted by `@ColumnType`. Use it on a `KPojo` property to choose the DDL type that table operations render for the current database dialect.

```kotlin name="kotlin" icon="kotlin" {8}
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

@Table("tb_column_type_catalog")
data class ColumnTypeCatalog(
    @ColumnType(KColumnType.VARCHAR, length = 64)
    var code: String? = null,
) : KPojo

val code = ColumnTypeCatalog().__columns.single { it.name == "code" }
```

Result:

| Field metadata | Value |
|----------------|-------|
| `code.type` | `KColumnType.VARCHAR` |
| `code.length` | `64` |
| `code.scale` | `0` |

## Text And UUID Types

Use string-like `KColumnType` values for short text, long text, and UUID values.

```kotlin name="kotlin" icon="kotlin" {8,11,14}
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

@Table("tb_text_type")
data class TextType(
    @ColumnType(KColumnType.VARCHAR, length = 64)
    var code: String? = null,

    @ColumnType(KColumnType.TEXT)
    var description: String? = null,

    @ColumnType(KColumnType.UUID)
    var externalId: String? = null,
) : KPojo
```

DDL type fragments:

| Dialect | `code` | `description` | `externalId` |
|---------|--------|---------------|--------------|
| MySQL | `VARCHAR(64)` | `TEXT` | `CHAR(36)` |
| PostgreSQL | `VARCHAR(64)` | `TEXT` | `UUID` |
| SQLite | `TEXT` | `TEXT` | `TEXT` |
| SQLServer | `VARCHAR(64)` | `TEXT` | `CHAR(36)` |
| Oracle | `VARCHAR2(64)` | `CLOB` | `CHAR(36)` |

## Numeric Types

Use numeric `KColumnType` values for booleans, integers, and exact decimal values.

```kotlin name="kotlin" icon="kotlin" {9,12,15}
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.math.BigDecimal

@Table("tb_numeric_type")
data class NumericType(
    @ColumnType(KColumnType.BIT)
    var active: Boolean? = null,

    @ColumnType(KColumnType.BIGINT)
    var visits: Long? = null,

    @ColumnType(KColumnType.DECIMAL, length = 12, scale = 4)
    var balance: BigDecimal? = null,
) : KPojo
```

DDL type fragments:

| Dialect | `active` | `visits` | `balance` |
|---------|----------|----------|-----------|
| MySQL | `TINYINT(1)` | `BIGINT(20)` | `DECIMAL(12,4)` |
| PostgreSQL | `BOOLEAN` | `BIGINT` | `DECIMAL(12,4)` |
| SQLite | `INTEGER` | `INTEGER` | `NUMERIC` |
| SQLServer | `BIT` | `BIGINT` | `DECIMAL(12,4)` |
| Oracle | `NUMBER(1)` | `NUMBER(19)` | `NUMBER(12,4)` |

## Date And Time Types

Use temporal `KColumnType` values for date, time, datetime, and timestamp columns. `scale` controls fractional-second precision where the dialect uses it.

```kotlin name="kotlin" icon="kotlin" {12,15,18,21}
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Table("tb_temporal_type")
data class TemporalType(
    @ColumnType(KColumnType.DATE)
    var eventDate: LocalDate? = null,

    @ColumnType(KColumnType.TIME)
    var eventTime: LocalTime? = null,

    @ColumnType(KColumnType.DATETIME)
    var createdAt: LocalDateTime? = null,

    @ColumnType(KColumnType.TIMESTAMP, scale = 4)
    var touchedAt: Timestamp? = null,
) : KPojo
```

DDL type fragments:

| Dialect | `eventDate` | `eventTime` | `createdAt` | `touchedAt` |
|---------|-------------|-------------|-------------|-------------|
| MySQL | `DATE` | `TIME` | `DATETIME` | `TIMESTAMP` |
| PostgreSQL | `DATE` | `TIME(0)` | `TIMESTAMP(0)` | `TIMESTAMP(4)` |
| SQLite | `TEXT` | `TEXT` | `TEXT` | `TEXT` |
| SQLServer | `DATE` | `TIME` | `DATETIME` | `TIMESTAMP` |
| Oracle | `DATE` | `TIMESTAMP(0)` | `TIMESTAMP(6)` | `TIMESTAMP(4)` |

## JSON, Binary, Spatial, And XML Types

Use `JSON` with `@Serialize` when the Kotlin value is an object and the database column should use the dialect JSON type. Binary, spatial, and XML values follow the same `@ColumnType` pattern.

```kotlin name="kotlin" icon="kotlin" {11,12,15,18,21}
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

data class AuditPayload(val ip: String, val tags: List<String>)

@Table("tb_document_type")
data class DocumentType(
    @ColumnType(KColumnType.JSON)
    @Serialize
    var payload: AuditPayload? = null,

    @ColumnType(KColumnType.BLOB)
    var attachment: ByteArray? = null,

    @ColumnType(KColumnType.POINT)
    var position: String? = null,

    @ColumnType(KColumnType.XML)
    var document: String? = null,
) : KPojo
```

DDL type fragments:

| Dialect | `payload` | `attachment` | `position` | `document` |
|---------|-----------|--------------|------------|------------|
| MySQL | `JSON` | `BLOB` | `POINT` | `TEXT` |
| PostgreSQL | `JSONB` | `BYTEA` | `POINT` | `XML` |
| SQLite | `TEXT` | `BLOB` | `TEXT` | `TEXT` |
| SQLServer | `JSON` | `VARBINARY(MAX)` | `GEOMETRY` | `XML` |
| Oracle | `JSON` | `BLOB` | `SDO_GEOMETRY` | `XMLType` |

## Compact Reference

This table shows the type strings rendered by table DDL for common `KColumnType` values. Cells with `n`, `p`, or `s` use `length` and `scale` from `@ColumnType`; decimal and numeric cells with `p` render `p,0` when only `length` is set.

| `KColumnType` | MySQL | PostgreSQL | SQLite | SQLServer | Oracle |
|---------------|-------|------------|--------|-----------|--------|
| `BIT` | `TINYINT(1)` | `BOOLEAN` | `INTEGER` | `BIT` | `NUMBER(1)` |
| `TINYINT` | `TINYINT(4)` | `SMALLINT` | `INTEGER` | `TINYINT` | `NUMBER(3)` |
| `SMALLINT` | `SMALLINT(6)` | `SMALLINT` | `INTEGER` | `SMALLINT` | `NUMBER(5)` |
| `MEDIUMINT` | `MEDIUMINT(9)` | `INTEGER` | `INTEGER` | `INT` | `NUMBER(7)` |
| `INT` | `INT(11)` | `INTEGER` | `INTEGER` | `INT` | `NUMBER(10)` |
| `BIGINT` | `BIGINT(20)` | `BIGINT` | `INTEGER` | `BIGINT` | `NUMBER(19)` |
| `SERIAL` | `INT(11)` | `SERIAL` | `INTEGER` | `INT` | `NUMBER` |
| `YEAR` | `YEAR` | `INTEGER` | `INTEGER` | `INT` | `NUMBER(4)` |
| `REAL` | `REAL` | `REAL` | `REAL` | `REAL` | `BINARY_FLOAT` |
| `FLOAT` | `FLOAT` or `FLOAT(p,s)` | `DOUBLE PRECISION` or `FLOAT(p)` | `REAL` | `FLOAT` or `FLOAT(p)` | `BINARY_DOUBLE` or `FLOAT(p)` |
| `DOUBLE` | `DOUBLE` or `DOUBLE(p,s)` | `DOUBLE PRECISION` | `REAL` | `FLOAT(53)` | `BINARY_DOUBLE` |
| `DECIMAL` | `DECIMAL(10,0)` or `DECIMAL(p,s)` | `DECIMAL` or `DECIMAL(p,s)` | `NUMERIC` | `DECIMAL(18,0)` or `DECIMAL(p,s)` | `NUMBER(10,0)` or `NUMBER(p,s)` |
| `NUMERIC` | `NUMERIC(10,0)` or `NUMERIC(p,s)` | `NUMERIC` or `NUMERIC(p,s)` | `NUMERIC` | `NUMERIC(18,0)` or `NUMERIC(p,s)` | `NUMERIC(10,0)` or `NUMERIC(p,s)` |
| `CHAR` | `CHAR(255)` or `CHAR(n)` | `CHAR(255)` or `CHAR(n)` | `TEXT` | `CHAR(255)` or `CHAR(n)` | `CHAR(255)` or `CHAR(n)` |
| `VARCHAR` | `VARCHAR(255)` or `VARCHAR(n)` | `TEXT` or `VARCHAR(n)` | `TEXT` | `VARCHAR(255)` or `VARCHAR(n/MAX)` | `VARCHAR2(255)` or `VARCHAR2(n)` |
| `NCHAR` | `CHAR(255)` or `CHAR(n)` | `CHAR(255)` or `CHAR(n)` | `TEXT` | `NVARCHAR(255)` or `NVARCHAR(n)` | `NCHAR(255)` or `NCHAR(n)` |
| `NVARCHAR` | `VARCHAR(255)` or `VARCHAR(n)` | `TEXT` or `VARCHAR(n)` | `TEXT` | `NVARCHAR(255)` or `NVARCHAR(n/MAX)` | `NVARCHAR2(255)` or `NVARCHAR2(n)` |
| `TEXT` | `TEXT` | `TEXT` | `TEXT` | `TEXT` | `CLOB` |
| `MEDIUMTEXT` | `MEDIUMTEXT` | `TEXT` | `TEXT` | `TEXT` | `CLOB` |
| `LONGTEXT` | `LONGTEXT` | `TEXT` | `TEXT` | `TEXT` | `CLOB` |
| `CLOB` | `CLOB` | `TEXT` | `TEXT` | `TEXT` | `CLOB` |
| `NCLOB` | `NCLOB` | `TEXT` | `TEXT` | `NTEXT` | `NCLOB` |
| `DATE` | `DATE` | `DATE` | `TEXT` | `DATE` | `DATE` |
| `TIME` | `TIME` | `TIME(0-6)` | `TEXT` | `TIME` or `TIME(s)` | `TIMESTAMP(0)` |
| `DATETIME` | `DATETIME` | `TIMESTAMP(0-6)` | `TEXT` | `DATETIME` or `DATETIME2(s)` | `TIMESTAMP(6)` |
| `TIMESTAMP` | `TIMESTAMP` | `TIMESTAMP(0-6)` | `TEXT` | `TIMESTAMP` | `TIMESTAMP(0-9)` |
| `BINARY` | `BINARY(255)` or `BINARY(n)` | `BYTEA` | `BLOB` | `BINARY(255)` or `BINARY(n)` | `RAW(2000)` or `RAW(n)` |
| `VARBINARY` | `VARBINARY(255)` or `VARBINARY(n)` | `BYTEA` | `BLOB` | `VARBINARY(255)` or `VARBINARY(n)` | `RAW(2000)` or `RAW(n)` |
| `LONGVARBINARY` | `LONGBLOB` | `BYTEA` | `BLOB` | `VARBINARY(MAX)` | `BLOB` |
| `BLOB` | `BLOB` | `BYTEA` | `BLOB` | `VARBINARY(MAX)` | `BLOB` |
| `MEDIUMBLOB` | `MEDIUMBLOB` | `BYTEA` | `BLOB` | `VARBINARY(MAX)` | `BLOB` |
| `LONGBLOB` | `LONGBLOB` | `BYTEA` | `BLOB` | `VARBINARY(MAX)` | `BLOB` |
| `JSON` | `JSON` | `JSONB` | `TEXT` | `JSON` | `JSON` |
| `ENUM` | `ENUM` | `VARCHAR(255)` or `VARCHAR(n)` | `TEXT` | `NVARCHAR(255)` | `VARCHAR2(255)` |
| `SET` | `SET` | `TEXT` | `INTEGER` | `NVARCHAR(255)` | `VARCHAR2(1000)` |
| `UUID` | `CHAR(36)` | `UUID` | `TEXT` | `CHAR(36)` | `CHAR(36)` |
| `GEOMETRY` | `GEOMETRY` | `GEOMETRY` | `TEXT` | `GEOMETRY` | `SDO_GEOMETRY` |
| `POINT` | `POINT` | `POINT` | `TEXT` | `GEOMETRY` | `SDO_GEOMETRY` |
| `LINESTRING` | `LINESTRING` | `LINESTRING` | `TEXT` | `GEOMETRY` | `SDO_GEOMETRY` |
| `XML` | `TEXT` | `XML` | `TEXT` | `XML` | `XMLType` |

> **Note**
> Automatic Kotlin type inference is covered in {{ $.keyword("mapping/column-types", ["Kotlin Type To KColumnType"]) }}. `@ColumnType` is the user-facing API for choosing a different enum value.
