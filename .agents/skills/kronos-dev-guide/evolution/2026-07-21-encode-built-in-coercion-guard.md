# ENCODE Logical-Type Guards Must Preserve Built-In Coercion

## Symptom

ORM patch, subquery, DDL, cascade, and direct parameter tests failed with
`ValueMappingException` when a declared numeric or temporal field received a
legacy-compatible source such as `"42"` for `Int`.

## Cause

The Registry validated ENCODE input assignability before codec selection. This
correctly stopped user codecs from accepting arbitrary wrong logical inputs, but
also prevented `BasicValueCodec` and `TemporalValueCodec` from performing their
existing ENCODE coercions.

## Fix

When an ENCODE value does not match the logical target, allow the request only
if storage is `NONE` and the built-in Basic or Temporal codec already reports
support. Keep normal codec priority after admission, so user overrides still
work within the built-in conversion domain. A user codec alone must not expand
the set of mismatched logical inputs accepted by the guard.

## Prevention

Cover both sides of the boundary: legacy Basic/Temporal ENCODE coercion must
succeed, while an unrelated value such as `String` for an enum target must fail
before any user codec callback runs.
