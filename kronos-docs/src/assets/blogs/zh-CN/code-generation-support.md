# Kronos 代码生成器

<center>
<img src="/assets/images/features/img-3.png" width="300"/>
</center>

--------

在`Kronos`0.0.3版本中，我们引入了代码生成器的支持。这个功能可以帮助开发者更快速地生成`Kronos`相关的代码，减少手动编写的工作量。

## Database First

在`Database First`模式下，代码生成器可以根据数据库中的表结构自动生成对应的`Kronos`
实体类和相关的查询代码。这样，开发者可以直接使用生成的代码进行数据库操作，而无需手动编写实体类。

## 代码生成器的设计过程

我们希望能够以简单的配置 + 模板代码的形式来实现代码生成器的功能。这样，开发者可以根据自己的需求自定义生成的代码。

在最初，我们首先选择了`toml`作为配置文件格式，因为它简单易读，适合存储配置信息。
而且对于熟悉android开发的开发者来说，`toml`格式的配置文件也比较容易上手。

模板方面，我们考虑了使用`FreeMarker`这样的渲染引擎，但是我们希望能够通过编写`Kotlin`代码来实现模板渲染，这样可以更好地与
`Kronos`的代码风格保持一致。 因此我们不得不放弃了`FreeMarker`。

继而我们转向了`Kotlin Poet`，它是一个用于生成`Kotlin`代码的库，可以帮助我们实现代码生成器的功能。但是`Kotlin Poet`
有一定的学习曲线，需要一定的时间来熟悉它的用法，我们不希望开发者在使用代码生成器时需要学习新的库。

最终我们选择使用`kts`+自研的dsl结合字符串模板的方式来实现代码生成器。这样可以更好地与`Kronos`的代码风格保持一致，同时也可以更灵活地生成代码。

## 代码生成器的使用

代码生成器的使用非常简单，只需要配置好配置文件和模板文件，然后运行代码生成器即可。

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

配置讲解：

- `table`：定义需要生成代码的数据库表，每个表可以指定名称和对应的实体类名称。
    - `name`：数据库表的名称。
    - `className`：生成的实体类名称，如果不指定则默认为表名的驼峰命名形式，首字母大写。
- `strategy`：定义生成代码的策略，包括表名策略、字段名策略、创建时间策略、更新时间策略、逻辑删除策略、乐观锁策略等，这些策略也可以在
  `Kronos`的配置中进行全局设置。
    - `tableNamingStrategy`：表名策略，默认为`NoneNamingStrategy`。
    - `fieldNamingStrategy`：字段名策略，默认为`NoneNamingStrategy`。
    - `createTimeStrategy`：创建时间策略，默认为`NoneNamingStrategy`。
    - `updateTimeStrategy`：更新时间策略，默认为`NoneNamingStrategy`。
    - `logicDeleteStrategy`：逻辑删除策略，默认为`NoneNamingStrategy`。
    - `optimisticLockStrategy`：乐观锁策略，默认为`NoneNamingStrategy`。
- `dataSource`：定义数据库连接信息，包括数据源类名、连接URL、用户名、密码等。
    - `wrapperClassName`：数据源包装类，默认为`com.kotlinorm.KronosBasicWrapper`。
    - `dataSourceClassName`：数据源类名。
    - `url`：数据库连接URL。
    - `username`：数据库用户名。
    - `password`：数据库密码。
    - `driverClassName`：数据库驱动类名。
    - 其他连接池相关配置，如`initialSize`、`maxActive`等，根据具体的连接池实现进行配置。
- `output`：定义生成代码的输出目录，可以指定多个目录。
    - `targetDir`：生成代码的目标目录，默认为`../src/main/kotlin/com/kotlinorm/orm/pojo`。
    - `packageName`：生成代码的包名，默认为根据`targetDir`推断的包名。
    - `tableCommentLineWords`：表注释的行字数限制，默认为80。

### 模板文件

模板文件使用`kts`格式编写，可以使用字符串模板来生成代码。以下是一个示例模板文件：

**example.main.kts**

```kotlin
#!/usr/bin/env kotlin

@file:Repository("https://central.sonatype.com/repository/maven-snapshots/")
@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.0.6")
@file:DependsOn("com.kotlinorm:kronos-core:0.0.6")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:0.0.6")
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

模板创作讲解：

- 使用`kotlin`脚本语言编写模板文件，可以使用字符串模板来生成代码，使用`@file:Repository`注释来指定依赖库的仓库地址，使用
  `@file:DependsOn`注释来指定依赖库的坐标。
- kts脚本命名格式需为`xxx.main.kts`，其中`xx`为模板名称，这样kts脚本才能独立运行。
- 使用`init("config.toml")`来初始化配置文件，读取配置文件中的内容。
- 使用`template`函数来定义模板内容，可以使用字符串模板来生成代码。
- 使用`write`函数来写入生成的代码到文件中。

`codegen`提供的变量和函数，用于获取表名、类名、字段等信息，并生成对应的代码：

- `packageName`：从配置文件中获取生成代码的包名。
- `imports`：自动生成需要导入的类列表。
- `formatedComment`：表注释的格式化字符串。
- `tableName`：从数据库表中获取的表名。
- `className`：从配置文件中获取的类名。
- `fields`：从数据库表中获取的字段列表，每个字段包含名称、类型等信息。
- `indexes`：从数据库表中获取的索引列表。
- `indent(n)`：用于缩进的函数，返回n个空格的字符串。
- `annotations()`：用于获取字段的注解列表。
- `toAnnotations()`：用于将索引列表转换为注解列表。

## 运行代码生成器

要运行代码生成器，只需执行以下命令：

```bash
kotlinc -script example.main.kts
```

这将会根据配置文件中的信息生成代码，并写入到指定的目录中。
