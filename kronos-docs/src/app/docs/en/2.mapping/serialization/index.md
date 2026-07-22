{% import "../../../macros/macros-en.njk" as $ %}

## Mark serialized storage

Use `@Serialize` when a persisted Kotlin value must be stored as text and restored after a typed read. The annotation selects `ValueStorage.SERIALIZED`; it does not install a serializer. Register one serialized `ValueCodec` during application startup.

```kotlin group="Serialized model" name="model" icon="kotlin"
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }
data class ProfileSetting(val theme: String, val shortcuts: List<String>)

@Table("tb_user_profile")
data class UserProfile(
    var id: Int? = null,
    @ColumnType(KColumnType.JSON)
    @Serialize
    var setting: ProfileSetting? = null,
    @Serialize
    var statuses: List<Status>? = null
) : KPojo
```

The codec receives the complete property `KType` in both directions. `ProfileSetting`, `List<Status>`, and nested collections therefore use the same registration. A serialized collection is converted as one field value; Kronos does not run the enum codec for each element.

Unless a column type is explicitly supplied, `@Serialize` fields use a string-compatible `VARCHAR` column type. `@ColumnType` changes the DDL type, while `@Serialize` continues to select the `SERIALIZED` storage protocol.

## Register Gson once

`serializedValueCodec` wraps two functions as a normal `ValueCodec`. Register the result through the only conversion registration entry, `Kronos.registerValueCodec`.

```kotlin group="Gson codec" name="startup" icon="kotlin"
import com.google.gson.Gson
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.serializedValueCodec
import kotlin.reflect.jvm.javaType

val gson = Gson()

val serializationRegistration = Kronos.registerValueCodec(
    serializedValueCodec(
        encode = { value, type -> gson.toJson(value, type.javaType) },
        decode = { text, type -> gson.fromJson(text, type.javaType) }
    )
)
```

Later registrations have higher priority. Closing `serializationRegistration` removes only this codec and restores an older matching registration. Applications normally keep it for their lifetime; closing is useful for tests or hot reload.

When no codec accepts serialized storage, writes and typed database/delegate reads fail with `MissingSerializedCodec`. SQL null is handled before codec invocation. Safe Map mapping keeps a value that is already assignable to the target type.

## Register Kotlinx Serialization

Kotlinx Serialization uses the same registration shape. It receives the complete `KType`, so its serializer can retain generic element types.

```kotlin group="Kotlinx codec" name="startup" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.serializedValueCodec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

Kronos.registerValueCodec(
    serializedValueCodec(
        encode = { value, type ->
            @Suppress("UNCHECKED_CAST")
            val valueSerializer = serializer(type) as KSerializer<Any>
            json.encodeToString(valueSerializer, value)
        },
        decode = { text, type -> json.decodeFromString(serializer(type), text) }
    )
)

@Serializable
data class ProfileSetting(
    val theme: String,
    val shortcuts: List<String>
)
```

> **Note**
> Classes handled by Kotlinx Serialization still need `@Serializable`. For `List<ProfileSetting>`, the element type must also be serializable.

## Read through safe mapping

Raw `mapperTo` / `fromMapData` assignment does not invoke codecs. Use safe mapping or typed JDBC results when stored text should be decoded.

```kotlin group="Safe mapping" name="decode" icon="kotlin"
val profile = UserProfile().safeFromMapData<UserProfile>(
    mapOf(
        "id" to 1,
        "setting" to """{"theme":"dark","shortcuts":["search","save"]}""",
        "statuses" to """["READY","CLOSED"]"""
    )
)

profile.statuses == listOf(Status.READY, Status.CLOSED)
```

## Use a delegated serialized view

For an existing string column, retain the stored `String?` property and expose a typed view with `serialize(::column)`. The delegate uses the same registered codec, the complete target `KType`, and the `DELEGATE` origin for both directions.

```kotlin group="Serialized delegate" name="delegate" icon="kotlin"
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

See {{ $.keyword("mapping/enum-serialization", ["Enum Storage and Serialization"]) }} for scalar enum columns and `List<Enum>` boundaries. See {{ $.keyword("configuration/value-codec", ["Value Codec"]) }} for the common registration contract. Use {{ $.keyword("mapping/column-types", ["Column Types"]) }} when the serialized column also needs a DDL type such as `JSON`.
