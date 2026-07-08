# Kotlin Compiler Plugin Troubleshooting

Use this table before changing implementation or golden files. First classify the failure by compiler layer, then inspect the smallest artifact for that layer.

## Symptom To Layer

| Symptom | Likely layer | First thing to inspect |
| --- | --- | --- |
| Plugin code never runs | Service registration or test configurator | Registrar service file, test plugin classpath, command-line processor |
| Custom option is always default | CLI or Gradle option wiring | Command-line processor option name, compiler configuration key, Gradle subplugin option |
| Generated function is unresolved in source | FIR declaration generation | `getCallableNamesForClass`, `generateFunctions`, annotation predicate, `.fir.txt` |
| Generated class or package is unresolved | FIR top-level generation/package existence | Generated class IDs, callable IDs, package-existence hook |
| Diagnostic is missing | FIR checker registration | FIR registrar, checker group, diagnostic container, declaration kind |
| Diagnostic reports at wrong location | FIR checker source selection | `reportOn` source element and declaration/expression checker kind |
| Annotation is found on some declarations but not others | Predicate or annotation ClassId | Registered predicate, fully qualified annotation class ID, meta-annotation behavior |
| Annotation arguments are empty or unresolved | FIR phase mismatch | Extension point phase and whether arguments are resolved yet |
| Supertypes are missing | FIR supertype generation | `needTransformSupertypes`, resolver usage, generated type refs |
| Visibility/modality change is ignored | FIR status transform | `needTransformStatus`, typed overload, forbidden class/typealias visibility changes |
| Generated declaration reaches FIR but has no runtime behavior | IR body generation | IR matcher, origin/name/owner checks, `function.body` assignment |
| Backend crashes after body generation | Invalid IR | Return type, receivers, value argument indices, type arguments, IR verifier |
| Box test compiles but returns wrong result | IR generation or runtime library | Generated body dump, symbol lookup, target runtime dependency |
| `.fir.txt` changed unexpectedly | Frontend behavior or Kotlin upgrade | Phase section that changed, generated declaration list, diagnostic list |
| `.fir.ir.txt` changed unexpectedly | FIR-to-IR or backend transformation | Generated bodies, origins, symbol names, lowered structure |

## Debugging Order

1. Reproduce with one test file and one declaration.
2. Decide whether the failure is plugin loading, frontend FIR, FIR-to-IR, backend IR, lowering, or runtime.
3. Inspect `.fir.txt` for all frontend questions.
4. Inspect `.fir.ir.txt` or IR dumps for backend questions.
5. Check registration only after confirming no extension output appears at all.
6. Update golden files only after identifying the exact phase and semantic reason for the diff.

## Plugin Does Not Load

Check:

- Compiler plugin registrar service metadata exists and names the correct class.
- Test infrastructure adds the compiler plugin artifact to the test compiler classpath.
- The registrar declares K2 support when testing K2/FIR.
- Command-line processor plugin ID matches the Gradle/test option prefix.
- Gradle plugin passes subplugin options to the same plugin ID.

Fast probe: add a tiny test-only option or diagnostic that must appear when the plugin loads. Remove ad hoc logging after fixing the test.

## Generated Declaration Is Unresolved

Do not touch IR first. Source resolution happens before backend.

Check:

- The user annotation artifact is on the test compile classpath.
- The annotation `ClassId` has the correct package and short name.
- The predicate is registered in `registerPredicates`.
- `getCallableNamesForClass` returns the generated name for the annotated owner.
- `generateFunctions` or `generateProperties` handles the same callable ID.
- The generated FIR declaration has a valid return type, receiver/owner, status, and plugin origin.

Expected progression:

1. Before FIR generation: unresolved call.
2. After FIR signature generation: call resolves, but backend may fail or body may be absent.
3. After IR body generation: runtime test passes.

## Diagnostic Is Missing

Check:

- The checker extension is registered from the FIR registrar.
- The checker is added to the correct checker group for the declaration, expression, or type.
- The custom diagnostic container and renderer are registered if the Kotlin version requires it.
- The checker filters by fully qualified annotation ID, not a simple name.
- The test expects the diagnostic at the element whose source is passed to `reportOn`.

If a checker needs expression types, make sure it runs after the necessary FIR phase. If it only needs declaration shape and annotations, keep it early and simple.

## IR Body Is Missing

Check:

- FIR actually generated the declaration; confirm in `.fir.txt`.
- The declaration appears in `.fir.ir.txt`.
- The IR matcher recognizes the generated declaration by origin, owner, annotation, or stable callable ID.
- The transformer visits the declaration. If using `transformChildrenVoid`, ensure the parent traversal reaches generated members.
- `body` is assigned on the returned/transformed `IrSimpleFunction`.

Avoid matching only `name == "hello"` in production. That can accidentally transform user functions with the same name.

## Invalid IR

Check:

- Return expression type equals the function return type.
- Dispatch receiver and extension receiver are assigned to the right fields.
- Value arguments use the Kotlin compiler version's current parameter indexing API.
- Generic type arguments are set when calling generic functions.
- Constructed classes use the correct constructor symbol and type arguments.
- Nullability matches the target type.

Useful reduction: write equivalent hand-authored Kotlin and compare its IR dump with the generated IR. If equivalent Kotlin is impossible, the generated IR is more likely to violate an invariant.

## Golden File Discipline

Golden files are compiler behavior contracts, not snapshots to accept blindly.

Before updating a golden:

- Identify the changed phase.
- Explain whether the change comes from plugin code, Kotlin compiler upgrade, or test source change.
- Confirm no unrelated generated declaration, diagnostic, status, or body changed.
- Keep the diff small by reducing noisy test source.

If the explanation is "the dump changed and tests say so", do not update the golden yet.
