{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 使用内置日志

Kronos 通过 `Kronos.defaultLogger` 输出 SQL 执行日志。`kronos-core` 已包含内置日志实现，可以输出到控制台、写入文件，或同时输出到两个位置。

```kotlin group="Default Logger 1 1" name="console and file" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.enums.KLoggerType

with(Kronos) {
    loggerType = KLoggerType.DEFAULT_LOGGER
    logPath = ["console", "logs/kronos"]
}
```

执行 ORM 任务时，内置日志会输出操作类型、SQL、参数和结果。

```text group="Default Logger 1 2" name="output"
[yyyy-MM-dd HH:mm:ss.SSS] [info] [Kronos] Executing [SELECT] task:
SQL:    SELECT `id`, `name` FROM `user` WHERE `id` = :id
PARAMS: {id=1}
Found rows: 1
-----------------------
```

## 写入日志目录

需要让内置日志创建每日文件时，将目录路径加入 `Kronos.logPath`。

```kotlin group="Default Logger 2 1" name="file only" icon="kotlin"
with(Kronos) {
    loggerType = KLoggerType.DEFAULT_LOGGER
    logPath = ["logs/kronos"]
}
```

文件名使用内置日志的默认规则。

```text group="Default Logger 2 2" name="file"
logs/kronos/kronos-log-yyyy-MM-dd.log
```

## Android 日志

Android/JVM 日志配置说明统一放在 {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }} 接入章节。

## 添加{{ $.title("kronos-logging") }}

需要让 Kronos 日志进入 Android Log、JDK Logger 或 Apache Commons Logging 等适配器时，引入 `kronos-logging`。

```kotlin group="kronos-logging" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm:kronos-logging:{{ $.kronosVersion() }}")
}
```

```xml group="kronos-logging" name="maven" icon="maven"
<dependency>
    <groupId>com.kotlinorm</groupId>
    <artifactId>kronos-logging</artifactId>
    <version>{{ $.kronosVersion() }}</version>
</dependency>
```

## 使用 JDK Logger

在应用启动时调用一次 `KronosLoggerApp.detectLoggerImplementation()`，再选择 JDK logger 类型。

```kotlin group="JDK Logger 1" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp
import com.kotlinorm.enums.KLoggerType

KronosLoggerApp.detectLoggerImplementation()

Kronos.loggerType = KLoggerType.JDK_LOGGER
```

JDK Logger 会接收同样的 ORM 执行信息。

```text group="JDK Logger 2" name="output"
INFO: Executing [INSERT] task:
SQL:    INSERT INTO `user` (`name`) VALUES (:name)
PARAMS: {name=Kronos}
Affected rows: 1
-----------------------
```

## 使用 Apache Commons Logging

加入应用使用的 Commons Logging API 后，选择 `KLoggerType.COMMONS_LOGGER`。

```kotlin group="Commons Logging" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm:kronos-logging:{{ $.kronosVersion() }}")
    implementation("commons-logging:commons-logging:<latest-stable>")
}
```

```kotlin group="Commons Logging" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp
import com.kotlinorm.enums.KLoggerType

KronosLoggerApp.detectLoggerImplementation()

Kronos.loggerType = KLoggerType.COMMONS_LOGGER
```

> **Note**
> 适配器会从运行时 classpath 创建目标 logger。选择对应 `KLoggerType` 前，先加入应用实际使用的日志 API 和 binding。

## 探测可用适配器

`detectLoggerImplementation()` 会检查运行时 classpath，并将 `Kronos.defaultLogger` 设置为探测到的适配器。

```kotlin group="Detect Logger 1" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp

KronosLoggerApp.detectLoggerImplementation()

println(Kronos.loggerType)
```

在只包含 JDK logger 的标准 JVM 项目中，探测结果为：

```text group="Detect Logger 2" name="output"
JDK_LOGGER
```

完整数据库连接配置请参考 {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }} 和 {{ $.keyword("configuration/global-config", ["全局设置"]) }}。
