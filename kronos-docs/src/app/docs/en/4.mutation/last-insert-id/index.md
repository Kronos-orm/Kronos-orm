{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Use cases

`LastInsertIdPlugin` reads the generated ID after an insert on an identity primary key.

```kotlin group="KPojo" name="User.kt" icon="kotlin"
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.interfaces.KPojo

data class User(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    var name: String? = null
) : KPojo
```

> **Note**
> `lastInsertId` is available when the inserted KPojo uses `@PrimaryKey(identity = true)` and the insert lets the database generate the primary key value.

## Read {{ $.title("lastInsertId") }} from an insert result

Enable `LastInsertIdPlugin` globally when every identity insert should collect the generated ID.

```kotlin group="Read Id" name="kotlin" icon="kotlin"
import com.kotlinorm.plugins.LastInsertIdPlugin
import com.kotlinorm.plugins.LastInsertIdPlugin.lastInsertId

LastInsertIdPlugin.enabled = true

val result = User(name = "Kronos")
    .insert()
    .execute()

val affectedRows = result.affectedRows
val lastInsertId = result.lastInsertId
```

The result is a `KronosOperationResult`. `affectedRows` comes from the insert execution, and `lastInsertId` is read from the operation result stash. With the built-in `KronosJdbcWrapper`, Kronos reads JDBC generated keys during the insert execution.

## Disable the {{ $.title("lastInsertId") }} plugin

Set `LastInsertIdPlugin.enabled` directly when identity inserts should skip generated ID collection by default.

```kotlin group="Disable" name="kotlin" icon="kotlin"
import com.kotlinorm.plugins.LastInsertIdPlugin

LastInsertIdPlugin.enabled = false
```

Use `.withId()` for one insert that still needs the generated ID.

```kotlin group="Single Insert" name="kotlin" icon="kotlin"
import com.kotlinorm.plugins.LastInsertIdPlugin.lastInsertId
import com.kotlinorm.plugins.LastInsertIdPlugin.withId

val result = User(name = "Kronos")
    .insert()
    .withId()
    .execute()

val lastInsertId = result.lastInsertId
```

## Dialect SQL used by wrapper fallback paths

Wrappers that read the generated ID with a follow-up query use the active data source dialect.

```sql group="Dialect SQL" name="Mysql" icon="mysql"
SELECT LAST_INSERT_ID()
```

```sql group="Dialect SQL" name="PostgreSQL" icon="postgres"
SELECT LASTVAL()
```

```sql group="Dialect SQL" name="SQLite" icon="sqlite"
SELECT last_insert_rowid()
```

```sql group="Dialect SQL" name="SQLServer" icon="sqlserver"
SELECT SCOPE_IDENTITY()
```

```sql group="Dialect SQL" name="Oracle" icon="oracle"
SELECT * FROM DUAL
```
