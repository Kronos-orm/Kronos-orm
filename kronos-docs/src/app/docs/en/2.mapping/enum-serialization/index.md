{% import "../../../macros/macros-en.njk" as $ %}

## Use enums in a model

Enum properties keep their Kotlin type and work with the usual select, insert, update, and condition APIs. A scalar enum property uses a `VARCHAR` column by default and stores the entry name.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class Account(
    var status: Status? = null,
) : KPojo
```

| Kotlin property | Default column | Stored value |
|-----------------|----------------|--------------|
| `Status?` | `VARCHAR` | `"READY"` or `"CLOSED"` |

This suits tables whose stored value follows the enum entry name.

## Store a numeric position

Choose an integer column when an existing schema stores the position of an enum entry. The property remains a `Status` in Kotlin.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class LegacyAccount(
    @ColumnType(KColumnType.INT)
    var status: Status? = null,
) : KPojo
```

```text
Status.READY  <-> 0
Status.CLOSED <-> 1
```

Enum positions follow declaration order. A business code gives the database a stable value chosen by the application.

## Store a business code

Register a conversion rule when the database stores a business code. This example saves `Status.READY` as `10` and restores the same enum when an account is read.

```kotlin name="model" icon="kotlin"
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

enum class Status(val code: Int) {
    READY(10),
    CLOSED(20),
}

data class Account(
    @ColumnType(KColumnType.INT)
    var status: Status? = null,
) : KPojo
```

```kotlin name="startup" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.interfaces.valueCodec

Kronos.registerValueCodec(
    valueCodec(
        supports = { value, context ->
            context.targetType.classifier == Status::class &&
                when (context.direction) {
                    ValueCodecDirection.ENCODE -> value is Status
                    ValueCodecDirection.DECODE -> value is Number
                }
        },
        convert = { value, context ->
            when (context.direction) {
                ValueCodecDirection.ENCODE -> (value as Status).code
                ValueCodecDirection.DECODE -> Status.entries.first { entry ->
                    entry.code == (value as Number).toInt()
                }
            }
        }
    )
)
```

The rule matches `Status` properties. It writes the `code` and restores the matching enum entry. See {{ $.keyword("configuration/value-codec", ["Custom Value Mapping"]) }} for the same pattern with a `Money` value.

## Store an enum collection

Use {{ $.annotation("Serialize") }} for a collection such as `List<Status>`. Configure Gson or Kotlinx Serialization once; the list is written as JSON text in a `VARCHAR` column while application code reads and writes a Kotlin list.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class AccountHistory(
    @Serialize
    var statuses: List<Status>? = null,
) : KPojo
```

For example, a JSON library can save the property as `["READY","CLOSED"]`. Configure Gson or Kotlinx Serialization on {{ $.keyword("mapping/serialization", ["Serialization"]) }}.
