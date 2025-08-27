# 1. Module Overview

kronos-logging provides implementations of the Kronos logging contract (KLogger/KLoggerFactory) and a runtime detector. It lets kronos-core output structured SQL and task logs to the logging backend you already use.

- Adapters (via reflection): SLF4J, java.util.logging (JUL), Apache Commons Logging, Android Log.
- Default bundled logger: colored console/file output (implementation in kronos-core), used when no external logger is available.
- Auto-detection: backend detection is automatic in typical setups; the helper KronosLoggerApp.detectLoggerImplementation() is available for explicit control.

Use-cases:
- Integrate Kronos logs into your existing logging pipeline.
- Switch logging backend without touching core.
- Customize default logger behavior (level switches, datetime pattern, file naming, output paths).
