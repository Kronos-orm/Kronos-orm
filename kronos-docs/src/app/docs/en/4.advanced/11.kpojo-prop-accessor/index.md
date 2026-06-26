{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## The {{ $.title("Get") }} property accessor of {{ $.title("KPojo") }}

In Kronos, we can use `KPojo`'s `get` property accessor to access the fields of a data table.

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

This writing style looks like it uses reflection, but in fact, Kronos generates accessor methods at compile time.
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

## The {{ $.title("Set") }} property accessor of {{ $.title("KPojo") }}

In Kronos, we can use `KPojo`'s `set` attribute accessor to modify the fields of a data table.

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

user["id"] = 2 // Immutable values are also modifiable by this accessor
user["name"] = "Kronos" // Immutable values are also modifiable by this accessor
user["age"] = 20 // Immutable values are also modifiable by this accessor
```

This writing style looks like it uses reflection, but in fact, Kronos generates accessor methods at compile time.

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
