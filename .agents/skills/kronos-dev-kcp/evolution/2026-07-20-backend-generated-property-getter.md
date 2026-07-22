# Backend-Generated Getters Must Belong To An IR Property

## Symptom

A compiler-plugin-generated class compiled successfully but failed to load with `ClassFormatError: Illegal method name "<get-id>"`.

## Cause

The backend created an ordinary `IrSimpleFunction` using the special IR accessor name copied from an interface getter. With `IrDeclarationOrigin.DEFINED` and no corresponding `IrProperty`, JVM lowering emitted `<get-id>` directly into the class file instead of lowering it to `getId`.

## Fix

Generate the override as an `IrProperty`, add its getter with `IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR`, and set both property and getter override symbols. Attach the getter body to that accessor.

## Prevention

- Never create `<get-...>` or `<set-...>` as ordinary backend functions.
- Model generated property overrides as properties with corresponding accessor symbols.
- Verify the generated class through a runtime box test; `javap` should show a legal JVM accessor such as `getId()`.
