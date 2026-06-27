{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Supported logging frameworks

Kronos can integrate with external logging frameworks. The currently supported frameworks are:

- [SLF4J](https://www.slf4j.org/manual.html)
- [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/)
- [Android Logging](https://developer.android.com/reference/kotlin/android/util/Log)
- [JDK Logger](https://docs.oracle.com/javase/8/docs/api/java/util/logging/Logger.html)

When using the default logging implementation, explicitly set `KLoggerType` to `DEFAULT_LOGGER`, or omit the `KLoggerType` parameter.

## log4j integration example

The following example uses `Spring Boot + Kronos + JDK 17 + Maven + Kotlin 2.0.0` to demonstrate how to integrate Kronos with `log4j`.

### 1. Specify KLoggerType explicitly

Set `loggerType` to the corresponding type. If it is not specified, Kronos uses its default log output format.
Only part of the configuration is shown here. For details, see {{ $.keyword("getting-started/global-config", ["Global Config"]) }}.

```kotlin
Kronos.apply {
    loggerType = KLoggerType.SLF4J_LOGGER
}
```

### 2. Dependencies

Add the required Spring Boot dependencies and `log4j` dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Includes mvc, aop, and related resources -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <!-- Switch log reading to log4j2 -->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Configure log4j2 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>

<!-- Required for recognizing log4j2.yml -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

### 3. Configure log4j2.yml and application.yml

#### log4j2.yml

```yml
Configuration:
  status: WARN
  monitorInterval: 5

  Properties:
    Property:
      - name: log.level.console
        value: info
      - name: log.path
        value: ./${project.name}_log
      - name: project.name
        value: daily
      - name: log.pattern
        value: "%d{yyyy-MM-dd HH:mm:ss.SSS} -%5p ${PID:-} [%15.15t] %-30.30C{1.} : %m%n"

  Appenders:
    Console:
      name: CONSOLE
      target: SYSTEM_OUT
      PatternLayout:
        pattern: ${log.pattern}
    RollingFile:
      - name: ROLLING_FILE
        fileName: ${log.path}/daily/${project.name}.log
        filePattern: "${log.path}/daily/$${date:yyyy-MM-dd}/${project.name}-%d{yyyy-MM-dd}-%i.log.gz"
        PatternLayout:
          pattern: ${log.pattern}
        Filters:
          ThresholdFilter:
            - level: error
              onMatch: DENY
              onMismatch: NEUTRAL
            - level: info
              onMatch: ACCEPT
              onMismatch: DENY
        Policies:
          SizeBasedTriggeringPolicy:
            size: "10MB"
          TimeBasedTriggeringPolicy:
            modulate: true
            interval: 1
        DefaultRolloverStrategy:
          max: 20
      - name: EXCEPTION_ROLLING_FILE
        ignoreExceptions: false
        fileName: ${log.path}/exception/${project.name}_exception.log
        filePattern: "${log.path}/exception/$${date:yyyy-MM-dd}/${project.name}-%d{yyyy-MM-dd}-%i.log.gz"
        ThresholdFilter:
          level: error
          onMatch: ACCEPT
          onMismatch: DENY
        PatternLayout:
          pattern: ${log.pattern}
        Policies:
          SizeBasedTriggeringPolicy:
            size: "10MB"
          TimeBasedTriggeringPolicy:
            modulate: true
            interval: 1
        DefaultRolloverStrategy:
          max: 100
      - name: WARN_ROLLING_FILE
        ignoreExceptions: false
        fileName: ${log.path}/warn/${project.name}_warn.log
        filePattern: "${log.path}/warn/$${date:yyyy-MM-dd-dd}/${project.name}-%d{yyyy-MM-dd}-%i.log.gz"
        ThresholdFilter:
          level: warn
          onMatch: ACCEPT
          onMismatch: DENY
        PatternLayout:
          pattern: ${log.pattern}
        Policies:
          SizeBasedTriggeringPolicy:
            size: "10MB"
          TimeBasedTriggeringPolicy:
            modulate: true
            interval: 1
        DefaultRolloverStrategy:
          max: 20
      - name: ROLLING_FILE_USER
        fileName: ${log.path}/user/user-${project.name}.log
        filePattern: "${log.path}/user/$${date:yyyy-MM-dd}/user-${project.name}-%d{yyyy-MM-dd}-%i.log.gz"
        PatternLayout:
          pattern: ${log.pattern}
        Filters:
          ThresholdFilter:
            - level: error
              onMatch: DENY
              onMismatch: NEUTRAL
            - level: info
              onMatch: ACCEPT
              onMismatch: DENY
        Policies:
          SizeBasedTriggeringPolicy:
            size: "10MB"
          TimeBasedTriggeringPolicy:
            modulate: true
            interval: 1
        DefaultRolloverStrategy:
          max: 100

  Loggers:
    Root:
      level: info
      AppenderRef:
        - ref: CONSOLE
        - ref: ROLLING_FILE
        - ref: EXCEPTION_ROLLING_FILE
        - ref: WARN_ROLLING_FILE
    Logger:
      - name: exception
        level: debug
        additivity: true
        AppenderRef:
          - ref: EXCEPTION_ROLLING_FILE
      - name: com.example.aspect
        additivity: false
        level: info
        AppenderRef:
          - ref: CONSOLE
          - ref: ROLLING_FILE_USER
```

#### application.yml

Point Spring Boot to `log4j2.yml`. Remove the built-in Spring Boot logging configuration that conflicts with this setup.

```yml
logging:
  config: classpath:log4j2.yml
```

## Other frameworks

For logging frameworks such as `Apache Commons Logging`, the integration pattern is almost the same. Replace the dependency and framework-specific configuration according to the logging implementation you use.
