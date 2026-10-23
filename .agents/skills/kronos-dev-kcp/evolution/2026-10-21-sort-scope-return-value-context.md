# Sort scope callbacks must preserve consumed return values

## Observed failure

Inside an order-by callback, `val label = run { return@run "name" }` compiled successfully but
assigned `null` to `label`. The official box test failed at runtime with
`Nested return changed null`. The same ownership rule applies to `with(this)` results consumed
by a variable initializer, local variable assignment or property setter.

## Cause

An unqualified Kotlin scope function can inherit the surrounding `KTableForSort` receiver.
`KronosParserTransformer` dispatched every function with that receiver to `SortTransformer`,
including the ordinary scope callback whose value was needed by its caller. Field collection
then replaced its String return with the DSL's neutral value.

## Verified correction

Record the direct callbacks of standard `kotlin.run` and `kotlin.with` calls when their results
are consumed by an initializer or assignment. Keep those callback bodies ordinary while
continuing to traverse them. Apply this exception only to sort dispatch. Scope calls returned
directly by a sort selector retain their existing field-collection behavior.

## Verification

The official FIR/IR box pipeline passed all 51 projection, static metadata and sort cases with
CLI SHA-256 `dc8ee0e2ba6d7121815b4b3973332bea4e9ab38a8bb3e0a94f0028bf271971d4`.
`nestedFunctionReturnsKeepOuterSort` checks initializer, local reassignment and property-setter
values, one-time side effects, and the exact outer sort fields and directions.
`scopeFunctionTailSelectors` checks the exact fields collected by tail `run`, `with` and `apply`.
The complete 918-file detekt scan passed without rule or exclusion changes.
