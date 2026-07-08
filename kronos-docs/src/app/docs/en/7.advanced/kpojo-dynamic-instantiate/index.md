{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Create a KPojo from KClass

Use `createInstance()` when generic code receives a `KClass` and needs a KPojo instance. Kronos uses generated or registered KPojo factories for this API.

```kotlin group="Create 1" name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.createInstance

data class User(
    var id: Int? = null,
    var name: String = ""
) : KPojo

val instance = User::class.createInstance()
// User(id = null, name = "")
```

Generic helper functions can use the same API.

```kotlin group="Create 2" name="generic helper" icon="kotlin"
inline fun <reified T : KPojo> newKPojo(): T {
    return T::class.createInstance()
}

val user = newKPojo<User>()
// User(id = null, name = "")
```

Result shape:

```text group="Create 3" name="result"
The returned value has the requested KPojo type.
Constructor parameters use the same defaults as a normal User() call.
```

## Register Explicit Constructors

Use `registerKPojo` when a model type is provided from another module, a third-party package, or a factory boundary.

```kotlin group="Register 1" name="single type" icon="kotlin"
import com.kotlinorm.utils.registerKPojo

registerKPojo(User::class) { User() }
registerKPojo(Permissions::class) { Permissions() }
```

When the target type is known at the call site, the reified overload keeps the same registration in one place.

```kotlin group="Register 2" name="reified" icon="kotlin"
import com.kotlinorm.utils.registerKPojo

registerKPojo<User> { User() }
```

Use `registerKPojoFactory` when one registration should cover several model types.

```kotlin group="Register 3" name="factory" icon="kotlin"
import com.kotlinorm.utils.registerKPojoFactory

registerKPojoFactory { kClass ->
    when (kClass) {
        User::class -> User()
        Permissions::class -> Permissions()
        else -> null
    }
}
```

The factory returns `null` for classes it does not own, allowing later factories to handle the same request.

```text group="Register 4" name="result"
User::class.createInstance() returns User().
Permissions::class.createInstance() returns Permissions().
Unknown KPojo classes continue to the next registered factory.
```

## Handle Missing Factories

When no generated or registered factory matches the class, `createInstance()` fails with a clear message. Enable the Kronos compiler plugin for the module that declares the KPojo, or register an explicit constructor.

```kotlin group="Missing factory 1" name="kotlin" icon="kotlin"
data class ExternalUser(
    val id: Int
) : KPojo

ExternalUser::class.createInstance()
```

```text group="Missing factory 1" name="result"
KClass ... instantiation failed.
No generated or registered KPojo factory matched this class.
```

Register the constructor when the class cannot use generated factories.

```kotlin group="Missing factory 2" name="registration" icon="kotlin"
registerKPojo(ExternalUser::class) { ExternalUser(id = 0) }

val external = ExternalUser::class.createInstance()
// ExternalUser(id = 0)
```

For map conversion APIs that use the same instantiation path, see {{ $.keyword("advanced/mapper-to", ["Map/KPojo Conversion"]) }}.
