# Preserve the IDEA SDK runtime layout in editor tests

Kotlin Toolchain 0.13 accepts Maven dependencies for JVM compilation but has no JVM file-dependency schema. `scripts/ide.main.kts` stages unchanged SDK and official test-framework JARs into a private Maven repository for `:ide:compileJvmTest`.

Those Maven paths must not become the editor process classpath. IDEA resolves resources relative to its classes’ original locations: moving `intellij.java.impl.jar` causes `JDK annotations not found`, and moving Kotlin SDK JARs causes `KotlinArtifacts` initialization failure. Copying individual adjacent resources only exposes the next layout dependency.

Compile with the official Toolchain, then launch the catalog-pinned official JUnit ConsoleLauncher from the Kotlin script. Use the original SDK and test-framework JAR locations, the compiled test output, and resolved ordinary test dependencies. The pinned 0.13 compilation record supplies `jdk.home`, `inputFiles` and `outputFiles`; validate its schema and fail if any expected field or test class is absent. Keep SDK classes ahead of unrelated compiler artifacts and exclude duplicate JUnit platform engines supplied by the framework POM when using ConsoleLauncher standalone.

Select plugin `lib/*.jar` plus `lib/modules/**/*.jar`. Recursively including every plugin `lib` descendant also includes Kotlin’s `frontend-split` plugin, producing conflicting content-module IDs and preventing Kotlin and KEFS from loading.

The SDK repository stays under `build/ide/repository`, separate from the ORM publication/bootstrap repository. The test runner retains the real SDK launch VM options, isolated sandbox, current complete CLI compiler JAR, and strict standard-JUnit XML guard.

Declare JetBrains' `https://packages.jetbrains.team/maven/p/ij/intellij-dependencies` repository in `kronos-testing/ide/module.yaml`. The official test-framework POM depends on `org.jetbrains:jetCheck:0.2.2`, which Maven Central does not supply. The first hosted Toolchain IDE check at `e97b6d9b9` failed dependency resolution despite the local editor pass: a previously downloaded `.m2.cache` artifact had hidden the missing repository. Verify this fix with the `jetCheck` version directory moved out of the shared dependency cache as well as the actual editor suite; preserve checksum verification.

The repository fix passed all 26 editor contracts locally. A separate `:ide:resolveDependenciesJvmTest` run with the cached `jetCheck/0.2.2` directory moved aside downloaded the POM, JAR and both SHA-512 sidecars from that JetBrains repository successfully. Evidence: `/tmp/kronos-ide-repository-fix.log` and `/tmp/kronos-ide-jetcheck-cold-resolution.log`.

Verified on Intel macOS with IDEA 2026.2.3, embedded Kotlin `2.4.20-ij262-52`, KEFS `0.3.12-262`, and Toolchain 0.13.0: 26/26 real editor contracts passed with zero skips/errors/failures. This includes hover documentation and `it.rn` property navigation. All 65 loaded FIR/registrar classes matched the complete 151-class CLI artifact, SHA-256 `e215452d26a4c1f3a6e6e7b5e08b7cfa1b106d31a5b54e26ed47713d3d7ad16e`. Evidence: `build/ide/verification.json`, `build/ide/toolchain/reports/ide/jvm/TEST-junit-vintage.xml`, and `/tmp/kronos-ide-toolchain-final15.log`.
