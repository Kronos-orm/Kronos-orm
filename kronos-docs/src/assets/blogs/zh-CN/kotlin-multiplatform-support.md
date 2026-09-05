# Kronos 平台支持与路线图

Kronos 在服务端 JVM 项目与 Android/JVM 应用中使用同一套 Kotlin ORM DSL。当前平台方向如下：

| 平台 | 可用性 | 接入方式 |
| --- | --- | --- |
| JVM 服务端应用 | 已可用 | JDBC 与框架专用 wrapper |
| Android/JVM | `0.3.0` 可用 | Android `SQLiteDatabase` wrapper |
| Kotlin/Native 与 JavaScript | 路线图 | 平台专用存储集成 |

## Android/JVM 和 SQLite

Android/JVM 应用使用 `kronos-core`，以及基于 Android `SQLiteDatabase` 的 `KronosDataSourceWrapper`。应用可以继续使用相同的表操作与 CRUD DSL。

[Android SQLite 指南](/documentation/zh-CN/database/android-sqlite) 集中说明配置、wrapper 职责、事务范围、日志建议和参考应用。完整实现见 [kronos-example-android](https://github.com/Kronos-orm/kronos-example-android)。

## 后续路线

Kotlin/Native 与 JavaScript 是后续平台目标。两者的存储 API 和运行时约束需要平台专用集成，其设计会独立于 Android/JVM SQLite 路径演进。
