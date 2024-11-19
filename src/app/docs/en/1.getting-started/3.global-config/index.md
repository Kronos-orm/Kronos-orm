{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Default Data Source Settings

If you do not specify a data source during database operations, kronos automatically uses this default data source.

The data source type is `KronosDataSourceWrapper`, and the creation method can be found at {{ $.keyword("database/connect-to-db", ["Connect to database"]) }}.

```kotlin
Kronos.dataSource = { yourDataSourceWrapper }
```

> **Warning**
> The default data source defaults to `NoneDataSourceWrapper`, be sure to modify your configuration file before using it.
>

> **Warning**
> Kronos supports multiple data sources and dynamic data sources.
>
> **Multiple data sources**: for specific operations, such as KPojo.update.execute(wrapper), other `KronosDataSourceWrapper` can be passed in the `execute` function.
> instances in the `execute` function, thus using other data sources.
>
> **Dynamic data source**: `Kronos.dataSource` is a function where you can implement your logic to return different instances of the data source, implementing a dynamic data source.

## Global table name strategy

The table name policy means that by default (no annotated configuration), kronos automatically generates a **table name** for the database based on the **Kotlin class name**, e.g. `User` -> `user`.

**parameters**：
{{$.params([['tableNamingStrategy', 'Global Table Name Strategy', 'KronosNamingStrategy', 'NoneNamingStrategy']])}}

Creating a custom table naming strategy `KronosNamingStrategy` is detailed in: {{ $.keyword("concept/naming-strategy", ["concept", "naming-strategy"]) }}.

We **by default** provide both `LineHumpNamingStrategy` and `NoneNamingStrategy` **table name strategies**:

**1. {{ $.title("LineHumpNamingStrategy") }} underscore/hump naming strategy**

This policy converts kotlin class names to underscore-separated lowercase strings, e.g. `ADataClass` -> `a_data_class`
This strategy converts database table/column names to camel names, e.g. `user_name` -> `userName`.

```kotlin
Kronos.init {
    tableNamingStrategy = lineHumpNamingStrategy
}
```

**2. {{ $.title("NoneNamingStrategy") }} none naming strategy**

This strategy leaves kotlin class names as they are, e.g. `ADataClass` -> `ADataClass`, and database table/column names as they are, e.g. `user_name` -> `user_name`.

By default, kronos uses the `NoneNamingStrategy` table name strategy.

## Global column name strategy

Similar to the global table name strategy, the column name strategy means that by default, kronos automatically generates **column names** based on the **property name** of the Kotlin class, e.g. `classId` -> `class_id`.

**Parameters**:
{{$.params([['fieldNamingStrategy', 'Global column name strategy', 'KronosNamingStrategy', 'NoneNamingStrategy']])}}

The column name strategy class is common to the table name strategy and is set up in the following way:

```kotlin
Kronos.init {
    fieldNamingStrategy = lineHumpNamingStrategy
}
```

## Create time strategy

Used to set the creation time field for all tables.

**Parameters**:
{{$.params([
['createTimeStrategy',
'Create time strategy with information on <b>whether to turn on</b>, <b>kotlin attribute names</b> and <b>database column names</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(false, Field("createTime"))']
])}}

Customize the creation of a time strategy by creating a `KronosCommonStrategy`, see: {{ $.keyword("concept/common-strategy", ["concept", "common strategy"]) }}).

The global **default** for creating time policies is turned off** and needs to be turned on manually.

```kotlin
Kronos.createTimeStrategy = KronosCommonStrategy(true, Field("createTime"))
```

> **Note**
> After setting the creation time strategy globally, the global setting can still be overridden in the `KPojo` class via {{ $.keyword("class-definition/annotation-config", ["Annotation Settings","@CreateTime Create Time Column"]) }}.

## Update time strategy

Used to set the update time field for all tables (**whether on**, **kotlin attribute name** and **database column name**).

**Parameters**:
{{$.params([
['updateTimeStrategy',
'Update time strategy with information on <b>whether to turn on</b>, <b>kotlin attribute names</b> and <b>database column names</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(false, Field("updateTime"))']
])}}

Customize the update time strategy by creating a `KronosCommonStrategy`, see: {{ $.keyword("concept/common-strategy", ["concept", "common strategy"]) }}).

The update time strategy is turned off by global default and needs to be turned on manually.

```kotlin
Kronos.updateTimeStrategy = KronosCommonStrategy(true, Field("updateTime"))
```

> **Note**
> After setting the logic update time policy globally, the global setting can still be overridden in the `KPojo` class via {{ $.keyword("class-definition/annotation-config", ["annotation-config", "@UpdateTime Update Time Column"]) }}.

## Logical deletion policy

Used to set logical delete fields for all tables (**whether on**, **kotlin attribute name** and **database column name**).

**Parameter**:
{{$.params([['logicDeleteStrategy', 'KronosCommonStrategy', 'KronosCommonStrategy(false, "deleted")']]) }}

Customize the logical deletion strategy by creating a `KronosCommonStrategy`, see: {{ $.keyword("concept/common-strategy", ["concept", "common strategy"]) }}).

The Logical Deletion Policy is turned off by global default and needs to be turned on manually.

```kotlin
Kronos.logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
```

> **Note**
> After setting the logic deletion policy globally, the global setting can still be overridden in the `KPojo` class via {{ $.keyword("class-definition/annotation-config", ["Annotation Settings","@LogicDelete Logic Delete Columns"]) }}.

## Optimistic locking (versioning) strategy

Used to set the optimistic lock version field for all tables (**whether it is on**, **kotlin attribute name** and **database column name**).

**Parameters**:
{{$.params([
['optimisticLockStrategy',
'Optimistic locking (versioning) policy with information on <b>whether to turn on</b>, <b>kotlin attribute name</b> and <b>database column name</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(false, Field("version"))']
])}}

Customize the optimistic locking (versioning) strategy by creating a `KronosCommonStrategy`, see: {{ $.keyword("concept/common-strategy", ["concepts", "common strategy"]) }}).

The optimistic locking strategy is turned off by global default and needs to be turned on manually.

```kotlin
Kronos.optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
```

> **Note**
> After setting the optimistic locking policy globally, the global setting can still be overridden in the `KPojo` class via {{ $.keyword("class-definition/annotation-config", ["Annotation Settings","@Version Optimistic Locking (Version) Column"]) }}.

## Default datetime format

Used to specify the default format for date formatting, following the `ISO 8601` specification, defaulting to `yyyy-MM-dd HH:mm:ss`.

**Parameters**:
{{$.params([['defaultDateFormat', 'Default Date Time Format', 'String', 'yyyy-MM-dd HH:mm:ss']])}}

Kronos uses `yyyy-MM-dd HH:mm:ss` to format the date/time by default, you can change the default format by the following ways:

```kotlin
Kronos.defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
```

> **Note**
> After setting the default date format globally, the global setting can still be overridden in the `KPojo` class via {{ $.keyword("class-definition/annotation-config", ["Annotation Settings", "@DateTimeFormat date time format"]) }}.

## Default time zone

Used to specify the default time zone, following the `ISO 8601` specification, when creating time, updating time and formatting date/time.

**Parameters**:
{{$.params([['timeZone', 'Default Time Zone', 'java.time.ZoneId', 'ZoneId.systemDefault()']])}}

Kronos uses the current system time zone by default. You can change the default time zone by doing the following:

```kotlin
Kronos.timeZone = ZoneId.of("UTC")
Kronos.timeZone = ZoneId.of("Asia/Shanghai")
Kronos.timeZone = ZoneId.systemDefault()
Kronos.timeZone = ZoneId.of("GMT+8")
```

## No-value strategy

Used to set the policy for generating SQL statements when the value in `where`/`having`/`on` etc. conditional statements is `null` by global default.

> **Note**
> For example, in a query scenario where a condition has a value of null you may want to ignore the query condition or convert it to a condition such as `is null`, `is not null` etc.
> `NoValueStrategy` strategy for handling `null` value conditions in different scenarios

> **Alert**
> Note that if you are customizing the no-value policy, use the `Ignore` policy with caution during `DELETE` and `UPDATE` operations to avoid full table deletions or full table updates.

**Parameters**:
{{$.params([['noValueStrategy', 'strategy without value', 'NoValueStrategy', 'DefaultNoValueStrategy']])}}

The `NoValueStrategy` strategy accepts two parameters, the type of operation (`SELECT`/`UPDATE`/`DELETE`) and the statement condition, and returns the `NoValueStrategyType`, as detailed in:{{ $.keyword("concept/no-value-strategy", ["concept", "no-value-strategy"])}}.

## Serialization Deserialization Processor

Deserialize strings in the database to objects at query time, and automatically serialize objects when inserting into the database.

**Parameters**:
{{$.params([['serializeProcessor', 'Serialization Deserialization Processor', 'KronosSerializeProcessor', 'NoneSerializeProcessor']])}}

By creating`KronosSerializeProcessor`Custom Serialization Parser，for further details, refer to：{{ $.keyword("concept/serialize-processor", ["Automatic Serialization and Deserialization"])}}。

For example, a serialization parser can be implemented by introducing the `GSON` library:

```kotlin group="GsonProcessor" name="GsonProcessor.kt" icon="kotlin"
object GsonProcessor : KronosSerializeProcessor {
    // Serializing Objects with GSON
    override fun serialize(obj: Any): String {
        return Gson().toJson(obj)
    }
    
    // Deserializing Objects with GSON
    override fun deserialize(serializedStr: String, kClass: KClass<*>): Any {
        return Gson().fromJson(serializedStr, kClass.java)
    }
}
```

```kotlin group="GsonProcessor" name="KronosConfig.kt" icon="kotlin"
Kronos.serializeProcessor = GsonProcessor
```

Here we are using `GSON` library to implement serialization deserialization parser, you can use any library you like like like `Kotlinx.serialization`, `Jackson`, `Moshi`, `FastJson` etc.