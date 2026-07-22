# Runtime KType Assignability Must Normalize Primitive Boxing

## Symptom

Value conversion rejected a valid encode such as `false` to a logical
`typeOf<Boolean>()` target before any codec ran. The inferred
`value::class.starProjectedType` and the declared Kotlin type represented
Boolean with different JVM class forms.

## Cause

`KType.isStructurallyAssignableTo` compared `KClass.java` directly. Kotlin
primitive classifiers can resolve to JVM primitive classes while runtime
values expose boxed JVM classes, so Java assignability reported a false
negative even though the logical Kotlin type matched.

## Fix

Compare `KClass.javaObjectType` for both source and target classifiers before
checking generic arguments. Keep recursive runtime collection element checks
and declared generic `KType` structure checks unchanged.

## Prevention

Cover conversion paths where source type is inferred from primitive runtime
values, and separately cover declared generic source mismatches such as
`List<String>` versus `List<Enum>` so boxing normalization does not weaken
generic validation.
