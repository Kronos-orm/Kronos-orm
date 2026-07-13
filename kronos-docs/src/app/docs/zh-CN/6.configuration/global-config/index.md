{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

先用下表选择需要配置的全局项。

| 层级 | 配置项 | 入口 |
|------|--------|------|
| 必须 | 默认数据源 wrapper | [默认数据源设置](#默认数据源设置) |
| 常用 | 表名/列名策略、默认日期格式、时区、日志 | [全局表名策略](#全局表名策略)、[全局列名策略](#全局列名策略)、[默认日期时间格式](#默认日期时间格式)、[默认时区](#默认时区)、[日志输出路径及开关](#日志输出路径及开关) |
| 按业务启用 | 通用字段策略、无值行为、序列化、智能值转换 | [创建时间策略](#创建时间策略)、[逻辑删除策略](#逻辑删除策略)、[无值策略](#无值策略)、[序列化反序列化处理器](#序列化反序列化处理器)、[关闭智能值转换](#关闭智能值转换) |

编译插件配置在构建工具中完成。source set 和诊断检查见 {{ $.keyword("configuration/compiler-plugins", ["编译器插件"]) }}。

## 默认数据源设置

在执行 ORM 操作前设置 `Kronos.dataSource`。没有显式传入 wrapper 的操作，会调用这个函数取得默认数据源。

数据源 wrapper 的创建方式见 {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }}。

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
> 默认数据源的默认值为`NoneDataSourceWrapper`，在使用前请务必修改您的配置文件。

单次操作需要使用其他数据源时，把 wrapper 传给 `execute(wrapper)`。

```kotlin group="Data source 2" name="operation wrapper" icon="kotlin"
val archiveWrapper = KronosJdbcWrapper(archiveDataSource)

User(id = 1)
    .update()
    .set { it.name = "Kronos ORM" }
    .where()
    .execute(archiveWrapper)
```

应用需要按运行时上下文选择数据源时，在 `Kronos.dataSource` 函数里返回不同 wrapper。

```kotlin group="Data source 3" name="dynamic" icon="kotlin"
with(Kronos) {
    dataSource = {
        if (TenantContext.current() == "archive") archiveWrapper else primaryWrapper
    }
}
```

## 全局表名策略

表名策略指在默认情况下（无注解配置），kronos自动根据**Kotlin类名**生成数据库的**表名**，如：`TbUser` -> `tb_user`。

**参数**：
{{$.params([['tableNamingStrategy', '全局表名策略', 'KronosNamingStrategy', 'NoneNamingStrategy']])}}

创建来自定义表名策略`KronosNamingStrategy`详见：{{ $.keyword("configuration/naming-strategy", ["概念","命名策略"]) }}。

### {{ $.title("LineHumpNamingStrategy") }} 表名

该策略将kotlin类名转换为下划线分隔的小写字符串，如：`ADataClass` -> `a_data_class`
，将数据库表/列名转为驼峰命名，如：`user_name` -> `userName`。

```kotlin group="Naming 1" name="table" icon="kotlin"
with(Kronos) {
    tableNamingStrategy = lineHumpNamingStrategy
}
```

```text group="Naming 1" name="result"
UserProfile -> user_profile
```

### {{ $.title("NoneNamingStrategy") }} 表名

`NoneNamingStrategy` 保持 Kotlin 类名和数据库名称原样。Kronos 默认使用该策略。

```kotlin group="Naming 2" name="none" icon="kotlin"
with(Kronos) {
    tableNamingStrategy = noneNamingStrategy
}
```

```text group="Naming 2" name="none result"
UserProfile -> UserProfile
```

## 全局列名策略

同全局表名策略类似，列名策略指在默认情况下，kronos自动根据Kotlin类的**属性名**生成**列名**，如：`classId` -> `class_id`。

**参数**：
{{$.params([['fieldNamingStrategy', '全局列名策略', 'KronosNamingStrategy', 'NoneNamingStrategy']])}}

Kotlin 属性名需要映射到数据库列名时，给字段设置同样的命名策略。

```kotlin group="Naming 3" name="field" icon="kotlin"
with(Kronos) {
    fieldNamingStrategy = lineHumpNamingStrategy
}
```

```text group="Naming 3" name="field result"
userName -> user_name
```

## 创建时间策略

用于设置所有表的创建时间字段。

**参数**：
{{$.params([
    ['createTimeStrategy',
    '创建时间策略，包含<b>是否开启</b>、<b>kotlin属性名</b>及<b>数据库列名等信息</b>',
    'KronosCommonStrategy',
    'KronosCommonStrategy(enabled = false, field = Field("create_time", "createTime"))']
])}}

通过创建`KronosCommonStrategy`自定义创建时间策略，详见：{{ $.keyword("configuration/common-strategy", ["概念","通用策略"]) }}）。

创建时间策略的全局**默认关闭**，需要手动开启。

> **Warning**
> 全局设置创建时间后，KPojo中必须拥有该成员属性，否则该策略不会对该表生效。

```kotlin group="Common strategies 1" name="create time" icon="kotlin" {6}
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

with(Kronos) {
    createTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("create_time", "createTime"))
}

data class User(
    @PrimaryKey(true)
    var id: Int? = null,
    var createTime: LocalDateTime? = null
    // 若没有createTime属性，则创建时间策略不会对该表生效
) : KPojo
```

> **Note**
> 全局设置创建时间策略后，仍可在`KPojo`类中通过{{ $.keyword("mapping/annotations", ["注解设置","@CreateTime创建时间列"]) }}覆盖全局设置。

## 更新时间策略

用于设置所有表的更新时间字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

**参数**：
{{$.params([
['updateTimeStrategy',
'更新时间策略，包含<b>是否开启</b>、<b>kotlin属性名</b>及<b>数据库列名等信息</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(enabled = false, field = Field("update_time", "updateTime"))']
])}}

通过创建`KronosCommonStrategy`自定义更新时间策略，详见：{{ $.keyword("configuration/common-strategy", ["概念","通用策略"]) }}）。

更新时间策略的全局默认关闭，需要手动开启。

> **Warning**
> 全局设置更新时间后，KPojo中必须拥有该成员属性，否则该策略不会对该表生效。

```kotlin group="Common strategies 2" name="update time" icon="kotlin" {2}
with(Kronos) {
    updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))
}

data class User(
    @PrimaryKey(true)
    var id: Int? = null,
    var updateTime: LocalDateTime? = null
    // 若没有updateTime属性，则更新时间策略不会对该表生效
) : KPojo
```

> **Note**
> 全局设置更新时间策略后，仍可在`KPojo`类中通过{{ $.keyword("mapping/annotations", ["注解设置","@UpdateTime更新时间列"]) }}覆盖全局设置。

## 逻辑删除策略

用于设置所有表的逻辑删除字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

**参数**：
{{$.params([['logicDeleteStrategy', '逻辑删除策略', 'KronosCommonStrategy', 'KronosCommonStrategy(enabled = false, field = Field("deleted"))']])}}

通过创建`KronosCommonStrategy`自定义逻辑删除策略，详见：{{ $.keyword("configuration/common-strategy", ["概念","通用策略"]) }}）。

逻辑删除策略的全局默认关闭，需要手动开启。

> **Warning**
> 全局设置逻辑删除后，KPojo中必须拥有该成员属性，否则该策略不会对该表生效。

```kotlin group="Common strategies 3" name="logic delete" icon="kotlin" {2}
with(Kronos) {
    logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
}

data class User(
    @PrimaryKey(true)
    var id: Int? = null,
    var deleted: Boolean? = null
    // 若没有deleted属性，则逻辑删除策略不会对该表生效
) : KPojo
```

```sql group="Common strategies 3" name="logic delete sql" icon="mysql"
SELECT `id`, `name`, `deleted`
FROM `user`
WHERE `user`.`deleted` = 0
```

> **Note**
> 全局设置逻辑删除策略后，仍可在`KPojo`类中通过{{ $.keyword("mapping/annotations", ["注解设置","@LogicDelete逻辑删除列"]) }}覆盖全局设置。

## 乐观锁（版本）策略

用于设置所有表的乐观锁版本字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

**参数**：
{{$.params([
['optimisticLockStrategy',
'乐观锁（版本）策略，包含<b>是否开启</b>、<b>kotlin属性名</b>及<b>数据库列名等信息</b>',
'KronosCommonStrategy',
'KronosCommonStrategy(enabled = false, field = Field("version"))']
])}}

通过创建`KronosCommonStrategy`自定义乐观锁（版本）策略，详见：{{ $.keyword("configuration/common-strategy", ["概念","通用策略"]) }}）。

乐观锁策略的全局默认关闭，需要手动开启。

> **Warning**
> 全局设置乐观锁后，KPojo中必须拥有该成员属性，否则该策略不会对该表生效。

```kotlin group="Common strategies 4" name="version" icon="kotlin" {2}
with(Kronos) {
    optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
}

data class User(
    @PrimaryKey(true)
    var id: Int? = null,
    var version: Int? = null
    // 若没有version属性，则乐观锁策略不会对该表生效
) : KPojo
```

> **Note**
> 全局设置乐观锁策略后，仍可在`KPojo`类中通过{{ $.keyword("mapping/annotations", ["注解设置","@Version乐观锁（版本）列"]) }}覆盖全局设置。

## 默认日期时间格式

用于指定默认日期时间格式，默认值为 `yyyy-MM-dd HH:mm:ss`。

**参数**：
{{$.params([['defaultDateFormat', '默认日期时间格式', 'String', 'yyyy-MM-dd HH:mm:ss']])}}

Kronos默认使用`yyyy-MM-dd HH:mm:ss`格式化日期/时间，你可以通过以下方式修改默认格式：

```kotlin group="Time 1" name="format" icon="kotlin"
with(Kronos) {
    defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
}
```

> **Note**
> 全局设置默认日期格式后，仍可在`KPojo`类中通过{{ $.keyword("mapping/annotations", ["注解设置", "@DateTimeFormat日期时间格式"]) }}覆盖全局设置。

## 默认时区

用于指定默认时区，遵循`ISO 8601`规范，在创建时间、更新时间及格式化日期/时间时使用。

**参数**：
{{$.params([['timeZone', '默认时区', 'java.time.ZoneId', 'ZoneId.systemDefault()']])}}

Kronos默认使用当前系统时区，你可以通过以下方式修改默认时区：

```kotlin group="Time 2" name="zone" icon="kotlin"
import java.time.ZoneId

with(Kronos) {
    timeZone = ZoneId.of("UTC")
    timeZone = ZoneId.of("Asia/Shanghai")
    timeZone = ZoneId.systemDefault()
    timeZone = ZoneId.of("GMT+8")
}
```

## 无值策略

单个条件需要局部空值规则时，在条件表达式上使用 `.ifNoValue(...)`。

```kotlin group="No value 1" name="ignore" icon="kotlin"
val name: String? = null

val users = User()
    .select()
    .where { (it.name == name).ifNoValue(NoValueStrategyType.Ignore) }
    .toList()
```

```sql group="No value 1" name="ignore sql" icon="mysql"
SELECT `id`, `name`
FROM `user`
```

需要生成 SQL `IS NULL` 时，使用 `== null` 或 `isNull`。

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
> 在 `DELETE` 和 `UPDATE` 操作中请谨慎使用 `NoValueStrategyType.Ignore`，以免造成全表删除或全表更新。

详见：{{ $.keyword("configuration/no-value-strategy", ["概念","无值策略"]) }}。

## 序列化反序列化处理器

将数据库中的字符串在查询时反序列化为对象，在插入数据库时自动序列化对象。

**参数**：
{{$.params([['serializeProcessor', '序列化反序列化处理器', 'KronosSerializeProcessor', 'NoneSerializeProcessor']])}}

通过创建`KronosSerializeProcessor`自定义序列化解析器，详见：{{ $.keyword("configuration/serialization-processor", ["自动序列化与反序列化"])}}。

如可以通过引入`GSON`库来实现序列化解析器：

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
    // 使用GSON序列化对象
    override fun serialize(obj: Any, kType: KType): String {
        return Gson().toJson(obj)
    }
    
    // 使用GSON反序列化对象
    override fun deserialize(serializedStr: String, kType: KType): Any {
        return Gson().fromJson(serializedStr, kType.javaType)
    }
}
```

这里使用 `GSON` 库实现序列化反序列化解析器。你也可以使用 `Kotlinx.serialization`、`Jackson`、`Moshi`、`FastJson` 等库；处理器会收到字段声明上的 `KType`，因此可以显式处理泛型字段。

## 日志输出路径及开关

用于设置全局默认条件下的日志输出位置。

**参数**：
{{$.params([['logPath', '日志输出路径', 'List<String>', 'listOf("console")']])}}

设置 `logPath` 后，Kronos 会向控制台和指定路径输出日志。

```kotlin group="Logging 1" name="console and file" icon="kotlin"
with(Kronos) {
    logPath = listOf("console", "/var/log/kronos")
}
```

使用空数组关闭内置日志输出。

```kotlin group="Logging 2" name="off" icon="kotlin"
with(Kronos) {
    logPath = emptyList()
}
```

## 关闭智能值转换

Kronos在进行数据操作时，会自动将预期值与实际值进行智能转换，如`Int`与`Long`、`String`等等，详见：{{ $.keyword("configuration/value-transformer", ["概念", "值转换器"]) }}。

以下是一个简单的例子，展示了智能值转换的功能：
    
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
// strictResult.isFailure == true，因为 id 是 Long，User.id 需要 Int

val user = mapOfUser.safeMapperTo<User>()
// User(id = 1, name = "Kronos", createTime = LocalDateTime(...))
```

Kronos默认开启`getTypeSafeValue`以及`safeMapperTo`函数进行智能值转换。

当安全赋值需要保留 Map 原始值，并由目标属性赋值过程直接校验类型时，设置 `strictSetValue = true`。

```kotlin
with(Kronos) {
    strictSetValue = true
}
```
