# build-logic

Shared Gradle convention plugins for the Kronos ORM project.

## Plugins

### `kronos.publishing`
Maven Central publishing configuration:
- Group: `com.kotlinorm`
- Targets: Maven Central (release, GPG-signed), Central Portal Snapshots, Aliyun mirror
- POM: Apache 2.0 license, SCM, developers
- Aggregate tasks: `publishAllToMavenLocal`, `publishAllToMavenCentral`, `publishAllToCentralSnapshots`

### `kronos.dokka-convention`
Dokka API documentation:
- Configures Dokka DGP v2 per subproject
- Root `dokkaGenerateAll` task aggregates all subproject docs into `docs/<module>/`
