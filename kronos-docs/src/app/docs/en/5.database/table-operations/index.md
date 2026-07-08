{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Make sure each `KPojo` has the table, column, primary key, index, and strategy annotations required by the table operation.

> **Note**
> Table operations can be invoked through `Kronos.dataSource.table` after configuring the default data source, or through a concrete `KronosDataSourceWrapper.table`.
> Schema sync and CTAS have dedicated guides: {{ $.keyword("database/schema-sync", ["Schema Sync"]) }} and {{ $.keyword("database/create-table-as-select", ["Create Table As Select"]) }}.

## Build DDL statements and preview SQL

`TableOperation` exposes build APIs for dry-run review. Use statement builders when you need a syntax object, and task builders when you need the rendered SQL and parameters for the active dialect.

```kotlin group="Dry Run 1" name="statement" icon="kotlin"
val table = Kronos.dataSource.table

val createStatement = table.buildCreateTableStatement(User())
val dropStatement = table.buildDropTableStatement("user", ifExists = true)
val truncateStatement = table.buildTruncateTableStatement("user", restartIdentity = false)
```

```kotlin group="Dry Run 1" name="task" icon="kotlin"
val dropTask = table.buildDropTableTask("user")
val truncateTask = table.buildTruncateTableTask("user", restartIdentity = false)

val (sql, params, atomicTasks) = truncateTask

println(sql)
println(params)
println(atomicTasks.map { it.operationType })
```

```text group="Dry Run 1" name="SQLite output"
DELETE FROM "user"
{}
[TRUNCATE, TRUNCATE, TRUNCATE]
```

CTAS can also be built before execution.

```kotlin group="Dry Run 2" name="ctas task" icon="kotlin"
val paidOrders = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }

val task = table.buildCreateTableAsSelectTask(OrderArchive(), paidOrders)
val (ctasSql, ctasParams) = task
```

## 1. {{ $.title("exists(tableName)")}} whether the table exists

Check if the table exists by table name.

- **Function declaration**

    ```kotlin
    fun exists(tableName: String): Boolean
    ```

- **Usage Example**

    ```kotlin
    val exists = wrapper.table.exists("user")
    ```

- **Parameters**

  {{ $.params([['tableName', 'Table name', 'String']]) }}

- **Return value**

  `Boolean` - Whether the table exists

{{ $.hr() }}

## 2. {{ $.title("exists<T>(instance)")}} whether the table exists

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

Check if the table exists by KPojo.

- **Function declaration**
    
    ```kotlin
    inline fun <reified T : KPojo> exists(instance: T = T::class.createInstance()): Boolean
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["How does Kronos implement instantiating KClass&lt;KPojo&gt; without relying on reflection?"])}}_</small>

- **Usage Example**

    ```kotlin
    val exists = wrapper.table.exists(User())
    // or
    val exists = wrapper.table.exists<User>()
    ```

- **Parameters**

  {{ $.params([['instance', 'Entity object. The generic overload can create the default instance when `T` has a no-argument constructor.', 'T', 'T::class.createInstance()']]) }}

- **Return value**

  `Boolean` - Whether the table exists

{{ $.hr() }}

## 3. {{ $.title("createTable(instance)")}} Create a table

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

Create a table by KPojo.

- **Function declaration**
    
    ```kotlin
    inline fun <reified T : KPojo> createTable(instance: T = T::class.createInstance())
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["How does Kronos implement instantiating KClass&lt;KPojo&gt; without relying on reflection?"])}}_</small>

- **Usage Example**

    ```kotlin
    wrapper.table.createTable(User())
    // or
    wrapper.table.createTable<User>()
    ```

- **Parameters**

  {{ $.params([['instance', 'Entity object. The generic overload can create the default instance when `T` has a no-argument constructor.', 'T', 'T::class.createInstance()']]) }}

{{ $.hr() }}

## 4. {{ $.title("createTable(target, query)")}} Create table as select

Create a table from a `KSelectable` or `UnionClause` query by passing the target KPojo and the source query to `wrapper.table.createTable(target, query)`.

- **Function declaration**

    ```kotlin
    inline fun <reified T : KPojo> createTable(
        instance: T = T::class.createInstance(),
        query: KSelectable<*>
    )

    inline fun <reified T : KPojo> createTable(
        instance: T = T::class.createInstance(),
        query: UnionClause<*>
    )
    ```

- **Usage Example**

    ```kotlin
    val paidOrders = Order()
        .select { [it.id, it.userId, it.status] }
        .where { it.status == 1 }

    wrapper.table.createTable(OrderArchive(), paidOrders)
    ```

- **Generated SQL**

    ```sql
    CREATE TABLE IF NOT EXISTS `order_archive` AS
    SELECT `id`, `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`status` = :status
    ```

CTAS uses the query output as the table shape. Use normal `createTable(Target())` and then `KSelectable.insert<Target>()` when the table needs KPojo schema metadata such as indexes, comments, defaults, or primary key definitions.

{{ $.hr() }}

## 5. {{ $.title("truncateTable(vararg tableName, restartIdentity)")}} Clear the table

Clear one or more tables by table name.

- **Function declaration**

    ```kotlin
    fun truncateTable(vararg tableName: String, restartIdentity: Boolean = true)
    ```

- **Usage Example**

    ```kotlin
    wrapper.table.truncateTable("user")
    wrapper.table.truncateTable("user", "audit_log", restartIdentity = false)
    ```

- **Parameters**

  {{ $.params([['tableName', 'Table name list', 'vararg String'], ['restartIdentity', 'Whether to reset the auto-increment value. PostgreSQL and SQLite have explicit reset behavior; other dialects keep their native truncate behavior.', 'Boolean', 'true']]) }}

{{ $.hr() }}

## 6. {{ $.title("truncateTable(instance, restartIdentity)")}} Clear the table

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

Clear the table by KPojo.

- **Function declaration**

    ```kotlin
    inline fun <reified T : KPojo> truncateTable(
        instance: T = T::class.createInstance(),
        restartIdentity: Boolean = true
    )
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["Kronos is how to instantiate KClass&lt;KPojo&gt; without relying on reflection?"])}}_</small>

- **Usage Example**

    ```kotlin
    wrapper.table.truncateTable(User())
    // or
    wrapper.table.truncateTable<User>()
    ```

- **Parameters**

  {{ $.params([['instance', 'Entity object. The generic overload can create the default instance when `T` has a no-argument constructor.', 'T', 'T::class.createInstance()'], ['restartIdentity', 'Whether to reset the auto-increment value. PostgreSQL and SQLite have explicit reset behavior; other dialects keep their native truncate behavior.', 'Boolean', 'true']]) }}

{{ $.hr() }}

## 7. {{ $.title("dropTable(vararg tableNames)")}} Delete the table

Delete one or more tables by table name.

- **Function declaration**

    ```kotlin
    fun dropTable(vararg tableNames: String)
    ```

- **Usage Example**

    ```kotlin
    wrapper.table.dropTable("user")
    wrapper.table.dropTable("user", "audit_log")
    ```

- **Parameters**

  {{ $.params([['tableNames', 'Table name list', 'vararg String']]) }}

{{ $.hr() }}

## 8. {{ $.title("dropTable(instance)")}} Delete the table

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

- **Function declaration**

    ```kotlin
    inline fun <reified T : KPojo> dropTable(instance: T = T::class.createInstance())
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["Kronos is how to instantiate KClass&lt;KPojo&gt; without relying on reflection?"])}}_</small>

- **Usage Example**

    ```kotlin
    wrapper.table.dropTable(User())
    // or
    wrapper.table.dropTable<User>()
    ```

- **Parameters**
  
  {{ $.params([['instance', 'Entity object. The generic overload can create the default instance when `T` has a no-argument constructor.', 'T', 'T::class.createInstance()']]) }}

{{ $.hr() }}

## 9. {{ $.title("syncTable(instance)")}} Synchronize table structure

- **Generic parameters**： `<T>` Entity object type, inherited from `KPojo`

{{ $.hr(50) }}

- **Function declaration**
    
    ```kotlin
    inline fun <reified T : KPojo> syncTable(instance: T = T::class.createInstance()): Boolean
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["Kronos is how to instantiate KClass&lt;KPojo&gt; without relying on reflection?"])}}_</small>

- **Usage Example**

    ```kotlin
    wrapper.table.syncTable(User())
    // or
    wrapper.table.syncTable<User>()
    ```

- **Parameters**

  {{ $.params([['instance', 'Entity object. The generic overload can create the default instance when `T` has a no-argument constructor.', 'T', 'T::class.createInstance()']]) }}

- **Return value**

  `Boolean` - `false` means Kronos created the target table. `true` means Kronos synchronized an existing table.

{{ $.hr() }}

> **Note**
> When several table operations use the same object in sequence, keep that object in a local variable and pass it to `exists(instance)`, `createTable(instance)`, `dropTable(instance)`, or `syncTable(instance)`.
