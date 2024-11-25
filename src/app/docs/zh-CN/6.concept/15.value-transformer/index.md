{% import "../../../macros/macros-zh-CN.njk" as $ %}

Kronos在将Map值转换为KPojo对象时，会根据Map的键值对自动填充KPojo对象的属性。

在没有开启`strictMode`的情况下（见：{{$.keyword("getting-started/global-config", ["全局配置", "关闭智能值转换"])
}}），如果Map的键值对与KPojo对象的类型不匹配，Kronos会通过`ValueTransformer`实现类型转换。

## 默认转换器

Kronos提供了默认的值转换器，支持以下类型的转换：

### 1. {{ $.title("BasicTypeTransformer") }} **基本类型转换器**

该转换器支持基本类型的相互转换，包括：

- `kotlin.Int`
- `kotlin.Long`
- `kotlin.Double`
- `kotlin.Float`
- `kotlin.Boolean`
- `kotlin.Char`
- `kotlin.Byte`
- `kotlin.Short`

转换示例：

```kotlin
val res1: Int = getTypeSafeValue("kotlin.Int", "42")
val res2: Long = getTypeSafeValue("kotlin.Long", "42")
val res3: Long = getTypeSafeValue("kotlin.Long", 42.0)
val res4: Short = getTypeSafeValue("kotlin.Short", 42)
val res5: Char = getTypeSafeValue("kotlin.Char", 65) // 'A'
val res6: Boolean = getTypeSafeValue("kotlin.Boolean", "true")
val res7: Boolean = getTypeSafeValue("kotlin.Boolean", 0)
```

### 2. {{ $.title("JvmDateTransformer") }} **JVM日期转换器**

该转换器支持以下日期类型的相互转换以及与字符串、Long等类型的转换。

- `java.time.LocalDateTime`
- `java.time.LocalDate`
- `java.time.LocalTime`
- `java.time.Instant`
- `java.time.ZonedDateTime`
- `java.time.OffsetDateTime`
- `java.util.Date`
- `java.sql.Date`

转换示例：

```kotlin
val res1: LocalDateTime = getTypeSafeValue("java.time.LocalDateTime", "2023-10-17T10:00:00")

val res2: LocalDateTime = getTypeSafeValue("kotlin.String", res1) // 2023-10-17 10:00:00(根据全局设置-默认日期格式)
```

### 3. {{ $.title("ToStringTransformer") }} **ToString转换器**

该转换器用于将任意类型转换为字符串。

转换示例：

```kotlin
val res1: String = getTypeSafeValue("kotlin.String", someObject)
```

> **Note**
> 更多转换示例请参考：
> [Kronos测试用例](https://github.com/Kronos-orm/Kronos-orm/blob/d42270658c589f86f39bb6a44e06905acfa79c48/kronos-testing/src/test/kotlin/com/kotlinorm/utils/CommonUtilTest.kt#L60)

{{ $.hr() }}

## 创建、注册自定义值转换器

您可以通过实现`ValueTransformer`接口来自定义值转换器，实现自定义类型的转换。

### ValueTransformer接口

#### 1. {{ $.title("isMatch") }} **是否匹配**

该转换器是否可以用于转换指定的类型。

- **函数声明**

  ```kotlin
  fun isMatch(
      targetKotlinType: String,
      superTypesOfValue: List<String>,
      kClassOfValue: KClass<*>
  ): Boolean
  ```

- **使用示例**
  
   ```kotlin
  class TestTransformer : ValueTransformer {
      override fun isMatch(
          targetKotlinType: String,
          superTypesOfValue: List<String>,
          kClassOfValue: KClass<*>
      ): Boolean {
          return targetKotlinType == "com.example.Test" && kClassOfValue == String::class
  }
   ```

- **参数**

{{
$.params([['targetKotlinType', '目标类型', 'String'], ['superTypesOfValue', '值的超类', 'List<String>'], ['kClassOfValue', '值的KClass', 'KClass<*>']]) }}

- **返回值**

`Boolean` - 是否匹配

#### 2. {{ $.title("transform") }} **转换值**

将值转换为目标类型。

- **函数声明**

  ```kotlin
  fun transform(
      targetKotlinType: String,
      value: Any,
      superTypesOfValue: List<String>,
      dateTimeFormat: String?,
      kClassOfValue: KClass<*>
  ): Any
  ```

- **使用示例**

  ```kotlin
  class TestTransformer : ValueTransformer {
      override fun transform(
          targetKotlinType: String,
          value: Any,
          superTypesOfValue: List<String>,
          dateTimeFormat: String?,
          kClassOfValue: KClass<*>
      ): Any {
          return Test(value as String)
      }
  }
  ```

- **参数**

{{
$.params([['targetKotlinType', '目标类型', 'String'], ['value', '值', 'Any'], ['superTypesOfValue', '值的超类', 'List<String>'], ['dateTimeFormat', '日期格式', 'String?'], ['kClassOfValue', '值的KClass', 'KClass<*>']]) }}

- **返回值**

`Any` - 转换后的值

### 注册自定义值转换器

您可以通过以下方式注册自定义值转换器：

```kotlin
Kronos.init {
    registerValueTransformer(TestTransformer())
}
```
{{ $.hr() }}

## 自定义转换器示例

以下示例展示了如何实现自定义值转换器：

### 示例1：枚举类型转换器

数据库中字符串类型的枚举值转换为枚举类型。

```kotlin
enum class TestEnum {
    A, B, C
}

data class TestPojo(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null,
    val testEnum: TestEnum? = null
) : KPojo

// 自定义枚举类型转换器
class TestEnumTransformer : ValueTransformer {
    override fun isMatch(
        targetKotlinType: String,
        superTypesOfValue: List<String>,
        kClassOfValue: KClass<*>
    ): Boolean {
        // 判断是否为TestEnum类型，且值类型为String
        return targetKotlinType == TestEnum::class.qualifiedName && kClassOfValue == String::class
    }

    override fun transform(
        targetKotlinType: String,
        value: Any,
        superTypesOfValue: List<String>,
        dateTimeFormat: String?,
        kClassOfValue: KClass<*>
    ): Any {
        return TestEnum.valueOf(value as String)
    }
}

fun main(){
    registerValueTransformer(TestEnumTransformer())
    val map = mapOf(
        "id" to 1,
        "name" to "test",
        "age" to 18,
        "testEnum" to "A"
    )

    // 请注意：仅`safeMapperTo`函数支持自定义值转换器，`mapperTo`函数不支持
    val pojo = map.safeMapperTo<TestPojo>()
}
```

### 示例2：kotlinx.datetime类型转换器

该转换器支持`kotlinx.datetime`日期类型的相互转换以及与字符串、Long等类型的转换。

[参考示例](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-testing/src/test/kotlin/com/kotlinorm/utils/KotlinXDateTimeTransformer.kt)