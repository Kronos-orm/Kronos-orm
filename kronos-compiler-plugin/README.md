# Module kronos-compiler-plugin

K2 Kotlin compiler plugin for Kronos ORM. Performs compile-time IR transformations to enable zero-reflection ORM operations.

## What It Does

1. **KPojo class augmentation** — generates method bodies for `toDataMap`, `fromMapData`, `kronosColumns`, `get`/`set`, strategy methods, table metadata
2. **DSL lambda transformation** — rewrites `KTableForCondition/Select/Set/Sort/Reference` lambdas into `Criteria`/`Field` IR
3. **@KronosInit handling** — generates `kClassCreator` maps for reflection-free instance creation
4. **Typed query parameter injection** — injects `isKPojo` + `superTypes` into `queryList`/`queryOne` calls

## Architecture

```
KronosCommandLineProcessor → KronosCompilerPluginRegistrar (supportsK2 = true)
  → KronosIrGenerationExtension
    → KronosParserTransformer (single module-level IR traversal)
      → KronosIrClassTransformer + KronosClassBodyGenerator (KPojo augmentation)
      → SelectTransformer / SetTransformer / ConditionTransformer / SortTransformer / ReferenceTransformer
      → KClassMapGenerator (@KronosInit)
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

## Dependencies

- `compileOnly`: kotlin-compiler-embeddable, auto-service
- `implementation`: kotlinx-serialization
- Coverage: Kover with 80% minimum bound
