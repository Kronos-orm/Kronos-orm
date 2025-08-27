# 日志

kronos-core 通过轻量接口对接独立的 kronos-logging 模块，输出结构化、彩色的 SQL 执行日志。

- 核心接口：
  - KLogger（com.kotlinorm.interfaces）：trace/debug/info/warn/error + isXEnabled
  - KLoggerFactory（typealias）：(Any) -> KLogger，用于按类型创建 logger
- 默认输出：
  - utils/TaskUtil.kt 中的 handleLogResult 与 logAndReturn 负责在查询/变更后输出：
    - Task 类型（批量/单条）、SQL、绑定参数（过滤 null）、影响行数或结果条数；
    - 可显示 lastInsertId 等衍生信息（结合插件）；
- 适配实现：
  - 由 kronos-logging 提供 KLogMessage 与日志适配器（如到常见日志框架）
  - 在应用端配置 Kronos.defaultLogger: KLoggerFactory 实现切换
- 关闭/调整：
  - 可重载 handleLogResult 或替换 defaultLogger 工厂以调整格式与级别。
