## 配置级联关系

通过配置`KPojo`的<a href="/documentation/zh-CN/class-definition/table-class-definition#列关联设置">[列关联设置]</a>，指定关联字段关联信息（`@Reference`）中`usage`属性包含`Update`（或不指定，使用默认），即可开启该类（被）级联更新的功能。

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

## 使用<span style="color: #DD6666">cascade</span>设置当前级联操作

在Kronos中，我们可以使用`cascade`方法设置是否开启本次更新的级联功能并限制级联的最大层数

`KPojo.update().cascade().excute()`

- `enabled`： `Boolean` 手动设置是否开启本次更新的级联功能（可选，默认为`true`开启级联）
- `depth`： `Int` 限制级联的最大层数，默认为`-1`，即不限制级联层数， `0`表示不进行级联更新

## 使用<span style="color: #DD6666">update</span>及相关方法进行级联更新操作

级联更新的各方法与操作同[更新记录](/documentation/zh-CN/database/update-records)相关方法与操作基本一致。

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
