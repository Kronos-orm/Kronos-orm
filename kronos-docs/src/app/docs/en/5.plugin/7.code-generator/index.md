{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Code Generator

Kronos provides a code generator based on the **Database First** approach. It reads your database table structure and automatically generates Kotlin entity classes, reducing manual coding effort.

The code generator uses a TOML configuration file + Kotlin Script (kts) template, giving you full control over the generated code style.

> For more about the Database First concept, see **[Database First](/documentation/en/concept/database-first)**.

## {{ $.title("Dependencies")}}Adding Dependencies

Add the `kronos-codegen` dependency to your project. Since code generation is typically a build-time task, you can add it as a script dependency in your kts template file:

```kotlin
@file:DependsOn("com.kotlinorm:kronos-codegen:0.0.6")
@file:DependsOn("com.kotlinorm:kronos-core:0.0.6")
```

You also need a JDBC driver and connection pool for your database. For example, with MySQL + Druid:

```kotlin
@file:DependsOn("com.alibaba:druid:1.2.20")
@file:DependsOn("mysql:mysql-connector-java:8.0.33")
```

## {{ $.title("Configuration")}}Configuration File

Create a TOML configuration file (e.g. `config.toml`) with the following sections:

### Table Configuration

Define which tables to generate code for:

```toml
[[table]]
name = "tb_user"
className = "User"

[[table]]
name = "student"
```

- `name` (required): The database table name.
- `className` (optional): The generated class name. Defaults to the PascalCase form of the table name.

### Strategy Configuration

Define naming and special field strategies:

```toml
[strategy]
tableNamingStrategy = "lineHumpNamingStrategy"
fieldNamingStrategy = "lineHumpNamingStrategy"
createTimeStrategy = "create_time"
updateTimeStrategy = "update_time"
logicDeleteStrategy = "deleted"
optimisticLockStrategy = "version"
primaryKeyStrategy = "id"
```

- `tableNamingStrategy` / `fieldNamingStrategy`: `"lineHumpNamingStrategy"` (underscore to camelCase) or `"noneNamingStrategy"` (no conversion). Default: `"noneNamingStrategy"`.
- `createTimeStrategy` / `updateTimeStrategy` / `logicDeleteStrategy` / `optimisticLockStrategy` / `primaryKeyStrategy`: The column name of the corresponding special field. When set, the generator will add the appropriate annotation (e.g. `@CreateTime`, `@UpdateTime`, `@LogicDelete`).

### DataSource Configuration

Configure the database connection:

```toml
[dataSource]
wrapperClassName = "com.kotlinorm.KronosBasicWrapper"
dataSourceClassName = "com.alibaba.druid.pool.DruidDataSource"
url = "jdbc:mysql://localhost:3306/my_database?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false"
username = "root"
password = "your_password"
driverClassName = "com.mysql.cj.jdbc.Driver"
initialSize = 5
maxActive = 10
```

- `wrapperClassName`: The `KronosDataSourceWrapper` implementation class. Default: `com.kotlinorm.KronosBasicWrapper`.
- `dataSourceClassName`: The `javax.sql.DataSource` implementation class (e.g. Druid, HikariCP, DBCP2).
- Other properties (url, username, password, driverClassName, etc.) are set on the DataSource via reflection.

### Output Configuration

```toml
[output]
targetDir = "../src/main/kotlin/com/kotlinorm/orm/pojo"
packageName = "com.kotlinorm.orm.pojo"
tableCommentLineWords = 80
```

- `targetDir` (required): The output directory for generated files.
- `packageName` (optional): The package name. If omitted, it is inferred from `targetDir`.
- `tableCommentLineWords` (optional): Max characters per line in table comments. Default: 80.

### Configuration Inheritance

You can split common settings into a base config and extend it:

```toml
[extend]
path = "base-config.toml"
```

## {{ $.title("Template")}}Template File

Create a Kotlin Script file (e.g. `codegen.main.kts`):

```kotlin
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.0.6")
@file:DependsOn("com.kotlinorm:kronos-core:0.0.6")
@file:DependsOn("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")

import com.kotlinorm.codegen.*

init("config.toml")

template {
    +"package $packageName"
    +""
    +imports.joinToString("\n") { "import $it" }
    +""
    +formatedComment
    +"@Table(\"$tableName\")"
    +indexes.toAnnotations()
    +"data class $className("
    +fields.mapIndexed { index, field ->
        val annotations = field.annotations().joinToString("\n") { "${indent(4)}$it" }
        val annotationStr = if (annotations.isEmpty()) "" else "$annotations\n"
        val isLast = index == fields.size - 1
        val comma = if (isLast) "" else ","
        "${annotationStr}${indent(4)}var ${field.name}: ${field.kotlinType}? = null$comma"
    }.joinToString("\n")
    +") : KPojo"
}

write()
```

### Template Variables and Functions

The following variables and functions are available inside the `template { }` block:

| Variable / Function | Description |
|---------------------|-------------|
| `packageName` | The package name from the output configuration |
| `imports` | Auto-collected list of import statements |
| `tableName` | The database table name |
| `className` | The generated class name |
| `fields` | List of fields (columns) for the current table |
| `indexes` | List of indexes for the current table |
| `formatedComment` | Formatted table comment as a KDoc block |
| `indent(n)` | Returns a string of `n` spaces |
| `field.annotations()` | Returns the list of annotation strings for a field |
| `field.kotlinType` | The Kotlin type mapped from the database column type |
| `indexes.toAnnotations()` | Converts index definitions to `@TableIndex` annotations |
| `+""` (unaryPlus) | Appends a line to the generated output |

### Type Mapping

The code generator maps database column types to Kotlin types:

| Database Type | Kotlin Type |
|---------------|-------------|
| BIT | `Boolean` |
| TINYINT | `Byte` |
| SMALLINT | `Short` |
| INT / INTEGER | `Int` |
| BIGINT | `Long` |
| FLOAT | `Float` |
| DOUBLE | `Double` |
| DECIMAL / NUMERIC | `BigDecimal` |
| VARCHAR / CHAR / TEXT | `String` |
| JSON / ENUM | `String` |
| BLOB / VARBINARY | `ByteArray` |
| DATE | `LocalDate` |
| DATETIME | `LocalDateTime` |
| TIMESTAMP | `Instant` |
| UUID | `java.util.UUID` |

## {{ $.title("Run")}}Running the Code Generator

Execute the kts script directly:

```bash
kotlinc -script codegen.main.kts
```

The generated entity classes will be written to the directory specified in `targetDir`.

> **Note**
> - The kts file name must follow the `xxx.main.kts` format for standalone execution.
> - Make sure the database is accessible and the JDBC driver is available in the classpath.
> - You can add the `@file:Repository` annotation to specify custom Maven repositories for dependencies.
