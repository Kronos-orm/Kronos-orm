# {{ NgDocPage.title }}

## Configuring Cascade Relationships

By configuring the column associations of `KPojo` with the <a href="/documentation/en/class-definition/table-class-definition#column-association-setting">[column association settings]</a> directive and specifying the relationship information in `@Reference` with the `usage` property containing `Update` (or not specifying it, using the default), you can enable the cascading update feature for this class (or being) by specifying the associated fields.

```kotlin name="kotlin" icon="kotlin" {6, 14-18, 26-33}
@Table("school")
data class School(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var groupClass: List<GroupClass>? = null
) : KPojo

@Table("group_class")
data class GroupClass(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    val name: String? = null,
    @NotNull
    var schoolName: String? = null,
    @Reference(["schoolName"], ["name"], mapperBy = School::class)
    var school: School? = null,
    var students: List<Student>? = null
) : KPojo

@Table("student")
data class Student(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var schoolName: String? = null,
    var groupClassName: String? = null,
    @Reference(
        ["groupClassName", "schoolName"],
        ["name", "schoolName"],
        mapperBy = GroupClass::class
    )
    var groupClass: GroupClass? = null
) : KPojo
```

## Using the <span style="color: #DD6666">cascade</span> Setting for Current Cascade Operations

In Kronos, we can use the `cascade` method to set whether to enable the cascade feature for the current update and limit the maximum cascade levels.

`KPojo.update().cascade().execute()`

- `enabled`: `Boolean` Manually set whether to enable the cascade feature for the current update (optional, defaults to `true` for enabling cascade).
- `depth`: `Int` Limits the maximum cascade levels, defaulting to `-1`, which means no limit on cascade levels. `0` indicates no cascade update.

## Using <span style="color: #DD6666">update</span> and Related Methods for Cascade Updates

The methods and operations for cascading updates are similar to those for [updating records](/documentation/en/database/update-records).

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7-11}
School(name = "School").update().set { it.name = "School2" }.execute()
```

```sql group="Case 1" name="Mysql" icon="mysql"
UPDATE `student` SET `school_name` = "School2" WHERE `id` = :id AND `name` = :name AND `student_no` = :studentNo AND `school_name` = "School" AND `group_class_name` = :groupClassName

UPDATE `group_class` SET `school_name` = "School2" WHERE `id` = :id AND `name` = :name AND `school_name` = "School"

UPDATE `school` SET `name` = "School2" WHERE `id` = :id AND `name` = "School"
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
UPDATE "student" SET "school_name" = "School2" WHERE "id" = :id AND "name" = :name AND "student_no" = :studentNo AND "school_name" = "School" AND "group_class_name" = :groupClassName

UPDATE "group_class" SET "school_name" = "School2" WHERE "id" = :id AND "name" = :name AND "school_name" = "School"

UPDATE "school" SET "name" = "School2" WHERE "id" = :id AND "name" = "School"
```

```sql group="Case 1" name="SQLite" icon="sqlite"
UPDATE `student` SET `school_name` = "School2" WHERE `id` = :id AND `name` = :name AND `student_no` = :studentNo AND `school_name` = "School" AND `group_class_name` = :groupClassName

UPDATE `group_class` SET `school_name` = "School2" WHERE `id` = :id AND `name` = :name AND `school_name` = "School"

UPDATE `school` SET `name` = "School2" WHERE `id` = :id AND `name` = "School"
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
UPDATE [student] SET [school_name] = "School2" WHERE [id] = :id AND [name] = :name AND [student_no] = :studentNo AND [school_name] = "School" AND [group_class_name] = :groupClassName

UPDATE [group_class] SET [school_name] = "School2" WHERE [id] = :id AND [name] = :name AND [school_name] = "School"

UPDATE [school] SET [name] = "School2" WHERE [id] = :id AND [name] = "School"
```

```sql group="Case 1" name="Oracle" icon="oracle"
UPDATE "student" SET "school_name" = "School2" WHERE "id" = :id AND "name" = :name AND "student_no" = :studentNo AND "school_name" = "School" AND "group_class_name" = :groupClassName

UPDATE "group_class" SET "school_name" = "School2" WHERE "id" = :id AND "name" = :name AND "school_name" = "School"

UPDATE "school" SET "name" = "School2" WHERE "id" = :id AND "name" = "School"
```
