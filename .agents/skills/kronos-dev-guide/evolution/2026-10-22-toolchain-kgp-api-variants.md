# KGP API variants require upstream Toolchain disambiguation

Kotlin Toolchain 0.13 emits `More than a single variant provided` when resolving `org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.4.21` for `kronos-gradle-plugin`. The official API module has a base runtime variant plus Gradle-version-specific runtime variants. The build classpath includes all matching API JARs.

The exact upstream cause is `filterWellKnowSpecialLibraries` in [v0.13.0 maven.kt](https://github.com/JetBrains/kotlin-toolchain/blob/v0.13.0/sources/dependency-resolution/src/org/jetbrains/amper/dependency/resolution/maven.kt#L1169): it selects the variant without `org.gradle.plugin.api-version` for `kotlin-gradle-plugin` and `fus-statistics-gradle-plugin`, but omits `kotlin-gradle-plugin-api`. The frontend dependency/repository schema has no configurable Gradle API-version attribute or metadata-source selection. The ordinary dependency remains needed: Kronos uses `KotlinMultiplatformExtension`, `KotlinNativeTarget`, `KotlinMetadataTarget` and `KotlinJvmTarget`, which are absent from the API-only JAR.

Retain the official dependency metadata and compiler version. Do not patch cached `.module` files, downgrade Kotlin, filter away warnings, or republish upstream artifacts with guessed variant selection. An upstream resolver fix should extend its existing non-versioned variant rule to the API artifact. Gradle consumer projects use their own normal variant-aware resolution.

Evidence: the official KGP/KGP API 2.4.21 `.module` artifacts, local `build/incremental.state/_kronos-gradle-plugin_compileJvm-*`, the pinned Toolchain source above, and passing Windows JVM/MinGW checks at `73afe1802`. This upstream warning remains visible.
