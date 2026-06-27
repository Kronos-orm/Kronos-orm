{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Use cases and creation

In Kronos, task event plugins trigger callbacks at different stages of task execution.

These events can help you observe task execution, or run specific logic before and after database work.

Kronos events expose the task type, task details such as datasource information, `sql`, `KClass`, table names, field names, and execution status. By listening to these events, you can intercept tasks, record logs, send notifications, or even modify task content before it is executed.

The `Logging`, `LastInsertId`, and `DataGuard` plugins are all implemented through the task event plugin mechanism.

### Create a task event plugin

Create a class or object that implements the `TaskEventPlugin` interface.

You can define behavior by implementing these callbacks:

- `doBeforeQuery`: triggered before a query task is executed.
- `doAfterQuery`: triggered after a query task is executed.
- `doBeforeAction`: triggered before an action task is executed, including `create`, `drop`, `alter`, `truncate`, `insert`, `update`, and `delete`.
- `doAfterAction`: triggered after an action task is executed, including `create`, `drop`, `alter`, `truncate`, `insert`, `update`, and `delete`.

> **Note**
> Why is there no dedicated **Upsert** event?
>
> Because **Upsert** is a composed operation made of insert and update steps, Kronos does not expose a separate `Upsert` event.

Task event function types are `QueryTaskEvent?` or `ActionTaskEvent?`. Set a callback to `null` to disable the corresponding event.

### Example

The following example defines a simple task event plugin that prints log messages before and after task execution:

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
        when (task.operationType) {
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
