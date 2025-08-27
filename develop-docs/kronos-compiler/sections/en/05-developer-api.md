# 5. Developer API (Helpers Overview)

This section lists reusable helpers/utils to speed up extension work. It’s a quick reference; always consult the source for full details.

## 5.1 Generic IR Helpers (`com.kotlinorm.compiler.helpers`)

- `extensionReceiver / dispatchReceiverArgument / extensionReceiverArgument`: convenience accessors for receivers.
- `valueArguments`: uniform/safe way to access call arguments.
- `invoke / instantiate`: generate call/constructor IR from symbols quickly.
- `irEnum / irCast / sub`: common IR fragments and type handling.
- `kFunctionN / nType / toKClass`: helpers for KFunctionN types and KClass literals.

## 5.2 KClassCreatorUtil (`plugin.utils`)

- `kPojoClasses: MutableSet<IrClass>`: cache of collected KPojo subclasses.
- `initFunctions: MutableSet<Triple<IrPluginContext, IrBuilderWithScope, IrFunction>>`: functions captured via `@KronosInit`.
- `resetKClassCreator()`: reset state for a new compilation.
- `buildKClassMapper(function: IrFunction)`: rewrites target function to set `kClassCreator` based on `kPojoClasses`.

## 5.3 Query Task Helpers (KQueryTaskUtil)

- `fqNameOfTypedQuery: List<FqName>`: known query entrypoints needing type fixes.
- `fqNameOfSelectFromsRegexes: List<String>`: regex patterns for `SelectFrom*` variants.
- `updateTypedQueryParameters(irCall: IrCall): IrCall`:
  - Injects `isKPojo` and `superTypes` (as strings);
  - Keep the last two arguments in order.

## 5.4 KTableForSelect utilities (`kTableForSelect.KTableForSelectUtil`)

- `addFieldList(irFunction, irReturn)`: inject field collection before return.
- `collectFields(irFunction, element)`: parse property gets/`as_` aliasing/`+` & `-` operations/constants into `FieldSymbol`s.

## 5.5 KTableForCondition utilities (`kTableForCondition.KTableForConditionUtil`)

- `updateCriteriaIr(irFunction)`: assign built criteria to the receiver.
- `buildCriteria(irFunction, element, ...)`: recursively build `CriteriaIR` from boolean expressions; supports `and/or/not/in/between/like/isNull` and annotation-driven behavior.

## 5.6 KTableForSort utilities (`kTableForSort.KTableForSortUtil`)

- Recognize `asc()/desc()` and emit sort items into the IR collection.

## 5.7 KTableForSet utilities (`kTableForSet.KTableForSetUtil`)

- Parse `property to value` entries and build update expressions.

## 5.8 KTableForReference utilities (`kTableForReference.KTableForReferenceUtil`)

- Record cross-table/alias references to ensure correct table alias binding in generated queries.

## 5.9 Misc

- `IrKDocUtil`: KDoc handling.
- `IrNewClassUtil`: create new classes/constructors in IR.
- `IrSqlTypeUtil`: map Kotlin types to Kronos column types and constants.
- `IrKronosCommonStragety`, `IrKronosFieldUtil`: shared field/strategy helpers.

> Tip: prefer extending helpers over writing complex IR in transformers directly.
