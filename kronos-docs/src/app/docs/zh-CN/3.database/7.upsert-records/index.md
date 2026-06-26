{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

在Kronos中，我们可以使用`KPojo.upsert().execute()`方法用于向数据库中插入或更新记录。

由于各个数据库的实现不同，因此在Kronos中，我们对`upsert`操作进行了统一的封装，以实现跨数据库的兼容性。

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
  .on { it.id + it.name }
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
  .upsert { it.name + it.age }
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

## {{ $.title("lock") }} 设置查询时行锁

`limit`方法用于设置查询时行锁，此时Kronos会根据`lock`方法设置的锁类型进行锁的添加。

```kotlin group="Case 18" name="kotlin" icon="kotlin" {1-3}
val user: User = User(
        id = 1,
        name = "Kronos",
        age = 18
    )

user
  .upsert { it.name + it.age }
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

在Kronos中，我们可以使用`Iterable<KPojo>.upsert().execute()`或`Array<KPojo>.upsert().execute()`方法批量更新或插入记录。

```kotlin name="demo" icon="kotlin" {14-17}
val users: List<User> = listOf(
    User(
        id = 1,
        name = "Kronos",
        age = 18
    ),
    User(
        id = 2,
        name = "Kronos ORM",
        age = 18
    )
)

users
  .upsert { it.name + it.age }
  .on { it.id }
  .execute()
```

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
  .upsert { it.name + it.age }
  .on { it.id }
  .execute(customWrapper)
```
