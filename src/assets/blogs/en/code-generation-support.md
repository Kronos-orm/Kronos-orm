# Kronos Code Generator

<center>
<img src="/assets/images/features/img-3.png" width="300"/>
</center>

--------

In version 0.0.3 of `Kronos`, we introduced support for code generators. This feature helps developers generate `Kronos`
-related code more quickly, reducing the amount of manual coding required.

## Database First

In the `Database First` mode, the code generator can automatically generate corresponding `Kronos` entity classes and
related query code based on the table structure in the database. This allows developers to directly use the generated
code for database operations without manually writing entity classes.

## Design of Code Generator

We hope to implement the functionality of the code generator in the form of simple configuration + template code. This
way, developers can customize the generated code according to their own needs.

Initially, we first chose `toml` as the configuration file format because it is simple and easy to read, suitable for
storing configuration information.

Moreover, for developers familiar with Android development, the `toml` format configuration files are also relatively
easy to get started with.

In terms of templates, we considered using a rendering engine like `FreeMarker`, but we hope to achieve template
rendering by writing `Kotlin` code, which can better maintain consistency with the code style of `Kronos`. Therefore, we
had to give up on `FreeMarker`.

Then we turned to `Kotlin Poet`, a library for generating `Kotlin` code that can help us implement the functionality of
a code generator. However, `Kotlin Poet` has a certain learning curve and requires some time to get familiar with its
usage, and we do not want developers to need to learn a new library when using the code generator.

In the end, we chose to use `kts` + our self-developed DSL combined with string templates to implement the code
generator. This approach allows us to better maintain consistency with the `Kronos` code style while also enabling more
flexible code generation.

## Usage of code generator

The usage of the code generator is very simple, just configure the configuration file and template file, and then run
the code generator.

### 配置文件

```toml
[[table]]
name = "tb_user"
className = "User"

[[table]]
name = "student"

[strategy]
tableNamingStrategy = "lineHumpNamingStrategy"
fieldNamingStrategy = "lineHumpNamingStrategy"
createTimeStrategy = "create_time"
updateTimeStrategy = "update_time"
logicDeleteStrategy = "deleted"
optimisticLockStrategy = "version"

[dataSource]
wrapperClassName = "com.kotlinorm.KronosBasicWrapper"
dataSourceClassName = "com.alibaba.druid.pool.DruidDataSource"
url = "jdbc:mysql://localhost:3306/kronos_testing?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&useServerPrepStmts=true&rewriteBatchedStatements=true"
username = "root"
password = "******"
driverClassName = "com.mysql.cj.jdbc.Driver"
initialSize = 5
maxActive = 10

[output]
targetDir = "../src/main/kotlin/com/kotlinorm/orm/pojo"
packageName = "com.kotlinorm.orm.pojo"
tableCommentLineWords = 80
```

Configuration Explanation:

- `table`: Define the database tables that need to generate code, each table can specify the name and the corresponding
  entity class name.
    - `name`: Database table name.
    - `className`: The generated entity class name, if not specified, defaults to the camel case naming form of the
      table name with the first letter capitalized.
- `strategy`: Define the strategy for generating code, including table name strategy, field name strategy, creation time
  strategy, update time strategy, logical deletion strategy, optimistic lock strategy, etc. These strategies can also be
  globally set in the `Kronos` configuration.
    - `tableNamingStrategy`: Table name strategy, default is `NoneNamingStrategy`.
    - `fieldNamingStrategy`: Field name strategy, default is `NoneNamingStrategy`.
    - `createTimeStrategy`: Create a naming strategy, default is `NoneNamingStrategy`.
    - `updateTimeStrategy`: Update time strategy, default is `NoneNamingStrategy`.
    - `logicDeleteStrategy`: Logical deletion strategy, default is `NoneNamingStrategy`.
    - `optimisticLockStrategy`: Optimistic Locking Strategy, default is `NoneNamingStrategy`.
- `dataSource`: Define the database connection information, including the data source class name, connection URL,
  username, password, etc.
    - `wrapperClassName`: Data source wrapper class, default is `com.kotlinorm.KronosBasicWrapper`.
    - `dataSourceClassName`: Data source class name.
    - `url`: Database connection URL.
    - `username`: Database username.
    - `password`: Database password.
    - `driverClassName`: Database driver class name.
    - Other related configurations for connection pools, such as `initialSize`, `maxActive`, etc., should be configured
      according to the specific implementation of the connection pool.
- `output`: Define the output directory for generated code, multiple directories can be specified.
    - `targetDir`: The target directory for generating code, the default is `../src/main/kotlin/com/kotlinorm/orm/pojo`.
    - `packageName`: The package name for generating code, defaulting to the package name inferred from `targetDir`.
    - `tableCommentLineWords`: The line character limit for table comments is 80, with a default value.

### Template file

The template file is written in `kts` format and can use string templates to generate code. The following is an example
template file:
**example.main.kts**

```kotlin
#!/usr/bin/env kotlin

@file:Repository("https://central.sonatype.com/repository/maven-snapshots/")
@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.0.4")
@file:DependsOn("com.kotlinorm:kronos-core:0.0.4")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:0.0.4")
@file:DependsOn("com.mysql:mysql-connector-j:9.2.0")
@file:DependsOn("com.alibaba:druid:1.2.24")

import com.kotlinorm.codegen.KronosConfig.Companion.write
import com.kotlinorm.codegen.TemplateConfig.Companion.template
import com.kotlinorm.codegen.init
import com.kotlinorm.codegen.kotlinType
import java.time.LocalDateTime

val now = java.time.LocalDateTime.now()
init("config.toml")

template {
    +"package $packageName"
    +""
    +imports.joinToString("\n") { "import $it" }
    +""
    +formatedComment
    +"// @author: Kronos-Codegen"
    +"// @date: $now"
    +""
    +"@Table(name = \"$tableName\")"
    +indexes.toAnnotations()
    +"data class $className("
    fields.forEach { field ->
        field.annotations().forEach { annotation ->
            +"${indent(4)}$annotation"
        }
        +"${indent(4)}var ${field.name}: ${field.kotlinType}? = null,"
    }
    +"): KPojo"
}.write()
```

Template Creation Explanation:

- Write template files using the `kotlin` scripting language, which can generate code using string templates, specify
  the repository address of the dependency library using the `@file:Repository` annotation, and specify the coordinates
  of the dependency library using the `@file:DependsOn` annotation.
- The naming format of kts scripts must be `xxx.main.kts`, where `xxx` is the template name, so that the kts script can
  run independently.
- Use `init("config.toml")` to initialize the configuration file and read the contents of the configuration file.
- Use the `template` function to define template content, and string templates can be used to generate code.
- Use the `write` function to write the generated code to a file.

The variables and functions provided by `codegen` are used to obtain table names, class names, field information, and
generate corresponding code.

- `packageName`: Retrieve the package name for generating code from the configuration file.
- `imports`: Automatically generate the list of classes to be imported.
- `formatedComment`: Table annotation formatted string.
- `tableName`: Table names obtained from the database table.
- `className`: Class name obtained from the configuration file.
- `fields`: The field list obtained from the database table, each field includes name, type, and other information.
- `indexes`: Index list obtained from the database table.
- `indent(n)`: The function for indentation returns a string of n spaces.
- `annotations()`: Used to obtain the annotation list of the field.
- `toAnnotations()`: Used to convert an index list to an annotation list.

## Run code generator

To run the code generator, simply execute the following command:

```bash
kotlinc -script example.main.kts
```

This will generate code based on the information in the configuration file and write it to the specified directory.
