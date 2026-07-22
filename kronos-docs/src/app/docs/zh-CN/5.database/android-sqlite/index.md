{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 在 Android 上使用 SQLite

Android 应用使用 {{ $.code("KPojo") }} 模型，并通过应用内的 {{ $.code("KronosDataSourceWrapper") }} 访问 `SQLiteDatabase`。

## 添加 Kronos

配置标准的 `google()`、`mavenCentral()` 和 `gradlePluginPortal()` 仓库后，将 Kronos 插件和 core 库加入 Android 项目。

```kotlin name="build.gradle.kts" icon="gradlekts"
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}" apply false
}
```

> **Note**
> 本文示例使用 Android Gradle Plugin `8.13.2` 和 Kotlin `2.4.0`。参考应用使用 `minSdk 26` 和 JDK 17。

在每个声明 {{ $.code("KPojo") }} 的 Android application 或 library 模块中应用该插件。

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

## 定义模型

Android 表模型使用与其他 Kotlin 应用相同的模型注解。

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

## 连接 SQLiteDatabase

在应用中添加一个 {{ $.code("KronosDataSourceWrapper") }} 实现。[Android 参考 wrapper](https://github.com/Kronos-orm/kronos-example-android/blob/f22b19b/app/src/main/java/com/kotlinorm/example/android/AndroidSQLiteDataSourceWrapper.kt) 是应用源码，可以复制到项目中，再按应用的数据库名称和版本进行调整。

将该源码作为 `AndroidSQLiteDataSourceWrapper` 加入项目后，在应用的 `onCreate` 中创建一次并设置为 Kronos 数据源。

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

在 `AndroidManifest.xml` 中注册这个 Application 类。

```xml name="AndroidManifest.xml" icon="android"
<application
    android:name=".NotesApplication"
    android:label="@string/app_name"
    android:theme="@style/Theme.App">
    ...
</application>
```

已有 `Application` 类的应用可以把数据源设置放入现有类中。

## 将 SQL 日志写入 Logcat

应用需要将 Kronos SQL 日志写入 Logcat 时，加入 {{ $.title("kronos-logging") }}。

```kotlin name="app/build.gradle.kts" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm:kronos-logging:{{ $.kronosVersion() }}")
}
```

在 `NotesApplication.onCreate` 中调用初始化函数。

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.KronosLoggerApp

KronosLoggerApp.detectLoggerImplementation()
```

## 在表结构就绪后执行 CRUD

`NotesApplication` 会启动一次表结构准备。数据库操作在 executor 中执行，并在每次操作前调用 `awaitSchemaReady()`；结果和错误会投递回主线程。

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
                // 在界面中展示 note。
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

## 在事务中执行关联写入

相关写操作使用 {{ $.title("Kronos.transact") }}。Android SQLite 使用默认事务参数。

在同一个 Activity 中调用上一节的 `runDatabase` 辅助函数。

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

## 参考应用

[kronos-example-android](https://github.com/Kronos-orm/kronos-example-android/tree/f22b19b) 提供完整的 Markdown 笔记应用，包含 wrapper 源码、模型、仓储、界面和 instrumentation test。
