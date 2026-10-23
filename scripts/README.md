# Repository commands

Run from the repository root with JDK 21. `scripts/run` downloads the official Kotlin SDK selected by
`libs.versions.toml`, verifies its SHA256 and caches it. Windows uses `scripts\run.cmd`.

```sh
scripts/run build build -m kronos-core -p jvm
scripts/run build test -m kronos-core -p jvm
scripts/run build test -m kronos-gradle-plugin -p jvm
scripts/run ci integration
scripts/run ide test
scripts/run quality coverage --module kronos-core
scripts/run quality dokka
scripts/run release stage --repository build/release/repository
scripts/run version show
```

Each command lives in its corresponding `*.main.kts` file. `common.main.kts` provides catalog access,
process execution, XML/JSON handling and integration settings. The launcher generates dependency
annotations under the ignored `build/scripts/` directory from the catalog.

`build.main.kts` bootstraps the compiler from source and delegates to the Kotlin Toolchain wrapper.
`ci.main.kts` runs JVM, Native and database integration tests. Android, Spring Boot, Ktor and Vert.x
examples verify Gradle consumption of staged publications; Solon and Spring Boot examples verify Maven integration.
`ide.main.kts` stages KEFS coordinates and runs the real editor module in `kronos-testing/ide`.
`quality.main.kts` invokes the official Kover and Dokka CLIs and generates JOIN sources.
`release.main.kts` prepares and publishes Maven artifacts. `version.main.kts` keeps publication versions aligned.

Run root Toolchain commands one at a time because they share build outputs.

Database defaults are loaded automatically. Local overrides use `kronos-testing/env.local.properties`;
existing environment variables take precedence. `scripts/run ci env` exports these settings to
`GITHUB_ENV` in GitHub Actions. `scripts/run ci reflect-check` verifies production core sources;
`scripts/run ci collect-release` collects published binaries and release checksums.

CI is orchestrated by `.github/workflows/kronos-kmp-testing.yml`. A single JVM candidate feeds
the Android APK build, server examples, compatibility matrix and KEFS. Full KMP metadata is
assembled from the complete project, followed by six Native publication groups and their
platform consumers. The final repository combines the verified groups without recompiling them.

`scripts/run` namespaces the main.kts compilation cache by the shared script contents.
`KRONOS_COMPILER_REPOSITORY` selects a source-SHA- and digest-verified CI compiler candidate.
`release metadata-stage`, `shard-stage`, `assemble`, and `check-candidate` implement the artifact handoff.
SDK and dependency caches are independent of source candidates and test reports.
