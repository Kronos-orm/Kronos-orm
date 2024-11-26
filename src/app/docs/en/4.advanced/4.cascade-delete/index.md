{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Cascade delete strategy

{{ $.annotation("Cascade") }} annotations support setting strategies for cascading delete operations, please refer to：{{ $.keyword("concept/cascade-delete-action", ["Cascade delete strategy"]) }}。

### Setting Default Values

When using the {{$.code("SET_DEFAULT")}} strategy, you need to specify the `defaultValue` attribute in the annotation

{{$.code("defaultValue")}} accepts a string array, such as `["a", "b"]`, corresponding to the default values of multiple reference columns of the associated entity

> **Note**
> Please ensure that the lengths of `defaultValue`, `properties`, and `targetProperties` arrays are consistent

If a reference column does not need to be modified, you can set it to `RESERVED`. Kronos will ignore the reference column when the cascade delete is set to `SET_DEFAULT`, such as:

```kotlin
data class Child(
    var parentId: Long? = null,
    var parentName: String? = null
    @Cascade(["parentId", "parentName"], ["id", "name"], SET_DEFAULT, [RESERVED, "empty"])
    var parent: Parent? = null
) 
```

### Logical deletion

If the top-level delete function uses the logical delete function, and if the cascade delete strategy of the cascade delete sub-level is `CASCADE`, then logical delete will also be used when deleting the child table data.

## Partially enable or disable cascade delete

### Turn off cascading delete

Kronos enables the cascading delete function by default, which needs to be explicitly turned off in the `delete` function:

```kotlin
KPojo.delete().cascade(enable = false).execute()
```
### Partially enable cascade delete

When there are multiple cascade declarations in KPojo, but only some of them need to be deleted by cascade, the attributes that need to be deleted by cascade can be passed into the `cascade` function, and the remaining attributes and sub-attributes will not trigger cascade deletion.

```kotlin
// If only property1 and property2 in KPojo need to be cascaded deleted, then it is as follows:
KPojo.delete().cascade(KPojo::property1, KPojo::property2).execute()
```

You can limit the cascade deletion of its sub-attributes as follows:

```kotlin
KPojo.delete().cascade(
    KPojo::property1, 
    KPojo::property2, 
    Property1::subProperty1, 
    Property1::subProperty2
).execute()
```

## Logical deletion

## 1. One-to-one cascading relationship

This section describes how to use cascade delete for a pair of cascade relationships defined in Kronos.

### Data class definition

We define two entity classes, `User` and `Profile`. The `User` entity class contains a reference to the `Profile` entity class, and the `Profile` entity class contains a reference to the `User`
entity class.

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

### Cascade delete

Deleting a User or Profile entity will trigger a cascade delete

```kotlin
User(id = 1).delete().execute()
// This will delete all User records with id=1 and all Profile records with userId=1
```

Or：

```kotlin
Profile(id = 1).delete().execute()
// This will delete all Profile records with id=1 and all User records with userId=1
```

## 2. One-to-many cascading relationship

The usage of one-to-many relationships is similar to that of one-to-one relationships.

### Data class definition

First, we define the following two entity classes, `Parent` and `Child`. 
The `Parent` entity class contains a collection of `Child` entity classes, and the `Child` entity class contains a reference to the `Parent` entity class.

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

### Cascade delete

Use the `KPojo.delete()` method to implement cascading deletion of one-to-many cascading relationships.

In Kronos, only cascading deletion in the `Parent -> Child` direction is supported. When deleting, the Child is deleted first, then the Parent. If any level of the multi-level relationship has a `Restrict` cascading deletion policy, the deletion will not be triggered.

```kotlin
Parent(id = 1).delete().execute()
// This will delete the Parent record with id 1 and also delete the Child record with parentId 1.
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
// This will cascade delete all GroupClass and Student records with name="school"
```

## 3. Many-to-many cascading relationships

Through the `manyToMany` delegation, Kronos simplifies many-to-many relationships into one-to-many relationships and automatically creates association table records for you. For details, see: {{$.keyword("
advanced/cascade-definition", ["cascade relationship definition", "using delegation to implement cascading many-to-many cross-intermediate table relationships"] )}}.

### Data class definition

First, we define three entity classes: `User`, `Role`, and `Relation`. The `Relation` entity class contains references to the `User` and `Role` entity classes. `User` and `Role` are associated through `manyToMany` delegation.

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

### Cascade delete

Inserting a User or Role entity can perform cascade deletion. According to the cascade deletion strategy defined by the annotation, the cascade deletion will automatically delete the associated table records.

```kotlin
User(name = "user")
    .apply { roles = listOf(Role(name = "role")) }
    .delete().execute()
```

Or：

```kotlin
Role(name = "role")
    .apply { users = listOf(User(name = "user")) }
    .delete().execute()
``` 