{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Set the Kotlin project baseline

Kronos compile-time support is loaded by the Kotlin build. Use JDK 8+, Kotlin 2.4.0+, Maven 3.9+, or a Gradle version supported by Kotlin 2.4.0.

```kotlin group="Baseline 1" name="gradle(kts)" icon="gradlekts"
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.0"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}
```

```xml group="Baseline 1" name="maven" icon="maven"
<properties>
    <kotlin.version>2.4.0</kotlin.version>
    <maven.compiler.release>8</maven.compiler.release>
</properties>
```

The baseline build should compile before the Kronos data source and ORM code are added.

```text group="Baseline 2" name="output"
> Task :compileKotlin
BUILD SUCCESSFUL
```

## Enable {{ $.title("kronos-gradle-plugin") }}

Add the Kronos Gradle plugin and `kronos-core` to a JVM Kotlin project. The Gradle plugin wires Kronos compile-time support into `compileKotlin`.

```kotlin group="Gradle 1" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}
```

Run the Kotlin compilation task and check the plugin loading lines.

```bash group="Gradle 2" name="shell" icon="terminal"
./gradlew compileKotlin
```

```text group="Gradle 2" name="output"
Loaded Gradle plugin com.kotlinorm.compiler.plugin.KronosGradlePlugin version {{ $.kronosVersion() }}
Loaded Compiler plugin com.kotlinorm.kronos-compiler-plugin version {{ $.kronosVersion() }}
BUILD SUCCESSFUL
```

## Enable {{ $.title("kronos-maven-plugin") }}

In Maven, add `kronos-core` as an application dependency and register `kronos-maven-plugin` inside `kotlin-maven-plugin`.

```xml group="Maven 1" name="pom.xml" icon="maven"
<project>
    <properties>
        <kotlin.version>2.4.0</kotlin.version>
        <kronos.version>{{ $.kronosVersion() }}</kronos.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>${kronos.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <extensions>true</extensions>
                <configuration>
                    <compilerPlugins>
                        <plugin>kronos-maven-plugin</plugin>
                    </compilerPlugins>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>com.kotlinorm</groupId>
                        <artifactId>kronos-maven-plugin</artifactId>
                        <version>${kronos.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

Compile with Maven and check the Kronos plugin line in the build output.

```bash group="Maven 2" name="shell" icon="terminal"
mvn compile
```

```text group="Maven 2" name="output"
Loaded Maven plugin com.kotlinorm.compiler.plugin.KronosMavenPlugin
[INFO] BUILD SUCCESS
```

## Compile each Kronos source set

Apply the Kronos build plugin to every JVM source set that declares `KPojo` classes or uses Kronos DSL calls. A module that only consumes compiled entities can depend on that module normally, but a module that declares entities or query DSL must compile with the Kronos plugin active.

```kotlin group="Source set 1" name="module layout" icon="kotlin"
// :domain declares KPojo classes and must apply the Kronos Gradle or Maven plugin.
@Table("tb_user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null
) : KPojo

// :service writes DSL over User and must also compile with Kronos support.
val names = User()
    .select { [it.id, it.name] }
    .where { it.name like "A%" }
    .toList()
```

The fastest check is to compile the module that owns the source files.

```bash group="Source set 2" name="shell" icon="terminal"
./gradlew :service:compileKotlin
```

```text group="Source set 2" name="output"
[Kronos] Kronos compiler plugin K2 initialized
BUILD SUCCESSFUL
```

## Check generated {{ $.code("KPojo") }} members

After the build plugin is active, a `KPojo` can expose generated metadata and map conversion methods during runtime checks.

```kotlin group="KPojo Check 1" name="User.kt" icon="kotlin"
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null
) : KPojo

fun main() {
    val user = User(7, "Ada")

    println(user.__tableName)
    println(user.toDataMap())
    println(user.__columns.map { it.name })
}
```

The output should show table metadata and the property values collected by Kronos.

```text group="KPojo Check 2" name="output"
tb_user
{id=7, name=Ada}
[id, name]
```

> **Note**
> A message such as `__tableName must be overridden by the compiler plugin` means this source set was compiled without Kronos compile-time support.

## Check the DSL SQL output

Use a small `select` query to confirm that Kotlin property access and condition expressions are converted into Kronos SQL.

```kotlin group="DSL Check 1" name="kotlin" icon="kotlin"
import com.kotlinorm.orm.select.select

val (sql, params) = User()
    .select { [it.id, it.name] }
    .where { it.id == 7 }
    .build()

println(sql)
println(params)
```

For a MySQL data source, the check prints the selected columns and named parameter.

```text group="DSL Check 2" name="output"
SELECT `id`, `name` FROM `tb_user` WHERE `tb_user`.`id` = :id
{id=7}
```

For condition operators and query-by-example behavior, see {{ $.keyword("query/conditions", ["Conditions for Where, Having and On"]) }}.

## Use DSL rules from {{ $.title("kronos-syntax") }}

Kronos query, mutation, and DDL APIs all produce an inspectable SQL task before execution. Use `build()` when you want to review the SQL and parameter map without touching the database.

```kotlin group="Syntax 1" name="projection and condition" icon="kotlin"
val task = User()
    .select {
        [
            it.id,
            it.name.alias("username"),
            f.length(it.name).alias("nameLength")
        ]
    }
    .where { (it.id in listOf(1, 2, 3)) && it.name like "A%" }
    .build()

println(task.sql)
println(task.paramMap)
```

```text group="Syntax 1" name="output"
SELECT `id`, `name` AS `username`, LENGTH(`name`) AS `nameLength`
FROM `tb_user`
WHERE `tb_user`.`id` IN (:idList) AND `tb_user`.`name` LIKE :name
{idList=[1, 2, 3], name=A%}
```

The same rules apply in `where`, `having`, and `on`: Kotlin comparisons become SQL predicates, `&&` and `||` keep the boolean grouping, `in` accepts collections or selectable subqueries, and non-field select items need `.alias("name")`.

```kotlin group="Syntax 2" name="derived projection" icon="kotlin"
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val rows = nameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .toList()
```

Projection, subquery, and generated result-shape examples are covered in {{ $.keyword("query/projection", ["Projection"]) }} and {{ $.keyword("query/subqueries", ["Subqueries"]) }}.

## Read Kronos diagnostics

Kronos diagnostics appear in the IDE and in build output. The `KRONOS_` code identifies the DSL rule, and the message describes the fix.

```kotlin group="Diagnostics 1" name="invalid select" icon="kotlin"
import com.kotlinorm.orm.select.select

User()
    .select { [it.id, f.length(it.name)] }
```

This select item is a function expression, so the diagnostic asks for an explicit alias.

```text group="Diagnostics 2" name="output"
User.kt:6:23: error: Non-field select item must declare .alias("name")
KRONOS_SELECT_ITEM_REQUIRES_ALIAS
```

Add an alias and compile again.

```kotlin group="Diagnostics 3" name="fixed select" icon="kotlin"
User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
```

Compiler plugin errors emitted by Kronos also start with `[Kronos]` and may include a `Fix:` line.

```text group="Diagnostics 4" name="kronos output"
User.kt:12:13: error: [Kronos] Unsupported field
  Fix: Use a property reference
```

## Fix common diagnostics

Use the diagnostic code to find the DSL shape that needs to change.

| Diagnostic | What to check | Fix |
|------------|---------------|-----|
| `__tableName must be overridden by the compiler plugin` | The source set was compiled without Kronos support | Enable the Gradle or Maven plugin for that module and recompile |
| `KRONOS_GENERIC_KPOJO_NOT_SUPPORTED` | A KPojo class declares class-level type parameters | Remove the KPojo type parameters and use concrete property types |
| `KRONOS_SELECT_ITEM_REQUIRES_ALIAS` | Function, aggregate, scalar subquery, window function, or raw SQL in `select { ... }` | Add `.alias("resultName")` |
| `KRONOS_DUPLICATE_PROJECTION_FIELD` | Two selected items produce the same result property | Remove one field or give one item a different alias |
| `OPT_IN_USAGE_ERROR` | A selected alias replaces a still-present source field with the same logical name | Rename the alias, remove the source field first, or explicitly acknowledge it with `@OptIn(UnsafeProjectionOverride::class)` |
| `KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT` | A scalar subquery can return more than one row | Add `.limit(1)` before using it as a value |
| `KRONOS_SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN` | A scalar subquery selects multiple fields | Select one field or split the expression |
| `KRONOS_PREDICATE_SUBQUERY_COLUMN_COUNT_MISMATCH` | `field in query`, `ANY`, `SOME`, `ALL`, or row-value `IN` has mismatched arity | Make the left expression and right query return the same column count |
| `KRONOS_ROW_VALUE_TUPLE_REQUIRES_MULTIPLE_FIELDS` | A single-field tuple such as `[it.id] in query` was used | Write `it.id in query` |
| `KRONOS_INSERT_SELECT_VALUE_COUNT_MISMATCH` | `insert<Target> { ... }` value count differs from target fields | Select the same number of values as insertable target columns |
| `KRONOS_INSERT_SELECT_VALUE_TYPE_MISMATCH` | Insert-select values do not match target field types | Reorder values or cast the scalar expression to the expected Kotlin type |

An explicit projection alias normally must not replace a source field with the same logical name. If the replacement is intentional, opt in with the standard Kotlin mechanism. The marker is `com.kotlinorm.annotations.UnsafeProjectionOverride` and has error severity; its message warns that the selected value and Kotlin type may change.

```kotlin
import com.kotlinorm.annotations.UnsafeProjectionOverride

@OptIn(UnsafeProjectionOverride::class)
val query = User().select { [it.id, f.length(it.name).alias("name")] }
```

Removing a field first makes the replacement intentional without opt-in:

```kotlin
val query = User().select { [it - it.name, f.length(it.name).alias("name")] }
```

For build setup, start from {{ $.keyword("getting-started/quick-start", ["Quick Start"]) }}. For database execution and SQL logging, see {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }} and {{ $.keyword("configuration/logging", ["Logging"]) }}. For IDE inspection behavior, see {{ $.keyword("resources/idea-plugin", ["IntelliJ IDEA Plugin"]) }}.
