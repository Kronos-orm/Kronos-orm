{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 配置一个通用字段策略

`KronosCommonStrategy` 用于描述创建时间、更新时间、逻辑删除和乐观锁等通用字段。设置 `enabled = true`，并通过 `Field` 传入数据库列名和 Kotlin 属性名。

```kotlin group="CommonStrategy 1 1" name="create time" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

with(Kronos) {
    createTimeStrategy = KronosCommonStrategy(
        enabled = true,
        field = Field("create_time", "createTime")
    )
}
```

该策略指向 `create_time` 数据库列和 `createTime` Kotlin 属性。策略只会作用于包含该属性的 KPojo 模型。

```sql group="CommonStrategy 1 2" name="column" icon="mysql"
`create_time`
```

## 参数

{{ $.members([
    ["enabled", "是否启用该策略。", "Boolean"],
    ["field", "字段元数据。第一个参数是数据库列名，第二个参数是 Kotlin 属性名。", "Field"]
]) }}

全局策略不需要生效时，将 `enabled` 设置为 `false`。这样仍保留 `Field` 元数据，但不会把该行为应用到 insert、update、delete、select 或 upsert 操作。

```kotlin group="CommonStrategy 2" name="disabled" icon="kotlin"
with(Kronos) {
    logicDeleteStrategy = KronosCommonStrategy(
        enabled = false,
        field = Field("deleted")
    )
}
```

## 复用到其他通用字段

更新时间、逻辑删除和乐观锁字段使用同样的结构配置。

```kotlin group="CommonStrategy 3" name="global fields" icon="kotlin"
with(Kronos) {
    updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))
    logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
    optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
}
```

完整全局配置见 {{ $.keyword("configuration/global-config", ["全局配置"]) }}。
