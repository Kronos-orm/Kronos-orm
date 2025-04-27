{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 什么是多租户模式

多租户模式是指在同一套应用程序中，支持多个租户（客户）使用同一套代码和数据库的模式。每个租户的数据相互隔离，互不干扰。多租户模式可以有效地降低运维成本，提高资源利用率。

## Kronos的多数据源和动态数据源

Kronos最初设计就是用于`SaaS`（软件即服务）场景的，因此它支持多租户模式。Kronos通过`KronosDataSourceWrapper`接口实现了对多数据源和动态数据源的支持。

用户可以轻松地在应用程序中动态切换数据源，从而实现多租户模式。

## 示例：{{ $.title("Spring") }}中使用{{ $.title("Kronos") }}实现多租户模式

```kotlin
import com.kotlinorm.kronos.Kronos

val dataSourceMap = mutableMapOf<String, KronosDataSourceWrapper>()

fun main() {
    // 首先，在应用程序启动时初始化数据源，可以放在main函数中，也可以放于@PostConstruct注解的方法中
    // 示例：
    dataSourceMap["tenant1"] = dataSource1
    dataSourceMap["tenant2"] = dataSource2
    
    // 初始化Kronos
    Kronos.init {
        dataSource = { 
            // 可以在RequestContextHolder中获取当前请求的租户信息，如host、header等
            val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?)!!.request
            val tenantId = request.getHeader("tenantId") // 获取租户ID
            dataSourceMap[tenantId] ?: throw IllegalArgumentException("No data source found for tenant: $tenantId")
        }
    }
}
    
@RestController
class UserController {
    @GetMapping("/users")
    fun getUsers(pi: Int, ps: Int): Pair<Int, User> {
        // 在这里可以直接使用Kronos进行数据库操作，Kronos会自动切换数据源
        // 在异步函数中可能会无法获取到RequestContextHolder中的数据源，请手动将数据源传入到对应的请求上下文
        return User().select().page(pi, ps).withTotal().queryList()
    }
}
```

## 示例：{{ $.title("Solon") }}中使用{{ $.title("Kronos") }}实现多租户模式

```kotlin
import com.kotlinorm.kronos.Kronos

val dataSourceMap = mutableMapOf<String, KronosDataSourceWrapper>()

fun main() {
    // 首先，在应用程序启动时初始化数据源，可以放在main函数中，也可以放于@PostConstruct注解的方法中
    // 示例：
    dataSourceMap["tenant1"] = dataSource1
    dataSourceMap["tenant2"] = dataSource2
    
    // 初始化Kronos
    Kronos.init {
        dataSource = { 
            // 可以在Context中获取当前请求的租户信息，如host、header等
            val tenantId = Context.current().attr("tenantId") // 获取租户ID
            dataSourceMap[tenantId] ?: throw IllegalArgumentException("No data source found for tenant: $tenantId")
        }
    }
    Solon.start(DemoApp.class, args, app->{
       app.get("/users", { ctx ->
            // 在这里可以直接使用Kronos进行数据库操作，Kronos会自动切换数据源
            // 在异步函数中，请不要使用默认的ThreadLocal，请手动获取dataSourceMap[ctx.attr("tenantId")]进行数据库操作
            val pi = ctx.param("pi").toInt()
            val ps = ctx.param("ps").toInt()
            return@ctx User().select().page(pi, ps).withTotal().queryList()
        })
    });
}
```

--------

在其他框架中使用Kronos实现多租户模式的方式类似，只需要在应用程序启动时初始化数据源，并在请求处理时动态切换数据源即可。
