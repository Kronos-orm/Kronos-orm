{% import "../../../macros/macros-en.njk" as $ %}

## Parameters:

{{ $.members([
    ["enable", "Whether to enable the strategy", "Boolean"],
    ["field", "Field name, including `name` and `columnName`", "Field"]
]) }}

## Example Usage:

```kotlin name="demo" icon="kotlin"
KronosCommonStrategy(
    enable = true,
    field = Field(
        name = "createTime",
        columnName = "create_time" //If not specified here, it defaults to the result of column name conversion based on the value of `name`.
    )
)

```

