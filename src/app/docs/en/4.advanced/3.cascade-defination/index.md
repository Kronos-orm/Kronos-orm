{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 级联关系注解

KPojo提供了`@Cascade`注解来定义级联关系，详见：{{ $.keyword("
class-definition/annotation-config", ["注解配置", "@Cascade级联关系定义"]) }}。

## 1. 一对多级联关系

本节将介绍如何在KPojo实体类中定义一对多级联关系。

首先，我们有`Parent`和`Child`两个实体类，`Parent`实体类中包含了一个`Child`实体类的集合, `Child`实体类中包含了一个`Parent`
实体类的引用。

```kotlin {5,13,14}
data class Parent(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null,
    var children: List<Child> = listOf()
) : KPojo

data class Child(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null,
    var parentId: Long? = null,
    @Cascade(["parentId"], ["id"])
    var parent: Parent? = null
) : KPojo
```

以上代码中，`Child`实体类中的`@Cascade`注解声明了最简单的一对多级联关系。

`@Cascade`注解的第一个参数是`Child`实体类中的级联字段`parentId`，第二个参数是`Parent`实体类中的主键字段`id`。

至此，`Parent`和`Child`两个实体类之间的一对多级联关系定义完成。

如果需要通过多个字段进行级联关系定义，可以在`@Cascade`注解的第一个参数中使用逗号分隔多个级联字段，如下所示：

```kotlin {7,8}
data class Child(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null,
    var parentId: Long? = null,
    var parentName: String? = null,
    @Cascade(["parentId", "parentName"], ["id", "name"])
    var parent: Parent? = null
) : KPojo
```

> **Note**
> 通常我们认为，没有注解定义的一端为维护端，也是级联关系中的父端，而注解声明的一端为被维护端，也是级联关系中的子端。
> 在级联关系中，父端的变化会影响子端，而子端的变化不会影响父端。

## 2.一对一级联关系

本节将介绍如何在KPojo实体类中定义一对一级联关系。

首先，我们有`User`和`Profile`两个实体类，`User`实体类中包含了一个`Profile`实体类的引用, `Profile`实体类中包含了一个`User`
实体类的引用。

```kotlin {5,6,14}
data class User(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null,
    @Cascade(["id"], ["userId"])
    var profile: Profile? = null
) : KPojo

data class Profile(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var userId: Long? = null,
    var email: String? = null,
    var user: User? = null
) : KPojo
```

以上代码中，`User`实体类中的`@Cascade`注解声明了最简单的一对一级联关系。

`@Cascade`注解的第一个参数是`User`实体类中的主键字段`id`，第二个参数是`Profile`实体类中的级联字段`userId`。

至此，`User`和`Profile`两个实体类之间的一对一级联关系定义完成。

如果需要通过多个字段进行级联关系定义，可以在`@Cascade`注解的第一个参数中使用逗号分隔多个级联字段，如下所示：

```kotlin {6,7}
data class User(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null,
    var email: String? = null,
    @Cascade(["id", "email"], ["userId", "email"])
    var profile: Profile? = null
) : KPojo
```

> **Note**
> 一对一级联关系中，注解可以只在一端声明，也可以在两端同时声明，但是只有一端声明时，没有注解定义的一端为维护端，也是级联关系中的父端。

## 3.多对多级联关系

本节将介绍如何在KPojo实体类中定义多对多级联关系。

通常，多对多级联关系需要通过中间表来实现，中间表中包含了两个实体类的主键字段。

首先，我们有`User`、`Role`以及`Relation`三个实体类，其中`Relation`实体类中包含了`User`和`Role`两个实体类的引用。

```kotlin {5,6,7,8}
data class User(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null,
    var relations: List<Relation>? = listOf()
) : KPojo

data class Role(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null,
    var relations: List<Relation>? = listOf()
) : KPojo

data class Relation(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var userId: Long? = null,
    var roleId: Long? = null,
    @Cascade(["userId"], ["id"])
    var user: User? = null,
    @Cascade(["roleId"], ["id"])
    var role: Role? = null
) : KPojo
```

以上代码中，`Relation`实体类中的`@Cascade`注解声明了最简单的多对多级联关系。

`@Cascade`注解的第一个参数是`Relation`实体类中的级联字段`userId`，第二个参数是`User`实体类中的主键字段`id`。

`@Cascade`注解的第一个参数是`Relation`实体类中的级联字段`roleId`，第二个参数是`Role`实体类中的主键字段`id`。

至此，`User`、`Role`以及`Relation`三个实体类之间的多对多级联关系定义完成。

### 使用委托实现级联多对多跨中间表关系

级联功能中多对多关系中的目标属性通过**委托**方式来定义，需要放在class内声明，形如：

```kotlin
data class User(
    val id: Int? = null,
    val name: String? = null,
    // 多对多关系的中间表
    val relations: List<Relation>? = emptyList()
) : KPojo {
    // 多对多关系的目标表
    var roles: List<Role>? by manyToMany(::UserRoleRelation)
}
```

