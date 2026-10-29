{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 共享模型和查询

将 `KPojo` 模型和查询代码放在 `commonMain`。应用按平台提供数据库连接：JVM 使用 JDBC，Android/JVM 使用 `SQLiteDatabase`，Apple、Linux 和 Windows 使用 Native SQLite 适配器。

Native 目标包括：

- iOS：`iosArm64`、`iosSimulatorArm64`、`iosX64`。
- macOS：`macosArm64`、`macosX64`。
- tvOS：`tvosArm64`、`tvosSimulatorArm64`、`tvosX64`。
- watchOS：`watchosArm64`、`watchosDeviceArm64`、`watchosSimulatorArm64`、`watchosX64`。
- Linux：`linuxX64`、`linuxArm64`。
- Windows：`mingwX64`。

Android 应用按 {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }} 配置。Android 使用 JVM 字节码；AndroidX SQLite 是 Native 示例使用的数据库驱动库。

构建 Apple 目标需要 macOS 和 Xcode 16 或更新版本。用 `xcodebuild -version` 检查当前选中的 Xcode；安装多个版本时，通过 `DEVELOPER_DIR` 指定构建使用的 `Contents/Developer` 目录。

## 配置 Gradle

使用 Kotlin `2.4.21`。通过版本目录统一管理 Kronos 库和 Gradle 插件的版本。

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

在依赖仓库中配置 `mavenCentral()`，在插件仓库中配置 `gradlePluginPortal()`，再按应用需要选择目标平台。

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

在每个声明模型或查询的 Kotlin 模块中应用 Kronos 插件。各平台统一使用 `kronos-core` 依赖坐标，由 Gradle 选择对应的平台构件。

## 定义共享模型

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

共享模型使用 Kotlin 基础类型、可空值、枚举、`ByteArray` 和 `kotlin.time.Instant`。序列化字段通过 ValueCodec 配置，见 {{ $.keyword("mapping/serialization", ["序列化"]) }}。Java 时间、十进制、UUID 和 JDBC 类型放在 JVM source set 中。

## 初始化并连接数据库

在最终 Kotlin 应用或 framework 模块中调用一次 `initializeKronosMetadata()`，再配置平台连接，随后执行数据库操作。

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

Native SQLite 连接配置见 {{ $.keyword("database/native-sqlite", ["Native SQLite"]) }}。JVM 应用可直接按 {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }} 使用 `KronosJdbcWrapper`。

## 使用共享 DSL

模型和查询代码通过已配置的连接执行。

```kotlin name="commonMain/People.kt" icon="kotlin"
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select

wrapper.table.createTable(Person())
val inserted = Person(name = "Ada", age = 28).insert().execute()
val people = Person().select().where { it.age >= 18 }.toList()
```

需要浮点 SQL 平均值时，使用 `avgDouble`：

```kotlin name="commonMain/Average.kt" icon="kotlin"
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.avgDouble

val average = Person()
    .select { f.avgDouble(it.age).alias("average") }
    .first<Double>()
```

`avgDouble` 返回 `Double?`，使用浮点精度。JVM 的十进制聚合使用 `java.math.BigDecimal`。

数据库操作是同步调用。有界面的应用应在后台执行器或调度器中执行数据库操作，并在连接所属对象结束使用后关闭连接。
