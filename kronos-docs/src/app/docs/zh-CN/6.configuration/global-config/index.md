{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

先用下表选择需要配置的全局项。

| 层级 | 配置项 | 入口 |
|------|--------|------|
| 必须 | 默认数据源 wrapper | [默认数据源设置](#默认数据源设置) |
| 常用 | 表名/列名策略、默认日期格式、时区、日志 | [全局表名策略](#全局表名策略)、[全局列名策略](#全局列名策略)、[默认日期时间格式](#默认日期时间格式)、[默认时区](#默认时区)、[日志输出路径及开关](#日志输出路径及开关) |
| 按业务启用 | 通用字段策略、无值行为、JSON 序列化、自定义值映射、Map 值转换 | [创建时间策略](#创建时间策略)、[逻辑删除策略](#逻辑删除策略)、[无值策略](#无值策略)、{{ $.keyword("mapping/serialization", ["序列化"]) }}、[自定义值映射](#自定义值映射)、[Map 值转换](#map-值转换) |

编译插件配置在构建工具中完成。source set 和诊断检查见 {{ $.keyword("configuration/compiler-plugins", ["编译器插件"]) }}。

## 默认数据源设置

在执行 ORM 操作前设置 `Kronos.dataSource`。没有显式传入 wrapper 的操作，会调用这个函数取得默认数据源。

数据源 wrapper 的创建方式见 {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }}。

```kotlin group="Data source 1" name="default" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper = KronosJdbcWrapper(BasicDataSource())

Kronos.dataSource = { wrapper }
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
Kronos.dataSource = {
    if (TenantContext.current() == "archive") archiveWrapper else primaryWrapper
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
Kronos.tableNamingStrategy = lineHumpNamingStrategy
```

```text group="Naming 1" name="result"
UserProfile -> user_profile
```

### {{ $.title("NoneNamingStrategy") }} 表名

`NoneNamingStrategy` 保持 Kotlin 类名和数据库名称原样。Kronos 默认使用该策略。

```kotlin group="Naming 2" name="none" icon="kotlin"
Kronos.tableNamingStrategy = noneNamingStrategy
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
Kronos.fieldNamingStrategy = lineHumpNamingStrategy
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

```kotlin group="Common strategies 1" name="create time" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

Kronos.createTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("create_time", "createTime"))
```

> **Note**
> 在单个模型上配置 `@CreateTime` 可以覆盖全局设置，见 {{ $.keyword("mapping/annotations", ["注解"]) }}。

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

```kotlin group="Common strategies 2" name="update time" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

Kronos.updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))
```

> **Note**
> 在单个模型上配置 `@UpdateTime` 可以覆盖全局设置，见 {{ $.keyword("mapping/annotations", ["注解"]) }}。

## 逻辑删除策略

用于设置所有表的逻辑删除字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

**参数**：
{{$.params([['logicDeleteStrategy', '逻辑删除策略', 'KronosCommonStrategy', 'KronosCommonStrategy(enabled = false, field = Field("deleted"))']])}}

通过创建`KronosCommonStrategy`自定义逻辑删除策略，详见：{{ $.keyword("configuration/common-strategy", ["概念","通用策略"]) }}）。

逻辑删除策略的全局默认关闭，需要手动开启。

> **Warning**
> 全局设置逻辑删除后，KPojo中必须拥有该成员属性，否则该策略不会对该表生效。

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
> 在单个模型上配置 `@LogicDelete` 可以覆盖全局设置，见 {{ $.keyword("mapping/annotations", ["注解"]) }}。

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

```kotlin group="Common strategies 4" name="version" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

Kronos.optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
```

> **Note**
> 在单个模型上配置 `@Version` 可以覆盖全局设置，见 {{ $.keyword("mapping/annotations", ["注解"]) }}。

## 默认日期时间格式

用于指定默认日期时间格式，默认值为 `yyyy-MM-dd HH:mm:ss`。

**参数**：
{{$.params([['defaultDateFormat', '默认日期时间格式', 'String', 'yyyy-MM-dd HH:mm:ss']])}}

Kronos默认使用`yyyy-MM-dd HH:mm:ss`格式化日期/时间，你可以通过以下方式修改默认格式：

```kotlin group="Time 1" name="format" icon="kotlin"
Kronos.defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
```

> **Note**
> 在单个模型上配置 `@DateTimeFormat` 可以覆盖全局设置，见 {{ $.keyword("mapping/annotations", ["注解"]) }}。

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

Kotlin 条件为 true 时使用 `.takeIf(...)` 保留谓词，条件为 false 时使用 `.takeUnless(...)` 保留谓词。

```kotlin group="No value 1" name="ignore" icon="kotlin"
val name: String? = null

val users = User()
    .select()
    .where { (it.name == name).takeIf(name != null) }
    .toList()
```

Boolean 判定按普通 Kotlin 代码执行。例如 `(it.status == 0).takeUnless(includeInactive)` 会在 `includeInactive` 为 true 时跳过状态谓词。

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

详见 {{ $.keyword("configuration/no-value-strategy", ["无值处理"]) }}。

## 自定义值映射

模型属性使用 `Money` 等领域值、数据库列保存标量值时，在应用启动时注册映射。完整的 `Money` 示例见 {{ $.keyword("configuration/value-codec", ["自定义值映射"]) }}。

JSON 对象和集合使用 {{ $.annotation("Serialize") }}，配置方式见 {{ $.keyword("mapping/serialization", ["序列化"]) }}。

## 日志输出路径及开关

用于设置全局默认条件下的日志输出位置。

**参数**：
{{$.params([['logPath', '日志输出路径', 'List<String>', 'listOf("console")']])}}

设置 `logPath` 后，Kronos 会向控制台和指定路径输出日志。输出位置和 logger 适配器见 {{ $.keyword("configuration/logging", ["Kronos-logging"]) }}。

```kotlin group="Logging 1" name="console and file" icon="kotlin"
Kronos.logPath = listOf("console", "/var/log/kronos")
```

## Map 值转换

应用输入是 `Map` 时，使用 `safeMapperTo` 创建新模型，使用 `safeFromMapData` 填充已有模型。两个 API 都会将 Map key 与模型属性匹配，并转换兼容的值。完整的 Map 与模型、模型与模型 API 参考见 {{ $.keyword("advanced/mapper-to", ["Map/KPojo类型转换"]) }}。

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

输入边界已经完成值规范化时，设置 `strictSetValue = true`。开启后，内置数值、布尔值和日期时间值在符合模型属性类型时可以直接使用；已注册的自定义映射仍然可用。例如，`User.id: Int?` 接受 `Int` 输入，传入 `Long` 时会报告映射错误。

```kotlin
import com.kotlinorm.Kronos

Kronos.strictSetValue = true
```
