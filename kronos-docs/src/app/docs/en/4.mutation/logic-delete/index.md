{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Declare the logical delete column

Use `@LogicDelete` on the KPojo field that marks deleted rows. The annotation is enabled by default.

```kotlin group="KPojo 1" name="User.kt" icon="kotlin"
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.interfaces.KPojo

data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo
```

If your application uses the same logical delete field across tables, configure the global strategy and keep the field on each KPojo that should use it.

```kotlin group="KPojo 2" name="global config" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

with(Kronos) {
    logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
}
```

## Delete writes the delete marker

When logical delete is enabled, `delete().execute()` generates an `UPDATE` that writes the deleted value. The generated condition also keeps already deleted rows out of the target set.

```kotlin group="Delete" name="kotlin" icon="kotlin"
User(id = 1)
    .delete()
    .by { it.id }
    .execute()
```

```sql group="Delete" name="Mysql" icon="mysql"
UPDATE `user`
SET `deleted` = :deletedNew
WHERE `id` = :id
  AND `deleted` = 0
```

```text group="Delete" name="params"
id = 1
deletedNew = 1
```

Use `.logic(false)` for the current delete when the operation must generate a physical `DELETE`.

```kotlin group="Physical delete" name="kotlin" icon="kotlin"
User(id = 1)
    .delete()
    .logic(false)
    .by { it.id }
    .execute()
```

```sql group="Physical delete" name="Mysql" icon="mysql"
DELETE
FROM `user`
WHERE `id` = :id
```

## Select and update ignore deleted rows

Select and update statements add the active-row condition for a KPojo with logical delete enabled.

```kotlin group="Visible rows" name="kotlin" icon="kotlin"
User()
    .select { [it.id, it.name] }
    .toMapList()

User(id = 1)
    .update()
    .set { it.name = "Kronos" }
    .by { it.id }
    .execute()
```

```sql group="Visible rows" name="select" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `deleted` = 0
```

```sql group="Visible rows" name="update" icon="mysql"
UPDATE `user`
SET `name` = :nameNew
WHERE `id` = :id
  AND `deleted` = 0
```

## Insert and upsert initialize the marker

Insert initializes the logical delete field with the active value. `onConflict()` upsert also includes that field in the inserted columns when the strategy is active.

```kotlin group="Insert" name="kotlin" icon="kotlin"
User(name = "Kronos")
    .insert()
    .execute()
```

```sql group="Insert" name="Mysql" icon="mysql"
INSERT INTO `user` (`name`, `deleted`)
VALUES (:name, :deleted)
```

```text group="Insert" name="params"
name = "Kronos"
deleted = 0
```

## DataGuard boundary

Logical delete changes a delete into an update statement. If no user condition is provided, the generated statement can still target every active row.

```kotlin group="DataGuard" name="kotlin" icon="kotlin"
User()
    .delete()
    .execute()
```

```sql group="DataGuard" name="Mysql" icon="mysql"
UPDATE `user`
SET `deleted` = :deletedNew
WHERE `deleted` = 0
```

Enable {{ $.keyword("advanced/data-guard", ["DataGuard"]) }} when the application should reject full-table writes. See {{ $.keyword("mutation/delete", ["Delete"]) }} and {{ $.keyword("mutation/update", ["Update"]) }} for condition APIs.
