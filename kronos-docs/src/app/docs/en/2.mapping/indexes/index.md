{% import "../../../macros/macros-en.njk" as $ %}

## Declare indexes

Kronos reads `@TableIndex` metadata when `wrapper.table.createTable(...)` or `wrapper.table.syncTable(...)` builds DDL. The runtime metadata shape is `KTableIndex`.

Current source has no public `@ColumnIndex` annotation. For a single-column index, declare a class-level `@TableIndex` with one column name.

Use {{ $.keyword("mapping/table-and-column", ["Table and Column"]) }} for table and column name mapping before adding index definitions.

## Single-column index

Use one column in `columns`.

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

## Unique composite index

Set `type = "UNIQUE"` for a unique index. Set `method` only when the target database supports the method.

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

## Concurrent PostgreSQL index

`concurrently = true` renders `CREATE INDEX CONCURRENTLY`. Kronos executes concurrent index statements outside the transactional DDL batch.

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
> `concurrently` is intended for PostgreSQL. Other databases may reject this DDL keyword.

## Runtime `KTableIndex`

`KTableIndex` is the index metadata object used by generated metadata and table synchronization. Regular model classes usually use `@TableIndex`; manually assembled metadata can create `KTableIndex` directly.

```kotlin group="KTableIndex 1" name="kotlin" icon="kotlin"
import com.kotlinorm.beans.dsl.KTableIndex

val userNameIndex = KTableIndex(
    name = "idx_user_name",
    columns = arrayOf("name"),
    type = "NORMAL",
    method = "BTREE"
)
```

Attached to table metadata, the DDL shape is:

```sql group="KTableIndex 2" name="Mysql" icon="mysql"
CREATE INDEX `idx_user_name` ON `tb_user` (`name`) USING BTREE
```

## Field behavior

| Field | Current behavior |
|------|------------------|
| `name` | Index name rendered in `CREATE INDEX`. |
| `columns` | Column names rendered in order. |
| `type` | `"UNIQUE"` marks the index as unique. `"NORMAL"` and blank are omitted. Other values are copied before `INDEX` for dialects that support them, such as MySQL `FULLTEXT`, MySQL `SPATIAL`, SQL Server `NONCLUSTERED`, or Oracle `BITMAP`. |
| `method` | Blank, `"UNIQUE"`, and values equal to `type` are omitted as methods. Other values are copied into `USING <method>`. |
| `concurrently` | Renders `CONCURRENTLY` on create/drop index statements and is meant for PostgreSQL. |

## Dialect notes

| Dialect | Practical index settings |
|---------|--------------------------|
| MySQL | Use `type = "UNIQUE"` for unique indexes. `method = "BTREE"` or `"HASH"` is rendered after the column list. `FULLTEXT` and `SPATIAL` can be passed through `type`. |
| PostgreSQL | Use `method = "BTREE"`, `"HASH"`, `"GIST"`, `"SPGIST"`, `"GIN"`, or `"BRIN"` when the database supports it. `concurrently = true` renders `CREATE INDEX CONCURRENTLY`. |
| SQLite | Use blank `type`/`method` or `type = "UNIQUE"`. SQLite create-table DDL uses `CREATE INDEX IF NOT EXISTS`. |
| SQL Server | Use `type = "CLUSTERED"`, `"NONCLUSTERED"`, `"XML"`, or `"SPATIAL"` according to the target table. `method = "UNIQUE"` also marks the index as unique. |
| Oracle | Use `type = "UNIQUE"` or `"BITMAP"` when needed. Oracle identifiers are rendered with the Oracle dialect rules. |
