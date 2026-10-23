# IDE-generated projection members need an explicit symbol owner

## Observed failure

Stock Kotlin completion found a generated projection but failed while calculating the property's
containing class. `LLContainingClassCalculator` could not resolve the dynamically generated class
through the ordinary class-id lookup tag. It then followed the source field's PSI, whose declaration
belongs to a different class, and rejected the plugin declaration origin.

## Verified correction

Use `ConeClassLikeLookupTagWithFixedSymbol(classId, classSymbol)` where generated members refer to
their generated owner: the constructor result type, constructor containing-class attribute, and
property dispatch receiver. The IDE can then obtain the actual owner directly from the symbol.

Give generated classes and members `KtFakeSourceElementKind.PluginGenerated.Custom` sources. Use
the generated class ID as the class key and `(classId, fieldName)` as the member key. Preserve the
source anchor for file ownership while distinguishing the generated declaration from its original
entity or field declaration.

Do not require real entity-field PSI as the generated property's declaration source. The correct
contract is that stock editor analysis resolves the generated member with its owner and type.

## Verification

`kronos-testing/ide` runs the real IDEA 2026.2.3 SDK and KEFS 0.3.12 against the complete Kotlin
Toolchain 2.4.21 compiler-plugin JAR. Its editor tests cover generated types, nullable/list fields,
JOIN, aliases, subqueries, windows, empty-selector completion, collection literals and diagnostics.

The old `KronosProjectionIdeBridgeTest` source-string assertions described the former IDE bridge's
implementation. That bridge and its system-property broadcast were removed. The editor fixtures
replace those assertions with actual completion, type-checking and diagnostic-location contracts.
