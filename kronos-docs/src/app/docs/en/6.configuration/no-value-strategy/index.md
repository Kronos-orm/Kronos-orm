{% import "../../../macros/macros-en.njk" as $ %}

## {{ $.title("No-value Behavior") }}

The no-value strategy handles dynamic condition arguments that are `null` or empty collections. Literal `field == null` and `field != null` in a condition lambda mean SQL `IS NULL` and `IS NOT NULL`.

## Default Behavior

For dynamic condition values that enter the no-value strategy, the default logic is:

- When the operation type is `UPDATE` or `DELETE`:
  - If the condition type is an equality judgment, it is converted to `is null` or `is not null`.
  - If the condition type is like, in, between, or numeric, the condition is recognized as the opposite of the current condition.
  - If the condition is greater than, greater than or equal to, less than, less than or equal to, the condition is recognized as false.
  - In other cases, the conditional statement is ignored.
- Ignore this conditional statement when the operation type is `SELECT`.

## Dynamically set the no-value strategy

Simply call the `ifNoValue` method in the conditional statement as shown below:

```kotlin
val age: Int? = null
val namePattern: String? = null

where { (it.age == age).ifNoValue(NoValueStrategyType.Ignore) }
where { (it.name like namePattern).ifNoValue(NoValueStrategyType.False) }
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
