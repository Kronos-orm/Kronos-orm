{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.annotation("Table") }} 表名设置

用于指定数据类的表名，该注解的生效优先级高于{{ $.keyword("configuration/global-config", ["全局设置", "全局表名策略"]) }}的优先级。

**参数**：
{{$.params([['name', '表名', 'String']])}}

```kotlin
@Table("tb_user")
data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

带 DDL 形态的表与列映射示例见 {{ $.keyword("mapping/table-and-column", ["表与列"]) }}。

## {{ $.annotation("TableIndex") }}表索引

用于指定数据表的索引。

> **Note**
> 在使用`wrapper.table.createTable(instance)`、`wrapper.table.createTable<T>()`、`wrapper.table.syncTable(instance)`或`wrapper.table.syncTable<T>()`时生效。

**参数**：
{{$.params([
  ['name', '索引名', 'String'],
  ['columns', '索引列名', 'Array<String>'],
  ['type', $.noun("table-index", "索引类型"), 'String', '""'],
  ['method', $.noun("table-index", "索引方法"), 'String', '""'],
  ['concurrently', '是否并发创建索引，<b>仅适用于 PostgreSQL</b>', 'Boolean', false]
])}}

单列索引使用`@TableIndex(columns = [...])`，并传入一个列名。

```kotlin group="TableIndex" name="kotlin" icon="kotlin"
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
@TableIndex(
    name = "idx_name_create_time",
    columns = ["name", "create_time"],
    type = "UNIQUE",
    method = "BTREE"
)
data class User(
    val id: Int? = null,
    val name: String? = null,
    @Column("create_time")
    val createTime: String? = null
) : KPojo
```

```sql group="TableIndex" name="Mysql" icon="mysql"
CREATE UNIQUE INDEX `idx_name_create_time` ON `tb_user` (`name`, `create_time`) USING BTREE
```

`type`、`method`、`concurrently`、`KTableIndex`和方言表现见 {{ $.keyword("mapping/indexes", ["索引类型和索引方法"]) }}。

## {{ $.annotation("CreateTime") }}表创建时间

用于指定数据表是否开启创建时间策略，该注解的生效优先级高于{{ $.keyword("configuration/global-config", ["全局设置", "创建时间策略"]) }}的优先级。

**参数**：

{{$.params([['enable', '是否开启', 'Boolean', true]])}}

```kotlin
// 在全局开启创建时间的情况下取消某张表的创建时间功能
Kronos.createTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("create_time", "createTime"))

@CreateTime(enable = false)
data class User(
    val id: Int? = null,
    val createTime: String? = null // 或者直接删除该字段，禁用创建时间功能
) : KPojo

// 在全局关闭创建时间的情况下开启某张表的创建时间功能
Kronos.createTimeStrategy = KronosCommonStrategy(enabled = false, field = Field("create_time", "createTime"))

@CreateTime
data class User(
  val id: Int? = null,
  val createTime: String? = null // 创建时间字段
) : KPojo
```

## {{ $.annotation("UpdateTime") }}表更新时间

用于指定数据表是否开启更新时间策略，该注解的生效优先级高于{{ $.keyword("configuration/global-config", ["全局设置", "更新时间策略"]) }}的优先级。

**参数**：

{{$.params([['enable', '是否开启', 'Boolean', true]])}}

```kotlin
// 在全局开启更新时间的情况下取消某张表的更新时间功能
Kronos.updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))

@UpdateTime(enable = false)
data class User(
  val id: Int? = null,
  val updateTime: String? = null // 或者直接删除该字段，禁用更新时间功能
) : KPojo

// 在全局关闭更新时间的情况下开启某张表的更新时间功能
Kronos.updateTimeStrategy = KronosCommonStrategy(enabled = false, field = Field("update_time", "updateTime"))

@UpdateTime
data class User(
    val id: Int? = null,
    val updateTime: String? = null // 更新时间字段
) : KPojo
```

## {{ $.annotation("LogicDelete") }}表逻辑删除

用于指定数据表是否开启逻辑删除策略，该注解的生效优先级高于{{ $.keyword("configuration/global-config", ["全局设置", "逻辑删除策略"]) }}的优先级。

**参数**：

{{$.params([['enable', '是否开启', 'Boolean', true]])}}

```kotlin
// 在全局开启逻辑删除的情况下取消某张表的逻辑删除功能
Kronos.logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))

@LogicDelete(enable = false)
data class User(
    val id: Int? = null,
    @Default("0") // @Default("false") for Postgres
    val deleted: Boolean? = null // 或者直接删除该字段，禁用逻辑删除功能
) : KPojo


// 在全局关闭逻辑删除的情况下开启某张表的逻辑删除功能
Kronos.logicDeleteStrategy = KronosCommonStrategy(enabled = false, field = Field("deleted"))

@LogicDelete
data class User(
  val id: Int? = null,
  @Default("0") // @Default("false") for Postgres
  val deleted: Boolean? = null // 逻辑删除字段
) : KPojo
```

## {{ $.annotation("Column") }}列名

用于指定数据表的列名，该注解的生效优先级高于{{ $.keyword("configuration/global-config", ["全局设置", "全局列名策略"]) }}的优先级。

**参数**：

{{$.params([['name', 'String', '列名']])}}

```kotlin
data class User(
    @Column("user_name")
    val name: String? = null
) : KPojo
```

## {{ $.annotation("DateTimeFormat") }}日期时间格式

用于指定数据表的日期/时间格式，该注解的生效优先级高于{{ $.keyword("configuration/global-config", ["全局设置", "默认日期时间格式"]) }}的优先级。

**参数**：

{{$.params([['pattern', '日期/时间格式', 'String']])}}

```kotlin
data class User(
    @DateTimeFormat("yyyy-MM-dd")
    val birthday: String? = null
) : KPojo
```

## {{ $.annotation("Serialize") }}序列化存储

用于标记属性采用 serialized storage。`@Serialize` 只选择 `ValueStorage.SERIALIZED`，不会安装序列化器或第二套处理流程。应用按自己的文本格式，通过 `Kronos.registerValueCodec` 注册一次 `serializedValueCodec`；codec 在两个方向都会收到属性的完整 `KType`。

```kotlin
data class User(
    @Serialize
    val info: List<String>? = emptyList()
) : KPojo
```
序列化字段使用和 codec 注册方式见 {{ $.keyword("mapping/serialization", ["序列化存储"]) }}。

## {{ $.annotation("Cascade") }}级联关系声明

此注解用于声明列的级联关系，详细定义及用法请参考{{ $.keyword("advanced/cascade", ["进阶用法","级联关系定义"]) }}。

> **Note**
> 请将此注解加在类型为`KPojo`或`Collection<KPojo>`的属性上，确保当前定义的表内有关联字段（如以下例子中的companyId）。

**参数**：
{{ $.params([
  ['properties', '本表的关联字段属性名', 'Array<String>'],
  ['targetProperties', '关联目标表关联字段属性名', 'Array<String>'],
  ['onDelete', $.keyword("advanced/cascade-delete-action", ["关联删除策略"]), 'CascadeDeleteAction', 'NO_ACTION'],
  ['defaultValue', '指定级联删除方式为"SET DEFAULT"时设置的默认值（可选）', 'Array<String>', '[]'],
  ['usage', '用于声明本实体需要用到的关联操作', 'Array<KOperationType>', '[Insert, Update, Delete, Upsert, Select]']
])}}

```kotlin
// 一对多级联关系示例
@Table("tb_user")
data class Employee(
    val id: Int? = null,
    val companyId: Int? = null,
    @Cascade(["companyId"], ["id"], SET_DEFAULT, ["0"])
    val company: Company? = null
) : KPojo

@Table("tb_company")
data class Company(
    val id: Int? = null,
    val employees: List<Employee>? = null
) : KPojo
```

一对一、一对多和多字段关系示例见 {{ $.keyword("mapping/cascade-mapping", ["级联映射"]) }}。

## {{ $.annotation("Ignore") }} 忽略字段

此注解用于在部分操作中忽略字段。默认 `@Ignore` 等价于 `@Ignore([IgnoreAction.ALL])`。

**参数**：

{{$.params([['targets', '忽略操作列表', 'Array<IgnoreAction>', '[IgnoreAction.ALL]']])}}

`IgnoreAction.ALL` 会将属性排除为非数据库列。

```kotlin group="Ignore 1" name="all" icon="kotlin"
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore
    var displayName: String? = null
) : KPojo
```

`IgnoreAction.TO_MAP` 会让 `KPojo.toDataMap()` 跳过该属性。

```kotlin group="Ignore 2" name="to map" icon="kotlin"
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore([IgnoreAction.TO_MAP])
    var temporaryToken: String? = null
) : KPojo

val data = User(id = 1, temporaryToken = "token").toDataMap()
```

`IgnoreAction.FROM_MAP` 会让 `KPojo.fromMapData()` 和 `KPojo.safeFromMapData()` 跳过该属性。

```kotlin group="Ignore 3" name="from map" icon="kotlin"
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore([IgnoreAction.FROM_MAP])
    var localState: String? = null
) : KPojo

val user = User().fromMapData<User>(mapOf("id" to 1, "localState" to "draft"))
```

`IgnoreAction.SELECT` 会让 `select()` 的默认字段列表跳过该属性。

```kotlin group="Ignore 4" name="select" icon="kotlin"
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore([IgnoreAction.SELECT])
    var name: String? = null
) : KPojo
```

```sql group="Ignore 4" name="select sql" icon="mysql"
SELECT `id`
FROM `tb_user`
```

`IgnoreAction.CASCADE_SELECT` 会让级联查询跳过该关系属性。

```kotlin group="Ignore 5" name="cascade select" icon="kotlin"
@Table("tb_user")
data class Employee(
  val id: Int? = null,
  val companyId: Int? = null,
  @Cascade(["companyId"], ["id"], SET_DEFAULT, ["0"])
  val company: Company? = null
) : KPojo

@Table("tb_company")
data class Company(
  val id: Int? = null,
  @Ignore([IgnoreAction.CASCADE_SELECT])
  val employees: List<Employee>? = null
) : KPojo
```

> **Note**
> 请注意，此注解是单向的，即给`employees`加上`@Ignore([CASCADE_SELECT])`注解，`employees`不会被级联查询，但是`company`会被级联查询。

## {{ $.annotation("PrimaryKey") }}列主键设置

此注解用于声明列为主键。

**参数**：

{{$.params([
['identity', '是否自增', 'Boolean'],
['uuid', '是否使用uuid作为主键', 'Boolean'],
['snowflake', '是否使用雪花算法作为主键', 'Boolean'],
['custom', '是否使用自定义主键生成器', 'Boolean']
])}}

```kotlin
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    val id: Int? = null
) : KPojo
```

> **Note**
> 使用**自增主键**时，请保证`id`列的类型为`Int`或`Long`类型，否则无法正确生成主键。

> **Note**
> 使用**uuid**作为主键时，请保证`id`列的类型为`String`类型，否则无法正确生成主键。

> **Note**
> 使用**雪花算法**时，自增字段必须为`Long`类型，否则无法正确生成主键，请您在配置文件中配置`datacenterId`和`workerId`，参考：{{ $.keyword("configuration/global-config", ["主键生成器", "雪花算法"]) }}。

> **Note**
> 使用**自定义主键**时，请提前设置配置自定义主键生成器`customIdGenerator`(`KIdGenerator<T>`)，参考：{{ $.keyword("configuration/global-config", ["主键生成器", "自定义主键生成器"]) }}。

## {{ $.annotation("ColumnType") }}列类型及长度

对于不同的数据库类型，kronos会根据kotlin类型自动转换类型，您可以参考{{ $.keyword("mapping/column-types", ["Kotlin列类型推断"]) }}
查看Kotlin数据类型在各个数据库中的映射关系。
您可以通过此注解声明列类型及长度，如果不指定则使用默认的类型及长度，全部类型信息请参考：{{ $.keyword("mapping/column-type-reference", ["Kronos列类型"]) }}。

**参数**：

{{$.params([
  ['type', '类型', 'KColumnType'],
  ['length', '长度', 'Int', '根据类型默认'],
  ['scale', '小数位数', 'Int', '0']
])}}

```kotlin group="ColumnType" name="kotlin" icon="kotlin"
@Table("tb_user")
data class User(
    @ColumnType(KColumnType.CHAR, 10)
    val name: String? = null
) : KPojo
```

## {{ $.annotation("Default") }}列默认值

此注解用于声明列的数据库默认值。

**参数**：

{{$.params([['value', '默认 SQL 值', 'String']])}}

```kotlin group="Default" name="kotlin" icon="kotlin"
@Table("tb_user")
data class User(
    @Default("0")
    val score: Int? = null
) : KPojo
```

`@Default` 会把内容原样写入表结构 DDL，参数不是 Kotlin 值，Kronos 也不会自动把一种数据库方言的表达式转换成另一种方言。下面按自动推断出的 Kotlin 类型列出推荐的默认值字面量：

| Kotlin 类型 / 推断出的 `KColumnType` | MySQL | PostgreSQL | SQLite | SQLServer | Oracle |
|--------------------------------------|-------|------------|--------|-----------|--------|
| `Boolean` / `BIT` | `0` / `1` | `false` / `true` | `0` / `1` | `0` / `1` | `0` / `1` |
| `Byte`、`Short`、`Int`、`Long` / 整数类型 | `0` / `1` | `0` / `1` | `0` / `1` | `0` / `1` | `0` / `1` |
| `Float`、`Double`、`BigDecimal` / 数值类型 | `0.0` / `1.5` | `0.0` / `1.5` | `0.0` / `1.5` | `0.0` / `1.5` | `0.0` / `1.5` |
| `Char`、`String`、其他映射为 `VARCHAR` 的类型 | `'text'` | `'text'` | `'text'` | `'text'` | `'text'` |
| `Date`、`LocalDate` / `DATE` | `'2026-01-02'` | `DATE '2026-01-02'` | `'2026-01-02'` | `CONVERT(date, '2026-01-02')` | `DATE '2026-01-02'` |
| `LocalTime` / `TIME` | `'12:34:56'` | `TIME '12:34:56'` | `'12:34:56'` | `CONVERT(time, '12:34:56')` | `TIMESTAMP '1970-01-01 12:34:56'` |
| `LocalDateTime` / `DATETIME` | `'2026-01-02 03:04:05'` | `TIMESTAMP '2026-01-02 03:04:05'` | `'2026-01-02 03:04:05'` | `CONVERT(datetime2, '2026-01-02T03:04:05')` | `TIMESTAMP '2026-01-02 03:04:05'` |
| `java.sql.Timestamp` / `TIMESTAMP` | `'2026-01-02 03:04:05'` | `TIMESTAMP '2026-01-02 03:04:05'` | `'2026-01-02 03:04:05'` | SQLServer 的 `rowversion` 不支持用户默认值 | `TIMESTAMP '2026-01-02 03:04:05'` |
| `ByteArray` / `BLOB` | `NULL` | `NULL` | `NULL` | `NULL` | `NULL` |

例如，Boolean 默认值必须使用目标数据库接受的表达式：

```kotlin
// PostgreSQL
@Default("false")
var enabled: Boolean? = null

// MySQL、SQLite、SQLServer 或 Oracle
@Default("0")
var enabled: Boolean? = null
```

PostgreSQL 不接受 `BOOLEAN DEFAULT 0` 和 `BOOLEAN DEFAULT 1`，应改用 `false` 和 `true`。如果同一个模型需要在多个数据库上建表，请在 schema 定义或迁移脚本中分别维护方言默认值。`CURRENT_DATE`、`CURRENT_TIMESTAMP` 等数据库函数同样会作为原生表达式写入，必须符合目标数据库语法。非空二进制默认值会受到数据库版本和具体列类型限制，应放在方言专用迁移脚本中，不要依赖一套可移植的 `@Default` 写法。

## {{ $.annotation("NonNull") }}列非空约束

此注解用于声明列为非空，如果不指定则使用默认的非空约束

```kotlin
@Table("tb_user")
data class User(
    @NonNull
    val name: String? = null
) : KPojo
```

## {{ $.annotation("CreateTime") }}创建时间列

此注解用于声明列为创建时间字段，如果不指定则使用{{ $.keyword("configuration/global-config", ["全局设置","创建时间策略"])}}。

**参数**：

{{$.params([['enable', '是否启用', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @CreateTime
    val created: String? = null
) : KPojo
```

## {{ $.annotation("UpdateTime") }}更新时间列

此注解用于声明列为更新时间字段，如果不指定则使用{{ $.keyword("configuration/global-config", ["全局设置","更新时间策略"])}}。

**参数**：

{{$.params([['enable', '是否启用', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @UpdateTime
    val updated: String? = null
) : KPojo
```

## {{ $.annotation("LogicDelete") }}逻辑删除列

此注解用于声明列为逻辑删除字段，如果不指定则使用{{ $.keyword("configuration/global-config", ["全局设置","逻辑删除策略"])}}。

**参数**：

{{$.params([['enable', '是否启用', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    val deleted: Boolean? = null
) : KPojo
```

## {{ $.annotation("Version") }}乐观锁（版本）列

此注解用于声明列为乐观锁（版本）字段，如果不指定则使用{{ $.keyword("configuration/global-config", ["全局设置","乐观锁（版本）策略"])}}。

**参数**：

{{$.params([['enable', '是否启用', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @Version
    val version: Int? = null
) : KPojo
```
