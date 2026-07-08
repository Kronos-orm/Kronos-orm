{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Custom Functions

Kronos function DSL is declared with `FunctionHandler` extensions. Once declared, the function can be called through `f` in `select`, `where`, `orderBy`, and related DSL blocks. Kronos generates a SQL function expression for the call.

### Step 1: Declare a Custom Function

The extension function only describes the function name, parameter types, and return type. The body is not executed at runtime, so it normally returns `null`.

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

### Step 2: Use the Custom Function

```kotlin
User().select {
    [
        it.id,
        f.jsonExtract(it.payload, "$.name").alias("nameValue"),
        f.regexpReplace(it.name, "\\s+", "_").alias("normalizedName")
    ]
}.where {
    f.jsonExtract(it.payload, "$.status") == "enabled"
}.queryList()
```

```sql group="Custom function" name="mysql" icon="mysql"
SELECT `id`, json_extract(`payload`, '$.name') AS `nameValue`, regexp_replace(`name`, '\\s+', '_') AS `normalizedName`
FROM `user`
WHERE json_extract(`payload`, '$.status') = :param1
```

`@KronosFunction` sets the SQL function identifier. Unannotated extensions use the Kotlin function name. Built-in functions cover common aggregate, string, math, window, and PostgreSQL array functions.

Custom functions fit ordinary `name(arg1, arg2, ...)` SQL. For special SQL fragments, keyword functions, or database-specific syntax, use `asSql()` with `patch(...)` in conditions, or contribute built-in dialect support to Kronos.
