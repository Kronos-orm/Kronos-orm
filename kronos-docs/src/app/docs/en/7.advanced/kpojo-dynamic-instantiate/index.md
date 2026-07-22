{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Create a KPojo from KType

Use `Kronos.createKPojo(type)` when generic code has a complete `KType` and needs a fresh KPojo instance. The runtime uses compiler-generated or explicitly registered non-reflective factories.

```kotlin group="Create 1" name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.Kronos
import kotlin.reflect.typeOf

data class User(
    var id: Int? = null,
    var name: String = ""
) : KPojo

val instance = Kronos.createKPojo(typeOf<User>())
// User(id = null, name = "")
```

Generic helper functions can use the same API.

```kotlin group="Create 2" name="generic helper" icon="kotlin"
inline fun <reified T : KPojo> newKPojo(): T {
    return Kronos.createKPojo(typeOf<T>()) as T
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

Use `Kronos.registerKPojoFactory` when a model type is provided from another module, a third-party package, or a factory boundary. The registration key is the exact complete `KType`; generic KPojo types are not supported in this release.

```kotlin group="Register 1" name="single type" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.utils.KPojoFactory
import kotlin.reflect.typeOf

val userRegistration = Kronos.registerKPojoFactory(typeOf<User>(), KPojoFactory { _ -> User() })
val permissionsRegistration = Kronos.registerKPojoFactory(
    typeOf<Permissions>(),
    KPojoFactory { _ -> Permissions() }
)
```

Each registration covers one exact type. Later registrations for the same type have higher priority; call `close()` to remove only that override and reveal the previous user or generated factory.

```kotlin group="Register 2" name="lifecycle" icon="kotlin"
val user = Kronos.createKPojo(typeOf<User>())
userRegistration.close()
permissionsRegistration.close()
```

## Handle Missing Factories

When no generated or registered factory matches the `KType`, `createKPojo` fails with a clear message. Enable the Kronos compiler plugin for the module that declares the KPojo, or register an explicit constructor.

```kotlin group="Missing factory 1" name="kotlin" icon="kotlin"
data class ExternalUser(
    val id: Int
) : KPojo

Kronos.createKPojo(typeOf<ExternalUser>())
```

```text group="Missing factory 1" name="result"
KType ... instantiation failed.
No generated or registered KPojo factory matched this class.
```

Register the constructor when the class cannot use generated factories.

```kotlin group="Missing factory 2" name="registration" icon="kotlin"
val registration = Kronos.registerKPojoFactory(
    typeOf<ExternalUser>(),
    KPojoFactory { _ -> ExternalUser(id = 0) }
)

val external = Kronos.createKPojo(typeOf<ExternalUser>())
// ExternalUser(id = 0)
```

For map conversion APIs that use the same instantiation path, see {{ $.keyword("advanced/mapper-to", ["Map/KPojo Conversion"]) }}.
