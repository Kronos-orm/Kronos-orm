# Derive provider identities from module coordinates and actual compiler scope

## Contract

Toolchain modules configure `module-coordinate: com.kotlinorm:kronos-core` through the official
`settings.kotlin.compilerPlugins.options` mapping. Multiplatform modules additionally select
`jvm-source-set-prefix: jvm`; JVM-only modules use the empty default. The compiler expands this into the
existing `gradle:group:module::module:sourceSet` identity, its first 16 SHA-256 hex digits, and the original
JVM provider or Native metadata entry FQName. Existing Gradle and Maven explicit provider options remain
supported. Module YAML must not duplicate these derived hashes or FQNames.

The compiler's runtime module names are `kronos-core` and `kronos-core_test`. Metadata compilation instead
uses names such as `kronos-core_commonMain` and `kronos-core_nativeMain`; recognize these only for the
common metadata platform and retain FIR-only registration. Reject unknown runtime module/source-set
names rather than assigning them to main. IDE FIR-only registration may carry plugin options without
`MODULE_NAME`; keep the existing Missing mode in that case. Backend IR still rejects Missing identity.

## Resource and source-set distinction

The JVM ServiceLoader descriptor and the generated JVM class must use the same stable source-set prefix.
Keep that naming input distinct from the resource fragment: a JVM publication projection changes the
resource fragment from `@jvm` to the default fragment but must retain `jvmMain`/`jvmTest` provider identities.
The Native source generator and watchOS bridge use the same module-coordinate rule. The watch bridge
passes the coordinate to the compiler instead of computing and forwarding an explicit hash pair.

## Verified

Kotlin 2.4.21 / Toolchain 0.13.0:

- Native generator tests: 8/8, including exact original JVM main/test service names and Native manifest.
- Actual core common metadata compilation passed after recognizing metadata module names.
- Actual core macosX64 KLIB and syntax/core watchosX64 bridge compilation passed.
- Both actual Native KLIBs contain the original `KronosGeneratedTypeMetadata_1bfffc5c3b720294` entry and
  `gradle:com.kotlinorm:kronos-core::kronos-core:commonMain#1bfffc5c3b720294` provider ID.
- Actual JVM SQLite integration: 89/89 passed, zero failures or skips, consuming automatically named
  core/testing providers and actual database operations.

Evidence: `/tmp/kronos-module-coordinate-{native-plugin1,common-metadata2,native-macos-klib1,watch1,sqlite1}.log`
and `/tmp/kronos-module-coordinate-abi.json`.
