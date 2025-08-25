# TaskEventPlugin Mechanism

- Interface: TaskEventPlugin (com.kotlinorm.interfaces)
  - Four optional callbacks:
    - doBeforeQuery: QueryTaskEvent?
    - doAfterQuery: QueryTaskEvent?
    - doBeforeAction: ActionTaskEvent?
    - doAfterAction: ActionTaskEvent?
- Event types: typealias in KTask.kt
  - ActionTaskEvent = (task, wrapper) -> Unit
  - QueryTaskEvent = (task, wrapper) -> Unit
- Registry: TasKEventRegister (com.kotlinorm.beans.task)
  - registerTaskEventPlugin(plugin)
  - unregisterTaskEventPlugin(plugin)
  - Keeps event lists for before/after Query/Action
- Trigger points:
  - Before/after Query execution; before/after Action execution (see utils/TaskUtil.kt and beans/task/*)

Example: LastInsertIdPlugin
- Package: com.kotlinorm.plugins
- In doAfterAction, when operationType=INSERT and useIdentity=true, query back lastInsertId;
- Expose via extension KronosOperationResult.lastInsertId to callers;
- Controlled by global flag enabled or local InsertClause.withId().
