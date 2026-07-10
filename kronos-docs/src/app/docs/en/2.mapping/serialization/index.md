{% import "../../../macros/macros-en.njk" as $ %}

## Mark a property for serialization

Use `@Serialize` on a persisted property when the Kotlin value should be converted to a database string value on write and restored on read. The conversion is supplied by `Kronos.serializeProcessor`, whose type is `KronosSerializeProcessor`.

```kotlin group="Serialization 1 1" name="model" icon="kotlin"
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

data class ProfileSetting(
    val theme: String,
    val shortcuts: List<String>
)

@Table("tb_user_profile")
data class UserProfile(
    var id: Int? = null,
    @ColumnType(KColumnType.JSON)
    @Serialize
    var setting: ProfileSetting? = null
) : KPojo
```

When `setting = ProfileSetting("dark", listOf("search", "save"))`, insert and update parameters use the serialized value:

```text group="Serialization 1 2" name="write result"
setting -> {"theme":"dark","shortcuts":["search","save"]}
```

When a selected row contains that string value, `fromMapData()` and query mapping call the same processor to restore `ProfileSetting`.

```kotlin group="Serialization 1 3" name="read result" icon="kotlin"
val profile = UserProfile().fromMapData<UserProfile>(
    mapOf(
        "id" to 1,
        "setting" to """{"theme":"dark","shortcuts":["search","save"]}"""
    )
)

profile.setting == ProfileSetting("dark", listOf("search", "save"))
```

## Configure a processor

Implement `KronosSerializeProcessor` and assign it to `Kronos.serializeProcessor` before ORM operations that read or write serialized properties. The processor contract is documented in {{ $.keyword("configuration/serialization-processor", ["Serialization and Deserialization"]) }}.

```kotlin group="Serialization 2" name="processor" icon="kotlin"
import com.google.gson.Gson
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KronosSerializeProcessor
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

object GsonProcessor : KronosSerializeProcessor {
    private val gson = Gson()

    override fun serialize(obj: Any, kType: KType): String =
        gson.toJson(obj)

    override fun deserialize(serializedStr: String, kType: KType): Any =
        gson.fromJson(serializedStr, kType.javaType)
}

Kronos.serializeProcessor = GsonProcessor
```

## Configure Kotlinx Serialization

Use Kotlinx Serialization when serialized fields include Kotlin data classes, nested objects, or generic collections such as `List<String>`. Kronos passes the field declaration `KType` to the processor, so the serializer can keep generic element types.

```kotlin group="Serialization 3" name="processor" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KronosSerializeProcessor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

object KotlinxSerializeProcessor : KronosSerializeProcessor {
    override fun serialize(obj: Any, kType: KType): String {
        @Suppress("UNCHECKED_CAST")
        val valueSerializer = serializer(kType) as KSerializer<Any>
        return json.encodeToString(valueSerializer, obj)
    }

    override fun deserialize(serializedStr: String, kType: KType): Any {
        return json.decodeFromString(serializer(kType), serializedStr)
            ?: error("Kotlinx serialization returned null for $kType")
    }
}

Kronos.serializeProcessor = KotlinxSerializeProcessor

@Serializable
data class ProfileSetting(
    val theme: String,
    val shortcuts: List<String>
)
```

> **Note**
> Classes handled by Kotlinx Serialization still need `@Serializable`. For fields like `List<ProfileSetting>`, make the element type serializable as well.

## Use a delegated serialized view

For an existing string column, keep the stored column as `String?` and expose a typed property with the `serialize(::column)` delegate.

```kotlin group="Serialization 4" name="delegate" icon="kotlin"
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.serialize.serialize
import com.kotlinorm.interfaces.KPojo

@Table("tb_user_profile")
data class UserProfileRaw(
    var id: Int? = null,
    var settingJson: String? = null
) : KPojo {
    var setting: ProfileSetting? by serialize(::settingJson)
}

val profile = UserProfileRaw()
profile.setting = ProfileSetting("dark", listOf("search"))
profile.settingJson == """{"theme":"dark","shortcuts":["search"]}"""
```

Use {{ $.keyword("mapping/column-types", ["Column Types"]) }} when the serialized column also needs an explicit DDL type such as `JSON`.
