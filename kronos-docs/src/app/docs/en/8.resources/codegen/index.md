{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Code Generator

Kronos code generator reads database metadata and writes Kotlin `KPojo` entity classes. Use it when the database schema is the source of truth and the project needs entity classes that match existing tables.

The generator uses a TOML config file and a Kotlin Script template. The config selects tables, data source, naming strategy, and output directory. The template controls the generated Kotlin file content.

> For the Database First workflow, see **[Database First](/documentation/en/resources/database-first)**.
> For setup and runtime troubleshooting, see {{ $.keyword("resources/troubleshooting", ["Troubleshooting"]) }}.

## {{ $.title("Dependencies")}}Add script dependencies

Add `kronos-codegen`, `kronos-core`, and `kronos-jdbc-wrapper` to the `.main.kts` script. Add the JDBC driver and connection pool that match your database and JDK.

```kotlin group="Script" name="example.main.kts" icon="kotlin"
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:{{ $.kronosVersion() }}")
@file:DependsOn("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
@file:DependsOn("org.apache.commons:commons-dbcp2:<latest-stable>")
@file:DependsOn("com.mysql:mysql-connector-j:<latest-stable>")
```

Replace `<latest-stable>` with the current stable version from the connection pool or JDBC driver project.

## {{ $.title("Configuration")}}Select tables

Create `config.toml`. Each `[[table]]` item becomes one Kotlin file.

```toml group="Config 1" name="config.toml"
[[table]]
name = "tb_user"
className = "User"

[[table]]
name = "tb_order"
className = "Order"
```

With the output config below, the generator writes one file per table.

```text group="Config 2" name="generated files"
src/main/kotlin/com/example/entity/User.kt
src/main/kotlin/com/example/entity/Order.kt
```

Set `className` when the Kotlin class name should be controlled explicitly.

```toml group="Config 3" name="class name"
[[table]]
name = "sys_account"
className = "Account"
```

## Configure output

`targetDir` selects the generated file directory. `packageName` selects the package declaration written by the template.

```toml group="Output 1" name="config.toml"
[output]
targetDir = "src/main/kotlin/com/example/entity"
packageName = "com.example.entity"
tableCommentLineWords = 80
```

The template can read these values directly.

```kotlin group="Output 2" name="template" icon="kotlin"
template {
    +"package $packageName"
    +""
    +formatedComment
}
```

The generated file starts with the configured package and the formatted table comment.

```kotlin group="Output 3" name="User.kt" icon="kotlin"
package com.example.entity

// User account table
```

## Configure a data source

`dataSource` creates the JDBC connection used to read table comments, columns, and indexes. This example uses Apache DBCP2 and the built-in `KronosJdbcWrapper`.

```toml group="DataSource 1" name="config.toml"
[dataSource]
wrapperClassName = "com.kotlinorm.wrappers.KronosJdbcWrapper"
dataSourceClassName = "org.apache.commons.dbcp2.BasicDataSource"
url = "jdbc:mysql://localhost:3306/kronos_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false"
username = "root"
password = "your_password"
driverClassName = "com.mysql.cj.jdbc.Driver"
initialSize = 5
maxTotal = 10
```

Use the property names supported by the selected DataSource class.

```toml group="DataSource 2" name="Druid"
[dataSource]
wrapperClassName = "com.kotlinorm.wrappers.KronosJdbcWrapper"
dataSourceClassName = "com.alibaba.druid.pool.DruidDataSource"
url = "jdbc:mysql://localhost:3306/kronos_demo"
username = "root"
password = "your_password"
driverClassName = "com.mysql.cj.jdbc.Driver"
initialSize = 5
maxActive = 10
```

Add the matching pool dependency to the script when you switch the pool implementation.

```kotlin group="DataSource 3" name="Druid dependency" icon="kotlin"
@file:DependsOn("com.alibaba:druid:<latest-stable>")
```

## Configure naming

`lineHumpNamingStrategy` maps underscore names to camel-case Kotlin property names.

```toml group="Strategy 1" name="config.toml"
[strategy]
tableNamingStrategy = "lineHumpNamingStrategy"
fieldNamingStrategy = "lineHumpNamingStrategy"
```

With this strategy, a database column named `create_time` is generated as `createTime`.

```kotlin group="Strategy 2" name="User.kt" icon="kotlin"
var createTime: java.time.LocalDateTime? = null
```

Use `noneNamingStrategy` when the generated name should keep the database metadata name.

```toml group="Strategy 3" name="keep names"
[strategy]
tableNamingStrategy = "noneNamingStrategy"
fieldNamingStrategy = "noneNamingStrategy"
```

```kotlin group="Strategy 3" name="User.kt" icon="kotlin"
var create_time: java.time.LocalDateTime? = null
```

## Generate special field annotations

Add strategy fields for primary key, create time, update time, logic delete, and optimistic lock columns.

```toml group="Annotations 1" name="config.toml"
[strategy]
primaryKeyStrategy = "id"
createTimeStrategy = "create_time"
updateTimeStrategy = "update_time"
logicDeleteStrategy = "deleted"
optimisticLockStrategy = "version"
```

When the database table contains matching columns, `field.annotations()` writes the Kronos annotations. The logical-delete column must define `DEFAULT 0` (`DEFAULT false` on PostgreSQL) for Codegen to emit the `@Default` annotation shown below.

```kotlin group="Annotations 2" name="User.kt" icon="kotlin"
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.annotations.Version

data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @CreateTime
    var createTime: java.time.LocalDateTime? = null,
    @UpdateTime
    var updateTime: java.time.LocalDateTime? = null,
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = null,
    @Version
    var version: Int? = null
) : KPojo
```

For annotation behavior in ORM operations, see {{ $.keyword("mapping/annotations", ["Annotation Config"]) }}.

## Generate column metadata annotations

The template can turn database metadata into field annotations. A non-null database column is generated with `@NonNull`.

```kotlin group="Column 1" name="template" icon="kotlin"
fields.forEach { field ->
    field.annotations().forEach { annotation ->
        +"""${indent(4)}$annotation"""
    }
    +"""${indent(4)}var ${field.name}: ${field.kotlinType}? = null,"""
}
```

For a required `name` column, the generated property includes `@NonNull`.

```kotlin group="Column 2" name="User.kt" icon="kotlin"
@NonNull
var name: String? = null
```

A decimal column with precision metadata is generated with `@ColumnType`.

```kotlin group="Column 3" name="User.kt" icon="kotlin"
@ColumnType(type = KColumnType.DECIMAL, length = 10, scale = 2)
var balance: java.math.BigDecimal? = null
```

## Generate table indexes

Use `indexes.toAnnotations()` to write index annotations collected from the database metadata.

```kotlin group="Index 1" name="template" icon="kotlin"
+indexes.toAnnotations()
+"""@Table("$tableName")"""
```

The generated entity includes `@TableIndex` before the class declaration.

```kotlin group="Index 2" name="User.kt" icon="kotlin"
@TableIndex(name = "idx_user_email", columns = ["email"], type = "UNIQUE")
@Table("tb_user")
data class User(
    var email: String? = null
) : KPojo
```

## Create the template

Call `init("config.toml")` before `template { ... }`. The template block runs once for each configured table.

```kotlin group="Template 1" name="example.main.kts" icon="kotlin"
import com.kotlinorm.codegen.KronosConfig.Companion.write
import com.kotlinorm.codegen.TemplateConfig.Companion.template
import com.kotlinorm.codegen.init
import com.kotlinorm.codegen.kotlinType

init("config.toml")

template {
    +"package $packageName"
    +""
    +imports.joinToString("\n") { "import $it" }
    +""
    +formatedComment
    +indexes.toAnnotations()
    +"""@Table("$tableName")"""
    +"""data class $className("""
    +fields.mapIndexed { index, field ->
        val annotations = field.annotations().joinToString("\n") { "${indent(4)}$it" }
        val annotationBlock = if (annotations.isEmpty()) "" else "$annotations\n"
        val comma = if (index == fields.lastIndex) "" else ","
        """$annotationBlock${indent(4)}var ${field.name}: ${field.kotlinType}? = null$comma"""
    }.joinToString("\n")
    +") : KPojo"
}.write()
```

The generated file follows the structure assembled by the template.

```kotlin group="Template 2" name="User.kt" icon="kotlin"
package com.example.entity

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
data class User(
    var id: Int? = null,
    var name: String? = null
) : KPojo
```

## Use config inheritance

Put shared output and data source settings in a base TOML file.

```toml group="Extend 1" name="base.toml"
[output]
targetDir = "src/main/kotlin/com/example/entity"
packageName = "com.example.entity"

[dataSource]
wrapperClassName = "com.kotlinorm.wrappers.KronosJdbcWrapper"
dataSourceClassName = "org.apache.commons.dbcp2.BasicDataSource"
url = "jdbc:mysql://localhost:3306/kronos_demo"
username = "root"
password = "your_password"
driverClassName = "com.mysql.cj.jdbc.Driver"
```

The project config can extend the base file and add its own table list.

```toml group="Extend 2" name="config.toml"
extend = "base.toml"

[[table]]
name = "tb_user"
className = "User"
```

Run the script with the project config.

```kotlin group="Extend 3" name="example.main.kts" icon="kotlin"
init("config.toml")
```

## Run the generator

Kotlin script files should use the `xxx.main.kts` name format.

```bash group="Run 1" name="shell" icon="terminal"
kotlinc -script example.main.kts
```

After a successful run, the generator logs each written file.

```text group="Run 2" name="output"
File generated successfully: src/main/kotlin/com/example/entity/User.kt
```

If the config misses a required section, the script exits with a clear message.

```text group="Run 3" name="missing table"
IllegalArgumentException: Table configuration is required in config: config.toml
```
