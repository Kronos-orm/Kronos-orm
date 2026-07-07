{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

请确保每个`KPojo`带有表操作所需的表名、列名、主键、索引和策略注解。

> **Note**
> 配置默认数据源后，可以通过`Kronos.dataSource.table`调用表操作；也可以通过某个具体的`KronosDataSourceWrapper.table`调用。
> 表结构同步和 CTAS 有独立说明：{{ $.keyword("database/schema-sync", ["表结构同步"]) }} 与 {{ $.keyword("database/create-table-as-select", ["基于查询创建表"]) }}。

## 构建 DDL statement 和预览 SQL

`TableOperation` 提供 build API 作为 dry-run 入口。需要语法对象时使用 statement builder；需要当前方言渲染后的 SQL 和参数时使用 task builder。

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

CTAS 也可以先构建任务，再决定是否执行。

```kotlin group="Dry Run 2" name="ctas task" icon="kotlin"
val paidOrders = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }

val task = table.buildCreateTableAsSelectTask(OrderArchive(), paidOrders)
val (ctasSql, ctasParams) = task
```

## 1. {{ $.title("exists(tableName)")}} 表是否存在

通过表名判断表是否存在

- **函数声明**

    ```kotlin
    fun exists(tableName: String): Boolean
    ```

- **使用示例**

    ```kotlin
    val exists = wrapper.table.exists("user")
    ```

- **接收参数**

    {{ $.params([['tableName', '表名', 'String']]) }}

- **返回值**

    `Boolean` 表是否存在

{{ $.hr() }}

## 2. {{ $.title("exists<T>(instance)")}} 表是否存在

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

{{ $.hr(50) }}

通过KPojo判断表是否存在

- **函数声明**
    
    ```kotlin
    inline fun <reified T : KPojo> exists(instance: T = T::class.createInstance()): Boolean
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**

    ```kotlin
    val exists = wrapper.table.exists(User())
    // 或
    val exists = wrapper.table.exists<User>()
    ```

- **接收参数**

    {{ $.params([['instance', '实体对象。当`T`具备无参构造时，泛型重载可以创建默认实例。', 'T', 'T::class.createInstance()']]) }}

- **返回值**

    `Boolean` - 表是否存在

{{ $.hr() }}

## 3. {{ $.title("createTable(instance)")}} 创建表

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

{{ $.hr(50) }}

通过实体类创建表

- **函数声明**

    ```kotlin
    inline fun <reified T : KPojo> createTable(instance: T = T::class.createInstance())
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**

    ```kotlin
    wrapper.table.createTable(User())
    // 或
    wrapper.table.createTable<User>()
    ```

- **接收参数**

    {{ $.params([['instance', '实体对象。当`T`具备无参构造时，泛型重载可以创建默认实例。', 'T', 'T::class.createInstance()']]) }}

{{ $.hr() }}

## 4. {{ $.title("createTable(target, query)")}} CREATE TABLE AS SELECT

把目标 KPojo 和 `KSelectable` 或 `UnionClause` 来源查询传给 `wrapper.table.createTable(target, query)`，可以用查询结果创建表。

- **函数声明**

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

- **使用示例**

    ```kotlin
    val paidOrders = Order()
        .select { [it.id, it.userId, it.status] }
        .where { it.status == 1 }

    wrapper.table.createTable(OrderArchive(), paidOrders)
    ```

- **生成 SQL**

    ```sql
    CREATE TABLE IF NOT EXISTS `order_archive` AS
    SELECT `id`, `user_id` AS `userId`, `status`
    FROM `order`
    WHERE `order`.`status` = :status
    ```

CTAS 使用查询输出决定表形态。需要索引、注释、默认值、主键等 KPojo schema 元数据时，先使用普通 `createTable(Target())` 建表，再使用 `KSelectable.insert<Target>()` 写入数据。

{{ $.hr() }}

## 5. {{ $.title("truncateTable(vararg tableName, restartIdentity)")}} 清空表

通过表名清空一个或多个表。

- **函数声明**

    ```kotlin
    fun truncateTable(vararg tableName: String, restartIdentity: Boolean = true)
    ```

- **使用示例**

    ```kotlin
    wrapper.table.truncateTable("user")
    wrapper.table.truncateTable("user", "audit_log", restartIdentity = false)
    ```

- **接收参数**

    {{ $.params([['tableName', '表名列表', 'vararg String'], ['restartIdentity', '是否重置自动递增值。PostgreSQL 和 SQLite 有明确的重置行为，其他方言保持各自原生 truncate 行为。', 'Boolean', 'true']]) }}

{{ $.hr() }}

## 6. {{ $.title("truncateTable(instance, restartIdentity)")}} 清空表

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

{{ $.hr(50) }}

通过实体类清空表

- **函数声明**

    ```kotlin
    inline fun <reified T : KPojo> truncateTable(
        instance: T = T::class.createInstance(),
        restartIdentity: Boolean = true
    )
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**

    ```kotlin
    wrapper.table.truncateTable(User())
    // 或
    wrapper.table.truncateTable<User>()
    ```

- **接收参数**

    {{ $.params([['instance', '实体对象。当`T`具备无参构造时，泛型重载可以创建默认实例。', 'T', 'T::class.createInstance()'], ['restartIdentity', '是否重置自动递增值。PostgreSQL 和 SQLite 有明确的重置行为，其他方言保持各自原生 truncate 行为。', 'Boolean', 'true']]) }}

{{ $.hr() }}

## 7. {{ $.title("dropTable(vararg tableNames)")}} 删除表

通过表名删除一个或多个表。

- **函数声明**

    ```kotlin
    fun dropTable(vararg tableNames: String)
    ```

- **使用示例**

    ```kotlin
    wrapper.table.dropTable("user")
    wrapper.table.dropTable("user", "audit_log")
    ```

- **接收参数**

    {{ $.params([['tableNames', '表名列表', 'vararg String']]) }}

{{ $.hr() }}

## 8. {{ $.title("dropTable(instance)")}} 删除表

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

{{ $.hr(50) }}

通过实体类删除表

- **函数声明**

    ```kotlin
    inline fun <reified T : KPojo> dropTable(instance: T = T::class.createInstance())
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**

    ```kotlin
    wrapper.table.dropTable(User())
    // 或
    wrapper.table.dropTable<User>()
    ```

- **接收参数**

    {{ $.params([['instance', '实体对象。当`T`具备无参构造时，泛型重载可以创建默认实例。', 'T', 'T::class.createInstance()']]) }}

{{ $.hr() }}

## 9. {{ $.title("syncTable(instance)")}} 表结构同步和变更

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

通过实体类同步表结构

- **函数声明**

    ```kotlin
    inline fun <reified T : KPojo> syncTable(instance: T = T::class.createInstance()): Boolean
    ```

<small>_{{ $.keyword("advanced/kpojo-dynamic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**
    
    ```kotlin
    wrapper.table.syncTable(User())
    // 或
    wrapper.table.syncTable<User>()
    ```

- **接收参数**

    {{ $.params([['instance', '实体对象。当`T`具备无参构造时，泛型重载可以创建默认实例。', 'T', 'T::class.createInstance()']]) }}

- **返回值**

    `Boolean` - `false`表示 Kronos 创建目标表。`true`表示 Kronos 同步已有表。

{{ $.hr() }}

> **Note**
> 对同一个实体对象连续执行多个表操作时，可以把对象保存在局部变量中，再传给`exists(instance)`、`createTable(instance)`、`dropTable(instance)`或`syncTable(instance)`。
