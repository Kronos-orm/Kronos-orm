{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 声明版本列

在保存乐观锁版本的 KPojo 字段上使用 `@Version`。该注解默认启用。

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

全局策略可以为多个 KPojo 启用同名版本字段。

```kotlin group="KPojo 2" name="global config" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

with(Kronos) {
    optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
}
```

## 插入会初始化版本号

版本策略启用时，insert 会写入初始版本值。

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

## 更新会递增版本字段

`update().execute()` 会把版本递增加入生成的 `SET` 列表。同一次 update 中不能手动给版本字段赋值。

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

生成的 update SQL 会递增版本字段。应用需要按之前读取的版本进行匹配时，在 `where { ... }` 中加入版本条件。

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

## 逻辑删除也会递增版本号

KPojo 同时使用 `@LogicDelete` 和 `@Version` 时，逻辑删除会写入删除标记并递增版本字段。

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

## Upsert 使用已配置的 mutation 路径

按匹配字段执行 `upsert()` 时，Kronos 会根据匹配结果执行 `update()` 或 `insert()`。更新分支与普通 update 一样递增版本，插入分支与普通 insert 一样初始化版本。

`onConflict()` upsert 也会在插入时初始化版本，并在冲突更新赋值中递增版本。

```kotlin group="Upsert" name="kotlin" icon="kotlin"
Product(id = 1, name = "Keyboard")
    .upsert { it.name }
    .on { it.id }
    .execute()
```

```sql group="Upsert" name="match SQL" icon="mysql"
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

```sql group="Upsert Conflict" name="Mysql" icon="mysql"
INSERT INTO `product` (`id`, `name`, `version`, `deleted`)
VALUES (:id, :name, :version, :deleted)
ON DUPLICATE KEY UPDATE
    `name` = :name,
    `version` = `version` + :version2PlusNew
```

`on(...)` 和 `onConflict()` 示例见 {{ $.keyword("mutation/upsert", ["更新插入"]) }}。
