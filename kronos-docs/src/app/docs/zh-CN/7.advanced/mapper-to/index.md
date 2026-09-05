{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

应用接收 Map 形式的输入、准备接口响应或在模型间复制数据时，可以在 {{ $.code("KPojo") }} 模型与 `Map<String, Any?>` 之间转换值。

## 选择 API

| 任务 | API |
|------|-----|
| 将模型读取为 Map | `toDataMap()` |
| 用已符合属性类型的值创建新模型 | `mapperTo()` |
| 用需要 Kronos 常规转换的值创建新模型 | `safeMapperTo()` |
| 将已符合属性类型的值填入已有模型 | `fromMapData()` |
| 将需要 Kronos 常规转换的值填入已有模型 | `safeFromMapData()` |

## 从完整示例开始

`mapperTo` 适合处理 `toDataMap` 生成的 Map。`safeMapperTo` 适合 JSON 或表单等输入场景，其中数字可能以字符串形式传入。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.mapperTo
import com.kotlinorm.utils.Extensions.safeMapperTo

data class UserDto(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

val typedInput = mapOf("id" to 1, "name" to "Ada")
val copied = typedInput.mapperTo<UserDto>()
// UserDto(id = 1, name = "Ada")

val requestInput = mapOf("id" to "2", "name" to "Lin")
val converted = requestInput.safeMapperTo<UserDto>()
// UserDto(id = 2, name = "Lin")
```

## 将模型读取为 Map

调用 `toDataMap()` 可以取得模型的已映射属性名和属性值。

```kotlin name="kotlin" icon="kotlin"
val data = UserDto(id = 1, name = "Ada").toDataMap()
// {id=1, name=Ada}
```

## 创建新模型

Map 中的值已经符合 `T` 声明的 Kotlin 类型时，使用 `mapperTo<T>()`。输入值需要转换时，例如将 `"2"` 转为 `Int`，使用 `safeMapperTo<T>()`。

安全转换 API 也会使用应用已注册的映射规则。模型属性使用 `Money` 等领域值时，配置方式见 {{ $.keyword("configuration/value-codec", ["自定义值映射"]) }}。

```kotlin name="kotlin" icon="kotlin"
val direct = mapOf("id" to 3, "name" to "Mika").mapperTo<UserDto>()
val converted = mapOf("id" to "4", "name" to "Noa").safeMapperTo<UserDto>()
```

## 填充已有模型

应用已经持有目标模型时，使用 `fromMapData()` 或 `safeFromMapData()`。两个函数都会在填入提供的 key 后返回同一个模型；缺少的 key 会保留当前属性值。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo

data class UserDto(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

val user = UserDto(id = 1, name = "Draft")

user.fromMapData<UserDto>(mapOf("name" to "Published"))
// UserDto(id = 1, name = "Published")

user.safeFromMapData<UserDto>(mapOf("id" to "2"))
// UserDto(id = 2, name = "Published")
```

## 在模型间复制

{{ $.code("KPojo") }} 源对象也提供相同的新对象 API。它们会将同名属性复制到新的目标模型。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.mapperTo

data class UserRecord(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class UserCard(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

val card = UserRecord(id = 7, name = "Kai").mapperTo<UserCard>()
// UserCard(id = 7, name = "Kai")
```

同名属性需要转换时，使用 `source.safeMapperTo<T>()`。

## 运行时选择目标类型

代码需要在运行时选择目标模型时，可以使用 `mapperTo(type: KType)` 和 `safeMapperTo(type: KType)` 重载。目标类型已在源码中确定时，优先使用上面的泛型形式。
