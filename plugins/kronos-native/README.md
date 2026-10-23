# Kronos Native build plugin

Local Kotlin Toolchain plugin registered as `plugins/kronos-native` in the root `project.yaml`.
It generates Native metadata entry points and JVM provider resources, builds `watchosX64`,
and completes the Maven publications for all Native targets.

Release staging first publishes the unsigned artifacts, then runs the finalizer chain
syntax → core → logging. Syntax reads that preceding CLI publication through
`bootstrapStageRepository`; core and logging use `stageRepository` to depend on the
previous module's finalized output. Both inputs participate in incremental checks.

`native-targets.json` is the shared target list for the plugin and publication gates.
The plugin owns its `module.yaml`, `plugin.yaml`, `src/` implementation and `test/` tests.
Production modules enable it explicitly in their own `module.yaml`.

Shared Kotlin, JDK and publishing settings live in `templates/common.module-template.yaml`.
Runtime consumer modules declare the compiler dependency and module coordinate through
`settings.kotlin.compilerPlugins.options`; provider names are derived automatically.
`templates/kronos-runtime.module-template.yaml` keeps compilation-wide provider emission non-incremental.
Run repository commands through `scripts/run build`, and stage releases through `scripts/run release`.

See the official [local plugin structure](https://kotlin-toolchain.org/latest/user-guide/plugins/topics/structure/).
