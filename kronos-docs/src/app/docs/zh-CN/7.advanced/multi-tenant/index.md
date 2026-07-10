{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 按租户选择数据源

Kronos 会从 `Kronos.dataSource` 读取默认数据源。该属性是一个返回 `KronosDataSourceWrapper` 的函数。多租户应用可以在启动阶段为每个租户注册一个 wrapper，并在请求进入时解析当前租户对应的 wrapper。

```kotlin group="Tenant source" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import javax.sql.DataSource

val dataSourceMap = mutableMapOf<String, KronosDataSourceWrapper>()

fun registerTenantDataSources(tenant1: DataSource, tenant2: DataSource) {
    dataSourceMap["tenant1"] = KronosJdbcWrapper(tenant1)
    dataSourceMap["tenant2"] = KronosJdbcWrapper(tenant2)
}

fun tenantWrapper(tenantId: String): KronosDataSourceWrapper =
    dataSourceMap[tenantId]
        ?: throw IllegalArgumentException("No data source found for tenant: $tenantId")
```

`KronosJdbcWrapper` 接收 JDBC `DataSource`，并从数据库元数据推断数据库类型。如果项目已经有自己的 `KronosDataSourceWrapper` 实现，也可以直接放入同一个注册表。

## Spring 请求路由

在 servlet 请求中，可以让 `Kronos.dataSource` 从当前请求上下文读取租户 ID。没有显式传入 wrapper 的 Kronos 调用随后会使用默认 wrapper。

```kotlin group="Spring tenant" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

fun configureKronosTenantSource() {
    Kronos.dataSource = {
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
        val tenantId = request.getHeader("tenantId") ?: error("Missing tenantId header")
        tenantWrapper(tenantId)
    }
}

@RestController
class UserController {
    @GetMapping("/users")
    fun getUsers(pi: Int, ps: Int): Pair<Int, List<User>> {
        return User().select().page(pi, ps).withTotal().toList()
    }
}
```

> **Note**
> 使用默认 wrapper 时，`Kronos.dataSource` 会在操作构建或执行时求值。任务离开请求线程后，应先解析租户 wrapper，再显式传给 Kronos 操作。

## 在异步任务中显式传入 wrapper

查询和写入执行 API 可以接收可选的 `KronosDataSourceWrapper`。当租户数据源已经确定，或请求上下文不可用时，直接使用该参数。

```kotlin group="Explicit wrapper" name="kotlin" icon="kotlin"
fun loadTenantUsers(tenantId: String, pi: Int, ps: Int): Pair<Int, List<User>> {
    val wrapper = tenantWrapper(tenantId)

    return User()
        .select()
        .page(pi, ps)
        .withTotal()
        .toList(wrapper)
}

fun updateTenantUser(tenantId: String, id: Int, name: String) {
    val wrapper = tenantWrapper(tenantId)

    User(id = id)
        .update()
        .set { it.name = name }
        .by { it.id }
        .execute(wrapper)
}
```

## Solon 请求路由

Solon 应用可以使用同一模式：从 `Context` 读取租户 ID，解析 wrapper，并在 handler 中传给操作。

```kotlin group="Solon tenant" name="kotlin" icon="kotlin"
import org.noear.solon.Solon

fun main(args: Array<String>) {
    Solon.start(DemoApp::class.java, args) { app ->
        app.get("/users") { ctx ->
            val tenantId = ctx.header("tenantId") ?: error("Missing tenantId header")
            val wrapper = tenantWrapper(tenantId)
            val pi = ctx.param("pi").toInt()
            val ps = ctx.param("ps").toInt()

            User()
                .select()
                .page(pi, ps)
                .withTotal()
                .toList(wrapper)
        }
    }
}
```

其他框架也遵循同一规则：应用启动时创建或注册 wrapper，从请求信息解析租户 wrapper，然后从 `Kronos.dataSource` 返回它，或显式传给本次操作。
