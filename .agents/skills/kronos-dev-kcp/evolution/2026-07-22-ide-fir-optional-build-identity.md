# IDE FIR-Only Loading Must Allow Build Identity To Be Absent

## Symptom

An IDEA real fixture failed before FIR analysis with `Required plugin option not present: kronos-compiler-plugin:generated-provider-id` after module-unique generated-provider options were added to the compiler plugin.

## Cause

Both provider identity options were declared as individually required CLI options. IDEA loads the compiler plugin directly for FIR analysis and does not run the Gradle or Maven compilation integration that supplies those build-scoped values. CLI `required` metadata also cannot express the actual all-or-none pair invariant.

## Fix

Declare both CLI options as optional, then validate them together when reading compiler configuration: accept only the state where both are absent or both form a valid provider identity. Keep real IR compilation strict when provider configuration is absent, and keep Gradle, Maven, and official compiler tests responsible for supplying module-unique paired values.

## Prevention

- Separate plugin-loading requirements from build-compilation requirements.
- Use CLI option metadata only for independent option constraints; enforce paired invariants in configuration parsing.
- Do not invent a default provider identity for IDE loading.
- Cover the absent pair, both partial pairs, blank values, invalid FQ names, and one real IDEA FIR fixture.
