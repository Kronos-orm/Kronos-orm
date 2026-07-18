{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 定义 KPojo

创建数据类并实现 `KPojo`。Kronos 会读取类、注解和编译插件生成的元数据来构建 SQL。

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

映射规则见 {{ $.keyword("mapping/kpojo", ["KPojo"]) }}、{{ $.keyword("mapping/annotations", ["注解"]) }} 和 {{ $.keyword("mapping/primary-key", ["主键"]) }}。

## 配置 wrapper

在调用 `execute()`、`toList()`、`first()` 等终端方法前，先设置默认 wrapper。

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

连接细节见 {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }}。

## 创建表

需要从 KPojo 定义创建表时，使用 table operation。

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

DDL 选项和表结构同步见 {{ $.keyword("database/table-operations", ["表操作"]) }} 和 {{ $.keyword("database/schema-sync", ["表结构同步"]) }}。

## 插入一行

调用 `insert().execute()` 写入一个 KPojo 实例。

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

生成自增值的读取方式见 {{ $.keyword("mutation/last-insert-id", ["Last Insert Id"]) }}。

## 按对象值查询

空 `where()` 会把接收者对象上的非空值作为 query-by-example 条件。

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

需要显式表达条件时，使用 `where { ... }`。

```kotlin group="Select 2" name="lambda" icon="kotlin"
val users = User()
    .select()
    .where { it.name == "Kronos" }
    .toList()
```

更多查询行为见 {{ $.keyword("query/select", ["Select"]) }} 和 {{ $.keyword("query/conditions", ["条件 DSL"]) }}。
