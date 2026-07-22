{% import "../../../macros/macros-en.njk" as $ %}

## Android/JVM and SQLite

Kronos supports Android/JVM applications that use Android's `SQLiteDatabase`. The current platform scope is Android/JVM; Kotlin/Native and JS remain roadmap targets. The reference application uses Kotlin `2.4.0`, Android Gradle Plugin `8.13.2`, JDK 17, and minSdk 26.

Use `{{ $.kronosSnapshotVersion() }}` for the current Android preview. The next stable release will use its corresponding release coordinate.

```kotlin group="Android root setup" name="build.gradle.kts" icon="gradlekts"
// Root build.gradle.kts
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosSnapshotVersion() }}" apply false
}
```

```kotlin group="Android app setup" name="app/build.gradle.kts" icon="gradlekts"
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.kotlinorm.kronos-gradle-plugin")
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosSnapshotVersion() }}")
}
```

Android SQLite applications use `kronos-core` with a `KronosDataSourceWrapper` around `SQLiteDatabase`. Start from the [complete Android SQLite wrapper](https://github.com/Kronos-orm/kronos-example-android/blob/main/app/src/main/java/com/kotlinorm/example/android/AndroidSQLiteDataSourceWrapper.kt) in the example project.

## Application setup

KPojo models use the same table and CRUD APIs in Android/JVM applications. Install the wrapper and initialize the schema required by the app during startup.

## Wrapper responsibilities

An Android wrapper has the same `KronosDataSourceWrapper` contract as a server-side wrapper, but delegates to Android APIs instead of JDBC:

- Set `dbType = DBType.SQLite` so Kronos selects SQLite SQL and DDL rendering.
- Call `task.parsed()` for one task or `task.parsedArr()` for a batch to convert named parameters into SQL text and ordered values, then bind them to `SQLiteProgram`.
- Use the task result type and column types to map cursor rows to scalar values, maps, and KPojo instances. The reference wrapper shows the KPojo mapping flow.
- Use `executeInsert()` for a single insert and assign `task.lastInsertId` when a generated key was requested. Batch writes return per-row update counts.
- Implement the outer `beginTransaction()` / `setTransactionSuccessful()` / `endTransaction()` lifecycle and re-use the current transaction for nested Kronos blocks.

The reference wrapper owns an `SQLiteOpenHelper` and leaves scheduling to the application. Run repository work off the Android main thread and scope the helper to the application or another explicit lifecycle owner.

## Schema and transactions

After the wrapper is installed, normal schema operations work with the SQLite dialect.

```kotlin group="Android schema" name="kotlin" icon="kotlin"
val wrapper = AndroidSQLiteDataSourceWrapper(applicationContext)
Kronos.dataSource = { wrapper }
wrapper.table.syncTable(MarkdownDocument())
```

The Android reference wrapper provides outer transaction commit and rollback, and nested Kronos blocks reuse that transaction. Android SQLite uses the platform transaction lifecycle; JDBC integrations provide isolation, timeout, and savepoint controls.

## Logging

The sample configures `Kronos.logPath = emptyList()` and leaves device logging to the application. Use an Android-aware logger or an application-owned storage location when logs must be persisted.

## Reference implementation

See [kronos-example-android](https://github.com/Kronos-orm/kronos-example-android) for the complete application and [AndroidSQLiteDataSourceWrapper](https://github.com/Kronos-orm/kronos-example-android/blob/main/app/src/main/java/com/kotlinorm/example/android/AndroidSQLiteDataSourceWrapper.kt) for the reference implementation.
