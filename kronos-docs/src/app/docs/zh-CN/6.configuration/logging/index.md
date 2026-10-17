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

## 关闭内置日志输出

不需要内置 logger 输出到控制台或文件时，将 `Kronos.logPath` 设置为空数组：

```kotlin group="Default Logger 3" name="disabled" icon="kotlin"
Kronos.logPath = []
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

## 使用 SLF4J 和 Logback

`kronos-logging` 通过 SLF4J 输出日志。应用加入一个 SLF4J binding 后即可选择对应适配器，Logback 是常用选择。

```kotlin group="Logback" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm:kronos-logging:{{ $.kronosVersion() }}")
    implementation("ch.qos.logback:logback-classic:<version>")
}
```

```xml group="Logback" name="maven" icon="maven"
<dependency>
    <groupId>com.kotlinorm</groupId>
    <artifactId>kronos-logging</artifactId>
    <version>{{ $.kronosVersion() }}</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>${logback.version}</version>
</dependency>
```

在 `src/main/resources/logback.xml` 中配置 appender 和日志级别：

```xml group="Logback" name="logback.xml" icon="xml"
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>
    <logger name="com.kotlinorm" level="INFO"/>
    <root level="WARN">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

应用启动时选择 SLF4J 适配器：

```kotlin group="SLF4J" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp
import com.kotlinorm.enums.KLoggerType

KronosLoggerApp.detectLoggerImplementation()
Kronos.loggerType = KLoggerType.SLF4J_LOGGER
```

## 使用 SLF4J 和 Log4j2

Log4j2 通过 SLF4J 2 binding 接入。三个 Log4j2 模块使用相同版本。

```kotlin group="Log4j2" name="gradle(kts)" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm:kronos-logging:{{ $.kronosVersion() }}")
    implementation("org.apache.logging.log4j:log4j-api:<version>")
    runtimeOnly("org.apache.logging.log4j:log4j-core:<version>")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:<version>")
}
```

```xml group="Log4j2" name="maven" icon="maven"
<dependency>
    <groupId>com.kotlinorm</groupId>
    <artifactId>kronos-logging</artifactId>
    <version>{{ $.kronosVersion() }}</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>${log4j2.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>${log4j2.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j2-impl</artifactId>
    <version>${log4j2.version}</version>
</dependency>
```

在 `src/main/resources/log4j2.xml` 中配置 `com.kotlinorm` logger：

```xml group="Log4j2" name="log4j2.xml" icon="xml"
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger - %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Logger name="com.kotlinorm" level="info"/>
        <Root level="warn">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

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
