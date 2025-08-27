# 2. Layout and Key Classes

Source root: `kronos-logging/src/main/kotlin/com/kotlinorm`

- KronosLoggerApp
  - detectLoggerImplementation(): sets Kronos.defaultLogger and selects the first available KLoggerType among ANDROID/COMMONS/JDK/SLF4J/DEFAULT.
  - Internal getKotoLoggerInstance(loggingClazz: Any): builds concrete adapter by KLoggerType.
- adapter/*
  - AndroidUtilLoggerAdapter
  - ApacheCommonsLoggerAdapter
  - JavaUtilLoggerAdapter
  - Slf4jLoggerAdapter
- exceptions/KotoNoLoggerException: thrown when no supported logger type can be created.

Contract references (in kronos-core):
- interfaces/KLogger: trace/debug/info/warn/error with isXEnabled.
- enums/KLoggerType: ANDROID_LOGGER, COMMONS_LOGGER, JDK_LOGGER, SLF4J_LOGGER, DEFAULT_LOGGER, NONE.
- beans/logging/BundledSimpleLoggerAdapter: default colored console/file logger.
