# Use kotlin.time.Instant in platform temporal codecs

## Symptom

Kotlin 2.4.21 reports the `kotlinx.datetime.Instant` alias as deprecated in both JVM and Native platform
codec implementations and their JVM tests. The project uses kotlinx-datetime 0.8.0, whose instant type
is the standard-library `kotlin.time.Instant`.

## Fix

- Import and test `kotlin.time.Instant` directly. Local date/time types and timezone conversions remain
  in kotlinx-datetime.
- Keep accepting both the `kotlinx.datetime.Instant` and `kotlin.time.Instant` descriptor keys. Existing
  model metadata and explicit `StaticTypeRef` callers may retain the earlier spelling.
- Decode ISO instant text with `Instant.parse`; local timestamp text falls back to
  `LocalDateTime.parse(...).toInstant(zone)`. This already returns a standard instant, so rebuilding
  it from seconds/nanoseconds is unnecessary. Preserve nanoseconds and the configured timezone.
- Keep these optional kotlinx-datetime conversions in the platform bridge rather than adding the
  dependency to the shared codec's common implementation.

## Verified

The actual JVM temporal/static codec selection passed all 21 tests. Its existing local timestamp
contract now checks nine fractional digits through both descriptor spellings. The real
`:kronos-core:compileMetadataApple` task passed, including shared Common and Native metadata; the
deprecated alias warnings disappeared from both platform implementation compilations.

Evidence: `/tmp/kronos-temporal-jvm87.log` and `/tmp/kronos-temporal-native88.log`.
