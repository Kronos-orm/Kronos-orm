{% import "../../../macros/macros-zh-CN.njk" as $ %}

**How does Kronos implement instantiating KPojo's generic KClass without relying on reflection? **

Kronos detects and generates a mapping table from `KClass<KPojo>` to `KPojo` by means of a compiler plugin at compile time.

So you can write the following code:

```kotlin
import com.kotlinorm.utils.createInstance

data class User(val id: Int? = null, val name: String = ""): KPojo

val instance = User::class.createInstance() // -> Equivalent to User()

inline fun <reified T: KPojo> createInstance(): T {
    // Reflection will not be used, but the mapping table will be called directly, equivalent to T()
    return T::class.createInstance()
}
```

**This mapping table is directly generated as a `when function`, which neither occupies additional memory nor affects performance.**

The expressions we detect include:
1. class declarations such as `class User: KPojo`
2. class constructors such as `val a = User()`.
3. class references such as `User::class`.

**If the class you are referencing is not detected** (e.g. referenced from some third-party library or other building block and instantiated via a non-constructor function), you can customize the instantiation logic by `registerKPojo` or setting `kCreatorCustom`.

```kotlin
registerKPojo(User::class, Permissions::class, ...)
// or
kCreatorCustom = { kClass ->
    when (kClass) {
        User::class -> User()
        Permissions::class -> Permissions()
        else -> null
    }
}

// If a large number of classes are undetected, consider using reflection instantiation:
// import kotlin.reflect.full.createInstance
// kCreatorCustom = { kClass -> kClass.createInstance() }
```