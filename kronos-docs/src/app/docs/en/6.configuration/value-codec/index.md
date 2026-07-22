{% import "../../../macros/macros-en.njk" as $ %}

## Register one conversion mechanism

`ValueCodec` is the only public value-conversion extension. Safe Map mapping, typed JDBC results, ORM parameters, and serialized delegates all create a `ValueCodecContext` and select one matching codec.

| Context value | Meaning |
|---------------|---------|
| `direction` | `ENCODE` prepares a logical Kotlin value for JDBC; `DECODE` restores a logical target. |
| `origin` | `MAP`, `DATABASE`, `DELEGATE`, or `PARAMETER`; use it only when a codec needs a narrower boundary. |
| `sourceType` | Complete declared source `KType` when known; otherwise a runtime star-projected fallback. |
| `targetType` | Complete logical Kotlin `KType`, including generic arguments and nullability. |
| `field` | Optional field metadata such as `dateFormat`. |
| `storage` | `NONE` for scalar conversion or `SERIALIZED` for serialized text. |

Registry null handling runs before codecs, so `supports` and `convert` receive non-null values. Later registrations are checked first. Built-in enum, temporal, basic, and identity rules run after user codecs for `storage = NONE`.

## Add a custom type

Use `valueCodec` to define both directions, then register it once. This example stores `Money` as `BigDecimal`.

```kotlin group="Money codec" name="registration" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueStorage
import com.kotlinorm.interfaces.valueCodec
import java.math.BigDecimal
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

data class Money(val amount: BigDecimal)
private val moneyType = typeOf<Money>()

val moneyRegistration = Kronos.registerValueCodec(
    valueCodec(
        supports = { value, context ->
            context.storage == ValueStorage.NONE &&
                context.targetType.withNullability(false) == moneyType &&
                !(context.direction == ValueCodecDirection.DECODE && value is Money)
        },
        convert = { value, context ->
            when (context.direction) {
                ValueCodecDirection.ENCODE -> (value as Money).amount
                ValueCodecDirection.DECODE -> Money(value.toString().toBigDecimal())
            }
        }
    )
)
```

Codec exceptions are wrapped as `ValueMappingException` with direction, origin, field/column, target type, and batch index when available. A DECODE result must be assignable to the complete target type; an ENCODE result must be JDBC-bindable.

## Control priority and lifetime

A later matching registration overrides earlier user and built-in behavior. `close()` is idempotent and removes only its own registration; requests already using a registry snapshot finish with that snapshot.

```kotlin group="Codec lifetime" name="close" icon="kotlin"
val registration = Kronos.registerValueCodec(customCodec)

// Tests or hot reload only:
registration.close()
```

## Enum storage

Scalar enums are built in. Without a column override, Kronos infers `VARCHAR` and stores `Enum.name`. An explicitly integer `@ColumnType` uses ordinal values unless a later user `ValueCodec` overrides that field. String code/label mappings also use the normal `Kronos.registerValueCodec` entry point.

`@Serialize List<Status>` is a different protocol: the complete list is passed once to the serialized codec, with its complete `KType`. It never invokes the scalar enum codec for each element.

See {{ $.keyword("mapping/enum-serialization", ["Enum Storage and Serialization"]) }} for the column-type matrix, ordinal behavior, code/label override, and collection boundaries.

## Date and strict-mode behavior

The built-in temporal codec uses an explicit prepared format first, then `Field.dateFormat`, then `Kronos.defaultDateFormat`. `Kronos.timeZone` is used when conversion crosses instant and local date/time semantics. Native JDBC temporal conversions avoid a text round trip.

`Kronos.strictSetValue = true` disables implicit basic and temporal DECODE coercion. It does not disable registered user codecs, serialized storage, enum decoding, or required parameter encoding.

Raw `mapperTo` and `fromMapData` perform direct assignment and do not invoke the registry. Use `safeMapperTo`, `safeFromMapData`, typed queries, or normal ORM parameters when conversion is required.
