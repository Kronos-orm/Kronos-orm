# Kotlin Multiplatform with Kronos

Use one Kotlin model and one set of queries in a server, Android app and Native application, each with its own database connection. Kronos puts entity mapping, SQL construction, typed projections and database operations in shared code. Platform code opens the connection, schedules database work and manages resources. This article follows a `Person` model from project configuration to real SQLite operations.

<figure class="article-cover">
<img src="/assets/images/features/img-1.png" alt="Shared Kotlin models and queries connect desktop, Android and iOS applications to their own databases" width="1672" height="941">
<figcaption>Shared code, independent connections: each application runs the same KPojo and queries through its platform connection.</figcaption>
</figure>

## Decide what belongs in shared code

Put `KPojo` models, business queries, SQL expressions and result-processing logic in `commonMain`. Platform source sets provide the driver, database file location, connection lifetime and scheduling between database work and the UI.

“Find adult users” is shared logic. Choosing an Android database directory or triggering a query from SwiftUI belongs to the application. Shared queries execute through the wrapper configured for the current platform; each application owns its connection.

<figure class="article-figure">
<img src="/assets/images/blog/kmp-platform-connections.svg" alt="commonMain models and queries execute through JDBC, Android SQLiteDatabase and a Native SQLite adapter" width="1000" height="420" loading="lazy">
<figcaption>Share models and SQL; keep connections and drivers on the platform.</figcaption>
</figure>

| Platform | Database connection | Using the shared code |
| --- | --- | --- |
| JVM servers and desktop apps | A JDBC driver and `KronosJdbcWrapper` | Use the same models, queries and transaction API |
| Android/JVM | An application-owned `AndroidSQLiteDataSourceWrapper` around `SQLiteDatabase` | Call shared queries on the database executor |
| iOS, macOS, tvOS and watchOS | A Native SQLite adapter; the reference application uses AndroidX bundled SQLite | Initialize metadata in the final Kotlin application or framework |
| Linux | A Native SQLite adapter; the reference application uses AndroidX bundled SQLite | Call shared queries from the desktop executable |
| Windows | A Native SQLite adapter; the reference application uses a SQLite C API driver | Build and run the application with `mingwX64` |

Android/JVM is Android's JVM compilation target. AndroidX SQLite is a driver library used by the Native reference application for bundled SQLite. See [Android SQLite](/documentation/en/database/android-sqlite) and [Native SQLite](/documentation/en/database/native-sqlite) for their connection implementations.

## Configure dependencies in a version catalog

This example uses a Gradle KMP project with Kotlin `2.4.21`. Keep the Kronos plugin and runtime on the same version in `gradle/libs.versions.toml`.

```toml
[versions]
kotlin = "2.4.21"
kronos = "0.4.0"

[libraries]
kronos-core = { module = "com.kotlinorm:kronos-core", version.ref = "kronos" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kronos = { id = "com.kotlinorm.kronos-gradle-plugin", version.ref = "kronos" }
```

Configure `gradlePluginPortal()` for plugin resolution and `mavenCentral()` for dependencies. Apply the Kronos plugin to each Kotlin module that declares models or queries. This shared module targets JVM, iOS, macOS, Linux and Windows:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kronos)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcollection-literals",
            "-Xcontext-sensitive-resolution"
        )
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

The `kronos-core` coordinate is shared across targets; Gradle selects the appropriate platform artifact. `-Xcollection-literals` enables query syntax such as `[it.id, it.name]`, while `-Xcontext-sensitive-resolution` enables Kotlin's context-sensitive name resolution. See the [Kotlin Multiplatform guide](/documentation/en/getting-started/kmp) for target coverage and complete setup.

## Define a model in commonMain

Place this model in the shared module's `commonMain`. `@Table` selects the table name, and `@PrimaryKey(identity = true)` declares a database-generated primary key.

```kotlin
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

The compiler generates field information and data mapping for `KPojo`. Shared models can use Kotlin primitives, nullable properties, enums, `ByteArray` and `kotlin.time.Instant`. Keep Java time and JDBC-specific types in JVM source sets. Configure [ValueCodec and serialization](/documentation/en/mapping/serialization) when business values require database encoding.

Models and queries can live in separate modules. Apply the Kronos plugin to both, depend on them from the final application, and initialize Kronos at the application entry point.

## Initialize Kronos in the final application

Call `initializeKronosMetadata()` once in the final Kotlin application or framework module before ORM operations, then configure its platform connection. This function accepts an application-provided data source and bridges it to the shared ORM API:

```kotlin
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

The initialization entry point belongs to the final application or framework, where it connects generated model factories and enum metadata from the application's dependencies. `Kronos.dataSource` selects the wrapper used by subsequent shared queries. JVM applications can also configure `KronosJdbcWrapper` as shown in [Database connections](/documentation/en/database/connect-to-db).

## Open a real Native SQLite connection

The [Native reference application](https://github.com/Kronos-orm/kronos-example-ios) includes `KronosSqliteDataSource`, platform drivers, desktop executables and a SwiftUI app. After adding its adapter to your application, a platform entry point can use the initialization function above:

```kotlin
import com.kotlinorm.example.ios.KronosSqliteDataSource

val source = KronosSqliteDataSource(databaseName = ":memory:")
val wrapper = configureDatabase(source)
source.open()

// Run the schema, CRUD and transaction examples on this connection.
// Close the connection when its owning database object finishes using it.
source.close()
```

An in-memory database lives as long as its connection. To persist data, pass a file path in the application's writable data directory as `databaseName`. The iOS app chooses its storage directory, while Android components manage their connection lifetime. Shared query functions remain independent of those paths.

The [Android example](https://github.com/Kronos-orm/kronos-example-android) uses `SQLiteDatabase` with an application-owned wrapper. See [Native SQLite](/documentation/en/database/native-sqlite) for generated keys, transactions, savepoints and connection shutdown.

## Share a complete CRUD flow

Once initialization and connection setup are complete, use the same Kotlin DSL for schema creation and data operations:

```kotlin
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update

wrapper.table.createTable(Person())

val inserted = Person(name = "Ada", age = 28)
    .insert()
    .withId()
    .execute()

val saved = Person()
    .select()
    .where { it.name == "Ada" }
    .first()

Person(id = saved.id, name = "Grace")
    .update { it.name }
    .by { it.id }
    .execute()

val adults = Person()
    .select()
    .where { it.age >= 18 }
    .toList()

Person(id = saved.id)
    .delete()
    .by { it.id }
    .execute()
```

`withId()` requests a generated key, available as `inserted.lastInsertId`. The reference SQLite adapter reads the key on the same connection and execution path as the insert. Query conditions read model properties, which Kronos translates into SQL and parameters for the database.

## Select only the columns you need

A screen often needs only part of a model. Declare the output fields with `select`; the result row then exposes the corresponding Kotlin properties:

```kotlin
val names = Person()
    .select { [it.id, it.name] }
    .where { it.age >= 18 }
    .toList()

val firstName: String? = names.firstOrNull()?.name
```

Within this query, `where` reads the input model `Person`. It can filter by `age` even though the output contains only `id` and `name`. Give computed expressions a result-property name with `.alias("name")`. See [Projections](/documentation/en/query/projection) for result rows, subsequent queries and ordering contexts.

## Group writes in a transaction

Transactions and savepoints use the same connection. This example keeps the first insert and rolls back the optional insert after a savepoint:

```kotlin
Kronos.transact(wrapper = wrapper) {
    Person(name = "Keep", age = 30).insert().execute()
    val checkpoint = savepoint("optional_person")
    Person(name = "Discard", age = 20).insert().execute()
    rollbackToSavepoint(checkpoint)
    releaseSavepoint(checkpoint)
}
```

The transaction commits when the callback completes normally and rolls back when it throws. Savepoint tokens belong to their transaction callback. Database operations are synchronous; Android and iOS applications should invoke shared queries on their background executor or dispatcher and deliver the result back to the UI.

## Understand shared queries in the editor

Install [Kotlin External FIR Support (KEFS)](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support) so Kotlin editor analysis understands generated projection properties, their types and DSL diagnostics. Choose versions from the [Kronos ORM / KEFS / IDEA compatibility table](/documentation/en/resources/kefs), then configure `com.kotlinorm:kronos-compiler-plugin`.

Gradle projects use their normal project import. Source projects using Kotlin Toolchain also install its official build-model plugin. Confirm `Successfully Loaded` in KEFS Diagnostics after setup.

## Continue with a runnable application

The Native reference application includes platform entry points, database connections and tests. Run these commands to exercise the path described above:

```bash
# Apple Silicon macOS: real SQLite tests and desktop executable
./gradlew :shared:macosArm64Test :shared:runDebugExecutableMacosArm64

# Linux x64: real SQLite tests and desktop executable
./gradlew :shared:linuxX64Test :shared:runDebugExecutableLinuxX64
```

On Windows, run `gradlew.bat :shared:mingwX64Test :shared:runDebugExecutableMingwX64`. For iOS, generate the Xcode project with XcodeGen as described in the example, then run the `KronosIosApp` scheme. Android has its own reference application.

Continue with [KMP setup](/documentation/en/getting-started/kmp), [Android SQLite](/documentation/en/database/android-sqlite), [Native SQLite](/documentation/en/database/native-sqlite), [Transactions](/documentation/en/database/transactions) and [KEFS editor support](/documentation/en/resources/kefs).
