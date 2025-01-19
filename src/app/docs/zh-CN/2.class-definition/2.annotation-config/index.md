{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.annotation("Table") }} 表名设置

用于指定数据类的表名，该注解的生效优先级高于{{ $.keyword("
getting-started/global-config", ["全局设置", "全局表名策略"]) }}的优先级。

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

## {{ $.annotation("TableIndex") }}表索引

用于指定数据表的索引。

> **Note**
> 在使用`dataSource.table.createTable<KPojo>()`或`dataSource.table.syncTable<KPojo>()`时生效。

**参数**：
{{$.params([
  ['name', '索引名', 'String'],
  ['columns', '索引列名', 'Array<String>'],
  ['type', $.noun("table-index", "索引类型"), 'String', '数据库类型默认'],
  ['method', $.noun("table-index", "索引方法"), 'String', '数据库类型默认'],
  ['concurrently', '是否并发创建索引，<b>仅适用于 PostgreSQL</b>', 'Boolean', false]
])}}

```kotlin
import java.time.LocalDateTime

@TableIndex("idx_name_create_time", ["name", "create_time"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class User(
    val id: Int? = null,
    val name: String? = null,
    val createTime: LocalDateTime? = null
) : KPojo
```

## {{ $.annotation("CreateTime") }}表创建时间

用于指定数据表是否开启创建时间策略，该注解的生效优先级高于{{ $.keyword(
"getting-started/global-config", ["全局设置", "创建时间策略"]) }}的优先级。

**参数**：

{{$.params([['enabled', '是否开启', 'Boolean', true]])}}

```kotlin
// 在全局开启创建时间的情况下取消某张表的创建时间功能
@CreateTime(enabled = false)
data class User(
    val id: Int? = null
) : KPojo

// 在全局关闭创建时间的情况下开启某张表的创建时间功能
@CreateTime
data class User(
  val id: Int? = null,
  val createTime: String? = null
) : KPojo
```

## {{ $.annotation("UpdateTime") }}表更新时间

用于指定数据表是否开启更新时间策略，该注解的生效优先级高于{{
$.keyword("getting-started/global-config", ["全局设置", "更新时间策略"]) }}的优先级。

**参数**：

{{$.params([['enabled', '是否开启', 'Boolean', true]])}}

```kotlin
// 在全局开启更新时间的情况下取消某张表的更新时间功能
@UpdateTime(enabled = false)
data class User(
  val id: Int? = null
) : KPojo

// 在全局关闭更新时间的情况下开启某张表的更新时间功能
@UpdateTime
data class User(
    val id: Int? = null,
    val updateTime: String? = null
) : KPojo
```

## {{ $.annotation("LogicDelete") }}表逻辑删除

用于指定数据表是否开启逻辑删除策略，该注解的生效优先级高于{{
$.keyword("getting-started/global-config", ["全局设置", "逻辑删除策略"]) }}的优先级。

**参数**：

{{$.params([['enabled', '是否开启', 'Boolean', true]])}}

```kotlin
// 在全局开启逻辑删除的情况下取消某张表的逻辑删除功能
@LogicDelete(enabled = false)
data class User(
    val id: Int? = null
) : KPojo

// 在全局关闭逻辑删除的情况下开启某张表的逻辑删除功能
@LogicDelete
data class User(
  val id: Int? = null,
  val deleted: Boolean? = null
) : KPojo
```

## {{ $.annotation("Column") }}列名

用于指定数据表的列名，该注解的生效优先级高于{{
$.keyword("getting-started/global-config", ["全局设置", "全局列名策略"]) }}的优先级。

**参数**：

{{$.params([['name', 'String', '列名']])}}

```kotlin
data class User(
    @Column("user_name")
    val name: String? = null
) : KPojo
```

## {{ $.annotation("DateTimeFormat") }}日期时间格式

用于指定数据表的日期/时间格式，该注解的生效优先级高于{{
$.keyword("getting-started/global-config", ["全局设置", "默认日期时间格式"]) }}的优先级。

**参数**：

{{$.params([['pattern', '日期/时间格式', 'String']])}}

```kotlin
data class User(
    @DateTimeFormat("yyyy-MM-dd")
    val birthday: String? = null
) : KPojo
```

## {{ $.annotation("Serialize") }}列序列化反序列化设置

用于声明该列是否需要进行自动序列化、反序列化，使用该注解的字段Kronos将调用序列化反序列化处理器（见{{
$.keyword("getting-started/global-config", ["全局设置", "序列化反序列化处理器"])
}}）将该列的值在数据库存入和读取时进行序列化和反序列化操作。

```kotlin
data class User(
    @Serialize
    val info: List<String>? = emptyList()
) : KPojo
```
序列化反序列化的功能使用请参考：{{ $.keyword("concept/serialize-processor", ["自动序列化与反序列化"]) }}。

## {{ $.annotation("Cascade") }}级联关系声明

此注解用于声明列的级联关系，详细定义及用法请参考{{ $.keyword("advanced/cascade-definition", ["进阶用法","级联关系定义"]) }}。

> **Note**
> 请将此注解加在类型为`KPojo`或`Collection<KPojo>`的属性上，确保当前定义的表内有关联字段（如以下例子中的companyId）。

**参数**：
{{ $.params([
  ['properties', '本表的关联字段属性名', 'Array<String>'],
  ['targetProperties', '关联目标表关联字段属性名', 'Array<String>'],
  ['onDelete', $.keyword("concept/cascade-delete-action", ["关联删除策略"]), 'CascadeDeleteAction', 'NO_ACTION'],
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

## {{ $.annotation("Ignore") }} 忽略字段

此注解用于在部分操作中忽略字段。

**参数**：

{{$.params([['action', '忽略操作', 'IgnoreAction', 'ALL']])}}

1. `IgnoreAction.ALL`：忽略该属性，不进行任何数据库操作，也不认定该属性为数据库字段。
2. `IgnoreAction.TO_MAP`：忽略该属性，`KPojo.toDataMap()`方法不会将该属性加入到Map中。
3. `IgnoreAction.FROM_MAP`：忽略该属性，`KPojo.fromDataMap()`和`KPojo.safeFromDataMap()`方法不会将Map中的该属性赋值给实体对象。
4. `IgnoreAction.SELECT`：忽略该属性，不进行查询操作。
5. `IgnoreAction.INSERT`：忽略该属性，不进行插入操作（尚未实现）。
6. `IgnoreAction.UPDATE`：忽略该属性，不进行更新操作（尚未实现）。
7. `IgnoreAction.DELETE`：忽略该属性，不进行删除操作（尚未实现）。
8. `IgnoreAction.CASCADE_SELECT`：忽略该属性，不进行级联查询操作。

**示例**：

1. 排除某属性，该属性不视为数据库字段：

```kotlin
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore([IgnoreAction.ALL])
    var name: String? = null,
    @Ignore var age: Int? = null
) : KPojo
```
上述示例中，`name`和`age`属性不会被视为数据库字段。

2. 排除某属性，该属性不进行查询操作：

```kotlin
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore([IgnoreAction.SELECT])
    var name: String? = null
) : KPojo
```

上述示例中，使用该KPojo进行查询操作时，`name`属性不会被查询。

3. 排除某属性，该属性不进行级联查询操作：

```kotlin
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

上述示例中，查询`Employee`时，`Company`的`employees`属性不会被级联查询。

> **Note**
> 请注意，此注解是单向的，即给`employees`加上`@CascadeSelectIgnore`注解，`employees`不会被级联查询，但是`company`会被级联查询。

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
> 使用**雪花算法**时，自增字段必须为`Long`类型，否则无法正确生成主键，请您在配置文件中配置`datacenterId`和`workerId`，参考：{{ $.keyword("getting-started/global-config", ["主键生成器", "雪花算法"]) }}。

> **Note**
> 使用**自定义主键**时，请提前设置配置自定义主键生成器`customIdGenerator`(`KIdGenerator<T>`)，参考：{{ $.keyword("getting-started/global-config", ["主键生成器", "自定义主键生成器"]) }}。

## {{ $.annotation("ColumnType") }}列类型及长度

对于不同的数据库类型，kronos会根据kotlin类型自动转换类型，您可以参考{{ $.keyword("class-definition/kotlin-type-to-kcolumn-type", ["Kotlin列类型推断"]) }}
查看Kotlin数据类型在各个数据库中的映射关系。
您可以通过此注解声明列类型及长度，如果不指定则使用默认的类型及长度，全部类型信息请参考：{{ $.keyword("class-definition/kcolumn-type", ["Kronos列类型"]) }}。

**参数**：

{{$.params([
  ['type', '类型', 'String'],
  ['length', '长度', 'Int', '根据类型默认']
])}}

```kotlin
@Table("tb_user")
data class User(
    @ColumnType(KColumnType.Char, 10)
    val name: String? = null
) : KPojo
```

## {{ $.annotation("Necessary") }}列非空约束

此注解用于声明列为非空，如果不指定则使用默认的非空约束

```kotlin
@Table("tb_user")
data class User(
    @Necessary
    val name: String? = null
) : KPojo
```

## {{ $.annotation("CreateTime") }}创建时间列

此注解用于声明列为创建时间字段，如果不指定则使用{{ $.keyword("getting-started/global-config", ["全局设置","创建时间策略"])}}。

**参数**：

{{$.params([['enabled', '是否启用', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @CreateTime
    val created: String? = null
) : KPojo
```

## {{ $.annotation("UpdateTime") }}更新时间列

此注解用于声明列为更新时间字段，如果不指定则使用{{ $.keyword("getting-started/global-config", ["全局设置","更新时间策略"])}}。

**参数**：

{{$.params([['enabled', '是否启用', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @UpdateTime
    val UPDATED: String? = null
) : KPojo
```

## {{ $.annotation("LogicDelete") }}逻辑删除列

此注解用于声明列为逻辑删除字段，如果不指定则使用{{ $.keyword("getting-started/global-config", ["全局设置","逻辑删除策略"])}}。

**参数**：

{{$.params([['enabled', '是否启用', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @LogicDelete
    val deleted: String? = null
) : KPojo
```

## {{ $.annotation("Version") }}乐观锁（版本）列

此注解用于声明列为乐观锁（版本）字段，如果不指定则使用{{ $.keyword("getting-started/global-config", ["全局设置","乐观锁（版本）策略"])}}。

**参数**：

{{$.params([['enabled', '是否启用', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @Version
    val version: Int? = null
) : KPojo
```
