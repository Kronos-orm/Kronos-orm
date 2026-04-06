{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 主键类型

在 Kronos 中，`PrimaryKeyType` 枚举定义了主键的生成策略。你可以通过 `@PrimaryKey` 注解或全局 `primaryKeyStrategy` 来指定主键类型。

## PrimaryKeyType 枚举

| 类型 | 说明 | 字段类型要求 |
|------|------|-------------|
| `NOT` | 非主键 | 无 |
| `DEFAULT` | 默认主键，无自动生成 | 任意 |
| `IDENTITY` | 数据库自增 | `Int` / `Long` |
| `UUID` | UUID 字符串 | `String` |
| `SNOWFLAKE` | 雪花算法 | `Long` |
| `CUSTOM` | 自定义生成器 | 取决于生成器 |

## {{ $.title("NOT")}}NOT — 非主键

标记字段不是主键。这是非主键字段的默认值。

## {{ $.title("DEFAULT")}}DEFAULT — 默认主键

字段被标记为主键，但不使用任何自动生成策略。你需要在插入时手动提供主键值，或依赖数据库的默认值。

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var username: String? = null
) : KPojo
```

## {{ $.title("IDENTITY")}}IDENTITY — 数据库自增

使用数据库的自增机制生成主键。插入时无需提供主键值，数据库会自动分配。字段类型应为 `Int` 或 `Long`。

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null
) : KPojo
```

当使用 `IDENTITY` 类型时，如果主键值为 `null`，Kronos 会在插入 SQL 中排除主键字段，让数据库自动生成。

## {{ $.title("UUID")}}UUID — UUID 字符串

使用 `java.util.UUID.randomUUID()` 生成主键。字段类型应为 `String`。

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey(uuid = true)
    var id: String? = null,
    var username: String? = null
) : KPojo
```

Kronos 在执行插入操作时会自动调用 `UUIDGenerator.nextId()` 生成主键值。

## {{ $.title("SNOWFLAKE")}}SNOWFLAKE — 雪花算法

使用 Twitter Snowflake 算法生成分布式唯一 ID。字段类型应为 `Long`。

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey(snowflake = true)
    var id: Long? = null,
    var username: String? = null
) : KPojo
```

你可以在初始化时配置 `datacenterId` 和 `workerId`：

```kotlin
import com.kotlinorm.beans.generator.SnowflakeIdGenerator

Kronos.init {
    SnowflakeIdGenerator.datacenterId = 1 // 0-31
    SnowflakeIdGenerator.workerId = 1     // 0-31
}
```

> **Note**
> 在分布式环境中，请确保每个节点的 `datacenterId` 和 `workerId` 组合唯一，否则可能产生重复 ID。

## {{ $.title("CUSTOM")}}CUSTOM — 自定义生成器

使用自定义的 ID 生成器。你需要实现 `KIdGenerator<T>` 接口并注册到全局变量 `customIdGenerator`。

```kotlin
import com.kotlinorm.interfaces.KIdGenerator
import com.kotlinorm.beans.generator.customIdGenerator

// 实现自定义生成器
object MyIdGenerator : KIdGenerator<Long> {
    override fun nextId(): Long {
        // 你的 ID 生成逻辑
        return System.currentTimeMillis()
    }
}

// 注册生成器
Kronos.init {
    customIdGenerator = MyIdGenerator
}
```

在实体类中使用：

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey(custom = true)
    var id: Long? = null,
    var username: String? = null
) : KPojo
```

`KIdGenerator<T>` 接口定义如下：

```kotlin
interface KIdGenerator<T> {
    fun nextId(): T
}
```

## 全局主键策略

你可以通过 `Kronos.primaryKeyStrategy` 设置全局默认的主键策略，这样无需在每个实体类上单独配置：

```kotlin
Kronos.init {
    primaryKeyStrategy = KronosCommonStrategy(
        enabled = true,
        field = Field("id", "id", primaryKey = PrimaryKeyType.IDENTITY)
    )
}
```
