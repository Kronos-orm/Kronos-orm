# Kotlin 标量字段语法糖清单

状态：2026-07-24 完成 Kotlin 2.4 静态复核。本文件只记录可被改造成 SQL **单值表达式**的真实 Kotlin/JVM 源码形态；它不是 `f.*` 函数目录，也不承诺这些形态已经可用。

## 边界

本清单以当前编译器的 KPojo 列类型映射为边界：`Boolean`、`Byte`、`Short`、`Int`、`Long`、`Float`、`Double`、`java.math.BigDecimal`、`Char`、`String`、`ByteArray`、`java.util.Date`、`java.sql.Date`、`java.sql.Timestamp`、`java.time.LocalDate`、`java.time.LocalTime`、`java.time.LocalDateTime`、`kotlinx.datetime.LocalDate`、`kotlinx.datetime.LocalTime` 和 `kotlinx.datetime.LocalDateTime`。每个调用的字段、接收者和实参都必须是字段表达式、字面量或运行时标量值。

- 不纳入 `Iterable` / `Sequence` / 数组展开、`IntRange`、`Regex`、`Locale`、`MathContext`、`ChronoUnit`、`DateTimeUnit`、比较器和 lambda。它们不是当前定义的标量字段语法糖输入。
- 不纳入跨行聚合。`f.sum`、`f.avg`、`f.min`、`f.max`、`f.count`、`f.groupConcat` 仍是聚合 DSL，不是本文件的候选。
- `Int`、`Long` 等普通数值没有 `.sum()`、`.avg()`、`.min()` 或 `.max()` 成员。行内最小/最大值的真实 Kotlin 写法是 `minOf(a, b)` / `maxOf(a, b)`，或 `kotlin.math.min(a, b)` / `max(a, b)`；`BigDecimal.min(other)` / `max(other)` 是 Java 成员。
- 默认讨论非空字段。可空字段只能在通用安全调用 lowerer 完成后使用真实安全调用（例如 `field?.length`、`field?.replace(oldValue, newValue)`）；不得把仅接受非空类型的 callable 直接套到 `T?` 字段。
- “可改造”限定为不引入 UDF、过程语言、客户端回退或读取业务表其他行时，能以一列为输入产生一个结果的 SQL 标量表达式或标量子查询；因此本清单不把 JVM 特有的对象身份、哈希或格式化算法当作 SQL 语法糖候选。

插件必须按解析后的 callable 精确匹配 FQ 名、dispatch receiver、extension receiver、参数个数、参数类型和哪个参数是 SQL 源，不能仅按函数名匹配。

| 标记 | 含义 |
| --- | --- |
| 已降级 | 当前插件已经处理的源形态。 |
| 复用 | 只需新增精确 IR 规则，可复用现有 `f.*` 或 AST。 |
| 组合 | 由已有函数、算术、比较、`CASE`、`COALESCE` 组合。 |
| 方言原语 | 需要新增 AST 语义或逐方言 renderer。 |
| 契约 | SQL 与 Kotlin 在空值、Unicode、浮点数或异常方面有差异，必须先锁定行为。 |

## 当前已降级

| Kotlin 源码形态 | 已生成的 SQL 语义 | 当前边界 |
| --- | --- | --- |
| `field.uppercase()`、`field.lowercase()` | `UPPER(field)`、`LOWER(field)` | 仅 `String` 零参数扩展；条件 DSL 已覆盖安全调用。 |
| `field + value`、`-`、`*`、`/`、`%`、一元 `+field` / `-field` | 算术或字符串拼接 | 由运算符 lowerer 处理；直接写出的 `plus` / `minus` / `times` / `div` / `rem` 与对应运算符解析为同一 callable。 |
| `!field`、`field && other`、`field || other` | `NOT`、`AND`、`OR` | Boolean 条件 DSL。 |
| `field == value`、`!=`、`>`、`>=`、`<`、`<=`、`in` | 比较、`BETWEEN`、`IN` | 条件 DSL。 |
| `field.contains(value)`、`field.startsWith(value)`、`field.endsWith(value)` | 转义后的 `LIKE` | 当前只覆盖无 `ignoreCase`、无起始下标的条件匹配形态。 |

`KotlinSqlFunctionRules` 目前只登记了 `kotlin.text.uppercase` 和 `kotlin.text.lowercase`。属性 getter、JVM 成员、顶层 `kotlin.math` / `kotlin.comparisons` 函数、`BigDecimal` 成员以及字段不在 receiver 位置的调用尚未有通用规则。

## 可改造的 String 与 Char 形态

### 可复用现有函数或 AST

| Kotlin 源码形态 | 目标 SQL | 标记与边界 |
| --- | --- | --- |
| `field.length`、`field.count()`、`field.any()`、`field.none()` | `LENGTH(field)` | 复用；前者是 JVM `String` 属性，后三者是 `CharSequence` 扩展。JVM UTF-16 code unit 与数据库字符长度需契约。 |
| `field.replace(oldChar, newChar)`、`field.replace(oldValue, newValue)` | `REPLACE(field, old, new)` | 复用；仅 `ignoreCase = false`。空查找串、排序规则和 `NULL` 需要结果测试。 |
| `field.substring(startIndex)`、`field.substring(startIndex, endIndex)`、`field.subSequence(startIndex, endIndex)` | `SUBSTR` | 复用；Kotlin 从 0 开始且结束位置排他，SQL 通常从 1 开始。 |
| `field.take(n)`、`field.takeLast(n)` | `LEFT` / `RIGHT` 或 `SUBSTR` | 复用；目标长度不大于原长时 Kotlin 返回原串。 |
| `field.drop(n)`、`field.dropLast(n)` | `SUBSTR` + `LENGTH` | 组合；负数抛错，不能静默变成 SQL `NULL`。 |
| `field.reversed()` | `REVERSE(field)` | 复用；内置 H2 不支持，stock SQLite 也没有可靠内置实现。 |
| `field.trim(trimChar)`、`field.trimStart(trimChar)`、`field.trimEnd(trimChar)` | `TRIM`、`LTRIM`、`RTRIM` | 复用；单字符 `vararg Char` 形态可直接复用方言函数。 |
| `field.trim(firstTrimChar, secondTrimChar)`、`field.trimStart(firstTrimChar, secondTrimChar)`、`field.trimEnd(firstTrimChar, secondTrimChar)` | 多字符裁剪 | 方言原语；真实 `vararg Char` 还允许更多逐个列出的字符。语义是反复移除集合中的任意边界字符，不能用一次嵌套 `TRIM` 冒充。 |
| `field.repeat(count)` | `REPEAT(field, count)` | 复用 + 契约；Kotlin 的 `repeat(0)` 是空串，Oracle/DM8 原生结果是 `NULL`，因此不能直接宣称跨库 Kotlin 等价。 |
| `field.lastIndex` | `LENGTH(field) - 1` | 组合；空串结果为 `-1`。 |
| `field.getOrNull(index)`、`field.elementAtOrNull(index)`、`field.firstOrNull()`、`field.lastOrNull()`、`field.singleOrNull()` | `SUBSTR`、`LENGTH`、`CASE` | 组合；只列可空版本。`get` / `first` / `last` / `single` 的抛错语义不能降级为 `NULL`。 |
| `field.minOrNull()`、`field.maxOrNull()`、`field.min()`、`field.max()` | 字符串内部逐字符最小值 / 最大值 | 方言原语；它们返回 `Char`，不是 SQL 聚合。可空版本在空串返回 `NULL`，`min()` / `max()` 在空串抛 `NoSuchElementException`。 |
| `field.isEmpty()`、`field.isNotEmpty()` | `LENGTH` 比较 | 组合；Oracle/DM8 空串即 `NULL`，需单独处理。 |
| `field.isNullOrEmpty()`、`field.isNullOrBlank()`、`field.orEmpty()`、`field ?: default` | `IS NULL`、`COALESCE` | 组合；前两项分别是可空 `CharSequence` 扩展，`orEmpty()` 是可空 `String` 扩展，最后一项是 Elvis 表达式。`isNullOrBlank()` 还受 Unicode 空白契约约束。 |

### 需要位置查找或 `CASE` 组合

| Kotlin 源码形态 | 所需 SQL 语义 | 标记与边界 |
| --- | --- | --- |
| `field.indexOf(charDelimiter)`、`field.indexOf(charDelimiter, startIndex)`、`field.indexOf(stringDelimiter)`、`field.indexOf(stringDelimiter, startIndex)`、`field.lastIndexOf(charDelimiter)`、`field.lastIndexOf(charDelimiter, startIndex)`、`field.lastIndexOf(stringDelimiter)`、`field.lastIndexOf(stringDelimiter, startIndex)` | `INSTR` / `POSITION` / `LOCATE` | 方言原语；这些重载的 `ignoreCase` 必须是 `false`。Kotlin 是 0 基、未命中为 `-1`，并且有起始位置规则。 |
| `field.startsWith(prefix, startIndex)` | `SUBSTR` / `LENGTH` 比较 | 组合；无 `ignoreCase` 的起始下标重载不在现有 `LIKE` lowerer 覆盖范围内。 |
| `field.substringBefore(charDelimiter)`、`field.substringBefore(stringDelimiter)`、`field.substringBefore(charDelimiter, fallback)`、`field.substringBefore(stringDelimiter, fallback)` | 位置查找 + `SUBSTR` + `CASE` | 组合；`Char` / `String` 分隔符及 `missingDelimiterValue` 都必须按 Kotlin 回退语义处理。 |
| `field.substringAfter(charDelimiter)`、`field.substringAfter(stringDelimiter)`、`field.substringAfter(charDelimiter, fallback)`、`field.substringAfter(stringDelimiter, fallback)` | 位置查找 + `SUBSTR` + `CASE` | 组合；`Char` / `String` 分隔符及 `missingDelimiterValue` 都必须按 Kotlin 回退语义处理。 |
| `field.substringBeforeLast(charDelimiter)`、`field.substringBeforeLast(stringDelimiter)`、`field.substringBeforeLast(charDelimiter, fallback)`、`field.substringBeforeLast(stringDelimiter, fallback)` | 反向位置查找 + `SUBSTR` + `CASE` | 组合；`Char` / `String` 分隔符及 `missingDelimiterValue` 都必须按 Kotlin 回退语义处理。 |
| `field.substringAfterLast(charDelimiter)`、`field.substringAfterLast(stringDelimiter)`、`field.substringAfterLast(charDelimiter, fallback)`、`field.substringAfterLast(stringDelimiter, fallback)` | 反向位置查找 + `SUBSTR` + `CASE` | 组合；`Char` / `String` 分隔符及 `missingDelimiterValue` 都必须按 Kotlin 回退语义处理。 |
| `field.replaceBefore(charDelimiter, replacement)`、`field.replaceBefore(stringDelimiter, replacement)`、`field.replaceBefore(charDelimiter, replacement, fallback)`、`field.replaceBefore(stringDelimiter, replacement, fallback)` | 位置查找 + `SUBSTR` + `CONCAT` | 组合；不匹配时必须使用 Kotlin 的回退结果。 |
| `field.replaceAfter(charDelimiter, replacement)`、`field.replaceAfter(stringDelimiter, replacement)`、`field.replaceAfter(charDelimiter, replacement, fallback)`、`field.replaceAfter(stringDelimiter, replacement, fallback)` | 位置查找 + `SUBSTR` + `CONCAT` | 组合；不匹配时必须使用 Kotlin 的回退结果。 |
| `field.replaceBeforeLast(charDelimiter, replacement)`、`field.replaceBeforeLast(stringDelimiter, replacement)`、`field.replaceBeforeLast(charDelimiter, replacement, fallback)`、`field.replaceBeforeLast(stringDelimiter, replacement, fallback)` | 反向位置查找 + `SUBSTR` + `CONCAT` | 组合；不匹配时必须使用 Kotlin 的回退结果。 |
| `field.replaceAfterLast(charDelimiter, replacement)`、`field.replaceAfterLast(stringDelimiter, replacement)`、`field.replaceAfterLast(charDelimiter, replacement, fallback)`、`field.replaceAfterLast(stringDelimiter, replacement, fallback)` | 反向位置查找 + `SUBSTR` + `CONCAT` | 组合；不匹配时必须使用 Kotlin 的回退结果。 |
| `field.replaceFirst(oldChar, newChar)`、`field.replaceFirst(oldValue, newValue)` | 首次位置查找 + `SUBSTR` + `CONCAT` | 组合；只支持精确匹配的 `ignoreCase = false` 语义。 |
| `field.removePrefix(prefix)`、`field.removeSuffix(suffix)`、`field.removeSurrounding(prefix, suffix)`、`field.removeSurrounding(delimiter)` | 前后缀判断 + `CASE` + `SUBSTR` | 组合；不匹配时必须返回原串。 |
| `field.replaceRange(startIndex, endIndex, replacement)`、`field.removeRange(startIndex, endIndex)` | 前段 + replacement / 空串 + 后段 | 组合；只列两个标量索引重载，越界异常不能吞掉。 |
| `field.padStart(length, padChar)`、`field.padEnd(length, padChar)` | `CASE` + `REPEAT` + `CONCAT` | 组合；`LPAD` / `RPAD` 会截断，Kotlin 在目标长度不大于原长时不会。 |
| `field.commonPrefixWith(other)`、`field.commonSuffixWith(other)` | 逐字符比较 + `SUBSTR` | 方言原语；无 `ignoreCase` 的 `CharSequence` 重载也需要逐字符长度和 Unicode 契约。 |
| `field.hasSurrogatePairAt(index)`、`field.codePointAt(index)`、`field.codePointBefore(index)`、`field.codePointCount(beginIndex, endIndex)`、`field.offsetByCodePoints(index, codePointOffset)` | Unicode surrogate / code-point 函数 | 方言原语；这是 Kotlin/JVM `String` 扩展，索引越界异常和 UTF-16 / code-point 换算不能省略。 |
| `field.isBlank()`、`field.isNotBlank()`、`field.trim()`、`field.trimStart()`、`field.trimEnd()` | 空白判断或裁剪 | 契约；Kotlin 使用 Unicode `isWhitespace`，常见 SQL 只按普通空格。 |

### 大小写、比较和解析

| Kotlin 源码形态 | 所需 SQL 语义 | 标记与边界 |
| --- | --- | --- |
| `field.indexOf(charDelimiter, startIndex, ignoreCase = true)`、`field.indexOf(stringDelimiter, startIndex, ignoreCase = true)`、`field.lastIndexOf(charDelimiter, startIndex, ignoreCase = true)`、`field.lastIndexOf(stringDelimiter, startIndex, ignoreCase = true)`、`field.contains(other, ignoreCase = true)`、`field.startsWith(prefix, ignoreCase = true)`、`field.startsWith(prefix, startIndex, ignoreCase = true)`、`field.endsWith(suffix, ignoreCase = true)`、`field.replace(oldChar, newChar, ignoreCase = true)`、`field.replace(oldValue, newValue, ignoreCase = true)`、`field.replaceFirst(oldChar, newChar, ignoreCase = true)`、`field.replaceFirst(oldValue, newValue, ignoreCase = true)` | 忽略大小写查找、匹配和替换 | 契约；不能把它们降级为普通 `LIKE` 或 `REPLACE`，需要 Kotlin Unicode case folding 与数据库 collation 的约束。 |
| `field.equals(other)`、`field.compareTo(other)`、`field.contentEquals(other)`、`field.regionMatches(thisOffset, other, otherOffset, length)` | 相等、三路比较、分段比较 | 组合；`contentEquals(other)` 的实参限定为可映射的 `String` / `CharSequence`，不能把任意对象相等误降级为 SQL 相等。 |
| `field.equals(other, ignoreCase = true)`、`field.compareTo(other, ignoreCase = true)`、`field.regionMatches(thisOffset, other, otherOffset, length, ignoreCase = true)` | 忽略大小写相等 / 比较 / 分段比较 | 契约；`contentEquals(other)` 没有 `ignoreCase` 参数。 |
| `field.toByteOrNull()`、`field.toShortOrNull()`、`field.toIntOrNull()`、`field.toLongOrNull()`、`field.toFloatOrNull()`、`field.toDoubleOrNull()`、`field.toBigDecimalOrNull()` | `TRY_CAST` 或受控 `CASE` | 方言原语；空白、溢出、格式和结果类型必须与 Kotlin 对齐。 |
| `field.toByte()`、`field.toShort()`、`field.toInt()`、`field.toLong()`、`field.toFloat()`、`field.toDouble()`、`field.toBigDecimal()` | 严格文本解析 | 契约；抛错版本不能无声改成 SQL `NULL`。 |
| `field.toByte(radix)`、`field.toShort(radix)`、`field.toInt(radix)`、`field.toLong(radix)` 及对应的 `field.toByteOrNull(radix)`、`field.toShortOrNull(radix)`、`field.toIntOrNull(radix)`、`field.toLongOrNull(radix)` | 2--36 进制解析 | 方言原语；不是普通 `CAST`。 |
| `field.toBoolean()`、`field.toBooleanStrict()`、`field.toBooleanStrictOrNull()` | Kotlin Boolean 词表的 `CASE` | 契约；宽松、严格和可空版本的词表与异常不同。 |

### Char 字段

| Kotlin 源码形态 | 所需 SQL 语义 | 标记与边界 |
| --- | --- | --- |
| `charField.uppercase()`、`charField.lowercase()`、`charField.titlecase()`、`charField.uppercaseChar()`、`charField.lowercaseChar()`、`charField.titlecaseChar()` | 大小写转换 | 方言原语 + 契约；前三者返回 `String`，其余返回 `Char`。 |
| `charField.code`、`charField.digitToInt()`、`charField.digitToInt(radix)`、`charField.digitToIntOrNull()`、`charField.digitToIntOrNull(radix)` | 代码点 / 数字解析 | 方言原语；Unicode 和 radix 规则需精确定义。 |
| `charField.equals(other, ignoreCase = true)`、`charField.isDigit()`、`charField.isLetter()`、`charField.isLetterOrDigit()`、`charField.isWhitespace()`、`charField.isUpperCase()`、`charField.isLowerCase()`、`charField.isTitleCase()`、`charField.isDefined()`、`charField.isISOControl()`、`charField.isHighSurrogate()`、`charField.isLowSurrogate()`、`charField.isSurrogate()`、`charField.isIdentifierIgnorable()`、`charField.isJavaIdentifierStart()`、`charField.isJavaIdentifierPart()` | Unicode 相等、字符分类和 JVM 标识符分类 | 契约；不能假设数据库正则或 collation 等于 Kotlin Unicode / JVM 分类。 |

## 可改造的数值、BigDecimal 与 Boolean 形态

### 可复用现有数学函数或表达式

| Kotlin 源码形态 | 目标 SQL | 标记与边界 |
| --- | --- | --- |
| `kotlin.math.abs(field)`、`field.absoluteValue` | `ABS(field)` | 复用；真实重载是 `Int`、`Long`、`Float`、`Double`。`MIN_VALUE`、NaN、Infinity 需要契约。 |
| `field.sign`、`kotlin.math.sign(field)` | `SIGN(field)` | 复用；属性覆盖 `Int`、`Long`、`Float`、`Double`，顶层 `sign` 是 `Float` / `Double` 重载；`Long.sign` 返回 `Int`。 |
| `field.abs()`、`field.negate()`、`field.signum()` | `ABS`、一元负号、`SIGN` | 复用；仅 `java.math.BigDecimal` 的无参成员。 |
| `kotlin.math.ceil(field)`、`kotlin.math.floor(field)`、`kotlin.math.truncate(field)`、`kotlin.math.exp(field)`、`kotlin.math.ln(field)`、`kotlin.math.sqrt(field)`、`kotlin.math.log(field, base)`、`kotlin.math.log(value, field)` | 对应数学函数 | 复用；真实 Kotlin 2.4 重载为 `Float` / `Double`。顶层函数规则必须记录字段位于哪个实参槽。 |
| `kotlin.math.min(a, b)`、`kotlin.math.max(a, b)` | 行级 `LEAST`、`GREATEST` | 复用；真实重载是 `Int`、`Long`、`Float`、`Double`，不是聚合。 |
| `minOf(a, b)`、`minOf(a, b, c)`、`minOf(a, b, c, d)`、`maxOf(a, b)`、`maxOf(a, b, c)`、`maxOf(a, b, c, d)` | 行级 `LEAST`、`GREATEST` | 复用；真实数值重载有二元、三元和首参加 `vararg` 形态，所有参数可以是字段；不接受 `*array`。数值重载之外的 `Comparable` 类型须单独确认 collation / 空值。 |
| `bigDecimal.min(other)`、`bigDecimal.max(other)` | 行级 `LEAST`、`GREATEST` | 复用；仅 `BigDecimal` dispatch member，不能泛化为普通数值成员。 |
| `bigDecimal.add(other)`、`bigDecimal.subtract(other)`、`bigDecimal.multiply(other)`、`bigDecimal.remainder(other)` | 十进制算术 | 复用；这是 Java `BigDecimal` 成员，和 Kotlin 运算符版本都必须保留 DECIMAL 精度。 |
| `bigDecimal.setScale(scale, RoundingMode.DOWN)` | `TRUNC(field, scale)` | 复用；只覆盖 `DOWN`。其他 `RoundingMode` 不能按函数名猜测。 |
| `field.inc()`、`field.dec()` | `field + 1`、`field - 1` | 组合；适用于真实 Kotlin 数值成员，溢出策略要保持清晰。 |
| `field.coerceAtLeast(min)`、`field.coerceAtMost(max)`、`field.coerceIn(min, max)` | `LEAST` / `GREATEST` 或 `CASE` | 组合；只列两个标量边界的调用，不列 range 参数重载。 |
| `field.floorDiv(other)` | `FLOOR(field / other)` 或 `CASE` | 组合；仅整数 receiver 的实际重载，负数除法必须遵循 Kotlin floor division。 |
| `field.toByte()`、`field.toShort()`、`field.toInt()`、`field.toLong()`、`field.toFloat()`、`field.toDouble()`、`field.toBigDecimal()` | `CAST` | 契约；截断、溢出、NaN / Infinity 和返回 Kotlin 类型需测试。无 `MathContext` 的 `toBigDecimal()` 可作为独立规则。 |
| `byteField.toString(radix)`、`shortField.toString(radix)`、`intField.toString(radix)`、`longField.toString(radix)` | 非十进制文本格式化 | 方言原语；真实 receiver 为有符号整数类型，radix 的 2--36 边界和负数表示不能交给数据库默认格式。 |
| `field.compareTo(other)` | `CASE` 生成 `-1` / `0` / `1` | 组合；直接 callable 与 `<` / `>` 语法不同。数值 NaN、文本 collation 和可空值要单独约束。 |

### 需要新增数学或位运算原语

| Kotlin 源码形态 | 所需 SQL 语义 | 标记与边界 |
| --- | --- | --- |
| `kotlin.math.round(field)`、`field.roundToInt()`、`field.roundToLong()` | Kotlin 舍入 | 方言原语；`round` 为 ties-to-even，`roundToInt` / `roundToLong` 为 ties-to-positive-infinity 且 NaN 会抛错。 |
| `field.pow(exponent)`、`kotlin.math.cbrt(field)`、`kotlin.math.sin(field)`、`kotlin.math.cos(field)`、`kotlin.math.tan(field)`、`kotlin.math.asin(field)`、`kotlin.math.acos(field)`、`kotlin.math.atan(field)`、`kotlin.math.atan2(y, x)`、`kotlin.math.sinh(field)`、`kotlin.math.cosh(field)`、`kotlin.math.tanh(field)`、`kotlin.math.asinh(field)`、`kotlin.math.acosh(field)`、`kotlin.math.atanh(field)`、`kotlin.math.hypot(x, y)`、`kotlin.math.expm1(field)`、`kotlin.math.ln1p(field)`、`kotlin.math.log2(field)`、`kotlin.math.log10(field)` | 幂、三角、双曲和派生对数函数 | 方言原语；这些真实 Kotlin 2.4 API 有 `Float` / `Double` 重载。`atan2` / `hypot` 的两个实参都可为字段，需逐方言函数名、定义域和精度验证。 |
| `field.IEEErem(divisor)`、`field.withSign(sign)`、`field.isNaN()`、`field.isInfinite()`、`field.isFinite()` | IEEE 浮点余数、符号和特殊值判断 | 契约；多数数据库对 NaN / Infinity 的存储和比较不同。 |
| `floatField.toBits()`、`floatField.toRawBits()`、`doubleField.toBits()`、`doubleField.toRawBits()`、`Float.fromBits(bits)`、`Double.fromBits(bits)` | IEEE 位模式转换 | 方言原语；顶层 `fromBits` 的字段位于实参槽，NaN payload 与数据库浮点存储需要逐方言验证。 |
| `field.and(other)`、`field.or(other)`、`field.xor(other)`、`field.inv()`、`field.shl(bitCount)`、`field.shr(bitCount)`、`field.ushr(bitCount)` | 位运算 | 方言原语；`ushr`、位宽和负数二进制表示必须按 Kotlin 固定位宽实现。 |
| `field.countOneBits()`、`field.countLeadingZeroBits()`、`field.countTrailingZeroBits()`、`field.takeHighestOneBit()`、`field.takeLowestOneBit()`、`field.rotateLeft(bitCount)`、`field.rotateRight(bitCount)` | 位计数、掩码和循环移位 | 方言原语；`Byte` / `Short` / `Int` / `Long` 的位宽不能交给数据库默认整数宽度。 |
| `bigDecimal.pow(n)`、`bigDecimal.setScale(scale, roundingMode)`、`bigDecimal.movePointLeft(n)`、`bigDecimal.movePointRight(n)`、`bigDecimal.scaleByPowerOfTen(n)` | 精确十进制幂、舍入和缩放 | 方言原语；只列标量参数重载，`MathContext` 版本不在本清单。 |
| `bigDecimal.divide(other)`、`bigDecimal.divideToIntegralValue(other)`、`bigDecimal.stripTrailingZeros()`、`bigDecimal.ulp()`、`bigDecimal.scale()`、`bigDecimal.precision()` | 十进制除法、截断除法、规范化或元数据 | 方言原语 + 契约；Java 成员的非终止小数、尾随零和精度规则不能被普通 `/` 或 `CAST` 替代。 |
| `field.not()`、`field.and(other)`、`field.or(other)`、`field.xor(other)`、`field.compareTo(other)`、`field.toString()` | `NOT`、`AND`、`OR`、XOR `CASE`、排序比较、`CASE` 文本 | 组合；仅非空 `Boolean`。SQL 三值逻辑与 Kotlin Boolean 不同，`toString()` 必须固定为 Kotlin 的小写 `true` / `false`。 |

## 可改造的日期时间与二进制形态

时间 API 必须按 `java.time`、`kotlinx.datetime` 与旧 JDBC 类型分别匹配；它们同名属性和方法的 FQ 名、返回类型及空值/精度约定不同，不能按方法名合并规则。

### `java.time` 本地时间

| Kotlin 源码形态 | 所需 SQL 语义 | 标记与边界 |
| --- | --- | --- |
| `date.year`、`date.monthValue`、`date.dayOfMonth`、`date.dayOfYear`、`date.lengthOfMonth()`、`date.isLeapYear`，以及 `dateTime.year`、`dateTime.monthValue`、`dateTime.dayOfMonth`、`dateTime.dayOfYear` | `EXTRACT` / `CASE` | 方言原语；当前 AST 没有通用 `EXTRACT` 节点。`Month` / `DayOfWeek` 返回枚举，不在本标量类型边界内。 |
| `time.hour`、`time.minute`、`time.second`、`time.nano`、`time.toSecondOfDay()`、`time.toNanoOfDay()`，以及同名 `dateTime` 属性 | `EXTRACT` 和算术 | 方言原语；纳秒精度和 JDBC 类型精度需统一。 |
| `date.atTime(hour, minute)`、`date.atTime(hour, minute, second, nano)`、`date.atTime(time)`、`time.atDate(date)`、`date.atStartOfDay()` | DATE/TIME 组合 | 方言原语；每个实参都是标量，但构造和跨日规则要与 Java 时间类型一致。 |
| `dateTime.toLocalDate()`、`dateTime.toLocalTime()` | `CAST` / 日期时间截取 | 方言原语；必须定义每个方言的 DATE / TIME 转换。 |
| `value.isBefore(other)`、`value.isAfter(other)`、`value.isEqual(other)`、`value.compareTo(other)` | 比较运算符或三路比较 | 组合；只针对声明这些成员的 `LocalDate` / `LocalTime` / `LocalDateTime`，字段和参数精度需要结果测试。 |
| `value.plusDays(n)`、`value.minusDays(n)`、`value.plusWeeks(n)`、`value.minusWeeks(n)`、`value.plusMonths(n)`、`value.minusMonths(n)`、`value.plusYears(n)`、`value.minusYears(n)` | 日期时间 interval 算术 | 方言原语；适用于声明这些成员的 `LocalDate` / `LocalDateTime`。 |
| `value.plusHours(n)`、`value.minusHours(n)`、`value.plusMinutes(n)`、`value.minusMinutes(n)`、`value.plusSeconds(n)`、`value.minusSeconds(n)`、`value.plusNanos(n)`、`value.minusNanos(n)` | 时间 interval 算术 | 方言原语；适用于 `LocalTime` / `LocalDateTime`，须处理跨日和精度。 |
| `value.withYear(year)`、`value.withMonth(month)`、`value.withDayOfMonth(day)`、`value.withDayOfYear(dayOfYear)`、`value.withHour(hour)`、`value.withMinute(minute)`、`value.withSecond(second)`、`value.withNano(nano)` | 重建日期时间字段 | 方言原语；只在该 receiver 真实声明对应 member 时注册规则。 |
| `date.toEpochDay()` | epoch day | 方言原语；以 `1970-01-01` 为基准并明确数据库时区无关性。 |

### `kotlinx.datetime` 本地时间

| Kotlin 源码形态 | 所需 SQL 语义 | 标记与边界 |
| --- | --- | --- |
| `kxDate.year`、`kxDate.monthNumber`、`kxDate.dayOfMonth`、`kxDate.dayOfYear`，以及同名 `kxDateTime` 属性 | `EXTRACT` / `CASE` | 方言原语；`kotlinx.datetime` 使用 `monthNumber`，不是 `java.time` 的 `monthValue`。 |
| `kxTime.hour`、`kxTime.minute`、`kxTime.second`、`kxTime.nanosecond`、`kxTime.toSecondOfDay()`、`kxTime.toNanosecondOfDay()`，以及同名 `kxDateTime` 属性 | `EXTRACT` 和算术 | 方言原语；属性名是 `nanosecond`，不是 `java.time` 的 `nano`。 |
| `kxDate.atTime(hour, minute, second, nanosecond)`、`kxDate.atTime(kxTime)`、`kxTime.atDate(kxDate)`、`kxDateTime.date`、`kxDateTime.time` | DATE/TIME 组合或截取 | 方言原语；所有返回类型仍在当前列类型映射内。 |
| `kxDate.toEpochDays()`、`kxDate.compareTo(other)`、`kxTime.compareTo(other)`、`kxDateTime.compareTo(other)` | epoch day 或三路比较 | 方言原语 / 组合；`kotlinx.datetime` 的日期时间加减需要 `DateTimeUnit` / `DatePeriod`，不属于本清单的标量实参边界。 |

### 旧日期与二进制

| Kotlin 源码形态 | 所需 SQL 语义 | 标记与边界 |
| --- | --- | --- |
| `legacyDate.time`、`legacyDate.before(other)`、`legacyDate.after(other)`、`legacyDate.compareTo(other)` | epoch milliseconds 或比较 | 契约；适用于 `java.util.Date` / `java.sql.Date` / `java.sql.Timestamp` 的实际成员。时区、`java.sql.Date` 的日期截断和 JDBC 映射必须先锁定。 |
| `sqlDate.toLocalDate()`、`timestamp.toLocalDateTime()`、`timestamp.nanos` | JDBC 日期时间转换或纳秒字段 | 方言原语；分别限定为 `java.sql.Date` 与 `java.sql.Timestamp`，不能泛化到 `java.util.Date`。 |
| `binaryField.size`、`binaryField.isEmpty()`、`binaryField.isNotEmpty()`、`binaryField.contentEquals(other)` | `OCTET_LENGTH`、长度比较、二进制相等 | 方言原语；BLOB 相等比较并非所有数据库都支持，不能与字符串 `LENGTH` 混用。 |
| `binaryField.getOrNull(index)`、`binaryField.firstOrNull()`、`binaryField.lastOrNull()`、`binaryField.indexOf(byte)`、`binaryField.lastIndexOf(byte)` | BLOB 截取、字节定位与 `CASE` | 方言原语；Kotlin 是 0 基且未命中返回 `-1`，不能把二进制当作文本处理。 |
| `binaryField.decodeToString()`、`binaryField.decodeToString(startIndex, endIndex, throwOnInvalidSequence)` | 二进制到 UTF-8 文本 | 契约；必须固定 UTF-8 和非法序列行为，不能随数据库连接字符集变化。 |

## 实施顺序与验证

1. 扩展规则模型：描述 property getter、dispatch receiver、extension receiver、顶层函数、固定参数、可变参数和字段所在参数位置；普通运行时表达式必须继续作为参数，不得 SQL 化。
2. 先用一条完整垂直路径验证 `String.length` / `count()`、`replace`、`substring`、`take` / `takeLast`、`abs`、`sign`、`minOf` / `maxOf` 和 `BigDecimal` 对应成员。每条路径都要验证非空字段、字段参数、常量参数与安全调用。
3. 再加入位置查找、`CASE` 组合、空值谓词和 Boolean / 日期时间规则。`String.repeat` 在 Oracle/DM8 不能直接映射为 Kotlin 空串语义；除非引入明确的受限契约，否则不应开放原生 `.repeat()` 语法糖。
4. 将 Unicode、忽略大小写、文本解析、浮点特殊值、位运算、时区和 BLOB 比较作为逐方言能力，而不是把 Kotlin 函数名直接拼成 SQL。

每个新增形态都需要：精确 callable 的官方 compiler `testData`、每个声明支持方言的完整 SQL 断言，以及在语义依赖结果值时的真实数据库测试。本次为静态盘点；未运行测试。
