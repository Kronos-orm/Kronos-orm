{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 共享模型和查询

将 `KPojo` 模型和查询代码放在共享源码目录：Toolchain 使用 `src/`，Gradle 使用 `commonMain`。应用按平台提供数据库连接：JVM 使用 JDBC，Android/JVM 使用 `SQLiteDatabase`，Apple、Linux 和 Windows 使用 Native SQLite 适配器。

Native 目标包括：

- iOS：`iosArm64`、`iosSimulatorArm64`、`iosX64`。
- macOS：`macosArm64`、`macosX64`。
- tvOS：`tvosArm64`、`tvosSimulatorArm64`、`tvosX64`。
- watchOS：`watchosArm64`、`watchosDeviceArm64`、`watchosSimulatorArm64`、`watchosX64`。
- Linux：`linuxX64`、`linuxArm64`。
- Windows：`mingwX64`。

Android 应用按 {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }} 配置。Android 使用 JVM 字节码；AndroidX SQLite 是 Native 示例使用的数据库驱动库。

构建 Apple 目标需要 macOS 和 Xcode 16 或更新版本。用 `xcodebuild -version` 检查当前选中的 Xcode；安装多个版本时，通过 `DEVELOPER_DIR` 指定构建使用的 `Contents/Developer` 目录。

## 配置 Kotlin Toolchain

使用 Kotlin Toolchain `0.13.0` 和 Kotlin `2.4.21`。[下载完整多模块示例](/assets/downloads/kotlin-toolchain.zip)，解压后保留 wrapper、共享配置和准备脚本。示例由共享模型库 `models`、JVM 应用 `jvm-app` 和 macOS 应用 `native-app` 组成。

```text name="项目目录"
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

`project.yaml` 注册三个模块，公共模板统一 Kotlin 版本和 DSL 参数：

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

共享库通过同一个 `kronos-core` 坐标选择各平台构件，并导出给两个应用。编译器插件配置放在模块的通用 `settings` 中：

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

两个应用依赖 `../models`，并在自身模块加载编译器插件，以便继续编写查询和消费共享库公开返回的投影：

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

### 准备并运行

安装标准 Kotlin SDK，用 `kotlinc` 执行准备脚本；`./kotlin` 则是示例自带的 Toolchain wrapper。脚本从每个模块的 `module-coordinate` 自动生成主源码和测试源码的类型注册文件。新增模块时，在 `project.yaml` 注册模块，并同步准备脚本中的 `modules` 列表：`jvmScope` 指定 JVM source set，`nativeDependencies` 列出 Native 模块依赖。修改模块坐标或依赖后再次运行脚本，不需要手写生成的类型名称。

```bash name="JVM" icon="terminal"
kotlinc -script prepare.main.kts
./kotlin run -m jvm-app
./kotlin test -m models -p jvm
```

在 Apple Silicon Mac 上运行 Native 应用和共享模型测试；Intel Mac 将平台改为 `macosX64`：

```bash name="macOS" icon="terminal"
./kotlin run -m native-app -p macosArm64
./kotlin test -m models -p macosArm64
```

Toolchain 的共享源码放在 `src/`，平台源码使用 `src@jvm/`、`src@native/` 等目录；Gradle 对应使用 `commonMain`、`jvmMain` 和 `nativeMain`。下面的模型和查询可以用于两种构建方式。数据库连接仍由应用提供，SQLite 驱动接入见 {{ $.keyword("database/native-sqlite", ["Native SQLite"]) }}。

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
