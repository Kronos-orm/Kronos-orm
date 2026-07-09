# Module kronos-codegen

`kronos-codegen` reads database metadata and generates Kotlin `KPojo` entity classes. Use it for Database First projects where existing tables define the entity model.

The generator uses a TOML config file and a Kotlin Script template. The config selects tables, data source, naming strategy, and output directory. The template controls the generated Kotlin file content.

## Script dependencies

Create a script named `example.main.kts` and add the Kronos artifacts, JDBC driver, and connection pool.

```kotlin
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.1.2")
@file:DependsOn("com.kotlinorm:kronos-core:0.1.2")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:0.1.2")
@file:DependsOn("org.apache.commons:commons-dbcp2:<latest-stable>")
@file:DependsOn("com.mysql:mysql-connector-j:<latest-stable>")
```

Replace `<latest-stable>` with the current stable release from the connection pool or JDBC driver project.

## Select tables

Create `config.toml`. Each `[[table]]` item becomes one Kotlin file.

```toml
[[table]]
name = "tb_user"
className = "User"

[[table]]
name = "tb_order"
className = "Order"
```

With the output config below, the generator writes these files.

```text
src/main/kotlin/com/example/entity/User.kt
src/main/kotlin/com/example/entity/Order.kt
```

## Configure output

Set `targetDir` and `packageName`.

```toml
[output]
targetDir = "src/main/kotlin/com/example/entity"
packageName = "com.example.entity"
tableCommentLineWords = 80
```

The template can use the configured package directly.

```kotlin
template {
    +"package $packageName"
    +""
    +formatedComment
}
```

## Configure a data source

This example uses Apache DBCP2 and the built-in `KronosJdbcWrapper`.

```toml
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

```toml
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

Add the matching pool dependency to the script when switching pools.

```kotlin
@file:DependsOn("com.alibaba:druid:<latest-stable>")
```

`wrapperClassName` is created from the configured `DataSource`. Use a custom wrapper class with a `DataSource` constructor when metadata reading needs a forced `DBType` or custom `KronosJdbcConfig`.

## Configure naming and special fields

`lineHumpNamingStrategy` maps underscore database names to camel-case Kotlin names.

```toml
[strategy]
tableNamingStrategy = "lineHumpNamingStrategy"
fieldNamingStrategy = "lineHumpNamingStrategy"
primaryKeyStrategy = "id"
createTimeStrategy = "create_time"
updateTimeStrategy = "update_time"
logicDeleteStrategy = "deleted"
optimisticLockStrategy = "version"
```

A table with matching columns generates Kronos annotations.

```kotlin
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @CreateTime
    var createTime: java.time.LocalDateTime? = null,
    @UpdateTime
    var updateTime: java.time.LocalDateTime? = null,
    @LogicDelete
    var deleted: Boolean? = null,
    @Version
    var version: Int? = null
) : KPojo
```

## Create the template

Call `init("config.toml")` before `template { ... }`. The template block runs once for each configured table.

```kotlin
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

```kotlin
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

Put shared output and data source settings in a base file.

```toml
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

Extend it from the project config.

```toml
extend = "base.toml"

[[table]]
name = "tb_user"
className = "User"
```

## Run

Run the script with `kotlinc -script`.

```bash
kotlinc -script example.main.kts
```

Successful generation logs the written files.

```text
File generated successfully: src/main/kotlin/com/example/entity/User.kt
```

