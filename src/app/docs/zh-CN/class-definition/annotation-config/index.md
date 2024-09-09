{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.annotation("Table") }} 表名设置

用于指定数据类的表名，该注解的生效优先级高于{{ $.keyword("
getting-started/global-config", ["全局设置", "全局表名策略"]) }}的优先级。

**参数**：
{{$.params([['name', 'String', '表名', true]])}}

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
> 在使用`dataSource.table.create<Table>()`或`dataSource.table.sync<Table>()`时生效。

**参数**：
{{$.params([
  ['name', 'String', '索引名', true],
  ['columns', 'Array<String>', '索引列名', true],
  ['type', 'String', $.keyword("concept/table-index", ["索引类型"])],
  ['method', 'String', $.keyword("concept/table-index", ["索引方法"])],
  ['concurrently', 'Boolean', '是否并发创建索引，**仅适用于 PostgreSQL**']
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

{{$.params([['enabled', 'Boolean', '是否开启']])}}

```kotlin
@CreateTime(enabled = false)
data class User(
    val id: Int? = null,
    val createTime: String? = null
) : KPojo
```

## {{ $.annotation("UpdateTime") }}表更新时间

用于指定数据表是否开启更新时间策略，该注解的生效优先级高于{{
$.keyword("getting-started/global-config", ["全局设置", "更新时间策略"]) }}的优先级。

**参数**：

{{$.params([['enabled', 'Boolean', '是否开启']])}}

```kotlin
@UpdateTime(enabled = false)
data class User(
    val id: Int? = null,
    val updateTime: String? = null
) : KPojo
```

## {{ $.annotation("LogicDelete") }}表逻辑删除

用于指定数据表是否开启逻辑删除策略，该注解的生效优先级高于{{
$.keyword("getting-started/global-config", ["全局设置", "逻辑删除策略"]) }}的优先级。

**参数**：

{{$.params([['enabled', 'Boolean', '是否开启']])}}

> 在全局开启逻辑删除的情况下取消某张表的逻辑删除功能

```kotlin
@LogicDelete(enabled = false)
data class User(
    val id: Int? = null,
    val deleted: Boolean? = null
) : KPojo
```

## {{ $.annotation("Column") }}列名

用于指定数据表的列名，该注解的生效优先级高于{{
$.keyword("getting-started/global-config", ["全局设置", "逻辑删除策略"]) }}的优先级。

**参数**：

{{$.params([['name', 'String', '列名', true]])}}

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

{{$.params([['pattern', 'String', '日期/时间格式']])}}

```kotlin
data class User(
    @DateTimeFormat("yyyy-MM-dd")
    val birthday: String? = null
) : KPojo
```

## 列序列化/反序列化设置

`@UseSerializeResolver`

用于声明该列是否需要进行序列化/反序列化，使用该注解的字段Kronos将调用序列化解析器（见{{
$.keyword("getting-started/global-config", ["全局设置", "序列化反序列化处理器"])
}}）将该列的值在数据库存入和读取时进行序列化和反序列化操作。

```kotlin
data class User(
    @ColumnDeserialize
    val info: List<String>? = emptyList()
) : KPojo
```

## 级联关系声明

`@Cascade`

此注解用于声明列的级联设置，用于**级联查询**、**级联插入**、**级联更新**、**级联删除**等。支持**一对一**、**一对多**、**多对多
**关联。

级联操作的详细用法请参考[进阶用法]。

> **Note**
> 请将此注解加在类型为`KPojo`或`Collection<KPojo>`的属性上，确保当前定义的表内有关联字段（如以下例子中的companyId）。

**参数**：

- properties `Array<String>`：本表的关联字段属性名，如以下示例中`companyId`用于关联`Company`实体。
- targetProperties `Array<String>`：关联目标表关联字段属性名，如以下示例中`companyId`关联到`Company`的`id`属性。
- onDelete `CascadeDeleteAction`：{{ $.keyword("concept/cascade-delete-action", ["关联删除策略"])
  }}，默认为`NO_ACTION`（无操作）。
- defaultValue `Array<String>`：指定级联删除方式为"SET DEFAULT"时设置的默认值（可选）。
- usage `Array<KOperationType>`: 用于声明本实体需要用到的关联操作（可选，默认为
  `[Insert, Update, Delete, Upsert, Select]`）。

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
    val employees: List<Employee>? = null
) : KPojo
```

## 列主键设置

`@PrimaryKey(identity: Boolean)`

此注解用于声明列为主键。

**参数**：

- identity `Boolean`：是否自增

```kotlin
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    val id: Int? = null
) : KPojo
```

## 列类型及长度

`@ColumnType(type: String, length: Int)`

对于不同的数据库类型，kronos会根据kotlin类型自动转换类型，您可以参考[Kotlin列类型推断](/documentation/class-definition/kotlin-type-to-kcolumn-type)
查看Kotlin数据类型在各个数据库中的映射关系。
您可以通过此注解声明列类型及长度，如果不指定则使用默认的类型及长度，全部类型信息请参考：[Kronos列类型](/documentation/class-definition/kcolumn-type)

**参数**：

- type `String`：类型
- length `Int`：长度

```kotlin
@Table("tb_user")
data class User(
    @ColumnType(KColumnType.Char, 10)
    val name: String? = null
) : KPojo
```

## 列非空约束

`@NotNull`

此注解用于声明列为非空，如果不指定则使用默认的非空约束

```kotlin
@Table("tb_user")
data class User(
    @NotNull
    val name: String? = null
) : KPojo
```

## 列创建时间

`@CreateTime`

此注解用于声明列为创建时间字段，如果不指定则使用默认的创建时间策略。

**参数**：

- enabled `Boolean`：是否启用

```kotlin
@Table("tb_user")
data class User(
    @CreateTime
    val created: String? = null
) : KPojo
```

## 列更新时间

`@UpdateTime`

此注解用于声明列为更新时间字段，如果不指定则使用默认的更新时间策略。

**参数**：

- enabled `Boolean`：是否启用

```kotlin
@Table("tb_user")
data class User(
    @UpdateTime
    val updated: String? = null
) : KPojo
```

## 列逻辑删除

`@LogicDelete`

此注解用于声明列为逻辑删除字段，如果不指定则使用默认的逻辑删除策略。

**参数**：

- enabled `Boolean`：是否启用

```kotlin
@Table("tb_user")
data class User(
    @LogicDelete
    val deleted: String? = null
) : KPojo
```

## 列乐观锁

`@LogicDelete`

此注解用于声明列为乐观锁字段，如果不指定则使用默认的乐观锁策略。

**参数**：

- enabled `Boolean`：是否启用

```kotlin
@Table("tb_user")
data class User(
    @Version
    val version: Int? = null
) : KPojo
```
