{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 成员方法

### 1. {{ $.title("deserialize(serializedStr, kClass)") }} 反序列化

将字符串反序列化为指定的对象。

- **函数声明**

    ```kotlin
    fun deserialize(serializedStr: String, kClass: KClass<*>): Any
    ```

- **使用示例**

    ```kotlin
    val user = processor.deserialize(serializedStr, User::class)
    ```

- **参数**

  {{ $.params([
  ['serializedStr', '要反序列化的字符串', 'String'],
  ['kClass', '要反序列化的KClass', 'KClass<*>']
  ]) }}

- **返回值**

  `Any` 反序列化后的对象

{{ $.hr() }}

### 2. {{ $.title("serialize(obj)") }} 序列化

将对象序列化为字符串。

- **函数声明**

    ```kotlin
    fun serialize(obj: Any): String
    ```

- **使用示例**

    ```kotlin
    val serializedStr = processor.serialize(user)
    ```

- **参数**

  {{ $.params([
  ['obj', 'Object to serialize', 'Any']
  ]) }}

- **返回值**

  `String` 序列化后的字符串