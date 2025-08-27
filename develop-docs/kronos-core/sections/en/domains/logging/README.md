# Logging

kronos-core integrates with the separate kronos-logging module via lightweight interfaces to emit structured, colored SQL execution logs.

- Core interfaces:
  - KLogger (com.kotlinorm.interfaces): trace/debug/info/warn/error + isXEnabled
  - KLoggerFactory (typealias): (Any) -> KLogger, create logger by class/tag
- Default output behavior:
  - In utils/TaskUtil.kt, handleLogResult and logAndReturn print after query/action:
    - Task type (batch/single), SQL, filtered bind params, affected rows or result size;
    - Derived info like lastInsertId can be shown (via plugins);
- Adapters:
  - Provided by kronos-logging: KLogMessage and adapters to common logging stacks;
  - Configure Kronos.defaultLogger: KLoggerFactory to switch implementation in apps;
- Disable/tune:
  - Override handleLogResult or replace the defaultLogger factory to change format and level.
