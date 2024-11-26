{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Cascading Relationship Annotation

KPojo provides the `@Cascade` annotation to define the cascade relationship, see: {{ $.keyword("
class-definition/annotation-config", ["annotation configuration", "@Cascade cascade relationship definition"]) }}.

## 1. One-to-many cascading relationship

This section describes how to define a one-to-many cascade relationship in a KPojo entity class.

First, we have two entity classes, `Parent` and `Child`. The `Parent` entity class contains a collection of `Child` entity classes, 
and the `Child` entity class contains a reference to the `Parent` entity class.

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

In the above code, the `@Cascade` annotation in the `Child` entity class declares the simplest one-to-many cascade relationship.

The first parameter of the `@Cascade` annotation is the cascade field `parentId` in the `Child` entity class, and the second parameter is the primary key field `id` in the `Parent` entity class.

At this point, the one-to-many cascade relationship between the `Parent` and `Child` entity classes is defined.

If you need to define a cascade relationship through multiple fields, you can use commas to separate multiple cascade fields in the first parameter of the `@Cascade` annotation, as shown below:

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
> Usually we think that the end without annotation definition is the maintenance end, which is also the parent end in the cascade relationship, and the end declared by annotation is the maintained end, which is also the child end in the cascade relationship.
> In the cascade relationship, changes in the parent end will affect the child end, while changes in the child end will not affect the parent end.

## 2.One-to-one cascading relationship

This section will introduce how to define a pair of first-level cascade relationships in KPojo entity classes.

First, we have two entity classes, `User` and `Profile`. The `User` entity class contains a reference to the `Profile` entity class, and the `Profile` entity class contains a reference to the `User`
entity class.

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

In the above code, the `@Cascade` annotation in the `User` entity class declares the simplest one-to-one cascade relationship.

The first parameter of the `@Cascade` annotation is the primary key field `id` in the `User` entity class, and the second parameter is the cascade field `userId` in the `Profile` entity class.

At this point, the one-to-one cascade relationship between the `User` and `Profile` entity classes is defined.

If you need to define a cascade relationship through multiple fields, you can use commas to separate multiple cascade fields in the first parameter of the `@Cascade` annotation, as shown below:

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
> In a one-to-one cascade relationship, annotations can be declared at only one end or at both ends. However, when only one end is declared, the end without annotation definition is the maintenance end, which is also the parent end in the cascade relationship.

## 3.Many-to-many cascading relationships

This section will introduce how to define many-to-many cascading relationships in KPojo entity classes.

Usually, many-to-many cascading relationships need to be implemented through an intermediate table, which contains the primary key fields of the two entity classes.

First, we have three entity classes: `User`, `Role`, and `Relation`, where the `Relation` entity class contains references to the two entity classes `User` and `Role`.

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

In the above code, the `@Cascade` annotation in the `Relation` entity class declares the simplest many-to-many cascade relationship.

The first parameter of the `@Cascade` annotation is the cascade field `userId` in the `Relation` entity class, and the second parameter is the primary key field `id` in the `User` entity class.

The first parameter of the `@Cascade` annotation is the cascade field `roleId` in the `Relation` entity class, and the second parameter is the primary key field `id` in the `Role` entity class.

At this point, the many-to-many cascade relationship between the three entity classes `User`, `Role`, and `Relation` is defined.

### Using delegation to implement cascading many-to-many relationships across intermediate tables

The target attribute in the many-to-many relationship in the cascade function is defined by **delegation** and needs to be declared in the class, such as:

```kotlin
data class User(
    val id: Int? = null,
    val name: String? = null,
    // Intermediate table for many-to-many relationships
    val relations: List<Relation>? = emptyList()
) : KPojo {
    // Target table of many-to-many relationships
    var roles: List<Role>? by manyToMany(::UserRoleRelation)
}
```

