# Runtime Execution Flow

This section shows the end-to-end "SQL generation and execution" path from DSL to the database. It covers the compiler transformer, Action/Query tasks, named parameters, plugins and logging.

Sequence (simplified):

```mermaid
sequenceDiagram
  participant DSL as Caller/DSL
  participant KCP as Compiler Plugin<br/>kronos-compiler-plugin
  participant CORE as kronos-core<br/>ClauseInfo/Task
  participant NP as NamedParameterUtils
  participant DS as KronosDataSourceWrapper
  participant P as TaskEventPlugin(s)
  participant LOG as kronos-logging

  DSL->>KCP: Write DSL (Select/Insert/...)
  KCP-->>DSL: Inject KPojo bodies / collect structures at compile time
  DSL->>CORE: Build Select/Insert/Update/Delete ClauseInfo
  Note over CORE: Build KronosAtomic( Query | Action )Task
  P-->>CORE: Registered before* hooks can intercept
  CORE->>NP: parseSqlStatement + substituteNamedParameters
  NP-->>CORE: JDBC SQL + ordered args array
  CORE->>DS: Execute (Query/Update/BatchUpdate)
  DS-->>CORE: Results (List/Map/Object or OperationResult)
  CORE->>P: Trigger after* hooks (e.g., query lastInsertId)
  CORE->>LOG: defaultLogger prints SQL, params, row counts / result stats
  LOG-->>CORE: Adapted to a concrete logging backend
```

Key notes:
- Transformer (compile-time)
  - kronos-compiler-plugin rewrites/injects KPojo method bodies to ease runtime usages.
- ClauseInfo -> Task
  - ClauseInfo is only a data carrier; Task chains named parameter parsing, data source execution, plugins and logging.
- NamedParameterUtils
  - Safely turns named SQL into JDBC `?` placeholders and an ordered args array.
- KronosDataSourceWrapper
  - Unified forList/forMap/forObject/update/batchUpdate/transact API.
- TaskEventPlugin
  - Four hooks: before/after Query/Action.
- Logging
  - Via KLogger/KLoggerFactory and kronos-logging adapters for colored/structured logs.
