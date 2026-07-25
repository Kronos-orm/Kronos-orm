{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 声明索引

`wrapper.table.createTable(...)`或`wrapper.table.syncTable(...)`生成 DDL 时，会读取`@TableIndex`元数据。运行时的索引元数据结构是`KTableIndex`。

单列索引使用类级别的`@TableIndex`，并在`columns`中写一个列名。

添加索引定义前，可先查看 {{ $.keyword("mapping/table-and-column", ["表与列"]) }} 中的表名和列名映射。

## 单列索引

`columns`只传一个列名即可。

```kotlin group="TableIndex 1" name="single column" icon="kotlin"
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
@TableIndex(name = "idx_user_name", columns = ["name"])
data class User(
    val id: Int? = null,
    val name: String? = null
) : KPojo
```

```sql group="TableIndex 1" name="Mysql" icon="mysql"
CREATE INDEX `idx_user_name` ON `tb_user` (`name`)
```

## 组合唯一索引

设置`type = "UNIQUE"`会生成唯一索引。`method`只在目标数据库支持该索引方法时填写。

```kotlin group="TableIndex 2" name="unique composite" icon="kotlin"
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
@TableIndex(
    name = "idx_user_tenant_email",
    columns = ["tenant_id", "email"],
    type = "UNIQUE",
    method = "BTREE"
)
data class UserAccount(
    val id: Int? = null,
    @Column("tenant_id")
    val tenantId: Int? = null,
    val email: String? = null
) : KPojo
```

```sql group="TableIndex 2" name="Mysql" icon="mysql"
CREATE UNIQUE INDEX `idx_user_tenant_email` ON `tb_user` (`tenant_id`, `email`) USING BTREE
```

```sql group="TableIndex 2" name="PostgreSQL" icon="postgres"
CREATE UNIQUE INDEX "idx_user_tenant_email" ON "public"."tb_user" USING BTREE ("tenant_id", "email")
```

## PostgreSQL 并发索引

`concurrently = true`会生成`CREATE INDEX CONCURRENTLY`。Kronos 会把并发索引语句放在事务性 DDL 批次之外执行。

```kotlin group="TableIndex 3" name="concurrently" icon="kotlin"
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.interfaces.KPojo

@Table("tb_event")
@TableIndex(
    name = "idx_event_created_at",
    columns = ["created_at"],
    method = "BTREE",
    concurrently = true
)
data class EventLog(
    val id: Int? = null,
    @Column("created_at")
    val createdAt: String? = null
) : KPojo
```

```sql group="TableIndex 3" name="PostgreSQL" icon="postgres"
CREATE INDEX CONCURRENTLY "idx_event_created_at" ON "public"."tb_event" USING BTREE ("created_at")
```

> **Note**
> `concurrently`面向 PostgreSQL。其他数据库可能拒绝该 DDL 关键字。

## 运行时`KTableIndex`

`KTableIndex`是生成元数据和表结构同步使用的索引元数据对象。普通实体类通常使用`@TableIndex`；手动组装元数据时可以直接创建`KTableIndex`。

```kotlin group="KTableIndex 1" name="kotlin" icon="kotlin"
import com.kotlinorm.beans.dsl.KTableIndex

val userNameIndex = KTableIndex(
    name = "idx_user_name",
    columns = arrayOf("name"),
    type = "NORMAL",
    method = "BTREE"
)
```

挂到表元数据后，DDL 形态如下：

```sql group="KTableIndex 2" name="Mysql" icon="mysql"
CREATE INDEX `idx_user_name` ON `tb_user` (`name`) USING BTREE
```

## 字段行为

| 字段 | 当前行为 |
|------|----------|
| `name` | 渲染为`CREATE INDEX`中的索引名。 |
| `columns` | 按数组顺序渲染为索引列。 |
| `type` | `"UNIQUE"`会标记唯一索引。`"NORMAL"`和空字符串会被省略。其他值会复制到`INDEX`前，供支持该语法的方言使用，例如 MySQL `FULLTEXT`、MySQL `SPATIAL`、SQL Server `NONCLUSTERED`、Oracle `BITMAP`。 |
| `method` | 空字符串、`"UNIQUE"`、与`type`相同的值不会作为 method 渲染。其他值会生成`USING <method>`。 |
| `concurrently` | 创建/删除索引语句会渲染`CONCURRENTLY`，用于 PostgreSQL。 |

## 方言提示

| 方言 | 常用索引设置 |
|------|--------------|
| MySQL | 使用`type = "UNIQUE"`创建唯一索引。`method = "BTREE"`或`"HASH"`会渲染在列列表之后。`FULLTEXT`和`SPATIAL`可通过`type`传入。 |
| PostgreSQL | 数据库支持时可使用`method = "BTREE"`、`"HASH"`、`"GIST"`、`"SPGIST"`、`"GIN"`、`"BRIN"`。`concurrently = true`会生成`CREATE INDEX CONCURRENTLY`。 |
| SQLite | 使用空`type`/`method`或`type = "UNIQUE"`。SQLite 建表时的索引语句会带`CREATE INDEX IF NOT EXISTS`。 |
| H2 | 使用空`type`/`method`或`type = "UNIQUE"`。普通索引默认使用`BTREE`。 |
| SQL Server | 根据目标表使用`type = "CLUSTERED"`、`"NONCLUSTERED"`、`"XML"`或`"SPATIAL"`。`method = "UNIQUE"`也会标记唯一索引。 |
| Oracle | 需要时使用`type = "UNIQUE"`或`"BITMAP"`。Oracle 标识符按 Oracle 方言规则渲染。 |
| DM8 | 唯一索引使用`type = "UNIQUE"`。其他 DM8 索引选项请使用数据库专用迁移。 |
