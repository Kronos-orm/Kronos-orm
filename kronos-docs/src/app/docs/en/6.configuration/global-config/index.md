{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Use this table to choose the global settings to configure first.

| Level | Configure | Entry |
|-------|-----------|-------|
| Required | Default data source wrapper | [Default Data Source Settings](#default-data-source-settings) |
| Common | Table and column naming, default date format, time zone, logging | [Global Table Name Strategy](#global-table-name-strategy), [Global Column Naming Strategy](#global-column-naming-strategy), [Default Date Time Format](#default-date-time-format), [Default Time Zone](#default-time-zone), [Log output path and switch](#log-output-path-and-switch) |
| Enable by business need | Common field strategies, no-value behavior, JSON serialization, custom value mapping, map value conversion | [Creation Time Strategy](#creation-time-strategy), [Logical Deletion Strategy](#logical-deletion-strategy), [No-value strategy](#no-value-strategy), {{ $.keyword("mapping/serialization", ["Serialization"]) }}, [Custom value mapping](#custom-value-mapping), [Map value conversion](#map-value-conversion) |

Compiler-plugin setup is configured in the build tool. See {{ $.keyword("configuration/compiler-plugins", ["Compiler Plugins"]) }} for source-set and diagnostic checks.

## Default Data Source Settings

Set `Kronos.dataSource` before running ORM operations. Operations without an explicit wrapper call this function to get the default data source.

The data source wrapper setup is covered in {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }}.

```kotlin group="Data source 1" name="default" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper = KronosJdbcWrapper(BasicDataSource())

Kronos.dataSource = { wrapper }
```

> **Warning**
> The `Kronos.dataSource` defaults to `NoneDataSourceWrapper`, be sure to **modify your configuration file before using it**.

Pass a wrapper to `execute(wrapper)` when one operation should use another data source.

```kotlin group="Data source 2" name="operation wrapper" icon="kotlin"
val archiveWrapper = KronosJdbcWrapper(archiveDataSource)

User(id = 1)
    .update()
    .set { it.name = "Kronos ORM" }
    .where()
    .execute(archiveWrapper)
```

Return different wrappers from `Kronos.dataSource` when the application chooses a data source at runtime.

```kotlin group="Data source 3" name="dynamic" icon="kotlin"
import com.kotlinorm.Kronos

Kronos.dataSource = {
    if (TenantContext.current() == "archive") archiveWrapper else primaryWrapper
}
```

## Global Table Name Strategy

The table name strategy means that by default (no annotated configuration), kronos automatically generates a **table name** for the database based on the **Kotlin class name**, e.g. `TbUser` -> `tb_user`.

**Parameters**:
{{$.params([['tableNamingStrategy', 'Global Table Naming Strategy', 'KronosNamingStrategy', 'NoneNamingStrategy']])}}
Creating a custom table naming strategy `KronosNamingStrategy` is detailed in: {{ $.keyword("configuration/naming-strategy", ["concept", "Naming Strategy"]) }}.

### {{ $.title("LineHumpNamingStrategy") }} table names

This strategy converts kotlin class names to underscore-separated lowercase strings, e.g., `ADataClass` -> `a_data_class`, and database table/column names to camel names, e.g., `user_name` -> `userName`.

```kotlin group="Naming 1" name="table" icon="kotlin"
import com.kotlinorm.Kronos

Kronos.tableNamingStrategy = Kronos.lineHumpNamingStrategy
```

```text group="Naming 1" name="result"
UserProfile -> user_profile
```

### {{ $.title("NoneNamingStrategy") }} table names

`NoneNamingStrategy` leaves kotlin class names and database names unchanged. Kronos uses this strategy by default.

```kotlin group="Naming 2" name="none" icon="kotlin"
import com.kotlinorm.Kronos

Kronos.tableNamingStrategy = Kronos.noneNamingStrategy
```

```text group="Naming 2" name="none result"
UserProfile -> UserProfile
```

## Global Column Naming Strategy

Similar to the global table naming strategy, the column naming strategy refers to the default behavior where Kronos automatically generates **column names** based on the **property names** of Kotlin classes, for example: `classId` -> `class_id`.

**Parameters**:
{{$.params([['fieldNamingStrategy', 'Global Column Name Strategy', 'KronosNamingStrategy', 'NoneNamingStrategy']])}}

Use the same naming strategy for columns when Kotlin property names should map to database column names.

```kotlin group="Naming 3" name="field" icon="kotlin"
import com.kotlinorm.Kronos

Kronos.fieldNamingStrategy = Kronos.lineHumpNamingStrategy
```

```text group="Naming 3" name="field result"
userName -> user_name
```

## Creation Time Strategy

Used to set the creation time field for all tables.

**Parameters**:
{{$.params([
    ['createTimeStrategy',
    'Create time strategy, including <b>whether to enable</b>, <b>kotlin property name</b> and <b>database column name information</b>',
    'KronosCommonStrategy',
    'KronosCommonStrategy(enabled = false, field = Field("create_time", "createTime"))']
])}}

Customize the creation of a time strategy by creating a `KronosCommonStrategy`, see: {{ $.keyword("configuration/common-strategy", ["concept", "Common Strategy"]) }}).

The global default for the creation time strategy is turned off and needs to be manually enabled.

Add the configured property to each model that uses this strategy. `Field("create_time", "createTime")` uses a `createTime` property.

```kotlin group="Common strategies 1" name="create time" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

Kronos.createTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("create_time", "createTime"))
```

> **Note**
> Configure `@CreateTime` on an individual model to override the global setting. See {{ $.keyword("mapping/annotations", ["Annotations"]) }}.

## Update Time Strategy

Used to set the update time field for all tables (**whether to enable**, **Kotlin property name**, and **database column name**).

**Parameters**:
{{$.params([
['updateTimeStrategy',
'Update timing strategy, including <b>whether to enable</b>, <b>kotlin property name</b>, and <b>database column name information</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(enabled = false, field = Field("update_time", "updateTime"))']
])}}

By creating a custom update strategy `KronosCommonStrategy`, see: {{ $.keyword("configuration/common-strategy", ["concept", "Common Strategy"]) }}).

The global default for update time strategy is turned off and needs to be manually enabled.

Add the configured property to each model that uses this strategy. `Field("update_time", "updateTime")` uses an `updateTime` property.

```kotlin group="Common strategies 2" name="update time" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

Kronos.updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))
```

> **Note**
> Configure `@UpdateTime` on an individual model to override the global setting. See {{ $.keyword("mapping/annotations", ["Annotations"]) }}.

## Logical Deletion Strategy

Used to set the logical deletion strategy for all tables (**whether to enable**, **Kotlin property name**, and **database column name**).

**Parameters**:
{{$.params([['logicDeleteStrategy', 
'Logical Deletion Strategy, including <b>whether to enable</b>, <b>kotlin property name</b>, and <b>database column name information</b>',
'KronosCommonStrategy', 'KronosCommonStrategy(enabled = false, field = Field("deleted"))']])}}

By creating a custom logical deletion strategy `KronosCommonStrategy`, see: {{ $.keyword("configuration/common-strategy", ["concept", "common strategy"]) }}.

The global default for the logical delete strategy is turned off and needs to be manually enabled.

Add the configured property to each model that uses this strategy. `Field("deleted")` uses a `deleted` property.

```kotlin group="Common strategies 3" name="logic delete" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

Kronos.logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
```

```sql group="Common strategies 3" name="logic delete sql" icon="mysql"
SELECT `id`, `name`, `deleted`
FROM `user`
WHERE `user`.`deleted` = 0
```

> **Note**
> Configure `@LogicDelete` on an individual model to override the global setting. See {{ $.keyword("mapping/annotations", ["Annotations"]) }}.

## Optimistic Lock (Version) Strategy

Used to set the optimistic lock version field for all tables (**whether it is on**, **kotlin attribute name** and **database column name**).

**Parameters**:
{{$.params([
['optimisticLockStrategy',
'Optimistic lock strategy, including <b>whether to enable</b>, <b>kotlin property name</b>, and <b>database column name information</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(enabled = false, field = Field("version"))']
])}}

By creating a custom optimistic lock strategy `KronosCommonStrategy`, see: {{ $.keyword("configuration/common-strategy", ["concept", "Common Strategy"]) }}.

The global default for the optimistic lock strategy is turned off and needs to be manually enabled.

Add the configured property to each model that uses this strategy. `Field("version")` uses a `version` property.

```kotlin group="Common strategies 4" name="version" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

Kronos.optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
```

> **Note**
> Configure `@Version` on an individual model to override the global setting. See {{ $.keyword("mapping/annotations", ["Annotations"]) }}.

## Default Date Time Format

Used to specify the default date-time formatting pattern. The default value is `yyyy-MM-dd HH:mm:ss`.

**Parameters**:
{{$.params([['defaultDateFormat', 'Default Date Time Format', 'String', 'yyyy-MM-dd HH:mm:ss']])}}

Kronos uses `yyyy-MM-dd HH:mm:ss` to format the date/time by default, you can change the default format by the following ways:

```kotlin group="Time 1" name="format" icon="kotlin"
import com.kotlinorm.Kronos

Kronos.defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
```

> **Note**
> Configure `@DateTimeFormat` on an individual model to override the global setting. See {{ $.keyword("mapping/annotations", ["Annotations"]) }}.

## Default Time Zone

Used to specify the default time zone, following the `ISO 8601` standard, for use when creating timestamps, updating timestamps, and formatting date/time.

**Parameters**:
{{$.params([['timeZone', 'Default Time Zone', 'java.time.ZoneId', 'ZoneId.systemDefault()']])}}

Kronos uses the current system time zone by default. You can change the default time zone by doing the following:

```kotlin group="Time 2" name="zone" icon="kotlin"
import com.kotlinorm.Kronos
import java.time.ZoneId

with(Kronos) {
    timeZone = ZoneId.of("UTC")
    timeZone = ZoneId.of("Asia/Shanghai")
    timeZone = ZoneId.systemDefault()
    timeZone = ZoneId.of("GMT+8")
}
```

## No-value strategy

Use `.takeIf(...)` to keep a predicate when a Kotlin condition is true, or `.takeUnless(...)` to keep it when the condition is false.

```kotlin group="No value 1" name="ignore" icon="kotlin"
val name: String? = null

val users = User()
    .select()
    .where { (it.name == name).takeIf(name != null) }
    .toList()
```

The Boolean gate is ordinary Kotlin code. For example, `(it.status == 0).takeUnless(includeInactive)` omits the status predicate when `includeInactive` is true.

```sql group="No value 1" name="ignore sql" icon="mysql"
SELECT `id`, `name`
FROM `user`
```

Use `== null` or `isNull` when the generated condition should be SQL `IS NULL`.

```kotlin group="No value 2" name="is null" icon="kotlin"
val users = User()
    .select()
    .where { it.name == null }
    .toList()
```

```sql group="No value 2" name="is null sql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `user`.`name` IS NULL
```

> **Warning**
> Use `NoValueStrategyType.Ignore` with caution in `DELETE` and `UPDATE` operations to avoid full-table deletion or full-table updates.

For details, see {{ $.keyword("configuration/no-value-strategy", ["No-value Behavior"]) }}.

## Custom value mapping

Register a mapping at application startup when a model property uses a domain value such as `Money` and its column stores a scalar value. See {{ $.keyword("configuration/value-codec", ["Custom Value Mapping"]) }} for a complete `Money` example.

Use {{ $.annotation("Serialize") }} with {{ $.keyword("mapping/serialization", ["Serialization"]) }} for JSON objects and collections.

## Log output path and switch

Used to set the path and switch for log output under global default conditions.

**Parameters**:
{{$.params([['logPath', 'Log Output Path', 'Array<String>', '["console"]']])}}

Write logs to the console and a file path by setting `logPath`. The {{ $.keyword("configuration/logging", ["Kronos-logging"]) }} page covers output destinations and logger adapters.

```kotlin group="Logging 1" name="console and file" icon="kotlin"
import com.kotlinorm.Kronos

Kronos.logPath = ["console", "/var/log/kronos"]
```

## Map value conversion

Use `safeMapperTo` to create a new model from application input, and use `safeFromMapData` to apply input to a model that already exists. Both APIs match map keys with model properties and convert compatible values. See {{ $.keyword("advanced/mapper-to", ["Map/KPojo Conversion"]) }} for the complete map-to-model and model-to-model API reference.

```kotlin name="kotlin" icon="kotlin"
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.safeMapperTo

data class User(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

val input = mapOf("id" to 1L, "name" to "Kronos")

val created = input.safeMapperTo<User>()
// User(id = 1, name = "Kronos")

val updated = User().safeFromMapData<User>(input)
// User(id = 1, name = "Kronos")
```

Set `strictSetValue = true` for an input boundary that receives already-normalized values. With this setting, built-in numeric, boolean, and date/time values are accepted when they match the model property type. Registered custom mappings remain available. For example, `User.id: Int?` accepts an `Int` input and reports a mapping error for a `Long` input.

```kotlin
import com.kotlinorm.Kronos

Kronos.strictSetValue = true
```
