{% import "../../../macros/macros-zh-CN.njk" as $ %}

## Kotlin 类型推断

属性未使用{{ $.keyword("mapping/annotations", ["注解设置", "@ColumnType列类型及长度"]) }}时，Kronos 会为每个持久化的 `KPojo` 属性推断一个 `KColumnType`。推断结果可以从 `__columns` 看到，也会进入表结构 DDL。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

@Table("tb_type_profile")
data class TypeProfile(
    var enabled: Boolean? = null,
    var score: Int? = null,
    var payload: ByteArray? = null,
    var createdAt: LocalDateTime? = null,
) : KPojo

val types = TypeProfile().__columns.associate { it.name to it.type }
```

结果：

| 属性 | 推断出的 `KColumnType` |
|------|------------------------|
| `enabled` | `BIT` |
| `score` | `INT` |
| `payload` | `BLOB` |
| `createdAt` | `DATETIME` |

MySQL DDL 类型片段：

```sql name="MySQL" icon="mysql"
`enabled` TINYINT(1)
`score` INT(11)
`payload` BLOB
`createdAt` DATETIME
```

## 默认映射

以下表格用于速查自动映射关系。最终 DDL 类型由当前数据库方言渲染；方言示例见{{ $.keyword("mapping/column-type-reference", ["KColumnType"]) }}。

| Kotlin 类型 | 推断出的 `KColumnType` |
|-------------|------------------------|
| `kotlin.Boolean` | `BIT` |
| `kotlin.Byte` | `TINYINT` |
| `kotlin.Short` | `SMALLINT` |
| `kotlin.Int` | `INT` |
| `kotlin.Long` | `BIGINT` |
| `kotlin.Float` | `FLOAT` |
| `kotlin.Double` | `DOUBLE` |
| `java.math.BigDecimal` | `DECIMAL` |
| `kotlin.Char` | `CHAR` |
| `kotlin.String` | `VARCHAR` |
| `java.util.Date`、`java.sql.Date`、`java.time.LocalDate`、`kotlinx.datetime.LocalDate` | `DATE` |
| `java.time.LocalTime`、`kotlinx.datetime.LocalTime` | `TIME` |
| `java.time.LocalDateTime`、`kotlinx.datetime.LocalDateTime` | `DATETIME` |
| `java.sql.Timestamp` | `TIMESTAMP` |
| `kotlin.ByteArray` | `BLOB` |
| 标量 enum | `VARCHAR` |
| `@Serialize` 属性 | `VARCHAR`，除非由 `@ColumnType` 覆盖 |
| 其他属性类型 | `VARCHAR` |

标量 enum 在字符串类列中使用 `Enum.name`。显式整数 `@ColumnType` 使用 ordinal，除非用户为该字段注册了更晚的 `ValueCodec` 覆盖。`@Serialize` 把完整逻辑值存为文本，不会逐元素转换集合。完整规则见 {{ $.keyword("mapping/enum-serialization", ["Enum 存储与序列化"]) }}。

## 使用 {{ $.annotation("ColumnType") }} 覆盖

使用 `@ColumnType` 可以为单个属性指定列类型。注解中的类型会写入 `__columns`，覆盖自动推断结果。

```kotlin name="kotlin" icon="kotlin" {8,10}
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

@Table("tb_override_type")
data class OverrideType(
    @ColumnType(KColumnType.TEXT)
    var bio: String? = null,
    @ColumnType(KColumnType.UUID)
    var externalId: String? = null,
) : KPojo

val columns = OverrideType().__columns.associate { it.name to it.type }
```

结果：

| 属性 | `KColumnType` |
|------|---------------|
| `bio` | `TEXT` |
| `externalId` | `UUID` |

PostgreSQL DDL 类型片段：

```sql name="PostgreSQL" icon="postgres"
"bio" TEXT
"externalId" UUID
```

## 设置 `length` 和 `scale`

`@ColumnType` 还可以设置 `length` 和 `scale`。字符串类类型使用 `length`；精确数值类型使用 `length` 和 `scale`。

```kotlin name="kotlin" icon="kotlin" {9,11}
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.math.BigDecimal

@Table("tb_invoice_line")
data class InvoiceLine(
    @ColumnType(KColumnType.VARCHAR, length = 80)
    var title: String? = null,
    @ColumnType(KColumnType.DECIMAL, length = 12, scale = 2)
    var amount: BigDecimal? = null,
) : KPojo
```

DDL 类型片段：

| 方言 | `title` | `amount` |
|------|---------|----------|
| MySQL | `VARCHAR(80)` | `DECIMAL(12,2)` |
| PostgreSQL | `VARCHAR(80)` | `DECIMAL(12,2)` |
| SQLite | `TEXT` | `NUMERIC` |
| SQLServer | `VARCHAR(80)` | `DECIMAL(12,2)` |
| Oracle | `VARCHAR2(80)` | `NUMBER(12,2)` |

## 搭配 JSON 和序列化

`@Serialize` 负责值转换，`@ColumnType(KColumnType.JSON)` 负责 DDL 列类型。注册一个序列化 `ValueCodec`，即可依据每个字段完整的声明 `KType` 编码和解码所有序列化字段。

```kotlin name="kotlin" icon="kotlin" {13-16,22,23}
import com.google.gson.Gson
import com.kotlinorm.Kronos
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.serializedValueCodec
import kotlin.reflect.jvm.javaType

data class AuditPayload(val ip: String, val tags: List<String>)

val gson = Gson()
val gsonRegistration = Kronos.registerValueCodec(
    serializedValueCodec(
        encode = { value, _ -> gson.toJson(value) },
        decode = { text, type -> gson.fromJson(text, type.javaType) }
    )
)

@Table("tb_audit_event")
data class AuditEvent(
    @ColumnType(KColumnType.JSON)
    @Serialize
    var payload: AuditPayload? = null,
) : KPojo
```

插入 `AuditPayload("127.0.0.1", listOf("login"))` 时的参数结果：

```text name="result"
payload -> {"ip":"127.0.0.1","tags":["login"]}
```

DDL 类型片段：

| 方言 | `payload` |
|------|-----------|
| MySQL | `JSON` |
| PostgreSQL | `JSONB` |
| SQLite | `TEXT` |
| SQLServer | `JSON` |
| Oracle | `JSON` |

> **Note**
> `@Serialize` 使用{{ $.keyword("configuration/value-codec", ["ValueCodec"]) }}中介绍的 codec。`@ColumnType` 控制表操作渲染出的数据库类型。局部覆盖不再需要时调用 `gsonRegistration.close()`；后注册的 codec 优先匹配。

序列化字段的完整映射流程见 {{ $.keyword("mapping/serialization", ["序列化"]) }}。
