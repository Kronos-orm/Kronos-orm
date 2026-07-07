{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 任务事件插件的使用场景和创建方法

在Kronos中，任务事件插件用于在任务执行的不同阶段触发特定的事件。

这些事件可以帮助我们监控任务的执行情况，或者在任务执行的不同阶段执行特定的操作。

Kronos 事件中可以获取任务的 `operationType`、SQL 文本、参数 Map、结构化 statement 以及当前 `KronosDataSourceWrapper`。
通过监听这些事件，我们可以在任务执行的不同阶段执行特定操作，比如拦截任务、记录日志、发送通知或检查任务内容。

`Logging`、`LastInsertId`和`DataGuard`插件都是Kronos中重要的插件，他们就是通过任务事件插件来实现的。

### 创建任务事件插件

创建一个类或对象来实现`TaskEventPlugin`接口，即可实现一个任务事件插件。

您可以通过实现以下方法来定义任务事件插件的行为：

- doBeforeQuery: 在查询任务执行前触发。
- doAfterQuery: 在查询任务执行后触发。
- doBeforeAction: 在操作任务执行前触发，支持`create`、`drop`、`alter`、`truncate`、`insert`、`update`、`delete`和原生`upsert`。
- doAfterAction: 在操作任务执行后触发，支持`create`、`drop`、`alter`、`truncate`、`insert`、`update`、`delete`和原生`upsert`。

> **Note**
> 为什么没有单独的`doBeforeUpsert`事件？
>
> 任务事件插件只有 query 回调和 action 回调。fallback `upsert()` 会先触发存在性查询，再触发 insert 或 update 分支；原生 `onConflict()` upsert 会作为 `operationType = KOperationType.UPSERT` 的 action 任务触发。

任务函数的类型为`QueryTaskEvent?`或`ActionTaskEvent?`，您可以将它们设置为`null`以禁用对应的事件。

### 示例

以下是一个简单的示例，定义了一个简单的任务事件插件实现，它会在任务执行前后打印一些日志信息：

```kotlin group="Task event 1" name="kotlin" icon="kotlin"
import com.kotlinorm.beans.task.registerTaskEventPlugin
import com.kotlinorm.beans.task.unregisterTaskEventPlugin
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.TaskEventPlugin

class MyTaskEventPlugin : TaskEventPlugin {
    override val doBeforeQuery = { task, wrapper ->
        println("Before query: ${task.operationType} on ${wrapper.dbType}")
        println("SQL: ${task.sql}")
    }
    override val doAfterQuery = { task, wrapper ->
        println("Query finished with params: ${task.paramMap}")
    }
    override val doBeforeAction = { task, wrapper ->
        println("DBType: ${wrapper.dbType}")
        println("Before action: ${task.operationType}")
        println("SQL: ${task.sql}")
    }
    override val doAfterAction = { task, wrapper ->
        when (task.operationType) {
            KOperationType.INSERT -> {
                println("Inserted with params: ${task.paramMap}")
            }
            KOperationType.UPDATE -> {
                println("Updated with params: ${task.paramMap}")
            }
            KOperationType.UPSERT -> {
                println("Native upsert with params: ${task.paramMap}")
            }
            else -> {
                println("Action ${task.operationType} completed")
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

update 任务的可观察输出形态如下：

```text group="Task event 2" name="result"
Before action: UPDATE
SQL: UPDATE `user` SET `name` = :nameNew WHERE `id` = :id
Updated with params: {nameNew=Ada, id=1}
```
