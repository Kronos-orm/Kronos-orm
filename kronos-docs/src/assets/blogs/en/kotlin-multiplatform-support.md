# Kronos Platform Support and Roadmap

Kronos uses one Kotlin ORM DSL across server-side JVM projects and Android/JVM applications. The current platform direction is:

| Target | Availability | Integration |
| --- | --- | --- |
| JVM server applications | Available | JDBC and framework-specific wrappers |
| Android/JVM | Preview in `0.2.5-SNAPSHOT` | Android `SQLiteDatabase` wrapper |
| Kotlin/Native and JavaScript | Roadmap | Platform-specific storage integrations |

## Android/JVM and SQLite

Android/JVM applications use `kronos-core` and a `KronosDataSourceWrapper` backed by Android `SQLiteDatabase`. The same table and CRUD DSL can be used in an Android application.

The [Android SQLite guide](/documentation/en/database/android-sqlite) contains the supported configuration, wrapper responsibilities, transaction scope, logging guidance, and the reference application. The complete implementation is available in [kronos-example-android](https://github.com/Kronos-orm/kronos-example-android).

## Roadmap

Kotlin/Native and JavaScript remain future platform targets. Their storage APIs and runtime constraints need dedicated integrations, so their design will evolve independently from the Android/JVM SQLite path.
