{% import "../../../macros/macros-en.njk" as $ %}

## Store a domain value in one column

Custom value mapping keeps a domain value in Kotlin while saving one database-friendly value in a column. This example stores {{ $.code("Money") }} as whole cents in a `BIGINT` column.

## Define the model

```kotlin name="model" icon="kotlin"
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

data class Money(val cents: Long)

@Table("tb_invoice")
data class Invoice(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    @ColumnType(KColumnType.BIGINT)
    var total: Money? = null,
) : KPojo
```

## Register the conversion

A {{ $.code("valueCodec") }} describes how a value travels between a model and its column. Register this `Money` conversion during application startup. `supports` selects `Money` properties, and `convert` writes cents or rebuilds `Money`. The direction is `ENCODE` while saving and `DECODE` while reading.

```kotlin name="startup" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.interfaces.valueCodec

Kronos.registerValueCodec(
    valueCodec(
        supports = { value, context ->
            context.targetType.classifier == Money::class &&
                when (context.direction) {
                    ValueCodecDirection.ENCODE -> value is Money
                    ValueCodecDirection.DECODE -> value is Number
                }
        },
        convert = { value, context ->
            when (context.direction) {
                ValueCodecDirection.ENCODE -> (value as Money).cents
                ValueCodecDirection.DECODE -> Money((value as Number).toLong())
            }
        }
    )
)
```

The type check keeps the conversion on properties declared as `Money`. For example, `Money(1_999)` is stored as `1999`.

## Use Money normally

After registration, application code creates and queries `Invoice` with `Money` values.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select

val invoiceId = requireNotNull(
    Invoice(total = Money(1_999))
        .insert()
        .withId()
        .execute()
        .lastInsertId
)

val loaded = Invoice()
    .select()
    .where { it.id == invoiceId }
    .first()

println(loaded.total?.cents)
```

## Choose the mapping

| Model value | Database value | Guide |
|-------------|----------------|-------|
| `Money` | `BIGINT` cents | This page |
| A settings object or `List<String>` | JSON text | {{ $.keyword("mapping/serialization", ["Serialization"]) }} |
| `Status` or another enum | Name, position, or business code | {{ $.keyword("mapping/enum-serialization", ["Enum Fields"]) }} |
