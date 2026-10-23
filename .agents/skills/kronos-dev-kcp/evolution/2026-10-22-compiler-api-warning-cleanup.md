# Use annotation and declaration finder APIs while preserving generated ownership

Kotlin 2.4.21 introduces `IrAnnotation.classSymbol` and deprecates accessing an annotation's constructor `symbol`. Match annotations through the official `IrAnnotation.isAnnotation(FqName)` helper; it handles lazy/unbound annotations as well. Keep annotation argument extraction and repeatable-annotation filtering intact.

Use `IrPluginContext.finderForBuiltins()` for the plugin's required runtime helper declarations, and `finderForSource(fromFile)` for model and field types reached from a source file. The latter records incremental source dependencies. Class-ID lookup rejects local IDs; a narrow `ClassIdBasedLocality` opt-in is appropriate for that lookup guard, rather than treating the ID flag as a general semantic locality test.

Declaration-container reads and mutations during IR generation need a narrow `UnsafeDuringIrConstructionAPI` opt-in on the responsible functions. Remove redundant exhaustive `when` branches and obsolete invisible-reference suppressions where compilation proves the APIs are public.

The dynamic projection generator still needs Kotlin FIR's internal `ownerGenerator`. The stock `FirExtensionDeclarationsSymbolProvider` sets it when generating known classes, while its top-level class-ID lists are cached. Kronos binds projection classes discovered during call refinement before that stock lookup and must retain their generator ownership for generated member scopes. Removing the assignment or disguising a generated class as a source class breaks that contract. Keep this single existing invisible-reference suppression until the compiler provides a public dynamic-generation/ownership path; keep the actual editor and projection compiler tests.

Source reference: the official `org.jetbrains.kotlin:kotlin-compiler:2.4.21:sources` artifact, especially `IrAnnotation.kt`, `AdditionalIrUtils.kt`, `IrPluginContext.kt`, `FirExtensionDeclarationsSymbolProvider.kt` and `FirGeneratedScopes.kt`. Compilation after these changes reports only the retained generator-ownership warning in the compiler-plugin module.

Validation: all 358 compiler-plugin tests and all 8 Native build-plugin tests passed without skipped or failed cases; changed Kotlin files passed detekt. Evidence: `/tmp/kronos-warning-api-tests.log`, `/tmp/kronos-warning-api-bootstrap.log`, `/tmp/kronos-warning-api-detekt.log`.
