{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`syncTable()` reads KPojo table, column, index, and comment metadata, then applies the required DDL for the active database dialect.

> **Note**
> For the full table operation API list, see {{ $.keyword("database/table-operations", ["Table Operations"]) }}. For dialect-specific DDL output, see {{ $.keyword("database/dialect-support", ["Database Dialect Support"]) }}.

## Sync a KPojo table

Call `syncTable(User())` or `syncTable<User>()` after configuring the data source.

```kotlin group="Sync 1" name="kotlin" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
@TableIndex("idx_user_email", ["email"])
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @ColumnType(KColumnType.VARCHAR, length = 120)
    var email: String? = null,
    @ColumnType(KColumnType.VARCHAR, length = 40)
    var status: String? = null
) : KPojo

val existed = Kronos.dataSource.table.syncTable(User())
```

When the table is missing, Kronos creates it and returns `false`.

```text group="Sync 2" name="result"
existed == false
```

When the table already exists, Kronos compares columns and indexes, executes the diff, and returns `true`.

```text group="Sync 3" name="result"
existed == true
```

## Add a missing column

If the database table already has `id` and `email`, adding `status` to the KPojo can produce an `ALTER TABLE` statement.

```sql group="AddColumn" name="Mysql" icon="mysql"
ALTER TABLE `tb_user`
ADD COLUMN `status` VARCHAR(40) NULL
```

```sql group="AddColumn" name="PostgreSQL" icon="postgres"
ALTER TABLE "tb_user"
ADD COLUMN "status" VARCHAR(40) NULL
```

## Sync indexes

`@TableIndex` metadata participates in schema sync.

```kotlin group="Index 1" name="kotlin" icon="kotlin"
@Table("tb_user")
@TableIndex("idx_user_email", ["email"])
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var email: String? = null
) : KPojo

Kronos.dataSource.table.syncTable(User())
```

```sql group="Index 1" name="Mysql" icon="mysql"
CREATE INDEX `idx_user_email` ON `tb_user` (`email`)
```

PostgreSQL concurrent indexes use the `concurrently` flag from `@TableIndex`.

```kotlin group="Index 2" name="concurrently" icon="kotlin"
@TableIndex("idx_user_email", ["email"], concurrently = true)
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var email: String? = null
) : KPojo
```

```sql group="Index 2" name="PostgreSQL" icon="postgres"
CREATE INDEX CONCURRENTLY "idx_user_email" ON "tb_user" ("email")
```

## Guard schema changes

`syncTable()` executes `ALTER` operations when it detects differences. Enable DataGuard when application code should allow schema changes only for known tables.

```kotlin group="DataGuard" name="kotlin" icon="kotlin"
DataGuardPlugin.enable {
    alter {
        allow {
            tableName = "tb_user"
        }
    }
}

Kronos.dataSource.table.syncTable(User())
```

For destructive table operations such as `dropTable()` and `truncateTable()`, see {{ $.keyword("advanced/data-guard", ["DataGuard"]) }}.
