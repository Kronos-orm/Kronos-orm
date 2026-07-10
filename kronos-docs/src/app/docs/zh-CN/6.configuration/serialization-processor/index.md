{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 成员方法

### 1. {{ $.title("deserialize(serializedStr, kType)") }} 反序列化

将字符串反序列化为指定的声明类型。

- **函数声明**

    ```kotlin
    fun deserialize(serializedStr: String, kType: KType): Any
    ```

- **使用示例**

    ```kotlin
    val user = processor.deserialize(serializedStr, typeOf<User>())
    ```

- **参数**

  {{ $.params([
  ['serializedStr', '要反序列化的字符串', 'String'],
  ['kType', '要反序列化到的 Kotlin 声明类型', 'KType']
  ]) }}

- **返回值**

  `Any` 反序列化后的对象

{{ $.hr() }}

### 2. {{ $.title("serialize(obj, kType)") }} 序列化

按对象的声明类型将对象序列化为字符串。

- **函数声明**

    ```kotlin
    fun serialize(obj: Any, kType: KType): String
    ```

- **使用示例**

    ```kotlin
    val serializedStr = processor.serialize(user, typeOf<User>())
    ```

- **参数**

  {{ $.params([
  ['obj', '要序列化的对象', 'Any'],
  ['kType', '对象的 Kotlin 声明类型', 'KType']
  ]) }}

- **返回值**

  `String` 序列化后的字符串

> **Note**
> ORM 写入和读取路径会自动传入字段声明上的 `KType`。自定义处理器处理 `List<ProfileSetting>` 这类泛型字段时，应使用这个类型信息。
