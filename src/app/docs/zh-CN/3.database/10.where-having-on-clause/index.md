{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Criteria 条件对象

Kronos使用Criteria对象构建条件表达式，并且支持复杂的条件组合，如`&&`、`||`、`!`等，用于`where`、`having`、`on`等条件中。

你可以使用where条件对象组成复杂的查询条件在`select`、`delete`、`update`、`join`功能中需要条件的方法中。

```kotlin
val list = user
    .select { it.name }
    .where { (it.id == 1 || it.age > 18) && it.name like "Kronos%" }
    .queryList<String>()
```

基于KCP，Kronos允许你使用真实的kotlin操作符来构建`Criteria`查询条件，如`==`、`!=`、`>`、`<`、`>=`、`<=`、`in`、`||` 、`&&`等，提供了超级富有表现力、简洁而又语义化的写法。

kronos表达式支持**动态构建条件**和根据对象的属性**自动生成条件**，并且支持传入**sql字符串**作为条件。

使用kronos就像在写原生的kotlin代码一样，这将大大**提高开发效率**和**降低学习成本及心智负担**。

## 如何构建表达式

Kronos支持的函数和操作符如下：

### 条件操作符

- {{ $.title("==") }}：等于
- {{ $.title("!=") }}：不等于
- {{ $.title(">") }}：大于
- {{ $.title("<") }}：小于
- {{ $.title("=") }}：大于等于
- {{ $.title("=") }}：小于等于
- {{ $.title("in") }}：在范围内，也可以使用`contains`代替，如`a in b`可以写成`b.contains(a)`
- {{ $.title("||") }}：或
- {{ $.title("&&") }}：与
- {{ $.title("!") }}：非，可以与其他函数和操作符一起使用，如`!(a == 1 || a == 2) && a !in listOf(3, 4)`
- {{ $.title("()") }}：括号，用于改变优先级

### 特殊表达

#### {{ $.title("KPojo.xxx.value") }} 获取字段值

在where表达式中，`user.name`表示字段`name`，`it.name.value`表示kotlin中`User`对象的`name`属性值

```kotlin {3,5}
val user = User(username = "Kronos")

User().select().where { it.username == user.username.value }.query()
//等同于
User().select().where { it.username == "Kronos" }.query()
```

#### {{ $.title("KPojo.eq") }} 通过KPojo自动生成判等条件

`user.eq`表示通过`User`为对象内所有非空字段自动生成判等条件，如`it.eq`等同于`it.id == id && it.name == name && it.age == age`，他可以与`&&`、`||`、`!`等操作符组合使用。

```kotlin {3}
val user = User(id = 1, name = "Kronos", age = 18)

User().select().where { it.eq || it.name like "Kronos%" }.query()
```

### 条件函数

#### between

在范围内，接收{{$.code("ClosedRange<*>")}}类型的参数

```kotlin
where { it.age.between(1..10) }
//支持中缀调用
where { it.age between 1..10 }
```

#### notBetween

不在范围内，接收ClosedRange类型的参数

```kotlin
where { it.age.notBetween(1..10) }
//支持中缀调用
where { it.age notBetween 1..10 }
```

#### like

模糊查询，接收String类型的参数

```kotlin
where { it.name.like("Kronos%") }
//支持中缀调用
where { it.name like "Kronos%" }
```

#### matchLeft

左模糊查询，接收String类型的参数，可以不传入参数，如`it.name.matchLeft()` 或 `it.name.matchLeft`

```kotlin
where { it.name.matchLeft("Kronos") }
//支持中缀调用
where { it.name matchLeft "Kronos" }
//等同于
where { it.name like "Kronos%" }
```

#### matchRight

右模糊查询，接收String类型的参数，可以不传入参数，如`it.name.matchRight()` 或 `it.name.matchRight`

```kotlin
where { it.name.matchRight("Kronos") }
//支持中缀调用
where { it.name matchRight "Kronos" }
//等同于
where { it.name like "%Kronos" }
```

#### matchBoth

全模糊查询，接收String类型的参数，可以不传入参数，如`it.name.matchBoth()` 或 `it.name.matchBoth`

```kotlin
where { it.name.matchBoth("Kronos") }
//支持中缀调用
where { it.name matchBoth "Kronos" }
//等同于
where { it.name like "%Kronos%" }
```
#### notLike

不匹配，接收String类型的参数

```kotlin
where { it.name.notLike("Kronos%") }
//支持中缀调用
where { it.name notLike "Kronos%" }
```

#### isNull

为空

```kotlin
where { it.name.isNull }
```

#### notNull

不为空

```kotlin
where { it.name.notNull }
```

#### regexp

正则表达式查询，接收String类型的参数

```kotlin
where { it.name.regexp("Kronos.*") }
// 支持中缀调用
where { it.name regexp "Kronos.*" }
```

#### asSql

自定义SQL查询条件，接收String类型的参数

```kotlin
where { "name = 'Kronos' and age > 18".asSql() }
```

#### eq

等于，等同于`==`，可以不传入参数，如`it.name.eq()` 或 `it.name.eq`

```kotlin
where { it.name.eq("Kronos") }
User(name = 'Kronos').select().where { it.name.eq } // 等同于 where { it.name.eq("Kronos") }
```

#### notEq

不等于，等同于`!=`，但是可以不传入参数，如`it.name.notEq()` 或 `it.name.notEq`

```kotlin
where { it.name.notEq("Kronos") }
User(name = 'Kronos').select().where { it.name.notEq } // 等同于 where { it.name.notEq("Kronos") }
```

#### gt

大于，等同于`>`，可以不传入参数，如`it.age.gt()` 或 `it.age.gt`

```kotlin
where { it.age.gt(18) }
User(age = 18).select().where { it.age.gt } // 等同于 where { it.age.gt(18) }
```

#### lt

小于，等同于`<`，可以不传入参数，如`it.age.lt()` 或 `it.age.lt`

```kotlin
where { it.age.lt(18) }
User(age = 18).select().where { it.age.lt } // 等同于 where { it.age.lt(18) }
```

#### ge

大于等于，等同于`>=`，可以不传入参数，如`it.age.ge()` 或 `it.age.ge`

```kotlin
where { it.age.ge(18) }
User(age = 18).select().where { it.age.ge } // 等同于 where { it.age.ge(18) }
```

#### le

小于等于，等同于`<=`，可以不传入参数，如`it.age.le()` 或 `it.age.le`

```kotlin
where { it.age.le(18) }
User(age = 18).select().where { it.age.le } // 等同于 where { it.age.le(18) }
```

#### ifNoValue

无值策略，接收参数`NoValueStrategy`，用于处理无值的情况，优先级高于Kronos默认的无值策略，详见：[无值策略](/documentation/zh-CN/database/no-value-strategy)

```kotlin
val age: Int? = null
User()
  .select()
  .where { (it.age == age).ifNoValue(Ignore) } // 无值时忽略条件
  .query()

val username: String? = null
User(username = username)
  .delete()
  .where { it.username.eq.ifNoValue(False) } // 无值时返回false
  .execute()
```

