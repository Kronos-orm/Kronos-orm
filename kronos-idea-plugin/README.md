# kronos-idea-plugin

IntelliJ IDEA plugin for Kronos ORM.

This module merges the standalone Kronos-Orm IDEA plugin with the monorepo IDE
support module:

- `Kronos-ORM` tool window with the Kronos icon.
- Project settings page at `Kronos ORM Setting`.
- Code generation script templates under `templates/`.
- K2 analysis support for Kronos compiler-plugin features, including generated
  projection classes, projection context members, and FIR diagnostics.

It is intentionally separate from `kronos-gradle-plugin`: the Gradle plugin makes
command-line compilation work, while this IDE plugin makes editor analysis and
IDE tooling work.

## Resources

- Plugin logo: `src/main/resources/META-INF/pluginIcon.svg`
- Tool window icon: `src/main/resources/icons/kronos-circle.svg`
- Full icon asset: `src/main/resources/icons/kronos.svg`
- Code generation templates: `templates/all`, `templates/pojo`, `templates/service`

## Build

Use a local IDEA installation when developing against the same IDE build:

```powershell
.\gradlew.bat :kronos-idea-plugin:buildPlugin -Pkronos.idea.localPath="C:/Program Files/JetBrains/IntelliJ IDEA 262.8377.35"
```

or set `IDEA_HOME`. Without a local path, Gradle downloads the IDEA dependency
selected by `-Pkronos.idea.version=...`, defaulting to `2026.2`.

On macOS:

```bash
./gradlew :kronos-idea-plugin:buildPlugin \
  -Pkronos.idea.localPath="/Applications/IntelliJ IDEA 2026.2 EAP.app/Contents"
```

The packaged plugin is written to:

```text
kronos-idea-plugin/build/distributions/kronos-idea-plugin.zip
```

## Canary releases

After a pull request is merged into `main`, the `Canary GitHub Release` workflow
builds the IDEA plugin and the JVM jar artifacts, then publishes them to a
GitHub prerelease.

The canary IDEA plugin version uses:

```text
<base-project-version>-canary.<github-run-number>.<short-commit-sha>
```

The release notes record the project version, IDEA plugin version, Kotlin
version, IntelliJ IDEA target version, IntelliJ Platform Gradle Plugin version,
source pull request, commit SHA, branch names, workflow run URL, and checksums.
