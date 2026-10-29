{% import "../../../macros/macros-en.njk" as $ %}

## Share models and queries

Put `KPojo` models and query code in shared sources: `src/` for Toolchain or `commonMain` for Gradle. Each application supplies a database connection for its platform: JDBC on the JVM, `SQLiteDatabase` on Android/JVM, and a Native SQLite adapter on Apple, Linux and Windows.

The Native targets are:

- iOS: `iosArm64`, `iosSimulatorArm64`, `iosX64`.
- macOS: `macosArm64`, `macosX64`.
- tvOS: `tvosArm64`, `tvosSimulatorArm64`, `tvosX64`.
- watchOS: `watchosArm64`, `watchosDeviceArm64`, `watchosSimulatorArm64`, `watchosX64`.
- Linux: `linuxX64`, `linuxArm64`.
- Windows: `mingwX64`.

Android applications use the {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }} setup. The Android app consumes JVM bytecode; AndroidX SQLite is a driver library used by the Native example.

Building Apple targets requires macOS and Xcode 16 or newer. Check the selected Xcode with `xcodebuild -version`; when multiple versions are installed, set `DEVELOPER_DIR` to the intended `Contents/Developer` directory.

## Configure Kotlin Toolchain

Use Kotlin Toolchain `0.13.0` with Kotlin `2.4.21`. [Download the complete multi-module example](/assets/downloads/kotlin-toolchain.zip) and keep its wrapper, shared configuration and preparation script. It contains the shared `models` library, a `jvm-app` application and a macOS `native-app` application.

```text name="Project layout"
project.yaml
common.module-template.yaml
kotlin / kotlin.bat
prepare.main.kts
models/
  module.yaml
  src/example/Person.kt
  test/example/ProjectionTest.kt
jvm-app/
  module.yaml
  src/example/Main.kt
native-app/
  module.yaml
  src/example/Main.kt
```

`project.yaml` lists the three modules. The shared template sets the Kotlin version and DSL options:

```yaml group="Toolchain project" name="project.yaml"
modules:
  - jvm-app
  - models
  - native-app
```

```yaml group="Toolchain project" name="common.module-template.yaml"
settings:
  kotlin:
    version: 2.4.21
    languageVersion: 2.4
    apiVersion: 2.4
    compileIncrementally: false
    freeCompilerArgs:
      - -Xcollection-literals
      - -Xcontext-sensitive-resolution
  jvm:
    release: 8
    jdk:
      version: 21
```

The shared library resolves `kronos-core` for each target and exports it to both applications. Configure the compiler plugin in the unqualified module `settings`:

```yaml name="models/module.yaml"
product:
  type: kmp/lib
  platforms: [jvm, macosArm64, macosX64, iosArm64, iosSimulatorArm64, linuxX64]

apply:
  - //common.module-template.yaml

dependencies:
  - com.kotlinorm:kronos-core:{{ $.kronosVersion() }}: exported

settings:
  kotlin:
    compilerPlugins:
      - id: kronos-compiler-plugin
        dependency: com.kotlinorm:kronos-compiler-plugin:{{ $.kronosVersion() }}
        options:
          module-coordinate: example:models
          jvm-source-set-prefix: 'jvm'
```

Both applications depend on `../models` and load the compiler plugin in their own module, so they can write queries and consume projections returned by the shared library:

```yaml group="Toolchain applications" name="jvm-app/module.yaml"
product: jvm/app

apply:
  - //common.module-template.yaml

dependencies:
  - ../models

settings:
  kotlin:
    compilerPlugins:
      - id: kronos-compiler-plugin
        dependency: com.kotlinorm:kronos-compiler-plugin:{{ $.kronosVersion() }}
        options:
          module-coordinate: example:jvm-app
          jvm-source-set-prefix: ''
```

```yaml group="Toolchain applications" name="native-app/module.yaml"
product:
  type: macos/app
  platforms: [macosArm64, macosX64]

apply:
  - //common.module-template.yaml

dependencies:
  - ../models

settings:
  native:
    entryPoint: example.main
  kotlin:
    compilerPlugins:
      - id: kronos-compiler-plugin
        dependency: com.kotlinorm:kronos-compiler-plugin:{{ $.kronosVersion() }}
        options:
          module-coordinate: example:native-app
          jvm-source-set-prefix: ''
```

### Prepare and run

Install the standard Kotlin SDK and run the preparation script with `kotlinc`. `./kotlin` is the Toolchain wrapper included in the example. The script derives main and test type registrations from each module’s `module-coordinate`. When adding a module, register it in `project.yaml` and update the script’s `modules` list: `jvmScope` selects its JVM source set and `nativeDependencies` lists its Native module dependencies. Run the script again after changing coordinates or dependencies; generated type names do not need to be maintained by hand.

```bash name="JVM" icon="terminal"
kotlinc -script prepare.main.kts
./kotlin run -m jvm-app
./kotlin test -m models -p jvm
```

Run the Native application and shared-model tests on an Apple Silicon Mac. Use `macosX64` on an Intel Mac:

```bash name="macOS" icon="terminal"
./kotlin run -m native-app -p macosArm64
./kotlin test -m models -p macosArm64
```

Toolchain uses `src/` for shared sources and directories such as `src@jvm/` and `src@native/` for platform sources. Gradle uses `commonMain`, `jvmMain` and `nativeMain`. The models and queries below work with either build tool. Applications supply their database connections; see {{ $.keyword("database/native-sqlite", ["Native SQLite"]) }} for SQLite driver integration.

## Configure Gradle

Use Kotlin `2.4.21`. Keep Kronos dependencies and its Gradle plugin on the same version through the version catalog.

```toml name="gradle/libs.versions.toml" icon="code"
[versions]
kotlin = "2.4.21"
kronos = "{{ $.kronosVersion() }}"

[libraries]
kronos-core = { module = "com.kotlinorm:kronos-core", version.ref = "kronos" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kronos = { id = "com.kotlinorm.kronos-gradle-plugin", version.ref = "kronos" }
```

Configure `mavenCentral()` for dependencies and `gradlePluginPortal()` for plugins, then select the targets your application uses.

```kotlin name="shared/build.gradle.kts" icon="gradlekts"
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kronos)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcollection-literals", "-Xcontext-sensitive-resolution")
    }

    jvm()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kronos.core)
        }
    }
}
```

Apply the Kronos plugin to each Kotlin module that declares models or queries. Dependencies on `kronos-core` use the same coordinate across targets; Gradle selects the matching platform artifact.

## Define a shared model

```kotlin name="commonMain/Person.kt" icon="kotlin"
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("person")
data class Person(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null
) : KPojo
```

Use Kotlin primitives, nullable values, enums, `ByteArray` and `kotlin.time.Instant` in shared models. Configure serialized fields through a value codec; see {{ $.keyword("mapping/serialization", ["Serialization"]) }}. Java temporal, decimal, UUID and JDBC types belong in JVM source sets.

## Initialize and connect

Call `initializeKronosMetadata()` once in the final Kotlin application or framework module, before the first database operation. Then install the platform connection.

```kotlin name="commonMain/Database.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KronosExecutionDataSource
import com.kotlinorm.utils.initializeKronosMetadata
import com.kotlinorm.wrappers.KronosExecutionWrapper

fun configureDatabase(source: KronosExecutionDataSource): KronosExecutionWrapper {
    initializeKronosMetadata()
    val wrapper = KronosExecutionWrapper(source)
    Kronos.dataSource = { wrapper }
    Kronos.timeZone = "UTC"
    return wrapper
}
```

For a Native SQLite connection, use {{ $.keyword("database/native-sqlite", ["Native SQLite"]) }}. JVM applications can use {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }} and `KronosJdbcWrapper` directly.

## Use the shared DSL

The same model and query code runs against the configured connection.

```kotlin name="commonMain/People.kt" icon="kotlin"
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select

wrapper.table.createTable(Person())
val inserted = Person(name = "Ada", age = 28).insert().execute()
val people = Person().select().where { it.age >= 18 }.toList()
```

For a floating-point SQL average, use `avgDouble`:

```kotlin name="commonMain/Average.kt" icon="kotlin"
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.avgDouble

val average = Person()
    .select { f.avgDouble(it.age).alias("average") }
    .first<Double>()
```

`avgDouble` returns `Double?` and uses floating-point precision. JVM decimal aggregates use `java.math.BigDecimal`.

Database operations are synchronous. Run them on a background executor or dispatcher in applications with a UI, and close the platform connection when its owner finishes using it.
