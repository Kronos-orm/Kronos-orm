{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Cascade delete action

Use the `onDelete` parameter of `@Cascade` to decide what happens to related records when the root record is deleted. The full delete workflow is described in {{ $.keyword("advanced/cascade-delete", ["Cascade Delete"]) }}.

{{ $.members([
["NO_ACTION", "Do not create a child delete or update task. This is the default.", "CascadeDeleteAction"],
["CASCADE", "Delete matching child records before deleting the root record.", "CascadeDeleteAction"],
["RESTRICT", "Reject the delete when matching child records exist.", "CascadeDeleteAction"],
["SET_NULL", "Set the child reference columns to <code>null</code> before deleting the root record.", "CascadeDeleteAction"],
["SET_DEFAULT", "Set the child reference columns to values from <code>defaultValue</code> before deleting the root record.", "CascadeDeleteAction"]
]) }}

## Declare a delete action

Put `onDelete = CascadeDeleteAction.CASCADE` on the child-side reference when the child rows should be removed with the parent.

```kotlin group="CASCADE 1" name="kotlin" icon="kotlin" {14}
@Table("dept")
data class Dept(
    @PrimaryKey
    var id: Int? = null,
    var users: List<User> = listOf()
) : KPojo

@Table("user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var deptId: Int? = null,
    @Cascade(["deptId"], ["id"], onDelete = CascadeDeleteAction.CASCADE)
    var dept: Dept? = null
) : KPojo

Dept(id = 7).delete().by { it.id }.execute()
```

```sql group="CASCADE 1" name="mysql" icon="mysql"
SELECT `id` FROM `dept` WHERE `id` = :id;
SELECT `id`, `dept_id` AS `deptId` FROM `user` WHERE `dept_id` = :deptId;
DELETE FROM `user` WHERE `id` = :id AND `dept_id` = :deptId;
DELETE FROM `dept` WHERE `id` = :id;
```

Result shape:

```text group="CASCADE 2" name="result"
Kronos queries the root row, queries matching child rows, deletes child rows first, then deletes the root row.
```

## Use SET_NULL

Use `SET_NULL` when child rows should stay in the database but stop pointing to the deleted root record.

```kotlin group="SET_NULL 1" name="kotlin" icon="kotlin" {14}
@Table("dept")
data class Dept(
    @PrimaryKey
    var id: Int? = null,
    var users: List<User> = listOf()
) : KPojo

@Table("user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var deptId: Int? = null,
    @Cascade(["deptId"], ["id"], onDelete = CascadeDeleteAction.SET_NULL)
    var dept: Dept? = null
) : KPojo

Dept(id = 7).delete().by { it.id }.execute()
```

```sql group="SET_NULL 1" name="mysql" icon="mysql"
UPDATE `user`
SET `dept_id` = :deptIdNew
WHERE `id` = :id AND `dept_id` = :deptId;
DELETE FROM `dept` WHERE `id` = :id;
```

Result shape:

```text group="SET_NULL 2" name="result"
The child row remains. Its dept_id parameter is updated to null before the parent delete runs.
```

## Use SET_DEFAULT

Use `SET_DEFAULT` with `defaultValue` when a child reference should be moved to a known value.

```kotlin group="SET_DEFAULT 1" name="kotlin" icon="kotlin" {14}
@Table("dept")
data class Dept(
    @PrimaryKey
    var id: Int? = null,
    var users: List<User> = listOf()
) : KPojo

@Table("user")
data class User(
    @PrimaryKey
    var id: Int? = null,
    var deptId: Int? = null,
    @Cascade(["deptId"], ["id"], onDelete = CascadeDeleteAction.SET_DEFAULT, defaultValue = ["0"])
    var dept: Dept? = null
) : KPojo

Dept(id = 7).delete().by { it.id }.execute()
```

```sql group="SET_DEFAULT 1" name="mysql" icon="mysql"
UPDATE `user`
SET `dept_id` = :deptIdNew
WHERE `id` = :id AND `dept_id` = :deptId;
DELETE FROM `dept` WHERE `id` = :id;
```

```text group="SET_DEFAULT 1" name="result"
Before deleting dept, Kronos updates matching user.deptId to "0".
```

For multi-column references, keep `properties`, `targetProperties`, and `defaultValue` in the same order. Use `Cascade.RESERVED` for a column that should keep its current value.

```kotlin group="SET_DEFAULT 2" name="reserved" icon="kotlin"
@Cascade(
    ["deptId", "tenantId"],
    ["id", "tenantId"],
    onDelete = CascadeDeleteAction.SET_DEFAULT,
    defaultValue = [Cascade.RESERVED, "0"]
)
var dept: Dept? = null
```

## Use RESTRICT

Use `RESTRICT` when the parent can be deleted only after related rows are removed or detached.

```kotlin group="RESTRICT" name="kotlin" icon="kotlin"
@Cascade(["deptId"], ["id"], onDelete = CascadeDeleteAction.RESTRICT)
var dept: Dept? = null

Dept(id = 7).delete().by { it.id }.execute()
```

```text group="RESTRICT" name="result"
UnsupportedOperationException: The record cannot be deleted because it is restricted by a cascade.
```

> **Note**
> `NO_ACTION` only means Kronos does not create an extra child task for that relationship. Database foreign-key constraints still behave according to the database schema.
