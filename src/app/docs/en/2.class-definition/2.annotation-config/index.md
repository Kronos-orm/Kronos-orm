{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.annotation("Table") }} Table Name Setting

Used to specify the table name for the data class, the effective priority of this annotation is higher than the priority of {{ $.keyword("
getting-started/global-config", ["Global Setting", "Global Table Naming Strategy"]) }}。

**Parameters**：
{{$.params([['name', 'table name', 'String']])}}

```kotlin
@Table("tb_user")
data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

## {{ $.annotation("TableIndex") }}Table index

Used to specify the index of the data table。

> **Note**
This feature is effective when using `dataSource.table.createTable<KPojo>()` or `dataSource.table.syncTable<KPojo>()`.

**Parameters**:

{{$.params([
['name', 'Index Name', 'String'],
['columns', 'Index Column Names', 'Array<String>'],
['type', $.noun("table-index", "Index Type"), 'String', 'Database Type Default'],
['method', $.noun("table-index", "Index Method"), 'String', 'Database Type Default'],
['concurrently', 'Whether to create index concurrently, <b>only applicable to PostgreSQL</b>', 'Boolean', false]
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

## {{ $.annotation("CreateTime") }}Table creation time

The annotation is used to specify whether the creation time strategy is enabled for the data table. The effective priority of this annotation is higher than that of {{ $.keyword("getting-started/global-config", ["Global Settings", "Creation Time Strategy"]) }}.

**Parameters**:

{{$.params([['enabled', 'Enable', 'Boolean', true]])}}

```kotlin
// Disable the creation time feature for a specific table when the global creation time feature is enabled.
@CreateTime(enabled = false)
data class User(
    val id: Int? = null
) : KPojo

// Enabling the creation time for a table with creation time turned off globally
@CreateTime
data class User(
  val id: Int? = null,
  val createTime: String? = null
) : KPojo
```

## {{ $.annotation("UpdateTime") }}Table update time

Used to specify whether the data table is enabled for update time strategy, the priority of this annotation is higher than that of {{$.keyword("getting-started/global-config", ["Global Settings", "Update Time Strategy"])}}.

**Parameters**:

{{$.params([['enabled', 'Enable', 'Boolean', true]])}}

```kotlin
// Disable the update time feature for a specific table when the global update time function is enabled.
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

## {{ $.annotation("LogicDelete") }}Table Logical Deletion

Used to specify whether the logical deletion strategy is enabled for the data table, the priority of this annotation is higher than that of {{
$.keyword("getting-started/global-config", ["Global Settings", "Logical Deletion Strategy"])}}.

**Parameters**:

{{$.params([['enabled', 'Enabled', 'Boolean', true]])}}

```kotlin
// Disable the logical deletion for a table with logical deletion globally enabled
@LogicDelete(enabled = false)
data class User(
    val id: Int? = null
) : KPojo

// Enabling the logical deletion for a table with logical deletion turned off globally
@LogicDelete
data class User(
  val id: Int? = null,
  val deleted: Boolean? = null
) : KPojo
```

## {{ $.annotation("Column") }}Column name

Used to specify the column names of a data table, this annotation takes precedence over the {{
$.keyword("getting-started/global-config", ["global-set", "global-column-name-policy"]) }} priority.

**Parameters**:

{{$.params([['name', 'String', 'Column name']])}}

```kotlin
data class User(
    @Column("user_name")
    val name: String? = null
) : KPojo
```

## {{ $.annotation("DateTimeFormat") }}Date and time format

The annotation is used to specify the date/time format for the data table, and its effective priority is higher than that of {{$.keyword("getting-started/global-config", ["Global Settings", "Default Date/Time Format"])}}.

**参数**：

{{$.params([['pattern', 'Date/Time Format', 'String']])}}

```kotlin
data class User(
    @DateTimeFormat("yyyy-MM-dd")
    val birthday: String? = null
) : KPojo
```

## {{ $.annotation("Serialize") }}List Serialization/Deserialization Settings

Used to declare whether the column needs to be autoserialized, deserialized or not, fields using this annotation Kronos will invoke the serialization deserialization handler (see {{
$.keyword("getting-started/global-config", ["Global Settings", "Serialization Deserialization Processor"])
}}) to serialize and deserialize the value of the column as it is deposited into and read from the database.

```kotlin
data class User(
    @Serialize
    val info: List<String>? = emptyList()
) : KPojo
```
Please refer to the documentation for the serialization and deserialization feature usage:{{ $.keyword("concept/serialize-processor", ["Automatic Serialization and Deserialization"]) }}。

## {{ $.annotation("Cascade") }}Cascading Relationship Declaration

This annotation is used to declare the cascade relationship of columns, please refer to {{ $.keyword("advanced/cascade-definition", ["Advanced Usage", "Cascade Relationship Definition"]) }} for detailed definition and usage.

> **Note**
> Please add this annotation to an attribute of type `KPojo` or `Collection<KPojo>` to ensure that there is an associated field within the currently defined table (e.g. companyId in the following example).

**Parameters**:

{{ $.params([
['properties', 'This table\'s associated field property name', 'Array<String>'],
['targetProperties', 'Associated target table associated field property name', 'Array<String>'],
['onDelete', $.keyword("concept/cascade-delete-action", ["associated delete strategy"]), 'CascadeDeleteAction', 'NO_ACTION'],
['defaultValue', 'The default value set when the cascade delete method is specified as "SET DEFAULT" (optional)', 'Array<String>', '[]'],
['usage', 'Used to declare the associated operations required by this entity', 'Array<KOperationType>', '[Insert, Update, Delete, Upsert, Select]']
])}}

```kotlin
// A multi-level cascading relationship example
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

## {{ $.annotation("Ignore") }} Ignore Fields

This annotation is used to ignore fields in partial operations.

**Parameters**:

{{$.params([['action', 'Ignore action', 'IgnoreAction', 'ALL']])}}

1. `IgnoreAction.ALL`: The attribute is ignored, no database operation is performed, and the attribute is not recognized as a database field.
2. `IgnoreAction.SELECT`: the attribute is ignored and no query operation is performed.
3. `IgnoreAction.INSERT`: ignores the attribute and does not perform an insert operation (not yet implemented).
4. `IgnoreAction.UPDATE`: ignore this attribute, no update operations (not yet implemented).
5. `IgnoreAction.DELETE`: ignore this attribute and do not perform a delete operation (not yet implemented).
6. `IgnoreAction.CASCADE_SELECT`: ignore this attribute, no cascade query operation.

**Examples**:

1. Exclude an attribute, which is not considered a database field:

```kotlin
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore([IgnoreAction.ALL])
    var name: String? = null,
    @Ignore var age: Int? = null
) : KPojo
```
In the above example, the `name` and `age` properties will not be treated as database fields.

2. Exclude a certain attribute, which does not perform a query operation:

```kotlin
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore([IgnoreAction.SELECT])
    var name: String? = null
) : KPojo
```

In the above example, when querying using the KPojo, the `name` property will not be queried.

3. Exclude a certain attribute, which does not perform cascading query operations:

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

In the above example, when querying `Employee`, the `employees` attribute of `Company` will not be cascaded queried.

> **Note**
> Please notice that this annotation is unidirectional, meaning that adding the `@CascadeSelectIgnore` annotation to `employees` will prevent cascading queries on `employees`, but `company` will still be cascaded.

## {{ $.annotation("PrimaryKey") }}Column primary key setting

This annotation is used to declare a column as the primary key.

**Parameters**:

{{$.params([
['identity', 'Whether to auto-increment', 'Boolean'],
['uuid', 'Whether to use UUID as the primary key', 'Boolean'],
['snowflake', 'Whether to use the Snowflake algorithm as the primary key', 'Boolean'],
['custom', 'Whether to use a custom primary key generator', 'Boolean']
])}}

```kotlin
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    val id: Int? = null
) : KPojo
```

## {{ $.annotation("ColumnType") }}Column Type and Length

For different database types, kronos will automatically convert types based on the kotlin type, you can refer to [Kotlin column type inference](/documentation/class-definition/kotlin-type-to-kcolumn-type)
See how Kotlin data types are mapped across databases.
You can declare the column type and length through this annotation, if not specified then use the default type and length, full type information please refer to: [Kronos column type](/documentation/class-definition/kcolumn-type)

**Parameters**:

{{$.params([
['type', 'Type', 'String'],
['length', 'Length', 'Int', 'Default based on type']
])
}}

```kotlin
@Table("tb_user")
data class User(
    @ColumnType(KColumnType.Char, 10)
    val name: String? = null
) : KPojo
```

## {{ $.annotation("Necessary") }}Column non-null constraint

This annotation is used to declare a column as non-null; if not specified, the default non-null constraint is used.

```kotlin
@Table("tb_user")
data class User(
    @Necessary
    val name: String? = null
) : KPojo
```

## {{ $.annotation("CreateTime") }}Create Time Column

This annotation is used to declare the field as the creation time field. If not specified, it uses {{ $.keyword("getting-started/global-config", ["Global Settings", "Creation Time Strategy"]) }}.

**Parameters**:

{{$.params([['enabled', 'Enabled', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @CreateTime
    val created: String? = null
) : KPojo
```

## {{ $.annotation("UpdateTime") }}Update Time Column

This annotation is used to declare the field as the update time field. If not specified, it uses {{ $.keyword("getting-started/global-config", ["Global Settings", "Update Time Strategy"]) }}.

**Parameters**：

{{$.params([['enabled', 'Enabled', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @UpdateTime
    val updated: String? = null
) : KPojo
```

## {{ $.annotation("LogicDelete") }}Logical Deletion Column

This annotation is used to declare a field as logically deleted. If not specified, it uses {{ $.keyword("getting-started/global-config", ["Global Settings", "Logical Deletion Strategy"]) }}.
**Parameters**：

{{$.params([['enabled', 'Enabled', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @LogicDelete
    val deleted: String? = null
) : KPojo
```

## {{ $.annotation("Version") }}Optimistic Lock (Version) Column

This annotation is used to declare the field as an optimistic lock (version), if not specified, it uses {{ $.keyword("getting-started/global-config", ["Global Settings", "Optimistic Lock (Version) Strategy"]) }}.
**Parameters**：

{{$.params([['enabled', 'Enabled', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @Version
    val version: Int? = null
) : KPojo
```
