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

## Control dynamic no-value predicates

Use Kotlin conditions to decide which predicate participates. Use an ordinary `if`/`else` when a missing value needs an explicit fallback; `.takeIf(...)` remains available when a predicate only needs to be omitted.

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

## No-value outcomes

Kronos uses these outcomes internally when it handles no-value conditions:

{{ $.params([
['Ignore', 'Ignore this conditional statement', "Outcome", "ignore"],
['False', 'The conditional statement is false', "Outcome", "false"],
['True', 'The conditional statement is true', "Outcome", "true"],
['JudgeNull', 'Convert to `is null` or `is not null`', "Outcome", "judgeNull"],
['Auto', 'Use Kronos default behavior', "Outcome", "auto"]
]) }}
