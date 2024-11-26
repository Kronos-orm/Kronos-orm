{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Custom functions/dialects

Kronos supports custom built-in functions/dialects. You can refer to the following example steps to implement custom functions:

### Step 1: Create a custom function declaration

By adding an extension function to `FunctionHandler`, declare the built-in function to be added so that it can be quickly called out through the handle `f` when used

Here you only need to define the function name, parameter name and type, and return value type. There is no need to define the specific logic of the function.

```kotlin
import com.kotlinorm.functions.FunctionHandler

@Suppress("unused")
object CustomerFunctions {
    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.curDate(): String? = null

    @Suppress("unused", "UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun FunctionHandler.showName(name: String?): String? = null
}
```

### Step 2: Implement the specific logic of the custom function

```kotlin
import com.kotlinorm.functions.FunctionHandler

@Suppress("unused")
object CustomerFunctionBuilder : FunctionBuilder {
    private val all = arrayOf(
        DBType.Mysql, DBType.Postgres, DBType.SQLite, DBType.Oracle, DBType.Mssql
    )

    // Define the supported function names and corresponding database types
    override val supportFunctionNames: (String) -> Array<DBType> = {
        when (it) {
            "curDate" -> all
            "dayTime" -> arrayOf(DBType.Mysql)
            else -> emptyArray()
        }
    }

    // Define the specific logic of the function and return the final generated SQL statement part
    override fun transform(field: FunctionField, dataSource: KronosDataSourceWrapper, showTable: Boolean, showAlias: Boolean): String {
        return when (field.functionName) {
            "curDate" -> {
                when (dataSource.dbType) {
                    DBType.Postgres, DBType.Mssql -> "CURRENT_DATE"
                    else -> "CURDATE"
                }
                
                // This function is the method used by Kronos to generate SQL statements with parameter functions.
                buildFields(field.functionName, field.name.takeIf { showAlias } ?: "", field.fields, dataSource, showTable)
            }
            "showName" -> "'$field.fields.first().value'"
            else -> throw IllegalArgumentException("Unsupported function: ${field.functionName}")
        }
    
    }
}
```

#### {{ $.title("FunctionField") }}

`FunctionField` is a data structure used by Kronos to generate SQL statements with parameters, which contains information such as function name, field name, field value, etc.

- **Member variables**

{{ $.params([['functionName', 'function name', 'String'], ['fields', 'field name and value', 'List<Pair<Field?, Any?>>', 'emptyList()']]) }}


### Step 3: Register a custom built-in function

Register the defined built-in function Builder to the `FunctionManager` so that it can be called when used

```kotlin
FunctionManager.registerFunctionBuilder(CustomerFunctionBuilder())
```

### Step 4: Use custom built-in functions

When querying `select` and conditional filtering `where`, call the custom built-in function through the handle `f`

```kotlin
KPojo.select { f.showName("user") }.where { f.curDate() == "2024-01-01" }.queryList()
```

