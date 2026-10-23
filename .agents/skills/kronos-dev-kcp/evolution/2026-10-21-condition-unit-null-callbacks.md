# Condition lowering must preserve callbacks without a condition value

## Symptom

Compiling the runtime's ordinary `afterFilter` callbacks reported unrecognized `with`, `let`,
`andWhere`, or unsupported `IrSetValueImpl`/null expressions. These callbacks forward an actual
condition and then copy state or perform side effects; their Unit result is not a SQL predicate.
Expanding `with(context)` merely moved the false diagnostic to the next ordinary call.

## Cause and correction

`KronosParserTransformer` normalizes Unit-adapted lambda tails into returns so a Boolean expression
discarded by Kotlin still reaches the condition collector. `ConditionTransformer` also analyzed the
ordinary Unit or literal-null returns created by this normalization.

Before SQL analysis, preserve returns whose **value** is Unit or a literal-null constant. Do not
exclude a function just because its declared return type is Unit: a real Boolean condition tail has
already been restored to a Boolean value by the parser and must still generate SQL and parameters.
Retain return targets, ordinary side effects, forwarded condition collection, and the original
`with(context)` source structure. No package-name exception or warning suppression is needed.

## Warning regression tests must run after the backend

The official IR-input handler runs too early to assert these plugin warnings. The new callback suite
uses a JVM binary artifact handler, after plugin execution, and reads the module configuration's
actual CLI diagnostics collector. It rejects unexpected warnings only in its two focused cases.
Other tests that intentionally exercise unsupported expressions retain their own diagnostic policy.

The warning assertion was verified by running the same compiled official test classes against the
old and fixed complete compiler artifacts: the old artifact failed both cases specifically with
`Unexpected callback IR warnings: KRONOS_IR_WARNING`; the fixed artifact passed both. This catches a
warning regression even when runtime SQL happens to remain correct.

## Verified

- All 51 official condition tests passed, including exact SQL/parameter checks, single evaluation,
  `with` side effects, assignment tails, Unit-adapted Boolean predicates, and null returns.
- Both new testData files failed against the old CLI with `-Werror` and compiled without warnings
  against the fixed CLI.
- The fixed complete CLI SHA-256 is
  `e215452d26a4c1f3a6e6e7b5e08b7cfa1b106d31a5b54e26ed47713d3d7ad16e`.

Evidence: `/tmp/kronos-condition-return-official97.log`,
`/tmp/kronos-condition-return-probe95/{old-results,new-results}.json`, and
`/tmp/kronos-condition-warning-official-comparison102/verification.json`.
