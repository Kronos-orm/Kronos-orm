{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 任务事件插件的使用场景和创建方法

在Kronos中，任务事件插件用于在任务执行的不同阶段触发特定的事件。

这些事件可以帮助我们监控任务的执行情况，或者在任务执行的不同阶段执行特定的操作。

Kronos中，支持在事件中获取任务的类型（如插入、更新、删除等）、任务的具体信息（如数据源信息、`sql`、`KClass`、表名、字段名等）以及任务的执行状态（如成功、失败等）。
通过监听这些事件，我们可以在任务执行的不同阶段执行特定的操作，比如拦截任务、记录日志、发送通知甚至是修改任务内容本身。

`Logging`、`LastInsertId`和`DataGuard`插件都是Kronos中重要的插件，他们就是通过任务事件插件来实现的。

### 创建任务事件插件

创建一个类或对象来实现`TaskEventPlugin`接口，即可实现一个任务事件插件。

您可以通过实现以下方法来定义任务事件插件的行为：

- doBeforeQuery: 在查询任务执行前触发。
- doAfterQuery: 在查询任务执行后触发。
- doBeforeAction: 在操作任务执行前触发，支持包括`create`、`drop`、`alter`、`truncate`、`insert`、`update`、`delete`等操作。
- doAfterAction: 在操作任务执行后触发，支持包括`create`、`drop`、`alter`、`truncate`、`insert`、`update`、`delete`等操作。

> **Note**
> 为什么没有**Upsert**事件？
> 
> 因为**Upsert**操作实际上是一个组合操作，包含了插入和更新两个步骤，因此在Kronos中没有单独的`Upsert`事件。

任务函数的类型为`QueryTaskEvent?`或`ActionTaskEvent?`，您可以将它们设置为`null`以禁用对应的事件。

### 示例

以下是一个简单的示例，定义了一个简单的任务事件插件实现，它会在任务执行前后打印一些日志信息：

```kotlin
class MyTaskEventPlugin : TaskEventPlugin {
    override val doBeforeQuery = { task, wrapper ->
        println("Before query for task ${task.type}")
    }
    override val doAfterQuery = { task, wrapper ->
        println("Task ${task.type} executed with SQL: ${task.sql}")
    }
    override val doBeforeAction = { task, wrapper ->
        println("DBType: ${wrapper.dbType}")
        println("Before action for task ${task.type} on table ${task.tableName}")
    }
    override val doAfterAction = { task, wrapper ->
        when (task.operationType){
            OperationType.INSERT -> {
                println("Inserted data: ${task.data}")
            }
            OperationType.UPDATE -> {
                println("Updated data: ${task.data}")
            }
            else -> {
                println("Task ${task.type} completed")
            }
        }
    }
    
    private var loaded = false // 是否已加载插件
    var enabled: Boolean // 是否启用
        get() = loaded
        set(value) {
            loaded = value
            if (value) {
                registerTaskEventPlugin(this)
            } else {
                unregisterTaskEventPlugin(this)
            }
        }
}
```
