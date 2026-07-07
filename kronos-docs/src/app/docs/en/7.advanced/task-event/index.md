{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Use cases and creation

In Kronos, task event plugins trigger callbacks at different stages of task execution.

These events can help you observe task execution, or run specific logic before and after database work.

Kronos events expose the task `operationType`, SQL text, parameter map, structured statement, and the active `KronosDataSourceWrapper`. By listening to these events, you can intercept tasks, record logs, send notifications, or inspect task content before it is executed.

The `Logging`, `LastInsertId`, and `DataGuard` plugins are all implemented through the task event plugin mechanism.

### Create a task event plugin

Create a class or object that implements the `TaskEventPlugin` interface.

You can define behavior by implementing these callbacks:

- `doBeforeQuery`: triggered before a query task is executed.
- `doAfterQuery`: triggered after a query task is executed.
- `doBeforeAction`: triggered before an action task is executed, including `create`, `drop`, `alter`, `truncate`, `insert`, `update`, `delete`, and native `upsert`.
- `doAfterAction`: triggered after an action task is executed, including `create`, `drop`, `alter`, `truncate`, `insert`, `update`, `delete`, and native `upsert`.

> **Note**
> Why is there no dedicated `doBeforeUpsert` event?
>
> Task event plugins use query callbacks and action callbacks. Fallback `upsert()` emits the existence query and then the insert or update branch. Native `onConflict()` upsert is an action task whose `operationType` is `KOperationType.UPSERT`.

Task event function types are `QueryTaskEvent?` or `ActionTaskEvent?`. Set a callback to `null` to disable the corresponding event.

### Example

The following example defines a simple task event plugin that prints log messages before and after task execution:

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

    private var loaded = false
    var enabled: Boolean
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

Observable output for an update task:

```text group="Task event 2" name="result"
Before action: UPDATE
SQL: UPDATE `user` SET `name` = :nameNew WHERE `id` = :id
Updated with params: {nameNew=Ada, id=1}
```
