`KronosCommonStrategy`是更新时间/创建时间/逻辑删除配置策略的通用配置策略接口。

## 参数：

### <span style="color: #DD6666">enable</span>

`Boolean`类型，表示是否启用该策略。

### <span style="color: #DD6666">field</span>

`Field`类型，表示对应的字段名。

其中需要指定的字段名属性包括`name`和`columnName`。

```kotlin name="demo" icon="kotlin"
KronosCommonStrategy(
    enable = true,
    field = Field(
        name = "createTime",
        columnName = "create_time" //此处若不指定，则默认为`name`的值执行列名转换后的结果
    )
)

```

