{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 什么是 {{ $.title("kronos-orm-guide") }}

{{ $.code("kronos-orm-guide") }} 是从 Kronos 仓库 {{ $.code("release/llm") }} 分支发布的 AI 技能。它帮助 AI 编程助手理解 {{ $.title("Kronos ORM") }} API，并为你的项目生成正确的 ORM 代码。

该技能会教会 AI 助手：

- 使用合适的注解定义 {{ $.keyword("mapping/code-first", ["KPojo"]) }} 数据类
- 编写 CRUD 操作：{{ $.code("select") }}、{{ $.code("insert") }}、{{ $.code("update") }}、{{ $.code("delete") }}、{{ $.code("upsert") }}
- 使用条件 DSL：{{ $.code("where") }}、{{ $.code("having") }}、{{ $.code("on") }}
- 编写联表查询、联合查询和子查询
- 使用 {{ $.keyword("database/transactions", ["事务"]) }}、级联操作和 DDL 表操作
- 使用聚合、字符串、日期、数学等内置函数
- 配置命名策略、逻辑删除和乐观锁

## 一键安装

将 {{ $.code("release/llm") }} 分支克隆到 AI 工具约定的技能目录：

| 工具 | 命令 |
|------|------|
| {{ $.title("Cursor") }} | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .cursor/skills/kronos-orm-guide && rm -rf .cursor/skills/kronos-orm-guide/.git` |
| {{ $.title("默认 / 通用") }} | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .agents/skills/kronos-orm-guide && rm -rf .agents/skills/kronos-orm-guide/.git` |

Windsurf 和其他可读取 {{ $.code(".agents/skills/") }} 的工具使用默认命令。

## 准备项目上下文

让 AI 助手先读取 skill 和决定 Kronos 配置的项目文件，再开始生成代码。

```text group="Context" name="files"
.agents/skills/kronos-orm-guide/SKILL.md
.agents/skills/kronos-orm-guide/references/advanced.md
.agents/skills/kronos-orm-guide/references/annotations.md
build.gradle.kts 或 pom.xml
src/main/kotlin/... 现有 KPojo 类
```

需要结合 docs 生成代码时，优先给相关页面链接，不要粘贴大段示例。常见任务对应这些页面：

| 任务 | 文档页面 |
|------|----------|
| 项目配置 | {{ $.keyword("configuration/compiler-plugins", ["编译器插件"]) }} |
| 实体映射 | {{ $.keyword("mapping/code-first", ["KPojo"]) }} 和 {{ $.keyword("mapping/annotations", ["注解配置"]) }} |
| 条件和投影 | {{ $.keyword("query/conditions", ["条件表达式"]) }} 和 {{ $.keyword("query/projection", ["投影"]) }} |
| 子查询和 INSERT SELECT | {{ $.keyword("query/subqueries", ["子查询"]) }} |
| 数据库执行 | {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }} |

## 要求生成当前 API

提示词中写清构建工具、数据库和期望的 Kronos API，可以让回答落在当前编译器插件和 DSL 写法上。

```text group="Prompt 1" name="project setup"
使用 Kronos ORM skill。为这个 JVM Kotlin Gradle 项目添加 Kronos。
使用 Kotlin 2.4.0、com.kotlinorm.kronos-gradle-plugin、kronos-core
和 kronos-jdbc-wrapper。使用 KronosJdbcWrapper 配置 Kronos.dataSource。
全局设置使用 Kronos object 的直接属性赋值。
```

期望的依赖形态如下：

```kotlin group="Prompt 2" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
}
```

生成 ORM 代码时，一次只让 AI 处理一个实体和一个可运行查询。

```text group="Prompt 3" name="query"
使用 Kronos ORM，为 tb_user 表创建 User KPojo，包含 id、name、email、
deleted 和 version 字段。然后写一个查询，选择 id 和 name，
按 email 域名筛选用户，并用 build() 打印生成 SQL 和 paramMap。
```

回答应使用当前 DSL 形态。

```kotlin group="Prompt 4" name="expected kotlin" icon="kotlin"
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var email: String? = null,
    @LogicDelete
    var deleted: Boolean? = false,
    @Version
    var version: Int? = null
) : KPojo

val task = User()
    .select { [it.id, it.name] }
    .where { it.email like "%@example.com" }
    .build()

println(task.sql)
println(task.paramMap)
```

## 技能内容

| 文件 | 内容 |
|------|------|
| {{ $.code("SKILL.md") }} | 核心指南，包含项目配置、KPojo 定义、CRUD、条件 DSL、联表、事务和 DDL |
| {{ $.code("references/advanced.md") }} | 级联操作、内置/自定义函数、原生 SQL、跨库联表和序列化 |
| {{ $.code("references/annotations.md") }} | `@Table`、`@PrimaryKey`、`@Column`、`@CreateTime` 等注解参考 |

## 示例提示词

技能激活后，可以向 AI 助手提出 Kronos 相关任务：

- "创建一个包含 id、name、email 和时间戳的 User 实体"
- "写一个 User 和 Order 联表查询，带分页"
- "添加一个事务，插入用户及其订单"
- "如何为这个实体配置逻辑删除？"
- "写一个按 email 冲突时更新的 upsert"

AI 会根据技能中的 API 知识生成符合 Kronos DSL 习惯的代码，而不是泛泛的 ORM 写法。

## 验证回答

AI 修改代码后，先让它运行窄范围构建检查，并在执行数据库写入前查看 SQL。

```bash group="Validate 1" name="shell" icon="terminal"
./gradlew compileKotlin
```

```text group="Validate 1" name="output"
[Kronos] Kronos compiler plugin K2 initialized
BUILD SUCCESSFUL
```

查询和变更操作先要求给出 `build()` 示例，让 SQL 和参数可见。

```kotlin group="Validate 2" name="sql" icon="kotlin"
val task = User(id = 7)
    .delete()
    .where()
    .build()

println(task.sql)
println(task.paramMap)
```

参与 Kronos 仓库本身开发时，使用 {{ $.keyword("resources/contributing-with-ai", ["使用 AI 参与开发"]) }}，不要使用面向业务项目的 ORM skill。
