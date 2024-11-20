{% import "../../../macros/macros-zh-CN.njk" as $ %}

## {{ $.title("NoValueStrategy") }} 无值策略

无值策略是查询条件语句Criteria中的一种策略，当条件语句为二元操作符时(即可以接收变量参数)，如果参数为`null`，那么将会使用无值策略生成SQL语句。

## 自定义全局默认的无值策略

`Kronos`默认的无值策略为`DefaultNoValueStrategy`，它的主要逻辑如下：

- 当操作类型为`UPDATE`或`DELETE`时：
    - 如果条件类型为相等判断，则转换为`is null`或`is not null`
    - 如果条件类型为like、in、between或数值判断，则直接认定条件为当前条件的相反值
    - 如果条件为大于、大于等于、小于、小于等于，则认定条件为false
    - 其他情况忽略该条件语句
- 当操作类型为`SELECT`时，忽略该条件语句

创建一个自定义的无值策略，只需要实现`NoValueStrategy`接口即可，如下所示：

```kotlin group="custom" name="YourCustomNoValueStrategy.kt"
object YourCustomNoValueStrategy : NoValueStrategy {
    fun ifNoValue(kOperateType: KOperationType, criteria: Criteria): NoValueStrategyType {
        // your logic
    }
}
```

```kotlin group="custom" name="Main.kt"
Kronos.noValueStrategy = YourCustomNoValueStrategy
```

## 动态设置无值策略

只需在条件语句中调用`ifNoValue`方法即可，如下所示：

```kotlin
where { (it.age == null).ifNoValue(Ignore) }
```

## 无值策略的类型

目前支持的无值策略有：

{{ $.params([
['Ignore', '忽略该条件语句', "NoValueStrategyType", "ignore"],
['False', '条件语句为false', "NoValueStrategyType", "false"],
['True', '条件语句为true', "NoValueStrategyType", "true"],
['JudgeNull', '转换为`is null`或`is not null`', "NoValueStrategyType", "judgeNull"],
['Auto', '默认策略，根据全局默认策略生成SQL语句，通常不需要手动设置', "NoValueStrategyType", "auto"]
]) }}