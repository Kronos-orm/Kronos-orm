{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## The {{ $.title("Get") }} property accessor of {{ $.title("KPojo") }}

In Kronos, we can use `KPojo`'s `get` property accessor to access the fields of a data table.

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
missing field -> null
```

## The {{ $.title("Set") }} property accessor of {{ $.title("KPojo") }}

In Kronos, we can use `KPojo`'s `set` attribute accessor to modify the fields of a data table.

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

user["id"] = 2 // Immutable values are also modifiable by this accessor
user["name"] = "Kronos" // Immutable values are also modifiable by this accessor
user["age"] = 20 // Immutable values are also modifiable by this accessor
```

```text group="Set" name="result"
user.id == 2
user.name == "Kronos"
user.age == 20
```

Use these accessors when generic code receives field names at runtime. For map conversion APIs that use the same KPojo field metadata, see {{ $.keyword("advanced/mapper-to", ["Map/KPojo Conversion"]) }}.
