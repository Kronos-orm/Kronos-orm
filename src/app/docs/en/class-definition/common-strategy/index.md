# {{ NgDocPage.title }}

`KronosCommonStrategy` is a common configuration strategy interface for update time/create time/logical deletion configuration strategies.

## Parameters:

### <span style="color: #DD6666">enable</span>

`Boolean` type, indicating whether the strategy is enabled.

### <span style="color: #DD6666">field</span>

`Field` type, indicating the corresponding field name.

The field name attributes that need to be specified include `name` and `columnName`.

```kotlin name="demo" icon="kotlin"
KronosCommonStrategy(
        enable = true,
        field = Field(
        name = "createTime",
        columnName = "create_time" //If not specified here, the default is the result of the column name conversion performed by the value of `name`
    )
)
```