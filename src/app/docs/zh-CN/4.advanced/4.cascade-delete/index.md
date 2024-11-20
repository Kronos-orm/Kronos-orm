{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 级联删除策略

{{ $.annotation("Cascade") }}注解支持设置级联删除操作的策略，请参考：{{ $.keyword("concept/cascade-delete-action", ["级联删除策略"]) }}。

### 设置默认值

使用{{$.code("SET_DEFAULT")}}策略时，需要在注解中指定`defaultValue`属性

{{$.code("defaultValue")}}接受一个字符串数组，如`["a", "b"]`，对应关联实体的多个引用列的默认值

> **Note**
> 请保证`defaultValue`、`properties`、`targetProperties`数组长度一致

若其中的某个引用列不需要修改，可以通过将其设置为`RESERVED`，Kronos在级联删除设置为`SET_DEFAULT`时会忽略该引用列，如：

```kotlin
data class Child(
    var parentId: Long? = null,
    var parentName: String? = null
    @Cascade(["parentId", "parentName"], ["id", "name"], SET_DEFAULT, [RESERVED, "empty"])
    var parent: Parent? = null
) 
```

### 逻辑删除

若顶层的删除函数使用了逻辑删除功能，若级联删除子层级的级联删除策略为`CASCADE`，那么删除子表数据时也会使用逻辑删除。

## 部分开启及关闭级联删除

### 关闭级联删除

Kronos默认开启级联删除功能，需要在`delete`函数中显式关闭：

```kotlin
KPojo.delete().cascade(enable = false).execute()
```
### 部分开启级联删除

当KPojo中有多个级联声明，但只有部分需要级联删除时，可以将需要级联删除的属性传入`cascade`函数，其余的属性及子属性将不触发级联删除。

```kotlin
// 若KPojo中只有property1和property2需要级联删除，那么如下：
KPojo.delete().cascade(KPojo::property1, KPojo::property2).execute()
```

可以限制其子属性级联删除，如下：

```kotlin
KPojo.delete().cascade(
    KPojo::property1, 
    KPojo::property2, 
    Property1::subProperty1, 
    Property1::subProperty2
).execute()
```

## 逻辑删除

## 1. 一对一级联关系

本节将介绍如何使用Kronos中定义的一对一级联关系的级联删除。

### 数据类定义

我们定义`User`和`Profile`两个实体类，`User`实体类中包含了一个`Profile`实体类的引用，`Profile`实体类中包含了一个`User`
实体类的引用。

```kotlin group="case3" name="User.kt" icon="kotlin"
data class User(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var profileId: Long? = null,
    @Cascade(["profileId"], ["id"], CASCADE)
    var profile: Profile? = null
)
```

```kotlin group="case3" name="Profile.kt" icon="kotlin"
data class Profile(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var userId: Long? = null,
    @Cascade(["userId"], ["id"], CASCADE)
    var user: User? = null
)
```

### 级联删除

删除`User`或`Profile`实体都会触发级联删除

```kotlin
User(id = 1).delete().execute()
// 这将删除所有id=1的User记录和所有userId=1的Profile记录
```

或者：

```kotlin
Profile(id = 1).delete().execute()
// 这将删除所有id=1的Profile记录和所有userId=1的User记录
```

## 2. 一对多级联关系

一对多关系和一对一关系的用法类似。

### 数据类定义

首先，我们定义以下的`Parent`和`Child`两个实体类，`Parent`实体类中包含了一个`Child`实体类的集合，`Child`实体类中包含了一个
`Parent`实体类的引用。

```kotlin group="case1" name="Parent.kt" icon="kotlin"
data class Parent(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null,
    var children: List<Child> = listOf()
)
```

```kotlin group="case1" name="Child.kt" icon="kotlin"
data class Child(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null,
    var parentId: Long? = null,
    @Cascade(["parentId"], ["id"], CASCADE)
    var parent: Parent? = null
)
```

### 级联删除

使用`KPojo.delete()`方法，可以实现一对多级联关系的级联删除。

在Kronos中，仅支持`Parent -> Child`方向的级联删除，删除时先删除Child,再删除Parent，若多层级的任意一层级的关系存在`Restrict`级联删除策略，则不触发删除。

```kotlin
Parent(id = 1).delete().execute()
// 这会删除id为1的Parent记录，同时删除parentId为1的Child记录
```

以下是一个更加复杂的、层级更深的例子：

```kotlin group="case2" name="School.kt" icon="kotlin"
data class School(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var groupClasses: List<GroupClass> = listOf()
)
```

```kotlin group="case2" name="GroupClass.kt" icon="kotlin"
data class GroupClass(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var schoolId: Long? = null,
    @Cascade(["schoolId"], ["id"], CASCADE)
    var school: School? = null,
    var students: List<Student> = listOf()
)
```

```kotlin group="case2" name="Student.kt" icon="kotlin"
data class Student(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var groupClassId: Long? = null,
    var schoolId: Long? = null,
    @Cascade(["groupClassId", "schoolId"], ["id", "schoolId"], CASCADE)
    var groupClass: GroupClass? = null
)
```

```kotlin
School(name = "school").delete().execute()
// 这会级联删除所有name="school"的GroupClass和Student记录
```

## 3. 多对多级联关系

通过`manyToMany`委托，Kronos简化多对多关系为一对多关系，并为您自动创建关联表记录，详见：{{$.keyword("
advanced/cascade-definition", ["级联关系定义", "使用委托实现级联多对多跨中间表关系"] )}}。

### 数据类定义

首先我们定义`User`、`Role`、`Relation`三个实体类，其中`Relation`实体类中包含了`User`和`Role`两个实体类的引用，`User`和`Role`
通过`manyToMany`委托关联。

```kotlin group="case4" name="User.kt" icon="kotlin"
data class User(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var relations: List<Relation> = listOf()
) {
    var roles: List<Role> by manyToMany(::relations)
}
```

```kotlin group="case4" name="Role.kt" icon="kotlin"
data class Role(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var relations: List<Relation> = listOf()
) {
    var users: List<User> by manyToMany(::relations)
}
```

```kotlin group="case4" name="Relation.kt" icon="kotlin"
data class Relation(
    @PrimaryKey
    var id: Long? = null,
    var userId: Long? = null,
    var roleId: Long? = null,
    @Cascade(["userId", "roleId"], ["id", "id"])
    var user: User? = null,
    @Cascade(["userId", "roleId"], ["id", "id"])
    var role: Role? = null
)
```

### 级联删除

插入`User`或`Role`实体都可以进行级联删除，根据注解定义的级联删除策略，级联删除会自动删除关联表记录。

```kotlin
User(name = "user")
    .apply { roles = listOf(Role(name = "role")) }
    .delete().execute()
```

或者：

```kotlin
Role(name = "role")
    .apply { users = listOf(User(name = "user")) }
    .delete().execute()
``` 