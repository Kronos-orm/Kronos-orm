{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 1. One-to-one cascading relationship

This section describes the cascade insert for one-to-many cascade relationships defined in Kronos.

### Data class definition

We define two entity classes, `User` and `Profile`. The `User` entity class contains a reference to the `Profile` entity class, and the `Profile` entity class contains a reference to the `User` entity class.

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

### Cascade Insert

Cascading inserts can be performed when inserting `User` or `Profile` entities. Cascading inserts will automatically assign values to the referenced attributes of the entities created in the cascade (supporting auto-incrementing primary keys and multi-level cascading relationships).

```kotlin
User(
    name = "user",
    profile = Profile(
        name = "profile"
    )
).insert().execute()
```

Or：

```kotlin
Profile(
    name = "profile",
    user = User(
        name = "user"
    )
).insert().execute()
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
    var parentId: Long? = null,
    @Cascade(["parentId"], ["id"])
    var parent: Parent? = null
)
```

### Cascade Insert

Use the `KPojo.insert()` method to implement cascade insertion of one-to-many cascade relationships.

When inserting one-to-many in Kronos, only cascade inserts in the `Parent -> Child` direction are supported.

There is no need to specify the `parentId` of the `Child` entity during cascading inserts. Cascading inserts will automatically assign a value to the `parentId` of the `Child` entity (supporting auto-incrementing primary keys and multi-level cascading relationships).

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

## 3. Many-to-many cascading relationships

Through the `manyToMany` delegation, Kronos simplifies many-to-many relationships into one-to-many relationships and automatically creates association table records for you. For details, see: {{$.keyword("advanced/cascade-definition", ["Cascade relationship definition", "Use delegation to implement cascading many-to-many cross-intermediate table relationships"] )}}.

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
    var userId: Long? = null,
    var roleId: Long? = null,
    @Cascade(["userId", "roleId"], ["id", "id"])
    var user: User? = null,
    @Cascade(["userId", "roleId"], ["id", "id"])
    var role: Role? = null
)
```

### Cascade Insert

Cascading inserts can be performed when inserting `User` or `Role` entities. Cascading inserts will automatically assign values to the referenced attributes of the entities created in the cascade (supporting auto-incrementing primary keys and multi-level cascading relationships).
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