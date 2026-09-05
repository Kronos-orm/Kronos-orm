{% import "../../../macros/macros-en.njk" as $ %}

## Kotlin type inference

Kronos chooses a column type from each persisted `KPojo` property when the property has no {{ $.keyword("mapping/annotations", ["Annotations"]) }}. The selected database dialect renders the final DDL type when the table is created or synchronized.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

@Table("tb_type_profile")
data class TypeProfile(
    var enabled: Boolean? = null,
    var score: Int? = null,
    var payload: ByteArray? = null,
    var createdAt: LocalDateTime? = null,
) : KPojo
```

For this model, Kronos selects the following column types:

| Property | Inferred `KColumnType` |
|----------|-------------------------|
| `enabled` | `BIT` |
| `score` | `INT` |
| `payload` | `BLOB` |
| `createdAt` | `DATETIME` |

MySQL renders those columns as:

```sql name="MySQL" icon="mysql"
`enabled` TINYINT(1)
`score` INT(11)
`payload` BLOB
`createdAt` DATETIME
```

## Default mapping

Use this table as a compact reference for automatic mapping. The selected database dialect renders the final DDL type; see {{ $.keyword("mapping/column-type-reference", ["Column Type Reference"]) }} for dialect examples.

| Kotlin type | Inferred `KColumnType` |
|-------------|-------------------------|
| `kotlin.Boolean` | `BIT` |
| `kotlin.Byte` | `TINYINT` |
| `kotlin.Short` | `SMALLINT` |
| `kotlin.Int` | `INT` |
| `kotlin.Long` | `BIGINT` |
| `kotlin.Float` | `FLOAT` |
| `kotlin.Double` | `DOUBLE` |
| `java.math.BigDecimal` | `DECIMAL` |
| `kotlin.Char` | `CHAR` |
| `kotlin.String` | `VARCHAR` |
| `java.util.Date`, `java.sql.Date`, `java.time.LocalDate`, `kotlinx.datetime.LocalDate` | `DATE` |
| `java.time.LocalTime`, `kotlinx.datetime.LocalTime` | `TIME` |
| `java.time.LocalDateTime`, `kotlinx.datetime.LocalDateTime` | `DATETIME` |
| `java.sql.Timestamp` | `TIMESTAMP` |
| `kotlin.ByteArray` | `BLOB` |
| Scalar enum | `VARCHAR` |
| `@Serialize` property | `VARCHAR` unless `@ColumnType` overrides it |
| Other property types | `VARCHAR` |

Scalar enums use `Enum.name` for string-compatible columns. An integer {{ $.annotation("ColumnType") }} stores the enum position. Use a custom mapping for a business code, and see {{ $.keyword("mapping/enum-serialization", ["Enum Fields"]) }} for examples.

## Override with {{ $.annotation("ColumnType") }}

Use `@ColumnType` to select the column type for one property.

```kotlin name="kotlin" icon="kotlin" {8,10}
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

@Table("tb_override_type")
data class OverrideType(
    @ColumnType(KColumnType.TEXT)
    var bio: String? = null,
    @ColumnType(KColumnType.UUID)
    var externalId: String? = null,
) : KPojo
```

For this model, the selected column types are:

| Property | `KColumnType` |
|----------|----------------|
| `bio` | `TEXT` |
| `externalId` | `UUID` |

PostgreSQL renders them as:

```sql name="PostgreSQL" icon="postgres"
"bio" TEXT
"externalId" UUID
```

## Set `length` and `scale`

The `@ColumnType` annotation also accepts `length` and `scale`. String-like types use `length`; decimal types use `length` and `scale`.

```kotlin name="kotlin" icon="kotlin" {9,11}
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.math.BigDecimal

@Table("tb_invoice_line")
data class InvoiceLine(
    @ColumnType(KColumnType.VARCHAR, length = 80)
    var title: String? = null,
    @ColumnType(KColumnType.DECIMAL, length = 12, scale = 2)
    var amount: BigDecimal? = null,
) : KPojo
```

DDL type fragments:

| Dialect | `title` | `amount` |
|---------|---------|----------|
| MySQL | `VARCHAR(80)` | `DECIMAL(12,2)` |
| PostgreSQL | `VARCHAR(80)` | `DECIMAL(12,2)` |
| SQLite | `TEXT` | `NUMERIC` |
| H2 | `VARCHAR(80)` | `NUMERIC(12,2)` |
| SQLServer | `VARCHAR(80)` | `DECIMAL(12,2)` |
| Oracle | `VARCHAR2(80)` | `NUMBER(12,2)` |
| DM8 | `VARCHAR2(80)` | `NUMBER(12,2)` |

## Use a native JSON column

With the Gson or Kotlinx Serialization setup shown on {{ $.keyword("mapping/serialization", ["Serialization"]) }}, {{ $.annotation("Serialize") }} properties are encoded as JSON text. Add {{ $.annotation("ColumnType") }} with `KColumnType.JSON` when the database table should use a native JSON column.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

data class AuditPayload(val ip: String, val tags: List<String>)

@Table("tb_audit_event")
data class AuditEvent(
    @ColumnType(KColumnType.JSON)
    @Serialize
    var payload: AuditPayload? = null,
) : KPojo
```

When the registered JSON configuration saves `AuditPayload("127.0.0.1", listOf("login"))`, the insert parameter is:

```text name="result"
payload -> {"ip":"127.0.0.1","tags":["login"]}
```

DDL type fragments:

| Dialect | `payload` |
|---------|-----------|
| MySQL | `JSON` |
| PostgreSQL | `JSONB` |
| SQLite | `TEXT` |
| H2 | `JSON` |
| SQLServer | `JSON` |
| Oracle | `JSON` |
| DM8 | `JSON` |

> **Note**
> The registered JSON configuration sends `payload` as a JSON string through JDBC. {{ $.annotation("ColumnType") }} selects the type used in generated table DDL. Use the default `VARCHAR` column for JSON text tables.
