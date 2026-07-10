# Kronos ORM 高级功能

## 目录

1. [级联操作](#级联操作)
2. [内置函数](#内置函数)
3. [逻辑删除](#逻辑删除)
4. [乐观锁](#乐观锁)
5. [命名策略](#命名策略)
6. [空值策略](#空值策略)
7. [序列化](#序列化)
8. [原生SQL](#原生sql)
9. [表操作、索引和 DDL](#表操作索引和-ddl)
10. [子查询、投影和派生来源](#子查询投影和派生来源)
11. [多数据源](#多数据源)
12. [日志配置](#日志配置)
13. [DataGuard](#dataguard)
14. [Codegen](#codegen)

---

## 级联操作

级联通过 `@Cascade` 注解定义关系，支持级联查询、插入、更新、删除。

### 模型定义

```kotlin
// 一对多
data class Director(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    @Cascade(["id"], ["directorId"])
    var movies: List<Movie>? = null
) : KPojo

data class Movie(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var directorId: Int? = null,
    @Cascade(["directorId"], ["id"])
    var director: Director? = null
) : KPojo
```

### 级联查询

```kotlin
// 查询 Director 时自动加载关联的 movies
val director = Director(id = 1).select()
    .cascade { [Director::movies] }
    .first()
// director.movies 自动填充
```

### 级联插入

```kotlin
val director = Director(
    name = "Spielberg",
    movies = listOf(
        Movie(name = "Jaws"),
        Movie(name = "E.T.")
    )
)
director.insert().cascade(enabled = true).execute()
// 自动插入 director 和关联的 movies，并填充外键
```

### 级联更新

```kotlin
director.update().cascade(enabled = true).set { it.name }.by { it.id }.execute()
```

### 级联删除

```kotlin
director.delete().cascade(enabled = true).where { it.id == 1 }.execute()
// 自动删除关联的 movies
```

---

## 内置函数

Kronos 提供数据库函数的类型安全调用，通过 `f` 对象访问：

```kotlin
// 聚合函数
User().select { f.count(it.id) }.first<Int>()
User().select { f.sum(it.age) }.first<Int>()
User().select { f.avg(it.age) }.first<Double>()
User().select { f.max(it.age) }.first<Int>()
User().select { f.min(it.age) }.first<Int>()

// 字符串函数
User().select { f.upper(it.name) }.toList()
User().select { f.lower(it.name) }.toList()
User().select { f.length(it.name) }.toList()
User().select { f.concat(it.name, it.age) }.toList()

// 数学函数
User().select { f.abs(it.age) }.toList()
User().select { f.ceil(it.score) }.toList()
User().select { f.round(it.score, 2) }.toList()
User().select { f.trunc(it.score, 2) }.toList()

// 在条件中使用函数
User().select().where { f.length(it.name) > 5 }.toList()

// 在分组中使用
User().select { [it.age, f.count(it.id)] }
    .groupBy { it.age }
    .having { f.count(it.id) > 5 }
    .toList()
```

函数表达式用于 `select { ... }` 结果时要使用 `.alias("name")`，alias 会成为 `toMapList()` 的 Map key 和生成投影属性名。窗口函数当前入口是 `f.rowNumber()`，需要导入 `com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber`：

```kotlin
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber

val ranked = Order()
    .select {
        [
            it.id,
            it.userId,
            f.rowNumber()
                .over {
                    partitionBy(it.userId)
                    orderBy(it.status.desc())
                }
                .alias("rn")
        ]
    }

val rows = ranked
    .orderBy { it.rn.asc() }
    .toList()

val rowNumber: Int? = rows.first().rn
```

函数 SQL 由当前数据库方言渲染。窗口 alias 可以在同层 `orderBy` 中使用；如果要在 `where`、`groupBy` 或 `having` 中使用窗口 alias，先生成投影，再进入下一层查询。

---

## 逻辑删除

配置方式：

1. 全局策略：
```kotlin
with(Kronos) {
    logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
}
```

2. 注解方式：
```kotlin
data class User(
    @LogicDelete
    var deleted: Boolean? = false
) : KPojo
```

启用后：
- `delete()` 自动变为 `UPDATE ... SET deleted = 1`
- `select()` 自动添加 `WHERE deleted = 0`
- 需要物理删除时，对当前 delete 调用 `.logic(false)`

---

## 乐观锁

```kotlin
data class User(
    @Version
    var version: Int? = null
) : KPojo
```

或全局配置：
```kotlin
with(Kronos) {
    optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
}
```

insert 会初始化版本号，update 和逻辑删除会递增版本号。需要按读取时的版本匹配时，在 `where { ... }` 中显式加入版本条件。

```kotlin
User(id = 1, version = 3)
    .update()
    .set { it.name = "Kronos" }
    .where { it.id == 1 && it.version == 3 }
    .execute()
```

---

## 命名策略

```kotlin
with(Kronos) {
    // 驼峰转下划线：userName -> user_name
    tableNamingStrategy = lineHumpNamingStrategy
    fieldNamingStrategy = lineHumpNamingStrategy
    tableNamingStrategy = noneNamingStrategy
}
```

---

## 空值策略

控制条件中值为 null 时的行为：

```kotlin
where { (it.age == nullableAge).ifNoValue(NoValueStrategyType.Ignore) }
where { (it.age == nullableAge).ifNoValue(NoValueStrategyType.True) }
where { (it.age == nullableAge).ifNoValue(NoValueStrategyType.False) }
```

---

## 序列化

复杂对象字段可通过 `@Serialize` 注解以 JSON 形式存储：

```kotlin
data class User(
    @Serialize
    var tags: List<String>? = null
) : KPojo
```

需要配置序列化处理器：
```kotlin
with(Kronos) {
    serializeProcessor = JacksonProcessor()  // 或 GsonProcessor
}
```

自定义处理器实现 `KronosSerializeProcessor` 时，`serialize` 和 `deserialize` 都会收到字段声明上的 `KType`。处理 `List<String>`、`List<List<String>>`、`List<Profile>` 等泛型字段时，直接使用这个完整声明类型。

Kotlinx Serialization 接入示例：

```kotlin
import com.kotlinorm.interfaces.KronosSerializeProcessor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

object KotlinxSerializeProcessor : KronosSerializeProcessor {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun serialize(obj: Any, kType: KType): String {
        @Suppress("UNCHECKED_CAST")
        val valueSerializer = serializer(kType) as KSerializer<Any>
        return json.encodeToString(valueSerializer, obj)
    }

    override fun deserialize(serializedStr: String, kType: KType): Any {
        return json.decodeFromString(serializer(kType), serializedStr)
            ?: error("Kotlinx serialization returned null for $kType")
    }
}

@Serializable
data class ProfileSetting(
    val theme: String,
    val shortcuts: List<String>
)
```

交给 Kotlinx Serialization 的类型需要有 serializer；data class 通常加 `@Serializable`，集合元素类型也要可序列化。

---

## 原生SQL

```kotlin
import com.kotlinorm.database.SqlExecutor.queryList

val users = dataSource.queryList<User>(
    "SELECT * FROM user WHERE name = :name AND age > :age",
    mapOf("name" to "Kronos", "age" to 18)
)
```

---

## 表操作、索引和 DDL

表操作从 `Kronos.dataSource().table` 或某个 `KronosDataSourceWrapper.table` 进入。

```kotlin
val table = Kronos.dataSource().table

table.exists("tb_user")
table.exists<User>()
table.createTable(User())
table.syncTable(User())
table.truncateTable("tmp_session", restartIdentity = false)
table.dropTable(User())
```

`createTable(instance)` 和 `syncTable(instance)` 读取 KPojo 元数据，包括 `@Table`、`@Column`、`@PrimaryKey`、`@ColumnType`、`@Default`、`@NonNull`、表注释和 `@TableIndex`。`@ColumnType` 的 `length`、`scale` 会进入方言渲染，例如 `VARCHAR(80)`、`DECIMAL(12,2)` 或 Oracle `NUMBER(12,2)`。

`@TableIndex` 使用字符串 `type`、`method` 和 PostgreSQL 专用的 `concurrently`：

```kotlin
@TableIndex(
    name = "idx_user_tenant_email",
    columns = ["tenant_id", "email"],
    type = "UNIQUE",
    method = "BTREE"
)
data class User(...) : KPojo
```

CTAS 使用查询输出创建表：

```kotlin
val paidOrders = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }

Kronos.dataSource().table.createTable(OrderArchive(), paidOrders)
```

CTAS 的表形态来自查询输出。需要主键、默认值、注释或索引等 KPojo schema 元数据时，先 `createTable(Target())`，再使用 `KSelectable.insert<Target>()` 写入查询结果。`dropTable`、`truncateTable`、`syncTable` 等维护操作建议配合 DataGuard 或发布流程审批。

`syncTable(instance)` 会根据 KPojo 元数据同步表结构。表不存在时创建表并返回 `false`；表已存在时执行列、注释和索引差异同步并返回 `true`。

```kotlin
val existed = Kronos.dataSource().table.syncTable(User())
```

需要预览 DDL 时，使用 `TableOperation` 的 build API。statement builder 返回语法对象，task builder 返回当前方言渲染后的 SQL 和参数：

```kotlin
val table = Kronos.dataSource().table

val createStatement = table.buildCreateTableStatement(User())
val dropStatement = table.buildDropTableStatement("user", ifExists = true)
val truncateStatement = table.buildTruncateTableStatement("user", restartIdentity = false)

val truncateTask = table.buildTruncateTableTask("user", restartIdentity = false)
val (sql, params, atomicTasks) = truncateTask
```

CTAS 也可以通过 `buildCreateTableAsSelectTask(Target(), query)` 先生成 SQL，再进入发布审批或执行流程。

---

## 子查询、投影和派生来源

函数、聚合、标量子查询和窗口函数等非直接 select 字段要使用 `.alias("name")`。alias 会成为生成投影的属性名。

```kotlin
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val generatedRows = nameLengths.toList()
val firstLength: Int? = generatedRows.first().nameLength

val rows = nameLengths
    .select { [it.id, it.nameLength] }
    .where { it.nameLength > 8 }
    .toList()
```

无参 `toList()` / `first()` 返回编译器生成的投影类型；需要命名结果类型时，使用 `select(UserSummary::class) { ... }` 映射到 DTO，select 输出名要和 DTO 属性名对应。

原生 SQL select item 使用字符串表达式，alias 决定 Map key 或生成投影属性名：

```kotlin
val rows = User()
    .select { ["count(*)".alias("total")] }
    .toMapList()

val total = rows.first()["total"]
```

`"count(*)".alias("total")` 会保留 `count(*)` 作为 SQL 表达式，并使用 `total` 作为结果名。需要绑定值时，把参数放在 `where { ... }` 和 `patch(...)` 中。

窗口函数结果也是投影字段。需要过滤窗口排名时，先选出 alias，再把查询作为下一层来源：

```kotlin
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber

val ranked = Order()
    .select {
        [
            it.id,
            it.userId,
            it.status,
            f.rowNumber()
                .over {
                    partitionBy(it.userId)
                    orderBy(it.status.asc())
                }
                .alias("rn")
        ]
    }

val firstOrders = ranked
    .select { [it.id, it.userId, it.status] }
    .where { it.rn == 1 }
    .toList()
```

排序、分页和聚合可以和投影组合：

```kotlin
val (total, rows) = User()
    .select { [it.id, it.name, f.count(it.id).alias("orderCount")] }
    .groupBy { [it.id, it.name] }
    .having { f.count(it.id) > 0 }
    .orderBy { it.id.desc() }
    .page(1, 20)
    .withTotal()
    .toList<User>()
```

标量子查询可以作为 select 字段使用。

标量子查询选择一个字段并通常配合 `limit(1)`。Kotlin 需要类型提示时，在 `limit(1)` 后添加类型转换，转换只用于 DSL 类型推断。

```kotlin
val users = User()
    .select { user ->
        [
            user.id,
            Order()
                .select { order -> order.status }
                .where { order -> order.userId == user.id }
                .limit(1)
                .alias("lastOrderStatus")
        ]
    }
    .toList()
```

谓词子查询可以用于 `in`、`!in`、`exists`、`!exists`、`any`、`some`、`all` 和 row-value tuple 条件。

```kotlin
val users = User()
    .select()
    .where {
        it.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
    }
    .toList()
```

`KSelectable` 可以作为 join 的派生查询源。

```kotlin
val paidOrders = Order()
    .select { [it.userId, it.status] }
    .where { it.status == 1 }

val users = User().join(paidOrders) { user, order ->
    leftJoin(order) { user.id == order.userId }
    select { [user.id, user.name, order.status] }
}.toList()
```

INSERT SELECT 使用 `KSelectable<Selected>.insert<Target>()`。

```kotlin
Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }
    .insert<OrderArchive>()
    .execute()
```

没有显式 values 时，源 select 字段数量必须匹配目标 KPojo 的可插入字段数量；目标字段顺序来自目标 KPojo 的 insertable 字段。目标使用 identity 主键时，该字段会从目标插入字段列表中排除。

CTAS 使用查询输出创建表。

```kotlin
val paidOrders = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }

Kronos.dataSource().table.createTable(OrderArchive(), paidOrders)
```

Update 可以使用子查询筛选要更新的行。

```kotlin
User(name = "active")
    .update { [it.name] }
    .where {
        it.id in Order()
            .select { order -> order.userId }
            .where { order -> order.status == 1 }
    }
    .execute()
```

Delete 可以使用 `exists` 或 `!exists` 子查询筛选要删除的行。

```kotlin
User()
    .delete()
    .where { user ->
        !exists(
            Order()
                .select()
                .where { order -> order.userId == user.id }
        )
    }
    .execute()
```

Upsert 的冲突更新赋值可以使用标量子查询。

```kotlin
User(id = 1, name = "seed")
    .upsert()
    .on { it.id }
    .set {
        it.name = (Order()
            .select { order -> order.status }
            .where { order -> order.status == 1 }
            .limit(1) as String?)
    }
    .onConflict()
    .execute()
```

`patch(...)` 可以为 `onConflict()` 提供动态冲突更新值，支持 `SqlExpr`、`KronosFunctionExpr`、`Field` 和标量 `KSelectable`：

```kotlin
import com.kotlinorm.beans.dsl.KronosFunctionExpr
import com.kotlinorm.syntax.expr.SqlExpr

User(id = 7, name = "seed", count = 2)
    .upsert { it.name }
    .patch(
        "count" to SqlExpr.NumberLiteral("10"),
        "name" to KronosFunctionExpr(SqlExpr.StringLiteral("patched"), "literal")
    )
    .on { it.id }
    .onConflict()
    .execute()

val countField = User().kronosColumns().single { it.name == "count" }

User(id = 8, name = "seed", count = 5)
    .upsert { it.name }
    .patch("name" to countField)
    .on { it.id }
    .onConflict()
    .execute()
```

`patch(...)` 的值在 `onConflict()` 路径中作为冲突更新赋值；fallback upsert 路径中，相同字段进入存在性检查后的 update set。

---

## 多数据源

```kotlin
val mysqlWrapper = KronosJdbcWrapper(mysqlDataSource)
val pgWrapper = KronosJdbcWrapper(pgDataSource)

// 默认数据源
with(Kronos) {
    dataSource = { mysqlWrapper }
}

// 指定数据源执行
user.insert().execute(pgWrapper)
user.select().toList(pgWrapper)

// 指定数据源的事务
Kronos.transact(pgWrapper) {
    user.insert().execute()
}
```

`KronosJdbcWrapper` 也可以在构造器中传入 `databaseType`，并通过配置 block 设置 JDBC statement、warning、参数绑定和结果映射；回答连接或方言识别问题时优先参考主指南“数据库与方言”段落。

---

## 日志配置

默认使用 `kronos-core` 的内置日志：

```kotlin
with(Kronos) {
    loggerType = KLoggerType.DEFAULT_LOGGER
    logPath = listOf("console", "logs/kronos")
}
```

日志输出包含 SQL、参数和执行结果：

```text
Executing [INSERT] task:
SQL:    INSERT INTO `user` (`name`) VALUES (:name)
PARAMS: {name=Kronos}
Affected rows: 1
-----------------------
```

关闭内置日志输出：

```kotlin
with(Kronos) {
    loggerType = KLoggerType.DEFAULT_LOGGER
    logPath = emptyList()
}
```

使用 `kronos-logging` 接入 JDK Logger：

```kotlin
dependencies {
    implementation("com.kotlinorm:kronos-logging:0.2.0")
}
```

```kotlin
KronosLoggerApp.detectLoggerImplementation()

with(Kronos) {
    loggerType = KLoggerType.JDK_LOGGER
}
```

---

## DataGuard

启用默认保护：

```kotlin
DataGuardPlugin.enable()
```

默认拒绝全表 delete：

```kotlin
User()
    .delete()
    .execute()
```

```text
UnsupportedOperationException: Delete operation is not allowed.
```

带条件的 delete 可以执行：

```kotlin
User()
    .delete()
    .where { it.id == 1 }
    .execute()
```

允许维护表执行全表更新：

```kotlin
DataGuardPlugin.enable {
    updateAll {
        allow {
            tableName = "user_archive"
        }
    }
}
```

允许临时表 DDL：

```kotlin
DataGuardPlugin.enable {
    truncate { allow { tableName = "tmp_%" } }
    drop { allow { tableName = "tmp_%" } }
    alter { allow { tableName = "tmp_%" } }
}
```

拒绝敏感表 drop：

```kotlin
DataGuardPlugin.enable {
    drop {
        allowAll()
        deny {
            tableName = "sensitive_%"
        }
    }
}
```

---

## Codegen

Codegen 用于 Database First 项目，从数据库表结构生成 Kotlin `KPojo` 实体类。

脚本依赖使用 Kronos `0.2.0`，JDBC Driver 和连接池使用与数据库、JDK 匹配的最新稳定版：

```kotlin
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.2.0")
@file:DependsOn("com.kotlinorm:kronos-core:0.2.0")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:0.2.0")
@file:DependsOn("org.apache.commons:commons-dbcp2:<latest-stable>")
@file:DependsOn("com.mysql:mysql-connector-j:<latest-stable>")
```

用 `[[table]]` 选择要生成的表：

```toml
[[table]]
name = "tb_user"
className = "User"

[[table]]
name = "tb_order"
className = "Order"
```

配置生成目录和包名：

```toml
[output]
targetDir = "src/main/kotlin/com/example/entity"
packageName = "com.example.entity"
tableCommentLineWords = 80
```

配置读取元数据的数据源：

```toml
[dataSource]
wrapperClassName = "com.kotlinorm.wrappers.KronosJdbcWrapper"
dataSourceClassName = "org.apache.commons.dbcp2.BasicDataSource"
url = "jdbc:mysql://localhost:3306/kronos_demo"
username = "root"
password = "your_password"
driverClassName = "com.mysql.cj.jdbc.Driver"
initialSize = 5
maxTotal = 10
```

配置命名和特殊字段策略：

```toml
[strategy]
tableNamingStrategy = "lineHumpNamingStrategy"
fieldNamingStrategy = "lineHumpNamingStrategy"
primaryKeyStrategy = "id"
createTimeStrategy = "create_time"
updateTimeStrategy = "update_time"
logicDeleteStrategy = "deleted"
optimisticLockStrategy = "version"
```

匹配字段会生成对应注解：

```kotlin
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
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

模板用 `init("config.toml")` 读取配置，用 `template { ... }.write()` 写文件：

```kotlin
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

执行脚本：

```bash
kotlinc -script example.main.kts
```

生成结果示例：

```text
File generated successfully: src/main/kotlin/com/example/entity/User.kt
```

