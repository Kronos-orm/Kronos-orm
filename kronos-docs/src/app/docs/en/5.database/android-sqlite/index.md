{% import "../../../macros/macros-en.njk" as $ %}

## Use SQLite on Android

An Android application uses {{ $.code("KPojo") }} models with an app-owned {{ $.code("KronosDataSourceWrapper") }} for `SQLiteDatabase`.

## Add Kronos

Configure the standard `google()`, `mavenCentral()`, and `gradlePluginPortal()` repositories, then add the Kronos plugin and core library to the Android project.

```kotlin name="build.gradle.kts" icon="gradlekts"
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}" apply false
}
```

> **Note**
> These snippets use Android Gradle Plugin `8.13.2` and Kotlin `2.4.0`. The reference application uses `minSdk 26` and JDK 17.

Apply the plugin to every Android application or library module that declares a {{ $.code("KPojo") }}.

```kotlin name="app/build.gradle.kts" icon="gradlekts"
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.kotlinorm.kronos-gradle-plugin")
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}
```

## Define a model

Declare Android tables with the same model annotations used by other Kotlin applications.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("notes")
data class Note(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var title: String? = null,
    var content: String? = null,
    var favorite: Boolean? = false,
) : KPojo
```

## Connect SQLiteDatabase

Add a {{ $.code("KronosDataSourceWrapper") }} implementation to the application. The [Android reference wrapper](https://github.com/Kronos-orm/kronos-example-android/blob/main/app/src/main/java/com/kotlinorm/example/android/AndroidSQLiteDataSourceWrapper.kt) is an application source file that you can copy into the project and adapt for its database name and version.

With that source available as `AndroidSQLiteDataSourceWrapper`, create it once in the application's `onCreate` and make it the Kronos data source.

```kotlin name="kotlin" icon="kotlin"
import android.app.Application
import com.kotlinorm.Kronos
import com.kotlinorm.orm.ddl.table
import java.util.concurrent.Executors
import java.util.concurrent.Future

class NotesApplication : Application() {
    lateinit var database: AndroidSQLiteDataSourceWrapper
        private set
    private val schemaExecutor = Executors.newSingleThreadExecutor()
    private lateinit var schemaReady: Future<*>

    override fun onCreate() {
        super.onCreate()
        database = AndroidSQLiteDataSourceWrapper(this)
        Kronos.dataSource = { database }
        schemaReady = schemaExecutor.submit {
            database.table.syncTable<Note>()
        }
    }

    fun awaitSchemaReady() {
        schemaReady.get()
    }
}
```

Register the application class in `AndroidManifest.xml`.

```xml name="AndroidManifest.xml" icon="android"
<application
    android:name=".NotesApplication"
    android:label="@string/app_name"
    android:theme="@style/Theme.App">
    ...
</application>
```

Applications that already have an `Application` class can put the data-source setup there.

## Log SQL to Logcat

Add {{ $.title("kronos-logging") }} when the application should send Kronos SQL logs to Logcat.

```kotlin name="app/build.gradle.kts" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm:kronos-logging:{{ $.kronosVersion() }}")
}
```

Call the setup function from `NotesApplication.onCreate`.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.KronosLoggerApp

KronosLoggerApp.detectLoggerImplementation()
```

## Use CRUD after the schema is ready

`NotesApplication` starts schema preparation once. Run database work on an executor and call `awaitSchemaReady()` before each operation. The example posts results and errors to the main thread.

```kotlin name="kotlin" icon="kotlin"
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kotlinorm.Kronos
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import java.util.concurrent.Executors

class NotesActivity : Activity() {
    private val databaseExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun <T> runDatabase(
        action: () -> T,
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        databaseExecutor.execute {
            runCatching {
                (application as NotesApplication).awaitSchemaReady()
                action()
            }.onSuccess { result ->
                mainHandler.post { onSuccess(result) }
            }.onFailure { error ->
                mainHandler.post { onError(error) }
            }
        }
    }

    fun createNote() {
        runDatabase(
            action = {
                val id = Note(title = "First note", content = "Hello Android")
                    .insert()
                    .withId()
                    .execute()
                    .lastInsertId
                    ?: error("SQLite did not return the generated note id")

                Note()
                    .select()
                    .where { it.id == id }
                    .first()
            },
            onSuccess = { note ->
                // Render note in the UI.
            },
            onError = ::reportDatabaseError,
        )
    }

    private fun reportDatabaseError(error: Throwable) {
        Log.e("Notes", "Database operation failed", error)
    }

    override fun onDestroy() {
        databaseExecutor.shutdownNow()
        super.onDestroy()
    }
}
```

## Run related writes in a transaction

Use {{ $.title("Kronos.transact") }} for related writes. Android SQLite uses the default transaction settings.

In the same activity, call the `runDatabase` helper from the previous example.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.update.update

fun createFavoriteNote() {
    runDatabase(
        action = {
            Kronos.transact {
                val id = Note(title = "Plan", content = "Review Android support")
                    .insert()
                    .withId()
                    .execute()
                    .lastInsertId
                    ?: error("SQLite did not return the generated note id")

                Note(id = id)
                    .update()
                    .set { it.favorite = true }
                    .by { it.id }
                    .execute()
            }
        },
        onSuccess = {},
        onError = ::reportDatabaseError,
    )
}
```

## Reference application

[kronos-example-android](https://github.com/Kronos-orm/kronos-example-android) contains a complete Markdown notebook with the wrapper source, model, repository, UI, and instrumentation tests.
