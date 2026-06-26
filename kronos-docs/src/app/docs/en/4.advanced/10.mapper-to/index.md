{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos provides a set of functions for converting between entity objects and Maps, which can help you quickly convert between entity objects and Maps.

**The implementation of Kronos does not rely on reflection but is achieved through code generated at compile time, thus the conversion efficiency and performance are very high.**

We have generated the toMap() and fromMap() functions for KPojo, which are used for conversion between entity objects and Maps:

```kotlin
data class User(
    val id: Int? = null,
    val name: String? = null
) : KPojo

// We will generate the following function for User
data class User {
    fun toMap(): Map<String, Any?>{
        return mapOf("id" to id, "name" to name)
    }
    
    fun fromMap(map: Map<String, Any?>): User{
        try { this.id = map["id"] }
        // name is a val property and cannot be assigned directly, so skip it
        return this
    }
    
    fun safeFromMap(map: Map<String, Any?>): User{
        this.id = safeCast(map["id"])
        // name is a val property and cannot be assigned directly, so skip it
        return this
    }
}
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

### 1. {{ $.title("Map<String, Any?>.mapperTo(KClass<KPojo>)")}}

Converted to an entity object via Map. When the value in the Map does not match the type of an entity object property, the property assignment will be skipped.

The kClass can be a covariant KPojo type or a concrete entity object type.

- **Declaration**

    ```kotlin
    fun Map<String, Any?>.safeMapperTo(kClass: KClass<KPojo>): Any
    ```

- **Example**

    ```kotlin
    val user = mapOf("id" to 1, "name" to "Tom").safeMapperTo(User::class)
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

### 3. {{ $.title("KPojo.mapperTo(KClass<KPojo>)")}}

By converting an entity object to another entity object. When the value in the Map does not match the type of an entity object property, the property assignment will be skipped.

kClass can be a covariant KPojo type, or a specific entity object type.

- **Declaration**

    ```kotlin
    fun KPojo.safeMapperTo(kClass: KClass<KPojo>): Any
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

### 1. {{ $.title("Map<String, Any?>.safeMapperTo(KClass<KPojo>)")}}

Converted to an entity object via Map, when the value in Map does not match the type of the entity object property, a type conversion is attempted, see: {{ $.keyword("
concept/value-transformer", ["Concept", "Value Transformer"])}}.

The kClass can be a covariant KPojo type or a concrete entity object type.

- **Declaration**

    ```kotlin
    fun Map<String, Any?>.safeMapperTo(kClass: KClass<KPojo>): Any
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

Converted to an entity object via Map, when the value in the Map does not match the type of the entity object property, a type conversion is attempted, see: {{ $.keyword("
concept/value-transformer", ["Concept", "Value Transformer"])}}.

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

### 3. {{ $.title("KPojo.safeMapperTo(KClass<KPojo>)")}}

Converting to another entity object via an entity object attempts a type conversion when the type of the entity object attribute does not match the type of the target entity object attribute, see: {{
$.keyword("concept/value-transformer", ["Concept", "Value Transformer"])}}.
The kClass can be a covariant KPojo type or a concrete entity object type.

- **Declaration**

    ```kotlin
    fun KPojo.safeMapperTo(kClass: KClass<KPojo>): Any
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

Converting to another entity object via an entity object attempts a type conversion when the type of the entity object property does not match the type of the target entity object property, see: {{ $.keyword("
concept/value-transformer", ["Concept", "Value Transformer"])}}.

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


