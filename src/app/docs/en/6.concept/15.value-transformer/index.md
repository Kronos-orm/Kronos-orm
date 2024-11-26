{% import "../../../macros/macros-en.njk" as $ %}

Kronos automatically populates the properties of a KPojo object based on the Map's key-value pairs when converting Map values to KPojo objects.

Without `strictMode` turned on (see: {{$.keyword("getting-started/global-config", ["Global Configuration", "Turn off Smart Value Transformations"])}}), if the Map's key-value pairs don't match the type of the KPojo object, Kronos will pass ` ValueTransformer` to implement the type conversion.

## Default Value Transformer

Kronos provides a default value transformer that supports basic type conversions and date conversions. You can also create and register custom value transformers.

### 1. {{ $.title("BasicTypeTransformer") }} **Basic Type Transformer**

This transformer supports the following basic types of mutual conversion and conversion with strings.

- `kotlin.Int`
- `kotlin.Long`
- `kotlin.Double`
- `kotlin.Float`
- `kotlin.Boolean`
- `kotlin.Char`
- `kotlin.Byte`
- `kotlin.Short`

Conversion Example:

```kotlin
val res1: Int = getTypeSafeValue("kotlin.Int", "42")
val res2: Long = getTypeSafeValue("kotlin.Long", "42")
val res3: Long = getTypeSafeValue("kotlin.Long", 42.0)
val res4: Short = getTypeSafeValue("kotlin.Short", 42)
val res5: Char = getTypeSafeValue("kotlin.Char", 65) // 'A'
val res6: Boolean = getTypeSafeValue("kotlin.Boolean", "true")
val res7: Boolean = getTypeSafeValue("kotlin.Boolean", 0)
```

### 2. {{ $.title("JvmDateTransformer") }} **Jvm Date Transformer**

This transformer supports the following date types of mutual conversion and conversion with strings.

- `java.time.LocalDateTime`
- `java.time.LocalDate`
- `java.time.LocalTime`
- `java.time.Instant`
- `java.time.ZonedDateTime`
- `java.time.OffsetDateTime`
- `java.util.Date`
- `java.sql.Date`

Conversion Example:

(Datetime will be converted to the default format {{ $.keyword("getting-started/global-config", ["Global Configuration", "Default Date Time Format"]) }}))

```kotlin
val res1: LocalDateTime = getTypeSafeValue("java.time.LocalDateTime", "2023-10-17T10:00:00")

val res2: LocalDateTime = getTypeSafeValue("kotlin.String", res1) // 2023-10-17 10:00:00
```

### 3. {{ $.title("ToStringTransformer") }} **ToString Transformer**

This transformer converts any type to a string.

Conversion Example:

```kotlin
val res1: String = getTypeSafeValue("kotlin.String", someObject)
```

> **Note**
> For more conversion examples, please refer to：
> [Kronos Test Cases](https://github.com/Kronos-orm/Kronos-orm/blob/d42270658c589f86f39bb6a44e06905acfa79c48/kronos-testing/src/test/kotlin/com/kotlinorm/utils/CommonUtilTest.kt#L60)

{{ $.hr() }}

## create, register custom value transformer

You can customize the value transformer by implementing the `ValueTransformer` interface for custom types.

### ValueTransformer Interface

#### 1. {{ $.title("isMatch") }} **Match Type**

Whether this converter can be used to convert the specified type.

- **Function Declaration**

  ```kotlin
  fun isMatch(
      targetKotlinType: String,
      superTypesOfValue: List<String>,
      kClassOfValue: KClass<*>
  ): Boolean
  ```

- **Usage Example**
  
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

- **Parameters**

  {{$.params([['targetKotlinType', 'Target type', 'String'], ['superTypesOfValue', 'Superclass of the value', 'List<String>'], ['kClassOfValue', 'KClass of the value', 'KClass<*>']])}}

- **Return Value**

  `Boolean` - Whether the converter can be used to convert the specified type.

#### 2. {{ $.title("transform") }} **Type Conversion**

Convert the value to the specified type.

- **Function Declaration**

  ```kotlin
  fun transform(
      targetKotlinType: String,
      value: Any,
      superTypesOfValue: List<String>,
      dateTimeFormat: String?,
      kClassOfValue: KClass<*>
  ): Any
  ```

- **Usage Example**

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

- **Parameters**

  {{$.params([['targetKotlinType', 'Target type', 'String'], ['value', 'Value', 'Any'], ['superTypesOfValue', 'Superclass of the value', 'List<String>'], ['dateTimeFormat', 'Date format', 'String?'], ['kClassOfValue', 'KClass of the value', 'KClass<*>']])}}

- **返回值**

  `Any` - Converted value

### Register Custom Value Transformer

You can register custom value transformers through the `registerValueTransformer` function.

```kotlin
Kronos.init {
    registerValueTransformer(TestTransformer())
}
```
{{ $.hr() }}

## Example

The following example shows how to implement a custom value transformer.

### Example 1: Enum Type Transformer

This transformer supports the mutual conversion of enum types and conversion with strings.

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

// Custom value transformer
class TestEnumTransformer : ValueTransformer {
    override fun isMatch(
        targetKotlinType: String,
        superTypesOfValue: List<String>,
        kClassOfValue: KClass<*>
    ): Boolean {
        // Determine whether the target type is an enum type and the value type is a string
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

    // Only the `safeMapperTo` function supports custom value converters, while the `mapperTo` function does not support this.
    val pojo = map.safeMapperTo<TestPojo>()
}
```

### Example 2: kotlinx.datetime Type Transformer

The converter supports conversion of `kotlinx.datetime` date types to each other and to strings, Long, etc.

[Reference Example](https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-testing/src/test/kotlin/com/kotlinorm/utils/KotlinXDateTimeTransformer.kt)