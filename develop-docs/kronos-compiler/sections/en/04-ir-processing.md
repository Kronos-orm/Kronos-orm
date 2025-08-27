# 4. IR Processing Flow

This section outlines the compilation-time IR processing order and key logic based on the source code.

## 4.1 Entry: KronosParserExtension.generate

Kotlin (K2) calls IrGenerationExtension during IR generation:
- Call `resetKClassCreator()` to clear caches;
- Apply `KronosParserTransformer` to the `moduleFragment`;
- After traversal, replay `initFunctions` and call `buildKClassMapper` in the collected contexts;
- If `debug` is enabled, use `dumpKotlinLike` to write each IR file to `debugInfoPath`.

## 4.2 KronosParserTransformer: Core Dispatcher

- visitCall
  - Iterate calls; if any type argument’s class is a KPojo subclass (`superTypes.any { it.classFqName == KPojoFqName }`), add it into `kPojoClasses`;
  - If the callee has `@KronosInit`, capture the function expression and store into `initFunctions` to be replayed later;
  - For `TypedQuery` and `SelectFrom*` calls, run `updateTypedQueryParameters(expression)` to fix type parameters;
- visitFunctionNew
  - Route function bodies to KTable* transformers based on the extension receiver’s FQ name:
    - `KTableForSelect` → `KTableParserForSelectTransformer`
    - `KTableForSet` → `KTableParserForSetTransformer`
    - `KTableForCondition` → `KTableParserForConditionTransformer`
    - `KTableForSort` → `KTableParserForSortReturnTransformer`
    - `KTableForReference` → `KTableParserForReferenceTransformer`
- visitClassNew / visitClassReference / visitConstructorCall
  - Collect all KPojo subclasses for later use.

## 4.3 KTable* Transformation Approach

Take `KTableParserForSelectTransformer` as an example:
- Inject IR before return: `addFieldList(irFunction, expression)`;
- The helper parses DSL expressions like `it.username.as_("alias")` and converts them into internal field descriptors being appended;
- The original behavior is preserved, while the needed field list is collected at compile time.

Other transformers replace/enhance the body in similar ways for conditions, sorting, and references.

## 4.4 Initialization Hooks and Mapping Generation

- Function expressions annotated with `@KronosInit` are captured in visitCall;
- At the end of compilation, `KClassCreatorUtil.initFunctions` replays them and calls `buildKClassMapper` under the proper `builder` and `context`;
- This step generates mappings based on the collected `kPojoClasses` for runtime or later phases.
