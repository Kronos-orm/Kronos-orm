{% import "../../../macros/macros-zh-CN.njk" as $ %}

## {{ $.title("无值处理") }}

无值策略用于处理条件参数为 `null` 或空集合的场景。当条件 DSL 中的比较、like、in、between 等表达式没有可用值时，Kronos 会根据当前操作类型和条件类型决定是忽略条件、生成恒真/恒假条件，还是转换为 `IS NULL` / `IS NOT NULL`。

## 默认行为

默认逻辑如下：

- 当操作类型为`UPDATE`或`DELETE`时：
    - 如果条件类型为相等判断，则转换为`is null`或`is not null`
    - 如果条件类型为like、in、between或数值判断，则直接认定条件为当前条件的相反值
    - 如果条件为大于、大于等于、小于、小于等于，则认定条件为false
    - 其他情况忽略该条件语句
- 当操作类型为`SELECT`时，忽略该条件语句

## 动态设置无值策略

只需在条件语句中调用`ifNoValue`方法即可，如下所示：

```kotlin
where { (it.age == null).ifNoValue(NoValueStrategyType.Ignore) }
where { (it.name like null).ifNoValue(NoValueStrategyType.False) }
```

## 无值策略的类型

目前支持的无值策略有：

{{ $.params([
['Ignore', '忽略该条件语句', "NoValueStrategyType", "ignore"],
['False', '条件语句为false', "NoValueStrategyType", "false"],
['True', '条件语句为true', "NoValueStrategyType", "true"],
['JudgeNull', '转换为`is null`或`is not null`', "NoValueStrategyType", "judgeNull"],
['Auto', '使用 Kronos 默认策略，通常不需要手动设置', "NoValueStrategyType", "auto"]
]) }}
