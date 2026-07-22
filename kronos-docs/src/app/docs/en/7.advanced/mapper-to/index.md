{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos provides conversion APIs between KPojo entities and `Map<String, Any?>`.

Use `toDataMap()` when you need a column-value map, and use `mapperTo()` or `safeMapperTo()` when you need to build another KPojo value from a map.

```kotlin
data class User(
    var id: Int? = null,
    var name: String? = null
) : KPojo

val user = User(id = 1, name = "Tom")

val data = user.toDataMap()
// mapOf("id" to 1, "name" to "Tom")

val copied = data.mapperTo<User>()
// User(id = 1, name = "Tom")

val converted = mapOf("id" to "1", "name" to "Tom").safeMapperTo<User>()
// User(id = 1, name = "Tom")
```

## KPojo to Map Conversion

### 1. {{ $.title("KPojo.toDataMap()")}}

Converts an entity object to a Map.

- **Declaration**

    ```kotlin
    fun KPojo.toDataMap(): Map<String, Any?>
    ```

- **Example**

    ```kotlin
    val user = User(id = 1, name = "Tom").toDataMap()
    ```

- **Return**

    `Map<String, Any?>` Map

{{ $.hr() }}

## Mapper to Strict Conversion

MapperTo contains a total of 4 functions for type conversion, namely:

### 1. {{ $.title("Map<String, Any?>.mapperTo(KType)")}}

Converted to an entity object via Map. When the value in the Map does not match the type of an entity object property, the property assignment will be skipped.

Pass the complete target type with `typeOf<T>()`. Generic KPojo construction is reserved for a later release; the current target must be a concrete, non-generic KPojo type.

- **Declaration**

    ```kotlin
    fun Map<String, Any?>.mapperTo(type: KType): Any
    ```

- **Example**

    ```kotlin
    val user = mapOf("id" to 1, "name" to "Tom").mapperTo(typeOf<User>())
    ```

- **Parameters**

  {{ $.params([['type', 'Complete entity target type', 'KType']]) }}

- **Return**

  `Any` Entity object

{{ $.hr() }}

### 2. {{ $.title("Map<String, Any?>.mapperTo<T: KPojo>()")}}

When converting a Map to an entity object. When the value in the Map does not match the type of an entity object property, the property assignment will be skipped.

- **Declaration**

    ```kotlin
    inline fun <reified T : KPojo> Map<String, Any?>.mapperTo(): T
    ```

- **Examples**

    ```kotlin
    val user = mapOf("id" to 1, "name" to "Tom").mapperTo<User>()
    ```

- **Return**

  `T` Entity object

{{ $.hr() }}

### 3. {{ $.title("KPojo.mapperTo(KType)")}}

By converting an entity object to another entity object. When the value in the Map does not match the type of an entity object property, the property assignment will be skipped.

Pass the exact concrete KPojo target with `typeOf<T>()`.

- **Declaration**

    ```kotlin
    fun KPojo.mapperTo(type: KType): Any
    ```

- **Examples**

    ```kotlin
    val student = User(id = 1, name = "Tom").mapperTo(typeOf<Student>())
    ```

- **Parameters**

  {{ $.params([['type', 'Complete entity target type', 'KType']]) }}

- **Return**

  `Any` Entity object

{{ $.hr() }}

### 4. {{ $.title("KPojo.mapperTo<T: KPojo>()")}}

By converting an entity object to another entity object. When the value in the Map does not match the type of an entity object property, the property assignment will be skipped.

- **Declaration**

    ```kotlin
    inline fun <reified T : KPojo> KPojo.mapperTo(): T
    ```

- **Examples**

    ```kotlin
    val student = User(id = 1, name = "Tom").mapperTo<Student>()
    ```

- **Return**

  `T` Entity object

{{ $.hr() }}

## Safe Mapper to Secure Conversion

SafeMapperTo contains 4 functions for type conversion, which are:

### 1. {{ $.title("Map<String, Any?>.safeMapperTo(KType)")}}

Converted to an entity object via Map. When a value does not match the property type, the unified conversion registry attempts a safe conversion; see {{ $.keyword("configuration/value-codec", ["Concept", "ValueCodec"]) }}.

Pass the complete target type with `typeOf<T>()`. Generic KPojo construction is reserved for a later release; the current target must be a concrete, non-generic KPojo type.

- **Declaration**

    ```kotlin
    fun Map<String, Any?>.safeMapperTo(type: KType): Any
    ```

- **Examples**

    ```kotlin
    val user = mapOf("id" to "1", "name" to "Tom").safeMapperTo(typeOf<User>())
    ```

- **Parameters**

    {{ $.params([['type', 'Complete entity target type', 'KType']]) }}

- **Return**

  `Any` Entity object

{{ $.hr() }}

### 2. {{ $.title("Map<String, Any?>.safeMapperTo<T: KPojo>()")}}

Converted to an entity object via Map. When a value does not match the property type, the unified conversion registry attempts a safe conversion; see {{ $.keyword("configuration/value-codec", ["Concept", "ValueCodec"]) }}.

- **Declaration**

    ```kotlin
    inline fun <reified T : KPojo> Map<String, Any?>.safeMapperTo(): T
    ```

- **Examples**

    ```kotlin
    val user = mapOf("id" to "1", "name" to "Tom").safeMapperTo<User>()
    ```

- **Return**

    `T` Entity object

{{ $.hr() }}

### 3. {{ $.title("KPojo.safeMapperTo(KType)")}}

Converting from one entity object to another attempts safe conversion when source and target properties use different types; see {{ $.keyword("configuration/value-codec", ["Concept", "ValueCodec"]) }}.
Pass the exact concrete KPojo target with `typeOf<T>()`.

- **Declaration**

    ```kotlin
    fun KPojo.safeMapperTo(type: KType): Any
    ```

- **Examples**

    ```kotlin
    val student = User(id = 1, name = "Tom").safeMapperTo(typeOf<Student>())
    ```

- **Parameters**

    {{ $.params([['type', 'Complete entity target type', 'KType']]) }}

- **Return**

    `Any` Entity object

{{ $.hr() }}

### 4. {{ $.title("KPojo.safeMapperTo<T: KPojo>()")}}

Converting to another entity object attempts safe conversion when source and target property types differ; see {{ $.keyword("configuration/value-codec", ["Concept", "ValueCodec"]) }}.

- **Declaration**

    ```kotlin
    inline fun <reified T : KPojo> KPojo.safeMapperTo(): T
    ```

- **Examples**

    ```kotlin
    val student = User(id = 1, name = "Tom").safeMapperTo<Student>()
    ```

- **Return**

    `T` Entity object
