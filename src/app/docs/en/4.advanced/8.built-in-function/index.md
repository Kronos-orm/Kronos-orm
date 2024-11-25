{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 使用Kronos提供的内置函数

Kronos提供了一些内置函数，可以在查询`select`和条件筛选`where`时使用，如下：

```kotlin
KPojo.select { f.count(1) }.where { it.username like f.concat("%", it.username, "%") }.queryList()
```

## 内置函数列表

Kronos提供了包括部分数学计算函数`MathFunction`、部分聚合函数`PolymerizationFunction`和部分字符串函数`StringFunction`作为支持的默认内置函数

### 聚合函数

#### 1. {{ $.title("count") }} 总数

返回指定列中非NULL值的个数

- **函数声明**

```kotlin
fun FunctionHandler.count(x: Any?): Number?
```

- **使用示例**

```kotlin group="Case 1" name="kotlin" icon="kotlin"
user.select { f.count(1) }.queryList()
```

```sql group="Case 1" name="mysql" icon="mysql"
SELECT COUNT(1) FROM `user`
```

```sql group="Case 1" name="postgresql" icon="postgres"
SELECT COUNT(1) FROM "user"
```

```sql group="Case 1" name="sqlite" icon="sqlite"
SELECT COUNT(1) FROM "user"
```

```sql group="Case 1" name="oracle" icon="oracle"
SELECT COUNT(1) FROM "user"
```

```sql group="Case 1" name="mssql" icon="sqlserver"
SELECT COUNT(1) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列', 'Any?']]) }}

#### 2. {{ $.title("sum") }} 求和

返回指定列的所有值之和

- **函数声明**

```kotlin
fun FunctionHandler.sum(x: Any?): Number?
```

- **使用示例**

```kotlin group="Case 2" name="kotlin" icon="kotlin"
user.select { f.sum(it.age) }.queryList()
```

```sql group="Case 2" name="mysql" icon="mysql"
SELECT SUM(`age`) FROM `user`
```

```sql group="Case 2" name="postgresql" icon="postgres"
SELECT SUM("age") FROM "user"
```

```sql group="Case 2" name="sqlite" icon="sqlite"
SELECT SUM(`age`) FROM "user"
```

```sql group="Case 2" name="oracle" icon="oracle"
SELECT SUM("age") FROM "user"
```

```sql group="Case 2" name="mssql" icon="sqlserver"
SELECT SUM([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列', 'Any?']]) }}

#### 3. {{ $.title("avg") }} 平均值

返回指定列的平均值

- **函数声明**

```kotlin
fun FunctionHandler.avg(x: Any?): Number?
```

- **使用示例**

```kotlin group="Case 3" name="kotlin" icon="kotlin"
user.select { f.avg(it.age) }.queryList()
```

```sql group="Case 3" name="mysql" icon="mysql"
SELECT AVG(`age`) FROM "user"
```

```sql group="Case 3" name="postgresql" icon="postgres"
SELECT AVG("age") FROM "user"
```

```sql group="Case 3" name="sqlite" icon="sqlite"
SELECT AVG(`age`) FROM `user`
```

```sql group="Case 3" name="oracle" icon="oracle"
SELECT AVG("age") FROM "user"
```

```sql group="Case 3" name="mssql" icon="sqlserver"
SELECT AVG([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列', 'Any?']]) }}

#### 4. {{ $.title("max") }} 最大值

返回指定列的最大值

- **函数声明**

```kotlin
fun FunctionHandler.max(x: Any?): Number?
```

- **使用示例**

```kotlin group="Case 4" name="kotlin" icon="kotlin"
user.select { f.max(it.age) }.queryList()
```

```sql group="Case 4" name="mysql" icon="mysql"
SELECT MAX(`age`) FROM `user`
```

```sql group="Case 4" name="postgresql" icon="postgres"
SELECT MAX("age") FROM "user"
```

```sql group="Case 4" name="sqlite" icon="sqlite"
SELECT MAX(`age`) FROM "user"
```

```sql group="Case 4" name="oracle" icon="oracle"
SELECT MAX("age") FROM "user"
```

```sql group="Case 4" name="mssql" icon="sqlserver"
SELECT MAX([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列', 'Any?']]) }}

#### 5. {{ $.title("min") }} 最小值

返回指定列的最小值

- **函数声明**

```kotlin
fun FunctionHandler.min(x: Any?): Number?
```

- **使用示例**

```kotlin group="Case 5" name="kotlin" icon="kotlin"
user.select { f.min(it.age) }.queryList()
```

```sql group="Case 5" name="mysql" icon="mysql"
SELECT MIN(`age`) FROM `user`
```

```sql group="Case 5" name="postgresql" icon="postgres"
SELECT MIN("age") FROM "user"
```

```sql group="Case 5" name="sqlite" icon="sqlite"
SELECT MIN(`age`) FROM "user"
```

```sql group="Case 5" name="oracle" icon="oracle"
SELECT MIN("age") FROM "user"
```

```sql group="Case 5" name="mssql" icon="sqlserver"
SELECT MIN([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列', 'Any?']]) }}

#### 6. {{ $.title("groupConcat") }} 字符串连接

返回由属于一组的列值连接组合而成的结果

- **函数声明**

```kotlin
fun FunctionHandler.groupConcat(x: Any?): Any?
```

- **使用示例**

```kotlin group="Case 6" name="kotlin" icon="kotlin"
user.select { f.groupConcat(it.username) }.queryList()
```

```sql group="Case 6" name="mysql" icon="mysql"
SELECT GROUP_CONCAT(`username`) FROM `user`
```

```sql group="Case 6" name="postgresql" icon="postgres"
SELECT STRING_AGG("username", ',') FROM "user"
```

```sql group="Case 6" name="sqlite" icon="sqlite"
SELECT GROUP_CONCAT(`username`) FROM "user"
```

```sql group="Case 6" name="oracle" icon="oracle"
SELECT LISTAGG("username", ',') WITHIN GROUP (ORDER BY "username") FROM "user"
```

```sql group="Case 6" name="mssql" icon="sqlserver"
SELECT STRING_AGG([username], ',') FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列', 'Any?']]) }}

### 数学计算函数

#### 1. {{ $.title("add") }} 加法

返回x+y+...的和

- **函数声明**

```kotlin
fun FunctionHandler.add(vararg x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 7" name="kotlin" icon="kotlin"
user.select { f.add(it.age, 1) }.queryList()
```

```sql group="Case 7" name="mysql" icon="mysql"
SELECT `age` + 1 FROM `user`
```

```sql group="Case 7" name="postgresql" icon="postgres"
SELECT "age" + 1 FROM "user"
```

```sql group="Case 7" name="sqlite" icon="sqlite"
SELECT `age` + 1 FROM "user"
```

```sql group="Case 7" name="oracle" icon="oracle"
SELECT "age" + 1 FROM "user"
```

```sql group="Case 7" name="mssql" icon="sqlserver"
SELECT [age] + 1 FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 2. {{ $.title("sub") }} 减法

返回x-y-...的差

- **函数声明**

```kotlin
fun FunctionHandler.sub(vararg x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 8" name="kotlin" icon="kotlin"
user.select { f.sub(it.age, 1) }.queryList()
```

```sql group="Case 8" name="mysql" icon="mysql"
SELECT `age` - 1 FROM `user`
```

```sql group="Case 8" name="postgresql" icon="postgres"
SELECT "age" - 1 FROM "user"
```

```sql group="Case 8" name="sqlite" icon="sqlite"
SELECT `age` - 1 FROM "user"
```

```sql group="Case 8" name="oracle" icon="oracle"
SELECT "age" - 1 FROM "user"
```

```sql group="Case 8" name="mssql" icon="sqlserver"
SELECT [age] - 1 FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 3. {{ $.title("mul") }} 乘法

返回x*y*...的积

- **函数声明**

```kotlin
fun FunctionHandler.mul(vararg x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 9" name="kotlin" icon="kotlin"
user.select { f.mul(it.age, 2) }.queryList()
```

```sql group="Case 9" name="mysql" icon="mysql"
SELECT `age` * 2 FROM `user`
```

```sql group="Case 9" name="postgresql" icon="postgres"
SELECT "age" * 2 FROM "user"
```

```sql group="Case 9" name="sqlite" icon="sqlite"
SELECT `age` * 2 FROM "user"
```

```sql group="Case 9" name="oracle" icon="oracle"
SELECT "age" * 2 FROM "user"
```

```sql group="Case 9" name="mssql" icon="sqlserver"
SELECT [age] * 2 FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 4. {{ $.title("div") }} 除法

返回x/y/...的商

- **函数声明**

```kotlin
fun FunctionHandler.div(vararg x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 10" name="kotlin" icon="kotlin"
user.select { f.div(it.age, 2) }.queryList()
```

```sql group="Case 10" name="mysql" icon="mysql"
SELECT `age` / 2 FROM `user`
```

```sql group="Case 10" name="postgresql" icon="postgres"
SELECT "age" / 2 FROM "user"
```

```sql group="Case 10" name="sqlite" icon="sqlite"
SELECT `age` / 2 FROM "user"
```

```sql group="Case 10" name="oracle" icon="oracle"
SELECT "age" / 2 FROM "user"
```

```sql group="Case 10" name="mssql" icon="sqlserver"
SELECT [age] / 2 FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 5. {{ $.title("abs") }} 绝对值

返回x的绝对值

- **函数声明**

```kotlin
fun FunctionHandler.abs(x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 11" name="kotlin" icon="kotlin"
user.select { f.abs(it.age) }.queryList()
```

```sql group="Case 11" name="mysql" icon="mysql"
SELECT ABS(`age`) FROM `user`
```

```sql group="Case 11" name="postgresql" icon="postgres"
SELECT ABS("age") FROM "user"
```

```sql group="Case 11" name="sqlite" icon="sqlite"
SELECT ABS(`age`) FROM "user"
```

```sql group="Case 11" name="oracle" icon="oracle"
SELECT ABS("age") FROM "user"
```

```sql group="Case 11" name="mssql" icon="sqlserver"
SELECT ABS([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 6. {{ $.title("bin") }} 二进制

返回x的二进制

- **函数声明**

```kotlin
fun FunctionHandler.bin(x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 12" name="kotlin" icon="kotlin"
user.select { f.bin(it.age) }.queryList()
```

```sql group="Case 12" name="mysql" icon="mysql"
SELECT BIN(`age`) FROM `user`
```

```sql group="Case 12" name="postgresql" icon="postgres"
SELECT BIN("age") FROM "user"
```

```sql group="Case 12" name="sqlite" icon="sqlite"
SELECT BIN(`age`) FROM "user"
```

```sql group="Case 12" name="oracle" icon="oracle"
SELECT BIN("age") FROM "user"
```

```sql group="Case 12" name="mssql" icon="sqlserver"
SELECT BIN([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 7. {{ $.title("ceiling") }} 向上取整

返回大于x的最小整数值

- **函数声明**

```kotlin
fun FunctionHandler.ceiling(x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 13" name="kotlin" icon="kotlin"
user.select { f.ceiling(it.age) }.queryList()
```

```sql group="Case 13" name="mysql" icon="mysql"
SELECT CEIL(`age`) FROM `user`
```

```sql group="Case 13" name="postgresql" icon="postgres"
SELECT CEIL("age") FROM "user"
```

```sql group="Case 13" name="sqlite" icon="sqlite"
SELECT CEIL(`age`) FROM "user"
```

```sql group="Case 13" name="oracle" icon="oracle"
SELECT CEIL("age") FROM "user"
```

```sql group="Case 13" name="mssql" icon="sqlserver"
SELECT CEILING([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 8. {{ $.title("exp") }} 指数

返回值e（自然对数的底）的x次方

- **函数声明**

```kotlin
fun FunctionHandler.exp(x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 14" name="kotlin" icon="kotlin"
user.select { f.exp(it.age) }.queryList()
```

```sql group="Case 14" name="mysql" icon="mysql"
SELECT EXP(`age`) FROM `user`
```

```sql group="Case 14" name="postgresql" icon="postgres"
SELECT EXP("age") FROM "user"
```

```sql group="Case 14" name="sqlite" icon="sqlite"
SELECT EXP(`age`) FROM "user"
```

```sql group="Case 14" name="oracle" icon="oracle"
SELECT EXP("age") FROM "user"
```

```sql group="Case 14" name="mssql" icon="sqlserver"
SELECT EXP([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 9. {{ $.title("floor") }} 向下取整

返回小于x的最大整数值

- **函数声明**

```kotlin
fun FunctionHandler.floor(x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 15" name="kotlin" icon="kotlin"
user.select { f.floor(it.age) }.queryList()
```

```sql group="Case 15" name="mysql" icon="mysql"
SELECT FLOOR(`age`) FROM `user`
```

```sql group="Case 15" name="postgresql" icon="postgres"
SELECT FLOOR("age") FROM "user"
```

```sql group="Case 15" name="sqlite" icon="sqlite"
SELECT FLOOR(`age`) FROM "user"
```

```sql group="Case 15" name="oracle" icon="oracle"
SELECT FLOOR("age") FROM "user"
```

```sql group="Case 15" name="mssql" icon="sqlserver"
SELECT FLOOR([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 10. {{ $.title("greatest") }} 最大值

返回集合中最大的值

- **函数声明**

```kotlin
fun FunctionHandler.greatest(vararg x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 16" name="kotlin" icon="kotlin"
user.select { f.greatest(it.age, 1) }.queryList()
```

```sql group="Case 16" name="mysql" icon="mysql"
SELECT GREATEST(`age`, 1) FROM `user`
```

```sql group="Case 16" name="postgresql" icon="postgres"
SELECT GREATEST("age", 1) FROM "user"
```

```sql group="Case 16" name="sqlite" icon="sqlite"
SELECT GREATEST(`age`, 1) FROM "user"
```

```sql group="Case 16" name="oracle" icon="oracle"
SELECT GREATEST("age", 1) FROM "user"
```

```sql group="Case 16" name="mssql" icon="sqlserver"
SELECT GREATEST([age], 1) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 11. {{ $.title("least") }} 最小值

返回集合中最小的值

- **函数声明**

```kotlin
fun FunctionHandler.least(vararg x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 17" name="kotlin" icon="kotlin"
user.select { f.least(it.age, 1) }.queryList()
```

```sql group="Case 17" name="mysql" icon="mysql"
SELECT LEAST(`age`, 1) FROM `user`
```

```sql group="Case 17" name="postgresql" icon="postgres"
SELECT LEAST("age", 1) FROM "user"
```

```sql group="Case 17" name="sqlite" icon="sqlite"
SELECT LEAST(`age`, 1) FROM "user"
```

```sql group="Case 17" name="oracle" icon="oracle"
SELECT LEAST("age", 1) FROM "user"
```

```sql group="Case 17" name="mssql" icon="sqlserver"
SELECT LEAST([age], 1) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 12. {{ $.title("ln") }} 自然对数

返回x的自然对数

- **函数声明**

```kotlin
fun FunctionHandler.ln(x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 18" name="kotlin" icon="kotlin"
user.select { f.ln(it.age) }.queryList()
```

```sql group="Case 18" name="mysql" icon="mysql"
SELECT LN(`age`) FROM `user`
```

```sql group="Case 18" name="postgresql" icon="postgres"
SELECT LN("age") FROM "user"
```

```sql group="Case 18" name="sqlite" icon="sqlite"
SELECT LN(`age`) FROM "user"
```

```sql group="Case 18" name="oracle" icon="oracle"
SELECT LN("age") FROM "user"
```

```sql group="Case 18" name="mssql" icon="sqlserver"
SELECT LOG([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 13. {{ $.title("log") }} 对数

返回x的以y为底的对数

- **函数声明**

```kotlin
fun FunctionHandler.log(x: Number?, y: Number?): Number?
```

- **使用示例**

```kotlin group="Case 19" name="kotlin" icon="kotlin"
user.select { f.log(it.age, 2) }.queryList()
```

```sql group="Case 19" name="mysql" icon="mysql"
SELECT LOG(2, `age`) FROM `user`
```

```sql group="Case 19" name="postgresql" icon="postgres"
SELECT LOG(2, "age") FROM "user"
```

```sql group="Case 19" name="sqlite" icon="sqlite"
SELECT LOG(2, `age`) FROM "user"
```

```sql group="Case 19" name="oracle" icon="oracle"
SELECT LOG(2, "age") FROM "user"
```

```sql group="Case 19" name="mssql" icon="sqlserver"
SELECT LOG(2, [age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?'], ['y', '指定底数', 'Number?']]) }}

#### 14. {{ $.title("mod") }} 模

返回x/y的模（余数）

- **函数声明**

```kotlin
fun FunctionHandler.mod(x: Number?, y: Number?): Number?
```

- **使用示例**

```kotlin group="Case 20" name="kotlin" icon="kotlin"
user.select { f.mod(it.age, 2) }.queryList()
```

```sql group="Case 20" name="mysql" icon="mysql"
SELECT MOD(`age`, 2) FROM `user`
```

```sql group="Case 20" name="postgresql" icon="postgres"
SELECT MOD("age", 2) FROM "user"
```

```sql group="Case 20" name="sqlite" icon="sqlite"
SELECT MOD(`age`, 2) FROM "user"
```

```sql group="Case 20" name="oracle" icon="oracle"
SELECT MOD("age", 2) FROM "user"
```

```sql group="Case 20" name="mssql" icon="sqlserver"
SELECT [age] % 2 FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?'], ['y', '指定除数', 'Number?']]) }}

#### 15. {{ $.title("pi") }} 圆周率

返回pi的值（圆周率）

- **函数声明**

```kotlin
fun FunctionHandler.pi(): Number?
```

- **使用示例**

```kotlin group="Case 21" name="kotlin" icon="kotlin"
user.select { f.pi() }.queryList()
```

```sql group="Case 21" name="mysql" icon="mysql"
SELECT PI() FROM `user`
```

```sql group="Case 21" name="postgresql" icon="postgres"
SELECT PI() FROM "user"
```

```sql group="Case 21" name="sqlite" icon="sqlite"
SELECT PI() FROM "user"
```

```sql group="Case 21" name="oracle" icon="oracle"
SELECT PI() FROM "user"
```

```sql group="Case 21" name="mssql" icon="sqlserver"
SELECT PI() FROM [user]
```

#### 16. {{ $.title("rand") }} 随机值

返回０到１内的随机值

- **函数声明**

```kotlin
fun FunctionHandler.rand(): Number?
```

- **使用示例**

```kotlin group="Case 22" name="kotlin" icon="kotlin"
user.select { f.rand() }.queryList()
```

```sql group="Case 22" name="mysql" icon="mysql"
SELECT RAND() FROM `user`
```

```sql group="Case 22" name="postgresql" icon="postgres"
SELECT RANDOM() FROM "user"
```

```sql group="Case 22" name="sqlite" icon="sqlite"
SELECT RANDOM() FROM "user"
```

```sql group="Case 22" name="oracle" icon="oracle"
SELECT DBMS_RANDOM.VALUE FROM "user"
```

```sql group="Case 22" name="mssql" icon="sqlserver"
SELECT RAND() FROM [user]
```

#### 17. {{ $.title("round") }} 四舍五入

返回参数x的四舍五入的有y位小数的值

- **函数声明**

```kotlin
fun FunctionHandler.round(x: Number?, y: Number?): Number?
```

- **使用示例**

```kotlin group="Case 23" name="kotlin" icon="kotlin"
user.select { f.round(it.age, 2) }.queryList()
```

```sql group="Case 23" name="mysql" icon="mysql"
SELECT ROUND(`age`, 2) FROM `user`
```

```sql group="Case 23" name="postgresql" icon="postgres"
SELECT ROUND("age", 2) FROM "user"
```

```sql group="Case 23" name="sqlite" icon="sqlite"
SELECT ROUND(`age`, 2) FROM "user"
```

```sql group="Case 23" name="oracle" icon="oracle"
SELECT ROUND("age", 2) FROM "user"
```

```sql group="Case 23" name="mssql" icon="sqlserver"
SELECT ROUND([age], 2) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?'], ['y', '指定小数位数', 'Number?']]) }}

#### 18. {{ $.title("sign") }} 符号

返回代表数字x的符号的值

- **函数声明**

```kotlin
fun FunctionHandler.sign(x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 24" name="kotlin" icon="kotlin"
user.select { f.sign(it.age) }.queryList()
```

```sql group="Case 24" name="mysql" icon="mysql"
SELECT SIGN(`age`) FROM `user`
```

```sql group="Case 24" name="postgresql" icon="postgres"
SELECT SIGN("age") FROM "user"
```

```sql group="Case 24" name="sqlite" icon="sqlite"
SELECT SIGN(`age`) FROM "user"
```

```sql group="Case 24" name="oracle" icon="oracle"
SELECT SIGN("age") FROM "user"
```

```sql group="Case 24" name="mssql" icon="sqlserver"
SELECT SIGN([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 19. {{ $.title("sqrt") }} 平方根

返回一个数的平方根

- **函数声明**

```kotlin
fun FunctionHandler.sqrt(x: Number?): Number?
```

- **使用示例**

```kotlin group="Case 25" name="kotlin" icon="kotlin"
user.select { f.sqrt(it.age) }.queryList()
```

```sql group="Case 25" name="mysql" icon="mysql"
SELECT SQRT(`age`) FROM `user`
```

```sql group="Case 25" name="postgresql" icon="postgres"
SELECT SQRT("age") FROM "user"
```

```sql group="Case 25" name="sqlite" icon="sqlite"
SELECT SQRT(`age`) FROM "user"
```

```sql group="Case 25" name="oracle" icon="oracle"
SELECT SQRT("age") FROM "user"
```

```sql group="Case 25" name="mssql" icon="sqlserver"
SELECT SQRT([age]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?']]) }}

#### 20. {{ $.title("truncate") }} 截断

返回数字x截短为y位小数的结果

- **函数声明**

```kotlin
fun FunctionHandler.truncate(x: Number?, y: Number?): Number?
```

- **使用示例**

```kotlin group="Case 26" name="kotlin" icon="kotlin"
user.select { f.truncate(it.age, 2) }.queryList()
```

```sql group="Case 26" name="mysql" icon="mysql"
SELECT TRUNCATE(`age`, 2) FROM `user`
```

```sql group="Case 26" name="postgresql" icon="postgres"
SELECT TRUNC("age", 2) FROM "user"
```

```sql group="Case 26" name="sqlite" icon="sqlite"
SELECT TRUNC(`age`, 2) FROM "user"
```

```sql group="Case 26" name="oracle" icon="oracle"
SELECT TRUNC("age", 2) FROM "user"
```

```sql group="Case 26" name="mssql" icon="sqlserver"
SELECT ROUND([age], 2) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定列或数字', 'Number?'], ['y', '指定小数位数', 'Number?']]) }}

### 字符串函数

#### 1. {{ $.title("length") }} 字符数

返回字符串x中的字符数

- **函数声明**

```kotlin
fun FunctionHandler.length(x: String?): Any?
```

- **使用示例**

```kotlin group="Case 27" name="kotlin" icon="kotlin"
user.select { f.length(it.username) }.queryList()
```

```sql group="Case 27" name="mysql" icon="mysql"
SELECT LENGTH(`username`) FROM `user`
```

```sql group="Case 27" name="postgresql" icon="postgres"
SELECT LENGTH("username") FROM "user"
```

```sql group="Case 27" name="sqlite" icon="sqlite"
SELECT LENGTH(`username`) FROM "user"
```

```sql group="Case 27" name="oracle" icon="oracle"
SELECT LENGTH("username") FROM "user"
```

```sql group="Case 27" name="mssql" icon="sqlserver"
SELECT LEN([username]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?']]) }}

#### 2. {{ $.title("upper") }} 大写

返回将字符串x中所有字符转变为大写后的结果

- **函数声明**

```kotlin
fun FunctionHandler.upper(x: String?): Any?
```

- **使用示例**

```kotlin group="Case 28" name="kotlin" icon="kotlin"
user.select { f.upper(it.username) }.queryList()
```

```sql group="Case 28" name="mysql" icon="mysql"
SELECT UPPER(`username`) FROM `user`
```

```sql group="Case 28" name="postgresql" icon="postgres"
SELECT UPPER("username") FROM "user"
```

```sql group="Case 28" name="sqlite" icon="sqlite"
SELECT UPPER(`username`) FROM "user"
```

```sql group="Case 28" name="oracle" icon="oracle"
SELECT UPPER("username") FROM "user"
```

```sql group="Case 28" name="mssql" icon="sqlserver"
SELECT UPPER([username]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?']]) }}

#### 3. {{ $.title("lower") }} 小写

返回将字符串x中所有字符转变为小写后的结果

- **函数声明**

```kotlin
fun FunctionHandler.lower(x: String?): Any?
```

- **使用示例**

```kotlin group="Case 29" name="kotlin" icon="kotlin"
user.select { f.lower(it.username) }.queryList()
```

```sql group="Case 29" name="mysql" icon="mysql"
SELECT LOWER(`username`) FROM `user`
```

```sql group="Case 29" name="postgresql" icon="postgres"
SELECT LOWER("username") FROM "user"
```

```sql group="Case 29" name="sqlite" icon="sqlite"
SELECT LOWER(`username`) FROM "user"
```

```sql group="Case 29" name="oracle" icon="oracle"
SELECT LOWER("username") FROM "user"
```

```sql group="Case 29" name="mssql" icon="sqlserver"
SELECT LOWER([username]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?']]) }}

#### 4. {{ $.title("substr") }} 截取

返回字符串x从y位置开始的z个字符

- **函数声明**

```kotlin
fun FunctionHandler.substr(x: String?, y: Int?, z: Int?): Any?
```

- **使用示例**

```kotlin group="Case 30" name="kotlin" icon="kotlin"
user.select { f.substr(it.username, 1, 2) }.queryList()
```

```sql group="Case 30" name="mysql" icon="mysql"
SELECT SUBSTRING(`username`, 1, 2) FROM `user`
```

```sql group="Case 30" name="postgresql" icon="postgres"
SELECT SUBSTRING("username", 1, 2) FROM "user"
```

```sql group="Case 30" name="sqlite" icon="sqlite"
SELECT SUBSTR(`username`, 1, 2) FROM "user"
```

```sql group="Case 30" name="oracle" icon="oracle"
SELECT SUBSTR("username", 1, 2) FROM "user"
```

```sql group="Case 30" name="mssql" icon="sqlserver"
SELECT SUBSTRING([username], 1, 2) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?'], ['y', '开始位置', 'Int?'], ['z', '字符数', 'Int?']]) }}

#### 5. {{ $.title("replace") }} 替换

返回字符串x中所有y替换为z后的结果

- **函数声明**

```kotlin
fun FunctionHandler.replace(x: String?, y: String?, z: String?): Any?
```

- **使用示例**

```kotlin group="Case 31" name="kotlin" icon="kotlin"
user.select { f.replace(it.username, "a", "b") }.queryList()
```

```sql group="Case 31" name="mysql" icon="mysql"
SELECT REPLACE(`username`, 'a', 'b') FROM `user`
```

```sql group="Case 31" name="postgresql" icon="postgres"
SELECT REPLACE("username", 'a', 'b') FROM "user"
```

```sql group="Case 31" name="sqlite" icon="sqlite"
SELECT REPLACE(`username`, 'a', 'b') FROM "user"
```

```sql group="Case 31" name="oracle" icon="oracle"
SELECT REPLACE("username", 'a', 'b') FROM "user"
```

```sql group="Case 31" name="mssql" icon="sqlserver"
SELECT REPLACE([username], 'a', 'b') FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?'], ['y', '被替换字符', 'String?'], ['z', '替换字符', 'String?']]) }}

#### 6. {{ $.title("left") }} 左截取

返回字符串x中最左边的y个字符

- **函数声明**

```kotlin
fun FunctionHandler.left(x: String?, y: Int?): Any?
```

- **使用示例**

```kotlin group="Case 32" name="kotlin" icon="kotlin"
user.select { f.left(it.username, 2) }.queryList()
```

```sql group="Case 32" name="mysql" icon="mysql"
SELECT LEFT(`username`, 2) FROM `user`
```

```sql group="Case 32" name="postgresql" icon="postgres"
SELECT LEFT("username", 2) FROM "user"
```

```sql group="Case 32" name="sqlite" icon="sqlite"
SELECT SUBSTR(`username`, 1, 2) FROM "user"
```

```sql group="Case 32" name="oracle" icon="oracle"
SELECT SUBSTR("username", 1, 2) FROM "user"
```

```sql group="Case 32" name="mssql" icon="sqlserver"
SELECT LEFT([username], 2) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?'], ['y', '字符数', 'Int?']]) }}

#### 7. {{ $.title("right") }} 右截取

返回字符串x中最右边的y个字符

- **函数声明**

```kotlin

fun FunctionHandler.right(x: String?, y: Int?): Any?
```

- **使用示例**

```kotlin group="Case 33" name="kotlin" icon="kotlin"
user.select { f.right(it.username, 2) }.queryList()
```

```sql group="Case 33" name="mysql" icon="mysql"
SELECT RIGHT(`username`, 2) FROM `user`
```

```sql group="Case 33" name="postgresql" icon="postgres"
SELECT RIGHT("username", 2) FROM "user"
```

```sql group="Case 33" name="sqlite" icon="sqlite"
SELECT SUBSTR(`username`, -2) FROM "user"
```

```sql group="Case 33" name="oracle" icon="oracle"
SELECT SUBSTR("username", -2) FROM "user"
```

```sql group="Case 33" name="mssql" icon="sqlserver"
SELECT RIGHT([username], 2) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?'], ['y', '字符数', 'Int?']]) }}

#### 8. {{ $.title("repeat") }} 重复

返回字符串x重复y次的结果

- **函数声明**

```kotlin
fun FunctionHandler.repeat(x: String?, y: Int?): Any?
```

- **使用示例**

```kotlin group="Case 34" name="kotlin" icon="kotlin"

user.select { f.repeat(it.username, 2) }.queryList()
```

```sql group="Case 34" name="mysql" icon="mysql"
SELECT REPEAT(`username`, 2) FROM `user`
```

```sql group="Case 34" name="postgresql" icon="postgres"
SELECT REPEAT("username", 2) FROM "user"
```

```sql group="Case 34" name="sqlite" icon="sqlite"
SELECT REPLACE(HEX(RANDOM()), '0', '1') FROM "user"
```

```sql group="Case 34" name="oracle" icon="oracle"
SELECT RPAD('x', 2, 'x') FROM "user"
```

```sql group="Case 34" name="mssql" icon="sqlserver"
SELECT REPLICATE([username], 2) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?'], ['y', '重复次数', 'Int?']]) }}

#### 9. {{ $.title("reverse") }} 颠倒

返回颠倒字符串x的结果

- **函数声明**

```kotlin
fun FunctionHandler.reverse(x: String?): Any?
```

- **使用示例**

```kotlin group="Case 35" name="kotlin" icon="kotlin"
user.select { f.reverse(it.username) }.queryList()
```

```sql group="Case 35" name="mysql" icon="mysql"
SELECT REVERSE(`username`) FROM `user`
```

```sql group="Case 35" name="postgresql" icon="postgres"
SELECT REVERSE("username") FROM "user"
```

```sql group="Case 35" name="sqlite" icon="sqlite"
SELECT REVERSE(`username`) FROM "user"
```

```sql group="Case 35" name="oracle" icon="oracle"
SELECT REVERSE("username") FROM "user"
```

```sql group="Case 35" name="mssql" icon="sqlserver"
SELECT REVERSE([username]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?']]) }}

#### 10. {{ $.title("trim") }} 去除空格

去除字符串首部和尾部的所有空格

- **函数声明**

```kotlin
fun FunctionHandler.trim(x: String?): Any?
```

- **使用示例**

```kotlin group="Case 36" name="kotlin" icon="kotlin"
user.select { f.trim(it.username) }.queryList()
```

```sql group="Case 36" name="mysql" icon="mysql"
SELECT TRIM(`username`) FROM `user`
```

```sql group="Case 36" name="postgresql" icon="postgres"
SELECT TRIM("username") FROM "user"
```

```sql group="Case 36" name="sqlite" icon="sqlite"
SELECT TRIM(`username`) FROM "user"
```

```sql group="Case 36" name="oracle" icon="oracle"
SELECT TRIM("username") FROM "user"
```

```sql group="Case 36" name="mssql" icon="sqlserver"
SELECT LTRIM(RTRIM([username])) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?']]) }}

#### 11. {{ $.title("ltrim") }} 去除左空格

去除字符串首部的所有空格

- **函数声明**

```kotlin
fun FunctionHandler.ltrim(x: String?): Any?
```

- **使用示例**

```kotlin group="Case 37" name="kotlin" icon="kotlin"
user.select { f.ltrim(it.username) }.queryList()
```

```sql group="Case 37" name="mysql" icon="mysql"
SELECT LTRIM(`username`) FROM `user`
```

```sql group="Case 37" name="postgresql" icon="postgres"
SELECT LTRIM("username") FROM "user"
```

```sql group="Case 37" name="sqlite" icon="sqlite"
SELECT LTRIM(`username`) FROM "user"
```

```sql group="Case 37" name="oracle" icon="oracle"
SELECT LTRIM("username") FROM "user"
```

```sql group="Case 37" name="mssql" icon="sqlserver"
SELECT LTRIM([username]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?']]) }}

#### 12. {{ $.title("rtrim") }} 去除右空格

去除字符串尾部的所有空格

- **函数声明**

```kotlin
fun FunctionHandler.rtrim(x: String?): Any?
```

- **使用示例**

```kotlin group="Case 38" name="kotlin" icon="kotlin"
user.select { f.rtrim(it.username) }.queryList()
```

```sql group="Case 38" name="mysql" icon="mysql"
SELECT RTRIM(`username`) FROM `user`
```

```sql group="Case 38" name="postgresql" icon="postgres"
SELECT RTRIM("username") FROM "user"
```

```sql group="Case 38" name="sqlite" icon="sqlite"
SELECT RTRIM(`username`) FROM "user"
```

```sql group="Case 38" name="oracle" icon="oracle"
SELECT RTRIM("username") FROM "user"
```

```sql group="Case 38" name="mssql" icon="sqlserver"
SELECT RTRIM([username]) FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?']]) }}

#### 13. {{ $.title("concat") }} 连接

将x1,x2...,xn连接成字符串

- **函数声明**

```kotlin
fun FunctionHandler.concat(vararg x: String?): String?
```

- **使用示例**

```kotlin group="Case 39" name="kotlin" icon="kotlin"
user.select { f.concat(it.username, it.age) }.queryList()
```

```sql group="Case 39" name="mysql" icon="mysql"
SELECT CONCAT(`username`, `age`) FROM `user`
```

```sql group="Case 39" name="postgresql" icon="postgres"
SELECT "username" || "age" FROM "user"
```

```sql group="Case 39" name="sqlite" icon="sqlite"
SELECT `username` || `age` FROM "user"
```

```sql group="Case 39" name="oracle" icon="oracle"
SELECT "username" || "age" FROM "user"
```

```sql group="Case 39" name="mssql" icon="sqlserver"
SELECT [username] + [age] FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定字符串', 'String?']]) }}

#### 14. {{ $.title("join") }} 连接

将y1,y2...,yn用分隔符x连接成字符串

- **函数声明**

```kotlin
fun FunctionHandler.join(x: String?, vararg y: String?): String?
```

- **使用示例**

```kotlin group="Case 40" name="kotlin" icon="kotlin"
user.select { f.join(",", it.username, it.age) }.queryList()
```

```sql group="Case 40" name="mysql" icon="mysql"
SELECT CONCAT_WS(',', `username`, `age`) FROM `user`
```

```sql group="Case 40" name="postgresql" icon="postgres"
SELECT CONCAT_WS(',', "username", "age") FROM "user"
```

```sql group="Case 40" name="sqlite" icon="sqlite"
SELECT `username` || ',' || `age` FROM "user"
```

```sql group="Case 40" name="oracle" icon="oracle"
SELECT "username" || ',' || "age" FROM "user"
```

```sql group="Case 40" name="mssql" icon="sqlserver"
SELECT [username] + ',' + [age] FROM [user]
```

- **接收参数**

{{ $.params([['x', '指定分隔符', 'String?'], ['y', '指定字符串', 'String?']]) }}