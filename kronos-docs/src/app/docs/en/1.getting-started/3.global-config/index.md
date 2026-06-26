{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Default Data Source Settings

If you do not specify a data source during database operations, kronos automatically uses this default data source.

The data source type is `KronosDataSourceWrapper` and the creation method can be referred to:{{ $.keyword("database/connect-to-db", ["Connect to DB"]) }}。

```kotlin
Kronos.init {
    dataSource = YourDataSourceWrapper()
}
```

> **Warning**
> The `Kronos.dataSource` defaults to `NoneDataSourceWrapper`, be sure to **modify your configuration file before using it**.
>

> **Warning**
> Kronos supports multiple data sources and dynamic data sources
>
> **Multiple data sources**: In specific operations, such as KPojo.update.execute(wrapper), other `KronosDataSourceWrapper` instances can be passed in the `execute` function to use other data sources.
>
> **Dynamic Data Source**: `Kronos.dataSource` is a function where you can implement your logic to return different instances of the data source, implementing a dynamic data source.

## Global Table Name Strategy

The table name strategy means that by default (no annotated configuration), kronos automatically generates a **table name** for the database based on the **Kotlin class name**, e.g. `TbUser` -> `tb_user`.

**Parameters**:
{{$.params([['tableNamingStrategy', 'Global Table Naming Strategy', 'KronosNamingStrategy', 'NoneNamingStrategy']])}}
Creating a custom table naming strategy `KronosNamingStrategy` is detailed in: {{ $.keyword("concept/naming-strategy", ["concept", "Naming Strategy"]) }}.

We **by default** provide both `LineHumpNamingStrategy` and `NoneNamingStrategy` **table name strategies**:

**1. {{ $.title("LineHumpNamingStrategy") }} Underscore/Camel Case Naming Strategy**

This strategy converts kotlin class names to underscore-separated lowercase strings, e.g., `ADataClass` -> `a_data_class`, and database table/column names to camel names, e.g., `user_name` -> `userName`.

```kotlin
Kronos.init {
    tableNamingStrategy = lineHumpNamingStrategy
}
```

**2. {{ $.title("NoneNamingStrategy") }} none naming strategy**

This strategy leaves the kotlin class names as they are, e.g. `ADataClass` -> `ADataClass`, and the database table/column names as they are, e.g. `user_name` -> `user_name`.

By default, kronos uses the `NoneNamingStrategy` table name strategy.

## Global Column Naming Strategy

Similar to the global table naming strategy, the column naming strategy refers to the default behavior where Kronos automatically generates **column names** based on the **property names** of Kotlin classes, for example: `classId` -> `class_id`.

**Parameters**:
{{$.params([['fieldNamingStrategy', 'Global Column Name Strategy', 'KronosNamingStrategy', 'NoneNamingStrategy']])}}

The column name strategy is common to the table name strategy, and the setting method is:

```kotlin
Kronos.init {
    fieldNamingStrategy = lineHumpNamingStrategy
}
```

## Creation Time Strategy

Used to set the creation time field for all tables.

**Parameters**:
{{$.params([
    ['createTimeStrategy',
    'Create time strategy, including <b>whether to enable</b>, <b>kotlin property name</b> and <b>database column name information</b>',
    'KronosCommonStrategy',
    'KronosCommonStrategy(false, Field("createTime"))']
])}}

Customize the creation of a time strategy by creating a `KronosCommonStrategy`, see: {{ $.keyword("concept/common-strategy", ["concept", "Common Strategy"]) }}).

The global settings for the creation time strategy are **enabled by default**, and need to be turned on manually.

```kotlin
Kronos.init {
    createTimeStrategy = KronosCommonStrategy(true, Field("createTime"))
}
```

> **Note**
> After the global setting for the creation time strategy is established, it can still be overridden in the `KPojo` class through {{ $.keyword("class-definition/annotation-config", ["Annotation Settings", "@CreateTime Creation Time Column"]) }}.

## Update Time Strategy

Used to set the update time field for all tables (**whether to enable**, **Kotlin property name**, and **database column name**).

**Parameters**:
{{$.params([
['updateTimeStrategy',
'Update timing strategy, including <b>whether to enable</b>, <b>kotlin property name</b>, and <b>database column name information</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(false, Field("updateTime"))']
])}}

By creating a custom update strategy `KronosCommonStrategy`, see: {{ $.keyword("concept/common-strategy", ["concept", "Common Strategy"]) }}).

The global default for update time strategy is turned off and needs to be manually enabled.

```kotlin
Kronos.init {
    updateTimeStrategy = KronosCommonStrategy(true, Field("updateTime"))
}
```

> **Note**
> After setting the logic update time strategy globally, the global setting can still be overridden in the `KPojo` class via {{ $.keyword("class-definition/annotation-config", ["Annotation Settings", "@UpdateTime Update Time Column"]) }}.

## Logical Deletion Strategy

Used to set the logical deletion strategy for all tables (**whether to enable**, **Kotlin property name**, and **database column name**).

**Parameters**:
{{$.params([['logicDeleteStrategy', 
'Logical Deletion Strategy, including <b>whether to enable</b>, <b>kotlin property name</b>, and <b>database column name information</b>',
'KronosCommonStrategy', 'KronosCommonStrategy(false, "deleted")']])}}

By creating a custom logical deletion strategy `KronosCommonStrategy`, see: {{ $.keyword("concept/common-strategy", ["concept", "common strategy"]) }}.

The global default for the logical delete strategy is turned off and needs to be manually enabled.

```kotlin
Kronos.init {
    logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
}
```

> **Note**
> After setting the global logical deletion strategy, it can still be overridden in the `KPojo` class through {{ $.keyword("class-definition/annotation-config", ["Annotation Settings","@LogicDelete Logical Delete Column"]) }}.

## Optimistic Lock (Version) Strategy

Used to set the optimistic lock version field for all tables (**whether it is on**, **kotlin attribute name** and **database column name**).

**Parameters**:
{{$.params([
['optimisticLockStrategy',
'Optimistic lock strategy, including <b>whether to enable</b>, <b>kotlin property name</b>, and <b>database column name information</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(false, Field("version"))']
])}}

By creating a custom optimistic lock strategy `KronosCommonStrategy`, see: {{ $.keyword("concept/common-strategy", ["concept", "Common Strategy"]) }}.

The global default for the optimistic lock strategy is turned off and needs to be manually enabled.

```kotlin
Kronos.init {
    optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
}
```

> **Note**
> After setting the global optimistic lock strategy, it can still be overridden in the `KPojo` class through {{ $.keyword("class-definition/annotation-config", ["Annotation Settings", "@Version Optimistic Lock (Version) Column"]) }}.

## Default Date Time Format

Used to specify the default format for date formatting, following the `ISO 8601` specification, defaulting to `yyyy-MM-dd HH:mm:ss`.

**Parameters**:
{{$.params([['defaultDateFormat', 'Default Date Time Format', 'String', 'yyyy-MM-dd HH:mm:ss']])}}

Kronos uses `yyyy-MM-dd HH:mm:ss` to format the date/time by default, you can change the default format by the following ways:

```kotlin
Kronos.init {
    defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
}
```

> **Note**
> After setting the default date format globally, it can still be overridden in the `KPojo` class through {{ $.keyword("class-definition/annotation-config", ["Annotation Settings", "@DateTimeFormat Date and Time Format"]) }}.

## Default Time Zone

Used to specify the default time zone, following the `ISO 8601` standard, for use when creating timestamps, updating timestamps, and formatting date/time.

**Parameters**:
{{$.params([['timeZone', 'Default Time Zone', 'java.time.ZoneId', 'ZoneId.systemDefault()']])}}

Kronos uses the current system time zone by default. You can change the default time zone by doing the following:

```kotlin
Kronos.init {
    timeZone = ZoneId.of("UTC")
    timeZone = ZoneId.of("Asia/Shanghai")
    timeZone = ZoneId.systemDefault()
    timeZone = ZoneId.of("GMT+8")
}
```

## No-value strategy

Used to set the policy for generating SQL statements when the value in `where`/`having`/`on` etc. conditional statements is `null` by global default.

> **Note**
> For example, in a query scenario, a condition with a null value may want to ignore the query condition, or convert it to a condition such as `is null`, `is not null`, and so on.
> The `NoValueStrategy` strategy is used to handle `null` value conditions in different scenarios.

> **Alert**
> Note that if you are customizing the no-value policy, please use the `Ignore` strategy with caution in `DELETE` and `UPDATE` operations to avoid full table deletion or full table updates.

**Parameters**:
{{$.params([['noValueStrategy', 'No Value Strategy', 'NoValueStrategy', 'DefaultNoValueStrategy']])}}

The `NoValueStrategy` strategy accepts two parameters: operation type (`SELECT`/`UPDATE`/`DELETE`) and statement conditions, returning `NoValueStrategyType`. For details, see: {{ $.keyword("concept/no-value-strategy", ["concept", "No Value Strategy"]) }}.

```kotlin
Kronos.init {
    noValueStrategy = YourCustomNoValueStrategy()
}
```

## Serialization Deserialization Processor

Deserialize strings in the database to objects at query time, and automatically serialize objects when inserting into the database.

**Parameters**:
{{$.params([['serializeProcessor', 'Serialization Deserialization Processor', 'KronosSerializeProcessor', 'NoneSerializeProcessor']])}}

By creating a `KronosSerializeProcessor` custom serialization processor, see: {{ $.keyword("concept/serialize-processor", ["Automatic Serialization and Deserialization"])}}.

For example, serialization parsers can be implemented by introducing the `GSON` library:

```kotlin group="GsonProcessor" name="Main.kt" icon="kotlin"
Kronos.init {
    serializeProcessor = GsonProcessor
}
```

```kotlin group="GsonProcessor" name="GsonProcessor.kt" icon="kotlin"
object GsonProcessor : KronosSerializeProcessor {
    // Use GSON to serialize objects
    override fun serialize(obj: Any): String {
        return Gson().toJson(obj)
    }
    
    // Use GSON to deserialize strings
    override fun deserialize(serializedStr: String, kClass: KClass<*>): Any {
        return Gson().fromJson(serializedStr, kClass.java)
    }
}
```

Here we are using `GSON` library to implement serialization deserialization parser, you can use any library you like like like `Kotlinx.serialization`, `Jackson`, `Moshi`, `FastJson` etc.

## Log output path and switch

Used to set the path and switch for log output under global default conditions.

- When `logPath` is empty, turn off log output.
- When `logPath` is not empty, log output is enabled, and the log output path includes all paths within `logPath`, with `console` being the console output.

**Parameters**:
{{$.params([['logPath', 'Log Output Path', 'List<String>', 'emptyList()']])}}

```kotlin
Kronos.init {
    logPath = listOf("your/log/path")
}
```

Kronos enables log output by default and outputs it to the console.

## Smart Value Conversion

Kronos automatically performs intelligent conversions of expected values to actual values when performing data manipulation, such as `Int` to `Long`, `String`, and so on, as detailed in: {{ $.keyword("concept/value-transformer", ["Concepts", "Value Transformer"]) }}.

The following is a simple example that demonstrates the functionality of smart value conversion:
    
```kotlin
data class User(
    var id: Int? = null,
    var name: String? = "",
    var createTime: kotlinx.datetime.LocalDateTime? = null
)

val mapOfUser = mapOf("id" to 1L, "name" to "Kronos", "createTime" to "2023-10-17T10:00:00")

val user = mapOfUser.mapperTo<User>()
// ❌ The `mapperTo` function does not use smart value conversions by default, in which case it will throw an exception

val user = mapOfUser.safeMapperTo<User>()
// ✅ `safeMapperTo` function defaults to intelligent value conversion, at this time it will automatically convert 1L to Int, and "2023-10-17T10:00:00" to LocalDateTime.

```

Kronos enables the `getTypeSafeValue` and `safeMapperTo` functions for smart value conversion by default.

Kronos' value conversion feature **does not use reflection** but is implemented through code generated by the compiler plugin, which avoids the performance loss caused by reflection, but this feature may still cause some performance loss, and you can turn off smart value conversion** if you don't need it by doing the following**:

```kotlin
Kronos.init {
    strictSetValue = true
}
```
