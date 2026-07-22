{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 将领域值保存到单个列

模型属性使用小型领域类、数据库列保存标量值时，可以使用自定义值映射。下面的发票在 Kotlin 中使用 {{ $.code("Money") }}，在数据库中使用 `DECIMAL` 保存金额。

## 定义模型

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

## 添加转换规则

为 `Money` 添加一条保存和读取时都可使用的转换规则。Kronos 将这类规则称为 {{ $.code("valueCodec") }}，在应用启动时注册一次即可。下面的规则只处理 `Money` 属性：保存发票时取出金额，读取时重新创建 `Money`。

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

这条规则按模型属性的 `Money` 类型匹配。`ENCODE` 用于保存，`DECODE` 用于读取。

## 像普通值一样使用 Money

注册完成后，业务代码可以直接用 `Money` 创建和查询 `Invoice`。

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

## 选择映射方式

| 模型中的值 | 数据库中的值 | 参考页面 |
|------------|--------------|----------|
| `Money` | `DECIMAL` | 本页 |
| 设置对象或 `List<String>` | JSON 文本 | {{ $.keyword("mapping/serialization", ["序列化"]) }} |
| `Status` 等枚举 | 名称、位置或业务 code | {{ $.keyword("mapping/enum-serialization", ["枚举字段"]) }} |
