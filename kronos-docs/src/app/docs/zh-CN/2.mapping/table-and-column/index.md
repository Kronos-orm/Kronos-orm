{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 指定表名和列名

数据库表名需要明确指定时，在 `KPojo` 类上使用 `@Table`。数据库列名需要和 Kotlin 属性名不同时，在属性上使用 `@Column`。这些注解的优先级高于 {{ $.keyword("configuration/naming-strategy", ["命名策略"]) }}。

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

表元数据形态如下：

| Kotlin 属性 | 数据库列 | DDL 行为 |
|-------------|----------|----------|
| `id` | `id` | 主键自增列。 |
| `username` | `user_name` | `VARCHAR(80) NOT NULL`。 |
| `loginCount` | `loginCount` | 默认值 `0`。 |
| `createdAt` | `createdAt` | 创建时间策略填充的字段。 |
| `updatedAt` | `updatedAt` | 更新时间策略填充的字段。 |

## 非空和默认值

表 DDL 中，属性默认可空；主键字段或使用 `@NonNull` 的字段会生成非空约束。`@Default` 中填写数据库默认值表达式。

```sql group="TableColumn 2 1" name="MySQL" icon="mysql"
CREATE TABLE IF NOT EXISTS `tb_account` (
    `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `user_name` VARCHAR(80) NOT NULL,
    `loginCount` INT DEFAULT 0,
    `createdAt` DATETIME,
    `updatedAt` DATETIME
)
```

字符串字面量默认值需要把引号写进注解值：

```kotlin group="TableColumn 2 2" name="default" icon="kotlin"
@Default("'active'")
var status: String? = null
```

```sql group="TableColumn 2 2" name="default sql" icon="mysql"
`status` VARCHAR(255) DEFAULT 'active'
```

## 创建时间和更新时间

需要在插入、更新或 upsert 时写入当前时间的属性，可以使用 `@CreateTime` 和 `@UpdateTime`。

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

插入参数形态：

```text group="TableColumn 3 2" name="insert params"
createdAt -> 2026-07-06T10:15:30
updatedAt -> 2026-07-06T10:15:30
```

更新参数形态：

```text group="TableColumn 3 3" name="update params"
updatedAt -> 2026-07-06T10:20:00
```

完整注解参数见 {{ $.keyword("mapping/annotations", ["注解"]) }}，Kotlin 类型推断见 {{ $.keyword("mapping/column-types", ["列类型"]) }}，表索引映射见 {{ $.keyword("mapping/indexes", ["索引"]) }}。
