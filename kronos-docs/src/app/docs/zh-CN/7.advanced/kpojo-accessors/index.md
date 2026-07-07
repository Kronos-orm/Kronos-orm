{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.title("KPojo") }}的{{ $.title("Get") }}属性访问器

在Kronos中，我们可以使用`KPojo`的`get`属性访问器来访问数据表的字段。

```kotlin group="Get" name="kotlin" icon="kotlin"
data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo

val user = User(
    id = 1,
    name = "Kronos",
    age = 18
)

val valueOfId = user["id"] // 1
val valueOfName = user["name"] // "Kronos"
val valueOfAge = user["age"] // 18
```

```text group="Get" name="result"
id -> 1
name -> Kronos
age -> 18
不存在的字段 -> null
```

## {{ $.title("KPojo") }}的{{ $.title("Set") }}属性访问器

在Kronos中，我们可以使用`KPojo`的`set`属性访问器来修改数据表的字段。

```kotlin group="Set" name="kotlin" icon="kotlin"
data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo

val user = User(
    id = 1,
    name = "Kronos",
    age = 18
)

user["id"] = 2 // 不可变的值也是可以通过该访问器修改的
user["name"] = "Kronos" // 不可变的值也是可以通过该访问器修改的
user["age"] = 20 // 不可变的值也是可以通过该访问器修改的
```

```text group="Set" name="result"
user.id == 2
user.name == "Kronos"
user.age == 20
```

泛型代码在运行时拿到字段名时，可以使用这些访问器。使用同一 KPojo 字段元数据的 Map 转换 API 见 {{ $.keyword("advanced/mapper-to", ["Map/KPojo 类型转换"]) }}。
