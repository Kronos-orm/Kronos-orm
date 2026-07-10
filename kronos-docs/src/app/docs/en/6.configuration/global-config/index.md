{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

Use this table to choose the global settings to configure first.

| Level | Configure | Entry |
|-------|-----------|-------|
| Required | Default data source wrapper | [Default Data Source Settings](#default-data-source-settings) |
| Common | Table and column naming, default date format, time zone, logging | [Global Table Name Strategy](#global-table-name-strategy), [Global Column Naming Strategy](#global-column-naming-strategy), [Default Date Time Format](#default-date-time-format), [Default Time Zone](#default-time-zone), [Log output path and switch](#log-output-path-and-switch) |
| Enable by business need | Common field strategies, no-value behavior, serialization, smart value conversion | [Creation Time Strategy](#creation-time-strategy), [Logical Deletion Strategy](#logical-deletion-strategy), [No-value strategy](#no-value-strategy), [Serialization Deserialization Processor](#serialization-deserialization-processor), [Smart Value Conversion](#smart-value-conversion) |

Compiler-plugin setup is configured in the build tool. See {{ $.keyword("configuration/compiler-plugins", ["Compiler Plugins"]) }} for source-set and diagnostic checks.

## Default Data Source Settings

Set `Kronos.dataSource` before running ORM operations. Operations without an explicit wrapper call this function to get the default data source.

The data source wrapper setup is covered in {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }}.

```kotlin group="Data source 1" name="default" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper = KronosJdbcWrapper(BasicDataSource())

with(Kronos) {
    dataSource = { wrapper }
}
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
with(Kronos) {
    dataSource = {
        if (TenantContext.current() == "archive") archiveWrapper else primaryWrapper
    }
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
with(Kronos) {
    tableNamingStrategy = lineHumpNamingStrategy
}
```

```text group="Naming 1" name="result"
UserProfile -> user_profile
```

### {{ $.title("NoneNamingStrategy") }} table names

`NoneNamingStrategy` leaves kotlin class names and database names unchanged. Kronos uses this strategy by default.

```kotlin group="Naming 2" name="none" icon="kotlin"
with(Kronos) {
    tableNamingStrategy = noneNamingStrategy
}
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
with(Kronos) {
    fieldNamingStrategy = lineHumpNamingStrategy
}
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

```kotlin group="Common strategies 1" name="create time" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

with(Kronos) {
    createTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("create_time", "createTime"))
}
```

> **Note**
> After the global setting for the creation time strategy is established, it can still be overridden in the `KPojo` class through {{ $.keyword("mapping/annotations", ["Annotation Settings", "@CreateTime Creation Time Column"]) }}.

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

```kotlin group="Common strategies 2" name="update time" icon="kotlin"
with(Kronos) {
    updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))
}
```

> **Note**
> After setting the logic update time strategy globally, the global setting can still be overridden in the `KPojo` class via {{ $.keyword("mapping/annotations", ["Annotation Settings", "@UpdateTime Update Time Column"]) }}.

## Logical Deletion Strategy

Used to set the logical deletion strategy for all tables (**whether to enable**, **Kotlin property name**, and **database column name**).

**Parameters**:
{{$.params([['logicDeleteStrategy', 
'Logical Deletion Strategy, including <b>whether to enable</b>, <b>kotlin property name</b>, and <b>database column name information</b>',
'KronosCommonStrategy', 'KronosCommonStrategy(enabled = false, field = Field("deleted"))']])}}

By creating a custom logical deletion strategy `KronosCommonStrategy`, see: {{ $.keyword("configuration/common-strategy", ["concept", "common strategy"]) }}.

The global default for the logical delete strategy is turned off and needs to be manually enabled.

```kotlin group="Common strategies 3" name="logic delete" icon="kotlin"
with(Kronos) {
    logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
}
```

```sql group="Common strategies 3" name="logic delete sql" icon="mysql"
SELECT `id`, `name`, `deleted`
FROM `user`
WHERE `user`.`deleted` = 0
```

> **Note**
> After setting the global logical deletion strategy, it can still be overridden in the `KPojo` class through {{ $.keyword("mapping/annotations", ["Annotation Settings","@LogicDelete Logical Delete Column"]) }}.

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

```kotlin group="Common strategies 4" name="version" icon="kotlin"
with(Kronos) {
    optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
}
```

> **Note**
> After setting the global optimistic lock strategy, it can still be overridden in the `KPojo` class through {{ $.keyword("mapping/annotations", ["Annotation Settings", "@Version Optimistic Lock (Version) Column"]) }}.

## Default Date Time Format

Used to specify the default date-time formatting pattern. The default value is `yyyy-MM-dd HH:mm:ss`.

**Parameters**:
{{$.params([['defaultDateFormat', 'Default Date Time Format', 'String', 'yyyy-MM-dd HH:mm:ss']])}}

Kronos uses `yyyy-MM-dd HH:mm:ss` to format the date/time by default, you can change the default format by the following ways:

```kotlin group="Time 1" name="format" icon="kotlin"
with(Kronos) {
    defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
}
```

> **Note**
> After setting the default date format globally, it can still be overridden in the `KPojo` class through {{ $.keyword("mapping/annotations", ["Annotation Settings", "@DateTimeFormat Date and Time Format"]) }}.

## Default Time Zone

Used to specify the default time zone, following the `ISO 8601` standard, for use when creating timestamps, updating timestamps, and formatting date/time.

**Parameters**:
{{$.params([['timeZone', 'Default Time Zone', 'java.time.ZoneId', 'ZoneId.systemDefault()']])}}

Kronos uses the current system time zone by default. You can change the default time zone by doing the following:

```kotlin group="Time 2" name="zone" icon="kotlin"
import java.time.ZoneId

with(Kronos) {
    timeZone = ZoneId.of("UTC")
    timeZone = ZoneId.of("Asia/Shanghai")
    timeZone = ZoneId.systemDefault()
    timeZone = ZoneId.of("GMT+8")
}
```

## No-value strategy

Use `.ifNoValue(...)` on one condition when that expression needs a local empty-value rule.

```kotlin group="No value 1" name="ignore" icon="kotlin"
val users = User()
    .select()
    .where { (it.name == null).ifNoValue(NoValueStrategyType.Ignore) }
    .toList()
```

```sql group="No value 1" name="ignore sql" icon="mysql"
SELECT `id`, `name`
FROM `user`
```

Use `isNull` when the generated condition should be SQL `IS NULL`.

```kotlin group="No value 2" name="is null" icon="kotlin"
val users = User()
    .select()
    .where { it.name.isNull }
    .toList()
```

```sql group="No value 2" name="is null sql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `user`.`name` IS NULL
```

> **Warning**
> Use `NoValueStrategyType.Ignore` with caution in `DELETE` and `UPDATE` operations to avoid full-table deletion or full-table updates.

For details, see: {{ $.keyword("configuration/no-value-strategy", ["concept", "No Value Strategy"]) }}.

## Serialization Deserialization Processor

Deserialize strings in the database to objects at query time, and automatically serialize objects when inserting into the database.

**Parameters**:
{{$.params([['serializeProcessor', 'Serialization Deserialization Processor', 'KronosSerializeProcessor', 'NoneSerializeProcessor']])}}

By creating a `KronosSerializeProcessor` custom serialization processor, see: {{ $.keyword("configuration/serialization-processor", ["Automatic Serialization and Deserialization"])}}.

For example, serialization parsers can be implemented by introducing the `GSON` library:

```kotlin group="GsonProcessor" name="Main.kt" icon="kotlin"
with(Kronos) {
    serializeProcessor = GsonProcessor
}
```

```kotlin group="GsonProcessor" name="GsonProcessor.kt" icon="kotlin"
import com.google.gson.Gson
import com.kotlinorm.interfaces.KronosSerializeProcessor
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

object GsonProcessor : KronosSerializeProcessor {
    // Use GSON to serialize objects
    override fun serialize(obj: Any, kType: KType): String {
        return Gson().toJson(obj)
    }
    
    // Use GSON to deserialize strings
    override fun deserialize(serializedStr: String, kType: KType): Any {
        return Gson().fromJson(serializedStr, kType.javaType)
    }
}
```

Here we are using `GSON` library to implement serialization deserialization parser. You can also use libraries such as `Kotlinx.serialization`, `Jackson`, `Moshi`, or `FastJson`; processors receive the field declaration `KType` so generic fields can be handled explicitly.

## Log output path and switch

Used to set the path and switch for log output under global default conditions.

**Parameters**:
{{$.params([['logPath', 'Log Output Path', 'List<String>', 'listOf("console")']])}}

Write logs to the console and a file path by setting `logPath`.

```kotlin group="Logging 1" name="console and file" icon="kotlin"
with(Kronos) {
    logPath = listOf("console", "/var/log/kronos")
}
```

Turn off bundled log output with an empty list.

```kotlin group="Logging 2" name="off" icon="kotlin"
with(Kronos) {
    logPath = emptyList()
}
```

## Smart Value Conversion

Kronos automatically performs intelligent conversions of expected values to actual values when performing data manipulation, such as `Int` to `Long`, `String`, and so on, as detailed in: {{ $.keyword("configuration/value-transformer", ["Concepts", "Value Transformer"]) }}.

The following is a simple example that demonstrates the functionality of smart value conversion:
    
```kotlin
data class User(
    var id: Int? = null,
    var name: String? = "",
    var createTime: kotlinx.datetime.LocalDateTime? = null
)

val mapOfUser = mapOf("id" to 1L, "name" to "Kronos", "createTime" to "2023-10-17T10:00:00")

val strictResult = runCatching {
    mapOfUser.mapperTo<User>()
}
// strictResult.isFailure == true, because id is Long and User.id expects Int

val user = mapOfUser.safeMapperTo<User>()
// User(id = 1, name = "Kronos", createTime = LocalDateTime(...))
```

Kronos enables the `getTypeSafeValue` and `safeMapperTo` functions for smart value conversion by default.

Set `strictSetValue = true` when safe assignment should keep raw map values and let the target property assignment validate the type directly.

```kotlin
with(Kronos) {
    strictSetValue = true
}
```
