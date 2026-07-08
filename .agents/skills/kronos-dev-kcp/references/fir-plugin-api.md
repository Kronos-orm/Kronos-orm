# FIR Plugin API

This reference summarizes Kotlin FIR plugin extension points and how to use them. It is intentionally content-first: do not treat it as a link collection.

## Registrar

Register FIR extensions by implementing `FirExtensionRegistrar` and overriding `ExtensionRegistrarContext.configurePlugin`.

The registrar context supports unary-plus registration for extension factories:

```kotlin
class MyFirRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::MyCheckersExtension
        +::MyDeclarationGenerationExtension
        +::MySupertypeGenerationExtension
    }
}
```

Use the registrar to:

- Register FIR extensions.
- Register custom diagnostic containers.
- Wire plugin configuration into extension constructors.

Keep registrar logic declarative. Put behavior in extension classes.

## Predicate-Based Lookup

FIR plugins commonly discover declarations through annotation predicates. Register predicates in `FirExtension.registerPredicates` before querying predicate-based providers.

Useful predicate kinds:

- `has`: declaration has an annotation.
- `under`: declaration is inside a class or declaration with an annotation.
- `metaHas`: declaration has an annotation marked by a meta-annotation.
- `metaUnder`: declaration is under a declaration marked by a meta-annotation.
- Combined predicates: `hasOrUnder`, `metaHasOrUnder`, `and`, `or`.

Rules:

- Predicate annotations should be plugin-owned top-level annotations.
- Register every predicate you plan to query.
- Do not assume annotation arguments are resolved in early FIR phases.

## Phase Awareness

FIR is not a single fully-resolved tree. It is progressively enriched. Missing types, unresolved symbols, empty annotation arguments, or incomplete statuses often mean the plugin is reading data too early.

Phase-sensitive guidance:

- Raw FIR: declaration shape only.
- Import resolution: lookup context becomes meaningful.
- Supertype resolution: explicit supertypes become available.
- Status resolution: visibility, modality, and modifiers are finalized.
- Body resolve: expression-level types become reliable.
- Contracts and constant evaluation: later semantic facts.

Choose extension points by phase. Do not pull later facts into earlier phases by ad hoc resolution unless the API explicitly supports it.

## FirAdditionalCheckersExtension

Use this to add compiler diagnostics.

It exposes checker groups:

- Declaration checkers.
- Expression checkers.
- Type checkers.
- Language-version-settings checkers.

Use it when a plugin must reject or warn about source based on FIR declarations, expressions, or types. Keep checks narrow and diagnostics stable. Test these with diagnostics tests, not runtime `box` tests.

Good use cases:

- Invalid annotation placement.
- Invalid combination of modifiers.
- Unsupported expression form in a DSL.
- Type constraints that normal Kotlin cannot express.

## FirDeclarationGenerationExtension

Use this to generate declarations visible during resolution.

It can generate:

- Top-level class-like declarations.
- Nested class-like declarations.
- Top-level callable declarations.
- Member functions.
- Member properties.
- Constructors.
- Java fields.
- Package existence.

Important method groups:

- Class IDs and class generation.
- Callable IDs and callable generation.
- Names for class members.
- Nested classifier names and generation.
- Constructor generation.
- Package existence checks.

Contracts:

- `generate...` methods should be side-effect-free.
- Do not cache generated FIR elements or symbols outside the compiler cache contract.
- A `generate...` method is called only after the matching `get...` method declares the name or ID.
- Results are cached.
- Different generation methods can first run at different phases.
- Generated declarations must be resolved enough for the phase that requests them.
- Use plugin declaration origin for generated declarations.
- For generated constructors, expose the constructor name through member callable-name lookup.
- For generated companion objects, generate the companion class and then generate its members through the same extension.
- Prefer backend IR for generated bodies when frontend only needs signatures.

Use cases:

- Synthetic API members that user code can call.
- Generated nested classes.
- Companion helper generation.
- Annotation-driven declarations.

## FirExtensionSessionComponent

Use this for typed session-scoped shared services.

It is both a FIR extension and a `FirSessionComponent`. Each instance belongs to a `FirSession`, so it is the right place for per-session caches or helper services used by multiple FIR extensions.

Use it when:

- Checkers, declaration generation, and status/supertype extensions need shared computation.
- Expensive lookup should be cached per session.
- A plugin needs a typed component available from FIR session context.

Avoid:

- Global mutable state.
- Cross-session object leaks.
- Caches that assume one-shot compilation, because IDE analysis may retry computations.

## FirFunctionTypeKindExtension

Use this to register custom function type families.

The extension registers pairs:

- Non-reflect function type kind.
- Reflect function type kind.

This is a rare extension. Use it only when a language or DSL feature genuinely needs new function type families beyond ordinary Kotlin function types. Most compiler plugins should not use it.

## FirMetadataSerializerPlugin

This writes custom information into serialized Kotlin metadata.

The current source marks this extension point temporary/internal and warns not to use it for new work. Treat it as maintenance-only unless a Kotlin-version-specific design explicitly requires it.

Its core operation registers protobuf extensions for class symbols through a string table and proto registrar.

Use cases:

- Existing compiler-integrated plugins that already serialize custom metadata.
- Migration work where metadata compatibility is required.

Avoid for new plugin features unless there is no other durable handoff mechanism.

## FirStatusTransformerExtension

Use this to change declaration status: visibility, modality, and modifier flags.

Important methods:

- `needTransformStatus(declaration)`.
- General `transformStatus(status, declaration)`.
- Typed overloads for properties, functions, regular classes, type aliases, property accessors, constructors, fields, backing fields, and enum entries.

Rules:

- Do not change regular class visibility.
- Do not change type alias visibility.
- Those changes can affect type resolution and violate phase contracts.
- Use the typed overload when behavior differs by declaration kind.
- Preserve existing flags unless intentionally changing them.

Good use cases:

- Add `operator`, `infix`, `inline`, `suspend`, or similar flags when allowed.
- Change modality for generated or annotated non-class declarations.
- Adjust property/function visibility when it does not violate phase contracts.

## FirSupertypeGenerationExtension

Use this to add supertypes to existing class-like declarations.

Important methods:

- `needTransformSupertypes(declaration)`.
- `computeAdditionalSupertypes(classLikeDeclaration, resolvedSupertypes, typeResolver)`.
- Optional generated-nested-class supertype hook.

Rules:

- It adds supertypes; it does not replace explicitly declared supertypes.
- It runs after explicit supertypes are resolved but before final supertypes are written.
- Use the provided type resolver to resolve user type refs.
- The generated-nested-class hook is experimental and limited.
- Generated nested class handling is one level deep and does not apply to top-level generated classes.
- If new generated types are added, default `Any` may be removed automatically.

Good use cases:

- Annotation-driven marker interfaces.
- Generated nested classes whose parent type depends on annotation arguments.
- Plugin-specific supertypes needed before later resolution.

## FirTypeAttributeExtension

Use this to map type annotations to custom `ConeAttribute` values and back.

Important methods:

- `extractAttributeFromAnnotation(annotation)`.
- `convertAttributeToAnnotation(attribute)`.

Rules:

- Define a custom `ConeAttribute`.
- Define a `ConeAttributes` accessor using the attribute accessor delegate.
- Convert only attributes created by your plugin.
- Return `null` for compiler attributes and attributes from other plugins.

Good use cases:

- Type-level semantic markers that must survive through type resolution.
- Plugin-specific type attributes needed by later FIR logic.

## FIR to IR Handoff

Generated FIR declarations are converted to backend IR. A robust plugin keeps the contract simple:

- FIR owns signatures, symbols, diagnostics, supertypes, status, metadata, and resolution-visible declarations.
- IR owns executable bodies and binary behavior.
- Use plugin origins, stable names, annotations, or metadata to recognize FIR-generated declarations in backend code.
