{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

本文将指导您如何全局配置Kronos。

## 默认数据源设置

在数据库操作时，若不指定数据源，kronos会自动使用该默认数据源。

数据源类型为`KronosDataSourceWrapper`，创建方法可参考：[连接到数据库](/documentation/zh-CN/database/connect-to-db)

```kotlin
Kronos.dataSource = { yourDataSourceWrapper }
```

> **Warning**
> 默认数据源的默认值为`NoneDataSourceWrapper`，在使用前请务必修改您的配置文件。
>
> Kronos支持多数据源与动态数据源
>
> **多数据源**：在具体操作时，如KPojo.update.execute(wrapper)，可以在`execute`函数中传入其他`KronosDataSourceWrapper`
> 实例，从而使用其他的数据源。
>
> **动态数据源**：`Kronos.dataSource`是一个函数，您可以在该函数中实现您的逻辑返回不同的数据源实例，实现动态数据源。

## 全局表名策略

表名策略指在默认情况下（无注解配置），kronos自动根据**Kotlin类名**生成数据库的**表名**，如：`User` -> `user`。

| 参数名                   | 类型                     | 默认值                  |
|-----------------------|------------------------|----------------------|
| `tableNamingStrategy` | `KronosNamingStrategy` | `NoneNamingStrategy` |

通过创建`KronosNamingStrategy`
的实现类来自定义表名策略（详见：[命名策略](/documentation/zh-CN/class-definition/naming-strategy)），然后在配置文件中指定该实现类。

我们默认提供了`LineHumpNamingStrategy`和`NoneNamingStrategy`两种表名策略：

1. **下划线/驼峰命名策略（`LineHumpNamingStrategy`）**

该策略将kotlin类名转换为下划线分隔的小写字符串，如：`ADataClass` -> `a_data_class`
，将数据库表/列名转为驼峰命名，如：`user_name` -> `userName`。

```kotlin
Kronos.tableNamingStrategy = LineHumpNamingStrategy
```

2. **无命名策略（`NoneNamingStrategy`）**

该策略将kotlin类名保持原样，如：`ADataClass` -> `ADataClass`，将数据库表/列名保持原样，如：`user_name` -> `user_name`。

默认情况下，kronos使用的就是`NoneNamingStrategy`表名策略。

## 全局列名策略

同全局表名策略类似，列名策略指在默认情况下，kronos自动根据Kotlin类的**属性名**生成**列名**，如：`classId` -> `class_id`。

| 参数名                   | 类型                     | 默认值                  |
|-----------------------|------------------------|----------------------|
| `fieldNamingStrategy` | `KronosNamingStrategy` | `NoneNamingStrategy` |

列名策略类与表名策略通用，设置方式为：

```kotlin
Kronos.fieldNamingStrategy = LineHumpNamingStrategy
```

## 创建时间策略

用于设置所有表的创建时间字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

| 参数名                  | 类型                     | 默认值                                         |
|----------------------|------------------------|---------------------------------------------|
| `createTimeStrategy` | `KronosCommonStrategy` | `KronosCommonStrategy(false, "createTime")` |

通过创建`KronosCommonStrategy`
的实现类来自定义创建时间策略（详见：[通用策略](/documentation/class-definition/common-strategy)），然后在配置文件中指定该实现类。

创建时间策略的全局**默认关闭**，需要手动开启。

```kotlin
Kronos.createTimeStrategy = KronosCommonStrategy(true, Field("createTime"))
```

> **Note**
> 全局设置创建时间策略后，仍可在`KPojo`类中通过`@CreateTime`注解覆盖全局设置。

## 更新时间策略

用于设置所有表的更新时间字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

| 参数名                  | 类型                     | 默认值                                         |
|----------------------|------------------------|---------------------------------------------|
| `updateTimeStrategy` | `KronosCommonStrategy` | `KronosCommonStrategy(false, "updateTime")` |

通过创建`KronosCommonStrategy`
的实现类来自定义更新时间策略（详见：[通用策略](/documentation/class-definition/common-strategy)），然后在配置文件中指定该实现类。

更新时间策略的全局默认关闭，需要手动开启。

```kotlin
Kronos.updateTimeStrategy = KronosCommonStrategy(true, Field("updateTime"))
```

> **Note**
> 全局设置逻更新时间策略后，仍可在`KPojo`类中通过`@UpdateTime`注解覆盖全局设置。

## 逻辑删除策略

用于设置所有表的逻辑删除字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

| 参数名                   | 类型                     | 默认值                                      |
|-----------------------|------------------------|------------------------------------------|
| `logicDeleteStrategy` | `KronosCommonStrategy` | `KronosCommonStrategy(false, "deleted")` |

通过创建`KronosCommonStrategy`
的实现类来自定义逻辑删除策略（详见：[通用策略](/documentation/class-definition/common-strategy)），然后在配置文件中指定该实现类。

逻辑删除策略的全局默认关闭，需要手动开启。

```kotlin
Kronos.logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
```

> **Note**
> 全局设置逻辑删除策略后，仍可在`KPojo`类中通过`@LogicDelete`注解覆盖全局设置。

## 乐观锁（版本）策略

用于设置所有表的乐观锁版本字段（**是否开启**、**kotlin属性名**及**数据库列名**）。

| 参数名                      | 类型                     | 默认值                                      |
|--------------------------|------------------------|------------------------------------------|
| `optimisticLockStrategy` | `KronosCommonStrategy` | `KronosCommonStrategy(false, "version")` |

通过创建`KronosCommonStrategy`
的实现类来自定义乐观锁策略（详见：[通用策略](/documentation/class-definition/common-strategy)），然后在配置文件中指定该实现类。

也可通过<a href="/documentation/class-definition/table-class-definition#列乐观锁">[列乐观锁]</a>对每一个实体对象单独配置

乐观锁策略的全局默认关闭，需要手动开启。

```kotlin
Kronos.optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
```

> **Note**
> 全局设置乐观锁策略后，仍可在`KPojo`类中通过`@Version`注解覆盖全局设置。

## 默认日期/时间格式

用于指定日期格式化的默认格式，遵循`ISO 8601`规范，默认为`yyyy-MM-dd HH:mm:ss`。

| 参数名                 | 类型       | 默认值                   |
|---------------------|----------|-----------------------|
| `defaultDateFormat` | `String` | `yyyy-MM-dd HH:mm:ss` |

Kronos默认使用`yyyy-MM-dd HH:mm:ss`格式化日期/时间，你可以通过以下方式修改默认格式：

```kotlin
Kronos.defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
```

> **Note**
> 全局设置默认日期格式后，仍可在`KPojo`类中通过`@DateTimeFormat`注解覆盖全局设置。

## 默认时区

用于指定默认时区，在创建时间、更新时间及格式化日期/时间时使用。

| 参数名        | 类型                 | 默认值                      |
|------------|--------------------|--------------------------|
| `timeZone` | `java.time.ZoneId` | `ZoneId.systemDefault()` |

Kronos默认使用当前系统时区，你可以通过以下方式修改默认时区：

```kotlin
Kronos.timeZone = ZoneId.of("UTC")
Kronos.timeZone = ZoneId.of("Asia/Shanghai")
Kronos.timeZone = ZoneId.systemDefault()
Kronos.timeZone = ZoneId.of("GMT+8")
```

## 序列化解析器

将数据库中的字符串在查询时反序列化为对象，在插入数据库时自动序列化对象。

| 参数名                 | 类型                        | 默认值                     |
|---------------------|---------------------------|-------------------------|
| `serializeResolver` | `KronosSerializeResolver` | `NoneSerializeResolver` |

通过创建`KronosSerializeResolver`
的实现类来自定义序列化解析器（详见：[序列化解析器](/documentation/zh-cn/class-definition/serialize-resolver)），然后在配置文件中指定该实现类。

如可以通过引入`GSON`库来实现序列化解析器：

```kotlin group="GsonResolver" name="GsonResolver.kt" icon="kotlin"
object GsonResolver : KronosSerializeResolver {
    override fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T {
        return Gson().fromJson<T>(serializedStr, kClass.java)
    }

    override fun deserializeObj(serializedStr: String, kClass: KClass<*>): Any {
        return Gson().fromJson(serializedStr, kClass.java)
    }

    override fun serialize(obj: Any): String {
        return Gson().toJson(obj)
    }
}
```

```kotlin group="GsonResolver" name="KronosConfig.kt" icon="kotlin"
Kronos.serializeResolver = GsonResolver
```

这里我们使用`GSON`库来实现序列化解析器，你可以使用任何库如`Kotlinx.serialization`、`Jackson`、`Moshi`、`FastJson`等。

