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

### 1. {{ $.title("Map<String, Any?>.mapperTo(KClass<out KPojo>)")}}

Converted to an entity object via Map. When the value in the Map does not match the type of an entity object property, the property assignment will be skipped.

The kClass can be a covariant KPojo type or a concrete entity object type.

- **Declaration**

    ```kotlin
    fun Map<String, Any?>.mapperTo(kClass: KClass<out KPojo>): Any
    ```

- **Example**

    ```kotlin
    val user = mapOf("id" to 1, "name" to "Tom").mapperTo(User::class)
    ```

- **Parameters**

  {{ $.params([['kClass', 'Entity object', 'KClass<KPojo>']]) }}

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

### 3. {{ $.title("KPojo.mapperTo(KClass<out KPojo>)")}}

By converting an entity object to another entity object. When the value in the Map does not match the type of an entity object property, the property assignment will be skipped.

kClass can be a covariant KPojo type, or a specific entity object type.

- **Declaration**

    ```kotlin
    fun KPojo.mapperTo(kClass: KClass<out KPojo>): Any
    ```

- **Examples**

    ```kotlin
    val student = User(id = 1, name = "Tom").mapperTo(Student::class)
    ```

- **Parameters**

  {{ $.params([['kClass', 'Entity object', 'KClass<KPojo>']]) }}

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

### 1. {{ $.title("Map<String, Any?>.safeMapperTo(KClass<out KPojo>)")}}

Converted to an entity object via Map, when the value in Map does not match the type of the entity object property, a type conversion is attempted, see: {{ $.keyword("configuration/value-transformer", ["Concept", "Value Transformer"]) }}.

The kClass can be a covariant KPojo type or a concrete entity object type.

- **Declaration**

    ```kotlin
    fun Map<String, Any?>.safeMapperTo(kClass: KClass<out KPojo>): Any
    ```

- **Examples**

    ```kotlin
    val user = mapOf("id" to "1", "name" to "Tom").safeMapperTo(User::class)
    ```

- **Parameters**

    {{ $.params([['kClass', 'Entity object', 'KClass<KPojo>']]) }}

- **Return**

  `Any` Entity object

{{ $.hr() }}

### 2. {{ $.title("Map<String, Any?>.safeMapperTo<T: KPojo>()")}}

Converted to an entity object via Map, when the value in the Map does not match the type of the entity object property, a type conversion is attempted, see: {{ $.keyword("configuration/value-transformer", ["Concept", "Value Transformer"]) }}.

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

### 3. {{ $.title("KPojo.safeMapperTo(KClass<out KPojo>)")}}

Converting from one entity object to another attempts type conversion when source and target attributes use different types, see: {{ $.keyword("configuration/value-transformer", ["Concept", "Value Transformer"]) }}.
The kClass can be a covariant KPojo type or a concrete entity object type.

- **Declaration**

    ```kotlin
    fun KPojo.safeMapperTo(kClass: KClass<out KPojo>): Any
    ```

- **Examples**

    ```kotlin
    val student = User(id = 1, name = "Tom").safeMapperTo(Student::class)
    ```

- **Parameters**

    {{ $.params([['kClass', 'Entity object', 'KClass<KPojo>']]) }}

- **Return**

    `Any` Entity object

{{ $.hr() }}

### 4. {{ $.title("KPojo.safeMapperTo<T: KPojo>()")}}

Converting to another entity object via an entity object attempts a type conversion when the type of the entity object property does not match the type of the target entity object property, see: {{ $.keyword("configuration/value-transformer", ["Concept", "Value Transformer"]) }}.

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
