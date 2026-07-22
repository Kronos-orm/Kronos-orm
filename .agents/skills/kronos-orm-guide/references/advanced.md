# Kronos ORM 高级功能

实体约束：KPojo 类本身不能声明类级泛型参数。请使用具体属性类型；否则编译时会报告 `KRONOS_GENERIC_KPOJO_NOT_SUPPORTED`。普通非 KPojo 泛型类不受此限制，非泛型 KPojo 可以使用 `List<String>` 等具体泛型属性。

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

val firstPerUser = ranked
    .filter { it.rn == 1 }
    .toList()

val rowNumber: Int? = rows.first().rn
```

函数 SQL 由当前数据库方言渲染。窗口 alias 可以在同层 `orderBy` 中使用；谓词需要读取窗口 alias 时，先生成投影，再使用 `filter` 建立派生查询。`filter` receiver 只有当前 `Selected` 字段，并保持该投影作为结果类型。

### PostgreSQL 数组比较

PostgreSQL 数组比较使用 `f.any(...)` 和 `f.all(...)`。导入对应函数后，把数组表达式放在比较右侧：

```kotlin
import com.kotlinorm.functions.bundled.exts.PostgresFunctions.all
import com.kotlinorm.functions.bundled.exts.PostgresFunctions.any

val acceptedIds = intArrayOf(1, 2, 3)
val blockedIds = intArrayOf(8, 9)

val rows = User()
    .select()
    .where {
        it.id == f.any(acceptedIds) &&
            it.id != f.all(blockedIds)
    }
    .toList()
```

该条件生成 PostgreSQL `ANY (...)` 和 `ALL (...)` 数组谓词。

---

## 逻辑删除

配置方式：

1. 全局策略：
```kotlin
Kronos.logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
```

2. 注解方式：
```kotlin
data class User(
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = false
) : KPojo
```

启用后：
- `delete()` 自动变为 `UPDATE ... SET deleted = 1`
- `select()` 自动添加 `WHERE deleted = 0`
- 普通 upsert 匹配到已逻辑删除记录时更新原行，并把逻辑删除字段恢复为活动值
- `onConflict()` upsert 会在插入列和冲突更新赋值中维护逻辑删除活动值
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
Kronos.optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
```

insert 会初始化版本号，update、逻辑删除和 upsert 更新分支会递增版本号。需要按读取时的版本匹配时，在 `where { ... }` 中显式加入版本条件。

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

## 动态空值条件

动态变量可能为 null 时，用 `takeIf` 决定条件为 true 时是否生成谓词，用 `takeUnless` 决定条件为 false 时是否生成谓词；需要显式 fallback 时使用普通 Kotlin `if`/`else`、`when` 和布尔 SQL：

```kotlin
where { (it.age == nullableAge).takeIf(nullableAge != null) }
where { (it.status == 0).takeUnless(includeInactive) }
where { if (nullableAge != null) { it.age == nullableAge } else { true.asSql() } }
where { if (nullableAge != null) { it.age == nullableAge } else { false.asSql() } }
data class UserFilter(val id: Int? = null, val name: String? = null)
val filter = UserFilter(id = 7)
where {
    when {
        filter.id != null -> it.id == filter.id
        filter.name != null -> it.name == filter.name
        else -> it.active == true
    }
}
```

`takeIf`/`takeUnless` 的 Boolean 参数和 `if`/`when` 的条件按普通 Kotlin 求值。

普通 class、data class、object、companion/`@JvmStatic` 和顶层属性在 SQL 比较中都是运行时值，直接使用即可。当前 source 字段也直接参与条件表达式。

对于当前查询 source 之外的 KPojo 属性，在属性链末端使用 `.value` 读取实际 Kotlin 值，例如 `it.id == probe.id.value`。

字面量 `where { it.age == null }` / `where { it.age != null }` 表示 SQL `IS NULL` / `IS NOT NULL`；动态变量为 `null` 时才进入无值策略。

---

## 序列化

复杂对象字段可通过 `@Serialize` 注解使用 serialized 文本存储；具体格式由注册的 `ValueCodec` 决定，JSON 只是常见示例：

```kotlin
data class User(
    @Serialize
    var tags: List<String>? = null
) : KPojo
```

使用 `serializedValueCodec` 把序列化库的两个函数包装成普通 `ValueCodec`，再通过唯一入口 `Kronos.registerValueCodec` 注册。编码和解码都收到字段声明上的完整 `KType`，一次注册即可处理对象、`List<String>`、`List<List<String>>`、`List<Profile>` 和 `List<Enum>`。

标量 enum 不需要 `@Serialize`：没有显式列类型时使用 `VARCHAR + Enum.name`，整数 `@ColumnType` 使用 ordinal。稳定的 code、value 或 label 通过同一个 `ValueCodec` 入口覆盖；`@Serialize List<Enum>` 则把完整集合作为一个字符串值交给 serialized codec。

Kotlinx Serialization 接入示例：

```kotlin
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.serializedValueCodec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

val serializationRegistration = Kronos.registerValueCodec(
    serializedValueCodec(
        encode = { value, type ->
            @Suppress("UNCHECKED_CAST")
            val valueSerializer = serializer(type) as KSerializer<Any>
            json.encodeToString(valueSerializer, value)
        },
        decode = { text, type -> json.decodeFromString(serializer(type), text) }
    )
)

@Serializable
data class ProfileSetting(
    val theme: String,
    val shortcuts: List<String>
)
```

交给 Kotlinx Serialization 的类型需要有 serializer；data class 通常加 `@Serializable`，集合元素类型也要可序列化。后注册的 codec 优先匹配；应用通常长期保留注册，测试或热更新结束时调用 `serializationRegistration.close()`。

需要覆盖编译器生成的 KPojo 构造时，使用精确 `KType` 注册 `Kronos.registerKPojoFactory(typeOf<T>(), KPojoFactory { ... })`。后注册的 factory 优先，关闭句柄恢复之前的用户或生成 factory；当前不支持泛型 KPojo。factory 只创建新实例，不接管字段 ValueCodec 转换，完整示例见主指南“数据类定义”中的 KPojo 工厂段落。

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

`select { it }`、`select { [it] }` 和 `select { listOf(it) }` 都和 `select()` 一样返回源 KPojo 类型。排除字段、追加 alias、函数、原生 SQL 或其他投影项时才生成投影类型。

重复投影输出名需要用 Kotlin 标准 opt-in 确认；保留全部值后，第一次出现使用原名，后续值使用 `_1`、`_2` 等确定性后缀。显式请求名会先全局保留，例如 `id, id, id_1` 解析为 `id, id_2, id_1`。

```kotlin
import com.kotlinorm.annotations.UnsafeProjectionOverride

@OptIn(UnsafeProjectionOverride::class)
val duplicated = User()
    .select { [it.id, it.id, it.name.alias("id_1")] }
```

Selected alias 遮蔽 Source 名称时，只有同层 `orderBy` Context 实际读取冲突名称才需要 opt-in；`where`、`groupBy` 和 `having` 仍读取 Source。`filter` 会进入外层派生查询并读取当前 `Selected`，其 receiver 不包含未选中的 Source 字段。先用 `it - it.name` 移除 Source 字段再恢复同名 alias 不需要 opt-in。生成投影和 Context 属性使用 selected 表达式的实际类型。

```kotlin
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val generatedRows = nameLengths.toList()
val firstLength: Int? = generatedRows.first().nameLength

val rows = nameLengths
    .filter { it.nameLength > 8 }
    .toList()
```

`nameLengths.filter { ... }` 始终建立派生查询边界，等价于 `nameLengths.select().where { ... }`。filter receiver 只暴露 `id` 和 `nameLength`；外层还需要改变输出字段时，使用显式 `select { ... }.where { ... }`。

无参 `toList()` / `first()` 返回编译器生成的投影类型；需要命名结果类型时，使用 `select(UserSummary::class) { ... }` 映射到 DTO，select 输出名要和 DTO 属性名对应。

原生 SQL select item 使用字符串表达式，alias 决定 Map key 或生成投影属性名：

```kotlin
val rows = User()
    .select { ["count(*)".alias("total")] }
    .toMapList()

val total = rows.first()["total"]
```

`"count(*)".alias("total")` 会保留 `count(*)` 作为 SQL 表达式，并使用 `total` 作为结果名。需要绑定值时，把参数放在 `where { ... }` 和 `patch(...)` 中。

窗口函数结果也是投影字段。需要过滤窗口排名时，先选出 alias，再使用 `filter`：

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
    .filter { it.rn == 1 }
    .toList()
```

排序、分页和聚合可以和投影组合：

```kotlin
val page = User()
    .select { [it.id, it.name, f.count(it.id).alias("orderCount")] }
    .groupBy { [it.id, it.name] }
    .having { f.count(it.id) > 0 }
    .orderBy { it.id.desc() }
    .page(1, 20)
    .withTotal()
    .toList<User>()

val rows = page.records
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
    leftJoin { user.id == order.userId }
        .select { [user.id, user.name, order.status] }
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
    .onConflict()
    .execute()

val countField = User().__columns.single { it.name == "count" }

User(id = 8, name = "seed", count = 5)
    .upsert { it.name }
    .patch("name" to countField)
    .onConflict()
    .execute()
```

`patch(...)` 的值在 `onConflict()` 路径中作为冲突更新赋值；普通 upsert 路径中，相同字段进入匹配后的 update set。

---

## 多数据源

```kotlin
val mysqlWrapper = KronosJdbcWrapper(mysqlDataSource)
val pgWrapper = KronosJdbcWrapper(pgDataSource)

// 默认数据源
Kronos.dataSource = { mysqlWrapper }

// 指定数据源执行
user.insert().execute(pgWrapper)
user.select().toList(pgWrapper)

// 指定数据源的事务
Kronos.transact(pgWrapper) {
    user.insert().execute()
}
```

`KronosJdbcWrapper` 也可以在构造器中传入 `databaseType`，并通过配置 block 设置 JDBC statement、warning、参数绑定和结果映射；回答连接或方言识别问题时优先参考主指南“数据库与方言”段落。

typed Map 查询只接受直接 `Map`/`MutableMap` 声明（顶层可空也支持），因为 JDBC 行容器固定为 `LinkedHashMap`；自定义、固定或重排的 Map subtype 会被拒绝。key 支持 `String`、`String?` 和 `*`，`Any`/`Any?`/`*` value 保持 raw，具体 value 通过完整 `KType` 转换。

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

使用 `kronos-logging` 接入 JDK Logger：

```kotlin
dependencies {
    implementation("com.kotlinorm:kronos-logging:0.3.0")
}
```

```kotlin
KronosLoggerApp.detectLoggerImplementation()

Kronos.loggerType = KLoggerType.JDK_LOGGER
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

脚本依赖使用 Kronos `0.3.0`，JDBC Driver 和连接池使用与数据库、JDK 匹配的最新稳定版：

```kotlin
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.kotlinorm:kronos-codegen:0.3.0")
@file:DependsOn("com.kotlinorm:kronos-core:0.3.0")
@file:DependsOn("com.kotlinorm:kronos-jdbc-wrapper:0.3.0")
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
    @Default("0") // @Default("false") for Postgres
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
