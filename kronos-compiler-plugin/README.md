# Module kronos-compiler-plugin

K2 Kotlin compiler plugin for Kronos ORM. Provides FIR diagnostics and typed projections, plus IR transformations for reflection-free ORM operations.

## What It Does

1. **KPojo class augmentation** — generates method bodies for `toDataMap`, `fromMapData`, `kronosColumns`, `get`/`set`, strategy methods, table metadata
2. **DSL lambda transformation** — rewrites `KTableForCondition/Select/Set/Sort/Reference` lambdas into `Criteria`/`Field` IR
3. **Generated type providers** — generates source-owned KPojo factories and metadata for reflection-free instantiation
4. **Typed query parameter injection** — supplies type information to runtime result mapping
5. **Typed projections and diagnostics** — generates query row/context types and validates fields, aliases and subquery shapes

## Architecture

```
KronosCommandLineProcessor → KronosCompilerPluginRegistrar (supportsK2 = true)
  → KronosFirExtensionRegistrar (checkers, call refinement, declaration generation)
  → KronosIrGenerationExtension
    → KronosParserTransformer (single module-level IR traversal)
      → KronosIrClassTransformer + KronosClassBodyGenerator (KPojo augmentation)
      → SelectTransformer / SetTransformer / ConditionTransformer / SortTransformer / ReferenceTransformer
      → Generated type provider generation
      → TypeParameterFixer (typed queries)
```

## Key Files

| File | Role |
|------|------|
| `core/ConditionAnalysis.kt` | Condition DSL → Criteria IR (~900 lines) |
| `core/FieldAnalysis.kt` | Field DSL → Field IR (~860 lines) |
| `core/Symbols.kt` | All IrSymbol resolution (~620 lines) |
| `utils/Constants.kt` | FqName and ClassId constants |
| `utils/TypeUtils.kt` | Kotlin type → KColumnType mapping |

## Build And Test

```sh
scripts/run build build -m kronos-compiler-plugin -p jvm
scripts/run build test -m kronos-compiler-plugin -p jvm
scripts/run build test -m kronos-compiler-plugin -p jvm --include-classes='*KPojoGeneratedBodiesBoxTest'
```

Production code uses the Kotlin compiler API as a compile-only dependency. Kotlinx Serialization is a cross-platform test dependency managed by the root version catalog. The registrar and command-line processor use static `META-INF/services` entries.

## Editor Integration

[Kotlin External FIR Support (KEFS)](https://github.com/Mr3zee/kotlin-external-fir-support), by Mr3zee, loads this compiler plugin into Kotlin IDE analysis. The shared `.idea/kotlin-plugins.xml` configures the Kronos bundle. The official Kotlin Toolchain IDE plugin imports the repository's `project.yaml` build model.

Run `scripts/run build bootstrap` to prepare the ordinary compiler coordinate for import and its IDE compatibility coordinate. The complete CLI JAR is reused byte-for-byte; `kronos-testing/ide` verifies actual editor types, completion, syntax and diagnostics against the loaded class bytes.
