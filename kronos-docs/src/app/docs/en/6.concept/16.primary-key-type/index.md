{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Primary Key Type

In Kronos, the `PrimaryKeyType` enum defines the generation strategy for primary keys. You can specify the primary key type via the `@PrimaryKey` annotation or the global `primaryKeyStrategy`.

## PrimaryKeyType Enum

| Type | Description | Field Type Requirement |
|------|-------------|----------------------|
| `NOT` | Not a primary key | N/A |
| `DEFAULT` | Default primary key, no auto-generation | Any |
| `IDENTITY` | Database auto-increment | `Int` / `Long` |
| `UUID` | UUID string | `String` |
| `SNOWFLAKE` | Snowflake algorithm | `Long` |
| `CUSTOM` | Custom generator | Depends on generator |

## {{ $.title("NOT")}}NOT — Not a Primary Key

Marks the field as not a primary key. This is the default value for non-primary-key fields.

## {{ $.title("DEFAULT")}}DEFAULT — Default Primary Key

The field is marked as a primary key but does not use any auto-generation strategy. You need to provide the primary key value manually on insert, or rely on the database default value.

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var username: String? = null
) : KPojo
```

## {{ $.title("IDENTITY")}}IDENTITY — Database Auto-Increment

Uses the database's auto-increment mechanism to generate primary keys. No primary key value is needed on insert — the database assigns one automatically. The field type should be `Int` or `Long`.

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var username: String? = null
) : KPojo
```

When using the `IDENTITY` type, if the primary key value is `null`, Kronos excludes the primary key field from the INSERT SQL, letting the database generate it.

## {{ $.title("UUID")}}UUID — UUID String

Uses `java.util.UUID.randomUUID()` to generate the primary key. The field type should be `String`.

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey(uuid = true)
    var id: String? = null,
    var username: String? = null
) : KPojo
```

Kronos automatically calls `UUIDGenerator.nextId()` to generate the primary key value during insert.

## {{ $.title("SNOWFLAKE")}}SNOWFLAKE — Snowflake Algorithm

Uses the Twitter Snowflake algorithm to generate distributed unique IDs. The field type should be `Long`.

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey(snowflake = true)
    var id: Long? = null,
    var username: String? = null
) : KPojo
```

You can configure `datacenterId` and `workerId` during initialization:

```kotlin
import com.kotlinorm.beans.generator.SnowflakeIdGenerator

Kronos.init {
    SnowflakeIdGenerator.datacenterId = 1 // 0-31
    SnowflakeIdGenerator.workerId = 1     // 0-31
}
```

> **Note**
> In a distributed environment, ensure each node has a unique combination of `datacenterId` and `workerId` to avoid duplicate IDs.

## {{ $.title("CUSTOM")}}CUSTOM — Custom Generator

Uses a custom ID generator. You need to implement the `KIdGenerator<T>` interface and register it to the global `customIdGenerator` variable.

```kotlin
import com.kotlinorm.interfaces.KIdGenerator
import com.kotlinorm.beans.generator.customIdGenerator

// Implement custom generator
object MyIdGenerator : KIdGenerator<Long> {
    override fun nextId(): Long {
        // Your ID generation logic
        return System.currentTimeMillis()
    }
}

// Register the generator
Kronos.init {
    customIdGenerator = MyIdGenerator
}
```

Usage in entity class:

```kotlin
@Table(name = "tb_user")
data class User(
    @PrimaryKey(custom = true)
    var id: Long? = null,
    var username: String? = null
) : KPojo
```

The `KIdGenerator<T>` interface is defined as:

```kotlin
interface KIdGenerator<T> {
    fun nextId(): T
}
```

## Global Primary Key Strategy

You can set a global default primary key strategy via `Kronos.primaryKeyStrategy`, so you don't need to configure each entity class individually:

```kotlin
Kronos.init {
    primaryKeyStrategy = KronosCommonStrategy(
        enabled = true,
        field = Field("id", "id", primaryKey = PrimaryKeyType.IDENTITY)
    )
}
```
