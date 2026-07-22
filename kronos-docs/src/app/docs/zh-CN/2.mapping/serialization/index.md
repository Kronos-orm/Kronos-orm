{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 保存 JSON 文本

需要把设置对象、列表等属性保存为 JSON 文本时，在属性上添加 {{ $.annotation("Serialize") }}。JSON 库会把属性编码为一个字符串，Kronos 将该字符串作为 JDBC 参数写入数据库；查询结果中的文本会恢复为原来的 Kotlin 属性类型。选择与 JSON 库对应的模型标签页，并在应用启动时完成配置。

```kotlin group="JSON 模型" name="Gson" icon="kotlin"
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

enum class Status { READY, CLOSED }

data class ProfileSetting(
    val theme: String,
    val shortcuts: List<String>
)

@Table("tb_user_profile")
data class UserProfile(
    var id: Int? = null,
    @Serialize
    var setting: ProfileSetting? = null,
    @Serialize
    var statuses: List<Status>? = null
) : KPojo
```

```kotlin group="JSON 模型" name="Kotlinx Serialization" icon="kotlin"
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import kotlinx.serialization.Serializable

@Serializable
enum class Status { READY, CLOSED }

@Serializable
data class ProfileSetting(
    val theme: String,
    val shortcuts: List<String>
)

@Table("tb_user_profile")
data class UserProfile(
    var id: Int? = null,
    @Serialize
    var setting: ProfileSetting? = null,
    @Serialize
    var statuses: List<Status>? = null
) : KPojo
```

上面的 `setting` 和 `statuses` 默认使用 `VARCHAR` 列。需要为已有表或建表语句指定列类型时，参考 {{ $.keyword("mapping/column-types", ["列类型"]) }}。

## 配置 Gson

先为应用加入 Gson 和 Kotlin reflection，再在启动阶段注册一次 JSON 文本转换。同一份配置可以处理带有 {{ $.annotation("Serialize") }} 的对象、列表和嵌套集合。

```kotlin name="build.gradle.kts" icon="gradlekts"
dependencies {
    implementation(kotlin("reflect"))
    implementation("com.google.code.gson:gson:2.14.0")
}
```

```kotlin name="startup" icon="kotlin"
import com.google.gson.Gson
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.serializedValueCodec
import kotlin.reflect.jvm.javaType

val gson = Gson()

Kronos.registerValueCodec(
    serializedValueCodec(
        encode = { value, type -> gson.toJson(value, type.javaType) },
        decode = { text, type -> gson.fromJson(text, type.javaType) }
    )
)
```

将这段注册放在应用启动路径中，例如 Android 的 `Application` 或服务端的启动代码。

## 配置 Kotlinx Serialization

启用 Kotlinx Serialization 编译器插件，加入 JSON 和 reflection 依赖，并选择上方的 Kotlinx Serialization 模型标签页。

```kotlin name="build.gradle.kts" icon="gradlekts"
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "<你的 Kotlin 版本>"
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
```

```kotlin name="startup" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.serializedValueCodec
import kotlinx.serialization.KSerializer
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
```

## 像普通属性一样使用

完成配置后，直接构造、保存和查询带有 JSON 属性的 `UserProfile`。

```kotlin name="kotlin" icon="kotlin"
val profile = UserProfile(
    setting = ProfileSetting("dark", listOf("search", "save")),
    statuses = listOf(Status.READY, Status.CLOSED)
)
profile.insert().execute()

val loaded = UserProfile()
    .select()
    .where { it.id == 1 }
    .first()

println(loaded.setting?.theme)
println(loaded.statuses)
```

标量枚举字段参考 {{ $.keyword("mapping/enum-serialization", ["枚举字段"]) }}。`Money` 等以标量列保存的领域值参考 {{ $.keyword("configuration/value-codec", ["自定义值映射"]) }}。
