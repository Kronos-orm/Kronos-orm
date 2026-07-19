# Kronos ORM 注解参考

## @Table

指定实体类对应的数据库表名。不使用此注解时，表名由 `tableNamingStrategy` 自动转换。

```kotlin
@Target(AnnotationTarget.CLASS)
annotation class Table(val name: String)
```

```kotlin
@Table("tb_user")
data class User(val id: Int? = null) : KPojo
```

KPojo 类本身不能声明类级泛型参数。为 KPojo 使用具体属性类型；否则编译时会报告 `KRONOS_GENERIC_KPOJO_NOT_SUPPORTED`。普通非 KPojo 泛型类仍然可以使用，非泛型 KPojo 中的 `List<String>` 等具体泛型属性也可以使用。

## @PrimaryKey

标记主键字段。不设置参数时为手动主键；设置 `identity`、`uuid`、`snowflake` 或 `custom` 时，字段分别映射为对应的 `PrimaryKeyType`。

```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class PrimaryKey(
    val identity: Boolean = false,
    val uuid: Boolean = false,
    val snowflake: Boolean = false,
    val custom: Boolean = false
)
```

| 写法 | PrimaryKeyType | 插入行为 |
|------|----------------|----------|
| `@PrimaryKey` | `DEFAULT` | 使用 KPojo 上已有的字段值 |
| `@PrimaryKey(identity = true)` | `IDENTITY` | 主键值为 `null` 时排除该列，由数据库生成 |
| `@PrimaryKey(uuid = true)` | `UUID` | 插入前写入 `UUIDGenerator.nextId()` |
| `@PrimaryKey(snowflake = true)` | `SNOWFLAKE` | 插入前写入 `SnowflakeIdGenerator.nextId()` |
| `@PrimaryKey(custom = true)` | `CUSTOM` | 插入前写入 `customIdGenerator?.nextId()` |

```kotlin
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null
) : KPojo
```

```kotlin
data class UuidUser(
    @PrimaryKey(uuid = true)
    var id: String? = null
) : KPojo
```

```kotlin
import com.kotlinorm.beans.generator.customIdGenerator
import com.kotlinorm.interfaces.KIdGenerator

object TicketIdGenerator : KIdGenerator<String> {
    override fun nextId(): String = "ticket-${System.currentTimeMillis()}"
}

fun configureTicketIds() {
    customIdGenerator = TicketIdGenerator
}
```

`identity` 推荐用于 `Int` 或 `Long` 字段，`uuid` 推荐用于 `String` 字段，`snowflake` 推荐用于 `Long` 字段，`custom` 字段类型应和生成器返回值一致。插入 `custom` 主键前必须设置 `customIdGenerator`，否则主键参数为 `null`。

## @Column

指定列名。

```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class Column(val name: String)
```

```kotlin
data class User(
    @Column("user_name")
    var name: String? = null
) : KPojo
```

## @ColumnType

显式指定列的数据库类型。没有此注解时，编译器插件按 Kotlin 属性类型推断 `KColumnType`；有此注解时，`type`、`length`、`scale` 会进入 `__columns`，并由表操作按当前数据库方言渲染 DDL。

```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class ColumnType(
    val type: KColumnType,
    val length: Int = 0,
    val scale: Int = 0
)
```

```kotlin
@ColumnType(KColumnType.VARCHAR, length = 80)
var title: String? = null

@ColumnType(KColumnType.DECIMAL, length = 12, scale = 2)
var amount: java.math.BigDecimal? = null

@ColumnType(KColumnType.TEXT)
var description: String? = null

@Serialize
@ColumnType(KColumnType.JSON)
var payload: Map<String, Any?>? = null
```

常见渲染结果：

| 设置 | MySQL | PostgreSQL | SQLite | SQL Server | Oracle |
|------|-------|------------|--------|------------|--------|
| `VARCHAR, length = 80` | `VARCHAR(80)` | `VARCHAR(80)` | `TEXT` | `VARCHAR(80)` | `VARCHAR2(80)` |
| `DECIMAL, length = 12, scale = 2` | `DECIMAL(12,2)` | `DECIMAL(12,2)` | `NUMERIC` | `DECIMAL(12,2)` | `NUMBER(12,2)` |
| `JSON` | `JSON` | `JSONB` | `TEXT` | `JSON` | `JSON` |

## @Default

指定列的默认值。

```kotlin
@Default("0")
var score: Int? = null

@Default("CURRENT_TIMESTAMP")
var createTime: String? = null
```

`@Default` 是原生数据库默认值表达式，不会在数据库之间自动转换。常用类型的推荐写法：

| Kotlin 类型 / `KColumnType` | MySQL | PostgreSQL | SQLite | SQL Server | Oracle |
|---|---|---|---|---|---|
| `Boolean` / `BIT` | `0` / `1` | `false` / `true` | `0` / `1` | `0` / `1` | `0` / `1` |
| `Byte`、`Short`、`Int`、`Long` / 整数 | `0` / `1` | `0` / `1` | `0` / `1` | `0` / `1` | `0` / `1` |
| `Float`、`Double`、`BigDecimal` / 数值 | `0.0` / `1.5` | `0.0` / `1.5` | `0.0` / `1.5` | `0.0` / `1.5` | `0.0` / `1.5` |
| `Char`、`String` / 字符串 | `'text'` | `'text'` | `'text'` | `'text'` | `'text'` |
| `Date`、`LocalDate` / `DATE` | `'2026-01-02'` | `DATE '2026-01-02'` | `'2026-01-02'` | `CONVERT(date, '2026-01-02')` | `DATE '2026-01-02'` |
| `LocalTime` / `TIME` | `'12:34:56'` | `TIME '12:34:56'` | `'12:34:56'` | `CONVERT(time, '12:34:56')` | `TIMESTAMP '1970-01-01 12:34:56'` |
| `LocalDateTime` / `DATETIME` | `'2026-01-02 03:04:05'` | `TIMESTAMP '2026-01-02 03:04:05'` | `'2026-01-02 03:04:05'` | `CONVERT(datetime2, '2026-01-02T03:04:05')` | `TIMESTAMP '2026-01-02 03:04:05'` |
| `java.sql.Timestamp` / `TIMESTAMP` | `'2026-01-02 03:04:05'` | `TIMESTAMP '2026-01-02 03:04:05'` | `'2026-01-02 03:04:05'` | 不支持，SQL Server 将其映射为 `rowversion` | `TIMESTAMP '2026-01-02 03:04:05'` |
| `ByteArray` / `BLOB` | `NULL` | `NULL` | `NULL` | `NULL` | `NULL` |

PostgreSQL 的 Boolean 列不接受数字默认值 `0/1`，请使用 `@Default("false")` 或 `@Default("true")`。数据库函数（如 `CURRENT_TIMESTAMP`）也必须使用目标数据库支持的原生语法。非空二进制默认值应放在方言专用迁移脚本中。

## @LogicDelete

标记逻辑删除字段。逻辑删除字段必须使用 `@Default` 声明数据库中的活动值：MySQL、SQLite、SQL Server 和 Oracle 使用 `@Default("0")`，PostgreSQL 使用 `@Default("false")`。启用后 delete 操作变为 UPDATE（设置标记值），select 自动过滤已删除记录。普通 upsert 匹配到已逻辑删除记录时会更新原行并恢复活动值；`onConflict()` upsert 会在插入列和冲突更新赋值中维护活动值。

```kotlin
// 属性级别
data class User(
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = false
) : KPojo

// 类级别（使用全局策略中配置的字段）
@LogicDelete
data class User(var id: Int? = null) : KPojo
```

## @UnsafeProjectionOverride

确认 projection 中的重复 Selected 输出名，或同层 `orderBy` Context 读取被 Selected 值遮蔽的 Source 名称。这个 marker 使用 Kotlin 标准 `@RequiresOptIn(Level.ERROR)`；可以在表达式、函数、类、文件或编译器范围使用 `@OptIn`。

重复输出名会保留全部值：第一次出现使用原名，后续值使用 `_1`、`_2` 等确定性后缀。显式请求名会先全局保留，因此 `id, id, id_1` 解析为 `id, id_2, id_1`。不同业务含义优先使用显式 alias。

```kotlin
import com.kotlinorm.annotations.UnsafeProjectionOverride

@OptIn(UnsafeProjectionOverride::class)
val query = User()
    .select { [it.id, it.id, it.name.alias("id_1")] }
```

Selected alias 复用 Source 字段名本身不要求 opt-in；只有同层 `orderBy` 读取冲突 Context 名称时才要求。`where`、`groupBy` 和 `having` 继续读取 Source。先通过 `it - it.name` 移除 Source 字段再恢复同名 alias，也不需要 opt-in。

## @Version

标记乐观锁版本字段。insert 会初始化版本号，update、逻辑删除和 upsert 更新分支会递增版本号。需要按读取时的版本匹配时，在 `where { ... }` 中显式加入版本条件。

```kotlin
data class User(
    @Version
    var version: Int? = null
) : KPojo
```

## @CreateTime / @UpdateTime

自动填充创建时间和更新时间。

```kotlin
data class User(
    @CreateTime
    var createTime: LocalDateTime? = null,
    @UpdateTime
    var updateTime: LocalDateTime? = null
) : KPojo
```

支持的时间类型：`LocalDateTime`、`Date`、`String`（配合 `@DateTimeFormat`）。

## @DateTimeFormat

指定时间字段的格式化模式（当字段类型为 String 时）。

```kotlin
@UpdateTime
@DateTimeFormat("yyyy-MM-dd HH:mm:ss")
var updateTime: String? = null
```

## @Cascade

定义级联关系。`properties` 是当前实体的外键字段，`targetProperties` 是目标实体的关联字段。

```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class Cascade(
    val properties: Array<String>,
    val targetProperties: Array<String>,
    val onDelete: CascadeDeleteAction = CascadeDeleteAction.NO_ACTION,
    val defaultValue: Array<String> = [],
    val usage: Array<KOperationType> = [
        KOperationType.INSERT,
        KOperationType.UPDATE,
        KOperationType.DELETE,
        KOperationType.SELECT,
        KOperationType.UPSERT
    ]
)
```

一对一：
```kotlin
data class Child(
    var parentId: Long? = null,
    @Cascade(["parentId"], ["id"])
    var parent: Parent? = null
) : KPojo
```

一对多：
```kotlin
data class Parent(
    var id: Long? = null,
    @Cascade(["id"], ["parentId"])
    var children: List<Child>? = null
) : KPojo
```

多对多（通过中间表）：
```kotlin
data class Student(
    var id: Long? = null,
    @Cascade(["id"], ["studentId"])
    var enrollments: List<Enrollment>? = null
) : KPojo

data class Enrollment(
    var studentId: Long? = null,
    var courseId: Long? = null,
    @Cascade(["courseId"], ["id"])
    var course: Course? = null
) : KPojo
```

## @TableIndex

定义表索引，可重复使用。

```kotlin
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class TableIndex(
    val name: String,
    val columns: Array<String>,
    val type: String = "",
    val method: String = "",
    val concurrently: Boolean = false
)
```

```kotlin
@TableIndex("idx_name", ["name"], "UNIQUE", "BTREE")
@TableIndex("idx_age", ["age"])
data class User(...) : KPojo
```

`type = "UNIQUE"` 标记唯一索引；`method = "BTREE"` 等值会在支持的方言中渲染为索引方法；`concurrently = true` 面向 PostgreSQL，并发索引语句会放在事务性 DDL 批次之外执行。单列索引使用类级别 `@TableIndex(columns = ["name"])`。

## @Serialize

标记需要 JSON 序列化存储的复杂类型字段。

```kotlin
data class User(
    @Serialize
    var tags: List<String>? = null,
    @Serialize
    var nestedTags: List<List<String>>? = null,
    @Serialize
    var profile: Profile? = null
) : KPojo
```

需要通过 `Kronos.serializeProcessor = ...` 配置序列化处理器（如 `GsonProcessor`、`JacksonProcessor`、Kotlinx Serialization 处理器）。处理器会收到字段声明 `KType`，可用于保留集合和嵌套集合的泛型信息。

## @Ignore

忽略字段，可指定忽略的操作类型。默认 `@Ignore` 等价于 `@Ignore([IgnoreAction.ALL])`。`@Ignore` 优先级低于显式的 `cascade { [Entity::relation] }` 引用选择 DSL。

```kotlin
data class User(
    @Ignore
    var tempField: String? = null
) : KPojo
```

常用目标：

- `IgnoreAction.ALL`：属性不是数据库列。
- `IgnoreAction.TO_MAP`：`toDataMap()` 跳过该属性。
- `IgnoreAction.FROM_MAP`：`fromMapData()` 和 `safeFromMapData()` 跳过该属性。
- `IgnoreAction.SELECT`：默认 `select()` 字段列表跳过该属性。
- `IgnoreAction.CASCADE_SELECT`：级联查询跳过该关系属性。

空 `where()` 的 query-by-example 条件也会排除逻辑删除字段、级联字段、非数据库列和被忽略字段。

## @NonNull

标记字段为必填，DDL 中生成 `NOT NULL`。

```kotlin
data class User(
    @NonNull
    var name: String? = null
) : KPojo
```
