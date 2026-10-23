# AndroidX Native SQLite TEXT reads must preserve embedded NUL

## Symptom

A real Native SQLite round trip through AndroidX bundled SQLite 2.6.2 stored `Kronos\u0000数据库🚀` but the
physical TEXT reader returned only `Kronos`. Ordinary Unicode strings passed, so a Unicode-only test missed
this truncation.

## Confirmed cause

The official 2.6.2 `sqlite-framework` sources contain `NativeSQLiteStatement`. Its text binding uses UTF-16
with an explicit length. Its text accessor decodes a zero-terminated UTF-16 pointer, so an embedded NUL
ends the decoded Kotlin string early. The same statement's blob accessor performs an explicit-length
`sqlite3_column_blob`/`sqlite3_column_bytes` read and has no storage-class restriction.

Source artifact: https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-framework/2.6.2/sqlite-framework-2.6.2-sources.jar
Relevant source: `nativeMain/androidx/sqlite/driver/NativeSQLiteStatement.kt`.

## Verified adapter fix

The example's AndroidX driver reads a TEXT cell with `delegate.getBlob(index).decodeToString()`. Keep the
row's original SQLite storage type and physical `Text` result. Keep `bindText` and SQL unchanged; a byte
accessor used while reading does not make the logical database value a BLOB.

`macosX64Test` passed all 26 real Native SQLite tests on 2026-10-18, including the exact embedded-NUL,
Chinese-character and emoji round trip. The desktop SQLite connection/insert/query executable also passed.
These tests used an isolated Kronos HEAD execution-contract baseline, not the in-flight full ORM migration.

## Prevention

Include embedded NUL alongside non-ASCII text when testing a C-backed text boundary. Check explicit byte
lengths rather than assuming that all SQLite TEXT values are C strings. Do not silently truncate or weaken
the test to the truncated prefix.
