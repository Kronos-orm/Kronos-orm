{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 使用Kronos提供的内置函数

### 调用内置函数

Kronos提供了一些内置函数，可以在查询`select`和条件筛选`where`时使用，如下：

```kotlin
KPojo.select { f.count(1) }.where { it.username like f.concat("%", it.username, "%") }.queryList()
```

### 内置函数列表

Kronos提供了包括部分数学计算函数`MathFunction`、部分聚合函数`PolymerizationFunction`和部分字符串函数`StringFunction`作为支持的默认内置函数

#### 数学计算函数
- `abs(x: Number?): Number?`： 返回x的绝对值
- `bin(x: Number?): Number?`：返回x的二进制
- `ceiling(x: Number?): Number?`：返回大于x的最小整数值
- `exp(x: Number?): Number?`：返回值e（自然对数的底）的x次方
- `floor(x: Number?): Number?`：返回小于x的最大整数值
- `greatest(vararg x: Number?): Number?`：返回集合中最大的值
- `least(vararg x: Number?): Number?`：返回集合中最小的值
- `ln(x: Number?): Number?`：返回x的自然对数
- `log(x: Number?, y: Number?): Number?`：返回x的以y为底的对数
- `mod(x: Number?, y: Number?): Number?`：返回x/y的模（余数）
- `pi(): Number?`：返回pi的值（圆周率）
- `rand(): Number?`：返回０到１内的随机值
- `round(x: Number?, y: Number?): Number?`：返回参数x的四舍五入的有y位小数的值
- `sign(x: Number?): Number?`：返回代表数字x的符号的值
- `sqrt(x: Number?): Number?`：返回一个数的平方根
- `truncate(x: Number?, y: Number?): Number?`：返回数字x截短为y位小数的结果
- `add(vararg x: Number?): Number?`：返回x+y的和
- `sub(vararg x: Number?): Number?`：返回x-y的差
- `mul(vararg x: Number?): Number?`：返回x*y的积
- `div(vararg x: Number?): Number?`：返回x/y的商

#### 聚合函数
- `count(x: Any?): Number?`：返回指定列中非NULL值的个数
- `sum(x: Any?): Number?`：返回指定列的所有值之和
- `avg(x: Any?): Number?`：返回指定列的平均值
- `max(x: Any?): Number?`：返回指定列的最大值
- `min(x: Any?): Number?`：返回指定列的最小值
- `groupConcat(x: Any?): Any?`：返回由属于一组的列值连接组合而成的结果

#### 字符串函数
- `length(x: String?): Any?`：返回字符串x中的字符数
- `upper(x: String?): Any?`：返回将字符串x中所有字符转变为大写后的结果
- `lower(x: String?): Any?`：返回将字符串x中所有字符转变为小写后的结果
- `substr(x: String?, y: Int?, z: Int?): Any?`：返回字符串x从y位置开始的z个字符
- `replace(x: String?, y: String?, z: String?): Any?`：返回字符串x中所有y替换为z后的结果
- `left(x: String?, y: Int?): Any?`：返回字符串x中最左边的y个字符
- `right(x: String?, y: Int?): Any?`：返回字符串x中最右边的y个字符
- `repeat(x: String?, y: Int?): Any?`：返回字符串x重复y次的结果
- `reverse(x: String?): Any?`：返回颠倒字符串x的结果
- `trim(x: String?): Any?`：去除字符串首部和尾部的所有空格
- `ltrim(x: String?): Any?`：去除字符串首部的所有空格
- `rtrim(x: String?): Any?`：去除字符串尾部的所有空格
- `concat(vararg x: String?): String?`：将x1,x2...,xn连接成字符串
- `join(x: String?, vararg y: String?): String?`：将y1,y2...,yn用分隔符x连接成字符串

## 自定义内置函数
Kronos支持自定义内置函数，您可以参考下面的示例步骤，实现自定义的内置函数功能：

### 步骤一：声明自定义内置函数
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

### 步骤三：注册自定义内置函数
将定义的内置函数Builder注册到`FunctionManager`中，以便在使用时能够调用
```kotlin
FunctionManager.registerFunctionBuilder(CustomerFunctionBuilder())
```

### 步骤四：使用自定义内置函数
在查询`select`和条件筛选`where`时，通过句柄`f`调用自定义内置函数
```kotlin
KPojo.select { f.showName("user") }.where { f.curDate == "2024-01-01" }.queryList()
```

