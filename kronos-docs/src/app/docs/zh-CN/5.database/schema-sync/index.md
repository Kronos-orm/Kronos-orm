{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`syncTable()` 会读取 KPojo 的表、列、索引和注释元数据，并按当前数据库方言执行需要的 DDL。

> **Note**
> 表操作 API 速查见 {{ $.keyword("database/table-operations", ["表操作"]) }}。方言 DDL 输出见 {{ $.keyword("database/dialect-support", ["数据库方言支持"]) }}。

## 同步 KPojo 表

配置数据源后，调用 `syncTable(User())` 或 `syncTable<User>()`。

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

表不存在时，Kronos 会创建表并返回 `false`。

```text group="Sync 2" name="result"
existed == false
```

表已存在时，Kronos 会比较列和索引，执行差异 DDL，并返回 `true`。

```text group="Sync 3" name="result"
existed == true
```

## 添加缺失字段

如果数据库表已有 `id` 和 `email`，在 KPojo 中加入 `status` 后，可以生成 `ALTER TABLE` 语句。

```sql group="AddColumn" name="Mysql" icon="mysql"
ALTER TABLE `tb_user`
ADD COLUMN `status` VARCHAR(40) NULL
```

```sql group="AddColumn" name="PostgreSQL" icon="postgres"
ALTER TABLE "tb_user"
ADD COLUMN "status" VARCHAR(40) NULL
```

## 同步索引

`@TableIndex` 元数据会参与表结构同步。

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

PostgreSQL 并发索引使用 `@TableIndex` 的 `concurrently` 标记。

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

## 保护结构变更

`syncTable()` 检测到差异时会执行 `ALTER` 操作。应用代码只允许指定表执行结构变更时，可以启用 DataGuard。

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

`dropTable()` 和 `truncateTable()` 等破坏性表操作见 {{ $.keyword("advanced/data-guard", ["DataGuard"]) }}。
