# Generated properties need navigable PSI declaration views

## Observed behavior

IDEA 2026.2.3's Analysis API `getAllowedPsi` rejects `PluginGenerated.Custom` fake sources. FIR correctly resolves a generated projection's property and type, but stock Quick Documentation returns no result, and Go to Declaration falls back to the containing source file. Checking completion or hovering the receiver does not detect this failure.

## Verified correction

Keep generated classes anchored to their original compilation file, preserve plugin declaration origins and fixed-symbol owner lookup tags, and retain fake sources for constructor parameters. For generated **properties**, a PSI compilation can expose a readable Kotlin declaration view containing the complete Result or Context field set. Use a contextual `KtPsiFactory.createPhysicalFile` so the declaration has an in-memory virtual file that the stock editor can open. An ordinary nonphysical PSI file can resolve to a property while still failing actual navigation.

Render each field's resolved FIR type with `ConeTypeRendererForReadability` and `ConeFullyQualifiedIdRenderer`. Supply its generated `KtProperty` as a real source element. Use property accessors in the view so its stock signature has the exact property name and type without displaying a fabricated initializer. The compiler's real generated implementation remains owned by FIR and IR.

The embeddable compiler relocates IntelliJ PSI classes, while the IDE compiler uses their original package names. Resolve the PSI getter, contextual factory overload and real-source constructor from the active compiler through reflection; directly linking their PSI parameter or return descriptors produces incompatible JVM signatures. All remaining FIR and Kotlin PSI operations stay strongly typed. Light-tree compilation retains the source anchors used by the existing backend.

## Verification

The complete Kotlin Toolchain 0.13.0 / Kotlin 2.4.21 compiler JAR passed 26 real IDEA 2026.2.3 and KEFS 0.3.12-262 editor contracts in the real editor fixture. The harness checked all 64 critical FIR/registrar/processor class bytes against that CLI JAR. Tests verify property hover signatures for `uid` and window alias `rn`, then resolve the property and use `FileEditorManager.openTextEditor` to prove that clicking `row.uid` and `it.rn` opens the readable declaration. The suite also verifies generic field types, completion, aliases, JOIN, query layers, collection literals and diagnostic locations.

`scripts/ide.main.kts verify` requires all 26 actual editor contracts and rejects skips, failures and evidence from a different compiler JAR. The `kronos-testing/ide` Toolchain module supplies IDEA, KEFS and their real test infrastructure.

`ProjectionPsiBoxTest` also runs the official Kotlin compiler pipeline with `FirParser.Psi`. Its box executes alias, window and nested generic projection mapping, checks the generated values and SQL, and restores the global data source afterward. The remaining suites keep their existing default `FirParser.LightTree` configuration, so both source-element paths receive real compiler coverage.
