{% import "../../../macros/macros-en.njk" as $ %}

## Scalar enum fields

Kronos keeps an enum property's logical `KType` as the enum type and infers `VARCHAR` when no column type is specified. The built-in enum codec stores `Enum.name` and restores the value through compiler-generated enum metadata.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class Account(
    var status: Status? = null,
) : KPojo
```

The field has these semantics:

| Logical type | Physical column | Built-in value |
|--------------|-----------------|----------------|
| `Status?` | `VARCHAR` | `"READY"` or `"CLOSED"` |

The default is case-sensitive `Enum.name`. Kronos does not use `toString()`, ordinal values, or reflective enum lookup for this mapping.

## Integer columns and ordinal values

Without another codec, an explicitly integer enum column uses the enum ordinal. The integer column type is still a physical `KColumnType`; the logical field type remains `Status`.

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

The built-in mapping is:

```text
Status.READY  <-> 0
Status.CLOSED <-> 1
```

The supported integer column families include `TINYINT`, `SMALLINT`, `INT`, `MEDIUMINT`, `SERIAL`, and `BIGINT`. Decode accepts integral JDBC `Number` values, checks the range, and reports an unknown or out-of-range value as a `ValueMappingException`.

Ordinal values are tied to enum declaration order. Use a stable custom code when database values must remain unchanged after entries are inserted or reordered.

## Custom code or label values

Code and label mappings use the same `ValueCodec` registration mechanism as every other custom value. A codec registered later has higher priority than the built-in name or ordinal rule.

```kotlin group="Enum code codec" name="registration" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.interfaces.valueCodec
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

enum class Status(val code: Int, val label: String) {
    READY(10, "ready"),
    CLOSED(20, "closed")
}

private val statusType = typeOf<Status>()

val statusRegistration = Kronos.registerValueCodec(
    valueCodec(
        supports = { value, context ->
            context.storage == ValueStorage.NONE &&
                context.field?.name == "status" &&
                context.field?.type == KColumnType.INT &&
                context.targetType.withNullability(false) == statusType &&
                ((context.direction == ValueCodecDirection.ENCODE && value is Status) ||
                    (context.direction == ValueCodecDirection.DECODE && value !is Status))
        },
        convert = { value, context ->
            when (context.direction) {
                ValueCodecDirection.ENCODE -> (value as Status).code
                ValueCodecDirection.DECODE -> when ((value as Number).toInt()) {
                    10 -> Status.READY
                    20 -> Status.CLOSED
                    else -> error("unknown Status code")
                }
            }
        }
    )
)
```

Match the field or column when one enum uses different protocols in different properties. A string `label` mapping uses the same shape but returns `Status.label` on encode and matches the stored string on decode. No enum-specific registry or factory registration is needed.

## `List<Enum>` serialization

`@Serialize` is a storage protocol for a complete value. It is needed for a persisted collection such as `List<Status>`, not for a scalar `Status` property.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class AccountHistory(
    @Serialize
    var statuses: List<Status>? = null,
) : KPojo
```

The field is represented as:

| Logical type | Default physical column | Storage protocol | Conversion |
|--------------|--------------------------|------------------|------------|
| `List<Status>?` | `VARCHAR` | `SERIALIZED` | one complete list value to/from text |

Register one serialized `ValueCodec` for the text format. Kronos passes the complete `List<Status>` `KType` to it and does not invoke the scalar enum codec for each element. `@ColumnType(TEXT)` or another string-compatible type can override the DDL column type.

## Conversion boundaries

- Typed JDBC results, safe Map mapping, delegates, ORM parameters, and `IN` parameters use the registry.
- An `IN` list of scalar enums is converted element by element using the field's name, ordinal, or custom code rule.
- A serialized `List<Status>` is converted once as a complete value.
- Raw `mapperTo`, `fromMapData`, and `patchTo` remain direct assignments and do not invoke codecs.

See {{ $.keyword("configuration/value-codec", ["Value Codec"]) }} for the common registration contract and {{ $.keyword("mapping/serialization", ["Serialization"]) }} for serialized text codecs.
