{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Configure one shared strategy

`KronosCommonStrategy` describes a shared field used by creation time, update time, logical delete, and optimistic lock configuration. Set `enabled = true` and pass the database column plus Kotlin property name through `Field`.

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

This strategy points to the `create_time` database column and the `createTime` Kotlin property. The strategy only applies to KPojo models that expose the configured property.

```sql group="CommonStrategy 1 2" name="column" icon="mysql"
`create_time`
```

## Parameters

{{ $.members([
    ["enabled", "Whether to enable the strategy.", "Boolean"],
    ["field", "Field metadata. The first argument is the database column name, and the second argument is the Kotlin property name.", "Field"]
]) }}

Set `enabled = false` when the global strategy should stay inactive. This keeps the configured `Field` metadata available without applying the behavior to insert, update, delete, select, or upsert operations.

```kotlin group="CommonStrategy 2" name="disabled" icon="kotlin"
with(Kronos) {
    logicDeleteStrategy = KronosCommonStrategy(
        enabled = false,
        field = Field("deleted")
    )
}
```

## Use the same pattern for other fields

Use the same structure for update time, logical delete, and optimistic lock fields.

```kotlin group="CommonStrategy 3" name="global fields" icon="kotlin"
with(Kronos) {
    updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))
    logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
    optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
}
```

For the complete global configuration surface, see {{ $.keyword("configuration/global-config", ["Global Config"]) }}.
