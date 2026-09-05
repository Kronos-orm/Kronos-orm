# Build A Kotlin Compiler Plugin

Use this as the first reference when an agent needs to implement a Kotlin compiler plugin and may not know the shape of one.

## Goal

A robust Kotlin compiler plugin usually has four layers:

1. User API: annotations, marker interfaces, or DSL types imported by user code.
2. Frontend/FIR: diagnostics, generated declarations, generated supertypes, status changes, type attributes, metadata, and symbol visibility.
3. Backend/IR: generated bodies or executable transformations.
4. Build/test integration: Gradle plugin, compiler options, service registration, and compiler test data.

Do not start with IR visitors. First decide what user code should look like and whether the source should type-check without plugin-generated declarations.

## Minimal Project Shape

Use this layout unless the repository already has a local convention:

```text
plugin-annotations/
  src/main/kotlin/com/example/plugin/Annotations.kt

compiler-plugin/
  src/main/kotlin/com/example/plugin/cli/MyCommandLineProcessor.kt
  src/main/kotlin/com/example/plugin/cli/MyCompilerPluginRegistrar.kt
  src/main/kotlin/com/example/plugin/fir/MyFirRegistrar.kt
  src/main/kotlin/com/example/plugin/fir/MyDiagnostics.kt
  src/main/kotlin/com/example/plugin/fir/MyCheckers.kt
  src/main/kotlin/com/example/plugin/fir/MyDeclarationGeneration.kt
  src/main/kotlin/com/example/plugin/ir/MyIrGenerationExtension.kt
  src/main/resources/META-INF/services/...
  testData/box/...
  testData/diagnostics/...

gradle-plugin/
  src/main/kotlin/com/example/plugin/gradle/MyGradlePlugin.kt
```

Keep annotations in `plugin-annotations`. User projects should not need compiler internals on their compile classpath.

## First Design Pass

Answer these before writing code:

- What syntax does the user write?
- Should generated declarations be visible to user code?
- Should invalid usage fail during compilation?
- Is generated behavior target-specific?
- Does the IDE need to understand the generated API?
- Which Kotlin compiler version is targeted?
- What test proves the plugin is active?

Decision:

- If generated declarations must be visible, use FIR declaration generation.
- If invalid usage needs diagnostics, use FIR checkers.
- If only bytecode/body behavior changes, use IR.
- If generated signatures need bodies, use FIR for signatures and IR for bodies.

## Minimal Registration Flow

1. Implement a command-line processor for plugin options.
2. Implement compiler plugin registrar.
3. Register FIR registrar if frontend extensions are needed.
4. Register IR generation extension if backend output is needed.
5. Add service metadata expected by the target Kotlin compiler version.
6. Add Gradle plugin integration that passes options and adds annotation dependencies.

Keep option names stable and documented. Tests should cover at least one option round-trip if options affect behavior.

## Common Implementation Order

For a feature that generates API and implementation:

1. Add user annotation.
2. Add diagnostic tests for invalid annotation usage.
3. Add FIR checker and diagnostics.
4. Add FIR declaration generation for the generated signatures.
5. Add a box test that calls the generated declaration.
6. Add IR body generation for the generated declaration.
7. Review `.fir.txt` and `.fir.ir.txt`.

For a feature that only validates source:

1. Add user annotation or DSL marker.
2. Add failing diagnostics test first.
3. Add FIR checker.
4. Add passing diagnostics test.
5. Add phase-specific tests if annotation arguments, supertypes, or body types are involved.

For a feature that only changes bytecode:

1. Add a box test describing runtime behavior.
2. Add IR extension.
3. Enable IR verification/dumps for debugging.
4. Add a negative test if malformed source should still be rejected by normal Kotlin.

## Anti-Patterns

- Generating invisible declarations only in IR and then expecting user source to reference them.
- Parsing strings from source text when FIR/IR already exposes structured declarations.
- Mutating FIR outside extension contracts.
- Depending on annotation arguments before the phase that resolves them.
- Using global mutable caches in FIR extensions.
- Updating golden dump files without reading phase-level differences.
- Putting compiler internals in user annotation artifacts.
