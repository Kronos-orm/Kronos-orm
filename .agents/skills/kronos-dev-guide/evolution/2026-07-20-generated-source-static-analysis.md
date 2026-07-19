# Generated sources need durable static-analysis policy

## Symptom

A generated Kotlin source passes compilation but floods detekt with structural warnings or fails Codacy file-size metrics.

## Cause

Generated overload matrices intentionally repeat arity-specific declarations, while analyzer limits assume hand-written source. Editing only the generated file is also temporary because regeneration removes the fix.

## Confirmed approach

- Emit applicable Kotlin suppressions from the code-generation template as well as the checked-in generated output.
- Exclude the exact generated path from repository-level analyzers whose metrics cannot be suppressed in Kotlin.
- Keep hand-written generator logic lint-clean; generated-code exclusions must not hide the generator itself.
- Re-run the affected compiler tests and both static-analysis gates after changing this policy.
