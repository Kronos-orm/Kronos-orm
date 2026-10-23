# Native composite consumer links need explicit JVM metaspace

## Symptom

A Kotlin 2.4.21 Native consumer includes Kronos sources as a Gradle composite. Main and Native test KLIB
compilation succeed, but `linkDebugTestMacosX64` fails with `OutOfMemoryError: Metaspace` while the Native
cache builder loads compiler classes. The daemon may then report a secondary lost-message error.

## Confirmed cause and fix

The example's implicit Gradle defaults were 512 MiB heap and 384 MiB metaspace. The composite loads Kotlin
Gradle DSL, compiler-plugin compilation, and the Kotlin/Native backend into the build process. Set an
explicit budget in the consumer's `gradle.properties`, for example:

```properties
org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8
```

Re-running the same Kotlin 2.4.21 / macOS x64 test link with this setting reached execution of all 26
real SQLite tests. The tests surfaced two actual data/fixture failures, which must be fixed independently;
successful linking does not imply those tests passed.

## Prevention

Distinguish Native link resource failures from source/IR errors and runtime assertions. Inspect the first
exception before following secondary daemon-communication failures. Stop only a known failed build's own
orphan daemon if needed; global `gradlew --stop` disrupts concurrent agents' builds.
