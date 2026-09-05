{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Define a KPojo

Create a data class and implement `KPojo`. Kronos reads the class, annotations, and compiler-plugin metadata to build SQL.

```kotlin group="Model" name="User.kt" icon="kotlin"
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    @CreateTime
    var createTime: LocalDateTime? = null,
    @UpdateTime
    var updateTime: LocalDateTime? = null
) : KPojo
```

For mapping rules, see {{ $.keyword("mapping/kpojo", ["KPojo"]) }}, {{ $.keyword("mapping/annotations", ["Annotations"]) }}, and {{ $.keyword("mapping/primary-key", ["Primary Key"]) }}.

## Configure a wrapper

Set the default wrapper once before using terminal methods such as `execute()`, `toList()`, and `first()`.

```kotlin group="Wrapper" name="Main.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import org.apache.commons.dbcp2.BasicDataSource

val wrapper = KronosJdbcWrapper(
    BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC"
        username = "user"
        password = "******"
    }
)

Kronos.dataSource = { wrapper }
```

Connection details are covered in {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }}.

## Create the table

Use table operations when you want Kronos to create a table from the KPojo definition.

```kotlin group="DDL" name="create table" icon="kotlin"
wrapper.table.createTable(User())
```

```sql group="DDL" name="mysql" icon="mysql"
CREATE TABLE `tb_user` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255),
    `create_time` DATETIME,
    `update_time` DATETIME
)
```

For DDL options and schema sync, see {{ $.keyword("database/table-operations", ["Table Operations"]) }} and {{ $.keyword("database/schema-sync", ["Schema Sync"]) }}.

## Insert a row

Call `insert().execute()` to write one KPojo instance.

```kotlin group="Insert" name="kotlin" icon="kotlin"
val result = User(name = "Kronos")
    .insert()
    .execute()

val affectedRows = result.affectedRows
```

```sql group="Insert" name="mysql" icon="mysql"
INSERT INTO `tb_user` (`name`, `create_time`, `update_time`)
VALUES (:name, :createTime, :updateTime)
```

For generated identity values, see {{ $.keyword("mutation/last-insert-id", ["Last Insert Id"]) }}.

## Query by example

An empty `where()` uses non-empty values on the receiver object as query-by-example conditions.

```kotlin group="Select 1" name="query by example" icon="kotlin"
val user = User(name = "Kronos")
    .select()
    .where()
    .first()
```

```sql group="Select 1" name="mysql" icon="mysql"
SELECT `id`, `name`, `create_time` AS `createTime`, `update_time` AS `updateTime`
FROM `tb_user`
WHERE `tb_user`.`name` = :name
```

Use `where { ... }` when the condition should be expressed explicitly.

```kotlin group="Select 2" name="lambda" icon="kotlin"
val users = User()
    .select()
    .where { it.name == "Kronos" }
    .toList()
```

More query behavior is documented in {{ $.keyword("query/select", ["Select"]) }} and {{ $.keyword("query/conditions", ["Conditions"]) }}.
