{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## What is multi-tenant mode

Multi-tenant mode refers to a mode in which multiple tenants (customers) can use the same application code and database in the same application. Each tenant's data is isolated from each other and does not interfere with each other. Multi-tenant mode can effectively reduce operation and maintenance costs and improve resource utilization.

## Kronos's multi-data source and dynamic data source

Kronos was originally designed for `SaaS` (Software as a Service) scenarios, so it supports a multi-tenant model.Kronos implements support for multiple data sources and dynamic data sources through the `KronosDataSourceWrapper` interface.

Users can easily switch data sources dynamically within an application to enable a multi-tenant model.

## Example: {{ $.title("Spring") }} using {{ $.title("Kronos") }} to implement multi-tenant mode

```kotlin
import com.kotlinorm.kronos.Kronos

val dataSourceMap = mutableMapOf<String, KronosDataSourceWrapper>()

fun main() {
    // First, initialize the data source at application startup, either in the main function or in a method annotated with @PostConstruct
    // Example:
    dataSourceMap["tenant1"] = dataSource1
    dataSourceMap["tenant2"] = dataSource2
    
    // Initialize Kronos：
    Kronos.init {
        dataSource = { 
            // You can get the tenant information of the current request in RequestContextHolder, such as host, header, etc.
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?)!!.request
            val tenantId = request.getHeader("tenantId") // Get Tenant ID
            dataSourceMap[tenantId] ?: throw IllegalArgumentException("No data source found for tenant: $tenantId")
        }
    }
}
    
@RestController
class UserController {
    @GetMapping("/users")
    fun getUsers(pi: Int, ps: Int): Pair<Int, User> {
        // Here you can directly use Kronos for database operations, Kronos will automatically switch the data source
        // You may not be able to get the data source in the RequestContextHolder in the asynchronous function, so please manually pass the data source into the corresponding request context.
        return User().select().page(pi, ps).withTotal().queryList()
    }
}
```

## Example: {{ $.title("Solon") }} in {{ $.title("Kronos") }} to implement a multi-tenant model

```kotlin
import com.kotlinorm.kronos.Kronos

val dataSourceMap = mutableMapOf<String, KronosDataSourceWrapper>()

fun main() {
    // First, initialize the data source at application startup, either in the main function or in a method annotated with @PostConstruct
    // Example:
    dataSourceMap["tenant1"] = dataSource1
    dataSourceMap["tenant2"] = dataSource2
    
    // Initializing Kronos
    Kronos.init {
        dataSource = { 
            // You can get the tenant information of the current request in the Context, such as host, header, etc.
            val tenantId = Context.current().attr("tenantId") // Get Tenant ID
            dataSourceMap[tenantId] ?: throw IllegalArgumentException("No data source found for tenant: $tenantId")
        }
    }
    Solon.start(DemoApp.class, args, app->{
       app.get("/users", { ctx ->
            // Here you can directly use Kronos for database operations, Kronos will automatically switch the data source
            // In the asynchronous function, please do not use the default ThreadLocal, please manually get dataSourceMap[ctx.attr("tenantId")] for database operation
            val pi = ctx.param("pi").toInt()
            val ps = ctx.param("ps").toInt()
            return@ctx User().select().page(pi, ps).withTotal().queryList()
        })
    });
}
```

--------

Using Kronos in other frameworks implements the multi-tenant model in a similar way, simply by initializing the data source at application startup and dynamically switching the data source during request processing.
