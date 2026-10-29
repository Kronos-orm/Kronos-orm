{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Use **Kotlin External FIR Support (KEFS)** by [Mr3zee](https://github.com/Mr3zee) to load Kronos compiler support in IntelliJ IDEA. Kotlin's editor analysis then provides generated projection members, property completion, inferred types and Kronos DSL diagnostics.

## Configure a Gradle project

Apply the Gradle plugin and add the runtime dependency in each module that uses Kronos:

```kotlin name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.21"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

repositories { mavenCentral() }

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcollection-literals")
    }
}
```

Reload the Gradle project so IDEA imports the compiler plugin and collection-literal option. KEFS provides editor integration for Gradle projects. Kotlin Toolchain projects use their corresponding build-model import plugin.

## Configure a Kotlin Toolchain project

Configure a JVM application with {{ $.keyword("getting-started/installation", ["Installation"]) }}, or use the complete multi-module example in {{ $.keyword("getting-started/kmp", ["Kotlin Multiplatform"]) }}. Load `kronos-compiler-plugin` in `module.yaml`, prepare the type registrations and compile the JVM target to confirm the configuration:

```bash name="Kotlin Toolchain" icon="terminal"
./kotlin build -p jvm
```

Install the official Toolchain IDE plugin, open the project directory and wait for `project.yaml` import to finish. Then configure the Kronos artifact group in KEFS as described below.

## Version compatibility

Choose a Kronos ORM, KEFS and IntelliJ IDEA combination from this table. `IDE Kotlin` identifies IDEA's embedded compiler, which KEFS uses to select the Kronos editor artifact. The project Kotlin version is managed by the build configuration.

{{ $.kefsCompatibilityTable() }}

Run `KEFS: Copy Kotlin IDE Version` through IDEA's **Find Action** to inspect the embedded compiler. Kronos editor artifacts use the version form `<IDE Kotlin>-<Kronos ORM>`, for example `2.4.20-ij262-52-{{ $.kronosVersion() }}`. Check the complete combination when upgrading, then reload the project.

## Install KEFS

Choose a combination from the compatibility table and install the corresponding version of [Kotlin External FIR Support](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support) from Marketplace:

```text name="IDEA settings"
Settings / Preferences -> Plugins -> Marketplace
Search: Kotlin External FIR Support
Install -> Restart IDE
```

For a Kotlin Toolchain project, also install the [Kotlin Toolchain plugin](https://kotlin-toolchain.org/latest/getting-started/ide-setup/) and open the directory containing `project.yaml`.

## Configure Kronos in KEFS

Open `Settings / Preferences -> Tools -> Kotlin External FIR Support -> Artifacts`.

1. In **Maven Repositories**, select Maven Central or add the URL `https://repo.maven.apache.org/maven2`.
2. In **Kotlin Compiler Plugins**, add a bundle named `Kronos`.
3. Enter `com.kotlinorm:kronos-compiler-plugin` under **Coordinates**, without a version.
4. Select **Exact** version matching, link Maven Central, and enable the bundle for the project.
5. Apply the settings and reload the project using its build tool.

KEFS saves the project configuration in `.idea/kotlin-plugins.xml`. Commit this file to share the configuration with other contributors.

Kronos's compiler dependency is configured by the project build; KEFS selects its matching editor artifact. See {{ $.keyword("configuration/compiler-plugins", ["Compiler Plugins"]) }} for build setup.

## Confirm the compiler plugin loaded

Open a Kotlin file containing a Kronos query, then open the **KEFS Diagnostics** tool window. Kronos should show **Successfully Loaded**. The details display the loaded JAR and its version.

Match the versions displayed in the tool window to the table’s `IDE Kotlin` and Kronos ORM entries, then check the loaded artifact coordinate.

Use **Find Action** (`⌘⇧A` on macOS) to run:

- `KEFS: Update Plugins`: check for artifact updates.
- `KEFS: Refresh Plugins`: reload compiler plugins.
- `KEFS: Copy Kotlin IDE Version`: inspect the exact embedded compiler version.

For local development, add a **Local (File path)** Maven repository pointing to the compatible compiler artifact repository and associate it with the Kronos bundle. `Plugin is Not Requested` means analysis has not requested the plugin yet. For `Not Found`, check the selected repositories, library version and matching compatibility artifact.

## Develop Kronos from source

The Kronos repository includes a shared [`.idea/kotlin-plugins.xml`](https://github.com/Kronos-orm/Kronos-orm/blob/main/.idea/kotlin-plugins.xml) with the Kronos bundle and its bootstrap, Maven Central and Sonatype snapshot repositories.

Install the official Kotlin Toolchain IDE plugin and KEFS, then prepare the source-built compiler:

```bash name="repository bootstrap" icon="terminal"
scripts/run build bootstrap
```

The Toolchain IDE plugin imports the project model, while KEFS loads compiler extensions for editor analysis. Bootstrap prepares the ordinary compiler coordinate in Maven Local for project import and the KEFS artifact in `build/repository/bootstrap`. Open the repository directory containing `project.yaml` and reload the project. The shared KEFS configuration refers to the private repository through `$PROJECT_DIR$`.

## Enable collection-literal syntax

Enable `-Xcollection-literals` in the project build so both compilation and the imported IDE model recognize the `[]` DSL.

```kotlin name="build.gradle.kts" icon="gradlekts"
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcollection-literals")
    }
}
```

```yaml name="module.yaml"
settings:
  kotlin:
    version: 2.4.21
    freeCompilerArgs: [ -Xcollection-literals ]
```

Reload the project after changing compiler arguments. The KEFS **General** tab shows the editor compiler version; compare it with `IDE Kotlin` in the compatibility table.

## Use editor analysis

Write a projection and access its result fields using ordinary Kotlin completion:

```kotlin name="projection" icon="kotlin"
val rows = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .toList()

val length: Int? = rows.first().nameLength
```

`nameLength` is inferred as `Int?`. The same inferred types drive property completion and type errors in subsequent query lambdas.

For database entity generation, see {{ $.keyword("resources/codegen", ["Code Generator"]) }}.

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

// 2. Generated by Kronos: each row returned by toList()
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

val rows = query.toList()
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
    .toList()
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
    .toList()
```

Subqueries, repeated `.select { ... }`, joins with query sources, and insert-select can all produce more result-row classes and ordering context classes. Application code does not need to reference their generated names; Kotlin editor analysis uses them for completion and checks.

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
    .toList()
```

If you write `where { it.rn == 1 }` directly on the same `ranked` query, IDEA reports `rn` because that `where` still uses `Order`.

The query above generates shapes like these:

```kotlin name="window-result-shapes" icon="kotlin"
// Generated by Kronos: rows returned by ranked.toList()
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
        leftJoin { user.id == totals.userId }
            .select { [user.id, user.name, totals.totalAmount] }
    }
    .toList()
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

val rows = users.toList()
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

Duplicate requested output names require the standard Kotlin `UnsafeProjectionOverride` opt-in. Without opt-in, IDEA reports the conflicting projection item:

```kotlin name="duplicate-projection-field" icon="kotlin"
User().select { [it.id, it.id] }
```

After opt-in, Kronos preserves every value. The first occurrence keeps its name, later occurrences receive `_1`, `_2`, and so on, and explicit requested names are reserved before suffix allocation.

```kotlin name="opted-in-duplicate-projection" icon="kotlin"
import com.kotlinorm.annotations.UnsafeProjectionOverride

@OptIn(UnsafeProjectionOverride::class)
val rows = User()
    .select { [it.id, it.id, it.name.alias("id_1")] }
    .toList()

rows.first().id
rows.first().id_2
rows.first().id_1
```

The requested names `id`, `id`, `id_1` resolve to `id`, `id_2`, `id_1`. IDEA uses these resolved names for completion and type inference. Prefer explicit aliases when the values have different meanings.

An alias may reuse an input field name. The alias itself, and Source clauses such as `where`, `groupBy`, and `having`, do not require opt-in:

```kotlin name="source-conflict" icon="kotlin"
User()
    .select { [it.id, f.length(it.name).alias("name")] }
    .where { it.name != null }
```

`orderBy` reads the post-select Context. If it actually reads the shadowed `name`, IDEA requires `UnsafeProjectionOverride`; after opt-in, the Context name resolves to the selected value and selected Kotlin type.

```kotlin name="opted-in-context-shadow" icon="kotlin"
@OptIn(UnsafeProjectionOverride::class)
val ordered = User()
    .select { [it.id, f.length(it.name).alias("name")] }
    .orderBy { it.name.desc() }
```

When replacement is intentional, remove the Source field before restoring the name. `select { [it - it.name, f.length(it.name).alias("name")] }` does not require opt-in. See {{ $.keyword("query/projection", ["Projection"]) }} for the complete allocation and Context rules.

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

Kronos compiler diagnostics are displayed by Kotlin editor analysis through KEFS:

| Scenario | Why it is invalid |
|----------|-------------------|
| Non-field select item without `.alias("name")` | The result class property has no name |
| Duplicate requested output names without `UnsafeProjectionOverride` | All values are preserved only after explicit opt-in and deterministic suffix allocation |
| `orderBy` reads a Selected value that shadows a Source name without opt-in | The post-select Context resolves that name to the Selected value and type after opt-in |
| `where` / `having` in the same query accesses a current select alias | Filters read input fields |
| Scalar subquery selects multiple columns | One property can hold only one value |
| Scalar subquery misses `.limit(1)` | It is not guaranteed to return one value |
| Right side of `field in query` returns multiple columns | Single-value predicate arity does not match |
| Row-value tuple arity does not match query arity | Tuple shape does not match |
| `[a] in query` | Single-column membership should be written as `a in query` |
| Insert-select values have the wrong count or type | Input query result fields cannot be mapped to target insertable fields |

## Troubleshoot editor behavior

Use these checks when the project builds but IDEA does not show Kronos completions.

| Symptom | Check | Fix |
|---------|-------|-----|
| `toDataMap()` or `__tableName` behaves like the default `KPojo` body | The source set was not compiled with Kronos support | Enable {{ $.keyword("configuration/compiler-plugins", ["Compiler Plugins"]) }} and reimport the project |
| Projection aliases such as `nameLength` are missing | Check the Kronos bundle in KEFS Artifacts | Enable the bundle, select Maven Central, and reload the project |
| Build diagnostics are missing in the editor | Check the Kronos artifact status in the KEFS tool window | Update KEFS artifacts and reload the project |
| `[]` has a syntax error | Check the imported compiler arguments | Enable `-Xcollection-literals` in the build and reload the project |

For more query, subquery, and INSERT SELECT rules, see {{ $.keyword("query/subqueries", ["Subqueries"]) }}. For setup problems, see {{ $.keyword("resources/troubleshooting", ["Troubleshooting"]) }}.
