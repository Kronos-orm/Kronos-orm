{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 在模型中使用枚举

枚举属性保留 Kotlin 类型，可以和常规的查询、插入、更新和条件 API 一起使用。标量枚举属性默认使用 `VARCHAR` 列，并保存枚举项名称。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class Account(
    var status: Status? = null,
) : KPojo
```

| Kotlin 属性 | 默认列类型 | 保存的值 |
|-------------|------------|----------|
| `Status?` | `VARCHAR` | `"READY"` 或 `"CLOSED"` |

数据库值与枚举项名称一致时，使用这种方式。

## 使用整数位置

已有表使用枚举项位置保存状态时，可以选择整数列。Kotlin 属性仍然是 `Status`。

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

枚举位置按照声明顺序确定。业务 code 可以让数据库保存应用定义的稳定值。

## 保存业务 code

数据库保存业务 code 时，注册一条转换规则。下面的示例将 `Status.READY` 保存为 `10`，读取账户时再恢复为对应的枚举值。

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

这条规则按 `Status` 属性匹配。保存时取出 `code`，读取时恢复对应的枚举项。`Money` 的同类写法见 {{ $.keyword("configuration/value-codec", ["自定义值映射"]) }}。

## 保存枚举集合

`List<Status>` 等枚举集合使用 {{ $.annotation("Serialize") }}。配置一次 Gson 或 Kotlinx Serialization 后，列表会以 JSON 文本写入 `VARCHAR` 列，业务代码仍可直接读写 Kotlin 列表。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class AccountHistory(
    @Serialize
    var statuses: List<Status>? = null,
) : KPojo
```

例如，JSON 库可以将该属性保存为 `["READY","CLOSED"]`。Gson 或 Kotlinx Serialization 的配置见 {{ $.keyword("mapping/serialization", ["序列化"]) }}。
