{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos 提供 KPojo 实体与 `Map<String, Any?>` 之间的转换 API。

需要列值 Map 时使用 `toDataMap()`，需要从 Map 构造 KPojo 时使用 `mapperTo()` 或 `safeMapperTo()`。

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

## KPojo to Map 转换

### 1. {{ $.title("KPojo.toDataMap()")}}

将实体对象转换为Map。

- **函数声明**

    ```kotlin
    fun KPojo.toDataMap(): Map<String, Any?>
    ```

- **使用示例**

    ```kotlin
    val user = User(id = 1, name = "Tom").toDataMap()
    ```

- **返回值**

  `Map<String, Any?>` Map

{{ $.hr() }}

## Mapper to 严格转换

MapperTo共包含4个函数用于类型转换，分别是：

### 1. {{ $.title("Map<String, Any?>.mapperTo(KType)")}}

通过Map转换为实体对象，当Map中的值与实体对象属性的类型不匹配时，则会跳过该属性。

通过 `typeOf<T>()` 传入完整目标类型。泛型 KPojo 构造留待后续版本支持，当前目标必须是具体、非泛型的 KPojo 类型。

- **函数声明**

    ```kotlin
    fun Map<String, Any?>.mapperTo(type: KType): Any
    ```

- **使用示例**

    ```kotlin
    val user = mapOf("id" to 1, "name" to "Tom").mapperTo(typeOf<User>())
    ```

- **接收参数**

  {{ $.params([['type', '完整实体目标类型', 'KType']]) }}

- **返回值**

  `Any` 实体对象

{{ $.hr() }}

### 2. {{ $.title("Map<String, Any?>.mapperTo<T: KPojo>()")}}

通过Map转换为实体对象，当Map中的值与实体对象属性的类型不匹配时，则会跳过该属性。

- **函数声明**

    ```kotlin
    inline fun <reified T : KPojo> Map<String, Any?>.mapperTo(): T
    ```

- **使用示例**

    ```kotlin
    val user = mapOf("id" to 1, "name" to "Tom").mapperTo<User>()
    ```

- **返回值**

  `T` 实体对象

{{ $.hr() }}

### 3. {{ $.title("KPojo.mapperTo(KType)")}}

通过实体对象转换为另一个实体对象，当实体对象属性的类型与目标实体对象属性的类型不匹配时，则会跳过该属性。

通过 `typeOf<T>()` 传入精确的具体 KPojo 目标类型。

- **函数声明**

    ```kotlin
    fun KPojo.mapperTo(type: KType): Any
    ```

- **使用示例**

    ```kotlin
    val student = User(id = 1, name = "Tom").mapperTo(typeOf<Student>())
    ```

- **接收参数**

  {{ $.params([['type', '完整实体目标类型', 'KType']]) }}

- **返回值**

  `Any` 实体对象

{{ $.hr() }}

### 4. {{ $.title("KPojo.mapperTo<T: KPojo>()")}}

通过实体对象转换为另一个实体对象，当实体对象属性的类型与目标实体对象属性的类型不匹配时，则会跳过该属性。

- **函数声明**

    ```kotlin
    inline fun <reified T : KPojo> KPojo.mapperTo(): T
    ```

- **使用示例**

    ```kotlin
    val student = User(id = 1, name = "Tom").mapperTo<Student>()
    ```

- **返回值**

  `T` 实体对象

{{ $.hr() }}

## Safe Mapper to 安全转换

SafeMapperTo共包含4个函数用于类型转换，分别是：

### 1. {{ $.title("Map<String, Any?>.safeMapperTo(KType)")}}

通过 Map 转换为实体对象。当值与属性类型不匹配时，统一转换注册表会尝试安全转换，详见 {{ $.keyword("configuration/value-codec", ["概念", "ValueCodec"]) }}。

通过 `typeOf<T>()` 传入完整目标类型。泛型 KPojo 构造留待后续版本支持，当前目标必须是具体、非泛型的 KPojo 类型。

- **函数声明**

    ```kotlin
    fun Map<String, Any?>.safeMapperTo(type: KType): Any
    ```

- **使用示例**

    ```kotlin
    val user = mapOf("id" to "1", "name" to "Tom").safeMapperTo(typeOf<User>())
    ```

- **接收参数**

    {{ $.params([['type', '完整实体目标类型', 'KType']]) }}

- **返回值**

  `Any` 实体对象

{{ $.hr() }}

### 2. {{ $.title("Map<String, Any?>.safeMapperTo<T: KPojo>()")}}

通过 Map 转换为实体对象。当值与属性类型不匹配时，统一转换注册表会尝试安全转换，详见 {{ $.keyword("configuration/value-codec", ["概念", "ValueCodec"]) }}。

- **函数声明**

    ```kotlin
    inline fun <reified T : KPojo> Map<String, Any?>.safeMapperTo(): T
    ```

- **使用示例**

    ```kotlin
    val user = mapOf("id" to "1", "name" to "Tom").safeMapperTo<User>()
    ```

- **返回值**

    `T` 实体对象

{{ $.hr() }}

### 3. {{ $.title("KPojo.safeMapperTo(KType)")}}

通过实体对象转换为另一个实体对象。源属性与目标属性类型不匹配时会尝试安全转换，详见 {{ $.keyword("configuration/value-codec", ["概念", "ValueCodec"]) }}。

通过 `typeOf<T>()` 传入精确的具体 KPojo 目标类型。

- **函数声明**

    ```kotlin
    fun KPojo.safeMapperTo(type: KType): Any
    ```

- **使用示例**

    ```kotlin
    val student = User(id = 1, name = "Tom").safeMapperTo(typeOf<Student>())
    ```

- **接收参数**

    {{ $.params([['type', '完整实体目标类型', 'KType']]) }}

- **返回值**

    `Any` 实体对象

{{ $.hr() }}

### 4. {{ $.title("KPojo.safeMapperTo<T: KPojo>()")}}

通过实体对象转换为另一个实体对象。源属性与目标属性类型不匹配时会尝试安全转换，详见 {{ $.keyword("configuration/value-codec", ["概念", "ValueCodec"]) }}。

- **函数声明**

    ```kotlin
    inline fun <reified T : KPojo> KPojo.safeMapperTo(): T
    ```

- **使用示例**

    ```kotlin
    val student = User(id = 1, name = "Tom").safeMapperTo<Student>()
    ```

- **返回值**

    `T` 实体对象
