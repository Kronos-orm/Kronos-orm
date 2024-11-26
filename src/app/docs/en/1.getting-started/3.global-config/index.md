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
> 全局设置逻更新时间策略后，仍可在`KPojo`类中通过{{ $.keyword("class-definition/annotation-config", ["注解设置","@UpdateTime更新时间列"]) }}覆盖全局设置。

## 逻辑删除策略

用于设置所有表的逻辑删除字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

**参数**：
{{$.params([['logicDeleteStrategy', '逻辑删除策略', 'KronosCommonStrategy', 'KronosCommonStrategy(false, "deleted")']])}}

通过创建`KronosCommonStrategy`自定义逻辑删除策略，详见：{{ $.keyword("concept/common-strategy", ["概念","通用策略"]) }}）。

逻辑删除策略的全局默认关闭，需要手动开启。

```kotlin
Kronos.init {
    logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
}
```

> **Note**
> 全局设置逻辑删除策略后，仍可在`KPojo`类中通过{{ $.keyword("class-definition/annotation-config", ["注解设置","@LogicDelete逻辑删除列"]) }}覆盖全局设置。

## 乐观锁（版本）策略

用于设置所有表的乐观锁版本字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

**参数**：
{{$.params([
['optimisticLockStrategy',
'乐观锁（版本）策略，包含<b>是否开启</b>、<b>kotlin属性名</b>及<b>数据库列名等信息</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(false, Field("version"))']
])}}

通过创建`KronosCommonStrategy`自定义乐观锁（版本）策略，详见：{{ $.keyword("concept/common-strategy", ["概念","通用策略"]) }}）。

乐观锁策略的全局默认关闭，需要手动开启。

```kotlin
Kronos.init {
    optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
}
```

> **Note**
> 全局设置乐观锁策略后，仍可在`KPojo`类中通过{{ $.keyword("class-definition/annotation-config", ["注解设置","@Version乐观锁（版本）列"]) }}覆盖全局设置。

## 默认日期时间格式

用于指定日期格式化的默认格式，遵循`ISO 8601`规范，默认为`yyyy-MM-dd HH:mm:ss`。

**参数**：
{{$.params([['defaultDateFormat', '默认日期时间格式', 'String', 'yyyy-MM-dd HH:mm:ss']])}}

Kronos默认使用`yyyy-MM-dd HH:mm:ss`格式化日期/时间，你可以通过以下方式修改默认格式：

```kotlin
Kronos.init {
    defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
}
```

> **Note**
> 全局设置默认日期格式后，仍可在`KPojo`类中通过{{ $.keyword("class-definition/annotation-config", ["注解设置", "@DateTimeFormat日期时间格式"]) }}覆盖全局设置。

## 默认时区

用于指定默认时区，遵循`ISO 8601`规范，在创建时间、更新时间及格式化日期/时间时使用。

**参数**：
{{$.params([['timeZone', '默认时区', 'java.time.ZoneId', 'ZoneId.systemDefault()']])}}

Kronos默认使用当前系统时区，你可以通过以下方式修改默认时区：

```kotlin
Kronos.init {
    timeZone = ZoneId.of("UTC")
    timeZone = ZoneId.of("Asia/Shanghai")
    timeZone = ZoneId.systemDefault()
    timeZone = ZoneId.of("GMT+8")
}
```

## 无值策略

用于设置全局默认条件下，当`where`/`having`/`on`等条件语句中的值为`null`时，生成SQL语句的策略。

> **Note**
> 如在查询场景下，条件值为null时可能想要忽略该查询条件，或将其转为`is null`，`is not null`等条件。
> `NoValueStrategy`策略用于处理在不同场景下的`null`值条件

> **Alert**
> 注意，如果您正在自定义无值策略，在`DELETE`和`UPDATE`操作中，请谨慎使用`Ignore`策略，以免造成全表删除或全表更新。

**参数**：
{{$.params([['noValueStrategy', '无值策略', 'NoValueStrategy', 'DefaultNoValueStrategy']])}}

`NoValueStrategy`策略接受操作类型（`SELECT`/`UPDATE`/`DELETE`）和语句条件两个参数，返回`NoValueStrategyType`，详见：{{ $.keyword("concept/no-value-strategy", ["概念","无值策略"]) }}。

```kotlin
Kronos.init {
    noValueStrategy = YourCustomNoValueStrategy()
}
```

## 序列化反序列化处理器

将数据库中的字符串在查询时反序列化为对象，在插入数据库时自动序列化对象。

**参数**：
{{$.params([['serializeResolver', '序列化反序列化处理器', 'KronosSerializeResolver', 'NoneSerializeResolver']])}}

通过创建`KronosSerializeResolver`自定义序列化解析器，详见：{{ $.keyword("concept/serialize-resolver", ["自动序列化与反序列化"])}}。

如可以通过引入`GSON`库来实现序列化解析器：

```kotlin group="GsonResolver" name="Main.kt" icon="kotlin"
Kronos.init {
    serializeResolver = GsonResolver
}
```

```kotlin group="GsonResolver" name="GsonResolver.kt" icon="kotlin"
object GsonResolver : KronosSerializeResolver {
    // 使用GSON序列化对象
    override fun serialize(obj: Any): String {
        return Gson().toJson(obj)
    }
    
    // 使用GSON反序列化对象
    override fun deserialize(serializedStr: String, kClass: KClass<*>): Any {
        return Gson().fromJson(serializedStr, kClass.java)
    }
}
```

这里我们使用`GSON`库来实现序列化反序列化解析器，您可以使用任何您喜欢的库如`Kotlinx.serialization`、`Jackson`、`Moshi`、`FastJson`等。

## 日志输出路径及开关

用于设置全局默认条件下，日志输出的路径及开关。

- 当`logPath`为空时，关闭日志输出。
- 当`logPath`不为空时，开启日志输出，日志输出路径为`logPath`内的所有路径，`console`为控制台输出。

**参数**：
{{$.params([['logPath', '日志输出路径', 'List<String>', 'emptyList()']])}}

```kotlin
Kronos.init {
    logPath = listOf("your/log/path")
}
```

Kronos默认开启日志输出，并输出到控制台。

## 关闭智能值转换

Kronos在进行数据操作时，会自动将预期值与实际值进行智能转换，如`Int`与`Long`、`String`等等，详见：{{ $.keyword("concept/value-transformer", ["概念", "值转换器"]) }}。

以下是一个简单的例子，展示了智能值转换的功能：
    
```kotlin
data class User(
    var id: Int? = null,
    var name: String? = "",
    var createTime: kotlinx.datetime.LocalDateTime? = null
)

val mapOfUser = mapOf("id" to 1L, "name" to "Kronos", "createTime" to "2023-10-17T10:00:00")

val user = mapOfUser.mapperTo<User>()
// ❌ mapperTo函数默认不使用智能值转换，此时会抛出异常

val user = mapOfUser.safeMapperTo<User>()
// ✅ safeMapperTo函数默认使用智能值转换，此时会自动将1L转为Int，将"2023-10-17T10:00:00"转为LocalDateTime

```

Kronos默认开启`getTypeSafeValue`以及`safeMapperTo`函数进行智能值转换。

Kronos的值转换功能**不使用反射**，而是通过编译器插件生成的代码来实现，这样可以避免反射带来的性能损耗，但是此功能仍然可能会带来一些性能损耗，如果不需要，您可以通过以下方式**关闭智能值转换**：

```kotlin
Kronos.init {
    strictSetValue = true
}
```