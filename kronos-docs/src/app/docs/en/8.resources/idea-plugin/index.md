{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

The Kronos IDEA plugin brings Kronos compiler-plugin information into IntelliJ IDEA. It is useful when your project already builds with the Gradle or Maven compiler plugin, but you also want the editor to understand generated `KPojo` members, projection result types, subquery shapes, and database-first code generation.

| Area | What the plugin adds |
|------|----------------------|
| Project model | Loads the bundled Kronos FIR compiler plugin during IDEA analysis |
| Projection docs | Shows generated `KronosSelectResult_*` and `KronosSelectContext_*` shapes in quick documentation |
| Editor diagnostics | Surfaces projection, scalar subquery, predicate subquery, and INSERT SELECT shape errors in the editor |
| Code Generator | Reads IDEA Database data sources and previews or writes `KPojo` files |
| Templates | Lets you copy the built-in KPojo template into `.kronos/templates` and customize generated code |

## UI preview

Projection completion uses the generated result or context shape, so aliases such as `nameLength`, `rn`, and aggregate fields are available in the editor.

<img src="/assets/images/idea-plugin/kronos-idea-projection-completion.png" alt="Kronos IDEA plugin projection completion" style="width: 100%; max-width: 640px; height: auto;" />

Quick documentation on the current query context renders the generated context class, including aliases that can be used by `orderBy { ... }`.

<img src="/assets/images/idea-plugin/kronos-idea-projection-context-docs.png" alt="Kronos IDEA plugin projection context documentation" style="width: 100%; max-width: 640px; height: auto;" />

Quick documentation on a local result value renders the generated result-row class, including field names and Kotlin types.

<img src="/assets/images/idea-plugin/kronos-idea-projection-docs.png" alt="Kronos IDEA plugin projection documentation" style="width: 100%; max-width: 640px; height: auto;" />

The `Kronos-ORM` tool window includes the Code Generator tab for choosing IDEA Database data sources, selecting tables, previewing generated Kotlin, and writing KPojo files.

<img src="/assets/images/idea-plugin/kronos-idea-code-generator.png" alt="Kronos IDEA plugin code generator" style="width: 100%; max-width: 640px; height: auto;" />

## Install {{ $.title("Kronos-ORM") }}

The IDEA plugin is packaged as `kronos-idea-plugin.zip`. Install it from a downloaded GitHub Release artifact or from a locally built zip.

```text group="Install 1" name="IDEA"
Settings / Preferences -> Plugins -> Install Plugin from Disk...
Select kronos-idea-plugin.zip
Restart IntelliJ IDEA
```

Formal releases attach the IDEA plugin zip to the GitHub Release together with JVM jars and checksum files. The GitHub Release notes are generated from merged changes.

```text group="Install 2" name="release artifact"
kronos-idea-plugin-{{ $.kronosVersion() }}.zip
```

When building from source, the zip is written to the plugin distribution directory.

```bash group="Install 3" name="source build" icon="terminal"
./gradlew :kronos-idea-plugin:buildPlugin -Pkronos.idea.version=2026.2
```

```text group="Install 3" name="output"
kronos-idea-plugin/build/distributions/kronos-idea-plugin.zip
```

## Match the project setup

The IDEA plugin provides editor analysis. The Gradle or Maven compiler plugin is still required for command-line builds and CI.

```kotlin group="Project setup 1" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}
```

After installing the IDEA plugin, reload the Gradle project or reimport the Maven project, then run Kotlin compilation once.

```bash group="Project setup 2" name="shell" icon="terminal"
./gradlew compileKotlin
```

```text group="Project setup 2" name="output"
[Kronos] Kronos compiler plugin K2 initialized
BUILD SUCCESSFUL
```

Use an IntelliJ IDEA build and Kotlin plugin that support Kotlin 2.4.0. The repository build targets IntelliJ IDEA `2026.2` for release plugin artifacts.

## Configure the plugin

The installed plugin adds a `Kronos-ORM` tool window and a settings page.

| UI entry | Use |
|----------|-----|
| `Kronos-ORM` tool window | Open Kronos IDE tools and code generation templates |
| `Settings / Preferences -> Kronos ORM Setting` | Set the Kronos config JSON file path |

The settings page stores the config path in `KronosPluginSettings`. The default value is `kronos.json`.

```json group="Settings" name="kronos.json"
{
  "dataSources": [
    {
      "dataSourceName": "main",
      "dataSourceUrl": "jdbc:mysql://localhost:3306/kronos_demo",
      "dataSourceUser": "root",
      "dataSourcePassword": "your_password",
      "dataSourceDriver": "com.mysql.cj.jdbc.Driver",
      "default": true
    }
  ],
  "templates": [
    "KPojoTemplate.kts",
    "ServiceTemplate.kts"
  ]
}
```

For script-based entity generation, use {{ $.keyword("resources/codegen", ["Code Generator"]) }}.

## Generate KPojo files in IDEA

Open `Kronos-ORM` from the right tool-window bar. The `Code Generator` tab reads tables from IDEA Database Tools instead of asking you to maintain a separate connection list.

```text group="Code Generator" name="workflow"
1. Configure a database connection in IDEA Database.
2. Open Kronos-ORM -> Code Generator.
3. Select a data source and one or more tables.
4. Set the package name and output directory.
5. Select a template.
6. Click Preview to inspect generated Kotlin.
7. Click Generate to write files into the project.
```

The generator maps table metadata to Kronos fields: table names become `@Table`, primary keys become `@PrimaryKey`, indexes become `@TableIndex`, SQL types are converted to `KColumnType`, and column comments are preserved in the generated file where metadata is available.

```kotlin group="Code Generator" name="output" icon="kotlin"
package com.example.entity

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table(name = "tb_user")
data class TbUser(
    var id: Long? = null,
    var name: String? = null,
) : KPojo
```

Enable `Overwrite existing files` only when you want the generator to replace an existing file with the same relative path.

## Customize templates

The `Templates` tab lists built-in templates and project templates. Use `Copy to Project` to copy the built-in KPojo template into `.kronos/templates`, then edit it in your project.

Project templates can use these placeholders:

| Placeholder | Value |
|-------------|-------|
| {{ $.templatePlaceholder("packageName") }} | Package entered in the Code Generator tab |
| {{ $.templatePlaceholder("imports") }} | Imports required by generated annotations and types |
| {{ $.templatePlaceholder("tableComment") }} | Formatted table comment |
| {{ $.templatePlaceholder("generatedAt") }} | Generation timestamp |
| {{ $.templatePlaceholder("tableName") }} | Source table name |
| {{ $.templatePlaceholder("className") }} | Generated Kotlin class name |
| {{ $.templatePlaceholder("tableIndexes") }} | Rendered `@TableIndex` annotations |
| {{ $.templatePlaceholder("fields") }} | Rendered Kotlin properties with Kronos annotations |

Keep this marker in project KPojo templates so the IDEA plugin can identify supported templates:

```kotlin group="Template" name="marker" icon="kotlin"
// KRONOS_IDEA_TEMPLATE:KPOJO
```

## Use editor analysis

Many Kronos features come from compile-time generated information: `KPojo` metadata and dynamic accessors, the fields available inside each query lambda, temporary `KPojo` projection classes generated by `select { ... }`, and shape-based diagnostics for subqueries and insert-select.

The IntelliJ IDEA plugin makes that generated information visible to IDEA K2 analysis. That lets the editor complete, type-check, and report errors at positions such as `toDataMap()`, `it.nameLength`, `it.rn`, `rows.first().totalAmount`, and `insert<Target> { ... }`.

Quick documentation on a generated projection receiver renders the generated class shape, so you can inspect which fields are available without finding an internal generated source file.

```kotlin group="Editor Analysis" name="projection" icon="kotlin"
val rows = User()
    .select {
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .queryList()

rows.first().nameLength
```

Quick documentation for the result type is equivalent to:

```kotlin group="Editor Analysis" name="generated shape" icon="kotlin"
data class KronosSelectResult_UserNameLength(
    var id: Int? = null,
    var nameLength: Int? = null,
) : KPojo
```

If completion works in Gradle output but not in the editor, reload the project and check that the installed plugin name is `Kronos-ORM` with plugin ID `com.kotlinorm.kronos-idea-plugin`.

## Three KPojo shapes in one query

For the example below, start with three `KPojo` shapes: one table entity written by the user, and two classes generated by Kronos during compilation.

```kotlin name="three-kpojo-shapes" icon="kotlin"
// 1. User-defined table KPojo. This is the input field source for User() queries.
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
) : KPojo

// 2. Generated by Kronos: each row returned by queryList()
// A readable name is used here; application code does not reference the real name.
data class UserNameLengthRow(
    var id: Int? = null,
    var nameLength: Int? = null,
) : KPojo

// 3. Generated by Kronos: fields visible to orderBy { ... } in the same query
// A readable name is used here; application code does not reference the real name.
data class UserNameLengthOrderContext(
    var id: Int? = null,
    var name: String? = null,
    var status: Int? = null,
    var nameLength: Int? = null,
) : KPojo
```

The query that produces those shapes looks like this. The comments show what type IDEA uses for `it` in each lambda:

```kotlin name="receiver-in-simple-query" icon="kotlin"
val query = User()
    .select {
        // it: User
        // Reads table fields and generates UserNameLengthRow(id, nameLength)
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .where {
        // it: User
        // Filters read input fields, so id/name/status are available here
        it.status == 1
    }
    .orderBy {
        // it: UserNameLengthOrderContext
        // Ordering can read input fields and aliases generated by the current select
        it.nameLength.desc()
    }

val rows = query.queryList()
// rows: List<UserNameLengthRow>

rows.first().id
rows.first().nameLength
```

> **Note**
> IDEA may show generated projection names while resolving code. Treat those names as editor-only details; application code should use the DSL result values and aliases.

The `where { ... }` in the same query reads input fields. `nameLength` is a result field generated by the current `select`, so IDEA reports the following code:

```kotlin name="invalid-current-alias-in-where" icon="kotlin"
User()
    .select {
        // it: User
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .where {
        // it: User
        // User has no nameLength field
        it.nameLength > 8
    }
    .queryList()
```

## Querying a query result

When `query` is used as the input of another query, the new lambdas read `UserNameLengthRow`. At that point, `nameLength` is an input field and can be used in `where { ... }`.

```kotlin name="select-from-projection" icon="kotlin"
val query = User()
    .select {
        // it: User
        [it.id, f.length(it.name).alias("nameLength")]
    }

val filtered = query
    .select {
        // it: UserNameLengthRow
        // Kronos generates another result-row class for this select
        [it.id, it.nameLength]
    }
    .where {
        // it: UserNameLengthRow
        it.nameLength > 8
    }
    .queryList()
```

Subqueries, repeated `.select { ... }`, joins with query sources, and insert-select can all produce more result-row classes and ordering context classes. Application code does not need to reference their generated names; the IDEA plugin uses them for completion and checks.

## Window function results

Window function aliases become fields on the result-row class and on the same-query ordering context class. Start with the actual query and the receiver comments:

```kotlin name="window-query-receiver" icon="kotlin"
@Table("tb_order")
data class Order(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var userId: Int? = null,
    var amount: BigDecimal? = null,
    var status: Int? = null,
    var createTime: LocalDateTime? = null,
) : KPojo

val ranked = Order()
    .select {
        // it: Order
        [
            it.id,
            it.userId,
            it.amount,
            f.rowNumber()
                .over {
                    // Still references fields from the outer it: Order
                    partitionBy(it.userId)
                    orderBy(it.createTime.desc())
                }
                .alias("rn")
        ]
    }
    .orderBy {
        // it: RankedOrderContext
        it.rn.asc()
    }

val firstOrders = ranked
    .select {
        // it: RankedOrderRow
        [it.id, it.userId, it.amount]
    }
    .where {
        // it: RankedOrderRow
        it.rn == 1
    }
    .queryList()
```

If you write `where { it.rn == 1 }` directly on the same `ranked` query, IDEA reports `rn` because that `where` still uses `Order`.

The query above generates shapes like these:

```kotlin name="window-result-shapes" icon="kotlin"
// Generated by Kronos: rows returned by ranked.queryList()
data class RankedOrderRow(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: BigDecimal? = null,
    var rn: Long? = null,
) : KPojo

// Generated by Kronos: orderBy { ... } context in the same query
data class RankedOrderContext(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: BigDecimal? = null,
    var status: Int? = null,
    var createTime: LocalDateTime? = null,
    var rn: Long? = null,
) : KPojo
```

## Receivers in joins

A `KSelectable` can be used as the right side of a join. The right parameter is completed as the result row generated by the right query.

```kotlin name="join-selectable-source" icon="kotlin"
val orderTotals = Order()
    .select {
        // it: Order
        [it.userId, f.sum(it.amount).alias("totalAmount")]
    }
    .groupBy {
        // it: Order
        it.userId
    }

User()
    .join(orderTotals) { user, totals ->
        // user: User
        // totals: OrderTotalRow, generated from orderTotals
        on { user.id == totals.userId }
        select { [user.id, user.name, totals.totalAmount] }
    }
    .queryList()
```

The right-side result row can be understood as:

```kotlin name="join-right-result-shape" icon="kotlin"
data class OrderTotalRow(
    var userId: Int? = null,
    var totalAmount: BigDecimal? = null,
) : KPojo
```

## Receiver in insert-select

The lambda in `insert<Target> { ... }` reads the result row generated by the source query. IDEA uses that row to offer fields and to check the value count and types.

```kotlin name="insert-select-target" icon="kotlin"
data class UserOrderSummary(
    var userId: Int? = null,
    var orderCount: Long? = null,
) : KPojo
```

```kotlin name="insert-select-source" icon="kotlin"
val orderCounts = Order()
    .select {
        // it: Order
        [it.userId, f.count(it.id).alias("orderCount")]
    }
    .groupBy {
        // it: Order
        it.userId
    }

orderCounts
    .insert<UserOrderSummary> {
        // it: OrderCountRow, generated from orderCounts
        [it.userId, it.orderCount]
    }
    .execute()
```

The source result row can be understood as:

```kotlin name="insert-select-source-shape" icon="kotlin"
data class OrderCountRow(
    var userId: Int? = null,
    var orderCount: Long? = null,
) : KPojo
```

The editor reports values lists with the wrong count or type:

```kotlin name="insert-select-invalid-count" icon="kotlin"
orderCounts
    .insert<UserOrderSummary> {
        // UserOrderSummary needs both userId and orderCount
        [it.userId]
    }
```

```kotlin name="insert-select-invalid-type" icon="kotlin"
orderCounts
    .insert<UserOrderSummary> {
        // userId expects Int?, and orderCount expects Long?
        [it.orderCount, it.userId]
    }
```

## Fields generated by scalar subqueries

A scalar subquery used as a select item also becomes a property on the result-row class.

```kotlin name="scalar-select-item" icon="kotlin"
val users = User()
    .select { u ->
        // u: User
        [
            u.id,
            Order()
                .select {
                    // it: Order
                    it.amount
                }
                .where {
                    // it: Order, and it can reference outer u: User
                    it.userId == u.id
                }
                .orderBy {
                    // it: Order
                    it.createTime.desc()
                }
                .limit(1)
                .alias("lastAmount")
        ]
    }

val rows = users.queryList()
// row elements contain id and lastAmount
```

This generates a shape like:

```kotlin name="scalar-result-shape" icon="kotlin"
data class UserLastAmountRow(
    var id: Int? = null,
    var lastAmount: BigDecimal? = null,
) : KPojo
```

The following examples are reported because Kronos cannot generate one clear property:

```kotlin name="scalar-invalid-examples" icon="kotlin"
// Missing property name
User().select { u ->
    [u.id, Order().select { it.amount }.where { it.userId == u.id }.limit(1)]
}

// Missing limit(1), so it is not guaranteed to be a single value
User().select { u ->
    [
        u.id,
        Order()
            .select { it.amount }
            .where { it.userId == u.id }
            .alias("lastAmount")
    ]
}

// Multiple selected columns cannot fit into one property
User().select { u ->
    [
        u.id,
        Order()
            .select { [it.amount, it.status] }
            .where { it.userId == u.id }
            .limit(1)
            .alias("lastOrder")
    ]
}
```

## Predicate subquery diagnostics

The plugin also reports shape errors for `IN`, `EXISTS`, `ANY`, `SOME`, `ALL`, and row-value tuple predicates. This form is valid:

```kotlin name="predicate-valid" icon="kotlin"
User()
    .select()
    .where { user ->
        // user: User
        user.id in Order().select {
            // it: Order
            it.userId
        } && exists(
            Order().select().where {
                // it: Order, and it can reference outer user: User
                it.userId == user.id
            }
        )
    }
```

The following forms are reported in the editor:

```kotlin name="predicate-invalid-examples" icon="kotlin"
// Single-value IN requires a single-column subquery
User().select().where {
    it.id in Order().select { [it.userId, it.status] }
}

// Row-value tuple arity must match the subquery arity
User().select().where {
    [it.id, it.status] in Order().select { it.userId }
}

// Single-element tuples are rejected; use it.id in query
User().select().where {
    [it.id] in Order().select { it.userId }
}

// any/some/all also require a single-column right-hand query
Order().select().where {
    it.status > any(Order().select { [it.status, it.amount] })
}
```

## Why aliases are required

Direct fields already have names:

```kotlin name="direct-field-name" icon="kotlin"
User().select { [it.id, it.name] }
```

This can generate:

```kotlin name="direct-field-result-shape" icon="kotlin"
data class UserIdNameRow(
    var id: Int? = null,
    var name: String? = null,
) : KPojo
```

Functions and computed expressions do not have a natural property name:

```kotlin name="missing-alias" icon="kotlin"
User().select { [it.id, f.length(it.name)] }
```

Kronos cannot decide whether the second property should be called `length`, `nameLength`, or something else, so IDEA asks you to write an alias:

```kotlin name="valid-alias" icon="kotlin"
User().select { [it.id, f.length(it.name).alias("nameLength")] }
```

One result class cannot contain duplicate property names:

```kotlin name="duplicate-projection-field" icon="kotlin"
User().select { [it.id, it.id] }
```

A new alias must avoid original input field names:

```kotlin name="source-conflict" icon="kotlin"
User().select { [it.id, f.length(it.name).alias("name")] }
```

For `orderBy { ... }` to receive an unambiguous ordering context class, aliases need to avoid original input field names. If `User.name` and the selected alias `name` both exist, `KronosSelectContext_*` would contain two `name` properties, and neither the editor nor the compiler could know which one you meant.

## Receiver quick reference

This table only summarizes the examples above:

| Syntax | `it` / parameter inside the lambda |
|--------|------------------------------------|
| `User().select { ... }` | `User` |
| `User().where { ... }` | `User` |
| `User().groupBy { ... }` | `User` |
| `User().having { ... }` | `User` |
| `User().select { ... }.orderBy { ... }` | Ordering context class composed from input fields and current select aliases |
| `query.select { ... }` | Result-row class generated by the previous select of `query` |
| `query.where { ... }` | Current input result-row class of `query` |
| `User().join(query) { left, right -> ... }` | `left` is `User`; `right` is the result-row class of `query` |
| `query.insert<Target> { ... }` | Result-row class of `query` |
| `where { ... }` inside a scalar subquery | The subquery table entity; outer lambda parameters can also be referenced |

## Supported diagnostics

The IDEA plugin surfaces these Kronos DSL rules directly in the editor:

| Scenario | Why it is invalid |
|----------|-------------------|
| Non-field select item without `.alias("name")` | The result class property has no name |
| Duplicate selected field names in one query | The result class would contain duplicate properties |
| Alias conflicts with an original input field | The ordering context class would contain duplicate properties |
| `where` / `having` in the same query accesses a current select alias | Filters read input fields |
| Scalar subquery selects multiple columns | One property can hold only one value |
| Scalar subquery misses `.limit(1)` | It is not guaranteed to return one value |
| Right side of `field in query` returns multiple columns | Single-value predicate arity does not match |
| Row-value tuple arity does not match query arity | Tuple shape does not match |
| `[a] in query` | Single-column membership should be written as `a in query` |
| Insert-select values have the wrong count or type | Input query result fields cannot be mapped to target insertable fields |

## Troubleshoot editor behavior

Use these checks when Gradle or Maven succeeds but IDEA does not show Kronos completions.

| Symptom | Check | Fix |
|---------|-------|-----|
| `toDataMap()` or `__tableName` behaves like the default `KPojo` body | The source set was not compiled with Kronos support | Enable {{ $.keyword("configuration/compiler-plugins", ["Compiler Plugins"]) }} and reimport the project |
| Projection aliases such as `nameLength` are missing | The IDEA plugin was installed before the project import completed | Reload Gradle/Maven and reopen the Kotlin file |
| Diagnostics are visible in build output but not in the editor | IDEA is using an incompatible Kotlin plugin or stale project model | Update IDEA/Kotlin plugin to a Kotlin 2.4.0-compatible build and invalidate caches if needed |
| Codegen UI cannot find data source settings | The config path does not point to the intended JSON file | Set `Config File` on `Kronos ORM Setting` |

For more query, subquery, and INSERT SELECT rules, see {{ $.keyword("query/subqueries", ["Subqueries"]) }}. For setup problems, see {{ $.keyword("resources/troubleshooting", ["Troubleshooting"]) }}.
