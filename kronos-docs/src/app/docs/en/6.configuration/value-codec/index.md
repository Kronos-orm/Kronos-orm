{% import "../../../macros/macros-en.njk" as $ %}

## Store a domain value in one column

Use custom value mapping when a model property uses a small domain class while its database column stores a scalar value. In this example, an invoice uses {{ $.code("Money") }} in Kotlin and stores the amount in a `DECIMAL` column.

## Define the model

```kotlin name="model" icon="kotlin"
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.math.BigDecimal

data class Money(val amount: BigDecimal)

@Table("tb_invoice")
data class Invoice(
    var id: Int? = null,
    @ColumnType(KColumnType.DECIMAL)
    var total: Money? = null,
) : KPojo
```

## Add the conversion

Add one conversion rule for `Money` that works while saving and reading. Kronos calls this rule a {{ $.code("valueCodec") }}; register it once during application startup. The rule below handles `Money` properties: it saves the amount and rebuilds `Money` when an invoice is read.

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
                    ValueCodecDirection.DECODE -> value is Number || value is String
                }
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

The rule matches model properties declared as `Money`. `ENCODE` runs while saving and `DECODE` runs while reading.

## Use Money normally

After registration, application code creates and queries `Invoice` with `Money` values.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select

val invoice = Invoice(total = Money("19.99".toBigDecimal()))
invoice.insert().execute()

val loaded = Invoice()
    .select()
    .where { it.id == 1 }
    .first()

println(loaded.total?.amount)
```

## Choose the mapping

| Model value | Database value | Guide |
|-------------|----------------|-------|
| `Money` | `DECIMAL` | This page |
| A settings object or `List<String>` | JSON text | {{ $.keyword("mapping/serialization", ["Serialization"]) }} |
| `Status` or another enum | Name, position, or business code | {{ $.keyword("mapping/enum-serialization", ["Enum Fields"]) }} |
