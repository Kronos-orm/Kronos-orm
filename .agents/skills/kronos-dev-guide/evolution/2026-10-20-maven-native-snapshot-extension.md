# Maven snapshot deployment must retain the primary file extension

## Observed failure

`snapshot_components()` correctly selected a Native `.klib` primary when its POM omitted packaging.
Maven Deploy Plugin 3.1.4 completed successfully but uploaded the bytes as `.jar`, because Maven's
default POM packaging determined the primary artifact extension. Snapshot metadata then had no
`klib` entry.

## Correction

Pass `-Dextension=<primary suffix>` to `deploy-file`. The extension parameter is supported since
3.1.3 and overrides packaging-based extension detection; see the official
[deploy-file parameter reference](https://maven.apache.org/plugins/maven-deploy-plugin/deploy-file-mojo.html).

Keep attachment classifiers aligned with files and types, including empty classifiers for Gradle
`.module` metadata and the MPP root metadata JAR. Maven 3.1.4 accepts those empty entries and
publishes their correct timestamped snapshot records.

## Verification

A local Maven deployment of three components to a fresh file repository checked timestamped
POM, real KLIB, module metadata, tooling metadata, sources and javadoc bytes, including a POM with
omitted packaging.

The full thirteen-component JVM publication was also deployed locally. All sixty payloads retained
their bytes, and a fresh Gradle consumer loaded the plugin marker and timestamped runtime/compiler
artifacts, executed the ORM test, and checked resolution against the original staged JAR hashes.
