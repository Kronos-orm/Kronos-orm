# Writable mapped properties need writable JVM backing fields

## Trigger

After the Toolchain migration compiled tests with JVM target 17, the real
`KCascadeTest.testManyToManyDelegate` test threw `IllegalAccessError` when the
generated dynamic setter assigned the source `val StudentCourse.student`.
The same invalid write had gone unnoticed with JVM target 8.

## Cause and fix

`KronosParserTransformer.processKPojoClass` converted mapped properties to
`isVar = true` and `isConst = false`, but their backing fields retained
`isFinal = true`. For a non-delegated mapped property, clear the backing field's
`isFinal` together with the property transformation. Generated `set`,
`fromMapData` and `safeFromMapData` then write a writable field.

Leave delegated-property backing fields alone: they hold delegate instances,
and mapped assignments use the delegate's accessors.

## Verification

- `valPropertiesAreWritable.kt` explicitly uses `// JVM_TARGET: 17` and checks
  dynamic access, ordinary mapping and safe mapping on source `val` properties.
- Toolchain focused compiler tests: 19 executed, zero failures or skips
  (`/tmp/kronos-toolchain-kcp-focused17.log`).
- Complete Toolchain core suite: 1010 executed, zero failures or skips
  (`/tmp/kronos-quality-full-coverage3.log`).

Do not work around final-field writes by lowering the JVM target or removing
the mapping contract.
