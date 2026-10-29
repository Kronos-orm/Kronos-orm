# 使用 Kronos 进行 Kotlin Multiplatform 开发

一份 Kotlin 模型，一组查询，让服务端、Android 和 Native 应用各自连接数据库。Kronos 将实体映射、SQL 构建、结果投影和数据库操作放进共享代码，平台代码负责打开连接、安排执行线程和管理资源。下面从一个 `Person` 模型开始，把配置、查询和真实 SQLite 连接串起来。

<figure class="article-cover">
<img src="/assets/images/features/img-1.png" alt="Kotlin 共享模型与查询分别连接桌面、Android 和 iOS 应用的数据库" width="1672" height="941">
<figcaption>共享代码，各自连接：相同的 KPojo 和查询在不同平台使用对应的数据库连接。</figcaption>
</figure>

## 先划清共享代码的边界

`commonMain` 适合放 `KPojo` 模型、业务查询、SQL 表达式和基于查询结果的业务逻辑。平台 source set 负责驱动、数据库文件位置、连接生命周期，以及 UI 与数据库执行线程之间的调度。

例如，“查询成年用户”属于共享逻辑；Android 的数据库文件放在哪里、SwiftUI 页面如何触发查询，则由应用决定。共享查询通过当前平台配置的 wrapper 执行，平台之间各自持有数据库连接。

<figure class="article-figure">
<img src="/assets/images/blog/kmp-platform-connections.svg" alt="commonMain 中的模型和查询，分别通过 JDBC、Android SQLiteDatabase 和 Native SQLite 适配器执行" width="1000" height="420" loading="lazy">
<figcaption>模型与 SQL 共用，连接和驱动留在平台侧。</figcaption>
</figure>

| 平台 | 数据库接入 | 共享代码的调用方式 |
| --- | --- | --- |
| JVM 服务端、桌面 | JDBC 驱动与 `KronosJdbcWrapper` | 使用相同的模型、查询和事务 API |
| Android/JVM | 应用内的 `AndroidSQLiteDataSourceWrapper` 连接 `SQLiteDatabase` | 在数据库执行线程调用共享查询 |
| iOS、macOS、tvOS、watchOS | Native SQLite 适配器；参考应用使用 AndroidX bundled SQLite | 在最终 Kotlin 应用或 framework 初始化元数据 |
| Linux | Native SQLite 适配器；参考应用使用 AndroidX bundled SQLite | 桌面可执行程序调用共享查询 |
| Windows | Native SQLite 适配器；参考应用使用 SQLite C API 驱动 | 使用 `mingwX64` 构建和运行应用 |

Android/JVM 是 Android 的 JVM 编译目标。AndroidX SQLite 是数据库驱动库，Native 示例通过它使用 bundled SQLite。具体连接代码分别见 [Android SQLite](/documentation/zh-CN/database/android-sqlite) 和 [Native SQLite](/documentation/zh-CN/database/native-sqlite)。

## 使用 Kotlin Toolchain 接入

Toolchain 项目在 `models/module.yaml` 中声明 `kmp/lib` 和目标平台，通过 `settings.kotlin.compilerPlugins` 加载 `kronos-compiler-plugin`。共享模型放在 `src/`，JVM 与 Native 源码分别放在 `src@jvm/` 和 `src@native/`。

[KMP 指南](/documentation/zh-CN/getting-started/kmp)提供完整可下载的三模块项目：共享模型、JVM 应用和 Native 应用。解压后先用标准 Kotlin SDK 执行类型注册准备，再用 Toolchain wrapper 运行应用：

```bash
kotlinc -script prepare.main.kts
./kotlin run -m jvm-app
./kotlin test -m models -p jvm
# Apple Silicon Mac
./kotlin run -m native-app -p macosArm64
```

示例同时演示共享库公开返回的投影类型，以及应用中的字段读取、排序和结果映射。选择 Gradle 的项目可以使用下面的版本目录配置。

## 用版本目录配置依赖

下面使用 Gradle KMP 项目演示接入。Kotlin 版本为 `2.4.21`，Kronos 插件和运行时保持同一版本，并统一放进 `gradle/libs.versions.toml`。

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

在插件仓库中配置 `gradlePluginPortal()`，依赖仓库中配置 `mavenCentral()`。在每个声明模型或查询的 Kotlin 模块中应用 Kronos 插件。下面是一个同时面向 JVM、iOS、macOS、Linux 和 Windows 的共享模块：

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

`kronos-core` 使用统一的依赖坐标，Gradle 根据当前目标选择平台构件。`-Xcollection-literals` 用于查询中的 `[it.id, it.name]` 语法；`-Xcontext-sensitive-resolution` 支持上下文中的 Kotlin 名称解析。目标平台和完整项目配置见 [Kotlin Multiplatform 指南](/documentation/zh-CN/getting-started/kmp)。

## 在 commonMain 定义模型

将下列模型放进共享模块的 `commonMain`。`@Table` 指定表名，`@PrimaryKey(identity = true)` 将 `id` 声明为数据库生成的主键。

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

编译器为 `KPojo` 生成字段信息和数据映射能力，共享代码可直接使用 Kotlin 基础类型、可空属性、枚举、`ByteArray` 和 `kotlin.time.Instant`。涉及 Java 时间、JDBC 对象等平台类型的代码放在 JVM source set；需要将业务值编码成数据库值时，通过 [ValueCodec 与序列化](/documentation/zh-CN/mapping/serialization) 配置映射。

多模块项目可以把模型放在一个模块，把查询放在另一个模块。两个模块都应用 Kronos 插件，最终应用依赖它们，再在应用入口完成初始化。

## 在最终应用初始化 Kronos

在最终 Kotlin 应用或 framework 模块执行 ORM 操作前调用一次 `initializeKronosMetadata()`，并配置平台连接。下面的函数接收应用提供的数据源，将它桥接到共享 ORM API：

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

初始化入口属于最终应用或 framework：它连接应用依赖中生成的模型工厂和枚举元数据。`Kronos.dataSource` 决定后续共享查询使用哪个 wrapper。JVM 应用也可以按 [连接数据库](/documentation/zh-CN/database/connect-to-db) 配置 `KronosJdbcWrapper`。

## 让 Native 应用真正打开 SQLite

[Native 参考应用](https://github.com/Kronos-orm/kronos-example-ios) 提供 `KronosSqliteDataSource`、各平台驱动、桌面程序和 SwiftUI 应用。从参考项目接入适配器后，平台入口可以这样使用前面的初始化函数：

```kotlin
import com.kotlinorm.example.ios.KronosSqliteDataSource

val source = KronosSqliteDataSource(databaseName = ":memory:")
val wrapper = configureDatabase(source)
source.open()

// 在这个连接上执行后面的建表、CRUD 和事务示例。
// 数据库所属对象结束使用后释放连接。
source.close()
```

`:memory:` 的数据随连接存活。要保存数据，将应用可写数据目录中的文件路径传给 `databaseName`。iOS 的文件目录由应用选择，Android 的连接生命周期由应用组件管理；共享查询函数不用处理这些路径差异。

Android 应用可直接参考 [Android 示例](https://github.com/Kronos-orm/kronos-example-android)，使用 `SQLiteDatabase` 和应用内的 wrapper。Native 连接的生成主键、事务、保存点和关闭行为见 [Native SQLite 指南](/documentation/zh-CN/database/native-sqlite)。

## 写一组可共享的 CRUD

完成初始化并打开连接后，建表和数据操作可以使用同一套 Kotlin DSL：

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

`withId()` 请求生成主键，结果从 `inserted.lastInsertId` 读取。参考 SQLite 适配器把主键读取与对应的写入放在同一连接和执行过程中。查询条件读取模型属性，由 Kronos 转换成对应数据库的 SQL 和参数。

## 只取需要的列，并保留类型

界面通常只需要部分列。通过 `select` 声明输出字段，后面的结果行就具有对应的 Kotlin 属性：

```kotlin
val names = Person()
    .select { [it.id, it.name] }
    .where { it.age >= 18 }
    .toList()

val firstName: String? = names.firstOrNull()?.name
```

同一条查询中的 `where` 读取输入模型 `Person`，因此即使输出只包含 `id` 和 `name`，仍可以按 `age` 过滤。计算表达式通过 `.alias("name")` 指定结果属性名。投影、后续查询和排序上下文的规则见 [投影](/documentation/zh-CN/query/projection)。

## 把一组写入放进事务

事务与保存点使用同一连接。下面先写入一行，再撤销保存点之后的可选写入：

```kotlin
Kronos.transact(wrapper = wrapper) {
    Person(name = "Keep", age = 30).insert().execute()
    val checkpoint = savepoint("optional_person")
    Person(name = "Discard", age = 20).insert().execute()
    rollbackToSavepoint(checkpoint)
    releaseSavepoint(checkpoint)
}
```

回调正常结束时提交事务，抛出异常时回滚。保存点 token 在所属事务回调内使用。数据库操作是同步调用，Android 和 iOS 界面应通过应用的后台执行器或调度器调用共享查询，再把结果交回 UI。

## 在 IDE 中理解共享查询

安装 [Kotlin External FIR Support（KEFS）](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support)，让 Kotlin 编辑器分析识别生成的投影属性、类型和 DSL 诊断。按 [Kronos ORM / KEFS / IDEA 兼容表](/documentation/zh-CN/resources/kefs) 选择版本，并配置 `com.kotlinorm:kronos-compiler-plugin`。

Gradle 项目使用自己的项目导入功能。使用 Kotlin Toolchain 的源码项目另安装对应的官方构建模型插件。完成后，在 KEFS Diagnostics 中确认 Kronos 显示 `Successfully Loaded`。

## 从可运行示例继续

Native 参考应用包含数据库连接、平台入口和测试，可以直接验证前面介绍的路径：

```bash
# Apple Silicon macOS：真实 SQLite 测试和桌面程序
./gradlew :shared:macosArm64Test :shared:runDebugExecutableMacosArm64

# Linux x64：真实 SQLite 测试和桌面程序
./gradlew :shared:linuxX64Test :shared:runDebugExecutableLinuxX64
```

Windows 使用 `gradlew.bat :shared:mingwX64Test :shared:runDebugExecutableMingwX64`。iOS 按示例中的说明用 XcodeGen 生成项目，再运行 `KronosIosApp` scheme。Android 使用独立的 Android 示例。

接下来可继续阅读 [KMP 项目配置](/documentation/zh-CN/getting-started/kmp)、[Android SQLite](/documentation/zh-CN/database/android-sqlite)、[Native SQLite](/documentation/zh-CN/database/native-sqlite)、[事务](/documentation/zh-CN/database/transactions) 和 [KEFS 编辑器支持](/documentation/zh-CN/resources/kefs)。
