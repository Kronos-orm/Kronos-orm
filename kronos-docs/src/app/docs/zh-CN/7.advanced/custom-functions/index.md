{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 自定义函数

Kronos 的函数 DSL 通过 `FunctionHandler` 扩展声明。声明后的函数可以在 `select`、`where`、`orderBy` 等 DSL 中通过句柄 `f` 调用，并生成 SQL 函数表达。

### 步骤一：声明自定义函数

扩展函数只需要描述函数名、参数类型和返回值类型。函数体不会在运行时执行，通常返回 `null`。

```kotlin
import com.kotlinorm.annotations.KronosFunction
import com.kotlinorm.functions.FunctionHandler

@Suppress("unused")
object CustomerFunctions {
    @KronosFunction("json_extract")
    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.jsonExtract(payload: String?, path: String?): String? = null

    @KronosFunction("regexp_replace")
    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.regexpReplace(value: String?, pattern: String?, replacement: String?): String? = null
}
```

### 步骤二：使用自定义函数

```kotlin
User().select {
    [
        it.id,
        f.jsonExtract(it.payload, "$.name").alias("nameValue"),
        f.regexpReplace(it.name, "\\s+", "_").alias("normalizedName")
    ]
}.where {
    f.jsonExtract(it.payload, "$.status") == "enabled"
}.toList()
```

```sql group="Custom function" name="mysql" icon="mysql"
SELECT `id`, json_extract(`payload`, '$.name') AS `nameValue`, regexp_replace(`name`, '\\s+', '_') AS `normalizedName`
FROM `user`
WHERE json_extract(`payload`, '$.status') = :param1
```

`@KronosFunction` 可以指定 SQL 函数标识，未标注时使用 Kotlin 扩展函数名。内置函数包含常用聚合、字符串、数学、窗口函数和 PostgreSQL 数组函数。

自定义函数适合普通 `name(arg1, arg2, ...)` 形式。需要特殊 SQL 片段、关键字函数或数据库专属语法时，可以在条件中使用 `asSql()` 和 `patch(...)`，或向 Kronos 提交内置方言支持。
