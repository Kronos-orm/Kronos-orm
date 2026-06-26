# Module kronos-codegen

Code generation module for Kronos ORM. Reads database schemas and generates annotated KPojo Kotlin data classes.

Uses TOML config + Kotlin Script (`.main.kts`) + DSL template pattern. No FreeMarker or Kotlin Poet — just plain Kotlin string templates with a thin DSL layer.

## How It Works

1. Write a TOML config file (data source, tables, strategies, output path)
2. Write a `.main.kts` script that calls `init()` + `template {}` DSL
3. Run the script: `kotlinc -script example.main.kts`
4. Generated KPojo data classes are written to the target directory

## Quick Start

### 1. TOML Config (`config.toml`)

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
url = "jdbc:mysql://localhost:3306/mydb"
username = "root"
password = "pass"
driverClassName = "com.mysql.cj.jdbc.Driver"
initialSize = 5
maxActive = 10

[output]
targetDir = "src/main/kotlin/com/example/entity"
packageName = "com.example.entity"
tableCommentLineWords = 80
```

### 2. Kotlin Script (`example.main.kts`)

```kotlin
#!/usr/bin/env kotlin

@file:Repository("https://central.sonatype.com/repository/maven-snapshots/")
@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.1.0-SNAPSHOT")
@file:DependsOn("com.kotlinorm:kronos-core:0.1.0-SNAPSHOT")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:0.1.0-SNAPSHOT")
@file:DependsOn("com.alibaba:druid:1.2.24")
@file:DependsOn("com.mysql:mysql-connector-j:9.2.0")

import com.kotlinorm.codegen.init
import com.kotlinorm.codegen.TemplateConfig.Companion.template

init("config.toml")

template {
    +"""package $packageName"""
    +""
    +imports.joinToString("\n") { "import $it" }
    +""
    +formatedComment
    +indexes.toAnnotations()
    +"""@Table("$tableName")"""
    +"""data class $className("""
    fields.forEach { field ->
        field.annotations().forEach { annotation ->
            +"""${indent(4)}$annotation"""
        }
        +"""${indent(4)}var ${field.name}: ${field.kotlinType}? = null,"""
    }
    +"""): KPojo"""
}.write()
```

### 3. Run

```bash
kotlinc -script example.main.kts
```

Script naming: must be `xxx.main.kts` for standalone execution.

## Template DSL Reference

### Properties (on `KronosTemplate`)

| Property | Type | Description |
|----------|------|-------------|
| `packageName` | `String` | Target package name from config |
| `tableName` | `String` | Database table name |
| `className` | `String` | Generated class name |
| `tableComment` | `String` | Table comment from DB |
| `formatedComment` | `String` | Word-wrapped comment as `// ...` lines |
| `fields` | `List<Field>` | Column metadata from DB |
| `indexes` | `List<KTableIndex>` | Index definitions from DB |
| `imports` | `LinkedHashSet<String>` | Auto-managed import set |
| `content` | `String` | Accumulated output |

### Operators & Functions

| Function | Description |
|----------|-------------|
| `+string` | Appends a line to output (`operator fun String?.unaryPlus()`) |
| `indent(n)` | Returns `n` spaces |
| `field.annotations()` | Returns annotation strings, auto-adds imports |
| `field.kotlinType` | Maps `KColumnType` → Kotlin type (`INT`→`"Int"`, `DATETIME`→`"java.time.LocalDateTime"`) |
| `indexes.toAnnotations()` | Generates `@TableIndex(...)` strings |

### Auto-Generated Annotations

Based on field metadata and global strategy config:
- `@PrimaryKey(identity = true)` — field matches primary key strategy
- `@Necessary` — non-nullable, non-PK fields
- `@Default("value")` — fields with default values
- `@ColumnType(type, length, scale)` — non-standard column types
- `@CreateTime` / `@UpdateTime` / `@LogicDelete` / `@Version` — field matches global strategy

## Dependencies

- `implementation`: kronos-core, jackson-dataformat-toml
