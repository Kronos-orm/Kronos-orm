{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

在Kronos中，我们可以使用`KPojo.upsert().execute()`方法用于向数据库中插入或更新记录。

由于各个数据库的实现不同，因此在Kronos中，我们对`upsert`操作进行了统一的封装，以实现跨数据库的兼容性。

更新和插入路径中的版本字段行为见 {{ $.keyword("mutation/optimistic-lock", ["乐观锁"]) }}。

## {{ $.title("on") }} 设置唯一性约束字段

`on`方法用于唯一性设置约束字段，可以是单个字段，也可以是多个字段。当记录存在时，Kronos会根据`on`方法设置的字段生成更新条件语句，否则生成插入语句。

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
SELECT COUNT(1) FROM `user` WHERE `id` = :id and `name` = :name;
# 若记录存在，则更新
UPDATE `user` SET `id` = :id, `name` = :name, `age` = :age WHERE `id` = :id and `name` = :name;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 1" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name;
# 若记录存在，则更新
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 1" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name;
# 若记录存在，则更新
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 1" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WHERE [id] = :id and [name] = :name;
# 若记录存在，则更新
UPDATE [user] SET [id] = :id, [name] = :name, [age] = :age WHERE [id] = :id and [name] = :name;
# 若记录不存在，则插入
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 1" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name;
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
SELECT COUNT(1) FROM `user` WHERE `id` = :id;
# 若记录存在，则更新
UPDATE `user` SET `name` = :name WHERE `id` = :id;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id;
# 若记录存在，则更新
UPDATE "user" SET "name" = :name WHERE "id" = :id;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 2" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM `user` WHERE `id` = :id;
# 若记录存在，则更新
UPDATE `user` SET `name` = :name WHERE `id` = :id;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WHERE [id] = :id;
# 若记录存在，则更新
UPDATE [user] SET [name] = :name WHERE [id] = :id;
# 若记录不存在，则插入
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 2" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id;
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
SELECT COUNT(1) FROM `user` WHERE `id` = :id;
# 若记录存在，则更新
UPDATE `user` SET `name` = :name, `age` = :age WHERE `id` = :id;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="PostgreSQL" icon="postgres"
SELECT COUNT(1) FROM "user" WHERE "id" = :id;
# 若记录存在，则更新
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 3" name="SQLite" icon="sqlite"
SELECT COUNT(1) FROM `user` WHERE `id` = :id;
# 若记录存在，则更新
UPDATE `user` SET `name` = :name, `age` = :age WHERE `id` = :id;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="SQLServer" icon="sqlserver"
SELECT COUNT(1) FROM [user] WHERE [id] = :id;
# 若记录存在，则更新
UPDATE [user] SET [name] = :name, [age] = :age WHERE [id] = :id;
# 若记录不存在，则插入
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 3" name="Oracle" icon="oracle"
SELECT COUNT(1) FROM "user" WHERE "id" = :id;
# 若记录存在，则更新
UPDATE "user" SET "name" = :name, "age" = :age WHERE "id" = :id;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

## {{ $.title("onConflict") }} 设置处理策略为冲突时更新

当使用`upsert`方法时，我们可以使用`onConflict`方法设置处理策略为冲突时更新，即当记录存在时，更新记录。

```kotlin group="Case 4" name="kotlin" icon="kotlin" {7-11}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { [it.name, it.age] }
  .on { [it.name, it.age] }
  .onConflict()
  .execute()
```

```sql group="Case 4" name="Mysql" icon="mysql"
# 使用on duplicate key update 语法
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age) ON DUPLICATE KEY UPDATE `name` = :name, `age` = :age;
```

```sql group="Case 4" name="PostgreSQL" icon="postgres"
INSERT INTO "user" ("id", "name", "age") SELECT :id, :name, :age WHERE NOT EXISTS (SELECT 1 FROM "user" WHERE "name" = :name and "age" = :age);
UPDATE "user" SET "name" = :name, "age" = :age WHERE "name" = :name and "age" = :age;
```

```sql group="Case 4" name="SQLite" icon="sqlite"
# 使用on conflict 语法
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age) ON CONFLICT(`name`, `age`) DO UPDATE SET `name` = :name, `age` = :age;
```

```sql group="Case 4" name="SQLServer" icon="sqlserver"
IF EXISTS (SELECT 1 FROM [user] WHERE [name] = :name and [age] = :age)
  BEGIN
    UPDATE [user] SET [name] = :name, [age] = :age WHERE [name] = :name and [age] = :age
  END
ELSE
  BEGIN
    INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
  END
```

```sql group="Case 4" name="Oracle" icon="oracle"
BEGIN
  INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
EXCEPTION
  WHEN DUP_VAL_ON_INDEX THEN
    UPDATE "user" SET "name" = :name, "age" = :age WHERE "name" = :name and "age" = :age;
END;
```

### 使用标量子查询设置冲突更新值

`set`可以把标量子查询赋给`onConflict`冲突更新的字段。生成的冲突更新 SQL 会保留该标量子查询作为赋值表达式。

```kotlin group="Case 4-1" name="kotlin" icon="kotlin"
Order(id = 1, status = 0)
  .upsert()
  .on { it.id }
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
  .on { it.id }
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
val countField = User().kronosColumns().single { it.name == "count" }

User(id = 8, name = "seed", count = 5)
  .upsert { it.name }
  .patch("name" to countField)
  .on { it.id }
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
  .on { it.id }
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

`patch(...)` 的值会作为 `onConflict()` 路径中的冲突更新赋值。fallback upsert 路径中，相同字段会进入存在性检查后的 update set。

## {{ $.title("lock") }} 设置查询时行锁

`lock`方法用于给 fallback upsert 流程中的存在性检查添加行锁。

```kotlin group="Case 18" name="kotlin" icon="kotlin" {1-3}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { [it.name, it.age] }
  .lock()
  .execute()
```

```sql group="Case 18" name="Mysql" icon="mysql"
SELECT `id`, `name`, `age` FROM `user` FOR UPDATE
# 若记录存在，则更新
UPDATE `user` SET `name` = :name WHERE `id` = :id;
# 若记录不存在，则插入
INSERT INTO `user` (`id`, `name`, `age`) VALUES (:id, :name, :age);
```

```sql group="Case 18" name="PostgreSQL" icon="postgres"
SELECT "id", "name", "age" FROM "user" FOR UPDATE
# 若记录存在，则更新
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 18" name="SQLite" icon="sqlite"
# 不支持对Sqlite添加行锁功能因为Sqlite本身没有行锁功能
SELECT COUNT(1) FROM "user" WHERE "id" = :id and "name" = :name;
# 若记录存在，则更新
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
```

```sql group="Case 18" name="SQLServer" icon="sqlserver"
SELECT [id], [name], [age] FROM [user] OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY ROWLOCK
# 若记录存在，则更新
UPDATE [user] SET [id] = :id, [name] = :name, [age] = :age WHERE [id] = :id and [name] = :name;
# 若记录不存在，则插入
INSERT INTO [user] ([id], [name], [age]) VALUES (:id, :name, :age);
```

```sql group="Case 18" name="Oracle" icon="oracle"
SELECT "id", "name", "age" FROM "user" FOR UPDATE(NOWAIT)
# 若记录存在，则更新
UPDATE "user" SET "id" = :id, "name" = :name, "age" = :age WHERE "id" = :id and "name" = :name;
# 若记录不存在，则插入
INSERT INTO "user" ("id", "name", "age") VALUES (:id, :name, :age);
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

一条原生 upsert SQL 需要用多组参数执行时，见 {{ $.keyword("mutation/batch-operations", ["批量操作"]) }}。

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
