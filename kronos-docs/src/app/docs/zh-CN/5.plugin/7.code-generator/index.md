{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 代码生成器

Kronos 提供了基于 **Database First** 模式的代码生成器。它读取数据库表结构，自动生成 Kotlin 实体类，减少手动编码工作量。

代码生成器使用 TOML 配置文件 + Kotlin Script (kts) 模板的方式，让你完全掌控生成代码的风格。

> 关于 Database First 概念的更多信息，请参阅 **[Database First](/documentation/zh-CN/concept/database-first)**。

## {{ $.title("依赖")}}添加依赖

将 `kronos-codegen` 依赖添加到你的项目中。由于代码生成通常是构建时任务，你可以在 kts 模板文件中以脚本依赖的方式添加：

```kotlin
@file:DependsOn("com.kotlinorm:kronos-codegen:0.0.7")
@file:DependsOn("com.kotlinorm:kronos-core:0.0.7")
```

你还需要数据库的 JDBC 驱动和连接池。例如使用 MySQL + Druid：

```kotlin
@file:DependsOn("com.alibaba:druid:1.2.20")
@file:DependsOn("mysql:mysql-connector-java:8.0.33")
```

## {{ $.title("配置")}}配置文件

创建一个 TOML 配置文件（如 `config.toml`），包含以下部分：

### 表配置

定义需要生成代码的数据库表：

```toml
[[table]]
name = "tb_user"
className = "User"

[[table]]
name = "student"
```

- `name`（必填）：数据库表名。
- `className`（可选）：生成的类名。默认为表名的大驼峰形式。

### 策略配置

定义命名和特殊字段策略：

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

- `tableNamingStrategy` / `fieldNamingStrategy`：`"lineHumpNamingStrategy"`（下划线转驼峰）或 `"noneNamingStrategy"`（不转换）。默认：`"noneNamingStrategy"`。
- `createTimeStrategy` / `updateTimeStrategy` / `logicDeleteStrategy` / `optimisticLockStrategy` / `primaryKeyStrategy`：对应特殊字段的列名。设置后，生成器会自动添加相应注解（如 `@CreateTime`、`@UpdateTime`、`@LogicDelete`）。

### 数据源配置

配置数据库连接信息：

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

- `wrapperClassName`：`KronosDataSourceWrapper` 实现类。默认：`com.kotlinorm.KronosBasicWrapper`。
- `dataSourceClassName`：`javax.sql.DataSource` 实现类（如 Druid、HikariCP、DBCP2）。
- 其他属性（url、username、password、driverClassName 等）通过反射设置到 DataSource 上。

### 输出配置

```toml
[output]
targetDir = "../src/main/kotlin/com/kotlinorm/orm/pojo"
packageName = "com.kotlinorm.orm.pojo"
tableCommentLineWords = 80
```

- `targetDir`（必填）：生成文件的输出目录。
- `packageName`（可选）：包名。如果省略，将从 `targetDir` 推断。
- `tableCommentLineWords`（可选）：表注释每行最大字符数。默认：80。

### 配置继承

你可以将公共配置抽取到基础配置文件中，然后通过 `extend` 继承：

```toml
[extend]
path = "base-config.toml"
```

## {{ $.title("模板")}}模板文件

创建一个 Kotlin Script 文件（如 `codegen.main.kts`）：

```kotlin
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.0.7")
@file:DependsOn("com.kotlinorm:kronos-core:0.0.7")
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

### 模板变量和函数

在 `template { }` 块中可以使用以下变量和函数：

| 变量 / 函数 | 说明 |
|-------------|------|
| `packageName` | 输出配置中的包名 |
| `imports` | 自动收集的 import 语句列表 |
| `tableName` | 数据库表名 |
| `className` | 生成的类名 |
| `fields` | 当前表的字段（列）列表 |
| `indexes` | 当前表的索引列表 |
| `formatedComment` | 格式化的表注释（KDoc 格式） |
| `indent(n)` | 返回 `n` 个空格的字符串 |
| `field.annotations()` | 返回字段的注解字符串列表 |
| `field.kotlinType` | 数据库列类型映射的 Kotlin 类型 |
| `indexes.toAnnotations()` | 将索引定义转换为 `@TableIndex` 注解 |
| `+""` (unaryPlus) | 向生成输出追加一行 |

### 类型映射

代码生成器将数据库列类型映射为 Kotlin 类型：

| 数据库类型 | Kotlin 类型 |
|-----------|-------------|
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

## {{ $.title("运行")}}运行代码生成器

直接执行 kts 脚本：

```bash
kotlinc -script codegen.main.kts
```

生成的实体类将写入 `targetDir` 指定的目录。

> **Note**
> - kts 文件名必须遵循 `xxx.main.kts` 格式才能独立执行。
> - 确保数据库可访问且 JDBC 驱动在类路径中可用。
> - 你可以使用 `@file:Repository` 注解指定自定义 Maven 仓库地址。
