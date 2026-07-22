{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 标记序列化存储

持久化 Kotlin 值需要以文本写入、并在 typed 读取后恢复时，给属性添加 `@Serialize`。该注解只选择 `ValueStorage.SERIALIZED`，不会安装序列化器。应用启动时注册一个 serialized `ValueCodec` 即可。

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

codec 在两个方向都会收到属性的完整 `KType`。因此 `ProfileSetting`、`List<Status>` 和嵌套集合共用一次注册。serialized 集合作为一个字段整体转换，Kronos 不会再逐元素调用 enum codec。

没有显式指定列类型时，`@Serialize` 字段使用字符串兼容的 `VARCHAR` 列类型。`@ColumnType` 只改变 DDL 类型，`@Serialize` 仍然选择 `SERIALIZED` 存储协议。

## 一次注册 Gson

`serializedValueCodec` 只把两个函数包装为普通 `ValueCodec`。通过唯一转换注册入口 `Kronos.registerValueCodec` 注册返回值。

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

后注册的 codec 优先级更高。关闭 `serializationRegistration` 只注销本次注册，并恢复更早的匹配项。应用通常在整个生命周期保留注册句柄；测试或热更新时才需要关闭。

没有 codec 接受 serialized storage 时，写入以及 typed 数据库/delegate 读取会抛出 `MissingSerializedCodec`。SQL null 在调用 codec 前处理。safe Map 中已经可赋值给目标类型的值保持不变。

## 注册 Kotlinx Serialization

Kotlinx Serialization 使用完全相同的注册方式。它收到完整 `KType`，因此可以保留泛型元素类型。

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
            json.encodeToString(serializer(type) as KSerializer<Any>, value)
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
> 交给 Kotlinx Serialization 处理的类仍需添加 `@Serializable`。字段为 `List<ProfileSetting>` 时，元素类型也必须可序列化。

## 通过 safe mapping 读取

raw `mapperTo` / `fromMapData` 赋值不会调用 codec。存储文本需要解码时，使用 safe mapping 或 typed JDBC 结果。

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

## 使用委托暴露序列化视图

已有字符串列需要保留时，可以保留 `String?` 存储属性，再用 `serialize(::column)` 暴露类型化视图。delegate 在两个方向都使用同一个已注册 codec、完整目标 `KType` 和 `DELEGATE` origin。

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

标量 enum 列和 `List<Enum>` 边界见 {{ $.keyword("mapping/enum-serialization", ["Enum 存储与序列化"]) }}；统一注册契约见 {{ $.keyword("configuration/value-codec", ["ValueCodec"]) }}。serialized 列还需要指定 `JSON` 等 DDL 类型时，见 {{ $.keyword("mapping/column-types", ["列类型"]) }}。
