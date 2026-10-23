# Generic IR Calls Need Substituted Result Types

Verified: 2026-10-17, Kotlin 2.4.21

The official compiler tests reject generated KPojo bodies with
`IR_VALIDATION_ERROR`: type parameters such as `K` and `V` from `mutableMapOf`
are not available in the scope of the generated function.

Setting `IrCall.typeArguments` does not automatically substitute `IrCall.type`.
The call builder can retain the callee's declaration-level generic return type.
Set the result type explicitly, for example:

```kotlin
irCall(mutableMapOfSymbol).apply {
    type = context.irBuiltIns.mutableMapClass.typeWith(keyType, valueType)
    typeArguments[0] = keyType
    typeArguments[1] = valueType
    arguments[0] = irVararg(pairType, pairs)
}
```

Apply the same rule to generic constructors and member calls whose return type
depends on the receiver (`Pair`, `Map.get`) as well as list factory functions.
Projection-helper replacements also require the concrete query result type:
`selectClass.typeWith(sourceType, resultType, contextType)`. Otherwise the return
type retains the helper's declaration-level type parameters, even when its three
call type arguments are populated correctly.
Keep IR validation enabled. A JVM lowering that happens to erase these types
does not prove the IR is valid for Native.

Evidence: all six `KPojoGeneratedBodiesBoxTest`, 48 `ConditionBoxTest` and
39 `ProjectionBoxTest` tests pass with strict IR validation after the repairs.
The complete compiler suite subsequently passed 339 tests. Native execution
remains a separate check.
