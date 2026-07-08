{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 使用 `KColumnType`

`KColumnType` 是 `@ColumnType` 接收的枚举。把它标注在 `KPojo` 属性上，可以指定表操作在当前数据库方言下渲染的 DDL 类型。

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

val code = ColumnTypeCatalog().kronosColumns().single { it.name == "code" }
```

结果：

| 字段元数据 | 值 |
|------------|----|
| `code.type` | `KColumnType.VARCHAR` |
| `code.length` | `64` |
| `code.scale` | `0` |

## 文本和 UUID 类型

字符串类 `KColumnType` 可用于短文本、长文本和 UUID 值。

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

DDL 类型片段：

| 方言 | `code` | `description` | `externalId` |
|------|--------|---------------|--------------|
| MySQL | `VARCHAR(64)` | `TEXT` | `CHAR(36)` |
| PostgreSQL | `VARCHAR(64)` | `TEXT` | `UUID` |
| SQLite | `TEXT` | `TEXT` | `TEXT` |
| SQLServer | `VARCHAR(64)` | `TEXT` | `CHAR(36)` |
| Oracle | `VARCHAR2(64)` | `CLOB` | `CHAR(36)` |

## 数值类型

数值类 `KColumnType` 可用于布尔值、整数和精确小数。

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

DDL 类型片段：

| 方言 | `active` | `visits` | `balance` |
|------|----------|----------|-----------|
| MySQL | `TINYINT(1)` | `BIGINT(20)` | `DECIMAL(12,4)` |
| PostgreSQL | `BOOLEAN` | `BIGINT` | `DECIMAL(12,4)` |
| SQLite | `INTEGER` | `INTEGER` | `NUMERIC` |
| SQLServer | `BIT` | `BIGINT` | `DECIMAL(12,4)` |
| Oracle | `NUMBER(1)` | `NUMBER(19)` | `NUMBER(12,4)` |

## 日期和时间类型

时间类 `KColumnType` 可用于日期、时间、日期时间和时间戳列。方言支持小秒精度时，`scale` 会参与渲染。

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

DDL 类型片段：

| 方言 | `eventDate` | `eventTime` | `createdAt` | `touchedAt` |
|------|-------------|-------------|-------------|-------------|
| MySQL | `DATE` | `TIME` | `DATETIME` | `TIMESTAMP` |
| PostgreSQL | `DATE` | `TIME(0)` | `TIMESTAMP(0)` | `TIMESTAMP(4)` |
| SQLite | `TEXT` | `TEXT` | `TEXT` | `TEXT` |
| SQLServer | `DATE` | `TIME` | `DATETIME` | `TIMESTAMP` |
| Oracle | `DATE` | `TIMESTAMP(0)` | `TIMESTAMP(6)` | `TIMESTAMP(4)` |

## JSON、二进制、空间和 XML 类型

Kotlin 值是对象且数据库列需要 JSON 类型时，可以把 `JSON` 与 `@Serialize` 搭配使用。二进制、空间和 XML 值也使用同样的 `@ColumnType` 写法。

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

DDL 类型片段：

| 方言 | `payload` | `attachment` | `position` | `document` |
|------|-----------|--------------|------------|------------|
| MySQL | `JSON` | `BLOB` | `POINT` | `TEXT` |
| PostgreSQL | `JSONB` | `BYTEA` | `POINT` | `XML` |
| SQLite | `TEXT` | `BLOB` | `TEXT` | `TEXT` |
| SQLServer | `JSON` | `VARBINARY(MAX)` | `GEOMETRY` | `XML` |
| Oracle | `JSON` | `BLOB` | `SDO_GEOMETRY` | `XMLType` |

## 紧凑参考表

下表展示表结构 DDL 对常见 `KColumnType` 的类型字符串。单元格中的 `n`、`p`、`s` 来自 `@ColumnType` 的 `length` 和 `scale`；decimal/numeric 只设置 `length` 时会渲染为 `p,0`。

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
| `FLOAT` | `FLOAT` 或 `FLOAT(p,s)` | `DOUBLE PRECISION` 或 `FLOAT(p)` | `REAL` | `FLOAT` 或 `FLOAT(p)` | `BINARY_DOUBLE` 或 `FLOAT(p)` |
| `DOUBLE` | `DOUBLE` 或 `DOUBLE(p,s)` | `DOUBLE PRECISION` | `REAL` | `FLOAT(53)` | `BINARY_DOUBLE` |
| `DECIMAL` | `DECIMAL(10,0)` 或 `DECIMAL(p,s)` | `DECIMAL` 或 `DECIMAL(p,s)` | `NUMERIC` | `DECIMAL(18,0)` 或 `DECIMAL(p,s)` | `NUMBER(10,0)` 或 `NUMBER(p,s)` |
| `NUMERIC` | `NUMERIC(10,0)` 或 `NUMERIC(p,s)` | `NUMERIC` 或 `NUMERIC(p,s)` | `NUMERIC` | `NUMERIC(18,0)` 或 `NUMERIC(p,s)` | `NUMERIC(10,0)` 或 `NUMERIC(p,s)` |
| `CHAR` | `CHAR(255)` 或 `CHAR(n)` | `CHAR(255)` 或 `CHAR(n)` | `TEXT` | `CHAR(255)` 或 `CHAR(n)` | `CHAR(255)` 或 `CHAR(n)` |
| `VARCHAR` | `VARCHAR(255)` 或 `VARCHAR(n)` | `TEXT` 或 `VARCHAR(n)` | `TEXT` | `VARCHAR(255)` 或 `VARCHAR(n/MAX)` | `VARCHAR2(255)` 或 `VARCHAR2(n)` |
| `NCHAR` | `CHAR(255)` 或 `CHAR(n)` | `CHAR(255)` 或 `CHAR(n)` | `TEXT` | `NVARCHAR(255)` 或 `NVARCHAR(n)` | `NCHAR(255)` 或 `NCHAR(n)` |
| `NVARCHAR` | `VARCHAR(255)` 或 `VARCHAR(n)` | `TEXT` 或 `VARCHAR(n)` | `TEXT` | `NVARCHAR(255)` 或 `NVARCHAR(n/MAX)` | `NVARCHAR2(255)` 或 `NVARCHAR2(n)` |
| `TEXT` | `TEXT` | `TEXT` | `TEXT` | `TEXT` | `CLOB` |
| `MEDIUMTEXT` | `MEDIUMTEXT` | `TEXT` | `TEXT` | `TEXT` | `CLOB` |
| `LONGTEXT` | `LONGTEXT` | `TEXT` | `TEXT` | `TEXT` | `CLOB` |
| `CLOB` | `CLOB` | `TEXT` | `TEXT` | `TEXT` | `CLOB` |
| `NCLOB` | `NCLOB` | `TEXT` | `TEXT` | `NTEXT` | `NCLOB` |
| `DATE` | `DATE` | `DATE` | `TEXT` | `DATE` | `DATE` |
| `TIME` | `TIME` | `TIME(0-6)` | `TEXT` | `TIME` 或 `TIME(s)` | `TIMESTAMP(0)` |
| `DATETIME` | `DATETIME` | `TIMESTAMP(0-6)` | `TEXT` | `DATETIME` 或 `DATETIME2(s)` | `TIMESTAMP(6)` |
| `TIMESTAMP` | `TIMESTAMP` | `TIMESTAMP(0-6)` | `TEXT` | `TIMESTAMP` | `TIMESTAMP(0-9)` |
| `BINARY` | `BINARY(255)` 或 `BINARY(n)` | `BYTEA` | `BLOB` | `BINARY(255)` 或 `BINARY(n)` | `RAW(2000)` 或 `RAW(n)` |
| `VARBINARY` | `VARBINARY(255)` 或 `VARBINARY(n)` | `BYTEA` | `BLOB` | `VARBINARY(255)` 或 `VARBINARY(n)` | `RAW(2000)` 或 `RAW(n)` |
| `LONGVARBINARY` | `LONGBLOB` | `BYTEA` | `BLOB` | `VARBINARY(MAX)` | `BLOB` |
| `BLOB` | `BLOB` | `BYTEA` | `BLOB` | `VARBINARY(MAX)` | `BLOB` |
| `MEDIUMBLOB` | `MEDIUMBLOB` | `BYTEA` | `BLOB` | `VARBINARY(MAX)` | `BLOB` |
| `LONGBLOB` | `LONGBLOB` | `BYTEA` | `BLOB` | `VARBINARY(MAX)` | `BLOB` |
| `JSON` | `JSON` | `JSONB` | `TEXT` | `JSON` | `JSON` |
| `ENUM` | `ENUM` | `VARCHAR(255)` 或 `VARCHAR(n)` | `TEXT` | `NVARCHAR(255)` | `VARCHAR2(255)` |
| `SET` | `SET` | `TEXT` | `INTEGER` | `NVARCHAR(255)` | `VARCHAR2(1000)` |
| `UUID` | `CHAR(36)` | `UUID` | `TEXT` | `CHAR(36)` | `CHAR(36)` |
| `GEOMETRY` | `GEOMETRY` | `GEOMETRY` | `TEXT` | `GEOMETRY` | `SDO_GEOMETRY` |
| `POINT` | `POINT` | `POINT` | `TEXT` | `GEOMETRY` | `SDO_GEOMETRY` |
| `LINESTRING` | `LINESTRING` | `LINESTRING` | `TEXT` | `GEOMETRY` | `SDO_GEOMETRY` |
| `XML` | `TEXT` | `XML` | `TEXT` | `XML` | `XMLType` |

> **Note**
> Kotlin 类型自动推断见{{ $.keyword("mapping/column-types", ["Kotlin Type 到 KColumnType"]) }}。`@ColumnType` 是用户主动选择其他枚举值的入口。
