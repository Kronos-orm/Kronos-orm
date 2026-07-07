{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 声明主键

在一个 `KPojo` 属性上使用 `@PrimaryKey` 声明主键。不设置生成策略参数时，字段映射为 `PrimaryKeyType.DEFAULT`。没有 `@PrimaryKey` 的字段映射为 `PrimaryKeyType.NOT`。

本页示例默认使用这些 import：

```kotlin group="PrimaryKey 1" name="model" icon="kotlin"
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.insert.insert

@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null
) : KPojo
```

生成的字段元数据如下：

```text group="PrimaryKey 2" name="metadata"
id.primaryKey -> PrimaryKeyType.IDENTITY
id.nullable -> false
username.primaryKey -> PrimaryKeyType.NOT
```

一个主键字段只使用一个生成策略参数：`identity`、`uuid`、`snowflake` 或 `custom`。

## PrimaryKeyType 速查

| 类型 | 注解写法 | 插入行为 | DDL 行为 |
|------|----------|----------|----------|
| `NOT` | 不使用 `@PrimaryKey` | 普通字段。 | 不是主键。 |
| `DEFAULT` | `@PrimaryKey` | 使用 KPojo 上已有的字段值。 | 主键列。 |
| `IDENTITY` | `@PrimaryKey(identity = true)` | 主键值为 `null` 时，从 insert 字段列表中移除。 | 数据库自增/identity 主键。 |
| `UUID` | `@PrimaryKey(uuid = true)` | 插入前写入 `UUIDGenerator.nextId()`。 | 主键列。 |
| `SNOWFLAKE` | `@PrimaryKey(snowflake = true)` | 插入前写入 `SnowflakeIdGenerator.nextId()`。 | 主键列。 |
| `CUSTOM` | `@PrimaryKey(custom = true)` | 插入前写入 `customIdGenerator?.nextId()`。 | 主键列。 |

## {{ $.title("DEFAULT") }} 手动主键

由应用程序或数据库默认值提供主键值时，直接使用不带参数的 `@PrimaryKey`。

```kotlin group="Manual key 1" name="kotlin" icon="kotlin"
@Table("tb_region")
data class Region(
    @PrimaryKey
    var code: String? = null,
    var name: String? = null
) : KPojo

Region(code = "CN", name = "China").insert().build()
```

```sql group="Manual key 1" name="Mysql" icon="mysql"
INSERT INTO `tb_region` (`code`, `name`) VALUES (:code, :name)
```

```text group="Manual key 1" name="params"
code -> "CN"
name -> "China"
```

建表时，该字段是普通主键列。

```sql group="Manual key 2" name="DDL" icon="mysql"
`code` VARCHAR(255) NOT NULL PRIMARY KEY
```

## {{ $.title("IDENTITY") }} 数据库生成主键

使用数据库自增主键时，在 `Int` 或 `Long` 字段上设置 `identity = true`。

```kotlin group="Identity key 1" name="kotlin" icon="kotlin"
@Table("tb_user")
data class IdentityUser(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null
) : KPojo

IdentityUser(username = "Ada").insert().build()
```

当 `id` 为 `null` 时，Kronos 会从 insert 字段列表中移除 identity 主键。

```sql group="Identity key 2" name="Mysql" icon="mysql"
INSERT INTO `tb_user` (`username`) VALUES (:username)
```

```text group="Identity key 2" name="params"
username -> "Ada"
```

MySQL 建表列形态如下：

```sql group="Identity key 3" name="DDL" icon="mysql"
`id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT
```

> **Note**
> 如果手动传入 identity 主键值，Kronos 会保留该主键列。需要读取数据库生成值时，使用 {{ $.keyword("mutation/last-insert-id", ["Last Insert ID"]) }}。

## {{ $.title("UUID") }} UUID 字符串主键

`String` 主键需要由 Kronos 生成 UUID 时，设置 `uuid = true`。Kronos 会在构建 insert SQL 前调用 `UUIDGenerator.nextId()`。

```kotlin group="UUID key 1" name="kotlin" icon="kotlin"
@Table("tb_uuid_user")
data class UuidUser(
    @PrimaryKey(uuid = true)
    var id: String? = null,
    var username: String? = null
) : KPojo

UuidUser(username = "Ada").insert().build()
```

```sql group="UUID key 1" name="Mysql" icon="mysql"
INSERT INTO `tb_uuid_user` (`id`, `username`) VALUES (:id, :username)
```

```text group="UUID key 1" name="params"
id -> "550e8400-e29b-41d4-a716-446655440000"
username -> "Ada"
```

建表时仍渲染为普通主键列。

```sql group="UUID key 2" name="DDL" icon="mysql"
`id` VARCHAR(255) NOT NULL PRIMARY KEY
```

## {{ $.title("SNOWFLAKE") }} 雪花算法主键

需要在应用进程内生成 `Long` 主键时，设置 `snowflake = true`。

```kotlin group="Snowflake key 1" name="kotlin" icon="kotlin"
@Table("tb_order")
data class OrderRecord(
    @PrimaryKey(snowflake = true)
    var id: Long? = null,
    var status: String? = null
) : KPojo

OrderRecord(status = "created").insert().build()
```

```sql group="Snowflake key 1" name="Mysql" icon="mysql"
INSERT INTO `tb_order` (`id`, `status`) VALUES (:id, :status)
```

```text group="Snowflake key 1" name="params"
id -> 1912549819912192000
status -> "created"
```

多节点应用生成雪花 ID 时，先配置节点标识。

```kotlin group="Snowflake key 2" name="config" icon="kotlin"
import com.kotlinorm.beans.generator.SnowflakeIdGenerator

fun configureSnowflakeIds() {
    SnowflakeIdGenerator.datacenterId = 1
    SnowflakeIdGenerator.workerId = 1
}
```

`datacenterId` 和 `workerId` 的取值范围都是 `0` 到 `31`。

## {{ $.title("CUSTOM") }} 自定义生成器主键

主键值需要由业务生成器提供时，设置 `custom = true` 并注册 `KIdGenerator`。

```kotlin group="Custom key" name="generator" icon="kotlin"
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.interfaces.KIdGenerator

object TicketIdGenerator : KIdGenerator<String> {
    override fun nextId(): String = "ticket-${System.currentTimeMillis()}"
}

fun configureTicketIds() {
    customIdGenerator = TicketIdGenerator
}
```

```kotlin group="Custom key" name="model" icon="kotlin"
@Table("tb_ticket")
data class Ticket(
    @PrimaryKey(custom = true)
    var id: String? = null,
    var title: String? = null
) : KPojo

Ticket(title = "Login issue").insert().build()
```

```sql group="Custom key" name="Mysql" icon="mysql"
INSERT INTO `tb_ticket` (`id`, `title`) VALUES (:id, :title)
```

```text group="Custom key" name="params"
id -> "ticket-1912549819912"
title -> "Login issue"
```

> **Warning**
> 插入 `custom` 主键前必须设置 `customIdGenerator`。没有设置时，生成的主键参数为 `null`。

## 全局主键兜底策略

推荐在主键属性上直接使用 `@PrimaryKey`。只有未标注主键的模型都使用同名主键字段时，才使用 `Kronos.primaryKeyStrategy` 作为主键查找兜底。

```kotlin group="Global primary key 1" name="config" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.PrimaryKeyType

fun configurePrimaryKeyStrategy() {
    with(Kronos) {
        primaryKeyStrategy = KronosCommonStrategy(
            enabled = true,
            field = Field("id", "id", primaryKey = PrimaryKeyType.IDENTITY)
        )
    }
}
```

```kotlin group="Global primary key 1" name="model" icon="kotlin"
@Table("tb_audit_log")
data class AuditLog(
    var id: Int? = null,
    var message: String? = null
) : KPojo
```

需要建表 DDL 和字段元数据都明确体现主键时，优先使用属性注解：

```kotlin group="Global primary key 2" name="recommended" icon="kotlin"
@PrimaryKey(identity = true)
var id: Int? = null
```
