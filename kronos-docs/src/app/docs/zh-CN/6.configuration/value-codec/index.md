{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 注册唯一转换机制

`ValueCodec` 是唯一公开值转换扩展点。safe Map 映射、typed JDBC 结果、ORM 参数和 serialized delegate 都会创建 `ValueCodecContext`，并选择一个匹配的 codec。

| 上下文值 | 含义 |
|----------|------|
| `direction` | `ENCODE` 把逻辑 Kotlin 值准备为 JDBC 值；`DECODE` 恢复逻辑目标值。 |
| `origin` | `MAP`、`DATABASE`、`DELEGATE` 或 `PARAMETER`；仅在需要细粒度边界时匹配。 |
| `sourceType` | 已知时为完整声明源 `KType`，否则为运行时 star-projected fallback。 |
| `targetType` | 完整逻辑 Kotlin `KType`，包含泛型参数与可空性。 |
| `field` | 可选字段 metadata，例如 `dateFormat`。 |
| `storage` | 标量转换使用 `NONE`，序列化文本使用 `SERIALIZED`。 |

Registry 会先处理 null，因此 `supports` 和 `convert` 只接收非空值。后注册的 codec 先匹配。对于 `storage = NONE`，用户 codec 之后才是内置 enum、temporal、basic 和 identity 规则。

## 添加自定义类型

用 `valueCodec` 定义两个方向，再注册一次。下面把 `Money` 存储为 `BigDecimal`。

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

codec 抛出的异常会包装为 `ValueMappingException`，并尽可能携带 direction、origin、字段/列、目标类型和 batch index。DECODE 结果必须能赋值给完整目标类型；ENCODE 结果必须能被 JDBC 绑定。

## 控制优先级和生命周期

后注册的匹配项会覆盖更早的用户行为和内置行为。`close()` 幂等且只注销自身；已经取得 registry snapshot 的请求仍会使用原 snapshot 完成。

```kotlin group="Codec lifetime" name="close" icon="kotlin"
val registration = Kronos.registerValueCodec(customCodec)

// 仅用于测试或热更新：
registration.close()
```

## Enum 存储

标量 enum 已内置处理。没有覆盖列类型时，Kronos 推断为 `VARCHAR` 并存储 `Enum.name`。显式整数 `@ColumnType` 使用 ordinal；如果用户为该字段注册了更晚的 `ValueCodec`，则由用户 codec 覆盖。String code/label 也使用普通的 `Kronos.registerValueCodec` 入口。

`@Serialize List<Status>` 是另一种协议：完整集合和完整 `KType` 只交给 serialized codec 一次，不会逐元素调用标量 enum codec。

列类型矩阵、ordinal、code/label 覆盖和集合边界见 {{ $.keyword("mapping/enum-serialization", ["Enum 存储与序列化"]) }}。

## 日期与 strict 模式

内置 temporal codec 的格式优先级为：显式 prepared format、`Field.dateFormat`、`Kronos.defaultDateFormat`。跨 instant 与本地日期时间语义时使用 `Kronos.timeZone`；原生 JDBC temporal 转换不会无意义地绕经文本。

`Kronos.strictSetValue = true` 会关闭隐式 basic/temporal DECODE coercion，但不会关闭用户 codec、serialized storage、enum 解码或参数写入所需的 ENCODE。

raw `mapperTo` 和 `fromMapData` 只直接赋值，不调用 registry。需要转换时使用 `safeMapperTo`、`safeFromMapData`、typed 查询或正常 ORM 参数。
