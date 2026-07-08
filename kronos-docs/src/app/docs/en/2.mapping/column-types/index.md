{% import "../../../macros/macros-en.njk" as $ %}

## Kotlin Type Inference

Kronos infers a `KColumnType` for each persisted `KPojo` property when the property does not use {{ $.keyword("mapping/annotations", ["Annotation Settings", "@ColumnType column type and length"]) }}. The inferred value is visible from `kronosColumns()` and is used by table DDL.

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

val types = TypeProfile().kronosColumns().associate { it.name to it.type }
```

Result:

| Property | Inferred `KColumnType` |
|----------|-------------------------|
| `enabled` | `BIT` |
| `score` | `INT` |
| `payload` | `BLOB` |
| `createdAt` | `DATETIME` |

MySQL DDL type fragments:

```sql name="MySQL" icon="mysql"
`enabled` TINYINT(1)
`score` INT(11)
`payload` BLOB
`createdAt` DATETIME
```

## Default Mapping

Use the table as a compact reference for the automatic mapping. The final DDL type is rendered by the selected database dialect; see {{ $.keyword("mapping/column-type-reference", ["KColumnType"]) }} for dialect examples.

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
| Other property types | `VARCHAR` |

## Override With {{ $.annotation("ColumnType") }}

Use `@ColumnType` to set the column type for one property. The annotation value replaces the inferred type in `kronosColumns()`.

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

val columns = OverrideType().kronosColumns().associate { it.name to it.type }
```

Result:

| Property | `KColumnType` |
|----------|----------------|
| `bio` | `TEXT` |
| `externalId` | `UUID` |

PostgreSQL DDL type fragments:

```sql name="PostgreSQL" icon="postgres"
"bio" TEXT
"externalId" UUID
```

## Set `length` And `scale`

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
| SQLServer | `VARCHAR(80)` | `DECIMAL(12,2)` |
| Oracle | `VARCHAR2(80)` | `NUMBER(12,2)` |

## Combine JSON And Serialization

Use `@Serialize` for value conversion and `@ColumnType(KColumnType.JSON)` for the DDL type. `Kronos.serializeProcessor` supplies the serializer used when writing and reading the property.

```kotlin name="kotlin" icon="kotlin" {20,24,25}
import com.google.gson.Gson
import com.kotlinorm.Kronos
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosSerializeProcessor
import kotlin.reflect.KClass

data class AuditPayload(val ip: String, val tags: List<String>)

object GsonProcessor : KronosSerializeProcessor {
    private val gson = Gson()
    override fun serialize(obj: Any): String = gson.toJson(obj)
    override fun deserialize(serializedStr: String, kClass: KClass<*>): Any =
        gson.fromJson(serializedStr, kClass.java)
}

Kronos.serializeProcessor = GsonProcessor

@Table("tb_audit_event")
data class AuditEvent(
    @ColumnType(KColumnType.JSON)
    @Serialize
    var payload: AuditPayload? = null,
) : KPojo
```

Parameter result when inserting `AuditPayload("127.0.0.1", listOf("login"))`:

```text name="result"
payload -> {"ip":"127.0.0.1","tags":["login"]}
```

DDL type fragments:

| Dialect | `payload` |
|---------|-----------|
| MySQL | `JSON` |
| PostgreSQL | `JSONB` |
| SQLite | `TEXT` |
| SQLServer | `JSON` |
| Oracle | `JSON` |

> **Note**
> `@Serialize` uses the processor described in {{ $.keyword("configuration/serialization-processor", ["Serialize Processor"]) }}. `@ColumnType` controls the database type rendered by table operations.

For the complete serialized-field mapping flow, see {{ $.keyword("mapping/serialization", ["Serialization"]) }}.
