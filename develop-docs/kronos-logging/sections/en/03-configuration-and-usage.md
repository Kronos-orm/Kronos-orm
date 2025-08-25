# 3. Configuration and Usage

1) Zero-config: automatic detection

In typical setups, kronos-logging auto-detects an available backend (SLF4J/JUL/Commons/Android, otherwise the bundled logger). No manual call is required.

2) Optional: explicit detection (only if you need to force detection timing)

```kotlin
fun main() {
    // Optional: trigger detection explicitly; normally not needed
    KronosLoggerApp.detectLoggerImplementation()
}
```

3) Force a specific backend

```kotlin
fun main() {
    Kronos.loggerType = KLoggerType.SLF4J_LOGGER
    Kronos.defaultLogger = { clazz -> Slf4jLoggerAdapter(clazz.toString()) }
}
```

4) Configure bundled simple logger (default)

```kotlin
fun main() {
    Kronos.logPath = mutableListOf("console") // or listOf("/var/log/app") to write files
    BundledSimpleLoggerAdapter.logDateTimeFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    BundledSimpleLoggerAdapter.debugEnabled = true
}
```

Notes
- kronos-core prints structured messages (KLogMessage) around task execution (SQL, params, affected rows, etc.).
- You can replace Kronos.defaultLogger factory to fully customize logging behavior.
- If multiple backends are present, the detector picks the first match in priority order (Android -> Commons -> JDK -> SLF4J -> Bundled).
