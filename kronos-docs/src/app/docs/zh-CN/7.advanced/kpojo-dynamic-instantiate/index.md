{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 通过 KType 创建 KPojo

泛型代码拿到完整 `KType` 并需要创建 KPojo 实例时，使用 `Kronos.createKPojo(type)`。运行时通过编译器生成或显式注册的非反射 factory 创建实例。

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

泛型辅助函数也可以使用同一个 API。

```kotlin group="Create 2" name="generic helper" icon="kotlin"
inline fun <reified T : KPojo> newKPojo(): T {
    return Kronos.createKPojo(typeOf<T>()) as T
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

模型类型来自其他模块、第三方包或工厂边界时，使用 `Kronos.registerKPojoFactory` 指定构造函数。注册键是完整且精确的 `KType`；当前版本不支持泛型 KPojo 类型。

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

每个注册只覆盖一个精确类型。同一类型后注册的 factory 优先；调用 `close()` 只移除本次覆盖，并恢复之前的用户或生成 factory。

```kotlin group="Register 2" name="lifecycle" icon="kotlin"
val user = Kronos.createKPojo(typeOf<User>())
userRegistration.close()
permissionsRegistration.close()
```

## 处理未注册类型

没有生成或注册的 factory 能处理该 `KType` 时，`createKPojo` 会抛出包含修复建议的错误。为声明该 KPojo 的模块启用 Kronos compiler plugin，或手动注册构造函数。

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

无法使用生成 factory 的类型，可以显式注册构造函数。

```kotlin group="Missing factory 2" name="registration" icon="kotlin"
val registration = Kronos.registerKPojoFactory(
    typeOf<ExternalUser>(),
    KPojoFactory { _ -> ExternalUser(id = 0) }
)

val external = Kronos.createKPojo(typeOf<ExternalUser>())
// ExternalUser(id = 0)
```

使用同一实例化路径的 Map 转换 API 见 {{ $.keyword("advanced/mapper-to", ["Map/KPojo 类型转换"]) }}。
