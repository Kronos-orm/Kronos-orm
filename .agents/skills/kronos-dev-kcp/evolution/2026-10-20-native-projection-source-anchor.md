# Native projection classes need a frontend source anchor

## Observed failure

Real Native SQLite ORM tests linked but failed during metadata initialization with `IrLinkageError`:
`typeOf` referenced an unlinked generated projection class. JVM projection tests passed. Inspecting the
test KLIB showed that the classes appeared among imported signatures but were absent from declared IR
and metadata signatures.

Native FIR-to-KLIB serialization pairs IR files with frontend source files. Adding an IR-only synthetic
file to the module does not provide the corresponding frontend file, so its declarations can be omitted.

## Verified correction

The Gradle Native metadata task generates a real Kotlin source file in `com.kotlinorm.generated.projection`,
with a deterministic anchor object derived from the compilation's metadata entry name. Main and test
compilations have separate anchors. The projection transformer attaches generated classes to that source
file and registers each completed class through `metadataDeclarationRegistrar.registerClassAsMetadataVisible`.
JVM projections retain their existing per-source file ownership.

The rebuilt Native test KLIB declared all six projection classes in both IR and metadata signatures,
including constructors and accessors. All 42 real SQLite cases then passed, including four full ORM
cases, and the default ORM console completed. The required-suite executable guard independently ran
the same 42 tests successfully.

Keep the official source-anchor box test and real Native execution. JVM box verification alone cannot
prove KLIB serialization. Do not suppress partial-linkage errors or remove generated projections from
metadata registration to hide this failure.
