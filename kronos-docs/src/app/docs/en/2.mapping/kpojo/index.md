{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Define a KPojo

`KPojo` is the table model interface used by Kronos. When a class implements `KPojo`, Kronos can read table metadata, `toDataMap()`, and object values from it. ORM operations then use the table shape and field values from this class.

Start the first model with a small `data class`. Use nullable properties with default values so Kronos can create objects for queries, inserts, updates, and result mapping.

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

`@Table("tb_user")` names the database table. `@PrimaryKey(identity = true)` declares an identity primary key. Other properties become columns, and their database names follow the current field naming strategy unless an annotation overrides them.

A KPojo class cannot declare class-level type parameters. Use concrete property types for each KPojo; otherwise compilation reports `KRONOS_GENERIC_KPOJO_NOT_SUPPORTED`. This restriction does not apply to ordinary non-KPojo generic classes or concrete generic property types such as `List<String>` in a non-generic KPojo.

```kotlin
data class User(
    var tags: List<String>? = null
) : KPojo
```

## Why var and defaults are used

Kronos can read `val` properties as columns, but use `var` when `update().set { ... }`, `upsert().set { ... }`, or map-based result filling needs to write a property.

```kotlin group="KPojo 2 1" name="update field" icon="kotlin"
User(id = 1)
    .update()
    .set { it.name = "Ada" }
    .where { it.id == 1 }
    .execute()
```

Default values make `User()` and `User(id = 1)` available, and they let `toList<User>()` create result objects.

```kotlin group="KPojo 2 2" name="query" icon="kotlin"
val users: List<User> = User()
    .select()
    .where { it.age > 18 }
    .toList()
```

## Metadata exposed by KPojo

After compilation, Kronos can read the table name, table comment, field list, indexes, strategy fields, and object values from the model. Application code usually does not read every metadata property directly, but these properties show how ORM SQL is built.

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

`@Ignore`, cascade properties, serialized properties, and time strategies can change the field list or write parameters. See {{ $.keyword("mapping/annotations", ["Annotations"]) }} for the complete rules.

## Dynamic object tables

When the table shape is only known at runtime, create an object that implements `KPojo` and set `__kType` to `typeOf<KPojo>()`. The object carries metadata properties and ordinary Kotlin properties, so it can be used with the same select DSL. Mark explicit metadata overrides with `@Ignore([IgnoreAction.ALL])` so they are not treated as table columns.

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

Use a dynamic object table for runtime-selected table names or column lists. For static application models, prefer normal `data class` KPojo definitions with annotations.

## Create a table and query it

Code First projects can create a table from KPojo metadata, then use the normal CRUD APIs.

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

See {{ $.keyword("database/table-operations", ["Table Operations"]) }} for DDL, schema sync, and CTAS boundaries. If the database already exists and you want to generate KPojo classes from it, see {{ $.keyword("resources/database-first", ["Database First"]) }}.

## Next steps

- Table names, column names, non-null fields, defaults, and timestamp fields: {{ $.keyword("mapping/table-and-column", ["Table and Column"]) }}.
- Primary key generation: {{ $.keyword("mapping/primary-key", ["Primary Keys"]) }}.
- Kotlin types to database types: {{ $.keyword("mapping/column-types", ["Column Types"]) }}.
- Indexes: {{ $.keyword("mapping/indexes", ["Indexes"]) }}.
- Full annotation parameters: {{ $.keyword("mapping/annotations", ["Annotations"]) }}.
- Cascade and serialization: {{ $.keyword("mapping/cascade-mapping", ["Cascade Mapping"]) }} and {{ $.keyword("mapping/serialization", ["Serialization"]) }}.
