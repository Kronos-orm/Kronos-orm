{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 从已有表结构生成 KPojo

数据库表已经存在，并且 Kotlin 模型需要跟随该表结构时，使用 Database First 工作流。在 Kronos 中，该工作流由 {{ $.keyword("resources/codegen", ["代码生成器"]) }} 承接：读取 JDBC 元数据，在 `config.toml` 中选择表，并通过 Kotlin Script 模板写出 Kotlin `KPojo` 文件。

先从数据库中已有的表开始。

```sql group="Schema" name="mysql" icon="mysql"
CREATE TABLE tb_user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    create_time DATETIME,
    update_time DATETIME,
    deleted BIT,
    version INT
);
```

## 选择表和输出目录

创建 `config.toml`。`[[table]]` 列表决定哪些数据库表生成 Kotlin 文件，`targetDir` 和 `packageName` 决定生成源码的写入位置。

```toml group="Config 1" name="config.toml"
[[table]]
name = "tb_user"
className = "User"

[output]
targetDir = "src/main/kotlin/com/example/entity"
packageName = "com.example.entity"
tableCommentLineWords = 80
```

使用这份配置时，生成器会写出一个文件。

```text group="Config 2" name="generated files"
src/main/kotlin/com/example/entity/User.kt
```

## 配置元数据读取

`dataSource` 用于创建 Codegen 读取表注释、字段和索引的 JDBC 连接。下面示例使用 Apache DBCP2 和内置 `KronosJdbcWrapper`。

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

使用当前 `DataSource` 类支持的属性名。脚本中需要加入匹配的 JDBC Driver 和连接池依赖。

## 将数据库命名映射为 KPojo 字段

下划线表名或列名需要生成驼峰 Kotlin 名称时，使用 `lineHumpNamingStrategy`。策略字段匹配到数据库列时，会生成 Kronos 注解。

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

对于上面的 `tb_user` 表，`create_time` 会生成 `createTime`，匹配策略的字段会带上对应的 Kronos 注解。

```kotlin group="Strategy 2" name="User.kt" icon="kotlin"
package com.example.entity

import com.kotlinorm.annotations.CreateTime
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
    var deleted: Boolean? = null,
    @Version
    var version: Int? = null
) : KPojo
```

注解在 insert、update、delete 和 query 中的行为见 {{ $.keyword("mapping/annotations", ["注解配置"]) }}。

## 运行生成器

创建 `example.main.kts`，加入 Kronos 依赖、JDBC Driver 和连接池。下面的模板会为每张配置的数据表写出紧凑的 KPojo 类。

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

使用 `kotlinc -script` 运行脚本。

```bash group="Run 1" name="shell" icon="terminal"
kotlinc -script example.main.kts
```

运行成功后，Codegen 会输出每个写入的文件。

```text group="Run 2" name="output"
File generated successfully: src/main/kotlin/com/example/entity/User.kt
```

## 保持生成的 KPojo 与表结构一致

数据库表结构变化后，如果表列表、输出包名或命名策略发生变化，先更新 `config.toml`，再重新运行脚本。提交前检查生成出来的 Kotlin 源码。

```text group="Review" name="workflow"
1. 应用数据库表结构变更。
2. 运行 kotlinc -script example.main.kts。
3. 检查生成的 KPojo 文件。
4. 在 Kronos 查询和变更操作中使用生成的 KPojo。
```

完整模板 API、配置继承、字段元数据注解和索引生成见 {{ $.keyword("resources/codegen", ["代码生成器"]) }}。需要从 KPojo 元数据应用表结构变化时，见 {{ $.keyword("database/schema-sync", ["表结构同步"]) }}。
