{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 级联删除策略

使用 `@Cascade` 的 `onDelete` 参数，可以决定删除根记录时如何处理关联记录。完整删除流程见 {{ $.keyword("advanced/cascade-delete", ["级联删除"]) }}。

{{ $.members([
["NO_ACTION", "不创建额外的子记录 delete 或 update 任务，默认使用该策略。", "CascadeDeleteAction"],
["CASCADE", "先删除匹配的子记录，再删除根记录。", "CascadeDeleteAction"],
["RESTRICT", "存在匹配子记录时拒绝删除。", "CascadeDeleteAction"],
["SET_NULL", "先将子记录引用列设置为<code>null</code>，再删除根记录。", "CascadeDeleteAction"],
["SET_DEFAULT", "先将子记录引用列设置为<code>defaultValue</code>中的值，再删除根记录。", "CascadeDeleteAction"]
]) }}

## 声明删除策略

子表引用属性上声明 `onDelete = CascadeDeleteAction.CASCADE`，表示删除父记录时同步删除子记录。

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

结果形态：

```text group="CASCADE 2" name="result"
Kronos 先查询根记录，再查询匹配的子记录，随后先删除子记录，最后删除根记录。
```

## 使用 SET_NULL

子记录需要保留但不再指向被删除的根记录时，使用 `SET_NULL`。

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

结果形态：

```text group="SET_NULL 2" name="result"
子记录会保留。删除父记录前，dept_id 参数会被更新为 null。
```

## 使用 SET_DEFAULT

子记录引用需要改为指定值时，使用 `SET_DEFAULT` 并设置 `defaultValue`。

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
删除 dept 前，Kronos 会把匹配 user.deptId 更新为 "0"。
```

多列引用中，`properties`、`targetProperties` 和 `defaultValue` 需要保持相同顺序。不希望修改某个列时，使用 `Cascade.RESERVED`。

```kotlin group="SET_DEFAULT 2" name="reserved" icon="kotlin"
@Cascade(
    ["deptId", "tenantId"],
    ["id", "tenantId"],
    onDelete = CascadeDeleteAction.SET_DEFAULT,
    defaultValue = [Cascade.RESERVED, "0"]
)
var dept: Dept? = null
```

## 使用 RESTRICT

只有关联记录已经被删除或解除关联后才允许删除父记录时，使用 `RESTRICT`。

```kotlin group="RESTRICT" name="kotlin" icon="kotlin"
@Cascade(["deptId"], ["id"], onDelete = CascadeDeleteAction.RESTRICT)
var dept: Dept? = null

Dept(id = 7).delete().by { it.id }.execute()
```

```text group="RESTRICT" name="result"
UnsupportedOperationException: The record cannot be deleted because it is restricted by a cascade.
```

> **Note**
> `NO_ACTION` 只表示 Kronos 不为该关系创建额外的子表任务。数据库外键约束仍按数据库表结构自身的规则生效。
