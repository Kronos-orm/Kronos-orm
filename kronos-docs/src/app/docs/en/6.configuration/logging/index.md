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

## Android logging

Android/JVM logging guidance is kept with the {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }} integration chapter.

## Disable bundled log output

Set `Kronos.logPath` to an empty array when the bundled logger should skip console and file output.

```kotlin group="Default Logger 3" name="disabled" icon="kotlin"
with(Kronos) {
    loggerType = KLoggerType.DEFAULT_LOGGER
    logPath = []
}
```

## Add {{ $.title("kronos-logging") }}

Use `kronos-logging` when the application wants Kronos logs to go through an adapter such as JDK Logger or Apache Commons Logging.

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
