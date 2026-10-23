# Toolchain local SNAPSHOTs need a Maven descriptor

## Symptom

A Kotlin Toolchain 0.13 consumer reports `Unable to download checksums` for a SNAPSHOT POM or
module descriptor even though the ordinary JAR and POM exist in its configured Maven Local.
Adding checksum files alone does not fix resolution.

## Cause

`SnapshotDependencyFile` in the Toolchain resolver's `files.kt` accepts local SNAPSHOT files only
when `maven-metadata-local.xml` registers them. It requires `versioning/snapshot/localCopy=true`
and a matching `snapshotVersions/snapshotVersion` entry for the file extension. That entry's
UTC `updated` timestamp must be at least the file's actual modification time rounded to seconds.

## Correction

When copying the source-built compiler coordinate for IDE import, write the version-directory
descriptor after the JAR, POM and checksums. Register both `jar` and `pom`, retain the ordinary
SNAPSHOT version, and timestamp the descriptor after the artifacts have been written.

The same requirement applies to complete release staging: Native finalization changes POM and
Gradle module metadata after the original publisher creates the local descriptor. In
`scripts/release.main.kts`, refresh every staged SNAPSHOT coordinate after Native finalization, Gradle
marker creation, KEFS staging and documentation attachment, then regenerate checksums. Register
each payload extension and classifier. Round the UTC timestamp up to the next second so a newly
written payload cannot appear newer than its descriptor.

Keep build bootstrap artifacts in the project-private repository. Copy only the current ordinary
Kronos compiler coordinate to the IDE importer's Maven Local; KEFS compatibility artifacts remain
in the private repository selected by `.idea/kotlin-plugins.xml`.

## Verification

An isolated Toolchain 0.13 project resolved the staged ordinary compiler from a temporary Maven
Local and compiled successfully. Its actual `-Xplugin` argument pointed to the copied JAR.
An actual complete fifteen-target staging copy passed the publication gate after `finish_stage`:
168 binary payload hashes stayed identical, 58 version descriptors registered all 285 payloads,
and every descriptor had current timestamps and all four valid checksum sidecars. A fresh
Toolchain project compiled against its core, syntax, logging and JDBC coordinates successfully.

Toolchain caches failed dependency resolutions in the consuming project's `build/incremental.state`.
Use a fresh consumer or build directory when verifying corrected local metadata; refreshing
descriptors alone does not invalidate an earlier failed project state.
