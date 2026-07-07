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

### 1. {{ $.title("Map<String, Any?>.mapperTo(KClass<out KPojo>)")}}

通过Map转换为实体对象，当Map中的值与实体对象属性的类型不匹配时，则会跳过该属性。

kClass可以是一个协变的KPojo类型，也可以是一个具体的实体对象类型。

- **函数声明**

    ```kotlin
    fun Map<String, Any?>.mapperTo(kClass: KClass<out KPojo>): Any
    ```

- **使用示例**

    ```kotlin
    val user = mapOf("id" to 1, "name" to "Tom").mapperTo(User::class)
    ```

- **接收参数**

  {{ $.params([['kClass', '实体对象类型', 'KClass<KPojo>']]) }}

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

### 3. {{ $.title("KPojo.mapperTo(KClass<out KPojo>)")}}

通过实体对象转换为另一个实体对象，当实体对象属性的类型与目标实体对象属性的类型不匹配时，则会跳过该属性。

kClass可以是一个协变的KPojo类型，也可以是一个具体的实体对象类型。

- **函数声明**

    ```kotlin
    fun KPojo.mapperTo(kClass: KClass<out KPojo>): Any
    ```

- **使用示例**

    ```kotlin
    val student = User(id = 1, name = "Tom").mapperTo(Student::class)
    ```

- **接收参数**

  {{ $.params([['kClass', '实体对象类型', 'KClass<KPojo>']]) }}

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

### 1. {{ $.title("Map<String, Any?>.safeMapperTo(KClass<out KPojo>)")}}

通过Map转换为实体对象，当Map中的值与实体对象属性的类型不匹配时，会尝试进行类型转换，详见：{{ $.keyword("configuration/value-transformer", ["概念", "值转换器"]) }}。

kClass可以是一个协变的KPojo类型，也可以是一个具体的实体对象类型。

- **函数声明**

    ```kotlin
    fun Map<String, Any?>.safeMapperTo(kClass: KClass<out KPojo>): Any
    ```

- **使用示例**

    ```kotlin
    val user = mapOf("id" to "1", "name" to "Tom").safeMapperTo(User::class)
    ```

- **接收参数**

    {{ $.params([['kClass', '实体对象类型', 'KClass<KPojo>']]) }}

- **返回值**

  `Any` 实体对象

{{ $.hr() }}

### 2. {{ $.title("Map<String, Any?>.safeMapperTo<T: KPojo>()")}}

通过Map转换为实体对象，当Map中的值与实体对象属性的类型不匹配时，会尝试进行类型转换，详见：{{ $.keyword("configuration/value-transformer", ["概念", "值转换器"]) }}。

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

### 3. {{ $.title("KPojo.safeMapperTo(KClass<out KPojo>)")}}

通过实体对象转换为另一个实体对象，当实体对象属性的类型与目标实体对象属性的类型不匹配时，会尝试进行类型转换，详见：{{ $.keyword("configuration/value-transformer", ["概念", "值转换器"]) }}。

kClass可以是一个协变的KPojo类型，也可以是一个具体的实体对象类型。

- **函数声明**

    ```kotlin
    fun KPojo.safeMapperTo(kClass: KClass<out KPojo>): Any
    ```

- **使用示例**

    ```kotlin
    val student = User(id = 1, name = "Tom").safeMapperTo(Student::class)
    ```

- **接收参数**

    {{ $.params([['kClass', '实体对象类型', 'KClass<KPojo>']]) }}

- **返回值**

    `Any` 实体对象

{{ $.hr() }}

### 4. {{ $.title("KPojo.safeMapperTo<T: KPojo>()")}}

通过实体对象转换为另一个实体对象，当实体对象属性的类型与目标实体对象属性的类型不匹配时，会尝试进行类型转换，详见：{{ $.keyword("configuration/value-transformer", ["概念", "值转换器"]) }}。

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
