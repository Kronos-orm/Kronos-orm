# 设计锁

更新日期：2026-07-22

## 术语与边界

- “集合 `any`”指在 Kronos 条件 lambda 内使用 Kotlin 的 `Iterable<T>.any((T) -> Boolean)` 谓词重载。
- 它不是 SQL 的量化 `ANY`、不是 `KTableForCondition.any(query)`，也不是 PostgreSQL 的 `f.any(...)`。
- “子谓词”是针对一个运行时集合元素生成的 Kronos syntax 表达式。

## 已确认事实

- `KTableForCondition.andExpr` 与 `orExpr` 将 `SqlExpr?` 子节点折叠为 `SqlExpr.Binary` 树；`containsConditionExpr` 构造已转义、已参数化的 `SqlExpr.Like`。
- `SqlExpr.Binary` 与 `SqlBinaryOperator.And` / `Or` 足以表达所需树结构，`StandardSqlRenderer` 会按谓词优先级补括号。
- `ConditionAnalysis` 当前会降低逻辑运算和字符串匹配；不支持的方法会报告未识别条件函数，尚不识别 Kotlin 集合 `any`。
- 空 `orExpr` 返回 `null`，因此直接套用它会错误地移除空集合 `any` 的过滤条件。

## 公开语法

- 对用户保持 Kotlin 原生写法：

```kotlin
where {
    keywords.any { keyword ->
        it.name.contains(keyword)
    } || it.status == 1
}
```

- 整个 `any` 调用是一个普通条件表达式，可位于 `||`、`&&` 任意一侧，可被 `!` 包围，也可放入括号。
- 内层谓词可使用既有 Kronos 条件表达式，包含其自身的 `||` 与 `&&`。

## 实现原则

- 只构造 syntax AST 与绑定参数；集合谓词不拼接 SQL 文本。
- 复用 `SqlExpr.Like`、`SqlExpr.Binary`、`SqlExpr.BooleanLiteral` 与既有 condition builder；不新增公开的 `likeAny`、`anyOr` 或 raw-SQL API。
- 通过已解析的符号身份、receiver 形状和谓词签名识别 Kotlin stdlib 调用，不能只检查函数名 `any`。
- 子谓词构造与参数分配保持集合迭代顺序。
- 集合 receiver 只能求值一次，不能为 `loadKeywords()` 之类表达式分别做空检查与元素构造而重复调用。
- 保持 `orExpr(emptyList()) == null` 的既有契约，集合 `any` 的空值行为在自身 lowering 中处理。

## 必须满足的语义

- 非空集合的受支持子谓词降低为子谓词的 OR 树。
- 空集合降低为 `SqlExpr.BooleanLiteral(false)`。
- 对集合 `any` 取反必须与 `NOT (any 结果)` 语义等价，因此空集合取反后为 true。
- 单个子谓词现有的无值策略保持不变。非空集合中所有子谓词均为 `null` 时的行为必须先明确并有回归测试，才能宣布该特性完成。

## 禁止项

- 不使用 `.asSql()`、`.patch()`、字符串插值或参数化原生 SQL 实现。
- 不为解决空 `any` 改变通用逻辑聚合的既有语义。
- 不误改 `KTableForCondition.any(query)` 或 `FunctionHandler.any(...)`。
- 第一阶段不支持 `all`、`none`、无参 `any`、`Sequence.any`、数组重载、嵌套集合量词，或谓词内任意 Kotlin 业务布尔逻辑。

## 文档规则

- 示例使用 `collection.any { element -> ... }` 与普通 Kotlin `||`、`&&`、`!`、括号。
- 中英文条件页必须描述相同的支持范围与空集合行为。
- 不得暗示该 `any` 会产生原生 SQL，也不得把它写成 SQL 量化 `ANY`。
