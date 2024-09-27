{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 1. 一对一级联关系

本节将介绍对于Kronos中定义的一对多级联关系的级联插入。

### 数据类定义

我们定义`User`和`Profile`两个实体类，`User`实体类中包含了一个`Profile`实体类的引用，`Profile`实体类中包含了一个`User`实体类的引用。

```kotlin group="case3" name="User.kt" icon="kotlin"
data class User(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var profileId: Long? = null,
    @Cascade(["profileId"], ["id"])
    var profile: Profile? = null
)
```

```kotlin group="case3" name="Profile.kt" icon="kotlin"
data class Profile(
    @PrimaryKey
    var id: Long? = null,
    var name: String? = null,
    var userId: Long? = null,
    @Cascade(["userId"], ["id"])
    var user: User? = null
)
```

### 级联插入

插入`User`或`Profile`实体都可以进行级联插入，级联插入会自动为级联创建的实体的引用属性赋值（支持自增主键和多层级级联关系）。

```kotlin
User(
    name = "user",
    profile = Profile(
        name = "profile"
    )
).insert().execute()
```

或者：

```kotlin
Profile(
    name = "profile",
    user = User(
        name = "user"
    )
).insert().execute()
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
    var parentId: Long? = null,
    @Cascade(["parentId"], ["id"])
    var parent: Parent? = null
)
```

### 级联插入
使用`KPojo.insert()`方法，可以实现一对多级联关系的级联插入。

在Kronos中的一对多插入时，仅支持`Parent -> Child`方向的级联插入。

在级联插入时不需要指定`Child`实体的`parentId`，级联插入会自动为`Child`实体的`parentId`赋值（支持自增主键和多层级级联关系）。

```kotlin
Parent(
    name = "parent",
    children = listOf(
        Child(
            name = "child-1"
        ),
        Child(
            name = "child-2"
        )
    )
).insert().execute()
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
    @Cascade(["schoolId"], ["id"])
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
    @Cascade(["groupClassId", "schoolId"], ["id", "schoolId"])
    var groupClass: GroupClass? = null
)
```
```kotlin
School(
    name = "school",
    groupClasses = listOf(
        GroupClass(
            name = "group-class-1",
            students = listOf(
                Student(
                    name = "student-1"
                ),
                Student(
                    name = "student-2"
                )
            )
        ),
        GroupClass(
            name = "group-class-2",
            students = listOf(
                Student(
                    name = "student-3"
                ),
                Student(
                    name = "student-4"
                )
            )
        )
    )
).insert().execute()
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
    var userId: Long? = null,
    var roleId: Long? = null,
    @Cascade(["userId", "roleId"], ["id", "id"])
    var user: User? = null,
    @Cascade(["userId", "roleId"], ["id", "id"])
    var role: Role? = null
)
```

### 级联插入

插入`User`或`Role`实体都可以进行级联插入，级联插入会自动为级联创建的实体的引用属性赋值（支持自增主键和多层级级联关系）。

```kotlin
User(name = "user")
    .apply { roles = listOf(Role(name = "role")) }
    .insert().execute()
```

或者：

```kotlin
Role(name = "role")
    .apply { users = listOf(User(name = "user")) }
    .insert().execute()
``` 