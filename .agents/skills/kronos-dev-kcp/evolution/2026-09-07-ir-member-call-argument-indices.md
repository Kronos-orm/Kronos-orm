# IR Member Call Argument Indices

## Symptom

A generated Native entry linked but failed at runtime with an `IrLinkageError` saying a required member
function parameter was absent, even though the generator appeared to assign every argument.

## Cause

For an `IrCall` to an instance member, the `arguments` array includes the dispatch receiver at index `0`.
Setting `dispatchReceiver` and then assigning `arguments[0]` overwrites that receiver. Subsequent values are
therefore shifted and Native can defer the failure until link/runtime validation.

## Verified Rule

Use one convention consistently. For generated Kronos member calls, assign the receiver and regular arguments
through the full `arguments` array:

```kotlin
irCall(memberSymbol).apply {
    arguments[0] = irGet(receiver)
    arguments[1] = firstValue
    arguments[2] = secondValue
}
```

The `GeneratedTypeProviderGenerator` registration calls are the local reference pattern. Verify generated
member calls through a Native runtime test, not only JVM compilation, because a malformed Native call can
survive compilation and fail as `IrLinkageError` later.
