{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 通过 KClass 创建 KPojo

泛型代码拿到 `KClass` 并需要创建 KPojo 实例时，使用 `createInstance()`。Kronos 会通过已生成或已注册的 KPojo factory 创建实例。

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

泛型辅助函数也可以使用同一个 API。

```kotlin group="Create 2" name="generic helper" icon="kotlin"
inline fun <reified T : KPojo> newKPojo(): T {
    return T::class.createInstance()
}

val user = newKPojo<User>()
// User(id = null, name = "")
```

结果形态：

```text group="Create 3" name="result"
返回值是请求的 KPojo 类型。
构造参数使用与普通 User() 调用相同的默认值。
```

## 注册显式构造函数

模型类型来自其他模块、第三方包或工厂边界时，使用 `registerKPojo` 指定构造函数。

```kotlin group="Register 1" name="single type" icon="kotlin"
import com.kotlinorm.utils.registerKPojo

registerKPojo(User::class) { User() }
registerKPojo(Permissions::class) { Permissions() }
```

调用点已知目标类型时，可以使用 reified 重载把同一注册写在一处。

```kotlin group="Register 2" name="reified" icon="kotlin"
import com.kotlinorm.utils.registerKPojo

registerKPojo<User> { User() }
```

一个注册需要覆盖多个模型类型时，使用 `registerKPojoFactory`。

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

factory 对不负责的类型返回 `null`，后续 factory 可以继续处理同一次请求。

```text group="Register 4" name="result"
User::class.createInstance() 返回 User()。
Permissions::class.createInstance() 返回 Permissions()。
未匹配的 KPojo 类型会继续交给后续注册的 factory。
```

## 处理未注册类型

没有生成或注册的 factory 能处理该类型时，`createInstance()` 会抛出包含修复建议的错误。为声明该 KPojo 的模块启用 Kronos compiler plugin，或手动注册构造函数。

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

无法使用生成 factory 的类型，可以显式注册构造函数。

```kotlin group="Missing factory 2" name="registration" icon="kotlin"
registerKPojo(ExternalUser::class) { ExternalUser(id = 0) }

val external = ExternalUser::class.createInstance()
// ExternalUser(id = 0)
```

使用同一实例化路径的 Map 转换 API 见 {{ $.keyword("advanced/mapper-to", ["Map/KPojo 类型转换"]) }}。
