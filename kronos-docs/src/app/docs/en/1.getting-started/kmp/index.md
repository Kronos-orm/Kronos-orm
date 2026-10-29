{% import "../../../macros/macros-en.njk" as $ %}

## Share models and queries

Put `KPojo` models and query code in `commonMain`. Each application supplies a database connection for its platform: JDBC on the JVM, `SQLiteDatabase` on Android/JVM, and a Native SQLite adapter on Apple, Linux and Windows.

The Native targets are:

- iOS: `iosArm64`, `iosSimulatorArm64`, `iosX64`.
- macOS: `macosArm64`, `macosX64`.
- tvOS: `tvosArm64`, `tvosSimulatorArm64`, `tvosX64`.
- watchOS: `watchosArm64`, `watchosDeviceArm64`, `watchosSimulatorArm64`, `watchosX64`.
- Linux: `linuxX64`, `linuxArm64`.
- Windows: `mingwX64`.

Android applications use the {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }} setup. The Android app consumes JVM bytecode; AndroidX SQLite is a driver library used by the Native example.

Building Apple targets requires macOS and Xcode 16 or newer. Check the selected Xcode with `xcodebuild -version`; when multiple versions are installed, set `DEVELOPER_DIR` to the intended `Contents/Developer` directory.

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
