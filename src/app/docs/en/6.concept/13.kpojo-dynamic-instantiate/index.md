{% import "../../../macros/macros-zh-CN.njk" as $ %}

**Kronos是如何不依赖反射实现将KPojo的泛型KClass实例化的？**

Kronos通过编译器插件，在编译期检测并生成了一个由`KClass<KPojo>`到`KPojo`的映射表，通过这个映射表来实现。

因此您可以写出如下代码：

```kotlin
import com.kotlinorm.utils.createInstance

data class User(val id: Int? = null, val name: String = ""): KPojo

val instance = User::class.createInstance() // -> 等同于 User()

inline fun <reified T: KPojo> createInstance(): T {
    // 不会使用反射，而是直接调用映射表，等同于 T()
    return T::class.createInstance()
}
```

**这个映射表直接生成为when函数，既不会占用额外的内存，也不会影响性能**。

我们检测的表达式包括：
1. class声明 如 `class User: KPojo`
2. class的构造函数 如 `val a = User()`
3. class的引用 如 `User::class`

**若您正在引用的类未被检测到**(如引用自一些第三方库或其他构建模块，且通过非构造函数实例化)，您可以通过`registerKPojo`或设置`kCreatorCustom`来自定义实例化逻辑。

```kotlin
registerKPojo(User::class, Permissions::class, ...)
//或
kCreatorCustom = { kClass ->
    when (kClass) {
        User::class -> User()
        Permissions::class -> Permissions()
        else -> null
    }
}

// 若大量类未被检测到，可考虑使用反射实例化：
// import kotlin.reflect.full.createInstance
// kCreatorCustom = { kClass -> kClass.createInstance() }
```