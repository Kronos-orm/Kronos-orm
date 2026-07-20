{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 使用内置函数

Kronos 在查询 DSL 中通过 `f` receiver 提供内置函数。函数可以写在 `select`、`where`、`having`、`orderBy` 和 join `on` 条件中。

```kotlin group="Function entry 1" name="kotlin" icon="kotlin"
val rows = User()
    .select {
        [
            it.id,
            f.length(it.name).alias("nameLength"),
            f.abs(it.score).alias("absoluteScore")
        ]
    }
    .where { f.length(it.name) > 3 && f.abs(it.score) > 10 }
    .orderBy { it.nameLength.desc() }
    .toList()
```

```sql group="Function entry 1" name="MySQL" icon="mysql"
SELECT `id`,
       LENGTH(`name`) AS `nameLength`,
       ABS(`score`) AS `absoluteScore`
FROM `user`
WHERE LENGTH(`name`) > :nameLength
  AND ABS(`score`) > :absoluteScore
ORDER BY `nameLength` DESC
```

函数表达式出现在 `select { ... }` 中时，通常要使用 `.alias("name")`。alias 会成为 `toMapList()` 的 Map key、生成投影的属性名，也可以被同层 `orderBy` 读取。投影结果形态见 {{ $.keyword("query/projection", ["投影"]) }}。

```kotlin group="Function entry 2" name="consume result" icon="kotlin"
val first = rows.first()
val nameLength: Int? = first.nameLength
val absoluteScore: Int? = first.absoluteScore
```

函数 SQL 由当前数据库方言渲染。完整内置方言列表见 {{ $.keyword("database/dialect-support", ["方言支持"]) }}。

## 聚合函数

聚合函数可以和 `groupBy`、`having` 一起使用。聚合表达式作为返回列时，也要设置 alias。

```kotlin group="Aggregate functions" name="kotlin" icon="kotlin"
val rows = Order()
    .select {
        [
            it.userId,
            f.count(it.id).alias("orderCount"),
            f.sum(it.amount).alias("totalAmount")
        ]
    }
    .groupBy { it.userId }
    .having { f.count(it.id) > 1 }
    .toList()
```

```sql group="Aggregate functions" name="MySQL" icon="mysql"
SELECT `user_id` AS `userId`,
       COUNT(`id`) AS `orderCount`,
       SUM(`amount`) AS `totalAmount`
FROM `order`
GROUP BY `user_id`
HAVING COUNT(`id`) > :orderCount
```

```kotlin group="Aggregate functions" name="consume result" icon="kotlin"
val first = rows.first()
val userId: Int? = first.userId
val orderCount: Int? = first.orderCount
val totalAmount: java.math.BigDecimal? = first.totalAmount
```

| 函数 | Kotlin 入口 | 常见用途 |
|------|-------------|----------|
| `count` | `f.count(x)` | 统计行数或非空值数量。 |
| `sum` | `f.sum(x)` | 数值求和。 |
| `avg` | `f.avg(x)` | 平均值。 |
| `max` | `f.max(x)` | 最大值。 |
| `min` | `f.min(x)` | 最小值。 |
| `groupConcat` | `f.groupConcat(x)` | 分组后拼接值，具体 SQL 按方言渲染。 |

## 数学函数和运算符

加、减、乘、除和取模优先使用 Kotlin 运算符；其他数学函数通过 `f` 调用。

```kotlin group="Math functions" name="kotlin" icon="kotlin"
val rows = User()
    .select {
        [
            it.id,
            (it.score + 10).alias("scorePlus"),
            (it.score % 2).alias("scoreMod"),
            f.ceil(it.score).alias("scoreCeil"),
            f.trunc(it.score, 2).alias("scoreTrunc")
        ]
    }
    .toList()
```

```sql group="Math functions" name="MySQL" icon="mysql"
SELECT `id`,
       (`score` + 10) AS `scorePlus`,
       MOD(`score`, 2) AS `scoreMod`,
       CEIL(`score`) AS `scoreCeil`,
       TRUNCATE(`score`, 2) AS `scoreTrunc`
FROM `user`
```

| 函数 | Kotlin 入口 |
|------|-------------|
| 加法 | `it.score + 1` 或 `f.add(...)` |
| 减法 | `it.score - 1` 或 `f.sub(...)` |
| 乘法 | `it.score * 2` 或 `f.mul(...)` |
| 除法 | `it.score / 2` 或 `f.div(...)` |
| 取模 | `it.score % 2` 或 `f.mod(x, y)` |
| 绝对值 | `f.abs(x)` |
| 二进制表示 | `f.bin(x)` |
| 向上取整 | `f.ceil(x)` |
| 指数 | `f.exp(x)` |
| 向下取整 | `f.floor(x)` |
| 多值最大 | `f.greatest(x, y, ...)` |
| 多值最小 | `f.least(x, y, ...)` |
| 自然对数 | `f.ln(x)` |
| 指定底数对数 | `f.log(x, y)` |
| 圆周率 | `f.pi()` |
| 随机数 | `f.rand()` |
| 四舍五入 | `f.round(x, scale)` |
| 符号 | `f.sign(x)` |
| 平方根 | `f.sqrt(x)` |
| 截断 | `f.trunc(x, scale)` |

## 字符串函数

字符串函数可以用于返回列，也可以用于条件。

```kotlin group="String functions" name="kotlin" icon="kotlin"
val users = User()
    .select {
        [
            it.id,
            f.upper(it.name).alias("upperName"),
            f.concat(it.name, "-", it.status).alias("displayName")
        ]
    }
    .where { f.length(it.name) > 3 }
    .toList()
```

```sql group="String functions" name="MySQL" icon="mysql"
SELECT `id`,
       UPPER(`name`) AS `upperName`,
       CONCAT(`name`, '-', `status`) AS `displayName`
FROM `user`
WHERE LENGTH(`name`) > :nameLength
```

| 函数 | Kotlin 入口 |
|------|-------------|
| 长度 | `f.length(x)` |
| 大写 | `f.upper(x)` |
| 小写 | `f.lower(x)` |
| 截取 | `f.substr(x, start, length)` |
| 替换 | `f.replace(x, old, new)` |
| 左截取 | `f.left(x, length)` |
| 右截取 | `f.right(x, length)` |
| 重复 | `f.repeat(x, times)` |
| 反转 | `f.reverse(x)` |
| 去除两端空白 | `f.trim(x)` |
| 去除左侧空白 | `f.ltrim(x)` |
| 去除右侧空白 | `f.rtrim(x)` |
| 拼接 | `f.concat(x, y, ...)` |
| 使用分隔符拼接 | `f.join(separator, x, y, ...)` |

## 使用窗口函数

Kronos 当前提供 `f.rowNumber()` 作为窗口函数入口。需要导入 `com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber`。

```kotlin group="Window function 1" name="kotlin" icon="kotlin"
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber

val ranked = Order()
    .select {
        [
            it.id,
            it.userId,
            it.status,
            f.rowNumber()
                .over {
                    partitionBy(it.userId)
                    orderBy(it.status.desc())
                }
                .alias("rn")
        ]
    }

val rows = ranked
    .orderBy { it.rn.asc() }
    .toList()
```

```sql group="Window function 1" name="MySQL" icon="mysql"
SELECT `id`,
       `user_id` AS `userId`,
       `status`,
       ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` DESC) AS rn
FROM `order`
ORDER BY `rn` ASC
```

窗口函数 alias 可以在同一层用于排序。谓词需要读取窗口 alias 时，先选出窗口结果，再用 `filter` 通过派生查询进行筛选；示例见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

```kotlin group="Window function 2" name="consume result" icon="kotlin"
val first = rows.first()
val rowNumber: Int? = first.rn
```

```kotlin group="Window function 2" name="filter alias" icon="kotlin"
val firstPerUser = ranked
    .filter { it.rn == 1 }
    .toList()
```

```sql group="Window function 2" name="filter sql" icon="mysql"
SELECT `q`.`id`, `q`.`userId`, `q`.`status`, `q`.`rn`
FROM (
    SELECT `id`,
           `user_id` AS `userId`,
           `status`,
           ROW_NUMBER() OVER (PARTITION BY `user_id` ORDER BY `status` DESC) AS rn
    FROM `order`
) AS `q`
WHERE `q`.`rn` = :rn
```

## PostgreSQL 数组辅助函数

PostgreSQL 数组比较可以使用专用的 `f.any(...)` 和 `f.all(...)`。使用前导入 `com.kotlinorm.functions.bundled.exts.PostgresFunctions.any` 或 `all`。这些函数只用于 PostgreSQL 数组语义，普通子查询量词 `any<T>(query)`、`some<T>(query)`、`all<T>(query)` 见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

```kotlin group="PostgreSQL functions" name="kotlin" icon="kotlin"
import com.kotlinorm.functions.bundled.exts.PostgresFunctions.any

val rows = User()
    .select()
    .where { it.id == f.any(intArrayOf(1, 2, 3)) }
    .toList()
```

## 自定义函数

需要项目内函数或方言专用函数时，可以使用自定义函数扩展。写法见 {{ $.keyword("advanced/custom-functions", ["自定义函数"]) }}。
