{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Move values between a {{ $.code("KPojo") }} model and `Map<String, Any?>` when an application receives map-shaped input, prepares an API response, or copies data between models.

## Choose an API

| Task | API |
|------|-----|
| Read a model as a map | `toDataMap()` |
| Create a new model from values already expressed as its property types | `mapperTo()` |
| Create a new model from values that need normal Kronos conversion | `safeMapperTo()` |
| Apply already-typed values to an existing model | `fromMapData()` |
| Apply values that need normal Kronos conversion to an existing model | `safeFromMapData()` |

## Start with a complete example

`mapperTo` is useful for a map returned by `toDataMap`. `safeMapperTo` is useful for input such as JSON or form data, where a number may arrive as a string.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.mapperTo
import com.kotlinorm.utils.Extensions.safeMapperTo

data class UserDto(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

val typedInput = mapOf("id" to 1, "name" to "Ada")
val copied = typedInput.mapperTo<UserDto>()
// UserDto(id = 1, name = "Ada")

val requestInput = mapOf("id" to "2", "name" to "Lin")
val converted = requestInput.safeMapperTo<UserDto>()
// UserDto(id = 2, name = "Lin")
```

## Read a model as a map

Call `toDataMap()` to obtain the model's mapped property names and values.

```kotlin name="kotlin" icon="kotlin"
val data = UserDto(id = 1, name = "Ada").toDataMap()
// {id=1, name=Ada}
```

## Create a new model

Use `mapperTo<T>()` when the map already contains the Kotlin types declared by `T`. Use `safeMapperTo<T>()` when input values need conversion, such as `"2"` to `Int`.

The safe APIs also use registered application mappings. See {{ $.keyword("configuration/value-codec", ["Custom Value Mapping"]) }} when a property uses a domain value such as `Money`.

```kotlin name="kotlin" icon="kotlin"
val direct = mapOf("id" to 3, "name" to "Mika").mapperTo<UserDto>()
val converted = mapOf("id" to "4", "name" to "Noa").safeMapperTo<UserDto>()
```

## Fill an existing model

Use `fromMapData()` or `safeFromMapData()` when the application already owns the target model. Both functions return that same model after applying the supplied keys. Missing keys leave the current property values in place.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo

data class UserDto(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

val user = UserDto(id = 1, name = "Draft")

user.fromMapData<UserDto>(mapOf("name" to "Published"))
// UserDto(id = 1, name = "Published")

user.safeFromMapData<UserDto>(mapOf("id" to "2"))
// UserDto(id = 2, name = "Published")
```

## Copy between models

The same new-object functions are available on a {{ $.code("KPojo") }} source. They copy matching property names into a new target model.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.mapperTo

data class UserRecord(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class UserCard(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

val card = UserRecord(id = 7, name = "Kai").mapperTo<UserCard>()
// UserCard(id = 7, name = "Kai")
```

Use `source.safeMapperTo<T>()` when matching properties require conversion.

## Runtime-selected target type

The `mapperTo(type: KType)` and `safeMapperTo(type: KType)` overloads are available when code selects the target model at runtime. The generic forms shown above are the usual choice when the target type is known in source code.
