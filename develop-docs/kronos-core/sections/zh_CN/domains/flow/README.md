# 运行时执行流

本节给出从 DSL 到数据库的“SQL 生成与执行全流程”，覆盖 transformer、Action/Query、命名参数、插件与日志。

时序（简化）：

```mermaid
sequenceDiagram
  participant DSL as 调用端/DSL
  participant KCP as 编译期插件<br/>kronos-compiler-plugin
  participant CORE as kronos-core<br/>ClauseInfo/Task
  participant NP as NamedParameterUtils
  participant DS as KronosDataSourceWrapper
  participant P as TaskEventPlugin(s)
  participant LOG as kronos-logging

  DSL->>KCP: 书写 DSL（Select/Insert/...）
  KCP-->>DSL: 编译期注入 KPojo 方法体/收集结构
  DSL->>CORE: 构造 Select/Insert/Update/Delete ClauseInfo
  Note over CORE: 构造 KronosAtomic( Query | Action )Task
  P-->>CORE: 注册的 before* 事件可拦截
  CORE->>NP: parseSqlStatement + substituteNamedParameters
  NP-->>CORE: JDBC SQL + 有序参数数组
  CORE->>DS: 执行（Query/Update/BatchUpdate）
  DS-->>CORE: 结果（List/Map/Object or OperationResult）
  CORE->>P: 触发 after* 事件（如回查 lastInsertId）
  CORE->>LOG: 通过 defaultLogger 打印 SQL、参数、影响行数/结果统计
  LOG-->>CORE: 适配具体日志实现
```

关键节点说明：
- Transformer（编译期）
  - 由 kronos-compiler-plugin 在 IR 阶段改写/注入 KPojo 等方法体，便于运行时调用；
- ClauseInfo -> Task
  - ClauseInfo 仅是数据载体；Task 负责串起命名参数解析、数据源执行、插件与日志；
- NamedParameterUtils
  - 将命名参数 SQL 安全地转为 JDBC `?` 占位与参数数组；
- KronosDataSourceWrapper
  - 统一 forList/forMap/forObject/update/batchUpdate/transact 接口；
- TaskEventPlugin
  - 支持四种钩子：before/after Query/Action；
- Logging
  - 通过 KLogger/KLoggerFactory 与 kronos-logging 适配器输出彩色/结构化日志。
