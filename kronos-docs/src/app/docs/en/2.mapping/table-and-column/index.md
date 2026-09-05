{% import "../../../macros/macros-en.njk" as $ %}

## Name a table and its columns

Use `@Table` on the `KPojo` class when the database table name should be explicit. Use `@Column` on a property when the database column name should differ from the Kotlin property name. These annotations take priority over the naming strategies described in {{ $.keyword("configuration/naming-strategy", ["Naming Strategy"]) }}.

```kotlin group="TableColumn 1" name="model" icon="kotlin"
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.NonNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

@Table("tb_account")
data class Account(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Column("user_name")
    @ColumnType(KColumnType.VARCHAR, length = 80)
    @NonNull
    var username: String? = null,
    @Default("0")
    var loginCount: Int? = null,
    @CreateTime
    var createdAt: LocalDateTime? = null,
    @UpdateTime
    var updatedAt: LocalDateTime? = null
) : KPojo
```

The table metadata shape is:

| Kotlin property | Database column | DDL behavior |
|-----------------|-----------------|--------------|
| `id` | `id` | Primary key identity column. |
| `username` | `user_name` | `VARCHAR(80) NOT NULL`. |
| `loginCount` | `loginCount` | Default value `0`. |
| `createdAt` | `createdAt` | Creation time field populated by the creation time strategy. |
| `updatedAt` | `updatedAt` | Update time field populated by the update time strategy. |

## Nullability and defaults

Properties are nullable in table DDL unless the field is a primary key or uses `@NonNull`. Put the raw database default expression in `@Default`.

```sql group="TableColumn 2 1" name="MySQL" icon="mysql"
CREATE TABLE IF NOT EXISTS `tb_account` (
    `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `user_name` VARCHAR(80) NOT NULL,
    `loginCount` INT DEFAULT 0,
    `createdAt` DATETIME,
    `updatedAt` DATETIME
)
```

For a string literal default, include the quotes in the annotation value:

```kotlin group="TableColumn 2 2" name="default" icon="kotlin"
@Default("'active'")
var status: String? = null
```

```sql group="TableColumn 2 2" name="default sql" icon="mysql"
`status` VARCHAR(255) DEFAULT 'active'
```

## Creation and update timestamps

Use `@CreateTime` and `@UpdateTime` on the timestamp properties that should receive the current time during insert, update, or upsert operations.

```kotlin group="TableColumn 3 1" name="timestamps" icon="kotlin"
@Table("tb_account")
data class AccountTime(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @CreateTime
    var createdAt: LocalDateTime? = null,
    @UpdateTime
    var updatedAt: LocalDateTime? = null
) : KPojo
```

Insert parameter shape:

```text group="TableColumn 3 2" name="insert params"
createdAt -> 2026-07-06T10:15:30
updatedAt -> 2026-07-06T10:15:30
```

Update parameter shape:

```text group="TableColumn 3 3" name="update params"
updatedAt -> 2026-07-06T10:20:00
```

See {{ $.keyword("mapping/annotations", ["Annotations"]) }} for the full annotation parameter list, {{ $.keyword("mapping/column-types", ["Column Types"]) }} for Kotlin type inference, and {{ $.keyword("mapping/indexes", ["Indexes"]) }} for table index mapping.
