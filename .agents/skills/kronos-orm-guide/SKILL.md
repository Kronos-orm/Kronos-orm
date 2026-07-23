---
name: kronos-orm-guide
description: >
  Kronos ORM 使用指南。当用户询问如何使用 Kronos ORM 进行数据库操作时触发此技能，包括：
  定义数据类(KPojo)、CRUD操作(select/insert/update/delete/upsert)、条件DSL(where/having/filter/on)、
  联表查询(join)、表操作(DDL)、事务、级联(cascade)、逻辑删除、乐观锁、内置函数、Codegen等。
  当用户提到 Kronos、KPojo、kronos-core、kronos-orm 或相关数据库操作时，务必使用此技能。
---

# Kronos ORM 使用指南

Kronos 是一个基于 Kotlin 编译器插件的现代 ORM 框架，零反射、强类型、支持多数据库（MySQL、PostgreSQL、SQLite、SQL Server、Oracle）。

## Evolution Memory Protocol

当遇到 ORM 使用问题、报错、行为不符合预期或重复踩坑时，先读 `Evolution.index.md`，不要默认浏览完整演进历史。除非索引命中相关条目，否则不要打开演进记录；索引命中时只打开它链接的 `evolution/*.md` 单条记录文件。

优先级：

1. 读 `Evolution.index.md`，按症状、API、错误信息、数据库类型匹配。
2. 如果索引命中，只读取索引链接的 `evolution/*.md` 条目文件。
3. 如果索引没有命中，不打开完整演进记录，直接读本 skill 的相关章节或 `references/advanced.md` / `references/annotations.md`。
4. 验证修复后，只有可复用、已确认的问题才新增一个 `evolution/*.md` 文件，并给 `Evolution.index.md` 增加一行关键词索引。
5. 定期删除、归档或改写已经过时的记录，避免索引继续指向错误经验。

## 目录

1. [项目配置](#项目配置)
2. [数据类定义](#数据类定义)
3. [Kotlin 类型与列类型](#kotlin-类型与列类型)
4. [全局配置](#全局配置)
5. [日志配置](#日志配置)
6. [数据保护 DataGuard](#数据保护-dataguard)
7. [Codegen 代码生成](#codegen-代码生成)
8. [数据库与方言](#数据库与方言)
9. [Insert 插入](#insert)
10. [Delete 删除](#delete)
11. [Update 更新](#update)
12. [Select 查询](#select)
13. [子查询与投影](#子查询与投影)
14. [Upsert 存在则更新](#upsert)
15. [批量操作](#批量操作)
16. [逻辑删除与乐观锁](#逻辑删除与乐观锁)
17. [条件DSL](#条件dsl)
18. [Join 联表查询](#join)
19. [事务](#事务)
20. [表操作 DDL](#表操作)

高级主题（级联、内置函数、自定义函数、日志、DataGuard、Codegen 等）见 `references/advanced.md`。
注解完整参考见 `references/annotations.md`。

当前 docs 用户入口按新 IA 使用短路由：

- 起步：`getting-started/installation`、`getting-started/quick-start`、`getting-started/first-query`。
- 映射工作流：`mapping/code-first`、`mapping/kpojo`、`mapping/table-and-column`、`mapping/primary-key`、`mapping/indexes`。
- 查询首页和条件规则：`query/select`、`query/conditions`、`query/subqueries`。
- 数据库和方言：`database/connect-to-db`、`database/dialect-support`、`database/create-database-dialect`、`database/android-sqlite`。
- 配置：`configuration/global-config`、`configuration/common-strategy`、`configuration/logging`、`configuration/compiler-plugins`。
- 修改操作：`mutation/insert`、`mutation/update`、`mutation/delete`、`mutation/upsert`、`mutation/logic-delete`、`mutation/optimistic-lock`、`mutation/last-insert-id`。
- 工具入口：`resources/database-first`、`resources/codegen`、`resources/idea-plugin`、`resources/troubleshooting`。

---

## 项目配置

### Gradle (Kotlin DSL)

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "0.3.0"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.3.0")
    // JDBC 包装器（可选，提供开箱即用的数据源支持）
    implementation("com.kotlinorm:kronos-jdbc-wrapper:0.3.0")
    // JDBC Driver 与连接池使用和数据库/JDK 匹配的最新稳定版
    implementation("org.apache.commons:commons-dbcp2:<latest-stable>")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

### Maven

```xml
<dependency>
    <groupId>com.kotlinorm</groupId>
    <artifactId>kronos-core</artifactId>
    <version>0.3.0</version>
</dependency>
```

在 `kotlin-maven-plugin` 中添加编译器插件：
```xml
<compilerPlugins>
    <plugin>kronos-maven-plugin</plugin>
</compilerPlugins>
```

要求：JDK 8+，Kotlin 2.4.0+

技能中的 Kronos 推荐稳定版本直接写 `0.3.0`。`kronos-docs` Markdown 的版本宏只用于 docs 源文件，不用于本使用指南。

---

### Android/JVM SQLite

Android/JVM applications use the Gradle plugin, `kronos-core`, and an application-owned `KronosDataSourceWrapper` around Android `SQLiteDatabase`. Add `kronos-logging` when SQL output should appear in Logcat.

```kotlin
plugins {
    id("com.kotlinorm.kronos-gradle-plugin") version "0.3.0" apply false
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.3.0")
    implementation("com.kotlinorm:kronos-logging:0.3.0") // Logcat SQL output
}
```

Start from the reference `AndroidSQLiteDataSourceWrapper` source and assign the wrapper to `Kronos.dataSource` in the app's `Application.onCreate`, then register that `Application` in `AndroidManifest.xml`. When `kronos-logging` is present, call `KronosLoggerApp.detectLoggerImplementation()` in `onCreate` to send Kronos SQL logs to Logcat. Run schema preparation and repository work on an I/O dispatcher or executor. Use `Kronos.transact { ... }` for related writes with the Android SQLite default transaction options.

The dedicated `database/android-sqlite` chapter is the source of truth for setup, the reference wrapper, transaction scope, logging, and the complete Android example.

---

## 数据类定义

所有实体类必须实现 `KPojo` 接口，属性使用可空类型并提供默认值：

```kotlin
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.annotations.*
import com.kotlinorm.enums.KColumnType.VARCHAR
import java.time.LocalDateTime

@Table("tb_movie")
@TableIndex("idx_name", ["name"], "UNIQUE", "BTREE")
data class Movie(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Column("name") @ColumnType(VARCHAR, 255)
    var name: String? = null,
    var directorId: Long? = null,
    @Cascade(["directorId"], ["id"])
    var director: Director? = null,
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = false,
    @CreateTime
    var createTime: LocalDateTime? = null,
    @UpdateTime
    var updateTime: Date? = null
) : KPojo
```

KPojo 类本身不能声明类级泛型参数。必须为 KPojo 使用具体的属性类型，否则编译时会报告 `KRONOS_GENERIC_KPOJO_NOT_SUPPORTED`；普通非 KPojo 泛型类仍然可以使用，非泛型 KPojo 也可以拥有 `List<String>` 这样的具体泛型属性。

### KPojo 工厂与 KType

编译器会为可映射 KPojo 自动生成 factory。泛型代码需要创建实例时使用完整 `KType`；只有需要覆盖默认构造时才注册精确 factory。factory 只创建新对象，字段赋值和类型转换仍由统一 mapper/ValueCodec 流程处理：

```kotlin
import com.kotlinorm.Kronos
import com.kotlinorm.utils.KPojoFactory
import kotlin.reflect.typeOf

val factoryRegistration = Kronos.registerKPojoFactory(
    typeOf<Movie>(),
    KPojoFactory { _ -> Movie() }
)

// 同一 KType 后注册的 factory 优先；关闭本次注册后恢复之前的用户或生成 factory。
factoryRegistration.close()
```

注册键是精确的 `KType`，当前版本不支持泛型 KPojo；`close()` 幂等。

常用注解速查：

| 注解 | 作用 | 示例 |
|------|------|------|
| `@Table("name")` | 指定表名 | `@Table("tb_user")` |
| `@PrimaryKey(identity=true)` | 主键，自增 | 标注在 id 属性上 |
| `@Column("name")` | 指定列名 | `@Column("user_name")` |
| `@ColumnType(type, length, scale)` | 指定列类型、长度和精度 | `@ColumnType(KColumnType.VARCHAR, length = 80)` |
| `@Default("value")` | 默认值 | `@Default("0")` |
| `@LogicDelete` | 逻辑删除标记 | 标注在 deleted 属性上 |
| `@Version` | 乐观锁版本号 | 标注在 version 属性上 |
| `@CreateTime` | 自动填充创建时间 | 标注在 createTime 上 |
| `@UpdateTime` | 自动填充更新时间 | 标注在 updateTime 上 |
| `@Cascade` | 级联关系 | `@Cascade(["fkId"], ["id"])` |
| `@TableIndex` | 表索引 | 标注在类上 |
| `@Serialize` | serialized 文本存储 metadata | 复杂对象字段 |
| `@Ignore` | 忽略字段或指定忽略场景 | 非数据库字段 |
| `@DateTimeFormat` | 日期格式 | `@DateTimeFormat("yyyy-MM-dd")` |

完整注解说明见 `references/annotations.md`。

Code First 项目以 KPojo 类作为表结构元数据来源。定义 `@Table`、`@Column`、`@PrimaryKey`、`@TableIndex` 等注解后，使用 `Kronos.dataSource.table.createTable(User())` 创建表，或使用 `Kronos.dataSource.table.syncTable(User())` 同步已有表。已有数据库元数据需要反向生成 KPojo 时，使用 Database First / Codegen。

当前 KPojo 元数据 API 使用属性：`__kType`、`__tableName`、`__tableComment`、`__columns`、`__tableIndexes`、`__createTime`、`__updateTime`、`__logicDelete`、`__optimisticLock`。回答用户时不要把 legacy metadata functions 当作当前 API；读取列元数据用 `__columns`。

动态对象表是手动实现的 `KPojo` 实例，必须把 `__kType` 设为 `typeOf<KPojo>()` 作为运行时表模型标记。它适合运行时决定表名或字段列表的场景，静态业务模型仍优先使用普通 `data class` KPojo 和注解。显式重载的元数据属性需要标记 `@Ignore([IgnoreAction.ALL])`，避免它们被当作表字段。

```kotlin
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KType
import kotlin.reflect.typeOf

val runtimeUser = object : KPojo {
    @Ignore([IgnoreAction.ALL])
    override var __kType: KType = typeOf<KPojo>()
    @Ignore([IgnoreAction.ALL])
    override var __tableName = "tb_runtime_user"
    @Ignore([IgnoreAction.ALL])
    override var __tableComment = "runtime user table"
    @Ignore([IgnoreAction.ALL])
    override var __columns = mutableListOf(
        Field("id", "id"),
        Field("name", "name")
    )
    @Ignore([IgnoreAction.ALL])
    override var __tableIndexes = mutableListOf<KTableIndex>()

    var id: Int? = 6
    var name: String? = null
}

val user = runtimeUser
    .select()
    .where { it.id == 6 }
    .firstOrNull()
```

---

## Kotlin 类型与列类型

属性没有显式 `@ColumnType` 时，编译器插件会按 Kotlin 类型推断 `KColumnType`。推断结果可从 `__columns` 读取，也会参与 `createTable` 和 `syncTable` 的 DDL 渲染。

常用自动映射：

| Kotlin 类型 | 推断 `KColumnType` |
|-------------|--------------------|
| `Boolean` | `BIT` |
| `Byte` / `Short` / `Int` / `Long` | `TINYINT` / `SMALLINT` / `INT` / `BIGINT` |
| `Float` / `Double` | `FLOAT` / `DOUBLE` |
| `java.math.BigDecimal` | `DECIMAL` |
| `Char` / `String` | `CHAR` / `VARCHAR` |
| `java.time.LocalDate` / `java.util.Date` / `java.sql.Date` | `DATE` |
| `java.time.LocalTime` | `TIME` |
| `java.time.LocalDateTime` | `DATETIME` |
| `java.sql.Timestamp` | `TIMESTAMP` |
| `ByteArray` | `BLOB` |
| 其他类型 | `VARCHAR` |

使用 `@ColumnType` 覆盖推断类型，并通过 `length`、`scale` 控制 DDL 类型参数：

```kotlin
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

data class Invoice(
    @ColumnType(KColumnType.VARCHAR, length = 80)
    var title: String? = null,
    @ColumnType(KColumnType.DECIMAL, length = 12, scale = 2)
    var amount: java.math.BigDecimal? = null,
    @Serialize
    var payload: Map<String, Any?>? = null
) : KPojo
```

方言会把同一个 `KColumnType` 渲染为不同数据库类型。常见输出：

| 设置 | MySQL | PostgreSQL | SQLite | SQL Server | Oracle |
|------|-------|------------|--------|------------|--------|
| `VARCHAR, length = 80` | `VARCHAR(80)` | `VARCHAR(80)` | `TEXT` | `VARCHAR(80)` | `VARCHAR2(80)` |
| `DECIMAL, length = 12, scale = 2` | `DECIMAL(12,2)` | `DECIMAL(12,2)` | `NUMERIC` | `DECIMAL(12,2)` | `NUMBER(12,2)` |
| `JSON` | `JSON` | `JSONB` | `TEXT` | `JSON` | `JSON` |

`@Serialize` 用于需要保存为 JSON 文本的对象和集合属性。应用启动时配置 Gson 或 Kotlinx Serialization 一次后，JSON 文本作为字符串参数写入，插入和查询仍直接使用对象、`List<String>`、嵌套列表或 `List<Enum>`。`@ColumnType(KColumnType.JSON)` 可在建表时选择 JSON 列类型。

```kotlin
data class User(
    @Serialize
    var payload: Profile? = null,
    @Serialize
    var tags: List<String>? = null
) : KPojo
```

Gson 配置示例：

```kotlin
val gson = Gson()

Kronos.registerValueCodec(
    serializedValueCodec(
        encode = { value, type -> gson.toJson(value, type.javaType) },
        decode = { text, type -> gson.fromJson(text, type.javaType) }
    )
)
```

领域值可以使用自定义映射。例如，`Money` 属性可以将整数分值保存到 `BIGINT` 列。在模型上标注列类型后，`supports` 选择 `Money` 属性，`convert` 在写入时取分值、读取时创建 `Money`。普通 enum 默认保存 `Enum.name`；整数列保存枚举位置；数据库使用业务 code 时采用同样的自定义映射。

```kotlin
import com.kotlinorm.Kronos
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.valueCodec

data class Money(val cents: Long)

data class Invoice(
    @ColumnType(KColumnType.BIGINT)
    var total: Money? = null,
) : KPojo

Kronos.registerValueCodec(
    valueCodec(
        supports = { value, context ->
            context.targetType.classifier == Money::class &&
                when (context.direction) {
                    ValueCodecDirection.ENCODE -> value is Money
                    ValueCodecDirection.DECODE -> value is Number
                }
        },
        convert = { value, context ->
            when (context.direction) {
                ValueCodecDirection.ENCODE -> (value as Money).cents
                ValueCodecDirection.DECODE -> Money((value as Number).toLong())
            }
        }
    )
)
```

完整的 JSON、领域值和枚举示例分别见 `kronos-docs` 的 `mapping/serialization`、`configuration/value-codec` 和 `mapping/enum-serialization` 页面。

---

## 全局配置

配置优先级：

- 必须：`Kronos.dataSource = { wrapper }`，没有显式 wrapper 的 `execute()`、`toList()`、`first()` 等终端方法会读取它。
- 常用：`tableNamingStrategy`、`fieldNamingStrategy`、`defaultDateFormat`、`timeZone`、`logPath`。
- 按业务启用：`createTimeStrategy`、`updateTimeStrategy`、`logicDeleteStrategy`、`optimisticLockStrategy`、`strictSetValue`；自定义转换通过 `Kronos.registerValueCodec` 注册。

```kotlin
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import java.time.ZoneId

val wrapper by lazy {
    BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/mydb"
        username = "user"
        password = "pass"
    }.let { KronosJdbcWrapper(it) }
}

with(Kronos) {
    dataSource = { wrapper }
    tableNamingStrategy = lineHumpNamingStrategy  // userName -> user_name
    fieldNamingStrategy = lineHumpNamingStrategy
    createTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("create_time", "createTime"))
    updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))
    logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
    optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
    defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
    timeZone = ZoneId.of("Asia/Shanghai")
}
```

---

## 日志配置

内置日志由 `kronos-core` 提供，默认输出到控制台。需要同时输出控制台和日志目录时，配置 `Kronos.logPath`：

```kotlin
import com.kotlinorm.Kronos
import com.kotlinorm.enums.KLoggerType

with(Kronos) {
    loggerType = KLoggerType.DEFAULT_LOGGER
    logPath = listOf("console", "logs/kronos")
}
```

执行 ORM 任务时，日志包含操作类型、SQL、参数和结果：

```text
[yyyy-MM-dd HH:mm:ss.SSS] [info] [Kronos] Executing [SELECT] task:
SQL:    SELECT `id`, `name` FROM `user` WHERE `id` = :id
PARAMS: {id=1}
Found rows: 1
-----------------------
```

需要使用 JDK Logger 或 Apache Commons Logging 适配器时，引入 `kronos-logging` 并在应用启动时调用探测入口：

```kotlin
dependencies {
    implementation("com.kotlinorm:kronos-logging:0.3.0")
}
```

```kotlin
import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp
import com.kotlinorm.enums.KLoggerType

KronosLoggerApp.detectLoggerImplementation()

Kronos.loggerType = KLoggerType.JDK_LOGGER
```

使用 Apache Commons Logging 时，应用还需要加入对应 API：

```kotlin
dependencies {
    implementation("com.kotlinorm:kronos-logging:0.3.0")
    implementation("commons-logging:commons-logging:<latest-stable>")
}
```

```kotlin
KronosLoggerApp.detectLoggerImplementation()

Kronos.loggerType = KLoggerType.COMMONS_LOGGER
```

---

## 数据保护 DataGuard

`DataGuardPlugin` 在 Kronos 写入操作和表操作执行前检查全表写入、truncate、drop 和 alter。

启用默认保护：

```kotlin
import com.kotlinorm.plugins.DataGuardPlugin

DataGuardPlugin.enable()
```

默认策略会拒绝不带 `by` 或 `where` 的全表 delete：

```kotlin
DataGuardPlugin.enable()

User()
    .delete()
    .execute()
```

```text
UnsupportedOperationException: Delete operation is not allowed.
```

带明确条件的 delete 可以执行：

```kotlin
User()
    .delete()
    .where { it.id == 1 }
    .execute()
```

允许某张表执行计划内全表 update：

```kotlin
DataGuardPlugin.enable {
    updateAll {
        allow {
            tableName = "user_archive"
        }
    }
}

UserArchive()
    .update()
    .set { it.status = "EXPIRED" }
    .execute()
```

允许临时表执行 truncate、drop 和 alter：

```kotlin
import com.kotlinorm.Kronos

DataGuardPlugin.enable {
    truncate { allow { tableName = "tmp_%" } }
    drop { allow { tableName = "tmp_%" } }
    alter { allow { tableName = "tmp_%" } }
}

val wrapper = Kronos.dataSource()

wrapper.table.truncateTable("tmp_session")
wrapper.table.dropTable("tmp_session")
wrapper.table.syncTable(TmpSession())
```

按数据库名和表名匹配：

```kotlin
DataGuardPlugin.enable {
    deleteAll {
        allow {
            databaseName = "kronos"
            tableName = "tmp_%"
        }
    }
}
```

使用 `allowAll()` 默认允许某类操作，再用 `deny { ... }` 拒绝敏感表：

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

关闭 DataGuard：

```kotlin
DataGuardPlugin.disable()
```

---

## Codegen 代码生成

`kronos-codegen` 用于 Database First 项目，从数据库表结构生成 Kotlin `KPojo` 实体类。

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

`config.toml` 中用 `[[table]]` 选择要生成的表：

```toml
[[table]]
name = "tb_user"
className = "User"

[[table]]
name = "tb_order"
className = "Order"
```

输出目录和包名通过 `[output]` 配置：

```toml
[output]
targetDir = "src/main/kotlin/com/example/entity"
packageName = "com.example.entity"
tableCommentLineWords = 80
```

数据源使用 `KronosJdbcWrapper` 时加入 `kronos-jdbc-wrapper`，连接池属性按当前 DataSource 类填写：

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

命名策略和特殊字段策略会影响生成属性名和注解：

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

匹配到的字段会生成对应 Kronos 注解：

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

模板脚本先调用 `init("config.toml")`，再调用 `template { ... }.write()`：

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

运行脚本：

```bash
kotlinc -script example.main.kts
```

成功后会写出配置的实体文件：

```text
File generated successfully: src/main/kotlin/com/example/entity/User.kt
```

---

## 数据库与方言

当前完整内置方言：MySQL、PostgreSQL、SQLite、SQL Server、Oracle。

内置方言对应的 `DBType`：MySQL 为 `DBType.Mysql`，PostgreSQL 为 `DBType.Postgres`，SQLite 为 `DBType.SQLite`，SQL Server 为 `DBType.Mssql`，Oracle 为 `DBType.Oracle`。

`KronosJdbcWrapper` 位于 `com.kotlinorm.wrappers`，接收 `DataSource`。未传 `databaseType` 时，数据库类型由 JDBC metadata 推导：

```kotlin
val wrapper = KronosJdbcWrapper(dataSource)

Kronos.dataSource = { wrapper }

println(wrapper.dbType)
println(wrapper.sqlDialect)
```

metadata 名称需要指定到某个方言时，传入 `databaseType`：

```kotlin
import com.kotlinorm.enums.DBType
import com.kotlinorm.wrappers.KronosJdbcWrapper

val wrapper = KronosJdbcWrapper(
    dataSource = dataSource,
    databaseType = DBType.Mysql
)
```

JDBC statement 设置、SQL warning 处理、自定义参数绑定和结果映射通过构造器配置 block 设置：

```kotlin
import com.kotlinorm.wrappers.KronosArgument
import com.kotlinorm.wrappers.KronosArgumentFactory
import com.kotlinorm.wrappers.KronosJdbcWrapper
import com.kotlinorm.wrappers.KronosPhysicalReadResult
import com.kotlinorm.wrappers.KronosPhysicalValueReader
import com.kotlinorm.wrappers.KronosSqlWarningPolicy
import java.sql.Types
import java.util.UUID

val wrapper = KronosJdbcWrapper(dataSource) {
    statement.fetchSize = 1000
    statement.maxRows = 5000
    statement.queryTimeoutSeconds = 30
    warningPolicy = KronosSqlWarningPolicy.THROW

    arguments.register(KronosArgumentFactory { value, _ ->
        if (value is UUID) {
            KronosArgument { position, statement, _ ->
                statement.setObject(position, value, Types.OTHER)
            }
        } else {
            null
        }
    })

    columnMappers.register(KronosPhysicalValueReader { resultSet, position, _ ->
        val jdbcTypeName = resultSet.metaData.getColumnTypeName(position)
        if (jdbcTypeName.equals("jsonb", ignoreCase = true)) {
            KronosPhysicalReadResult.Handled(resultSet.getString(position))
        } else {
            KronosPhysicalReadResult.NotHandled
        }
    })
}
```

物理 reader 在 `getObject` 前执行，只按 JDBC metadata/vendor 行为判断；处理后的 SQL `NULL` 必须返回 `Handled(null)`，不匹配时返回 `NotHandled`。UUID 等逻辑目标转换通过 `Kronos.registerValueCodec` 注册 `ValueCodec`，不要放进物理 reader。

连接池和 JDBC Driver 推荐使用对应厂商发布的最新稳定版，并按数据库服务端版本和 JDK 选择兼容构件。Kronos 自身依赖示例使用 `0.3.0`。

生产连接检查要覆盖连接池大小、连接/网络/查询/空闲超时、validation query 或 JDBC validation method、SSL/TLS 与证书配置、secret 来源、时区/编码 URL 参数，以及数据库服务端、JDK、认证和 TLS 能力对应的 driver 稳定分支。

常见 JDBC Driver 坐标：

```kotlin
dependencies {
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
    implementation("org.postgresql:postgresql:<latest-stable>")
    implementation("org.xerial:sqlite-jdbc:<latest-stable>")
    implementation("com.microsoft.sqlserver:mssql-jdbc:<latest-stable>")
    implementation("com.oracle.database.jdbc:ojdbc8:<latest-stable>")
}
```

`KronosDataSourceWrapper` 当前接口要点：

```kotlin
val url: String
val userName: String
val dbType: DBType
val sqlDialect: SqlDialect

fun toList(task: KAtomicQueryTask): List<Any?>
fun first(task: KAtomicQueryTask): Any?
fun update(task: KAtomicActionTask): Int
fun batchUpdate(task: KronosAtomicBatchTask): IntArray
fun transact(isolation: TransactionIsolation? = null, timeout: Int? = null, block: TransactionScope.() -> Any?): Any?
```

手写 wrapper 对接命名参数框架时使用 `task.sql` 和 `task.paramMap`；对接顺序参数框架时使用 `task.parsed()` 或 `KronosAtomicBatchTask.parsedArr()`：

```kotlin
val task = KronosAtomicActionTask(
    sql = "UPDATE user SET status = :status WHERE id = :id",
    paramMap = mapOf("status" to "ACTIVE", "id" to 1)
)

val (jdbcSql, params) = task.parsed()

println(jdbcSql)          // UPDATE user SET status = ? WHERE id = ?
println(params.toList())  // [ACTIVE, 1]
```

回答方言行为时，提供 DSL 示例和 SQL 示例：

方言行为矩阵入口包括 pagination、upsert / `onConflict()`、last insert id、DDL / schema sync、`KColumnType` 字段类型渲染、identifier quoting、functions / window functions。普通回答优先链接或引用 docs 中 `database/dialect-support`、`database/table-operations`、`database/schema-sync`、`mapping/column-types`、`query/functions` 和 `query/sorting-pagination-aggregation` 的示例。

```kotlin
User()
    .select { [it.id, it.name] }
    .orderBy { it.id.asc() }
    .page(2, 20)
    .withTotal()
    .build(wrapper)
```

```sql
-- MySQL
SELECT `id`, `name`
FROM `user`
ORDER BY `id` ASC
LIMIT 20 OFFSET 20
```

```sql
-- PostgreSQL / SQLite
SELECT "id", "name"
FROM "user"
ORDER BY "id" ASC
LIMIT 20 OFFSET 20
```

```sql
-- SQL Server
SELECT [id], [name]
FROM [user]
ORDER BY [id] ASC
OFFSET 20 ROWS FETCH NEXT 20 ROWS ONLY
```

```sql
-- Oracle
SELECT "id", "name"
FROM "user"
ORDER BY "id" ASC
OFFSET 20 ROWS FETCH NEXT 20 ROWS ONLY
```

Upsert 按当前方言渲染：

```kotlin
User(id = 1, name = "Ada")
    .upsert { it.name }
    .onConflict()
    .execute()
```

```sql
-- MySQL
ON DUPLICATE KEY UPDATE `name` = :name

-- PostgreSQL / SQLite
ON CONFLICT ("id") DO UPDATE SET "name" = :name

-- SQL Server / Oracle
MERGE INTO ...
```

自增主键插入需要返回数据库生成 ID 时，在本次 insert 上调用 `.withId()`：

```kotlin
val result = User(name = "Kronos")
    .insert()
    .withId()
    .execute()

val affectedRows = result.affectedRows
val lastInsertId = result.lastInsertId
```

```sql
-- MySQL
SELECT LAST_INSERT_ID()

-- PostgreSQL
SELECT LASTVAL()

-- SQLite
SELECT last_insert_rowid()

-- SQL Server
SELECT SCOPE_IDENTITY()

-- Oracle
SELECT MAX("ID") FROM "USER"
```

创建新数据库方言时，至少同步这些入口：

```kotlin
DBType.YourDatabase

val YourDatabaseDialect = SqlDialect(
    leftQuote = "\"",
    rightQuote = "\"",
    limitStyle = SqlLimitStyle.LimitOffset,
    family = SqlDialectFamily.Standard
)

SqlManager.registerDatabase(
    DBType.YourDatabase,
    YourDatabaseDialect,
    YourDatabaseStatements
)
```

新增方言要验证 select、分页、upsert、generated identity ID、DDL、schema sync、函数渲染和 JDBC metadata 识别。

---

## Insert

```kotlin
// 单条插入
val user = User(name = "Kronos", age = 18)
user.insert().execute()

// 获取自增ID
val result = user.insert().withId().execute()
val affectedRows = result.affectedRows
val lastId = result.lastInsertId

// 指定插入字段
user.insert { [it.name, it.age] }.execute()
```

---

## Delete

```kotlin
// 按条件删除
User().delete().where { it.id == 1 }.execute()

// 按对象字段值删除
User(id = 1).delete().where().execute()

// 按指定对象字段值删除
User(id = 1, name = "Kronos").delete().by { it.id }.execute()

```

如果实体配置了 `@LogicDelete`，delete 会自动执行逻辑删除（UPDATE 而非 DELETE）。

---

## Update

```kotlin
// 使用 set DSL 更新，where() 按对象非空字段生成条件
User(id = 1).update().set { it.name = "New Name" }.where().execute()

// 按字段选择器更新（使用对象中的值）
user.update { it.name }.by { it.id }.execute()

// 条件更新
User().update().set { it.age = 20 }.where { it.name == "Kronos" }.execute()

// set 右侧可以使用普通运行时 Kotlin 表达式
fun getName(): String? = null
User(id = 1).update().set { it.name = getName() ?: "匿名" }.where().execute()
```

`by` 用于指定匹配条件字段（取对象中的值），`where` 用于自定义条件表达式。

---

## Select

```kotlin
// 查询单条
val user: User = User(id = 1).select().where().first()

// 查询列表
val users: List<User> = User().select().where { it.age > 18 }.toList()

// 选择特定字段
val name: String = User().select { it.name }.where { it.id == 1 }.first<String>()

// 排序 + 带总数分页；返回 PageResult<User>
val page = User().select()
    .where { it.age > 18 }
    .orderBy { it.age.desc() }
    .page(1, 10)
    .withTotal()
    .toList()
val users = page.records

// having 筛选同层聚合；filter 通过派生查询筛选聚合 alias
val grouped = User().select { [it.age, f.count(it.id).alias("userCount")] }
    .groupBy { it.age }
    .having { f.count(it.id) > 5 }

val result = grouped
    .filter { it.userCount > 10 }
    .toList()

// 去重
User().select { it.name }.distinct().toList()
```

`having` 的 receiver 使用当前聚合层的 Source 表达式；`filter` 的 receiver 只有 `grouped` 的 `Selected` 字段 `age` 和 `userCount`，并始终建立外层派生查询。`filter` 没有无参 query-by-example 形式，必须传入 predicate lambda。

内置函数通过查询 DSL 中的 `f` 访问。函数表达式用于投影结果时要使用 `.alias("name")`，alias 会成为 `toMapList()` 的 Map key 和生成投影属性名。常用数学函数包括 `f.abs(...)`、`f.ceil(...)`、`f.round(...)`、`f.trunc(...)`；向上取整和截断分别使用 `ceil`、`trunc` 这两个函数名。

函数 SQL 由当前数据库方言渲染。回答跨数据库输出时，优先链接或参考 docs 的 `database/dialect-support` 口径。

窗口函数当前入口是 `f.rowNumber()`。使用前导入 `com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber`，再通过 `.over { partitionBy(...); orderBy(...) }` 定义窗口，并给结果设置 alias：

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

窗口 alias 可以在同层 `orderBy` 中使用。谓词需要读取窗口 alias 时，先生成投影，再使用 `filter` 建立派生查询；`filter` 的 receiver 只有当前 `Selected` 字段。

### 结果方法

```kotlin
// Map 列表
val rows: List<Map<String, Any?>> = User()
    .select { [it.id, it.name] }
    .toMapList()

// 类型列表
val users: List<User> = User()
    .select()
    .toList()

// 单行可空
val userOrNull: User? = User()
    .select()
    .where { it.id == 1 }
    .firstOrNull()

// 单列或自定义行类型
val ids: List<Int> = User()
    .select { it.id }
    .toList<Int>()
```

`toMapList()` 返回 `List<Map<String, Any?>>`，等价于 `toList<Map<String, Any?>>()`。`toList()` 返回类型列表。`toMap()` 等价于 `first<Map<String, Any?>>()`，空结果会抛错；`toMapOrNull()` 等价于 `firstOrNull<Map<String, Any?>>()` 或 `first<Map<String, Any?>?>()`，空结果返回 `null`。一般情况下，`firstOrNull<T>()` 等价于 `first<T?>()`。所有结果方法都可以传入 `KronosDataSourceWrapper`。

JDBC typed Map 结果只支持直接声明的 `Map`/`MutableMap`（顶层可空也支持），因为每行实际返回 `LinkedHashMap`；固定、重排泛型或自定义 Map 子类型会被拒绝。key 只能是 `String`、`String?` 或 `*`；`Any`、`Any?`、`*` value 保持 raw，具体 value 类型使用完整 `KType` 转换。

---

## 子查询与投影

`select { ... }` 返回自定义字段列表时，Kronos 会把字段和 alias 暴露为生成投影。直接字段保留字段名，函数、聚合、标量子查询和窗口函数等非直接字段要显式 `.alias("name")`。

显式投影中的 `it` 会展开当前 KPojo 的全部数据库列。`select { it }`、`select { [it] }` 和 `select { listOf(it) }` 都和 `select()` 一样返回源 KPojo 类型；通过 `-` 排除字段，或与 alias/函数/原生 SQL 等其他投影项组合时，才生成投影结果类型：

```kotlin
val allDirect = User().select { it }.toList()
val allInList = User().select { [it] }.toList()
val withoutId = User().select { it - it.id }.toList()
val withoutIdInList = User().select { [it - it.id] }.toList()
val withoutIdAndAge = User().select { it - it.id - it.age }.toList()

val withAlias = User()
    .select { [it, it.id.alias("sourceId")] }
    .toList()

val compactRows = User()
    .select { [it - [it.id, it.age], it.id.alias("sourceId")] }
    .toList()
```

`[]` 可以组织多个投影项。`it - it.id - it.age` 可以链式排除字段；`it - [it.id, it.age]` 可以一次排除多个字段，并且可以作为 `[]` 里的一个投影项继续追加 alias。重复请求名默认报错；opt-in `UnsafeProjectionOverride` 后保留全部值并分配确定性 `_N` 后缀。

显式 alias 如果替换仍存在的同名源字段，必须先使用 `@OptIn(UnsafeProjectionOverride::class)` 明确确认，因为结果值和生成属性的 Kotlin 类型可能改变。先通过 `it - it.name` 移除源字段，再使用同名 alias 恢复时，不需要 opt-in：

```kotlin
import com.kotlinorm.annotations.UnsafeProjectionOverride

@OptIn(UnsafeProjectionOverride::class)
val replacedName = User()
    .select { [it.id, f.length(it.name).alias("name")] }

val restoredName = User()
    .select { [it - it.name, f.length(it.name).alias("name")] }
```

```kotlin
val nameLengths = User()
    .select { [it.id, f.length(it.name).alias("nameLength")] }

val generatedRows = nameLengths.toList()
val firstLength: Int? = generatedRows.first().nameLength

val rows = nameLengths
    .filter { it.nameLength > 8 }
    .toList()
```

`KSelectable<Selected>.filter { ... }` 始终建立派生查询边界，并保持 `Selected` 作为结果类型。上例的 receiver 只暴露 `id` 和 `nameLength`，不暴露未选中的 `User` 字段；它等价于 `nameLengths.select().where { it.nameLength > 8 }`。外层还需要改变输出字段时，继续使用显式 `select { ... }.where { ... }`。

无参 `toList()` / `first()` 会返回编译器生成的投影类型，运行时类名是内部 `KronosSelectResult_...`，使用时按选中的字段和 alias 访问属性。需要在业务代码中命名结果类型时，定义 DTO 并传给 `select(...)`：

```kotlin
data class UserSummary(
    var id: Int? = null,
    var nameLength: Int? = null
) : KPojo

val summaries: List<UserSummary> = User()
    .select(UserSummary::class) {
        [it.id, f.length(it.name).alias("nameLength")]
    }
    .toList()
```

`toMapList()` 返回 `List<Map<String, Any?>>`，Map key 同样来自字段名和 alias。生成投影或 DTO 的属性名必须唯一。

原生 SQL select item 使用字符串表达式，alias 决定 Map key 或生成投影属性名：

```kotlin
val rows = User()
    .select { ["count(*)".alias("total")] }
    .toMapList()

val total = rows.first()["total"]
```

`"count(*)".alias("total")` 会保留 `count(*)` 作为 SQL 表达式，并使用 `total` 作为结果名。需要绑定值时，把参数放在 `where { ... }` 和 `patch(...)` 中。

窗口函数结果也是投影字段。需要过滤窗口排名时，先在第一层 select 中给窗口表达式 alias，再使用 `filter`：

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

投影结果可以作为下一层查询源，也可以作为 CTAS 的列来源：

```kotlin
val activeUsers = User()
    .select { [it.id, it.name, f.count(it.id).alias("orderCount")] }
    .groupBy { [it.id, it.name] }
    .having { f.count(it.id) > 0 }

val page = activeUsers
    .orderBy { it.orderCount.desc() }
    .page(1, 20)
    .withTotal()
    .toList()

val rows = page.records
```

排序使用 `orderBy { ... }`，只限制行数使用 `limit(size)`。`page(pageIndex, pageSize).toList()` 返回当前页记录；需要总数时使用 `page(...).withTotal().toList()`，返回包含 `total`、`records`、`totalPages`、`pageIndex` 和 `pageSize` 的 `PageResult`。

```kotlin
val page = User()
    .select { [it.id, it.name] }
    .where { it.age > 18 }
    .orderBy { it.id.desc() }
    .page(1, 20)
    .withTotal()
    .toList<User>()

val users = page.records
```

游标分页使用 `cursor(pageSize, after)`。第一次省略 `after`，下一次把返回的 `nextCursor` 传回。游标分页要求 `orderBy` 使用已选字段，返回包含 `hasNext`、`nextCursor` 和 `records` 的 `CursorResult`。

```kotlin
val query = User()
    .select { [it.id, it.name] }
    .where { it.age > 18 }
    .orderBy { it.id.asc() }

val firstPage = query
    .cursor(pageSize = 20)
    .toList<User>()

val nextPage = query
    .cursor(pageSize = 20, after = firstPage.nextCursor)
    .toList<User>()
```

`OffsetPageQuery` 仍是有限的 `KSelectable`，可以继续 `select`、join、union 或作为子查询。`TotalPageQuery` 和 `CursorPageQuery` 是执行阶段，不能再进入关系组合。创建 page/cursor view 不会修改基础查询。

标量子查询可以作为 select 字段或 where 比较值使用。

标量子查询选择一个字段并通常配合 `limit(1)`。Kotlin 需要类型提示时，在 `limit(1)` 后添加类型转换，转换只用于 DSL 类型推断。

```kotlin
val users = User()
    .select { user ->
        [
            user.id,
            user.name,
            Order()
                .select { order -> order.status }
                .where { order -> order.userId == user.id }
                .limit(1)
                .alias("lastOrderStatus")
        ]
    }
    .toList()
```

谓词子查询可以用于 `in`、`!in`、`exists`、`!exists` 和 row-value tuple 条件。

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

### 量化比较

使用 `any<T>(query)`、`some<T>(query)` 或 `all<T>(query)` 包装单列子查询，再与比较运算符组合：

```kotlin
val orders = Order()
    .select()
    .where {
        it.status > any<Int>(
            Order()
                .select { order -> order.status }
                .where { order -> order.userId == 27 }
        )
    }
    .toList()
```

`any<T>(query)`、`some<T>(query)` 和 `all<T>(query)` 分别生成 SQL `ANY`、`SOME` 和 `ALL` 比较。

`KSelectable` 可以作为下一层查询源，也可以作为 join source。

```kotlin
val paidOrders = Order()
    .select { [it.userId, it.status] }
    .where { it.status == 1 }

val users = User().join(paidOrders) { user, order ->
    leftJoin { user.id == order.userId }
        .select { [user.id, user.name, order.status] }
}.toList()
```

`KSelectable<Selected>.insert<Target>()` 用于 INSERT SELECT。

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

---

## Upsert

存在则更新，不存在则插入：

```kotlin
// 基本用法：按 on 字段匹配已有记录
user.upsert().on { it.id }.execute()

// 指定更新字段
user.upsert().on { it.id }.set { [it.name, it.age] }.execute()

```

`on` 指定用于匹配已有记录的字段，`set` 指定匹配到记录时要更新的字段。匹配到逻辑删除记录时，upsert 会更新该记录并恢复逻辑删除字段为活动值。

策略字段在 upsert 中按写入路径维护：插入分支初始化 `@CreateTime`、`@UpdateTime`、`@LogicDelete` 和 `@Version`；更新分支刷新 `@UpdateTime`、恢复 `@LogicDelete` 活动值并递增 `@Version`。

`onConflict()` 表示按数据库唯一约束冲突处理：插入记录，命中唯一约束时更新记录，并由 Kronos 按当前方言生成 SQL。省略 `on { ... }` 时，从 KPojo 唯一性元数据推导冲突目标：优先使用有值的主键，其次使用字段值完整的 `@TableIndex(type = "UNIQUE")` / `@TableIndex(method = "UNIQUE")`。需要指定某个业务唯一键，或存在多个候选唯一键时，显式调用 `on { ... }`。

启用策略字段时，`onConflict()` 的插入列包含创建时间、更新时间、逻辑删除和版本字段；冲突更新部分刷新更新时间、恢复逻辑删除活动值并递增版本。

使用 `onConflict()` 时，`patch(...)` 可以为冲突更新提供动态赋值，并把字段加入冲突更新列表：

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
```

`patch(...)` 支持的动态冲突更新值包括 `SqlExpr`、`KronosFunctionExpr`、`Field` 和标量 `KSelectable`：

```kotlin
val countField = User().__columns.single { it.name == "count" }

User(id = 8, name = "seed", count = 5)
    .upsert { it.name }
    .patch("name" to countField)
    .onConflict()
    .execute()

User(id = 1, name = "seed")
    .upsert()
    .patch(
        "name" to Order()
            .select { it.status }
            .where { it.status == 15 }
            .limit(1)
    )
    .onConflict()
    .execute()
```

`patch(...)` 的值在 `onConflict()` 路径中作为冲突更新赋值；普通 upsert 路径中，相同字段进入匹配后的 update set。

---

## 批量操作

同一条 SQL 需要用多组参数执行时，使用 `SqlExecutor.batchExecute` 或直接创建 `KronosAtomicBatchTask`。

```kotlin
import com.kotlinorm.database.SqlExecutor

val affectedRows: IntArray = with(SqlExecutor) {
    wrapper.batchExecute(
        "UPDATE user SET name = :name WHERE id = :id",
        arrayOf(
            mapOf("name" to "Alice", "id" to 1),
            mapOf("name" to "Bob", "id" to 2)
        )
    )
}
```

直接创建任务时，可以指定操作类型：

```kotlin
val task = KronosAtomicBatchTask(
    sql = "INSERT INTO user (name, age) VALUES (:name, :age)",
    paramMapArr = arrayOf(
        mapOf("name" to "Alice", "age" to 18),
        mapOf("name" to "Bob", "age" to 20)
    ),
    operationType = KOperationType.INSERT
)

val affectedRows = wrapper.batchUpdate(task)
```

`KronosAtomicBatchTask.parsedArr()` 可以把命名参数 SQL 转为 JDBC SQL 和每行参数列表。

---

## 逻辑删除与乐观锁

`@LogicDelete` 标记删除字段。逻辑删除字段必须使用 `@Default` 声明数据库中的活动值：MySQL、SQLite、SQL Server 和 Oracle 使用 `@Default("0")`，PostgreSQL 使用 `@Default("false")`。启用后，普通 `delete()` 生成写入删除标记的 `UPDATE`；需要物理删除时使用 `.logic(false)`。

```kotlin
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = null
) : KPojo

User(id = 1).delete().by { it.id }.execute()
// UPDATE ... SET deleted = 1 WHERE id = :id AND deleted = 0

User(id = 1).delete().logic(false).by { it.id }.execute()
// DELETE FROM ... WHERE id = :id
```

普通 upsert 匹配到逻辑删除记录时会更新原行并恢复逻辑删除字段为活动值；`onConflict()` 的冲突更新也会写回活动值。

`@Version` 标记版本字段。insert 会初始化版本字段，update、逻辑删除和 upsert 更新分支会递增版本字段。需要按读取时的版本匹配时，在 `where { ... }` 中显式加入版本条件。

```kotlin
data class Product(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Version
    var version: Int? = null
) : KPojo

Product(id = 1, version = 3)
    .update()
    .set { it.name = "Keyboard Pro" }
    .where { it.id == 1 && it.version == 3 }
    .execute()
```

---

## 条件DSL

Kronos 使用 Kotlin 原生语法构建条件，支持 `where`、`having`、`filter`、`on` 子句。`where` 筛选当前 SQL 层的 Source，`having` 筛选分组后的 source 表达式和聚合结果，`filter` 筛选 `KSelectable<Selected>` 的查询结果并建立外层派生查询：

```kotlin
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber

val rankedOrders = Order()
    .select {
        [
            it.id,
            f.rowNumber()
                .over { orderBy(it.id.asc()) }
                .alias("rn")
        ]
    }
    .filter { it.rn > 1 }
```

`filter` receiver 只暴露 `Selected` 中的 `id` 和 `rn`，不使用 `orderBy` 的更宽 Context，也不暴露未选中的 `Order` 字段。该调用等价于在已选查询上显式调用 `select().where { it.rn > 1 }`。

普通 Source 条件继续使用 `where`：

```kotlin
// select() 只选择当前表；对象值通过 where()/by 进入条件
User(id = 1).select()
// SQL: SELECT ... FROM user

// 空 where() 是 query-by-example，按当前对象可查询非空字段生成等值条件
User(id = 1, name = "A").select().where()
// SQL: WHERE id = 1 AND name = 'A'

// 带 lambda 的 where 由 lambda 生成本次条件
User(id = 1).select().where { it.name == "A" }
// SQL: WHERE name = 'A'

// 多次 where 调用按 AND 追加
User(id = 1).select()
    .where()
    .where { it.name == "A" }
// SQL: WHERE id = 1 AND name = 'A'

// 复杂 OR、分组、函数条件写在同一个 lambda 中
User(id = 1).select()
    .where()
    .where { it.name == "A" || it.age > 18 }
// SQL: WHERE id = 1 AND (name = 'A' OR age > 18)

// 相等和比较
where { it.age == 18 }
where { it.age != 18 }
where { it.age > 18 }
where { it.age >= 18 }
where { it.age < 18 }
where { it.age <= 18 }

// 范围
where { it.age between 1..10 }
where { it.age notBetween 1..17 }

// IN / NOT IN
where { it.id in listOf(1, 2, 3) }
where { it.id !in arrayOf(4, 5, 6) }

// 字符串匹配
where { it.name like "Kronos%" }
where { it.name notLike "%bot%" }
where { it.name.startsWith("Kronos") }
where { it.name.endsWith("ORM") }
where { it.name.contains("ron") }
where { it.name regexp "Kronos.*" }

// NULL 判断
where { it.name == null }
where { it.name != null }
where { it.name.isNull }
where { it.name.notNull }

// 级联关系字段链推荐用安全调用；Kronos 读取字段元数据，不读取运行时对象
where { it.directorId == it.director?.id }
// it.director!!.id 也支持

// 逻辑组合
where { (it.age > 18) && (it.name like "K%") }
where { (it.age > 18) || (it.name == "Kronos") }
where { !(it.age > 18) }

// 使用对象值的无参形式（取对象中对应属性的值）
val user = User(age = 18)
user.select().where { it.age.eq }  // WHERE age = 18
user.select().where { it.age.ge }  // WHERE age >= 18

// 展开对象内非空字段的等值条件
val probe = User(id = 1, name = "Kronos")
probe.select().where { it.eq }

// 排除字段
probe.select().where { (it - it.name).eq }

// 原生 SQL 条件
where { "name = 'Kronos' and age > 18".asSql() }
where { "name = :name".asSql() }.patch("name" to "Kronos")

// 动态空值条件
val age: Int? = null
where { (it.age == age).takeIf(age != null) }
where { (it.status == 0).takeUnless(includeInactive) }
where { if (age != null) { it.age == age } else { false.asSql() } }
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

`select().where()` 在没有可查询非空字段时保留无条件查询。`update().where()` 和 `delete().where()` 没有可查询字段时进入写入安全检查，建议启用 DataGuard 统一拦截全表写入。逻辑删除字段、级联字段、非数据库列和忽略字段不参与空 `where()` 的 query-by-example 条件。对象属性值为 `null` 时不会由空 `where()` 生成 `IS NULL`；需要 SQL NULL 判断时使用 `where { it.field == null }` 或 `where { it.field.isNull }`。动态变量值为 `null` 时仍走无值策略，例如 `where { it.field == value }`。

### 大小写归一化字符串条件

`f.lower(x)` 和 `f.upper(x)` 返回可空 `String?` 条件表达式。它们可以用于相等比较、`contains`、`like`、`startsWith`、`endsWith` 和集合成员条件：

```kotlin
import com.kotlinorm.functions.bundled.exts.StringFunctions.lower
import com.kotlinorm.functions.bundled.exts.StringFunctions.upper

val userName = "ADA"
val normalizedName = userName.lowercase()
val allowedNames = listOf("ada", "grace")

User().select().where {
    f.lower(it.userName) == normalizedName ||
        f.lower(it.userName).contains(normalizedName) ||
        normalizedName in f.lower(it.userName) ||
        f.lower(it.userName) like "ada%" ||
        f.lower(it.userName).startsWith("a") ||
        f.lower(it.userName).endsWith("a") ||
        f.lower(it.userName) in allowedNames
}
```

`f.upper(...)` 使用大写值时也支持相同的条件辅助函数。`in` 支持文本包含、集合成员和单列子查询。

source `String` 字段上的无参 `lowercase()` 和 `uppercase()` 会在条件中生成 SQL `LOWER` 和 `UPPER`，并使用同一组条件运算符：

```kotlin
val normalizedName = "Ada".lowercase()

User().select().where {
    it.userName?.lowercase() == normalizedName ||
        it.userName?.uppercase().contains("AD") ||
        it.userName?.lowercase() like "ada%"
}
```

`normalizedName` 等捕获值由 Kotlin 求值后作为参数绑定。

普通变量和当前 source 字段直接使用即可。需要读取 KPojo 属性实际值时，将 `.value` 放在属性链的末端，例如 `probe.userName.value`。

函数与方言示例见 `query/functions`。

### Iterable 集合条件

在条件 lambda 中，Kotlin 标准库 `Iterable.any`、`Iterable.all` 和 `Iterable.none` 会为集合中的每个元素构建条件：

```kotlin
val keywords = listOf("Ada", "Grace")
val minimumAges = listOf(18, 21)
val excludedFragments = listOf("test", "archive")

User().select().where {
    keywords.any { keyword -> it.name.contains(keyword) } &&
        minimumAges.all { minimumAge -> it.age >= minimumAge } &&
        excludedFragments.none { fragment -> it.name.contains(fragment) }
}
```

非空集合中，`any` 用 OR 组合子条件，`all` 用 AND 组合子条件，`none` 用 AND 组合每个子条件的否定。

`!any`、`!all` 和 `!none` 分别使用 AND NOT、OR NOT 和 OR 形式。

空集合或所有子条件均无值时，这三种调用和外层 `!` 都生成 `FALSE` 条件。`select`、`update` 和 `delete` 会保留该谓词。

---

## Join

JOIN block 的 source 参数只声明一次。每个右侧 source 需要一个明确的关系方法；`innerJoin`、`leftJoin`、`rightJoin`、`fullJoin` 接收条件，`crossJoin()` 不接收条件。关系方法返回 `JoinSource`，调用 `select` 后才得到可执行、可派生的 `JoinedSelectQuery`。

```kotlin
// 内连接
val result = User().join(Order()) { user, order ->
    innerJoin { user.id == order.userId }
        .select { [user.name, order.amount] }
        .where { user.age > 18 }
}.toList()

// 左连接
User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId }
        .select { [user.name, order.amount] }
}.toList()

// 多表连接
User().join(Order(), Product()) { user, order, product ->
    innerJoin { user.id == order.userId }
        .leftJoin { order.productId == product.id }
        .select { [user.name, product.name, order.amount] }
}.toList()

// 分页：select 后进入分页阶段
val query = User().join(Order()) { user, order ->
    leftJoin { user.id == order.userId }
        .select { [user.name, order.amount] }
}
val page = query.page(1, 20).withTotal().toList()
```

内层 block 只返回关系方法时会保留原始 `JoinSource`，可用于 `A JOIN (B JOIN C)`。JOIN `select` 返回 `KSelectable<Selected>`，可继续作为派生 source、join source 或 union operand。重复输出名和 Context 遮蔽遵循 `UnsafeProjectionOverride` 规则；不同业务含义优先使用显式 alias。

---

## 事务

```kotlin
// 基本事务
Kronos.transact {
    user1.insert().execute()
    user2.update().set { it.name = "test" }.by { it.id }.execute()
    // 抛出异常自动回滚
}

// 指定数据源的事务
Kronos.transact(wrapper) {
    user.insert().execute()
}
```

`Kronos.transact(wrapper = null, isolation = null, timeout = null) { ... }` 返回 block 的结果。`KronosJdbcWrapper` 中嵌套事务复用当前线程上的事务连接，外层事务负责最终提交或回滚。`TransactionIsolation` 可设置为 `READ_UNCOMMITTED`、`READ_COMMITTED`、`REPEATABLE_READ`、`SERIALIZABLE`，`timeout` 单位为秒。

`TransactionScope` 提供 savepoint 操作。savepoint 需要 wrapper 在执行 block 时传入当前事务 JDBC connection；`KronosJdbcWrapper` 已提供该 connection。

```kotlin
Kronos.transact(isolation = TransactionIsolation.READ_COMMITTED, timeout = 30) {
    val point = savepoint("before_status_update")
    try {
        user.update().set { it.name = "test" }.by { it.id }.execute()
        releaseSavepoint(point)
    } catch (e: Exception) {
        rollbackToSavepoint(point)
        throw e
    }
}
```

---

## 表操作

```kotlin
val dataSource = Kronos.dataSource()

// 检查表是否存在
val existsByName = dataSource.table.exists("user")
val existsByModel = dataSource.table.exists<User>()

// 创建表（如果不存在）
dataSource.table.createTable(User())
dataSource.table.createTable<User>()

// CREATE TABLE AS SELECT，表结构来自查询输出
val activeOrders = Order()
    .select { [it.id, it.userId, it.status] }
    .where { it.status == 1 }

dataSource.table.createTable(OrderArchive(), activeOrders)

// 同步表结构（自动添加/修改列、索引）
val tableAlreadyExisted = dataSource.table.syncTable(User())

// 删除表
dataSource.table.dropTable(User())

// 清空表；PostgreSQL/SQLite 可用 restartIdentity 控制自增重置
dataSource.table.truncateTable("tmp_session", restartIdentity = false)
```

`createTable(instance)` 使用 KPojo 的列、主键、默认值、注释和 `@TableIndex` 元数据建表。`createTable(target, query)` 是 CTAS，使用查询输出决定表形态；需要索引、注释、默认值或主键等 KPojo schema 元数据时，先普通建表，再用 `KSelectable.insert<Target>()` 写入数据。

`syncTable` 会对比当前数据类定义与数据库表结构的差异，执行 ALTER TABLE 添加/修改/删除字段并同步索引。表不存在时会创建表并返回 `false`；表已存在时执行差异同步并返回 `true`。

CTAS 可以先构建任务查看 SQL：

```kotlin
val task = dataSource.table.buildCreateTableAsSelectTask(OrderArchive(), activeOrders)
val (sql, params) = task

println(sql)
println(params)
```

普通 DDL 也可以先构建 statement 或 task 做 dry-run：

```kotlin
val table = dataSource.table

val createStatement = table.buildCreateTableStatement(User())
val dropStatement = table.buildDropTableStatement("user", ifExists = true)
val truncateStatement = table.buildTruncateTableStatement("user", restartIdentity = false)

val dropTask = table.buildDropTableTask("user")
val truncateTask = table.buildTruncateTableTask("user", restartIdentity = false)
val (sql, params, atomicTasks) = truncateTask
```

`dropTable`、`truncateTable` 和 `syncTable` 属于 DDL/维护操作，生产环境建议配合 DataGuard 或发布流程审批。

---

## 故障排查入口

- 依赖坐标无法解析：检查 `com.kotlinorm:kronos-core:0.3.0`、`com.kotlinorm:kronos-jdbc-wrapper:0.3.0` 和数据库 driver 的当前稳定版。
- 编译插件未生效：编译声明 `KPojo` 或 Kronos DSL 的模块，确认输出包含 `[Kronos] Kronos compiler plugin K2 initialized`；每个相关 source set 都要启用 Gradle 或 Maven 插件。
- 检查 KPojo generated members：`__kType`、`__tableName`、`__tableComment`、`__columns`、`__tableIndexes`、`__createTime`、`__updateTime`、`__logicDelete`、`__optimisticLock` 和 `toDataMap()` 依赖编译插件生成；出现 `__tableName must be overridden by the compiler plugin` 时检查 `configuration/compiler-plugins`。
- projection alias / 标量子查询诊断：函数、聚合、窗口函数、原生 SQL 和标量子查询 select item 使用 `.alias("name")`；标量子查询作为值时选择一个字段并使用 `.limit(1)`。
- 数据源未配置：设置 `Kronos.dataSource = { wrapper }`，或在结果方法中传入具体 `KronosDataSourceWrapper`。
- 方言输出不符合预期：打印 `wrapper.dbType` 与 `wrapper.sqlDialect`。
- `where()` 没有生成条件：`select()` 本身不读取对象值，空 `where()` 才执行 query-by-example；lambda `where { ... }` 由 lambda 决定本次条件。
- DataGuard 拦截：为写入加条件，或对维护表配置 allow 规则。
- 需要查看 SQL：优先使用页面中对应的 `build...Task` API，例如 `buildCreateTableAsSelectTask()`。

---

更多高级用法（级联操作、内置函数、自定义函数、多租户等）请参阅 `references/advanced.md`。
