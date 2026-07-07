{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 定义 KPojo

`KPojo` 是 Kronos 的表模型接口。一个类实现 `KPojo` 后，编译器插件会为它生成表名、列信息、主键策略和 `toDataMap()` 等运行时元数据，ORM 操作就可以从这个类读取表结构和字段值。

第一个模型建议从一个小的 `data class` 开始。属性使用可空类型并提供默认值，方便 Kronos 在查询、插入、更新和结果映射时创建对象。

```kotlin group="KPojo 1" name="User.kt" icon="kotlin"
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo

@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null
) : KPojo
```

`@Table("tb_user")` 指定数据库表名。`@PrimaryKey(identity = true)` 声明自增主键。普通列来自类属性，列名默认按当前列名策略生成。

## 为什么使用 var 和默认值

Kronos 可以读取 `val` 属性作为列，但 `update().set { ... }`、`upsert().set { ... }` 和 Map 结果回填需要可写属性时，应使用 `var`。

```kotlin group="KPojo 2 1" name="update field" icon="kotlin"
User(id = 1)
    .update()
    .set { it.name = "Ada" }
    .where { it.id == 1 }
    .execute()
```

默认值让 `User()`、`User(id = 1)` 这样的写法可用，也让 `queryList<User>()` 能创建结果对象。

```kotlin group="KPojo 2 2" name="query" icon="kotlin"
val users: List<User> = User()
    .select()
    .where { it.age > 18 }
    .queryList()
```

## KPojo 会提供哪些元数据

编译后，Kronos 可以从模型读取表名、字段列表和对象值。普通业务代码通常不需要直接调用这些方法，但它们解释了 ORM 如何生成 SQL。

```kotlin group="KPojo 3" name="metadata" icon="kotlin"
val user = User(id = 1, name = "Ada")

val tableName = user.__tableName
val columns = user.kronosColumns().map { it.name }
val values = user.toDataMap()
```

```text group="KPojo 3" name="metadata shape"
tableName -> tb_user
columns -> [id, name, age]
values -> {id=1, name=Ada, age=null}
```

`@Ignore`、级联属性、序列化属性和时间策略会影响字段列表或写入参数。完整规则见 {{ $.keyword("mapping/annotations", ["注解"]) }}。

## 创建表和查询

Code First 项目可以用 KPojo 元数据创建表，再执行普通 CRUD。

```kotlin group="KPojo 4" name="create and insert" icon="kotlin"
val wrapper = Kronos.dataSource()

if (!wrapper.table.exists<User>()) {
    wrapper.table.createTable(User())
}

User(name = "Ada", age = 20)
    .insert()
    .execute()
```

```sql group="KPojo 4" name="MySQL" icon="mysql"
CREATE TABLE IF NOT EXISTS `tb_user` (
    `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255),
    `age` INT
)
```

更多 DDL、同步表和 CTAS 边界见 {{ $.keyword("database/table-operations", ["表操作"]) }}。如果已有数据库，需要从表结构生成 KPojo，见 {{ $.keyword("resources/database-first", ["Database First"]) }}。

## 下一步

- 表名、列名、非空、默认值和时间字段：{{ $.keyword("mapping/table-and-column", ["表与列"]) }}。
- 主键生成策略：{{ $.keyword("mapping/primary-key", ["主键"]) }}。
- Kotlin 类型到数据库类型：{{ $.keyword("mapping/column-types", ["列类型"]) }}。
- 索引：{{ $.keyword("mapping/indexes", ["索引"]) }}。
- 完整注解参数：{{ $.keyword("mapping/annotations", ["注解"]) }}。
- 级联和序列化：{{ $.keyword("mapping/cascade-mapping", ["级联映射"]) }}、{{ $.keyword("mapping/serialization", ["序列化"]) }}。
