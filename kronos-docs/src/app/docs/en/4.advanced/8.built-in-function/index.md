{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Using built-in functions provided by Kronos

Kronos provides some built-in functions that can be used in query `select` and conditional filtering `where`, as follows:

```kotlin
KPojo.select { f.count(1) }.where { it.username like f.concat("%", it.username, "%") }.queryList()
```

## List of built-in functions

Kronos provides some mathematical functions `MathFunction`, some aggregation functions `PolymerizationFunction` and some string functions `StringFunction` as supported default built-in functions

### Aggregate functions

#### 1. {{ $.title("count") }} count

Returns the number of non-NULL values in the specified column

- **Function Declaration**

```kotlin
fun FunctionHandler.count(x: Any?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns', 'Any?']]) }}

#### 2. {{ $.title("sum") }} sum

Returns the sum of all values in the specified column

- **Function Declaration**

```kotlin
fun FunctionHandler.sum(x: Any?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns', 'Any?']]) }}

#### 3. {{ $.title("avg") }} average value

Returns the average value of target columns

- **Function Declaration**

```kotlin
fun FunctionHandler.avg(x: Any?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns', 'Any?']]) }}

#### 4. {{ $.title("max") }} maximum

Returns the maximum value of target columns

- **Function Declaration**

```kotlin
fun FunctionHandler.max(x: Any?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns', 'Any?']]) }}

#### 5. {{ $.title("min") }} minimum

Returns the minimum value of target columns

- **Function Declaration**

```kotlin
fun FunctionHandler.min(x: Any?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns', 'Any?']]) }}

#### 6. {{ $.title("groupConcat") }} string concatenation

Returns a result that is a combination of column values that belong to a group.

- **Function Declaration**

```kotlin
fun FunctionHandler.groupConcat(x: Any?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns', 'Any?']]) }}

### Mathematical calculation functions

#### 1. {{ $.title("add") }} addition

Returns the sum of x+y+...

- **Function Declaration**

```kotlin
fun FunctionHandler.add(vararg x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 2. {{ $.title("sub") }} subtraction

Returns the difference of x-y-...

- **Function Declaration**

```kotlin
fun FunctionHandler.sub(vararg x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 3. {{ $.title("mul") }} multiplication

Returns the product of x\*y\*...

- **Function Declaration**

```kotlin
fun FunctionHandler.mul(vararg x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 4. {{ $.title("div") }} division

Returns the quotient of x/y/...

- **Function Declaration**

```kotlin
fun FunctionHandler.div(vararg x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 5. {{ $.title("abs") }} absolute value

returns the absolute value of x

- **Function Declaration**

```kotlin
fun FunctionHandler.abs(x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 6. {{ $.title("bin") }} binary

Returns the binary value of x

- **Function Declaration**

```kotlin
fun FunctionHandler.bin(x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 7. {{ $.title("ceiling") }} round up

Returns the smallest integer value greater than x.

- **Function Declaration**

```kotlin
fun FunctionHandler.ceiling(x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 8. {{ $.title("exp") }} exponent

Returns the value e (the base of natural logarithms) raised to the power of x

- **Function Declaration**

```kotlin
fun FunctionHandler.exp(x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 9. {{ $.title("floor") }} round down

Returns the largest integer value less than x

- **Function Declaration**

```kotlin
fun FunctionHandler.floor(x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 10. {{ $.title("greatest") }} maximum

Returns the largest value in a collection

- **Function Declaration**

```kotlin
fun FunctionHandler.greatest(vararg x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 11. {{ $.title("least") }} minimum

Returns the smallest value in a collection

- **Function Declaration**

```kotlin
fun FunctionHandler.least(vararg x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 12. {{ $.title("ln") }} natural logarithm

Returns the natural logarithm of x

- **Function Declaration**

```kotlin
fun FunctionHandler.ln(x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 13. {{ $.title("log") }} logarithm

Returns the base y logarithm of x

- **Function Declaration**

```kotlin
fun FunctionHandler.log(x: Number?, y: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?'], ['y', 'specify base', 'Number?']]) }}

#### 14. {{ $.title("mod") }} mod

Returns the modulus (remainder) of x/y

- **Function Declaration**

```kotlin
fun FunctionHandler.mod(x: Number?, y: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?'], ['y', 'specify divisor', 'Number?']]) }}

#### 15. {{ $.title("pi") }} circumference of circle

Returns the value of pi (circumference of circle)

- **Function Declaration**

```kotlin
fun FunctionHandler.pi(): Number?
```

- **Usage Examples**

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

#### 16. {{ $.title("rand") }} random values

Returns a random value between 0 and 1

- **Function Declaration**

```kotlin
fun FunctionHandler.rand(): Number?
```

- **Usage Examples**

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

#### 17. {{ $.title("round") }} rounding

Returns the value of parameter x rounded to y decimal places.

- **Function Declaration**

```kotlin
fun FunctionHandler.round(x: Number?, y: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?'], ['y', 'specify the number of decimal places', 'Number?']]) }}

#### 18. {{ $.title("sign") }} symbol

Returns the value representing the sign of the number x

- **Function Declaration**

```kotlin
fun FunctionHandler.sign(x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 19. {{ $.title("sqrt") }} square root

Returns the square root of a number

- **Function Declaration**

```kotlin
fun FunctionHandler.sqrt(x: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?']]) }}

#### 20. {{ $.title("truncate") }} truncation

Returns the number x truncated to y decimal places.

- **Function Declaration**

```kotlin
fun FunctionHandler.truncate(x: Number?, y: Number?): Number?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target columns or number', 'Number?'], ['y', 'specify the number of decimal places', 'Number?']]) }}

### String functions

#### 1. {{ $.title("length") }} number of characters

Returns the number of characters in string x

- **Function Declaration**

```kotlin
fun FunctionHandler.length(x: String?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?']]) }}

#### 2. {{ $.title("upper") }} uppercase

Returns the result of converting all characters in string x to uppercase.

- **Function Declaration**

```kotlin
fun FunctionHandler.upper(x: String?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?']]) }}

#### 3. {{ $.title("lower") }} lowercase

Returns the result of converting all characters in string x to lowercase.

- **Function Declaration**

```kotlin
fun FunctionHandler.lower(x: String?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?']]) }}

#### 4. {{ $.title("substr") }} interception

Returns the z characters from string x starting at position y

- **Function Declaration**

```kotlin
fun FunctionHandler.substr(x: String?, y: Int?, z: Int?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?'], ['y', 'starting position', 'Int?'], ['z', 'number of characters', 'Int?']]) }}

#### 5. {{ $.title("replace") }} replace

Returns the result of replacing all y in string x with z

- **Function Declaration**

```kotlin
fun FunctionHandler.replace(x: String?, y: String?, z: String?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?'], ['y', 'replaced characters', 'String?'], ['z', 'characters to replace', 'String?']]) }}

#### 6. {{ $.title("left") }} left intercept

Returns the leftmost y characters from string x

- **Function Declaration**

```kotlin
fun FunctionHandler.left(x: String?, y: Int?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?'], ['y', 'number of characters', 'Int?']]) }}

#### 7. {{ $.title("right") }} right Intercept

Returns the rightmost y characters from string x

- **Function Declaration**

```kotlin

fun FunctionHandler.right(x: String?, y: Int?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?'], ['y', 'number of characters', 'Int?']]) }}

#### 8. {{ $.title("repeat") }} repeat

Returns the string x repeated y times.

- **Function Declaration**

```kotlin
fun FunctionHandler.repeat(x: String?, y: Int?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?'], ['y', 'repetitions', 'Int?']]) }}

#### 9. {{ $.title("reverse") }} reverse

Returns the result of reversing the string x

- **Function Declaration**

```kotlin
fun FunctionHandler.reverse(x: String?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?']]) }}

#### 10. {{ $.title("trim") }} remove spaces

Remove all spaces from the beginning and end of a string

- **Function Declaration**

```kotlin
fun FunctionHandler.trim(x: String?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?']]) }}

#### 11. {{ $.title("ltrim") }} remove left space

Remove all leading spaces from a string

- **Function Declaration**

```kotlin
fun FunctionHandler.ltrim(x: String?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?']]) }}

#### 12. {{ $.title("rtrim") }} remove right space

Remove all trailing spaces from a string

- **Function Declaration**

```kotlin
fun FunctionHandler.rtrim(x: String?): Any?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?']]) }}

#### 13. {{ $.title("concat") }} concat

Concatenate x1, x2..., xn into a string

- **Function Declaration**

```kotlin
fun FunctionHandler.concat(vararg x: String?): String?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target string', 'String?']]) }}

#### 14. {{ $.title("join") }} connect

Concatenate y1, y2..., yn into a string using separator x

- **Function Declaration**

```kotlin
fun FunctionHandler.join(x: String?, vararg y: String?): String?
```

- **Usage Examples**

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

- **Receiving Parameters**

{{ $.params([['x', 'target delimiter', 'String?'], ['y', 'target string', 'String?']]) }}