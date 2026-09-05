{% import "../../../macros/macros-en.njk" as $ %}

## Store JSON text

Use {{ $.annotation("Serialize") }} for a property that should be stored as JSON text, such as a settings object or a list. The JSON library encodes the property into one string, and Kronos writes that string as a JDBC parameter; query results restore the property's Kotlin type. Choose the model tab that matches the JSON library and configure it when the application starts.

```kotlin group="JSON model" name="Gson" icon="kotlin"
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

```kotlin group="JSON model" name="Kotlinx Serialization" icon="kotlin"
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

The `setting` and `statuses` properties above use `VARCHAR` columns by default. See {{ $.keyword("mapping/column-types", ["Column Types"]) }} when an existing schema or table creation needs a specific column type.

## Configure Gson

Add Gson and Kotlin reflection to the application, then register JSON text conversion once during startup. The same setup handles objects, lists, and nested values marked with {{ $.annotation("Serialize") }}.

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

Place this registration in the application's startup path, such as an Android `Application` or server bootstrap.

## Configure Kotlinx Serialization

Enable the Kotlinx Serialization compiler plugin, add the JSON and reflection dependencies, and choose the Kotlinx Serialization model tab above.

```kotlin name="build.gradle.kts" icon="gradlekts"
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "<your Kotlin version>"
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

## Use the property normally

After configuration, construct, save, and query `UserProfile` with its regular Kotlin properties.

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

See {{ $.keyword("mapping/enum-serialization", ["Enum Fields"]) }} for scalar enum columns. See {{ $.keyword("configuration/value-codec", ["Custom Value Mapping"]) }} for domain values such as `Money` that use a scalar database column.
