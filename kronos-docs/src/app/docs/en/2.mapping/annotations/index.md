{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## {{ $.annotation("Table") }} Table Name Setting

Used to specify the table name for the data class. The annotation takes precedence over {{ $.keyword("configuration/global-config", ["Global Settings", "Global Table Naming Strategy"]) }}.

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

See {{ $.keyword("mapping/table-and-column", ["Table and Column"]) }} for a table and column mapping walkthrough with DDL shape.

## {{ $.annotation("TableIndex") }}Table index

Used to specify the index of the data table.

> **Note**
> This feature is effective when using `wrapper.table.createTable(instance)`, `wrapper.table.createTable<T>()`, `wrapper.table.syncTable(instance)`, or `wrapper.table.syncTable<T>()`.

**Parameters**:

{{$.params([
['name', 'Index Name', 'String'],
['columns', 'Index Column Names', 'Array<String>'],
['type', $.noun("table-index", "Index Type"), 'String', '""'],
['method', $.noun("table-index", "Index Method"), 'String', '""'],
['concurrently', 'Whether to create index concurrently, <b>only applicable to PostgreSQL</b>', 'Boolean', false]
])}}

For a single-column index, pass one column name to `@TableIndex(columns = [...])`.

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

See {{ $.keyword("mapping/indexes", ["Table Index"]) }} for `type`, `method`, `concurrently`, `KTableIndex`, and dialect behavior.

## {{ $.annotation("CreateTime") }}Table creation time

The annotation is used to specify whether the creation time strategy is enabled for the data table. The effective priority of this annotation is higher than that of {{ $.keyword("configuration/global-config", ["Global Settings", "Creation Time Strategy"]) }}.

**Parameters**:

{{$.params([['enable', 'Enable', 'Boolean', true]])}}

```kotlin
// Disable the creation time feature for a specific table when the global creation time feature is enabled.
Kronos.createTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("create_time", "createTime"))

@CreateTime(enable = false)
data class User(
    val id: Int? = null,
    val createTime: String? = null // or just remove this field to disable creation time
) : KPojo

// Enabling the creation time for a table with creation time turned off globally
Kronos.createTimeStrategy = KronosCommonStrategy(enabled = false, field = Field("create_time", "createTime"))

@CreateTime
data class User(
  val id: Int? = null,
  val createTime: String? = null // creation time field
) : KPojo
```

## {{ $.annotation("UpdateTime") }}Table update time

Used to specify whether the data table is enabled for update time strategy, the priority of this annotation is higher than that of {{$.keyword("configuration/global-config", ["Global Settings", "Update Time Strategy"])}}.

**Parameters**:

{{$.params([['enable', 'Enable', 'Boolean', true]])}}

```kotlin
// Disable the update time feature for a specific table when the global update time function is enabled.
Kronos.updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))

@UpdateTime(enable = false)
data class User(
    val id: Int? = null,
    val updateTime: String? = null // or just remove this field to disable update time
) : KPojo

// Enabling update time for a table with update time turned off globally
Kronos.updateTimeStrategy = KronosCommonStrategy(enabled = false, field = Field("update_time", "updateTime"))

@UpdateTime
data class User(
    val id: Int? = null,
    val updateTime: String? = null // update time field
) : KPojo
```

## {{ $.annotation("LogicDelete") }}Table Logical Deletion

Used to specify whether the logical deletion strategy is enabled for the data table. The annotation takes precedence over {{ $.keyword("configuration/global-config", ["Global Settings", "Logical Deletion Strategy"]) }}.

**Parameters**:

{{$.params([['enable', 'Enabled', 'Boolean', true]])}}

```kotlin
// Disable the logical deletion for a table with logical deletion globally enabled
Kronos.logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))

@LogicDelete(enable = false)
data class User(
    val id: Int? = null,
    @Default("0") // @Default("false") for Postgres
    val deleted: Boolean? = null // or just remove this field to disable logical deletion
) : KPojo

// Enabling the logical deletion for a table with logical deletion turned off globally
Kronos.logicDeleteStrategy = KronosCommonStrategy(enabled = false, field = Field("deleted"))
@LogicDelete
data class User(
  val id: Int? = null,
  @Default("0") // @Default("false") for Postgres
  val deleted: Boolean? = null // logical deletion field
) : KPojo
```

## {{ $.annotation("Column") }}Column name

Used to specify the column name of a data table. The annotation takes precedence over {{ $.keyword("configuration/global-config", ["Global Settings", "Global Column Naming Strategy"]) }}.

**Parameters**:

{{$.params([['name', 'String', 'Column name']])}}

```kotlin
data class User(
    @Column("user_name")
    val name: String? = null
) : KPojo
```

## {{ $.annotation("DateTimeFormat") }}Date and time format

The annotation is used to specify the date/time format for the data table, and its effective priority is higher than that of {{$.keyword("configuration/global-config", ["Global Settings", "Default Date/Time Format"])}}.

**Parameters**:

{{$.params([['pattern', 'Date/Time Format', 'String']])}}

```kotlin
data class User(
    @DateTimeFormat("yyyy-MM-dd")
    val birthday: String? = null
) : KPojo
```

## {{ $.annotation("Serialize") }} JSON fields

Use this annotation for an object or collection property that the application stores as JSON. Configure Gson or Kotlinx Serialization when the application starts, then use the property with its regular Kotlin type.

```kotlin
data class User(
    @Serialize
    val info: List<String>? = emptyList()
) : KPojo
```
For setup and complete examples, see {{ $.keyword("mapping/serialization", ["Serialization"]) }}.

## {{ $.annotation("Cascade") }}Cascading Relationship Declaration

This annotation is used to declare the cascade relationship of columns, please refer to {{ $.keyword("advanced/cascade", ["Advanced Usage", "Cascade Relationship Definition"]) }} for detailed definition and usage.

> **Note**
> Please add this annotation to an attribute of type `KPojo` or `Collection<KPojo>` to ensure that there is an associated field within the currently defined table (e.g. companyId in the following example).

**Parameters**:

{{ $.params([
['properties', 'This table\'s associated field property name', 'Array<String>'],
['targetProperties', 'Associated target table associated field property name', 'Array<String>'],
['onDelete', $.keyword("advanced/cascade-delete-action", ["associated delete strategy"]), 'CascadeDeleteAction', 'NO_ACTION'],
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

See {{ $.keyword("mapping/cascade-mapping", ["Cascade Mapping"]) }} for one-to-one, one-to-many, and multi-field relationship examples.

## {{ $.annotation("Ignore") }} Ignore Fields

This annotation is used to ignore fields in selected operations. `@Ignore` is equivalent to `@Ignore([IgnoreAction.ALL])`.

**Parameters**:

{{$.params([['targets', 'Ignore action list', 'Array<IgnoreAction>', '[IgnoreAction.ALL]']])}}

`IgnoreAction.ALL` treats the property as a non-column property.

```kotlin group="Ignore 1" name="all" icon="kotlin"
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore
    var displayName: String? = null
) : KPojo
```

`IgnoreAction.TO_MAP` skips the property in `KPojo.toDataMap()`.

```kotlin group="Ignore 2" name="to map" icon="kotlin"
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore([IgnoreAction.TO_MAP])
    var temporaryToken: String? = null
) : KPojo

val data = User(id = 1, temporaryToken = "token").toDataMap()
```

`IgnoreAction.FROM_MAP` skips the property in `KPojo.fromMapData()` and `KPojo.safeFromMapData()`.

```kotlin group="Ignore 3" name="from map" icon="kotlin"
@Table("tb_user")
data class User(
    var id: Int? = null,
    @Ignore([IgnoreAction.FROM_MAP])
    var localState: String? = null
) : KPojo

val user = User().fromMapData<User>(mapOf("id" to 1, "localState" to "draft"))
```

`IgnoreAction.SELECT` skips the property in the default `select()` field list.

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

`IgnoreAction.CASCADE_SELECT` skips the relationship property during cascade select.

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
> Please notice that this annotation is unidirectional, meaning that adding the `@Ignore([CASCADE_SELECT])` annotation to `employees` will prevent cascading queries on `employees`, but `company` will still be cascaded.

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

For different database types, Kronos automatically maps Kotlin types to column types. See {{ $.keyword("mapping/column-types", ["Kotlin Type to KColumnType"]) }} for the mapping.
Use this annotation when a column needs an explicit database type or length. Full type information is covered in {{ $.keyword("mapping/column-type-reference", ["KColumnType"]) }}.

**Parameters**:

{{$.params([
['type', 'Type', 'KColumnType'],
['length', 'Length', 'Int', 'Default based on type'],
['scale', 'Scale for decimal types', 'Int', '0']
])
}}

```kotlin group="ColumnType" name="kotlin" icon="kotlin"
@Table("tb_user")
data class User(
    @ColumnType(KColumnType.CHAR, 10)
    val name: String? = null
) : KPojo
```

## {{ $.annotation("Default") }}Column default value

This annotation is used to declare the database default value for a column.

**Parameters**:

{{$.params([['value', 'Default SQL value', 'String']])}}

```kotlin group="Default" name="kotlin" icon="kotlin"
@Table("tb_user")
data class User(
    @Default("0")
    val score: Int? = null
) : KPojo
```

`@Default` is copied into the table DDL as a raw SQL expression. It is not a Kotlin value, and Kronos does not translate one database dialect's expression into another dialect's expression. The following table lists recommended literal forms for automatically inferred Kotlin types:

| Kotlin type / inferred `KColumnType` | MySQL | PostgreSQL | SQLite | SQLServer | Oracle |
|--------------------------------------|-------|------------|--------|-----------|--------|
| `Boolean` / `BIT` | `0` / `1` | `false` / `true` | `0` / `1` | `0` / `1` | `0` / `1` |
| `Byte`, `Short`, `Int`, `Long` / integer types | `0` / `1` | `0` / `1` | `0` / `1` | `0` / `1` | `0` / `1` |
| `Float`, `Double`, `BigDecimal` / numeric types | `0.0` / `1.5` | `0.0` / `1.5` | `0.0` / `1.5` | `0.0` / `1.5` | `0.0` / `1.5` |
| `Char`, `String`, other types mapped to `VARCHAR` | `'text'` | `'text'` | `'text'` | `'text'` | `'text'` |
| `Date`, `LocalDate` / `DATE` | `'2026-01-02'` | `DATE '2026-01-02'` | `'2026-01-02'` | `CONVERT(date, '2026-01-02')` | `DATE '2026-01-02'` |
| `LocalTime` / `TIME` | `'12:34:56'` | `TIME '12:34:56'` | `'12:34:56'` | `CONVERT(time, '12:34:56')` | `TIMESTAMP '1970-01-01 12:34:56'` |
| `LocalDateTime` / `DATETIME` | `'2026-01-02 03:04:05'` | `TIMESTAMP '2026-01-02 03:04:05'` | `'2026-01-02 03:04:05'` | `CONVERT(datetime2, '2026-01-02T03:04:05')` | `TIMESTAMP '2026-01-02 03:04:05'` |
| `java.sql.Timestamp` / `TIMESTAMP` | `'2026-01-02 03:04:05'` | `TIMESTAMP '2026-01-02 03:04:05'` | `'2026-01-02 03:04:05'` | Not supported by SQLServer `rowversion` | `TIMESTAMP '2026-01-02 03:04:05'` |
| `ByteArray` / `BLOB` | `NULL` | `NULL` | `NULL` | `NULL` | `NULL` |

For example, a Boolean default must use the expression accepted by the selected database:

```kotlin
// PostgreSQL
@Default("false")
var enabled: Boolean? = null

// MySQL, SQLite, SQLServer, or Oracle
@Default("0")
var enabled: Boolean? = null
```

PostgreSQL rejects `BOOLEAN DEFAULT 0` and `BOOLEAN DEFAULT 1`; use `false` and `true` instead. If one model must create schemas on multiple database types, keep dialect-specific defaults in separate schema definitions or migrations. Database functions such as `CURRENT_DATE` and `CURRENT_TIMESTAMP` are also raw expressions and must be valid for the selected database. Non-empty binary defaults vary by database version and column type; define them in a dialect-specific migration instead of relying on a portable `@Default` value.

## {{ $.annotation("NonNull") }}Column non-null constraint

This annotation is used to declare a column as non-null; if not specified, the default non-null constraint is used.

```kotlin
@Table("tb_user")
data class User(
    @NonNull
    val name: String? = null
) : KPojo
```

## {{ $.annotation("CreateTime") }}Create Time Column

This annotation is used to declare the field as the creation time field. If not specified, it uses {{ $.keyword("configuration/global-config", ["Global Settings", "Creation Time Strategy"]) }}.

**Parameters**:

{{$.params([['enable', 'Enabled', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @CreateTime
    val created: String? = null
) : KPojo
```

## {{ $.annotation("UpdateTime") }}Update Time Column

This annotation is used to declare the field as the update time field. If not specified, it uses {{ $.keyword("configuration/global-config", ["Global Settings", "Update Time Strategy"]) }}.

**Parameters**：

{{$.params([['enable', 'Enabled', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @UpdateTime
    val updated: String? = null
) : KPojo
```

## {{ $.annotation("LogicDelete") }}Logical Deletion Column

This annotation is used to declare a field as logically deleted. If not specified, it uses {{ $.keyword("configuration/global-config", ["Global Settings", "Logical Deletion Strategy"]) }}.
**Parameters**：

{{$.params([['enable', 'Enabled', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    val deleted: Boolean? = null
) : KPojo
```

## {{ $.annotation("Version") }}Optimistic Lock (Version) Column

This annotation is used to declare the field as an optimistic lock (version), if not specified, it uses {{ $.keyword("configuration/global-config", ["Global Settings", "Optimistic Lock (Version) Strategy"]) }}.
**Parameters**：

{{$.params([['enable', 'Enabled', 'Boolean', true]])}}

```kotlin
@Table("tb_user")
data class User(
    @Version
    val version: Int? = null
) : KPojo
```
