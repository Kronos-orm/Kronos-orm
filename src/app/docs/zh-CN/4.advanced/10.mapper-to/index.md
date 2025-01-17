{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Kronos提供了一组函数，用于实体对象与Map之间的转换，这些函数可以帮助您在实体对象与Map之间进行快速转换。

**Kronos的实现不依赖于反射，而是通过编译期生成的代码实现的，因此转换效率和性能非常高。**

我们为KPojo生成了toMap()和fromMap()函数，用于实体对象与Map之间的转换:
```kotlin
data class User(
    val id: Int? = null,
    val name: String? = null
) : KPojo

//我们会为User生成以下函数
data class User {
    fun toMap(): Map<String, Any?>{
        return mapOf("id" to id, "name" to name)
    }
    
    fun fromMap(map: Map<String, Any?>): User{
        return User(
            id = try { map["id"] as Int? } catch (e: IllegalArgumentException) { throw CastException() },
            name = try { map["name"] as String? } catch (e: IllegalArgumentException) { throw CastException() }
        )
    }
    
    fun safeFromMap(map: Map<String, Any?>): User{
        return User(
            id = safe(map["id"] as Int?),
            name = safe(map["name"] as String?)
        )
    }


}
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

### 1. {{ $.title("Map<String, Any?>.mapperTo(KClass<KPojo>)")}}

通过Map转换为实体对象，当Map中的值与实体对象属性的类型不匹配时，会抛出类型转换异常。

kClass可以是一个协变的KPojo类型，也可以是一个具体的实体对象类型。

- **函数声明**

    ```kotlin
    fun Map<String, Any?>.safeMapperTo(kClass: KClass<KPojo>): Any
    ```

- **使用示例**

    ```kotlin
    val user = mapOf("id" to 1, "name" to "Tom").safeMapperTo(User::class)
    ```

- **接收参数**

  {{ $.params([['kClass', '实体对象类型', 'KClass<KPojo>']]) }}

- **返回值**

  `Any` 实体对象

{{ $.hr() }}

### 2. {{ $.title("Map<String, Any?>.mapperTo<T: KPojo>()")}}

通过Map转换为实体对象，当Map中的值与实体对象属性的类型不匹配时，会抛出类型转换异常。

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

### 3. {{ $.title("KPojo.mapperTo(KClass<KPojo>)")}}

通过实体对象转换为另一个实体对象，当实体对象属性的类型与目标实体对象属性的类型不匹配时，会抛出类型转换异常。

kClass可以是一个协变的KPojo类型，也可以是一个具体的实体对象类型。

- **函数声明**

    ```kotlin
    fun KPojo.safeMapperTo(kClass: KClass<KPojo>): Any
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

### 4. {{ $.title("KPojo.mapperTo<T: KPojo>()")}}

通过实体对象转换为另一个实体对象，当实体对象属性的类型与目标实体对象属性的类型不匹配时，会抛出类型转换异常。

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

### 1. {{ $.title("Map<String, Any?>.safeMapperTo(KClass<KPojo>)")}}

通过Map转换为实体对象，当Map中的值与实体对象属性的类型不匹配时，会尝试进行类型转换，详见：{{ $.keyword("
concept/value-transformer", ["概念", "值转换器"]) }}。

kClass可以是一个协变的KPojo类型，也可以是一个具体的实体对象类型。

- **函数声明**

    ```kotlin
    fun Map<String, Any?>.safeMapperTo(kClass: KClass<KPojo>): Any
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

通过Map转换为实体对象，当Map中的值与实体对象属性的类型不匹配时，会尝试进行类型转换，详见：{{ $.keyword("
concept/value-transformer", ["概念", "值转换器"]) }}。

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

### 3. {{ $.title("KPojo.safeMapperTo(KClass<KPojo>)")}}

通过实体对象转换为另一个实体对象，当实体对象属性的类型与目标实体对象属性的类型不匹配时，会尝试进行类型转换，详见：{{ $
.keyword("
concept/value-transformer", ["概念", "值转换器"]) }}。

kClass可以是一个协变的KPojo类型，也可以是一个具体的实体对象类型。

- **函数声明**

    ```kotlin
    fun KPojo.safeMapperTo(kClass: KClass<KPojo>): Any
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

通过实体对象转换为另一个实体对象，当实体对象属性的类型与目标实体对象属性的类型不匹配时，会尝试进行类型转换，详见：{{ $.keyword("
concept/value-transformer", ["概念", "值转换器"]) }}。

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


