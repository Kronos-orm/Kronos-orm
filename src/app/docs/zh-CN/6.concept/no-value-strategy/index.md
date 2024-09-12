1. 什么是无值策略

无值策略是查询条件语句Criteria中的一种策略，当条件语句可接收右值时，如果右值为null，那么将会使用无值策略。

2.哪些条件语句支持无值策略

目前支持无值策略的条件语句有：`==`、`!=`、`>`、`<`、`>=`、`<=`、`in`、`contains`、`like`、`between`、`notBetween`、`like`、`notLike`、`matchLeft`、`matchRight`、`matchBoth`、`regexp`、`asSql`等。

3.如何使用无值策略

```kotlin
where { (it.age == null).ifNoValue(ignore) }
```

4.无值策略的类型

目前支持的无值策略有：
- `Ignore`(ignore)：忽略该条件语句
- `False`(alwaysFalse)：条件语句为false
- `True`(alwaysTrue)：条件语句为true
- `JudgeNull`(judgeNull)：转换为`is null`或`is not null`
- `Smart`(smart)：根据当前语境自动判断

5.默认无值策略smart的判断逻辑

当条件语句可接收右值时，如果右值为null，那么将会使用无值策略。

当条件类型为相等判断时，将其修改为对应的null判断，如`==`修改为`is null`，`!=`修改为`is not null`。
当条件为like、in、between或数值判断时，直接认定条件为假。


