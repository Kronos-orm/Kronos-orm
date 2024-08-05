# {{ NgDocPage.title }}

## 1.Criteria 条件对象

Kronos使用Criteria对象构建条件表达式，并且支持复杂的条件组合，如`and`、`or`、`not`等，用于`where`、`having`、`on`等条件中。

你可以使用where条件对象组成复杂的查询条件在`select`、`delete`、`update`、`join`中使用，形如

```kotlin
val list = user
    .select { it.name }
    .where { (it.id == 1 || it.age > 18) && it.name like "Kronos%" }
    .queryList<String>()
```

基于KCP，Kronos允许你使用真实的kotlin操作符来构建`Criteria`查询条件，如`==`、`!=`、`>`、`<`、`>=`、`<=`、`in`、`||` 、`&&`等，而不是其他框架中`eq`、`ne`、`gt`、`lt`、`ge`、`le`等自定义操作符，而从表达式到Criteria对象的转换是在编译期完成的。

kronos表达式支持**动态构建条件**和根据对象的属性**自动生成条件**，并且支持传入**sql字符串**作为条件。

使用kronos就像在写原生的kotlin代码一样，这将大大**提高开发效率**和**降低学习成本及心智负担**。

## 2.支持的函数和操作符

Kronos支持的函数和操作符如下：

### 2.1.操作符

- `==`：等于
- `!=`：不等于
- `>`：大于
- `<`：小于
- `>=`：大于等于
- `<=`：小于等于
- `in`：在范围内，也可以使用`contains`代替，如`a in b`可以写成`b.contains(a)`
- `||`：或
- `&&`：与
- `!`：非，可以与其他函数和操作符一起使用，如`!(a == 1 || a == 2) && a !in listOf(3, 4)`

### 2.2.函数

#### between

在范围内，接收ClosedRange类型的参数

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

无值策略，接收参数`NoValueStrategy`，用于处理无值的情况，优先级高于Kronos默认的无值策略，详见：[无值策略](/documentation/database/no-value-strategy)

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

