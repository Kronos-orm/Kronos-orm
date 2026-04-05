{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 自定义函数/方言

Kronos支持自定义内置函数/方言，您可以参考下面的示例步骤，实现自定义功能：

### 步骤一：创建自定义函数声明

通过对`FunctionHandler`添加扩展函数的形式，声明所要添加的内置函数，以便能在使用时通过句柄`f`快速调出

这里只需要定义函数名称、参数名称及类型、返回值类型即可，无需定义函数的具体逻辑

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

### 步骤二：实现自定义函数具体逻辑

```kotlin
import com.kotlinorm.functions.FunctionHandler

@Suppress("unused")
object CustomerFunctionBuilder : FunctionBuilder {
    private val all = arrayOf(
        DBType.Mysql, DBType.Postgres, DBType.SQLite, DBType.Oracle, DBType.Mssql
    )

    // 定义支持的函数名称及对应的数据库类型
    override val supportFunctionNames: (String) -> Array<DBType> = {
        when (it) {
            "curDate" -> all
            "dayTime" -> arrayOf(DBType.Mysql)
            else -> emptyArray()
        }
    }

    // 定义函数的具体逻辑，返回最终的生成在SQL语句部分
    override fun transform(field: FunctionField, dataSource: KronosDataSourceWrapper, showTable: Boolean, showAlias: Boolean): String {
        return when (field.functionName) {
            "curDate" -> {
                when (dataSource.dbType) {
                    DBType.Postgres, DBType.Mssql -> "CURRENT_DATE"
                    else -> "CURDATE"
                }
                
                //这个函数是Kronos用于生成带参函数sql语句的方法
                buildFields(field.functionName, field.name.takeIf { showAlias } ?: "", field.fields, dataSource, showTable)
            }
            "showName" -> "'$field.fields.first().value'"
            else -> throw IllegalArgumentException("Unsupported function: ${field.functionName}")
        }
    
    }
}
```

#### {{ $.title("FunctionField") }}

`FunctionField`是Kronos用于生成带参函数sql语句的数据结构，包含了函数名称、字段名称、字段值等信息

- **成员变量**

{{ $.params([['functionName', '函数名称', 'String'], ['fields', '字段名称及值', 'List<Pair<Field?, Any?>>', 'emptyList()']]) }}


### 步骤三：注册自定义内置函数

将定义的内置函数Builder注册到`FunctionManager`中，以便在使用时能够调用

```kotlin
FunctionManager.registerFunctionBuilder(CustomerFunctionBuilder())
```

### 步骤四：使用自定义内置函数

在查询`select`和条件筛选`where`时，通过句柄`f`调用自定义内置函数

```kotlin
KPojo.select { f.showName("user") }.where { f.curDate() == "2024-01-01" }.queryList()
```

