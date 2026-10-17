# Kotlin 2.4.21 FIR And Diagnostic Reporting APIs

Verified: 2026-10-16

The 2.4.0-to-2.4.21 upgrade fails compilation if a checker imports
`org.jetbrains.kotlin.fir.declarations.unwrapVarargValue` or the registrar reads
`CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY` directly.

- `unwrapVarargValue` is now in `org.jetbrains.kotlin.fir.analysis.checkers`.
- Production backend reporting should use `CompilerConfiguration.report` with
  `KtSourcelessDiagnosticFactory` and `reportInfo`, preserving message locations.
- Build renderer maps using the public delegated
  `KtDiagnosticFactoryToRendererMap(name) { map -> ... }` DSL; its plain constructor
  is internal.
- Keep collector injection for ordinary reporter unit tests, without reading the
  compiler configuration's collector in production.
- The official compiler black-box framework base is now
  `org.jetbrains.kotlin.test.runners.codegen.AbstractJvmBlackBoxCodegenTestBase`
  with `FirParser.LightTree`; the old FIR-specific base is absent.

Verified by compiler-plugin/core JVM compilation and 106 syntax JVM tests on
Kotlin 2.4.21. After the IR generic-call repairs recorded separately, the full
compiler suite passed 339 tests on 2026-10-17.
