# Action 机制

- 核心契约：
  - KActionInfo（com.kotlinorm.interfaces）：kClass/tableName/whereClause 通用信息体；
  - KOperationType（com.kotlinorm.enums）：INSERT/UPDATE/DELETE/SELECT 等；
- 运行时对象：
  - KronosAtomicActionTask / KronosAtomicBatchTask / KronosActionTask（com.kotlinorm.beans.task）
    - 负责批量聚合（相邻 SQL 合并为批处理）、事务执行、收敛结果；
    - 通过 utils.execute 执行，并触发事件与日志；
- 生命周期钩子：
  - beforeActionEvents / afterActionEvents（com.kotlinorm.beans.task.ActionEvent）
  - 通过 registerTaskEventPlugin 注入（见 TaskEventPlugin 机制）。
- 结果模型：
  - KronosOperationResult：affectedRows + stash（可附加 lastInsertId 等）。

注意：
- ActionTask 内部使用 KronosDataSourceWrapper.update/batchUpdate 执行；
- 执行前后均会触发事件；
- 插件可在 afterAction 设置衍生信息（如 LastInsertId）。
