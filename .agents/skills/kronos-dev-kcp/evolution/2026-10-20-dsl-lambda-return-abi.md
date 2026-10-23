# Transformed DSL lambdas must retain their returns

## Observed failure

The real Native ORM console inserted a SQLite row successfully, then exited with SIGILL before the
conditioned SELECT completed. Disassembly showed that a transformed Boolean condition lambda called
the SQL-expression setter and fell through to `ud2`: its original `IrReturn` had been removed.

## Verified correction

Condition, select, sort, reference and insert-select transformers retain the original return and target
symbol after collecting DSL side effects. The shared helper substitutes Unit or false for Boolean,
and uses the compiler's `IrConstImpl.defaultValueForType` for other declared return types. Public DSL
aliases allow callers to select a covariant primitive or reference return type, so the replacement
must retain that calling convention. It never reevaluates the user's original expression.

Explicit Unit lambdas can end with `IMPLICIT_COERCION_TO_UNIT` instead of an `IrReturn`. Normalize that
tail for the same field collectors, then restore it if no collection took place. Preserve forwarding
extension-lambda calls: an exact built-in extension `Function2.invoke` on the same DSL receiver already
collects values in the delegated lambda and must not add a second value. Ordinary Function2 callbacks
still need collection. Set transformation appends its own writes and is excluded from this tail rewrite.

The Native console now completes the insert and conditioned SELECT and reports id=1 with the inserted
name. The first 42 SQLite cases passed on both AndroidX and C backends, including real ORM CRUD,
enum/serialized decoding, upsert/pagination and renamed generated-key fields. Official box regressions
also verify explicit nullable/non-null primitive and reference returns, Unit collection, delegated and
ordinary callbacks, captured-value evaluation counts, collected metadata and labeled/nested returns.

Preserving the side effects alone is insufficient. Every transformed exit must still return through
its original function target with a value compatible with that function's return type.
