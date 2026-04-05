{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 使用场景

`lastInsertId`插件用于在执行插入操作后，获取数据库中最后插入的记录的ID。这在需要获取新创建记录的唯一标识符时非常有用。

> **Note**
> 该插件仅适用于自增主键的表。

## 禁用{{ $.title("lastInsertId") }}插件

默认地，`lastInsertId`插件是开启的。你可以在Kronos.init中进行配置：

```kotlin
Kronos.init {
    LastInsertId.enabled = false // 禁用lastInsertId插件
}
```

在全局配置中禁用后，所有的数据库操作都将不再返回最后插入的ID。但是可以使用`withId`方法来临时启用该插件：

```kotlin
KPojo().insert().withId().execute()
```
