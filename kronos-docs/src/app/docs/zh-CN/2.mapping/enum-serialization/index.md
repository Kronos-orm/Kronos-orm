{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 标量 enum 字段

Kronos 会保留 enum 属性的逻辑 `KType`。没有指定列类型时，标量 enum 默认推断为 `VARCHAR`，内置 enum codec 使用 `Enum.name` 写入，并通过编译器生成的 enum metadata 恢复值。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class Account(
    var status: Status? = null,
) : KPojo
```

该字段的语义是：

| 逻辑类型 | 物理列类型 | 内置值 |
|----------|------------|--------|
| `Status?` | `VARCHAR` | `"READY"` 或 `"CLOSED"` |

默认匹配区分大小写。Kronos 不使用 `toString()`、ordinal 或反射查找 enum。

## 整数列与 ordinal

没有其他 codec 覆盖时，显式整数 enum 列使用 enum ordinal。整数列仍然只是物理 `KColumnType`，字段的逻辑类型仍然是 `Status`。

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

内置映射为：

```text
Status.READY  <-> 0
Status.CLOSED <-> 1
```

支持的整数列族包括 `TINYINT`、`SMALLINT`、`INT`、`MEDIUMINT`、`SERIAL` 和 `BIGINT`。解码接受 JDBC 的整数 `Number`，会检查范围；未知或越界值会抛出 `ValueMappingException`。

ordinal 依赖 enum 声明顺序。如果数据库值必须在插入或调整 enum entry 后保持稳定，应使用稳定的业务 code 覆盖内置规则。

## 自定义 code 或 label

code 和 label 使用与其他自定义值相同的 `ValueCodec` 注册机制。后注册的 codec 优先级高于内置 name 或 ordinal 规则。

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

同一个 enum 在不同属性使用不同协议时，应匹配字段名或列名。String `label` 映射只需使用相同结构，在编码时返回 `Status.label`，解码时匹配数据库字符串。不需要 enum 专用 registry 或 factory 注册。

## `List<Enum>` 序列化

`@Serialize` 标记完整值的存储协议。持久化 `List<Status>` 这类集合时需要它；标量 `Status` 属性不需要 `@Serialize`。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class AccountHistory(
    @Serialize
    var statuses: List<Status>? = null,
) : KPojo
```

该字段表示为：

| 逻辑类型 | 默认物理列 | 存储协议 | 转换方式 |
|----------|------------|----------|----------|
| `List<Status>?` | `VARCHAR` | `SERIALIZED` | 整个集合一次转换为文本，或从文本恢复 |

注册一个处理文本格式的 serialized `ValueCodec`。Kronos 会把完整的 `List<Status>` `KType` 交给它，不会逐元素调用标量 enum codec。可以使用 `@ColumnType(TEXT)` 或其他字符串类型覆盖 DDL 列类型。

## 转换边界

- typed JDBC 结果、safe Map 映射、delegate、ORM 参数和 `IN` 参数都会进入 registry。
- 标量 enum 的 `IN` 列表会逐元素使用字段的 name、ordinal 或自定义 code 规则。
- serialized `List<Status>` 作为完整值只转换一次。
- raw `mapperTo`、`fromMapData` 和 `patchTo` 仍然是直接赋值，不调用 codec。

统一注册契约见 {{ $.keyword("configuration/value-codec", ["ValueCodec"]) }}；serialized 文本 codec 见 {{ $.keyword("mapping/serialization", ["序列化"]) }}。
