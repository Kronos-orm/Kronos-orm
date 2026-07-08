{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Declare the version column

Use `@Version` on the KPojo field that stores the optimistic lock version. The annotation is enabled by default.

```kotlin group="KPojo 1" name="Product.kt" icon="kotlin"
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Version
import com.kotlinorm.interfaces.KPojo

data class Product(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    @Version
    var version: Int? = null,
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo
```

A global strategy can enable the same version field name across KPojo classes.

```kotlin group="KPojo 2" name="global config" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

with(Kronos) {
    optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
}
```

## Insert initializes the version

When the version strategy is active, insert writes the initial version value.

```kotlin group="Insert" name="kotlin" icon="kotlin"
Product(name = "Keyboard")
    .insert()
    .execute()
```

```sql group="Insert" name="Mysql" icon="mysql"
INSERT INTO `product` (`name`, `version`, `deleted`)
VALUES (:name, :version, :deleted)
```

```text group="Insert" name="params"
name = "Keyboard"
version = 0
deleted = 0
```

## Update increments the version field

`update().execute()` adds a version increment to the generated `SET` list. The version field cannot be assigned manually in the same update.

```kotlin group="Update 1" name="kotlin" icon="kotlin"
Product(id = 1)
    .update()
    .set { it.name = "Keyboard Pro" }
    .by { it.id }
    .execute()
```

```sql group="Update 1" name="Mysql" icon="mysql"
UPDATE `product`
SET `name` = :nameNew,
    `version` = `version` + :version2PlusNew
WHERE `id` = :id
  AND `deleted` = 0
```

```text group="Update 1" name="params"
id = 1
nameNew = "Keyboard Pro"
version2PlusNew = 1
```

Generated update SQL increments the version field. Add a version condition in `where { ... }` when the application needs the update to match the version read earlier.

```kotlin group="Update 2" name="version condition" icon="kotlin"
Product(id = 1, version = 3)
    .update()
    .set { it.name = "Keyboard Pro" }
    .where { it.id == 1 && it.version == 3 }
    .execute()
```

```sql group="Update 2" name="version condition SQL" icon="mysql"
UPDATE `product`
SET `name` = :nameNew,
    `version` = `version` + :version2PlusNew
WHERE `product`.`id` = :id
  AND `product`.`version` = :version
  AND `deleted` = 0
```

## Logical delete also increments the version

For a KPojo that uses both `@LogicDelete` and `@Version`, logical delete writes the delete marker and increments the version field.

```kotlin group="Delete" name="kotlin" icon="kotlin"
Product(id = 1)
    .delete()
    .by { it.id }
    .execute()
```

```sql group="Delete" name="Mysql" icon="mysql"
UPDATE `product`
SET `deleted` = :deletedNew,
    `version` = `version` + :version2PlusNew
WHERE `id` = :id
  AND `deleted` = 0
```

## Upsert uses the configured mutation paths

Fallback `upsert()` checks existence through a select, then runs `update()` or `insert()`. The update branch gets the same version increment as a normal update, and the insert branch gets the same initial version as a normal insert.

```kotlin group="Upsert" name="kotlin" icon="kotlin"
Product(id = 1, name = "Keyboard")
    .upsert { it.name }
    .on { it.id }
    .execute()
```

```sql group="Upsert" name="fallback SQL" icon="mysql"
SELECT COUNT(1)
FROM `product`
WHERE `id` = :id

-- update branch
UPDATE `product`
SET `name` = :nameNew,
    `version` = `version` + :version2PlusNew
WHERE `id` = :id
  AND `deleted` = 0

-- insert branch
INSERT INTO `product` (`id`, `name`, `version`, `deleted`)
VALUES (:id, :name, :version, :deleted)
```

See {{ $.keyword("mutation/upsert", ["Upsert"]) }} for `on(...)` and `onConflict()` examples.
