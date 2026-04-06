# Module kronos-logging

Pluggable logging module for Kronos ORM. Auto-detects the available logging framework at runtime.

## Detection Order

1. Android `android.util.Log`
2. Apache Commons Logging
3. JDK `java.util.logging`
4. SLF4J
5. Bundled simple logger (fallback, from kronos-core)

All adapters use reflection to avoid compile-time dependencies on logging frameworks.

## Dependencies

- `compileOnly`: kronos-core
- `implementation`: kotlin-reflect
