# Module kronos-logging

Pluggable logging module for Kronos ORM. The shared `KronosLoggerApp` API installs the best logging implementation for the current target.

## Shared controls

```kotlin
Kronos.logging.enabled = false
Kronos.logging.enabled = true
Kronos.logging.path = arrayOf("console")
Kronos.logging.minimumLevel = KLogLevel.WARN
```

Logging is enabled by default with the `console` destination. The shared switch and minimum
level apply to built-in and custom adapters, before logger and message construction. Changing
the switch preserves the adapter, paths and custom factory. Bundled directory output is
provided on JVM; Native uses the console destination and includes exception stack traces.

After detection, select a JVM adapter with `Kronos.logging.type`. Set
`Kronos.logging.factory` to an application `KLoggerFactory`, or null to use the platform
adapter. A custom factory takes precedence without changing the selected type.

## Detection Order

1. Android `android.util.Log`
2. Apache Commons Logging
3. JDK `java.util.logging`
4. SLF4J
5. Bundled simple logger (fallback, from kronos-core)

The JVM adapters use reflection to avoid hard dependencies on optional logging frameworks. On Native,
`KronosLoggerApp.detectLoggerImplementation()` installs the native console adapter as
the platform adapter; core also provides a console fallback before detection runs.

## Targets

- JVM: Android `Log`, Apache Commons Logging, JDK logging, SLF4J, and bundled fallback detection.
- iOS, macOS, tvOS, watchOS, Linux and Windows Kotlin/Native: native console logging.

## Dependencies

- `api`: kronos-core
- `kotlin-reflect`, SLF4J API and Logback: JVM test scope only
