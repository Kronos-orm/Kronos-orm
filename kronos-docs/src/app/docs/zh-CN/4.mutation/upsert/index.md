{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

在Kronos中，我们可以使用`KPojo.upsert().execute()`方法用于向数据库中插入或更新记录。

由于各个数据库的实现不同，因此在Kronos中，我们对`upsert`操作进行了统一的封装，以实现跨数据库的兼容性。

更新和插入路径中的版本字段行为见 {{ $.keyword("mutation/optimistic-lock", ["乐观锁"]) }}。upsert 恢复逻辑删除记录的行为见 {{ $.keyword("mutation/logic-delete", ["逻辑删除"]) }}。

## {{ $.title("on") }} 设置匹配字段

`on`字段用于匹配已有记录。匹配到记录时执行更新；没有匹配记录时执行插入。`on`字段可以是单个字段，也可以是多个字段；它们用于生成匹配查询和 update 条件，不会自动创建数据库唯一约束。

如果匹配到的是已逻辑删除记录，Kronos 会更新原行，并把逻辑删除标记恢复为活动值。

普通 `on { ... }` upsert 会先执行匹配查询，再选择 insert 或 update 分支。未启用乐观锁策略时，匹配查询默认使用更新锁。

```kotlin group="Case 1" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert()
  .on { [it.id, it.name] }
  .execute()
```

```sql group="Case 1" name="Mysql" icon="mysql"
SELECT COUNT(1) FROM `user` WHERE `id` = :id and `name` = :name LIMIT 1 FOR UPDATE;
# 若记录存在，则更新
UPDATE `user` SET `id` = :id, `name` = :name, `age` = :age WHERE `id` = :id and `name` = :name;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name LIMIT 1 FOR UPDATE;
# 若记录存在，则更新
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 1" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name LIMIT 1;
# 若记录存在，则更新
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WITH (UPDLOCK, ROWLOCK) WHERE [id] = :id and [name] = :name;
# 若记录存在，则更新
UPDATE [user] SET [id] = :id, [name] = :name, [age] = :age WHERE [id] = :id and [name] = :name;
# 若记录不存在，则插入
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 1" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name FETCH NEXT 1 ROWS ONLY FOR UPDATE;
# 若记录存在，则更新
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

## {{ $.title("upsert") }} 设置更新字段

用于指定当记录存在时需要更新的字段。

```kotlin group="Case 2" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { it.name }
  .on { it.id }
  .execute()
```

```sql group="Case 2" name="Mysql" icon="mysql"
SELECT COUNT(1) FROM `user` WHERE `id` = :id LIMIT 1 FOR UPDATE;
# 若记录存在，则更新
UPDATE `user` SET `name` = :name WHERE `id` = :id;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id LIMIT 1 FOR UPDATE;
# 若记录存在，则更新
UPDATE "user" SET "name" = :name WHERE "id" = :id;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 2" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM "user" WHERE "id" = :id LIMIT 1;
# 若记录存在，则更新
UPDATE "user" SET "name" = :name WHERE "id" = :id;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WITH (UPDLOCK, ROWLOCK) WHERE [id] = :id;
# 若记录存在，则更新
UPDATE [user] SET [name] = :name WHERE [id] = :id;
# 若记录不存在，则插入
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id FETCH NEXT 1 ROWS ONLY FOR UPDATE;
# 若记录存在，则更新
UPDATE "user" SET "name" = :name WHERE "id" = :id;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

## {{ $.title("upsert") }} {{ $.title("-") }}设置排除的字段

用于指定当记录存在时不需要更新的字段。

```kotlin group="Case 3" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { it - it.id }
  .on { it.id }
  .execute()
```

```sql group="Case 3" name="Mysql" icon="mysql"
SELECT COUNT(1) FROM `user` WHERE `id` = :id LIMIT 1 FOR UPDATE;
# 若记录存在，则更新
UPDATE `user` SET `name` = :name, `age` = :age WHERE `id` = :id;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id LIMIT 1 FOR UPDATE;
# 若记录存在，则更新
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 3" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM "user" WHERE "id" = :id LIMIT 1;
# 若记录存在，则更新
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WITH (UPDLOCK, ROWLOCK) WHERE [id] = :id;
# 若记录存在，则更新
UPDATE [user] SET [name] = :name, [age] = :age WHERE [id] = :id;
# 若记录不存在，则插入
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id FETCH NEXT 1 ROWS ONLY FOR UPDATE;
# 若记录存在，则更新
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

## {{ $.title("onConflict") }} 按数据库唯一约束处理冲突

调用`onConflict()`表示：插入记录；命中数据库唯一约束时更新记录。Kronos 会按当前方言生成需要的 SQL。冲突目标必须指定为某个 key 时，可以在`onConflict()`前调用`on { ... }`。没有显式调用`on { ... }`时，Kronos 会从 KPojo 唯一性元数据推导目标：优先使用有值的主键，其次使用字段值完整的 `@TableIndex(type = "UNIQUE")` / `@TableIndex(method = "UNIQUE")`。

策略字段会在两个路径中维护：插入时初始化创建时间、更新时间、逻辑删除和版本字段；冲突更新时刷新更新时间、恢复逻辑删除活动值并递增版本字段。

```kotlin group="Case 4" name="kotlin" icon="kotlin" {7-10}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { [it.name, it.age] }
  .onConflict()
  .execute()
```

```sql group="Case 4" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`, `age`)
VALUES (:id, :name, :age)
ON DUPLICATE KEY UPDATE `name` = :name, `age` = :age;
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
INSERT INTO "user" ("id", "name", "age")
VALUES (:id, :name, :age)
ON CONFLICT ("id") DO UPDATE SET "name" = :name, "age" = :age;
```

```sql group="Case 4" name="SQLite" icon="sqlite"
INSERT INTO "user" ("id", "name", "age")
VALUES (:id, :name, :age)
ON CONFLICT ("id") DO UPDATE SET "name" = :name, "age" = :age;
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
MERGE INTO [user] AS [t1]
USING (SELECT :id AS [id], :name AS [name], :age AS [age]) AS [t2]
ON ([t1].[id] = [t2].[id])
WHEN MATCHED THEN UPDATE SET [t1].[name] = :name, [t1].[age] = :age
WHEN NOT MATCHED THEN INSERT ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 4" name="Oracle" icon="oracle"
MERGE INTO "USER" "T1"
USING (SELECT :id AS "ID", :name AS "NAME", :age AS "AGE") "T2"
ON ("T1"."ID" = "T2"."ID")
WHEN MATCHED THEN UPDATE SET "T1"."NAME" = :name, "T1"."AGE" = :age
WHEN NOT MATCHED THEN INSERT ("ID", "NAME", "AGE") VALUES (:id, :name, :age)
```

显式冲突字段用于主键以外的唯一约束。例如表上存在`email`唯一键时，可以写成：

```kotlin group="Case 4-target" name="kotlin" icon="kotlin"
User(email = "ada@example.com", name = "Ada")
  .upsert { it.name }
  .on { it.email }
  .onConflict()
  .execute()
```

如果自增主键没有提供值，并且模型声明了唯一索引，`onConflict()` 可以推导该索引：

```kotlin group="Case 4-unique" name="model" icon="kotlin"
@Table("user")
@TableIndex("uk_user_email", ["email"], type = "UNIQUE")
data class User(
  @PrimaryKey(identity = true)
  var id: Int? = null,
  var email: String? = null,
  var name: String? = null
) : KPojo
```

```kotlin group="Case 4-unique" name="kotlin" icon="kotlin"
User(email = "ada@example.com", name = "Ada")
  .upsert { it.name }
  .onConflict()
  .execute()
```

```sql group="Case 4-unique" name="PostgreSQL" icon="postgres"
INSERT INTO "user" ("email", "name")
VALUES (:email, :name)
ON CONFLICT ("email") DO UPDATE SET "name" = :name;
```

复合唯一索引推导要求索引里的每个字段都有值。存在多个可能唯一键时，请使用显式 `on { ... }` 让目标明确。

### 使用标量子查询设置冲突更新值

`set`可以把标量子查询赋给`onConflict`冲突更新的字段。生成的冲突更新 SQL 会保留该标量子查询作为赋值表达式。

```kotlin group="Case 4-1" name="kotlin" icon="kotlin"
Order(id = 1, status = 0)
  .upsert()
  .set {
      it.status = (Order()
          .select { order -> order.status }
          .where { order -> order.userId == 44 }
          .limit(1) as Int?)
  }
  .onConflict()
  .execute()
```

```sql group="Case 4-1" name="Mysql" icon="mysql"
INSERT INTO `order` (`id`, `status`)
VALUES (:id, :status)
ON DUPLICATE KEY UPDATE `status` = (
    SELECT `status`
    FROM `order`
    WHERE `order`.`user_id` = :userId
    LIMIT 1
)
```

```sql group="Case 4-1" name="PostgreSQL" icon="postgres"
INSERT INTO "order" ("id", "status")
VALUES (:id, :status)
ON CONFLICT ("id") DO UPDATE SET "status" = (
    SELECT "status"
    FROM "order"
    WHERE "order"."user_id" = :userId
    LIMIT 1
)
```

更多标量子查询赋值写法见 {{ $.keyword("query/subqueries", ["子查询"]) }}。

### 使用 patch 动态设置冲突更新值

`patch(...)` 会把字段加入冲突更新列表，并可以提供 `onConflict()` 使用的更新值。冲突更新值需要动态决定时使用它。

```kotlin group="Case 4-2" name="SqlExpr and function" icon="kotlin"
import com.kotlinorm.beans.dsl.KronosFunctionExpr
import com.kotlinorm.syntax.expr.SqlExpr

User(id = 7, name = "seed", count = 2)
  .upsert { it.name }
  .patch(
      "count" to SqlExpr.NumberLiteral("10"),
      "name" to KronosFunctionExpr(SqlExpr.StringLiteral("patched"), "literal")
  )
  .onConflict()
  .execute()
```

```sql group="Case 4-2" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`, `count`)
VALUES (:id, :name, :count)
ON DUPLICATE KEY UPDATE `name` = 'patched', `count` = 10
```

冲突更新需要复用另一列表达式时，可以传入 `Field`。

```kotlin group="Case 4-3" name="field expression" icon="kotlin"
val countField = User().__columns.single { it.name == "count" }

User(id = 8, name = "seed", count = 5)
  .upsert { it.name }
  .patch("name" to countField)
  .onConflict()
  .execute()
```

```sql group="Case 4-3" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`, `count`)
VALUES (:id, :name, :count)
ON DUPLICATE KEY UPDATE `name` = `user`.`count`
```

更新值来自标量子查询时，可以传入 `KSelectable`。

```kotlin group="Case 4-4" name="scalar subquery" icon="kotlin"
User(id = 1, name = "seed")
  .upsert()
  .patch(
      "name" to Order()
          .select { it.status }
          .where { it.status == 15 }
          .limit(1)
  )
  .onConflict()
  .execute()
```

```sql group="Case 4-4" name="Mysql" icon="mysql"
INSERT INTO `user` (`id`, `name`)
VALUES (:id, :name)
ON DUPLICATE KEY UPDATE `name` = (
    SELECT `status`
    FROM `order`
    WHERE `order`.`status` = :status
    LIMIT 1
)
```

`patch(...)` 的值会作为 `onConflict()` 路径中的冲突更新赋值。普通 upsert 路径中，相同字段会进入匹配后的 update set。

## {{ $.title("lock") }} 设置查询时行锁

普通 `on { ... }` upsert 默认会给匹配查询使用更新锁。需要显式声明锁类型时，可以调用 `lock(...)`。

```kotlin group="Case 18" name="kotlin" icon="kotlin" {1-3}
import com.kotlinorm.syntax.statement.SqlLock

val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { [it.name, it.age] }
  .on { it.id }
  .lock(SqlLock.Update())
  .execute()
```

```sql group="Case 18" name="Mysql" icon="mysql"
SELECT COUNT(1) FROM `user` WHERE `id` = :id LIMIT 1 FOR UPDATE;
# 若记录存在，则更新
UPDATE `user` SET `name` = :nameNew, `age` = :ageNew WHERE `id` = :id;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

## 影响行数

在Kronos中，我们可以使用`upsert`方法的`execute`方法获取影响行数。

```kotlin name="demo" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

val (affectedRows) = user
                .upsert()
                .on { it.id }
                .execute()
```

## 批量更新或插入记录

同一条 upsert SQL 需要用多组参数执行时，见 {{ $.keyword("mutation/batch-operations", ["批量操作"]) }}。

## 指定使用的数据源

在Kronos中，我们可以将`KronosDataSourceWrapper`传入`execute`方法，以实现自定义的数据库连接。

```kotlin name="demo" icon="kotlin" {9-12}
val customWrapper = CustomWrapper()

val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { [it.name, it.age] }
  .on { it.id }
  .execute(customWrapper)
```
