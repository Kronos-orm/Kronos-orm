# Share immutable SDK caches within a storage budget

The first parallel CI implementation keyed SDK downloads by every job lane. The four Apple producers each stored roughly 1.46GB of the same Toolchain SDK. The twenty obsolete PR-only v1 caches removed during verification occupied 10.78GiB, exceeding a 10GiB budget; older useful entries were absent from later API inventory.

Map compatible jobs to the `jvm`, `native` and `scripts` dependency families. Native SQLite consumers share an SDK entry by OS, architecture, Kotlin version and Xcode. Keep source-built Kronos artifacts, project task state and test reports outside these caches; downstream consumers receive verified artifacts from the current run.

Pure JVM/script downloads are portable across runner image revisions. The compiled main.kts namespace separately includes JAVA_HOME and all imported repository scripts. Native download caches retain the actual host image, with Xcode version included for the consumer SDK.

GitHub expression `env.ImageVersion` was empty in the generated cache keys even though the hosted shell supplied ImageVersion. Read it in the composite action shell and export an explicit GITHUB_ENV value for Native keys. The v2 API inventory confirmed real image identifiers, for example 20261013.0424.1, and a single common Apple Native producer cache rather than four copies.

Measure the cache API inventory and restore/save duration alongside job duration. A cache hit does not replace test execution. Old schema caches can be removed by exact PR ref and key prefix after their runs finish; preserve other refs and other cache categories.
