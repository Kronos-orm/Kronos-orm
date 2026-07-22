{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 将领域值保存到单个列

自定义值映射让 Kotlin 保留领域值，同时以数据库易处理的值保存到一个列。下面的发票使用 {{ $.code("Money") }}，并以整数分值写入 `BIGINT` 列。

## 定义模型

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

## 注册转换规则

{{ $.code("valueCodec") }} 用一对转换函数说明模型值和列值之间的往返转换。在应用启动时注册下面的 `Money` 规则。`supports` 选择 `Money` 属性，`convert` 在保存时写入分值、读取时重建 `Money`。保存时方向为 `ENCODE`，读取时方向为 `DECODE`。

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

类型判断会让这条规则只用于声明为 `Money` 的属性。例如，`Money(1_999)` 在列中保存为 `1999`。

## 像普通值一样使用 Money

注册完成后，业务代码可以直接用 `Money` 创建和查询 `Invoice`。

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

## 选择映射方式

| 模型中的值 | 数据库中的值 | 参考页面 |
|------------|--------------|----------|
| `Money` | `BIGINT` 分值 | 本页 |
| 设置对象或 `List<String>` | JSON 文本 | {{ $.keyword("mapping/serialization", ["序列化"]) }} |
| `Status` 等枚举 | 名称、位置或业务 code | {{ $.keyword("mapping/enum-serialization", ["枚举字段"]) }} |
