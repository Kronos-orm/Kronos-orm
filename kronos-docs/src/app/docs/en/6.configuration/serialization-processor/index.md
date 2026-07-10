{% import "../../../macros/macros-en.njk" as $ %}

## Methods：

### 1. {{ $.title("deserialize(serializedStr, kType)") }} Deserialize

Deserialize the string to the specified declaration type.

- **Function declaration**

    ```kotlin
    fun deserialize(serializedStr: String, kType: KType): Any
    ```
  
- **Usage example**
    
    ```kotlin
    val user = processor.deserialize(serializedStr, typeOf<User>())
    ```

- **Parameters**

    {{ $.params([
        ['serializedStr', 'String to deserialize', 'String'],
        ['kType', 'Kotlin declaration type to deserialize', 'KType']
    ]) }}

- **Return value**

    `Any` Deserialized object

{{ $.hr() }}

### 2. {{ $.title("serialize(obj, kType)") }} Serialize

Serialize the object with its declaration type.

- **Function declaration**

    ```kotlin
    fun serialize(obj: Any, kType: KType): String
    ```
  
- **Usage example**
    
    ```kotlin
    val serializedStr = processor.serialize(user, typeOf<User>())
    ```
  
- **Parameters**

    {{ $.params([
        ['obj', 'Object to serialize', 'Any'],
        ['kType', 'Kotlin declaration type of the object', 'KType']
    ]) }}

- **Return value**
    
  `String` Serialized string

> **Note**
> ORM write and read paths pass the field declaration `KType` automatically. Custom processors should use it when the field is generic, for example `List<ProfileSetting>`.
