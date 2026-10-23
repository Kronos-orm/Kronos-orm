{% import "../../../macros/macros-en.njk" as $ %}

## Connect a Native application

Use shared `KPojo` models and queries with an application-owned SQLite connection. Follow {{ $.keyword("getting-started/kmp", ["Kotlin Multiplatform"]) }} for Gradle configuration.

The [Native reference application](https://github.com/Kronos-orm/kronos-example-ios) contains the connection adapter, platform drivers, desktop executable and iOS app. Its `KronosSqliteDataSource` implements `KronosExecutionDataSource`; the core `KronosExecutionWrapper` connects it to the ORM.

Apple and Linux use AndroidX bundled SQLite. Windows uses the example's SQLite C API driver. Both drivers execute the same ORM and connection tests. Driver construction, native SQLite sources and connection lifetime belong to the application.

## Create a connection

Copy the reference adapter and the driver source sets into your application, or start from the reference project. With `KronosSqliteDataSource` available, initialize Kronos and open a database.

```kotlin name="nativeMain/Database.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.example.ios.KronosSqliteDataSource
import com.kotlinorm.utils.initializeKronosMetadata
import com.kotlinorm.wrappers.KronosExecutionWrapper

val source = KronosSqliteDataSource(databaseName = ":memory:")
val wrapper = KronosExecutionWrapper(source)

fun openDatabase() {
    initializeKronosMetadata()
    source.open()
    Kronos.dataSource = { wrapper }
}

fun closeDatabase() {
    source.close()
}
```

`:memory:` keeps one database for the connection's lifetime. To persist data, pass a file path in the application's writable data directory as `databaseName`.

## Create, write and query

The following operations use the `Person` model from the KMP guide and the same connection.

```kotlin name="nativeMain/People.kt" icon="kotlin"
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update

wrapper.table.createTable(Person())
val inserted = Person(name = "Ada", age = 28).insert().withId().execute()
val saved = Person().select().where { it.name == "Ada" }.first()

Person(id = saved.id, name = "Grace")
    .update { it.name }
    .by { it.id }
    .execute()

val people = Person().select { [it.id, it.name] }.toList()
Person(id = saved.id).delete().by { it.id }.execute()
```

Call `withId()` to request the generated key. `inserted.lastInsertId` comes from the same connection and execution that performed the insert. A write affecting no rows returns no generated key.

## Transactions and savepoints

Use `Kronos.transact` with SQLite's default transaction settings. An exception rolls back the transaction; savepoints let a block roll back part of its work.

```kotlin name="nativeMain/Transactions.kt" icon="kotlin"
Kronos.transact(wrapper = wrapper) {
    Person(name = "Keep", age = 30).insert().execute()
    val checkpoint = savepoint("optional_person")
    Person(name = "Discard", age = 20).insert().execute()
    rollbackToSavepoint(checkpoint)
    releaseSavepoint(checkpoint)
}
```

Keep transaction scopes and savepoint tokens inside their transaction callback. Keep row reads inside their mapper callback; return mapped values to the caller.

## Connection lifetime and concurrency

Create one data source for each database owner. The reference adapter serializes access to its connection and keeps generated-key retrieval with the corresponding write. Execute a transaction on its owning thread.

`close()` releases the connection and statements. An active transaction is rolled back and its scopes become invalid. `reopen()` explicitly starts a new connection; reopening an in-memory database creates an empty database.

## Run the examples

The example includes desktop executables and a SwiftUI application. Run the Gradle command matching the host:

```bash name="Apple Silicon macOS" icon="terminal"
./gradlew :shared:macosArm64Test :shared:runDebugExecutableMacosArm64
```

```bash name="Intel macOS" icon="terminal"
./gradlew :shared:macosX64Test :shared:runDebugExecutableMacosX64
```

```bash name="Linux x64" icon="terminal"
./gradlew :shared:linuxX64Test :shared:runDebugExecutableLinuxX64
```

```bash name="Windows x64" icon="terminal"
./gradlew :shared:mingwX64Test :shared:runDebugExecutableMingwX64
```

Windows builds use MinGW GCC and `ar` on `PATH`. The iOS app's `iosApp/project.yml` generates an Xcode project with XcodeGen; open its `KronosIosApp` scheme to run the app and its UI test.

The repository's [KMP workflow](https://github.com/Kronos-orm/Kronos-orm/actions/workflows/kronos-kmp-testing.yml) runs the reference application with the current ORM sources on Apple, Linux and Windows. Linux ARM64 test executables are linked on x64 and executed on an ARM64 runner.
