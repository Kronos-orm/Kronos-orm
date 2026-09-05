{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Generate KPojo classes from an existing schema

Use Database First when database tables already exist and the Kotlin model should follow that schema. In Kronos, this workflow is handled by {{ $.keyword("resources/codegen", ["Code Generator"]) }}: read JDBC metadata, select tables in `config.toml`, and write Kotlin `KPojo` files from a Kotlin Script template.

Start with a table that is already managed by the database team.

```sql group="Schema" name="mysql" icon="mysql"
CREATE TABLE tb_user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    create_time DATETIME,
    update_time DATETIME,
    deleted BIT DEFAULT 0,
    version INT
);
```

## Select tables and output

Create `config.toml`. The `[[table]]` list chooses which database tables become Kotlin files. `targetDir` and `packageName` decide where the generated source is written.

```toml group="Config 1" name="config.toml"
[[table]]
name = "tb_user"
className = "User"

[output]
targetDir = "src/main/kotlin/com/example/entity"
packageName = "com.example.entity"
tableCommentLineWords = 80
```

With this configuration, the generator writes one file.

```text group="Config 2" name="generated files"
src/main/kotlin/com/example/entity/User.kt
```

## Configure metadata reading

The `dataSource` section creates the JDBC connection used by Codegen. This example uses Apache DBCP2 and the built-in `KronosJdbcWrapper`.

```toml group="DataSource" name="config.toml"
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

Use the property names supported by the selected `DataSource` class. Add the matching JDBC driver and connection pool dependency in the script.

## Map database names to KPojo fields

Use `lineHumpNamingStrategy` when underscore table or column names should become camel-case Kotlin names. Strategy fields generate Kronos annotations when matching columns are present.

```toml group="Strategy 1" name="config.toml"
[strategy]
tableNamingStrategy = "lineHumpNamingStrategy"
fieldNamingStrategy = "lineHumpNamingStrategy"
primaryKeyStrategy = "id"
createTimeStrategy = "create_time"
updateTimeStrategy = "update_time"
logicDeleteStrategy = "deleted"
optimisticLockStrategy = "version"
```

For the `tb_user` table above, `create_time` becomes `createTime`, and matching strategy columns receive the corresponding Kronos annotations.

```kotlin group="Strategy 2" name="User.kt" icon="kotlin"
package com.example.entity

import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.NonNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.annotations.Version
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @NonNull
    var username: String? = null,
    var email: String? = null,
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

For annotation behavior in insert, update, delete, and query operations, see {{ $.keyword("mapping/annotations", ["Annotation Config"]) }}.

## Run the generator

Create `example.main.kts` and add the Kronos artifacts, JDBC driver, and connection pool. The template below writes a compact KPojo class for every configured table.

```kotlin group="Script" name="example.main.kts" icon="kotlin"
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:{{ $.kronosVersion() }}")
@file:DependsOn("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
@file:DependsOn("org.apache.commons:commons-dbcp2:<latest-stable>")
@file:DependsOn("com.mysql:mysql-connector-j:<latest-stable>")

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

Run the script with `kotlinc -script`.

```bash group="Run 1" name="shell" icon="terminal"
kotlinc -script example.main.kts
```

After a successful run, Codegen logs each written file.

```text group="Run 2" name="output"
File generated successfully: src/main/kotlin/com/example/entity/User.kt
```

## Keep generated KPojo files aligned

When the database schema changes, update `config.toml` if the table list, output package, or naming strategy changes, then rerun the script. Review the generated Kotlin source before committing it.

```text group="Review" name="workflow"
1. Apply the database schema change.
2. Run kotlinc -script example.main.kts.
3. Review generated KPojo files.
4. Use the generated KPojo in Kronos queries and mutations.
```

For the full template API, config inheritance, column metadata annotations, and index generation, see {{ $.keyword("resources/codegen", ["Code Generator"]) }}. For applying schema changes from KPojo metadata, see {{ $.keyword("database/schema-sync", ["Schema Sync"]) }}.
