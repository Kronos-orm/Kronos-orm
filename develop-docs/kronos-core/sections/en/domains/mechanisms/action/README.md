# Action Mechanism

- Core contracts:
  - KActionInfo (com.kotlinorm.interfaces): common payload (kClass/tableName/whereClause);
  - KOperationType (com.kotlinorm.enums): INSERT/UPDATE/DELETE/SELECT, etc.
- Runtime objects:
  - KronosAtomicActionTask / KronosAtomicBatchTask / KronosActionTask (com.kotlinorm.beans.task)
    - Merge adjacent SQL into batch, run inside transaction, aggregate results;
    - Use utils.execute, and trigger events and logging.
- Lifecycle hooks:
  - beforeActionEvents / afterActionEvents (com.kotlinorm.beans.task.ActionEvent)
  - Registered via registerTaskEventPlugin (see TaskEventPlugin mechanism).
- Result model:
  - KronosOperationResult: affectedRows + stash (can carry lastInsertId, etc.).

Notes:
- ActionTask uses KronosDataSourceWrapper.update/batchUpdate under the hood;
- Events are triggered before and after execution;
- Plugins can attach derived info (e.g., LastInsertId) in afterAction.
