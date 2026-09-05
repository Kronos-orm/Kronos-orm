# FIR Cookbook

Concrete patterns for writing FIR/frontend compiler plugin code.

## Add A Diagnostic Checker

Use this when invalid source should produce a compiler error or warning.

Steps:

1. Define a diagnostic factory and renderer.
2. Register the diagnostic container from the FIR registrar.
3. Implement a checker under `FirAdditionalCheckersExtension`.
4. Add the checker to the relevant checker group.
5. Add diagnostics test data for both invalid and valid source.

Shape:

```kotlin
class MyCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers: Set<FirRegularClassChecker> =
            setOf(MyClassChecker)
    }
}

object MyClassChecker : FirRegularClassChecker() {
    override fun check(
        declaration: FirRegularClass,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (!shouldReport(declaration, context)) return
        reporter.reportOn(declaration.source, MyErrors.INVALID_USAGE, context)
    }
}
```

Checker selection:

- Declaration checker: classes, functions, properties, constructors, annotations.
- Expression checker: calls, assignments, returns, lambdas, when/if, qualified access.
- Type checker: type references and type usage.
- Language-version checker: diagnostics depending on language settings.

Do not perform expensive full-project scans in every checker invocation. Use predicates or a session component to cache discovery.

## Use Predicate-Based Discovery

Use predicates to find annotated declarations efficiently.

Pattern:

```kotlin
class MyExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val myAnnotated = DeclarationPredicate.create {
        annotated(MyAnnotations.MyAnnotation)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(myAnnotated)
    }
}
```

Then query the predicate provider for symbols matching the predicate.

Rules:

- Register before querying.
- Use plugin-owned top-level annotations.
- Avoid annotation argument logic until the phase supports it.

## Generate A Member Function Signature

Use `FirDeclarationGenerationExtension` when user code should call a generated member.

Steps:

1. Decide owning class and generated function name.
2. Return that name from `getCallableNamesForClass`.
3. Generate function declarations in `generateFunctions`.
4. Use plugin declaration origin.
5. Leave the body absent or simple if backend IR will fill it.
6. Add a box test that calls the generated function.

Important contract: generation methods should be side-effect-free. Compute the same result from the same inputs.

Conceptual shape:

```kotlin
class MyDeclarationGeneration(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
    ): Set<Name> {
        if (!classSymbol.hasMyMarker()) return emptySet()
        return setOf(Name.identifier("generatedName"))
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        if (callableId.callableName.asString() != "generatedName") return emptyList()
        return listOf(buildGeneratedFunctionSymbol(callableId, context))
    }
}
```

Validate with:

- A test that compiles `obj.generatedName()`.
- A `.fir.txt` golden showing the generated declaration.
- A backend or runtime test if the function needs executable behavior.

## Generate A Top-Level Declaration

Use top-level declaration generation for plugin API not owned by a user class.

Steps:

1. Return generated class IDs from `getTopLevelClassIds`.
2. Generate class-like declarations from those IDs.
3. Return top-level callable IDs from `getTopLevelCallableIds`.
4. Generate functions/properties from those callable IDs.
5. Implement or fill bodies in IR if needed.

Use package existence generation if the generated package should be considered present even without source files.

## Add A Supertype

Use `FirSupertypeGenerationExtension` for annotation-driven marker interfaces or required supertypes.

Steps:

1. Return true from `needTransformSupertypes` only for relevant declarations.
2. Build user type refs for added supertypes.
3. Resolve them through the provided type resolver.
4. Return only additional supertypes.

Do not attempt to remove explicit user supertypes. This extension adds, not replaces.

## Transform Status

Use `FirStatusTransformerExtension` to change allowed status fields.

Steps:

1. Filter declarations in `needTransformStatus`.
2. Use the typed `transformStatus` overload for the declaration kind.
3. Copy/transform the existing status rather than rebuilding from scratch.
4. Preserve visibility for regular classes and type aliases.

Good targets:

- Generated functions.
- Annotated functions/properties.
- Property accessors.
- Constructors or fields where supported.

## Add A Session Component

Use `FirExtensionSessionComponent` for shared per-session services.

Pattern:

```kotlin
class MySessionComponent(session: FirSession) : FirExtensionSessionComponent(session) {
    override val componentClass: KClass<out FirExtensionSessionComponent>
        get() = MySessionComponent::class

    val cache = mutableMapOf<ClassId, MyComputedData>()
}
```

Use it for:

- Caching annotated symbol discovery.
- Sharing computed metadata between checkers and declaration generation.
- Centralizing plugin options after parsing.

Do not use process-global caches. FIR may be recomputed in IDE sessions.

## Add A Type Attribute

Use `FirTypeAttributeExtension` when type annotations need semantic data.

Steps:

1. Define a custom `ConeAttribute`.
2. Add a typed accessor on `ConeAttributes`.
3. Convert matching annotations to attributes.
4. Convert your attributes back to annotations when needed.
5. Ignore attributes that do not belong to your plugin.

Use this for type-level semantics, not declaration-level metadata.

## Function Type Kinds And Metadata

`FirFunctionTypeKindExtension` is rare. Use it only when introducing a new family of function types.

`FirMetadataSerializerPlugin` is maintenance-only for most projects. It is temporary/internal in Kotlin sources and should not be chosen casually for new plugin designs.
