# TaskEventPlugin 机制

- 接口：TaskEventPlugin（com.kotlinorm.interfaces）
  - 暴露四个可选回调：
    - doBeforeQuery: QueryTaskEvent?
    - doAfterQuery: QueryTaskEvent?
    - doBeforeAction: ActionTaskEvent?
    - doAfterAction: ActionTaskEvent?
- 事件类型：KTask.kt 中的 typealias
  - ActionTaskEvent = (task, wrapper) -> Unit
  - QueryTaskEvent = (task, wrapper) -> Unit
- 注册中心：TasKEventRegister（com.kotlinorm.beans.task）
  - registerTaskEventPlugin(plugin)
  - unregisterTaskEventPlugin(plugin)
  - 内部维护 before/after Query/Action 的事件列表
- 触发点：
  - Query 执行前后、Action 执行前后（见 utils/TaskUtil.kt 和 beans/task/*）

示例：LastInsertIdPlugin
- 位于 com.kotlinorm.plugins
- 在 doAfterAction 钩子中，当 operationType=INSERT 且 useIdentity=true 时，回查 lastInsertId；
- 通过扩展属性 KronosOperationResult.lastInsertId 暴露给调用层；
- 支持全局开关 enabled 或 InsertClause.withId() 局部开启。
