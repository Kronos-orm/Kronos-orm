{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 声明逻辑删除列

在标记删除状态的 KPojo 字段上使用 `@LogicDelete`。该注解默认启用。

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

应用中多个表使用同一个逻辑删除字段时，可以配置全局策略，并在需要启用的 KPojo 中保留该字段。

```kotlin group="KPojo 2" name="global config" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field

with(Kronos) {
    logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
}
```

## 删除会写入删除标记

启用逻辑删除后，`delete().execute()` 生成写入删除标记的 `UPDATE`。生成条件也会排除已经删除的行。

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

当前删除需要生成物理 `DELETE` 时，调用 `.logic(false)`。

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

## 查询和更新会排除已删除行

对启用逻辑删除的 KPojo，select 和 update 会追加活动行条件。

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

## 插入和更新插入会初始化标记

insert 会把逻辑删除字段初始化为活动值。策略启用时，`onConflict()` upsert 也会把该字段加入插入列。

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

## DataGuard 边界

逻辑删除会把 delete 转成 update。没有用户条件时，生成语句仍可能命中全部活动行。

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

需要拒绝全表写入时，启用 {{ $.keyword("advanced/data-guard", ["DataGuard"]) }}。条件 API 见 {{ $.keyword("mutation/delete", ["删除"]) }} 和 {{ $.keyword("mutation/update", ["更新"]) }}。
