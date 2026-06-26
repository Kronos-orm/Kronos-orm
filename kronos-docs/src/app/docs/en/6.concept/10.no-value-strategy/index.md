{% import "../../../macros/macros-zh-CN.njk" as $ %}

## {{ $.title("NoValueStrategy") }} No-value strategy

The no-value strategy is a strategy in the query conditional statement Criteria, when the conditional statement is a binary operator (i.e., it can take variable arguments), if the argument is `null`, then the no-value strategy will be used to generate the SQL statement.

## Customizing the global default no-value strategy

The default no-value strategy for `Kronos` is `DefaultNoValueStrategy`, which has the following main logic:

- When the operation type is `UPDATE` or `DELETE`:
  - If the condition type is an equality judgment, it is converted to `is null` or `is not null`.
  - If the condition type is like, in, between, or numeric, the condition is recognized as the opposite of the current condition.
  - If the condition is greater than, greater than or equal to, less than, less than or equal to, the condition is recognized as false.
  - In other cases, the conditional statement is ignored.
- Ignore this conditional statement when the operation type is `SELECT`.

To create a custom no-value strategy, simply implement the `NoValueStrategy` interface as shown below:

```kotlin group="custom" name="YourCustomNoValueStrategy.kt"
object YourCustomNoValueStrategy : NoValueStrategy {
    fun ifNoValue(kOperateType: KOperationType, criteria: Criteria): NoValueStrategyType {
        // your logic
    }
}
```

```kotlin group="custom" name="Main.kt"
Kronos.noValueStrategy = YourCustomNoValueStrategy
```

## Dynamically set the no-value strategy

Simply call the `ifNoValue` method in the conditional statement as shown below:

```kotlin
where { (it.age == null).ifNoValue(Ignore) }
```

## Types of value-free strategies

The currently supported value-free strategies are：

{{ $.params([
['Ignore', 'Ignore this conditional statement', "NoValueStrategyType", "ignore"],
['False', 'The conditional statement is false', "NoValueStrategyType", "false"],
['True', 'The conditional statement is true', "NoValueStrategyType", "true"],
['JudgeNull', 'Convert to `is null` or `is not null`', "NoValueStrategyType", "judgeNull"],
['Auto', 'The default strategy, generates SQL statements based on the global default strategy, usually no need to set manually', "NoValueStrategyType", "auto"]
]) }}