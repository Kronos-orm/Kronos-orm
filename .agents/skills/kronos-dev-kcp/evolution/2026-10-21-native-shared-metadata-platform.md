# Native shared metadata uses a Native compiler platform

## Symptom

A complete Kotlin Toolchain publication failed with `Unsupported compiler module 'kronos-core_native'`.
JVM tests and individual Native runtime compilations had passed because they do not compile the shared
Native metadata fragments used by the multiplatform publication.

## Cause

The provider configuration recognized metadata only through `TARGET_PLATFORM.isCommon()`. Kotlin 2.4.21
instead compiles Toolchain's shared Native fragments with Konan, `-Xmetadata-klib`, a Native platform,
and module names such as `kronos-core_native`, `kronos-core_apple`, and `kronos-core_linux`.

The actual Native SDK's `K2Native.setupCommonArguments` calls the shared argument configurator, which
copies `CommonCompilerArguments.metadataKlib` into `CommonConfigurationKeys.METADATA_KLIB` before plugin
registration. This flag is available through the shared compiler API and requires no Native-only
compiler dependency.

## Fix

Treat a compilation as frontend-only metadata when its platform is Common, or when its platform is
Native **and** `METADATA_KLIB` is true. Accept only the known shared fragment names `common`, `native`,
`apple`, `ios`, `linux`, `macos`, `mingw`, `tvos`, and `watchos`, retaining the existing `Main` spellings.
Unknown modules and fragment suffixes remain errors. Native and JVM runtime compilations still derive
the original main/test provider identities; real IR compilation still rejects a missing identity.

## Verified

- The old artifact reproduced the publication failure in the real `compileMetadataNative` task.
- The source-built compiler passed all 25 CLI configuration tests, including the metadata flag,
  platform, fragment-name rejection, and unchanged JVM/Native provider ABI contracts.
- Real `:kronos-core:compileMetadataApple` and `:kronos-core:compileMetadataLinux` tasks passed, including
  their Common and Native metadata dependencies. The emitted manifests identify the Native platform,
  Kotlin 2.4.21, and the exact `_native`, `_apple`, and `_linux` module names.
- A real JVM compiler invocation without a provider identity still failed from
  `KronosIrGenerationExtension` with `Missing compiler option 'generated-provider-id'`.

Evidence: `/tmp/kronos-native-metadata-auto-repro62.log`,
`/tmp/kronos-native-metadata-{bootstrap63,config-tests64,apple65,linux66,ir-missing68}.log`.
