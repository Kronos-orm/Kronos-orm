{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

安装、SQL 生成或运行结果需要检查时，可以从本页按入口定位。

## 检查依赖坐标

Kronos 依赖使用文档宏中的稳定版本，第三方驱动使用匹配数据库和 JDK 的当前稳定版。

```kotlin group="Dependencies" name="build.gradle.kts" icon="gradle"
dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

将 `<latest-stable>` 替换为当前数据库和 JDK 可用的稳定驱动版本。

完整配置见 {{ $.keyword("getting-started/quick-start", ["快速开始"]) }} 和 {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }}。

## 检查编译插件配置

Kronos 编译期支持在 Kotlin 编译时加载。编译声明 `KPojo` 或编写 Kronos DSL 的模块，并查看构建输出。

```bash group="Compiler plugin" name="Gradle" icon="terminal"
./gradlew :app:compileKotlin
```

```text group="Compiler plugin" name="output"
[Kronos] Kronos compiler plugin K2 initialized
BUILD SUCCESSFUL
```

声明实体或查询 DSL 的每个 source set 都需要启用 Kronos Gradle 或 Maven 插件。完整构建示例和 source set 规则见 {{ $.keyword("configuration/compiler-plugins", ["编译器插件"]) }}。

## 检查 KPojo generated members

`__tableName`、`toDataMap()`、`__columns` 和动态访问器会在 source set 经过 Kronos 编译后可用。

```kotlin group="KPojo Check" name="kotlin" icon="kotlin"
val user = User(id = 7, name = "Ada")

println(user.__tableName)
println(user.toDataMap())
println(user.__columns.map { it.name })
```

```text group="KPojo Check" name="output"
tb_user
{id=7, name=Ada}
[id, name]
```

运行结果出现 `__tableName must be overridden by the compiler plugin` 时，为该模块启用 Kronos 编译插件并重新导入 IDE 项目。完整检查见 {{ $.keyword("configuration/compiler-plugins", ["检查生成的 KPojo 成员"]) }}。

## projection alias 或标量子查询诊断

投影结果字段需要稳定名称。函数、聚合、窗口函数、原生 SQL 和标量子查询作为 select item 时，给它们设置 alias。

```kotlin group="Projection 1" name="alias" icon="kotlin"
User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }
    .toList()
```

标量子查询作为值使用时，选择一个字段并使用 `limit(1)`。

```kotlin group="Projection 2" name="scalar" icon="kotlin"
User()
    .select { user ->
        [
            user.id,
            Order()
                .select { it.amount }
                .where { it.userId == user.id }
                .limit(1)
                .alias("lastAmount")
        ]
    }
    .toList()
```

`KRONOS_SELECT_ITEM_REQUIRES_ALIAS`、`KRONOS_SCALAR_SUBQUERY_REQUIRES_LIMIT` 和 `KRONOS_SCALAR_SUBQUERY_REQUIRES_SINGLE_COLUMN` 这些诊断码见 {{ $.keyword("configuration/compiler-plugins", ["修复常见诊断"]) }}。查询形态示例见 {{ $.keyword("query/projection", ["投影"]) }} 和 {{ $.keyword("query/subqueries", ["子查询"]) }}。

## 配置数据源

Kronos ORM 操作需要配置 `Kronos.dataSource`。

```kotlin group="DataSource 1" name="kotlin" icon="kotlin"
val wrapper = KronosJdbcWrapper(dataSource)

Kronos.dataSource = { wrapper }

val users = User().select().toList()
```

某次操作需要指定数据源时，可以直接通过 wrapper 执行。

```kotlin group="DataSource 2" name="wrapper" icon="kotlin"
val users = User()
    .select()
    .toList(wrapper)
```

## 方言输出与预期不同

Kronos 根据 `KronosDataSourceWrapper.dbType` 渲染 SQL。标识符引用或分页语法不符合预期时，打印 wrapper 当前值。

```kotlin group="Dialect" name="kotlin" icon="kotlin"
val wrapper = KronosJdbcWrapper(dataSource)

println(wrapper.dbType)
println(wrapper.sqlDialect)
```

标识符引用、分页、upsert、DDL 和函数示例见 {{ $.keyword("database/dialect-support", ["数据库方言支持"]) }}。

## 检查 `where()` 条件

`select()` 只选择表和投影。空 `where()` 会按当前对象的可查询非空字段生成 query-by-example 条件。

```kotlin group="Where 1" name="select" icon="kotlin"
User(id = 1).select()
// SELECT ... FROM user

User(id = 1).select().where()
// WHERE id = 1
```

带 lambda 的 `where` 由 lambda 控制本次条件。

```kotlin group="Where 2" name="lambda" icon="kotlin"
User(id = 1)
    .select()
    .where { it.name == "Ada" }
// WHERE name = 'Ada'
```

`where` 组合规则见 {{ $.keyword("query/conditions", ["条件表达式"]) }}。

## 检查 DataGuard 写入和 DDL 保护

DataGuard 会在执行前拦截全表写入和受保护的表操作。

```kotlin group="DataGuard 1" name="result" icon="kotlin"
DataGuardPlugin.enable()

User()
    .delete()
    .execute()
```

```text group="DataGuard 1" name="error"
UnsupportedOperationException: Delete operation is not allowed.
```

为计划内维护操作增加条件，或配置允许规则。

```kotlin group="DataGuard 2" name="allow" icon="kotlin"
DataGuardPlugin.enable {
    deleteAll {
        allow {
            tableName = "tmp_import_error"
        }
    }
}
```

策略示例见 {{ $.keyword("advanced/data-guard", ["DataGuard"]) }}。

## 逻辑删除或乐观锁改变了 SQL

逻辑删除和乐观锁注解会根据 KPojo 元数据增加写入条件或赋值。

```kotlin group="Strategy" name="kotlin" icon="kotlin"
data class User(
    @PrimaryKey
    var id: Int? = null,
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = null,
    @Version
    var version: Int? = null
) : KPojo
```

变更行为见 {{ $.keyword("mutation/logic-delete", ["逻辑删除"]) }} 和 {{ $.keyword("mutation/optimistic-lock", ["乐观锁"]) }}。

## 查看生成 SQL

页面说明了 `build...Task` 形式时，可以先构建任务再查看 SQL。

```kotlin group="SQL" name="kotlin" icon="kotlin"
val task = Kronos.dataSource.table.buildCreateTableAsSelectTask(
    OrderArchive(),
    Order().select { [it.id, it.userId] }
)

println(task.sql)
println(task.paramMap)
```

select、join、subquery 和 CTAS 的 SQL 示例见 {{ $.keyword("query/subqueries", ["子查询"]) }} 和 {{ $.keyword("database/create-table-as-select", ["基于查询创建表"]) }}。

## Codegen 或 IDE 插件入口

数据库元数据需要生成 KPojo 文件时，使用 Codegen。

```kotlin group="Tools 1" name="codegen" icon="kotlin"
@file:DependsOn("com.kotlinorm:kronos-codegen:{{ $.kronosVersion() }}")
```

编辑器支持和诊断入口见 IDEA 插件页面。

```text group="Tools 2" name="pages"
资源 -> 代码生成器
资源 -> IntelliJ IDEA 插件
```

详见 {{ $.keyword("resources/codegen", ["代码生成器"]) }} 和 {{ $.keyword("resources/idea-plugin", ["IntelliJ IDEA 插件"]) }}。
