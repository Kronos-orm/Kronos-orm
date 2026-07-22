{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 定义 KPojo

`KPojo` 是 Kronos 的表模型接口。一个类实现 `KPojo` 后，Kronos 可以从它读取表元数据、`toDataMap()` 和对象值，ORM 操作就可以使用这个类上的表结构和字段值。

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

KPojo 类本身不能声明类级泛型参数。请为每个 KPojo 使用具体的属性类型；否则编译时会报告 `KRONOS_GENERIC_KPOJO_NOT_SUPPORTED`。这个限制不影响普通非 KPojo 泛型类，也不影响在非泛型 KPojo 中使用 `List<String>` 等具体泛型类型。

```kotlin
data class User(
    var tags: List<String>? = null
) : KPojo
```

## 为什么使用 var 和默认值

Kronos 可以读取 `val` 属性作为列，但 `update().set { ... }`、`upsert().set { ... }` 和 Map 结果回填需要可写属性时，应使用 `var`。

```kotlin group="KPojo 2 1" name="update field" icon="kotlin"
User(id = 1)
    .update()
    .set { it.name = "Ada" }
    .where { it.id == 1 }
    .execute()
```

默认值让 `User()`、`User(id = 1)` 这样的写法可用，也让 `toList<User>()` 能创建结果对象。

```kotlin group="KPojo 2 2" name="query" icon="kotlin"
val users: List<User> = User()
    .select()
    .where { it.age > 18 }
    .toList()
```

## KPojo 会提供哪些元数据

编译后，Kronos 可以从模型读取表名、表注释、字段列表、索引、策略字段和对象值。普通业务代码通常不需要读取每个元数据属性，但它们解释了 ORM 如何生成 SQL。

```kotlin group="KPojo 3" name="metadata" icon="kotlin"
val user = User(id = 1, name = "Ada")

val tableName = user.__tableName
val tableComment = user.__tableComment
val columns = user.__columns.map { it.name }
val indexes = user.__tableIndexes
val createTime = user.__createTime
val updateTime = user.__updateTime
val logicDelete = user.__logicDelete
val optimisticLock = user.__optimisticLock
val values = user.toDataMap()
```

```text group="KPojo 3" name="metadata shape"
tableName -> tb_user
columns -> [id, name, age]
values -> {id=1, name=Ada, age=null}
```

`@Ignore`、级联属性、序列化属性和时间策略会影响字段列表或写入参数。完整规则见 {{ $.keyword("mapping/annotations", ["注解"]) }}。

## 动态对象表

表结构只在运行时确定时，可以创建一个手动实现 `KPojo` 的对象，并把 `__kType` 设为 `typeOf<KPojo>()`。这个对象同时携带元数据属性和普通 Kotlin 属性，因此可以继续使用 select DSL。显式重载的元数据属性需要标记 `@Ignore([IgnoreAction.ALL])`，避免它们被当作表字段。

```kotlin group="KPojo Dynamic" name="dynamic table" icon="kotlin"
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KType
import kotlin.reflect.typeOf

val runtimeUser = object : KPojo {
    @Ignore([IgnoreAction.ALL])
    override var __kType: KType = typeOf<KPojo>()
    @Ignore([IgnoreAction.ALL])
    override var __tableName = "tb_runtime_user"
    @Ignore([IgnoreAction.ALL])
    override var __tableComment = "runtime user table"
    @Ignore([IgnoreAction.ALL])
    override var __columns = mutableListOf(
        Field("id", "id"),
        Field("name", "name")
    )
    @Ignore([IgnoreAction.ALL])
    override var __tableIndexes = mutableListOf<KTableIndex>()

    var id: Int? = 6
    var name: String? = null
}

val user = runtimeUser
    .select()
    .where { it.id == 6 }
    .firstOrNull()
```

动态对象表适合运行时决定表名或字段列表的场景。静态业务模型仍优先使用普通 `data class` KPojo 和注解定义。

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
