# Kronos Official Compiler TestData Style Guide

This guide defines how to write and review tests under `kronos-compiler-plugin/testData`.
The goal is to make official compiler tests precise enough to replace kctfork tests over time, without turning `testData` into vague smoke coverage.

## Purpose

Official compiler tests must prove compiler-plugin behavior through Kotlin's real FIR/IR pipeline.

Use official `testData` for:

- KPojo generated declarations and generated bodies.
- DSL lambda transformations.
- FIR/frontend generated declarations or diagnostics.
- IR verifier regressions.
- Runtime behavior that depends on compiler-generated code.

Do not use official `testData` for:

- Pure utility functions that do not require compiler execution.
- Broad smoke tests that do not assert a compiler-plugin contract.
- Database integration that requires external services.

## File Layout

Use module-local test data:

```text
kronos-compiler-plugin/testData/
  box/
    pluginLoading/
    kpojoGeneratedBodies/
    kpojoFieldMetadata/
    condition/
    select/
    set/
    sort/
    reference/
    dslIntegration/
    typeParameterFixer/
    kclassMap/
  diagnostics/
```

Each directory must have a matching JUnit suite class under:

```text
kronos-compiler-plugin/src/test/kotlin/com/kotlinorm/compiler/official/
```

Example:

```text
testData/box/select/collectionLiteralFields.kt
src/test/.../official/SelectBoxTest.kt
```

## Naming

File names must describe the exact behavior under test.

Prefer:

```text
collectionLiteralFields.kt
functionFieldRequiresAlias.kt
noArgEqUsesCurrentValue.kt
setPlusMinusAssignments.kt
referenceCollectionLiteralProperties.kt
typedProjectionKeepsSourceReceiver.kt
```

Avoid:

```text
advancedOperators.kt
diverseAssignments.kt
projectionFields.kt
test1.kt
smoke.kt
```

Use broad names only for true integration tests, such as:

```text
selectClauseStatement.kt
multiFileKPojo.kt
```

## File Header

Every `.kt` testData file must start with the Kronos Apache 2.0 copyright header:

```kotlin
/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

After the header, add a short test-purpose comment:

```kotlin
// Verifies that collection-literal select fields preserve field order and aliases.
```

## Test Scope

One `box` file should test one primary contract.

Good:

```text
condition/noArgEqUsesCurrentValue.kt
condition/takeIfFalseDropsCriteria.kt
condition/deMorganNegatedAnd.kt
```

Poor:

```text
condition/advancedOperators.kt
```

Integration tests may combine several behaviors, but the filename and top comment must say that explicitly.

## `box()` Contract

Every box test must expose:

```kotlin
fun box(): String
```

Return exactly:

```kotlin
"OK"
```

on success.

On failure, return:

```kotlin
"Fail: <specific reason>"
```

Failure messages must include the observed value when useful:

```kotlin
return "Fail: expected LIKE but was ${criteria.type}"
```

Do not return vague failures:

```kotlin
return "Fail"
return "wrong"
```

## Assertion Style

Use one consistent style per file.

Preferred for short tests:

```kotlin
fun box(): String {
    val result = ...

    if (result.size != 2) return "Fail: size was ${result.size}"
    if (result[0].name != "id") return "Fail: first field was ${result[0].name}"

    return "OK"
}
```

Preferred for multiple checks:

```kotlin
fun box(): String {
    val failures = listOfNotNull(
        expect(actual.size == 2) { "size was ${actual.size}" },
        expect(actual[0].name == "id") { "first field was ${actual[0].name}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
```

Do not mix multiple assertion styles in the same file unless there is a strong reason.

## Helpers

Avoid copy-pasting large helpers across many files.

Use one of these options:

1. Small local helper in the same file when it is only useful there.
2. Multi-file testData support:

```kotlin
// FILE: support.kt
...

// FILE: test.kt
...
```

3. A shared test support source in the official test runtime classpath, if the helper is framework-level and not part of the compiled user scenario.

Use local helpers for domain-specific assertions:

```kotlin
fun assertCondition(...): String?
```

Use shared helpers for generic test mechanics:

```kotlin
expect(...)
```

## `Kronos.init`

Only call `Kronos.init` when the test depends on naming strategies, initialization hooks, or `kClassCreator`.

If the test does not depend on initialization, omit it.

If initialization is required, keep it minimal:

```kotlin
Kronos.init {
    fieldNamingStrategy = lineHumpNamingStrategy
    tableNamingStrategy = lineHumpNamingStrategy
}
```

## Positive Tests

A positive box test must assert the plugin-generated result, not only that compilation succeeded.

Weak:

```kotlin
fun box(): String {
    User().select { [it.id] }
    return "OK"
}
```

Strong:

```kotlin
fun box(): String {
    val fields = User().collectSelect { [it.id] }
    if (fields.single().name != "id") return "Fail: field was ${fields.single().name}"
    return "OK"
}
```

## Negative Tests

Use `testData/diagnostics` for cases that should fail compilation.

Examples:

```text
diagnostics/projection/functionFieldRequiresAlias.kt
diagnostics/projection/duplicateProjectionProperty.kt
diagnostics/condition/unsupportedDynamicExpression.kt
```

Do not encode expected compiler failures as box tests.

## Golden Dumps

Use `.fir.txt` or `.fir.ir.txt` only for selected structural contracts:

- FIR-generated declarations.
- FIR call return type refinement.
- Complex IR body generation.
- Regression tests for invalid IR shape.

Do not add broad golden dumps for every box test. Golden files are expensive to maintain across Kotlin upgrades.

## Collection Literal Tests

Any test using `[]` collection literals must rely on official test configuration enabling:

```text
+CollectionLiterals
```

Do not assume Gradle's `compileTestKotlin` arguments apply to `testData` compilation.

If a test requires a language flag not enabled globally, document it with a file-level comment or directive.

## Replacement Criteria For kctfork Tests

An official testData file may replace an old kctfork test only when all of these are true:

- It asserts the same compiler-plugin contract.
- It runs through the official FIR/IR pipeline.
- It fails if the relevant plugin transformer or generator is disabled.
- It checks the same important values, not merely compilation.
- It covers the same edge case or an explicitly documented stronger one.

Do not remove kctfork tests for a feature area until a migration table marks every old behavior as:

```text
covered
covered by stronger official test
intentionally kept as unit test
obsolete
```

## Migration Table Format

When migrating a suite, add or update a mapping table near the official test suite or migration plan:

```markdown
| Old test | Official testData | Status | Notes |
| --- | --- | --- | --- |
| ConditionAnalysisTest.test no-arg eq uses object value | condition/noArgEqUsesCurrentValue.kt | covered | Same value and type assertions |
| FieldAnalysisTest.test plus expression analysis | select/collectionLiteralFields.kt | obsolete | `+` no longer means field projection |
```

## Review Checklist

Before accepting new testData, verify:

- The file has the Kronos copyright header.
- The top comment states the tested contract.
- The filename names one specific behavior.
- `box()` returns `OK` only after meaningful assertions.
- Failure messages include observed values.
- Helpers are not duplicated excessively.
- No external database or environment is required.
- Collection literals are supported by test configuration.
- The test would fail if the relevant plugin behavior broke.
- The old kctfork coverage status is updated when this test replaces one.
