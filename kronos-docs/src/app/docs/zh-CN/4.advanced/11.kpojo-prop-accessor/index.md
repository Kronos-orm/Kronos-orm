{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.title("KPojo") }}的{{ $.title("Get") }}属性访问器

在Kronos中，我们可以使用`KPojo`的`get`属性访问器来访问数据表的字段。

```kotlin
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

这种写法看起来就像使用了反射，但实际上是Kronos在编译时生成了访问器方法。

```kotlin
class User {
   open operator fun get(name: String): Any? {
       return when (name) {
           "ID" -> id
           "name" -> name
           "age" -> age
           else -> null
       }
   }
}
```

## {{ $.title("KPojo") }}的{{ $.title("Set") }}属性访问器

在Kronos中，我们可以使用`KPojo`的`set`属性访问器来修改数据表的字段。

```kotlin
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

这种写法看起来就像使用了反射，但实际上是Kronos在编译时生成了访问器方法。

```kotlin
class User {
    open operator fun set(name: String, value: Any?) {
         when (name) {
              "id" -> id = value as Int?
              "name" -> name = value as String?
              "age" -> age = value as Int?
              else -> throw IllegalArgumentException("No such property: $name")
         }
    }
}
```
