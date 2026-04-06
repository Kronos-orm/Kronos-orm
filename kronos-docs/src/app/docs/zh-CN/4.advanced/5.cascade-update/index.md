{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}


## 1. 一对一级联关系

本节将介绍对于Kronos中定义的一对多级联关系的级联更新。

### 数据类定义

我们定义`User`和`Profile`两个实体类，`User`实体类中包含了一个`Profile`实体类的引用，`Profile`实体类中包含了一个`User`实体类的引用。

```kotlin group="case3" name="User.kt" icon="kotlin"
data class User(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var profileName: String? = null,
    @Cascade(["profileName"], ["name"])
    var profile: Profile? = null
)
```

```kotlin group="case3" name="Profile.kt" icon="kotlin"
data class Profile(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var userName: String? = null,
    @Cascade(["userName"], ["name"])
    var user: User? = null
)
```

### 级联更新

更新`User`或`Profile`实体都可以进行级联更新，级联更新会自动修改为级联创建的实体的引用属性（支持自增主键和多层级级联关系）。

```kotlin
User(
    id = 1,
    name = "user" //这里会级联更新Profile的userName字段
).update().by { it.id }.execute()
```

或者：

```kotlin
Profile(
    id = 1,
    name = "profile" //这里会级联更新User的profileName字段
).update().by { it.id }.execute()
```

## 2. 一对多级联关系

一对多关系和一对一关系的用法类似。

### 数据类定义
首先，我们定义以下的`Parent`和`Child`两个实体类，`Parent`实体类中包含了一个`Child`实体类的集合，`Child`实体类中包含了一个`Parent`实体类的引用。

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
    var parentName: String? = null,
    @Cascade(["parentName"], ["name"])
    var parent: Parent? = null
)
```

### 级联更新
使用`KPojo.update()`方法，可以实现一对多级联关系的级联更新。

在Kronos中的一对多更新时，仅支持`Parent -> Child`方向的级联更新。

在级联更新时不需要指定`Child`实体的`parentId`，级联更新会自动修改为`Child`实体的`parentId`（支持自增主键和多层级级联关系）。

```kotlin
Parent(
    id = 1,
    name = "parent" //这里会级联更新Child的parentName字段
).update().by { it.id }.execute()
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
    var schoolName: String? = null,
    @Cascade(["schoolName"], ["name"])
    var school: School? = null,
    var students: List<Student> = listOf()
)
```

```kotlin group="case2" name="Student.kt" icon="kotlin"
data class Student(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var groupClassName: Name? = null,
    var schoolName: String? = null,
    @Cascade(["groupClassName", "schoolName"], ["name", "schoolName"])
    var groupClass: GroupClass? = null
)
```
```kotlin
School(
    id = 1,
    name = "school" //这里会级联更新GroupClass和Student的schoolName字段
).update().by { it.id }.execute()
```

## 3. 多对多级联关系

通过`manyToMany`委托，Kronos简化多对多关系为一对多关系，并为您自动创建关联表记录，详见：{{$.keyword("advanced/cascade-definition", ["级联关系定义", "使用委托实现级联多对多跨中间表关系"] )}}。

### 数据类定义

首先我们定义`User`、`Role`、`Relation`三个实体类，其中`Relation`实体类中包含了`User`和`Role`两个实体类的引用，`User`和`Role`通过`manyToMany`委托关联。

```kotlin group="case4" name="User.kt" icon="kotlin"
data class User(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var relations: List<Relation> = listOf()
){
    var roles: List<Role> by manyToMany(::relations)
}
```

```kotlin group="case4" name="Role.kt" icon="kotlin"
data class Role(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var relations: List<Relation> = listOf()
){
    var users: List<User> by manyToMany(::relations)
}
```

```kotlin group="case4" name="Relation.kt" icon="kotlin"
data class Relation(
    @PrimaryKey
    var id: Long? = null,
    var userName: String? = null,
    var roleName: String? = null,
    @Cascade(["userName", "roleName"], ["name", "name"])
    var user: User? = null,
    @Cascade(["userName", "roleName"], ["name", "name"])
    var role: Role? = null
)
```

### 级联更新

更新`User`或`Role`实体都可以进行级联更新，并自动更新级联创建的中间表实体的引用属性赋值。

> **warning**
> 这里我们并不会自动级联更新级联的另一端的实体，因为我们无法保证级联的另一端的实体是否被其他记录所引用，因此该功能属于业务层面的范畴，需要用户自行增加更新逻辑。

```kotlin
User(
    id = 1,
    name = "user" //这里会级联更新Relation的userName字段
).update().by { it.id }.execute()
```

或者：

```kotlin
Role(
    id = 1,
    name = "role" //这里会级联更新Relation的roleName字段
).update().by { it.id }.execute()
``` 