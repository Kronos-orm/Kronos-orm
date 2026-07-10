{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 给 select 添加锁

数据库查询需要悲观锁时，在 select 链路上使用 `lock(...)`。不传参数调用 `lock()` 时，默认使用 `SqlLock.Update()`。

```kotlin group="Lock 1" name="update lock" icon="kotlin"
import com.kotlinorm.syntax.statement.SqlLock

val user = User()
    .select()
    .where { it.id == 1 }
    .lock(SqlLock.Update())
    .first()
```

```sql group="Lock 1" name="mysql" icon="mysql"
SELECT `id`, `name`
FROM `user`
WHERE `user`.`id` = :id
FOR UPDATE
```

Kronos 会把锁信息记录到 select 任务中，并由当前数据库方言渲染。查询字段、条件和结果方法见 {{ $.keyword("query/select", ["查询"]) }} 和 {{ $.keyword("query/result-methods", ["结果方法"]) }}。

## 使用共享锁

查询需要共享锁或读锁时，使用 `SqlLock.Share()`。

```kotlin group="Lock 2" name="share lock" icon="kotlin"
val users = User()
    .select()
    .where { it.status == "ACTIVE" }
    .lock(SqlLock.Share())
    .toList()
```

共享锁和更新锁都会按当前 SQL 方言渲染。请把锁的使用放在需要锁定读取的事务边界附近。

## Upsert fallback 可以使用锁

fallback upsert 会先检查行是否存在，再选择 update 或 insert。需要锁定这次存在性检查时，使用 `.lock(...)`。调用 `.lock()` 也会让本次 upsert 不走乐观锁 fallback 路径。

```kotlin group="Lock 3" name="upsert" icon="kotlin"
User(id = 1, name = "Kronos")
    .upsert { it.name }
    .on { it.id }
    .lock(SqlLock.Update())
    .execute()
```

冲突字段和更新字段示例见 {{ $.keyword("mutation/upsert", ["更新插入"]) }}。

## 乐观锁属于 mutation 策略

乐观锁通过 `@Version` 或 `Kronos.optimisticLockStrategy` 配置。它会在 insert、update、逻辑删除和 fallback upsert 路径中初始化或递增版本字段。

```kotlin group="Lock 4" name="version" icon="kotlin"
data class Product(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Version
    var version: Int? = null
) : KPojo
```

版本字段行为见 {{ $.keyword("mutation/optimistic-lock", ["乐观锁"]) }} 和 {{ $.keyword("configuration/common-strategy", ["通用策略"]) }}。
