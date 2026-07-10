{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Use this page to check setup, SQL generation, and runtime behavior.

## Resolve dependency coordinates

Use the stable Kronos version from the docs macro in docs pages and a current stable version for third-party drivers.

```kotlin group="Dependencies" name="build.gradle.kts" icon="gradle"
dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

Replace `<latest-stable>` with the current stable driver version for your database and JDK.

For a complete setup, see {{ $.keyword("getting-started/quick-start", ["Quick Start"]) }} and {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }}.

## Compiler plugin setup needs a check

Kronos compile-time support runs during Kotlin compilation. Compile the module that declares `KPojo` classes or writes Kronos DSL calls, then check the build output.

```bash group="Compiler plugin" name="Gradle" icon="terminal"
./gradlew :app:compileKotlin
```

```text group="Compiler plugin" name="output"
[Kronos] Kronos compiler plugin K2 initialized
BUILD SUCCESSFUL
```

Apply the Kronos Gradle or Maven plugin to each source set that declares entities or query DSL. The complete build-tool examples and source-set rule are in {{ $.keyword("configuration/compiler-plugins", ["Compiler Plugins"]) }}.

## Check KPojo generated members

Generated members such as `__tableName`, `toDataMap()`, `kronosColumns()`, and dynamic accessors are available after the source set is compiled with Kronos support.

```kotlin group="KPojo Check" name="kotlin" icon="kotlin"
val user = User(id = 7, name = "Ada")

println(user.__tableName)
println(user.toDataMap())
println(user.kronosColumns().map { it.name })
```

```text group="KPojo Check" name="output"
tb_user
{id=7, name=Ada}
[id, name]
```

When runtime output mentions `__tableName must be overridden by the compiler plugin`, compile the module with the Kronos plugin active and reimport the project in the IDE. See {{ $.keyword("configuration/compiler-plugins", ["Check generated KPojo members"]) }} for the full check.

## Projection or scalar subquery diagnostics appear

Projection result fields need stable names. Give function, aggregate, window, raw SQL, and scalar subquery select items an alias.

```kotlin group="Projection 1" name="alias" icon="kotlin"
User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .toList()
```

Scalar subqueries used as values need one selected field and `limit(1)`.

```kotlin group="Projection 2" name="scalar" icon="kotlin"
User()
    .select { user ->
        [
            user.id,
            Order()
                .select { it.amount }
                .where { it.userId == user.id }
                .limit(1)
                .alias("lastAmount")
        ]
    }
    .toList()
```

The diagnostic codes `KRONOS_SELECT_ITEM_REQUIRES_ALIAS`, `KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT`, and `KRONOS_SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN` are listed in {{ $.keyword("configuration/compiler-plugins", ["Fix common diagnostics"]) }}. Query-shape examples are in {{ $.keyword("query/projection", ["Projection"]) }} and {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## Configure the data source

Kronos ORM operations need a configured `Kronos.dataSource`.

```kotlin group="DataSource 1" name="kotlin" icon="kotlin"
val wrapper = KronosJdbcWrapper(dataSource)

Kronos.dataSource = { wrapper }

val users = User().select().toList()
```

When a command needs a specific data source, call it through the wrapper.

```kotlin group="DataSource 2" name="wrapper" icon="kotlin"
val users = User()
    .select()
    .toList(wrapper)
```

## Dialect output looks different

Kronos renders SQL from `KronosDataSourceWrapper.dbType`. Print the wrapper values when SQL quoting or pagination is unexpected.

```kotlin group="Dialect" name="kotlin" icon="kotlin"
val wrapper = KronosJdbcWrapper(dataSource)

println(wrapper.dbType)
println(wrapper.sqlDialect)
```

For quoting, pagination, upsert, DDL, and function examples, see {{ $.keyword("database/dialect-support", ["Database Dialect Support"]) }}.

## Check `where()` conditions

`select()` alone chooses the table and projection. Empty `where()` applies query-by-example from non-null queryable fields.

```kotlin group="Where 1" name="select" icon="kotlin"
User(id = 1).select()
// SELECT ... FROM user

User(id = 1).select().where()
// WHERE id = 1
```

A lambda controls that `where` call directly.

```kotlin group="Where 2" name="lambda" icon="kotlin"
User(id = 1)
    .select()
    .where { it.name == "Ada" }
// WHERE name = 'Ada'
```

For `where` composition rules, see {{ $.keyword("query/conditions", ["Conditions"]) }}.

## Review DataGuard write and DDL protection

DataGuard rejects full-table writes and protected table operations before execution.

```kotlin group="DataGuard 1" name="result" icon="kotlin"
DataGuardPlugin.enable()

User()
    .delete()
    .execute()
```

```text group="DataGuard 1" name="error"
UnsupportedOperationException: Delete operation is not allowed.
```

Add a condition or configure an allow rule for planned maintenance.

```kotlin group="DataGuard 2" name="allow" icon="kotlin"
DataGuardPlugin.enable {
    deleteAll {
        allow {
            tableName = "tmp_import_error"
        }
    }
}
```

For policy examples, see {{ $.keyword("advanced/data-guard", ["DataGuard"]) }}.

## Logic delete or optimistic lock changes the SQL

Logic delete and optimistic lock annotations add write conditions or assignments based on KPojo metadata.

```kotlin group="Strategy" name="kotlin" icon="kotlin"
data class User(
    @PrimaryKey
    var id: Int? = null,
    @LogicDelete
    var deleted: Boolean? = null,
    @Version
    var version: Int? = null
) : KPojo
```

For the mutation behavior, see {{ $.keyword("mutation/logic-delete", ["Logic Delete"]) }} and {{ $.keyword("mutation/optimistic-lock", ["Optimistic Lock"]) }}.

## View generated SQL

Use the task-building APIs when a page documents a `build...Task` form.

```kotlin group="SQL" name="kotlin" icon="kotlin"
val task = Kronos.dataSource.table.buildCreateTableAsSelectTask(
    OrderArchive(),
    Order().select { [it.id, it.userId] }
)

println(task.sql)
println(task.paramMap)
```

For SQL generated from select, join, subquery, and CTAS, see {{ $.keyword("query/subqueries", ["Subqueries"]) }} and {{ $.keyword("database/create-table-as-select", ["Create Table As Select"]) }}.

## Codegen or IDE plugin needs a starting point

Use Codegen when database metadata should produce KPojo files.

```kotlin group="Tools 1" name="codegen" icon="kotlin"
@file:DependsOn("com.kotlinorm:kronos-codegen:{{ $.kronosVersion() }}")
```

Use the IDEA plugin page for editor support and diagnostics.

```text group="Tools 2" name="pages"
Resources -> Code Generator
Resources -> IntelliJ IDEA Plugin
```

See {{ $.keyword("resources/codegen", ["Code Generator"]) }} and {{ $.keyword("resources/idea-plugin", ["IntelliJ IDEA Plugin"]) }}.
