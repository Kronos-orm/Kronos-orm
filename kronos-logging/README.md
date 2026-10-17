# Module kronos-logging

Pluggable logging module for Kronos ORM. The shared `KronosLoggerApp` API installs the best logging implementation for the current target.

## Detection Order

1. Android `android.util.Log`
2. Apache Commons Logging
3. JDK `java.util.logging`
4. SLF4J
5. Bundled simple logger (fallback, from kronos-core)

The JVM adapters use reflection to avoid hard dependencies on optional logging frameworks. On iOS,
`KronosLoggerApp.detectLoggerImplementation()` installs the native console adapter as
`Kronos.defaultLogger`; core also provides a console fallback before detection runs.

## Targets

- JVM: Android `Log`, Apache Commons Logging, JDK logging, SLF4J, and bundled fallback detection.
- iOS x64, device ARM64, and Apple Silicon simulator ARM64: native console logging.

## Dependencies

- `api`: kronos-core
- `kotlin-reflect`: JVM test scope only
