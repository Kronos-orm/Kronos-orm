{% import "../../../macros/macros-zh-CN.njk" as $ %}

## {{ $.title("无值处理") }}

无值策略用于处理动态条件参数为 `null` 或空集合的场景。条件 lambda 中字面量 `field == null` 和 `field != null` 表示 SQL `IS NULL` 和 `IS NOT NULL`。

## 默认行为

动态条件值进入无值策略后，默认逻辑如下：

- 当操作类型为`UPDATE`或`DELETE`时：
    - 如果条件类型为相等判断，则转换为`is null`或`is not null`
    - 如果条件类型为like、in、between或数值判断，则直接认定条件为当前条件的相反值
    - 如果条件为大于、大于等于、小于、小于等于，则认定条件为false
    - 其他情况忽略该条件语句
- 当操作类型为`SELECT`时，忽略该条件语句

## 控制动态无值条件

使用 Kotlin 条件决定动态谓词是否参与查询。缺失值需要显式 fallback 时使用普通 `if`/`else`；只需跳过条件时仍可使用 `.takeIf(...)`。

```kotlin
val age: Int? = null
val namePattern: String? = null

where { (it.age == age).takeIf(age != null) }
where {
    if (namePattern != null) {
        it.name like namePattern
    } else {
        false.asSql()
    }
}
where { it.age.isNull.takeIf(age == null) }
```

## 无值处理结果

Kronos 内部处理无值条件时会使用以下结果：

{{ $.params([
['Ignore', '忽略该条件语句', "处理结果", "ignore"],
['False', '条件语句为 false', "处理结果", "false"],
['True', '条件语句为 true', "处理结果", "true"],
['JudgeNull', '转换为 `is null` 或 `is not null`', "处理结果", "judgeNull"],
['Auto', '使用 Kronos 默认策略', "处理结果", "auto"]
]) }}
