{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 标记需要序列化的属性

Kotlin 值写入数据库前需要转换为字符串、读取后需要恢复为对象时，在持久化属性上使用 `@Serialize`。转换逻辑由 `Kronos.serializeProcessor` 提供，类型是 `KronosSerializeProcessor`。

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

当 `setting = ProfileSetting("dark", listOf("search", "save"))` 时，insert 和 update 参数会使用序列化后的值：

```text group="Serialization 1 2" name="write result"
setting -> {"theme":"dark","shortcuts":["search","save"]}
```

查询到的行包含该字符串值时，`fromMapData()` 和查询映射会调用同一个处理器恢复 `ProfileSetting`。

```kotlin group="Serialization 1 3" name="read result" icon="kotlin"
val profile = UserProfile().fromMapData<UserProfile>(
    mapOf(
        "id" to 1,
        "setting" to """{"theme":"dark","shortcuts":["search","save"]}"""
    )
)

profile.setting == ProfileSetting("dark", listOf("search", "save"))
```

## 配置处理器

实现 `KronosSerializeProcessor`，并在读写序列化属性的 ORM 操作前赋值给 `Kronos.serializeProcessor`。处理器接口见 {{ $.keyword("configuration/serialization-processor", ["自动序列化与反序列化"]) }}。

```kotlin group="Serialization 2" name="processor" icon="kotlin"
import com.google.gson.Gson
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KronosSerializeProcessor
import kotlin.reflect.KClass

object GsonProcessor : KronosSerializeProcessor {
    private val gson = Gson()

    override fun serialize(obj: Any): String =
        gson.toJson(obj)

    override fun deserialize(serializedStr: String, kClass: KClass<*>): Any =
        gson.fromJson(serializedStr, kClass.java)
}

Kronos.serializeProcessor = GsonProcessor
```

## 使用委托暴露序列化视图

已有字符串列需要保留为 `String?` 时，可以把存储列留在类中，再用 `serialize(::column)` 委托暴露类型化属性。

```kotlin group="Serialization 3" name="delegate" icon="kotlin"
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

序列化列还需要指定 DDL 类型时，例如 `JSON`，见 {{ $.keyword("mapping/column-types", ["列类型"]) }}。
