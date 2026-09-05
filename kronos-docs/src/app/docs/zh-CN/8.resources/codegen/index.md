{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 代码生成器

Kronos 代码生成器会读取数据库元数据，并写出 Kotlin `KPojo` 实体类。数据库结构是项目实体来源时，可以用它让实体类与已有表结构保持一致。

代码生成器使用 TOML 配置文件和 Kotlin Script 模板。配置文件选择表、数据源、命名策略和输出目录，模板控制生成的 Kotlin 文件内容。

> Database First 工作流请参考 **[Database First](/documentation/zh-CN/resources/database-first)**。
> 安装与运行时排查见 {{ $.keyword("resources/troubleshooting", ["故障排查"]) }}。

## {{ $.title("依赖")}}添加脚本依赖

在 `.main.kts` 脚本中加入 `kronos-codegen`、`kronos-core` 和 `kronos-jdbc-wrapper`。JDBC Driver 和连接池使用与数据库、JDK 匹配的最新稳定版。

```kotlin group="Script" name="example.main.kts" icon="kotlin"
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:{{ $.kronosVersion() }}")
@file:DependsOn("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
@file:DependsOn("org.apache.commons:commons-dbcp2:<latest-stable>")
@file:DependsOn("com.mysql:mysql-connector-j:<latest-stable>")
```

将 `<latest-stable>` 替换为连接池或 JDBC Driver 项目发布的当前稳定版。

## {{ $.title("配置")}}选择数据表

创建 `config.toml`。每个 `[[table]]` 会生成一个 Kotlin 文件。

```toml group="Config 1" name="config.toml"
[[table]]
name = "tb_user"
className = "User"

[[table]]
name = "tb_order"
className = "Order"
```

配合下面的输出配置，生成器会为每张表写出一个文件。

```text group="Config 2" name="generated files"
src/main/kotlin/com/example/entity/User.kt
src/main/kotlin/com/example/entity/Order.kt
```

需要明确控制 Kotlin 类名时，设置 `className`。

```toml group="Config 3" name="class name"
[[table]]
name = "sys_account"
className = "Account"
```

## 配置输出目录

`targetDir` 指定生成文件目录，`packageName` 指定模板写入的包名。

```toml group="Output 1" name="config.toml"
[output]
targetDir = "src/main/kotlin/com/example/entity"
packageName = "com.example.entity"
tableCommentLineWords = 80
```

模板中可以直接读取这些值。

```kotlin group="Output 2" name="template" icon="kotlin"
template {
    +"package $packageName"
    +""
    +formatedComment
}
```

生成文件会以配置的包名和格式化后的表注释开头。

```kotlin group="Output 3" name="User.kt" icon="kotlin"
package com.example.entity

// User account table
```

## 配置数据源

`dataSource` 用于创建读取表注释、字段和索引的 JDBC 连接。下面示例使用 Apache DBCP2 和内置 `KronosJdbcWrapper`。

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

切换连接池时，使用该 DataSource 类支持的属性名。

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

切换连接池实现时，在脚本中加入对应依赖。

```kotlin group="DataSource 3" name="Druid dependency" icon="kotlin"
@file:DependsOn("com.alibaba:druid:<latest-stable>")
```

## 配置命名策略

`lineHumpNamingStrategy` 会将下划线名称映射为驼峰 Kotlin 属性名。

```toml group="Strategy 1" name="config.toml"
[strategy]
tableNamingStrategy = "lineHumpNamingStrategy"
fieldNamingStrategy = "lineHumpNamingStrategy"
```

使用该策略时，数据库字段 `create_time` 会生成 `createTime`。

```kotlin group="Strategy 2" name="User.kt" icon="kotlin"
var createTime: java.time.LocalDateTime? = null
```

希望生成名保持数据库元数据名称时，使用 `noneNamingStrategy`。

```toml group="Strategy 3" name="keep names"
[strategy]
tableNamingStrategy = "noneNamingStrategy"
fieldNamingStrategy = "noneNamingStrategy"
```

```kotlin group="Strategy 3" name="User.kt" icon="kotlin"
var create_time: java.time.LocalDateTime? = null
```

## 生成特殊字段注解

在策略中配置主键、创建时间、更新时间、逻辑删除和乐观锁字段。

```toml group="Annotations 1" name="config.toml"
[strategy]
primaryKeyStrategy = "id"
createTimeStrategy = "create_time"
updateTimeStrategy = "update_time"
logicDeleteStrategy = "deleted"
optimisticLockStrategy = "version"
```

数据库表包含对应字段时，`field.annotations()` 会写出 Kronos 注解。逻辑删除列需要定义`DEFAULT 0`（PostgreSQL 使用`DEFAULT false`），Codegen 才会生成下例中的`@Default`注解。

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

注解在 ORM 操作中的行为请参考 {{ $.keyword("mapping/annotations", ["注解配置"]) }}。

## 生成字段元数据注解

模板可以把数据库元数据转换成字段注解。非空数据库字段会生成 `@NonNull`。

```kotlin group="Column 1" name="template" icon="kotlin"
fields.forEach { field ->
    field.annotations().forEach { annotation ->
        +"""${indent(4)}$annotation"""
    }
    +"""${indent(4)}var ${field.name}: ${field.kotlinType}? = null,"""
}
```

必填的 `name` 字段会生成带 `@NonNull` 的属性。

```kotlin group="Column 2" name="User.kt" icon="kotlin"
@NonNull
var name: String? = null
```

带精度信息的 decimal 字段会生成 `@ColumnType`。

```kotlin group="Column 3" name="User.kt" icon="kotlin"
@ColumnType(type = KColumnType.DECIMAL, length = 10, scale = 2)
var balance: java.math.BigDecimal? = null
```

## 生成表索引

使用 `indexes.toAnnotations()` 写出从数据库元数据读取到的索引注解。

```kotlin group="Index 1" name="template" icon="kotlin"
+indexes.toAnnotations()
+"""@Table("$tableName")"""
```

生成的实体类会在类声明前带上 `@TableIndex`。

```kotlin group="Index 2" name="User.kt" icon="kotlin"
@TableIndex(name = "idx_user_email", columns = ["email"], type = "UNIQUE")
@Table("tb_user")
data class User(
    var email: String? = null
) : KPojo
```

## 创建模板

先调用 `init("config.toml")`，再调用 `template { ... }`。模板块会为每张配置的数据表执行一次。

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

生成文件会按照模板拼出的结构写入。

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

## 使用配置继承

把共享输出目录和数据源配置放入基础 TOML 文件。

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

项目配置可以继承基础文件，并补充自己的表列表。

```toml group="Extend 2" name="config.toml"
extend = "base.toml"

[[table]]
name = "tb_user"
className = "User"
```

脚本运行时读取项目配置。

```kotlin group="Extend 3" name="example.main.kts" icon="kotlin"
init("config.toml")
```

## 运行生成器

Kotlin 脚本文件名使用 `xxx.main.kts` 格式。

```bash group="Run 1" name="shell" icon="terminal"
kotlinc -script example.main.kts
```

运行成功后，生成器会输出每个写入的文件。

```text group="Run 2" name="output"
File generated successfully: src/main/kotlin/com/example/entity/User.kt
```

配置缺少必要区域时，脚本会输出明确错误信息。

```text group="Run 3" name="missing table"
IllegalArgumentException: Table configuration is required in config: config.toml
```
