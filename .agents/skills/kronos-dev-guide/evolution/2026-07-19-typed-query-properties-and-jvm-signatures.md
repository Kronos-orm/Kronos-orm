# Typed Query Properties And JVM Signatures

## Symptom

A query type can retain its generic `Source` parameter in the class declaration while DSL members exposed through an inherited `KSelectable.pojo` are still typed as `KPojo`. Internal selectors then lose the concrete source type and no longer expose source-specific fields.

The same class can also fail JVM compilation when it declares both `fun f(field: Field)` and a member extension `fun Field.f()`: both lower to the same JVM method signature even though their Kotlin call syntax differs.

## Cause

An inherited open property keeps the type declared by its parent unless the subclass narrows it with a covariant override. Kotlin member extensions are compiled as instance methods whose extension receiver becomes a regular JVM parameter, so receiver syntax does not make their bytecode signatures distinct.

## Fix

- In a `KSelectable` subclass parameterized by `Source`, narrow the inherited property explicitly with `override val pojo: Source`.
- Do not pair a regular method and a member extension that have the same name and effective parameter types. Rename one API or give one declaration a distinct JVM name where that is appropriate for its visibility and platform contract.

## Prevention

When introducing typed query wrappers, inspect every inherited open property used as a DSL receiver and override it with the narrowest public type. When adding member extensions, compare their lowered form as `method(extensionReceiver, valueParameters...)` against ordinary methods in the same class before choosing names.

Verified with:

```text
./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain
```
