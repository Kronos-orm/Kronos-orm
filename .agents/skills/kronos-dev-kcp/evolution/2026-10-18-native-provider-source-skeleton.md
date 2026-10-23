# Native provider constructors need a source-visible skeleton

## Observed failure

The four-module Native metadata consumer compiled and linked successfully, but its macOS executable
threw `IrLinkageError: No constructor found` when a public registration entry constructed a provider class
created only by backend IR. The constructor reference crossed a dependency KLIB boundary. JVM tests had not
exposed the problem.

## Verified correction

Generate the private Native provider class in a Gradle-owned common Kotlin source file. Its constructor,
`id` property and `contributeTo(GeneratedTypeRegistrar)` method are real frontend declarations. The source
registration entry constructs that class normally. Backend IR fills the existing `id` getter and `contributeTo` bodies, reusing the JVM
factory, enum and static-descriptor body generator. It does not synthesize a second callable declaration.

The same four-module `macosX64Test` then executed its dependency enum factory and explicit library/application
registration graph with zero failed or skipped tests. Its fixture also checks registration after freeze.
Keep Native execution and multi-module publication/link checks: compiling a KLIB or a framework alone does
not detect every deferred linkage failure.

Do not hide this class of error with automatic initialization, reflective construction, relaxed linking,
or a JVM-only replacement test.
