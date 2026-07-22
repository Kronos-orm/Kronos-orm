{% import "../../../macros/macros-zh-CN.njk" as $ %}

## Android/JVM 和 SQLite

Kronos 支持使用 Android `SQLiteDatabase` 的 Android/JVM 应用。当前平台范围为 Android/JVM，Kotlin/Native 和 JS 仍是后续路线目标。参考应用使用 Kotlin `2.4.0`、Android Gradle Plugin `8.13.2`、JDK 17 和 minSdk 26。

当前 Android 预览版本使用 `{{ $.kronosSnapshotVersion() }}`；稳定版发布后使用对应的 release 坐标。

```kotlin group="Android root setup" name="build.gradle.kts" icon="gradlekts"
// 根目录 build.gradle.kts
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

Android SQLite 应用使用 `kronos-core`，并围绕 `SQLiteDatabase` 实现 `KronosDataSourceWrapper`。可从示例中的完整 [Android SQLite wrapper](https://github.com/Kronos-orm/kronos-example-android/blob/main/app/src/main/java/com/kotlinorm/example/android/AndroidSQLiteDataSourceWrapper.kt) 开始。

## 应用初始化

KPojo 模型在 Android/JVM 应用中使用与其他平台相同的表操作和 CRUD API。应用启动时安装 wrapper，并初始化应用需要的表结构。

## Wrapper 职责

Android wrapper 与服务端 wrapper 使用同一个 `KronosDataSourceWrapper` 接口契约，只是把执行委托给 Android API 而非 JDBC：

- 设置 `dbType = DBType.SQLite`，让 Kronos 选择 SQLite SQL 和 DDL 渲染。
- 单条 task 用 `task.parsed()`，batch 用 `task.parsedArr()`，将命名参数转换为 SQL 文本和有序参数后再绑定到 `SQLiteProgram`。
- 使用 task 的结果类型和列类型，把 Cursor 行映射为标量、Map 和 KPojo。参考 wrapper 展示了 KPojo 映射流程。
- 单条 insert 使用 `executeInsert()`；请求生成 ID 时写入 `task.lastInsertId`。批量写入返回每行 update count。
- 实现最外层 `beginTransaction()` / `setTransactionSuccessful()` / `endTransaction()`，嵌套 Kronos block 复用当前事务。

参考实现持有 `SQLiteOpenHelper`，调度仍由应用负责。数据库仓储操作应离开 Android 主线程，并将 helper 绑定到 Application 或其他明确的生命周期 owner。

## 表结构和事务

安装 wrapper 后，常规表结构操作会使用 SQLite 方言。

```kotlin group="Android schema" name="kotlin" icon="kotlin"
val wrapper = AndroidSQLiteDataSourceWrapper(applicationContext)
Kronos.dataSource = { wrapper }
wrapper.table.syncTable(MarkdownDocument())
```

Android 参考 wrapper 提供最外层事务提交和回滚，嵌套 Kronos block 会复用该事务。Android SQLite 使用平台事务生命周期；JDBC 集成提供隔离级别、超时和保存点控制。

## 日志

示例通过 `Kronos.logPath = emptyList()` 将设备日志交由应用决定。需要持久化日志时，使用 Android-aware logger 或应用拥有的存储位置。

## 参考实现

完整应用见 [kronos-example-android](https://github.com/Kronos-orm/kronos-example-android)，参考实现见 [AndroidSQLiteDataSourceWrapper](https://github.com/Kronos-orm/kronos-example-android/blob/main/app/src/main/java/com/kotlinorm/example/android/AndroidSQLiteDataSourceWrapper.kt)。
