{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Add a lock to select

Use `lock(...)` on a select chain when the generated query should include a pessimistic lock. Calling `lock()` without arguments uses `SqlLock.Update()`.

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

Kronos stores the lock on the select task and renders it through the active database dialect. For query fields, conditions, and result methods, see {{ $.keyword("query/select", ["Select"]) }} and {{ $.keyword("query/result-methods", ["Result Methods"]) }}.

## Use a shared lock

Use `SqlLock.Share()` when the generated query should request a shared/read lock.

```kotlin group="Lock 2" name="share lock" icon="kotlin"
val users = User()
    .select()
    .where { it.status == "ACTIVE" }
    .lock(SqlLock.Share())
    .toList()
```

Shared and update locks are rendered by the SQL dialect. Keep lock usage close to the transaction boundary that needs the locked read.

## Regular upsert can use a lock

Regular `upsert().on { ... }` checks whether a matching row exists before choosing update or insert. The match query uses `SqlLock.Update()` by default when the optimistic lock strategy is not enabled. Use `.lock(...)` to make the lock explicit or to set a different lock.

```kotlin group="Lock 3" name="upsert" icon="kotlin"
import com.kotlinorm.syntax.statement.SqlLock

User(id = 1, name = "Kronos")
    .upsert { it.name }
    .on { it.id }
    .lock(SqlLock.Update())
    .execute()
```

For conflict-field and update-field examples, see {{ $.keyword("mutation/upsert", ["Upsert"]) }}.

## Optimistic locking is a mutation strategy

Optimistic lock behavior is configured through `@Version` or `Kronos.optimisticLockStrategy`. It initializes and increments the version field during insert, update, logical delete, and upsert paths.

```kotlin group="Lock 4" name="version" icon="kotlin"
data class Product(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Version
    var version: Int? = null
) : KPojo
```

For version-field behavior, see {{ $.keyword("mutation/optimistic-lock", ["Optimistic Lock"]) }} and {{ $.keyword("configuration/common-strategy", ["Common Strategy"]) }}.
