{% import "../../../macros/macros-en.njk" as $ %}

## {{ $.title("No-value Behavior") }}

The no-value strategy handles condition arguments that are `null` or empty collections. When a comparison, like, in, between, or similar condition has no usable value, Kronos decides whether to ignore the condition, emit a true/false condition, or convert it to `IS NULL` / `IS NOT NULL`.

## Default Behavior

The default logic is:

- When the operation type is `UPDATE` or `DELETE`:
  - If the condition type is an equality judgment, it is converted to `is null` or `is not null`.
  - If the condition type is like, in, between, or numeric, the condition is recognized as the opposite of the current condition.
  - If the condition is greater than, greater than or equal to, less than, less than or equal to, the condition is recognized as false.
  - In other cases, the conditional statement is ignored.
- Ignore this conditional statement when the operation type is `SELECT`.

## Dynamically set the no-value strategy

Simply call the `ifNoValue` method in the conditional statement as shown below:

```kotlin
where { (it.age == null).ifNoValue(NoValueStrategyType.Ignore) }
where { (it.name like null).ifNoValue(NoValueStrategyType.False) }
```

## Types of value-free strategies

The currently supported value-free strategies are：

{{ $.params([
['Ignore', 'Ignore this conditional statement', "NoValueStrategyType", "ignore"],
['False', 'The conditional statement is false', "NoValueStrategyType", "false"],
['True', 'The conditional statement is true', "NoValueStrategyType", "true"],
['JudgeNull', 'Convert to `is null` or `is not null`', "NoValueStrategyType", "judgeNull"],
['Auto', 'Use Kronos default behavior, usually no need to set manually', "NoValueStrategyType", "auto"]
]) }}
