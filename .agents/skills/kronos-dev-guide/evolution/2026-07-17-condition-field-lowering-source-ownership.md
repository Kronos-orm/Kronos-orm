# Condition Field Lowering Must Classify the Receiver

## Symptom

Condition lowering treated every `GET_PROPERTY` call as a SQL field. Ordinary Kotlin properties could therefore become source-less columns, while top-level properties inside `f.xxx(...)` could fail table-name extraction because they have no dispatch receiver.

Special-casing the property name `value` caused a second bug: a real KPojo column named `value`, including a nested chain such as `it.value?.value`, was skipped as though it were the condition DSL value helper.

## Cause

IR expression shape and property names do not establish SQL field ownership. Regular classes, data classes, objects, companion or static accessors, and top-level accessors can all appear as property-like expressions without representing query sources.

## Fix

- Treat a member property as a SQL field only when its dispatch receiver type is KPojo-like or a generated projection type.
- Preserve all other getters as ordinary Kotlin expressions so their runtime values reach parameter or function argument construction.
- Apply the same receiver classification to direct comparisons, function arguments, safe-call field chains, and table-name extraction.
- Do not identify the condition DSL `.value` helper by name in IR. A real KPojo member named `value` is still a field. FIR may identify the helper by its exact callable symbol.

## Prevention

Use official compiler box tests that execute the generated code and assert exact expressions and parameter maps for:

- regular class and data class properties;
- object, companion, `@JvmStatic`, and top-level properties;
- the same values inside `f.xxx(...)`;
- KPojo fields named `value` and nested `it.value?.value` chains;
- generated projection fields and existing relationship safe calls.
