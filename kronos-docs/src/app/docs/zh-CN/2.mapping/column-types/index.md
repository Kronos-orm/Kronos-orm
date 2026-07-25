{% import "../../../macros/macros-zh-CN.njk" as $ %}

## Kotlin 类型推断

属性未使用 {{ $.keyword("mapping/annotations", ["注解"]) }} 时，Kronos 会根据每个持久化 `KPojo` 属性选择列类型。建表或同步表结构时，当前数据库方言会渲染最终的 DDL 类型。

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
```

该模型会选择以下列类型：

| 属性 | 推断出的 `KColumnType` |
|------|------------------------|
| `enabled` | `BIT` |
| `score` | `INT` |
| `payload` | `BLOB` |
| `createdAt` | `DATETIME` |

MySQL 会将这些列渲染为：

```sql name="MySQL" icon="mysql"
`enabled` TINYINT(1)
`score` INT(11)
`payload` BLOB
`createdAt` DATETIME
```

## 默认映射

以下表格用于速查自动映射关系。当前数据库方言会渲染最终 DDL 类型；方言示例见 {{ $.keyword("mapping/column-type-reference", ["列类型参考"]) }}。

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

标量 enum 在字符串类列中使用 `Enum.name`。整数 {{ $.annotation("ColumnType") }} 保存枚举位置；数据库保存业务 code 时使用自定义映射。示例见 {{ $.keyword("mapping/enum-serialization", ["枚举字段"]) }}。

## 使用 {{ $.annotation("ColumnType") }} 覆盖

使用 `@ColumnType` 可以为单个属性选择列类型。

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
```

该模型会选择以下列类型：

| 属性 | `KColumnType` |
|------|---------------|
| `bio` | `TEXT` |
| `externalId` | `UUID` |

PostgreSQL 会将它们渲染为：

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
| H2 | `VARCHAR(80)` | `NUMERIC(12,2)` |
| SQLServer | `VARCHAR(80)` | `DECIMAL(12,2)` |
| Oracle | `VARCHAR2(80)` | `NUMBER(12,2)` |
| DM8 | `VARCHAR2(80)` | `NUMBER(12,2)` |

## 使用原生 JSON 列

按 {{ $.keyword("mapping/serialization", ["序列化"]) }} 配置 Gson 或 Kotlinx Serialization 后，{{ $.annotation("Serialize") }} 属性会编码为 JSON 文本。数据库表需要原生 JSON 列时，使用 {{ $.annotation("ColumnType") }} 指定 `KColumnType.JSON`。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

data class AuditPayload(val ip: String, val tags: List<String>)

@Table("tb_audit_event")
data class AuditEvent(
    @ColumnType(KColumnType.JSON)
    @Serialize
    var payload: AuditPayload? = null,
) : KPojo
```

注册的 JSON 配置保存 `AuditPayload("127.0.0.1", listOf("login"))` 时，插入参数为：

```text name="result"
payload -> {"ip":"127.0.0.1","tags":["login"]}
```

DDL 类型片段：

| 方言 | `payload` |
|------|-----------|
| MySQL | `JSON` |
| PostgreSQL | `JSONB` |
| SQLite | `TEXT` |
| H2 | `JSON` |
| SQLServer | `JSON` |
| Oracle | `JSON` |
| DM8 | `JSON` |

> **Note**
> 注册的 JSON 配置会将 `payload` 作为 JSON 字符串通过 JDBC 写入。{{ $.annotation("ColumnType") }} 用于生成表结构中的列类型。保存 JSON 文本的表使用默认 `VARCHAR` 列即可。
