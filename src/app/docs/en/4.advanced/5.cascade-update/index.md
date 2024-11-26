{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}


## 1. One-to-one cascading relationship

This section describes the cascading updates for one-to-many cascading relationships defined in Kronos.

### Data class definition

We define two entity classes, `User` and `Profile`. The `User` entity class contains a reference to the `Profile` entity class, 
and the `Profile` entity class contains a reference to the `User` entity class.

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

### Cascading Updates

Updating the `User` or `Profile` entity can perform cascading updates. 
Cascading updates will automatically modify the reference attributes of the entity created by the cascade 
(supporting auto-increment primary keys and multi-level cascading relationships).
```kotlin
User(
    id = 1,
    name = "user" //Here the userName field of Profile will be cascaded and updated
).update().by { it.id }.execute()
```

Or：

```kotlin
Profile(
    id = 1,
    name = "profile" //This will cascade update the User's profileName field
).update().by { it.id }.execute()
```

## 2. One-to-many cascading relationship

The usage of one-to-many relationships is similar to that of one-to-one relationships.

### Data class definition
First, we define the following `Parent` and `Child` two entity classes. The `Parent` entity class contains a collection of `Child` entity classes, and the `Child` entity class contains a reference to the `Parent` entity class.
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

### Cascading Updates
Use the `KPojo.update()` method to implement cascading updates of one-to-many cascading relationships.

When performing one-to-many updates in Kronos, only cascading updates in the `Parent -> Child` direction are supported.

When cascading updates, you do not need to specify the `parentId` of the `Child` entity. The cascading update will automatically change it to the `parentId` of the `Child` entity (supporting auto-increment primary keys and multi-level cascading relationships).

```kotlin
Parent(
    id = 1,
    name = "parent" //This will cascade update the parentName field of Child
).update().by { it.id }.execute()
```

Here is a more complex example with more layers:

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
    name = "school" //Here, the schoolName fields of GroupClass and Student will be cascaded and updated
).update().by { it.id }.execute()
```

## 3. Many-to-many cascading relationships

Through the `manyToMany` delegation, Kronos simplifies many-to-many relationships into one-to-many relationships and automatically creates association table records for you. For details, see: 
{{$.keyword("advanced/cascade-definition", ["Cascade relationship definition", "Use delegation to implement cascading many-to-many cross-intermediate table relationships"] )}}.

### Data class definition

First, we define three entity classes: `User`, `Role`, and `Relation`. The `Relation` entity class contains references to the `User` and `Role` entity classes. `User` and `Role` are associated through the `manyToMany` delegation.

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

### Cascading Updates

Updating the `User` or `Role` entity can perform cascading updates and automatically update the reference attribute assignments of the intermediate table entity created by the cascade.

> **warning**
> Here we will not automatically cascade update the entity at the other end of the cascade, because we cannot guarantee whether the entity at the other end of the cascade is referenced by other records. Therefore, this function belongs to the business level and requires users to add update logic by themselves.

```kotlin
User(
    id = 1,
    name = "user" //Here the userName field of the Relation will be cascaded and updated
).update().by { it.id }.execute()
```

Or：

```kotlin
Role(
    id = 1,
    name = "role" //Here the roleName field of Relation will be cascaded and updated
).update().by { it.id }.execute()
``` 