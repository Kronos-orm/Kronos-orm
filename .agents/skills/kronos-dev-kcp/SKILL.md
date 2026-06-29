---
name: kronos-dev-kcp
description: Kotlin compiler plugin programming guide focused on K2 FIR/frontend plugins, IR/backend plugins, extension registration, diagnostics, declaration generation, compiler-plugin testing, FIR predicate APIs, and Kotlin compiler API compatibility. Use when designing or modifying Kotlin compiler plugins, adding FIR checkers, generated declarations, supertypes, status transforms, metadata serializers, IR generation extensions, plugin options, Gradle plugin wiring, or testData-based compiler plugin tests.
---

# Kronos Dev KCP

Use this skill for Kotlin compiler plugin (KCP) programming. The guidance is about Kotlin's own compiler plugin model, especially K2 FIR/frontend plugin APIs, backend IR extensions, registration, diagnostics, generated declarations, and compiler-plugin tests.

## Required Reading

- For every compiler-plugin error, backend crash, invalid IR, or unexpected generated-declaration behavior, read [Evolution.md](Evolution.md) before changing code. Reuse any matching solution or prevention rule, and append newly verified pitfalls after a successful fix.
- Start with [references/build-a-plugin.md](references/build-a-plugin.md) when implementing a plugin or when the task is broad.
- If the agent is new to Kotlin compiler plugins, read [references/minimal-plugin-walkthrough.md](references/minimal-plugin-walkthrough.md) before writing production code.
- Read [references/kcp-overview.md](references/kcp-overview.md) before choosing frontend vs backend architecture.
- Read [references/fir-plugin-api.md](references/fir-plugin-api.md) before implementing FIR extensions, checkers, predicates, generated declarations, status transforms, or supertypes.
- Read [references/fir-cookbook.md](references/fir-cookbook.md) for concrete FIR checker, diagnostic, declaration-generation, supertype, status, and session-component patterns.
- Read [references/ir-cookbook.md](references/ir-cookbook.md) for concrete IR body-generation and transformation patterns.
- Read [references/testing.md](references/testing.md) before adding or changing compiler plugin tests.
- Read [references/troubleshooting.md](references/troubleshooting.md) when a plugin compiles but does not load, generated declarations are unresolved, diagnostics are missing, IR bodies are absent, or golden files change unexpectedly.

## Decision Rules

Prefer the narrowest compiler extension that can solve the problem:

- Use FIR/frontend logic for diagnostics, synthetic declarations, symbol/status changes, supertypes, and anything that must be visible to resolution or the IDE.
- Use IR/backend logic for executable bodies, lambda body rewrites, runtime object construction, and declaration body generation after frontend has established valid symbols and types.
- Use both when the frontend needs to expose or validate declarations and the backend needs to fill bodies or change generated code.
- Do not use backend IR to hide invalid references, unresolved symbols, broken types, or scope violations that frontend would reject.
- Consider KSP or a linter before writing a compiler plugin if the task does not require compiler resolution or code generation.
- Re-check plugin code after Kotlin upgrades. Treat every Kotlin compiler version bump as potentially breaking for plugin APIs.

Use FIR when:

- An API should appear as a real declaration during resolution.
- Invalid source should fail as a compiler diagnostic before backend transformation.
- IDE highlighting/completion must understand generated declarations.
- A class needs generated supertypes, declaration status changes, metadata, type attributes, or custom checkers.
- A plugin needs fast lookup over annotated declarations through FIR predicates.

Stay in IR when:

- The source already type-checks and only needs transformed executable behavior.
- The task is to add function bodies to frontend-generated declarations.
- The task changes binary output, lowers code, or rewrites existing declaration bodies.
- The feature can tolerate not being visible during source resolution.

## Implementation Workflow

1. Define the source-level behavior: annotation, declaration, expression, type, diagnostic, or generated member.
2. Choose FIR, IR, or both using the decision rules above.
3. Create or locate the annotation/API surface that user code imports.
4. Register extension points through the compiler plugin registrar and service metadata expected by the target Kotlin version.
5. Keep compiler-version-sensitive APIs isolated behind small classes or adapters.
6. Add compiler-plugin tests before relying on runtime tests.
7. For backend work, enable IR verification or dump IR when transformations are non-trivial.
8. For frontend work, test FIR dumps and diagnostics, not only successful compilation.

For a first implementation, build a narrow vertical slice before broadening the feature:

1. Make a plugin-loading test fail when the plugin is disabled.
2. Add the smallest user annotation or marker API.
3. Add one invalid-use diagnostics test.
4. Add one generated-declaration box test.
5. Add only enough FIR and IR code to turn those tests green.
6. Review FIR and FIR+IR dumps before expanding to options, more declaration kinds, or Gradle wiring.

## Implementation Guidance

- Keep frontend and backend responsibilities explicit: frontend creates and validates model shape; backend fills or rewrites executable implementation.
- Register FIR extensions through `FirExtensionRegistrar.configurePlugin` and its unary-plus DSL.
- Register additional diagnostics containers from the FIR registrar when custom diagnostics are used.
- Register predicates explicitly in `FirExtension.registerPredicates` before querying `FirPredicateBasedProvider`.
- Generated FIR declarations must be fully resolved enough for their phase and should use `FirDeclarationOrigin.Plugin`.
- Keep generated declaration computations side-effect-free; the IDE can retry cached computations.
- Use backend IR to fill bodies for generated declarations when FIR only creates signatures.
- Do not mutate generated FIR symbols outside the expected extension contract.
- Avoid relying on annotation arguments before the FIR phase where they are resolved.
- Prefer small extension classes with one responsibility. A plugin with checkers, generated declarations, and IR bodies should have separate classes for each.
- Name compiler-generated declarations deterministically. Tests and backend IR lookup depend on stable names.
- Keep user-facing annotations in a lightweight annotation artifact, separate from compiler internals.

## Validation

Use compiler-plugin tests for compiler behavior, not only runtime tests. When a change affects generated members, diagnostics, FIR predicates, FIR/IR dumps, or plugin options, include at least one test that would fail if the plugin did not run.

Prefer Kotlin compiler plugin template style tests:

- Put sources under `compiler-plugin/testData`.
- Use `testData/box` for code generation/runtime behavior.
- Use `testData/diagnostics` for frontend diagnostics.
- Review generated FIR and FIR+IR dump files as golden files.
- Update golden files only after checking diffs are intentional.

When tests fail:

1. Identify whether failure happens in frontend resolution, FIR extension execution, FIR-to-IR conversion, backend IR generation, lowering, or runtime.
2. Compare the failing Kotlin compiler version and API surface against the code being touched.
3. Inspect FIR/IR dumps or diagnostics before changing expected output.
4. Re-run the smallest compiler-plugin test first, then broader Gradle checks.

## Kronos Official Compiler TestData Style

Write Kronos compiler-plugin behavior tests under `kronos-compiler-plugin/testData`. These tests must prove behavior through Kotlin's real FIR/IR pipeline. Use them for KPojo generated declarations and bodies, DSL lambda transformations, FIR/frontend declarations or diagnostics, IR verifier regressions, and runtime behavior that depends on generated code.

Do not use official `testData` for pure utilities, broad smoke tests with no compiler-plugin contract, or database integration requiring external services.

Use this layout:

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

Each directory must have a matching thin JUnit suite under `kronos-compiler-plugin/src/test/kotlin/com/kotlinorm/compiler`. For example, `testData/box/select/collectionLiteralFields.kt` should be run by `SelectBoxTest.kt`.

Name files after the exact behavior under test. Prefer `collectionLiteralFields.kt`, `functionFieldRequiresAlias.kt`, `noArgEqUsesCurrentValue.kt`, `setPlusMinusAssignments.kt`, `referenceCollectionLiteralProperties.kt`, and `typedProjectionKeepsSourceReceiver.kt`. Avoid vague names such as `advancedOperators.kt`, `diverseAssignments.kt`, `projectionFields.kt`, `test1.kt`, or `smoke.kt` unless the file is a true integration test and its top comment says so.

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

After the header, add a short test-purpose comment, for example:

```kotlin
// Verifies that collection-literal select fields preserve field order and aliases.
```

Keep one `box` file focused on one primary contract. Good examples are `condition/noArgEqUsesCurrentValue.kt`, `condition/takeIfFalseDropsCriteria.kt`, and `condition/deMorganNegatedAnd.kt`. Integration tests may combine behaviors, but their filename and top comment must say that explicitly.

Every box test must expose `fun box(): String`, return exactly `"OK"` on success, and return `"Fail: <specific reason>"` on failure. Failure messages must include observed values when useful:

```kotlin
fun box(): String {
    val result = ...

    if (result.size != 2) return "Fail: size was ${result.size}"
    if (result[0].name != "id") return "Fail: first field was ${result[0].name}"

    return "OK"
}
```

For multiple checks, a local `expect` helper is acceptable:

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

Do not mix assertion styles in one file unless there is a strong reason. Avoid copy-pasting large helpers. Use a small local helper when it is only useful in that file, multi-file testData support for scenario-local support sources, or shared test runtime support only for framework-level mechanics.

Call `Kronos.init` only when the test depends on naming strategies, initialization hooks, or `kClassCreator`. If initialization is required, keep it minimal:

```kotlin
Kronos.init {
    fieldNamingStrategy = lineHumpNamingStrategy
    tableNamingStrategy = lineHumpNamingStrategy
}
```

Positive box tests must assert the plugin-generated result, not merely compilation. This is weak:

```kotlin
fun box(): String {
    User().select { [it.id] }
    return "OK"
}
```

This is strong:

```kotlin
fun box(): String {
    val fields = User().collectSelect { [it.id] }
    if (fields.single().name != "id") return "Fail: field was ${fields.single().name}"
    return "OK"
}
```

Use `testData/diagnostics` for cases that should fail compilation, such as `diagnostics/projection/functionFieldRequiresAlias.kt`, `diagnostics/projection/duplicateProjectionProperty.kt`, or `diagnostics/condition/unsupportedDynamicExpression.kt`. Do not encode expected compiler failures as box tests.

Use `.fir.txt` or `.fir.ir.txt` golden dumps only for selected structural contracts: FIR-generated declarations, FIR call return type refinement, complex IR body generation, and invalid-IR regressions. Do not add broad golden dumps for every box test.

Any test using `[]` collection literals must rely on official test configuration enabling `+CollectionLiterals`. Do not assume Gradle's `compileTestKotlin` arguments apply to `testData` compilation. If a test requires a language flag not enabled globally, document it with a file-level comment or directive.

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
