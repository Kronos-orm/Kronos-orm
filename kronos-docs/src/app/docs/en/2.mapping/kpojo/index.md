{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Define a KPojo

`KPojo` is the table model interface used by Kronos. When a class implements `KPojo`, the compiler plugin generates table name, column metadata, primary-key strategy, `toDataMap()`, and other runtime metadata. ORM operations then read the table shape and field values from this class.

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

## Why var and defaults are used

Kronos can read `val` properties as columns, but use `var` when `update().set { ... }`, `upsert().set { ... }`, or map-based result filling needs to write a property.

```kotlin group="KPojo 2 1" name="update field" icon="kotlin"
User(id = 1)
    .update()
    .set { it.name = "Ada" }
    .where { it.id == 1 }
    .execute()
```

Default values make `User()` and `User(id = 1)` available, and they let `queryList<User>()` create result objects.

```kotlin group="KPojo 2 2" name="query" icon="kotlin"
val users: List<User> = User()
    .select()
    .where { it.age > 18 }
    .queryList()
```

## Metadata exposed by KPojo

After compilation, Kronos can read the table name, field list, and object values from the model. Application code usually does not call these methods directly, but they show how ORM SQL is built.

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

`@Ignore`, cascade properties, serialized properties, and time strategies can change the field list or write parameters. See {{ $.keyword("mapping/annotations", ["Annotations"]) }} for the complete rules.

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
