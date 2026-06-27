# Minimal Plugin Walkthrough

Use this walkthrough when the agent may know nothing about Kotlin compiler plugins. It shows the smallest useful vertical slice: an annotation marks a class, FIR exposes a generated member, IR fills the member body, diagnostics reject invalid use, and tests prove the whole plugin is loaded.

The example feature:

```kotlin
@GenerateHello
class User

fun box(): String = User().hello()
```

Expected behavior:

- `hello()` is visible to source resolution.
- `hello()` returns `"OK"` at runtime.
- `@GenerateHello` on a non-class declaration is a compiler error.

## 1. Create The User API

Put annotations in a small artifact that has no compiler dependencies:

```kotlin
package sample.api

@Target(AnnotationTarget.CLASS)
annotation class GenerateHello
```

Do not place compiler plugin classes in the same artifact. User source should import only stable API types.

## 2. Define The First Tests

Write tests before implementation. A minimal plugin should have one runtime test and one diagnostics test.

Box test:

```kotlin
// testData/box/generateHello.kt
import sample.api.GenerateHello

@GenerateHello
class User

fun box(): String = User().hello()
```

Diagnostics test:

```kotlin
// testData/diagnostics/generateHelloOnFunction.kt
import sample.api.GenerateHello

@GenerateHello
fun notAClass() {}
```

The box test should fail with unresolved `hello` before FIR declaration generation exists. The diagnostics test should fail by missing the plugin diagnostic before the checker exists. Keep this red state; it proves the tests can catch absent plugin behavior.

## 3. Wire Plugin Registration

Create the compiler plugin registrar and register only the extensions needed by the feature:

```kotlin
class SampleCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(SampleFirRegistrar())
        IrGenerationExtension.registerExtension(SampleIrGenerationExtension())
    }
}
```

Also register the command-line processor if the plugin has options. Even when no options exist, add a loading test that proves the registrar service is discoverable by the compiler test infrastructure.

Service registration depends on the target Kotlin version. Verify the expected `META-INF/services/...` file points at the compiler plugin registrar class. A missing service entry usually means no FIR checker, generated declaration, or IR extension will ever run.

## 4. Register FIR Extensions

Use a FIR registrar to install diagnostics and declaration generation:

```kotlin
class SampleFirRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::GenerateHelloCheckersExtension
        +::GenerateHelloDeclarations
    }
}
```

If custom diagnostics use a diagnostic container, register that container from the FIR registrar according to the Kotlin version used by the project.

## 5. Add The Diagnostic

The checker should reject `@GenerateHello` anywhere except regular classes. Keep the check small and location-specific:

```kotlin
object GenerateHelloDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val hasAnnotation = declaration.hasAnnotation(GenerateHelloClassId, context.session)
        if (!hasAnnotation) return
        if (declaration is FirRegularClass) return

        reporter.reportOn(
            declaration.source,
            SampleErrors.GENERATE_HELLO_ONLY_ON_CLASS,
            context
        )
    }
}
```

Adapt the exact base checker class to the Kotlin version and declaration kinds supported by the plugin. The important rule is stable: diagnostics belong in FIR, not in IR, because invalid source should fail before backend generation.

Add diagnostics expectations that prove:

- The diagnostic appears on the invalid declaration.
- A valid annotated class has no diagnostic.
- An unannotated declaration is ignored.

## 6. Generate The FIR Signature

Use `FirDeclarationGenerationExtension` because user source calls `User().hello()` and therefore the member must exist during frontend resolution.

The extension needs two operations:

1. Announce that annotated classes have a member named `hello`.
2. Generate the function symbol/signature when the compiler asks for that callable.

Shape:

```kotlin
class GenerateHelloDeclarations(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val predicate = DeclarationPredicate.create {
        annotated(GenerateHelloClassId)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
    ): Set<Name> {
        if (!classSymbol.hasGenerateHelloAnnotation()) return emptySet()
        return setOf(Name.identifier("hello"))
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        if (callableId.callableName.asString() != "hello") return emptyList()
        val owner = context?.owner ?: return emptyList()
        return listOf(buildHelloFunctionSymbol(owner, callableId))
    }
}
```

The generated function should be a member function with:

- Name `hello`.
- No value parameters.
- Return type `String`.
- Plugin declaration origin.
- No source element unless the Kotlin version requires a synthetic source.
- No body if IR will fill it later.

After this step, the box test should move from unresolved `hello` to either backend/runtime failure or missing body. Inspect `.fir.txt`: if `hello` is absent, fix FIR before touching IR.

## 7. Fill The IR Body

Use backend IR only after FIR has created the callable. Match the generated function by stable owner, name, origin, or annotation-derived metadata.

Shape:

```kotlin
class SampleIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                val result = super.visitSimpleFunction(declaration) as IrSimpleFunction
                if (result.name.asString() == "hello" && result.body == null) {
                    result.body = DeclarationIrBuilder(pluginContext, result.symbol).irBlockBody {
                        +irReturn(irString("OK"))
                    }
                }
                return result
            }
        })
    }
}
```

In production code, move matching and body construction into helpers. Do not match by simple name alone if user code can also define `hello`. Add owner, origin, annotation, or callable ID checks.

After this step, the box test should return `OK`. If it compiles but crashes in backend, inspect `.fir.ir.txt` or IR dumps and verify the return expression type is exactly `String`.

## 8. Add Golden Dumps

For this feature, keep:

- `.fir.txt`: proves the generated member exists during frontend resolution.
- `.fir.ir.txt`: proves FIR-generated declarations reach IR and the backend body appears.

Review generated dumps by phase. Do not update expected files until the diff can be explained in terms of FIR generation, FIR-to-IR conversion, IR body insertion, or a Kotlin compiler upgrade.

## 9. Add Options Only After The Slice Works

If the feature needs options, add them after the basic tests pass:

1. Define plugin option names in one place.
2. Parse CLI options in the command-line processor.
3. Store values in compiler configuration.
4. Pass values into FIR/IR extension constructors.
5. Add a test proving an option changes behavior.

Do not let option parsing obscure whether basic plugin loading and extension registration work.

## Done Criteria

The minimal plugin is credible when:

- A disabled plugin makes the box test fail.
- A valid annotated class can call the generated function.
- Invalid annotation placement emits the custom diagnostic.
- `.fir.txt` contains the generated signature.
- `.fir.ir.txt` or an IR dump contains the generated body.
- No runtime test is the only proof of frontend behavior.
