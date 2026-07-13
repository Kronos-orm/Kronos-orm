{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Choose a data source per tenant

Kronos reads the default data source from `Kronos.dataSource`, a function that returns a `KronosDataSourceWrapper`. In a multi-tenant application, register one wrapper for each tenant during application startup and resolve the wrapper that matches the current request.

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

`KronosJdbcWrapper` takes a JDBC `DataSource` and infers the database type from metadata. If your project already implements `KronosDataSourceWrapper`, store that implementation in the same registry.

## Spring request routing

In a servlet request, set `Kronos.dataSource` to read the tenant ID from the current request context. Kronos calls without an explicit wrapper can then use the default wrapper.

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
    fun getUsers(pi: Int, ps: Int): Triple<Int, List<User>, Int> {
        return User().select().withTotal().page(pi, ps).toList()
    }
}
```

> **Note**
> `Kronos.dataSource` is evaluated when an operation builds or executes with the default wrapper. When work leaves the request thread, resolve the tenant wrapper before scheduling the work and pass it to the Kronos operation explicitly.

## Pass the wrapper explicitly in asynchronous work

Query and mutation execution APIs accept an optional `KronosDataSourceWrapper`. Use that parameter when the tenant source is already known or when request-local storage is not available.

```kotlin group="Explicit wrapper" name="kotlin" icon="kotlin"
fun loadTenantUsers(tenantId: String, pi: Int, ps: Int): Triple<Int, List<User>, Int> {
    val wrapper = tenantWrapper(tenantId)

    return User()
        .select()
        .withTotal()
        .page(pi, ps)
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

## Solon request routing

Solon applications can use the same pattern. Read the tenant ID from `Context`, resolve the wrapper, and pass it to the operation inside the handler.

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
                .withTotal()
                .page(pi, ps)
                .toList(wrapper)
        }
    }
}
```

The same rule applies in other frameworks: create or register wrappers during application startup, resolve the tenant wrapper from request data, and either return it from `Kronos.dataSource` or pass it to the operation.
