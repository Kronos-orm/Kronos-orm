{% import "../../../macros/macros-zh-CN.njk" as $ %}

## Methods：

### 1. {{ $.title("deserialize(serializedStr, kClass)") }} Deserialize

Deserialize the string to the specified object.

- **Function declaration**

    ```kotlin
    fun deserialize(serializedStr: String, kClass: KClass<*>): Any
    ```
  
- **Usage example**
    
    ```kotlin
    val user = processor.deserialize(serializedStr, User::class)
    ```

- **Parameters**

    {{ $.params([
        ['serializedStr', 'String to deserialize', 'String'],
        ['kClass', 'KClass to deserialize', 'KClass<*>']
    ]) }}

- **Return value**

    `Any` Deserialized object

{{ $.hr() }}

### 2. {{ $.title("serialize(obj)") }} Serialize

Serialize the object to a string.

- **Function declaration**

    ```kotlin
    fun serialize(obj: Any): String
    ```
  
- **Usage example**
    
    ```kotlin
    val serializedStr = processor.serialize(user)
    ```
  
- **Parameters**

    {{ $.params([
        ['obj', 'Object to serialize', 'Any']
    ]) }}

- **Return value**
    
  `String` Serialized string