# Installation snippets must enable DSL language features

A real Maven consumer of Kotlin 2.4.21 and the source-built Kronos compiler failed to compile `select { [it.id, it.name] }` when following the installation snippet: collection literals require `-Xcollection-literals`. Loading the Kronos plugin alone does not enable Kotlin language features.

Include `-Xcollection-literals` and `-Xcontext-sensitive-resolution` in Gradle `kotlin.compilerOptions.freeCompilerArgs` or the Kotlin Maven plugin's `<configuration><args>`. Apply the Kotlin JVM plugin in complete JVM installation snippets. Android flags belong in the app/library module, not a root `apply false` block; multiplatform flags belong in the shared Kotlin compiler options. Keep bilingual setup pages, README files, homepage install code, module instructions and the ORM guide aligned.

The corrected Maven configuration compiled and ran generated members, provider discovery, renamed columns and projections using compiler SHA `dc8ee0e2ba6d7121815b4b3973332bea4e9ab38a8bb3e0a94f0028bf271971d4`. Gradle Kotlin DSL and Groovy consumers passed the same runtime checks with the documented compiler options. Evidence: `/tmp/kronos-maven-consumer51/verification.json` and `/tmp/kronos-gradle-setup52/verification.json`.
