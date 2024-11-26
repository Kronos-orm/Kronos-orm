{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.annotation("Table") }} Table Name

Used to specify the table name of a data class, this annotation takes precedence over {{ $.keyword("getting-started/global-config", ["Global Configuration", "Table Naming Strategy"]) }}.

**参数**：
{{$.params([['name', 'table name', 'String']])}}

```kotlin
@Table("tb_user")
data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

## {{ $.annotation("TableIndex") }} Table Index

Used to specify the index of a data table.

> **Note**
> 在使用`dataSource.table.createTable<KPojo>()`或`dataSource.table.syncTable<KPojo>()`时生效。

**Parameters**:

{{$.params([
  ['name', 'index name', 'String'],
  ['columns', 'index column name', 'Array<String>'],
  ['type', $.noun("table-index", "index type"), 'String', 'default by database type'],
  ['method', $.noun("table-index", "index method"), 'String', 'default by database type'],
  ['concurrently', 'Whether to create the index concurrently, <b>only applicable to PostgreSQL</b>', 'Boolean', false]
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

## {{ $.annotation("CreateTime") }} Table Create Time

Used to specify whether a data table is enabled for create time strategy, this annotation takes precedence over {{ $.keyword("getting-started/global-config", ["Global Configuration", "Create Time Strategy"]) }}.

**Parameters**:

{{$.params([['enabled', 'Whether to enable', 'Boolean', true]])}}

```kotlin
// Disabling the creation time function for a table with creation time turned on globally
@CreateTime(enabled = false)
data class User(
    val id: Int? = null
) : KPojo

// Enabling creation time for a table with creation time turned off globally
@CreateTime
data class User(
  val id: Int? = null,
  val createTime: String? = null
) : KPojo
```

## {{ $.annotation("UpdateTime") }} Table Update Time

Used to specify whether a data table is enabled for update time strategy, this annotation takes precedence over {{ $.keyword("getting-started/global-config", ["Global Configuration", "Update Time Strategy"]) }}.

**Parameters**:

{{$.params([['enabled', 'Whether to enable', 'Boolean', true]])}}

```kotlin
// Disabling update time for a table with update time turned on globally
@UpdateTime(enabled = false)
data class User(
  val id: Int? = null
) : KPojo

// Enabling update time for a table with update time turned off globally
@UpdateTime
data class User(
    val id: Int? = null,
    val updateTime: String? = null
) : KPojo
```

## {{ $.annotation("LogicDelete") }} Table Logic Delete

Used to specify whether the logical deletion policy is turned on for a datasheet, this annotation takes precedence over {{ $.keyword("getting-started/global-config", ["Global Settings", "Logical Deletion Policy"]) }}.

**Parameters**:

{{$.params([['enabled', 'Whether to enable', 'Boolean', true]])}}

```kotlin
// Disabling logical deletion for a table with logical deletion turned on globally
@LogicDelete(enabled = false)
data class User(
    val id: Int? = null
) : KPojo

// Enabling logical deletion for a table with logical deletion turned off globally
@LogicDelete
data class User(
  val id: Int? = null,
  val deleted: Boolean? = null
) : KPojo
```

## {{ $.annotation("Column") }} Column Name

Used to specify the column name of a data class, this annotation takes precedence over {{ $.keyword("getting-started/global-config", ["Global Settings", "Field Naming Strategy"]) }}.

**Parameters**:

{{$.params([['name', 'column name', 'String']])}}

```kotlin
data class User(
    @Column("user_name")
    val name: String? = null
) : KPojo
```

## {{ $.annotation("DateTimeFormat") }} Date/Time Format

Used to specify the date/time format for the data table, the effectiveness of this annotation takes precedence over {{ $.keyword("getting-started/global-config", ["Global Settings", "Default Date Time Format"]) }}.

**Parameters**:

{{$.params([['pattern', 'Date/Time Format', 'String']])}}

```kotlin
data class User(
    @DateTimeFormat("yyyy-MM-dd")
    val birthday: String? = null
) : KPojo
```

## {{ $.annotation("Serializable") }} Serialization/Deserialization

Used to declare whether the column needs to be automatically serialized or deserialized. The field using this annotation, Kronos, will call the serialization and deserialization processor (see {{ $.keyword("getting-started/global-config", ["Global Settings", "Serialization and Deserialization Processor"]) }}) to perform serialization and deserialization operations on the column's values when storing and reading from the database.

```kotlin
data class User(
    @Serializable
    val info: List<String>? = emptyList()
) : KPojo
```
For the functionality of serialization and deserialization, please refer to: {{ $.keyword("concept/serialize-processor", ["automatic serialization and deserialization"]) }}.

## {{ $.annotation("Cascade") }} Cascade Relationship Declaration

This annotation is used to declare the cascade relationship of columns, please refer to {{ $.keyword("advanced/cascade-definition", ["Advanced Usage", "Cascade Relationship Definition"]) }} for detailed definition and usage.

> **Note**
> Please add this annotation to properties of type `KPojo` or `Collection<KPojo>`, ensuring that there are related fields in the currently defined table (such as the companyId in the example below).

**Parameters**:

{{ $.params([
    ['properties', 'The associated field properties of the current table', 'Array<String>'],
    ['targetProperties', 'The associated field properties of the target table', 'Array<String>'],
    ['onDelete', $.keyword("concept/cascade-delete-action", ["Cascade Delete Action"]), 'CascadeDeleteAction', 'NO_ACTION'],
    ['defaultValue', 'The default value set when the cascade delete method is set to "SET DEFAULT" (optional)', 'Array<String>', '[]'],
    ['usage', 'Used to declare the associated operations required by this entity', 'Array<KOperationType>', '[Insert, Update, Delete, Upsert, Select]']
])}}

```kotlin
// Cascade setting, no need for entity foreign keys, associate with the id of the Company table through companyId
// One-to-many relationship
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

## {{ $.annotation("Ignore") }} Ignore Property in Query

This annotation is used to declare that the column does not need to be queried in the specified query condition.
You need to pass in the query type to ignore {{ $.keyword("concept/ignore-action", ["Ignore Query Action"]) }}.

**Parameters**:

{{ $.params([
    ['target', 'Ignored query type (' + $.code('SELECT') + ', ' + $.code('CASCADE_SELECT') + ')', 'Array<IgnoreAction>']
])}}

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
  @Ignore([CASCADE_SELECT])
  val employees: List<Employee>? = null
) : KPojo
```

> **Note**
> Please note that during cascade operations, this annotation is unidirectional; that is, adding the `@Ignore` annotation to `employees` will prevent `employees` from being query cascaded, but `company` will be query cascaded.

## {{ $.annotation("PrimaryKey") }} Primary Key And Primary Key Type

This annotation is used to declare a column as a primary key.

**Parameters**:

{{$.params([
  ['identity', 'Whether it is self-incrementing', 'Boolean'],
  ['uuid', 'Whether to use uuid as the primary key', 'Boolean'],
  ['snowflake', 'Whether to use the snowflake algorithm as the primary key', 'Boolean'],
  ['custom', 'Whether to use a custom primary key generator', 'Boolean']
])}}

```kotlin
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    val id: Int? = null
) : KPojo
```

> **Note**
> When using a **self-augmenting primary key**, please make sure that the type of the `id` column is of type `Int` or `Long`, otherwise the primary key will not be generated correctly.

> **Note**
> When using **uuid** as the primary key, please make sure that the type of the `id` column is of type `String`, otherwise the primary key will not be generated correctly.

> **Note**
> When using the **Snowflake Algorithm**, the self-incremented field must be of type `Long`, otherwise the primary key will not be generated correctly, please configure `datacenterId` and `workerId` in your configuration file, refer to: {{ $.keyword("getting-started/global-config", ["Primary Key Generator" , "Snowflake Algorithm"]) }}.

> **Note**
> When using a **custom primary key**, please set up the configuration of the custom primary key generator `customIdGenerator` (`KIdGenerator<T>`) ahead of time, refer to: {{ $.keyword("getting-started/global-config", ["Primary Key Generator", "Custom Primary Key Generator"])}}.

## {{ $.annotation("ColumnType") }} Column Type And Length

For different types of databases, Kronos will automatically convert types based on Kotlin types. You can refer to {{ $.keyword("class-definition/kotlin-type-to-kcolumn-type", ["Kotlin column type inference"]) }} to view the mapping of Kotlin data types in various databases.

You can declare the column type and length through this annotation; if not specified, the default type and length will be used. For all type information, please refer to: {{ $.keyword("class-definition/kcolumn-type", ["Kronos column types"]) }}.

**Parameters**:

{{$.params([
  ['type', 'Type', 'String'],
  ['length', 'Length', 'Int', 'default by type']
])}}

```kotlin
@Table("tb_user")
data class User(
    @ColumnType(KColumnType.Char, 10)
    val name: String? = null
) : KPojo
```

## {{ $.annotation("NotNull") }} Non-Null Constraint

This annotation is used to declare a column as a non-null constraint.

```kotlin
@Table("tb_user")
data class User(
    @NotNull
    val name: String? = null
) : KPojo
```

## {{ $.annotation("CreateTime") }} Creation Time Column

This annotation is used to declare the column as a creation time field, or {{ $.keyword("getting-started/global-config", ["Global Configuration", "Creation Time Strategy"]) }}.

**Parameters**:

{{$.params([['enabled', 'Whether to enable', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @CreateTime
    val created: String? = null
) : KPojo
```

## {{ $.annotation("UpdateTime") }} Update Time Column

This annotation is used to declare the column as an update time field, or {{ $.keyword("getting-started/global-config", ["Global Configuration", "Update Time Strategy"]) }}.

**Parameters**:

{{$.params([['enabled', 'Whether to enable', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @UpdateTime
    val UPDATED: String? = null
) : KPojo
```

## {{ $.annotation("LogicDelete") }} Logical Delete Column

This annotation is used to declare the column as a logical delete field, or {{ $.keyword("getting-started/global-config", ["Global Configuration", "Logical Deletion Strategy"]) }}.

**Parameters**:

{{$.params([['enabled', 'Whether to enable', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @LogicDelete
    val deleted: String? = null
) : KPojo
```

## {{ $.annotation("Version") }} Optimistic Locking (Version) Column

This annotation is used to declare a field as an optimistic lock (version) field. If not specified, it uses {{ $.keyword("getting-started/global-config", ["Global Settings", "Optimistic Lock (Version) Strategy"])}}.

**Parameters**:

{{$.params([['enabled', 'Whether to enable', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @Version
    val version: Int? = null
) : KPojo
```
