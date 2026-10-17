{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Use the bundled logger

Kronos writes SQL execution logs through `Kronos.defaultLogger`. The bundled logger is available from `kronos-core` and can print to the console, write to files, or do both.

```kotlin group="Default Logger 1 1" name="console and file" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.enums.KLoggerType

with(Kronos) {
    loggerType = KLoggerType.DEFAULT_LOGGER
    logPath = ["console", "logs/kronos"]
}
```

When an ORM task executes, the bundled logger prints the operation type, SQL, parameters, and result.

```text group="Default Logger 1 2" name="output"
[yyyy-MM-dd HH:mm:ss.SSS] [info] [Kronos] Executing [SELECT] task:
SQL:    SELECT `id`, `name` FROM `user` WHERE `id` = :id
PARAMS: {id=1}
Found rows: 1
-----------------------
```

## Write bundled logs to a directory

Add a directory path to `Kronos.logPath` when the bundled logger should create a daily log file.

```kotlin group="Default Logger 2 1" name="file only" icon="kotlin"
with(Kronos) {
    loggerType = KLoggerType.DEFAULT_LOGGER
    logPath = ["logs/kronos"]
}
```

The file name follows the bundled logger rule.

```text group="Default Logger 2 2" name="file"
logs/kronos/kronos-log-yyyy-MM-dd.log
```

## Disable bundled output

Set `Kronos.logPath` to an empty array when the bundled logger should not write to the console or files:

```kotlin group="Default Logger 3" name="disabled" icon="kotlin"
Kronos.logPath = []
```

## Android logging

Android/JVM logging guidance is kept with the {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }} integration chapter.

## Add {{ $.title("kronos-logging") }}

Use `kronos-logging` when the application wants Kronos logs to go through an adapter such as Android Log, JDK Logger, or Apache Commons Logging.

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

## Use JDK Logger

Call `KronosLoggerApp.detectLoggerImplementation()` once during application startup, then select the JDK logger type.

```kotlin group="JDK Logger 1" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp
import com.kotlinorm.enums.KLoggerType

KronosLoggerApp.detectLoggerImplementation()

Kronos.loggerType = KLoggerType.JDK_LOGGER
```

JDK Logger receives the same ORM execution message.

```text group="JDK Logger 2" name="output"
INFO: Executing [INSERT] task:
SQL:    INSERT INTO `user` (`name`) VALUES (:name)
PARAMS: {name=Kronos}
Affected rows: 1
-----------------------
```

## Use Apache Commons Logging

Add the Commons Logging API used by your application, then select `KLoggerType.COMMONS_LOGGER`.

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
> The adapter creates the target logger from classes on the runtime classpath. Add the logging API and binding used by your application before selecting its `KLoggerType`.

## Use SLF4J with Logback

`kronos-logging` sends messages to SLF4J. Add one SLF4J binding to the application; Logback is a common choice.

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

Create `src/main/resources/logback.xml` and choose the appenders and levels for the application:

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

Select the SLF4J adapter during application startup:

```kotlin group="SLF4J" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp
import com.kotlinorm.enums.KLoggerType

KronosLoggerApp.detectLoggerImplementation()
Kronos.loggerType = KLoggerType.SLF4J_LOGGER
```

## Use SLF4J with Log4j2

Log4j2 connects through its SLF4J 2 binding. Keep the three Log4j2 modules on the same version.

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

Create `src/main/resources/log4j2.xml` and configure the `com.kotlinorm` logger:

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

## Detect an available adapter

`detectLoggerImplementation()` checks the runtime classpath and assigns `Kronos.defaultLogger` to the detected adapter.

```kotlin group="Detect Logger 1" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp

KronosLoggerApp.detectLoggerImplementation()

println(Kronos.loggerType)
```

On a standard JVM project that only has the JDK logger available, the detected type is:

```text group="Detect Logger 2" name="output"
JDK_LOGGER
```

For full database connection setup, see {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }} and {{ $.keyword("configuration/global-config", ["Global Config"]) }}.
