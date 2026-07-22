# Temporal Codec Must Not Eagerly Resolve Optional JDBC Types

## Symptom

Official compiler FIR/JVM box tests failed with `NoClassDefFoundError:
java/sql/Date` while loading `TemporalValueSupportKt`. The failure appeared in
tests that only exercised ordinary KPojo mapping or DSL operations and did not
use JDBC temporal values.

## Cause

Top-level maps initialized with `typeOf<java.sql.Date>()`,
`typeOf<java.sql.Time>()`, and `typeOf<java.sql.Timestamp>()` during file class
initialization. The official generated-class runtime can omit the `java.sql`
module, so resolving those KTypes before any temporal request made the whole
codec class unloadable. Runtime `java.sql.*::class.java` checks would cause the
same failure when a broad source value entered temporal matching.

## Fix

- Dispatch ordinary `java.time`, `kotlin.time.Instant`, `java.util.Date`,
  `Long`, and `String` targets directly from complete KTypes with nullable/non-null
  matching helpers.
- Resolve the three JDBC exact/nullable KTypes and runtime Classes through one
  nullable lazy holder. Its initializer catches only linkage and invalid
  reflection/type resolution failures; an unavailable `java.sql` module disables
  JDBC matching instead of falling back to classifier-name dispatch.
- Use the same holder for declared source KType subtype checks and runtime
  `java.util.Date` subclass refinement. Runtime class fallback is reached only
  when declared source metadata is absent, broad, or unreliable.

## Prevention

Keep optional JDK modules out of top-level initializers. Add at least one
official generated-class box test that exercises a non-JDBC codec path under the
restricted classloader, plus ordinary JVM tests for JDBC exact/nullable types,
custom subclasses, and date-format/zone behavior.
