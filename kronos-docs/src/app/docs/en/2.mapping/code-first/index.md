{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Define the schema in Kotlin

Code First means the KPojo class is the source of table metadata. Add table, column, primary key, index, and strategy annotations to the Kotlin model, then let table operations render DDL for the current database dialect.

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

For focused mapping rules, see {{ $.keyword("mapping/kpojo", ["KPojo"]) }}, {{ $.keyword("mapping/table-and-column", ["Table and Column"]) }}, {{ $.keyword("mapping/primary-key", ["Primary Key"]) }}, and {{ $.keyword("mapping/indexes", ["Indexes"]) }}.

## Create the table from KPojo metadata

Use `wrapper.table.createTable(...)` when the table does not exist yet. The active wrapper determines the database dialect.

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

`createTable(...)` creates from the current KPojo shape. Use {{ $.keyword("database/table-operations", ["Table Operations"]) }} for drop, truncate, CTAS, and generated task details.

## Synchronize an existing table

Use `syncTable(...)` when the table already exists and should be aligned with the KPojo definition.

```kotlin group="Code First 3" name="sync table" icon="kotlin"
val existed = wrapper.table.syncTable(User())
```

`syncTable(...)` returns `false` when it creates a missing table, and `true` when it compares an existing table and applies the required DDL.

For schema synchronization behavior and limits, see {{ $.keyword("database/schema-sync", ["Schema Sync"]) }}.

## Use Database First when the database owns the schema

Use Code First when Kotlin models are the source of truth. Use {{ $.keyword("resources/database-first", ["Database First"]) }} and {{ $.keyword("resources/codegen", ["Codegen"]) }} when existing database metadata should generate KPojo classes.
