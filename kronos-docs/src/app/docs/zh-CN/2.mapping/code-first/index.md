{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 在 Kotlin 中定义表结构

Code First 表示以 KPojo 类作为表结构元数据来源。在 Kotlin 模型上声明表、列、主键、索引和通用策略注解，再由表操作按当前数据库方言渲染 DDL。

```kotlin group="Code First 1" name="User.kt" icon="kotlin"
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.NonNull
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Column("user_name")
    @ColumnType(KColumnType.VARCHAR, 128)
    @NonNull
    var name: String? = null,
    var email: String? = null
) : KPojo
```

映射规则见 {{ $.keyword("mapping/kpojo", ["KPojo"]) }}、{{ $.keyword("mapping/table-and-column", ["表与列"]) }}、{{ $.keyword("mapping/primary-key", ["主键"]) }} 和 {{ $.keyword("mapping/indexes", ["索引"]) }}。

## 从 KPojo 元数据创建表

表不存在时，使用 `wrapper.table.createTable(...)` 创建表。当前 wrapper 决定使用哪个数据库方言。

```kotlin group="Code First 2" name="create table" icon="kotlin"
wrapper.table.createTable(User())
```

```sql group="Code First 2" name="MySQL" icon="mysql"
CREATE TABLE `tb_user` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_name` VARCHAR(128) NOT NULL,
    `email` VARCHAR(255)
)
```

`createTable(...)` 会按当前 KPojo 结构创建表。删除表、清空表、CTAS 和生成任务细节见 {{ $.keyword("database/table-operations", ["表操作"]) }}。

## 同步已有表

表已经存在并需要与 KPojo 定义对齐时，使用 `syncTable(...)`。

```kotlin group="Code First 3" name="sync table" icon="kotlin"
val existed = wrapper.table.syncTable(User())
```

`syncTable(...)` 在创建缺失表时返回 `false`；在比较已有表并应用所需 DDL 时返回 `true`。

表结构同步行为和边界见 {{ $.keyword("database/schema-sync", ["表结构同步"]) }}。

## 数据库结构为准时使用 Database First

当 Kotlin 模型是结构来源时使用 Code First。当已有数据库元数据需要生成 KPojo 类时，使用 {{ $.keyword("resources/database-first", ["Database First"]) }} 和 {{ $.keyword("resources/codegen", ["Codegen"]) }}。
