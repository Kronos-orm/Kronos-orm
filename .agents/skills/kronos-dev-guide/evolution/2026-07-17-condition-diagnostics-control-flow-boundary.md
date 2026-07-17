# Condition Diagnostics Must Respect Kotlin Control Flow

## Symptom

A FIR checker for unregistered KPojo sources can correctly reject `it.id == probe.id` but also incorrectly reject existing dynamic conditions such as:

```kotlin
where { (it.id == 1).takeIf(probe.id != null) }
where { if (probe.id != null) it.id == 1 else false.asSql() }
```

## Cause

Walking every property access in a condition lambda conflates two different roles:

- SQL operands transformed into columns or parameters;
- ordinary Kotlin control flow that decides which SQL-expression branch is used.

`takeIf`/`takeUnless` Boolean arguments and `if`/`when` conditions remain runtime Kotlin expressions. They are not rendered as SQL.

## Fix

Traverse the SQL-producing receiver or branch results, but skip dynamic gate arguments, `when` subjects, and branch conditions. Continue checking property accesses inside the SQL-expression branches.

For example, only the second `probe.id` below is an invalid SQL source:

```kotlin
where { if (probe.id != null) it.id == probe.id else it.id > 0 }
```

## Prevention

Condition diagnostics need paired compiler tests:

- positive cases that read KPojo properties in `takeIf`, `takeUnless`, `if`, and `when` control conditions;
- negative cases where the same control structures return a SQL branch containing an unregistered KPojo property;
- existing core tests compiled with the plugin, because they contain real dynamic-condition usage.
