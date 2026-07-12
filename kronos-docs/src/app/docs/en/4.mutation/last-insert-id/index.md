{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Use cases

Use `.withId()` when one insert should return the database-generated identity primary key.

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
> `lastInsertId` is available when the inserted KPojo uses `@PrimaryKey(identity = true)` and the primary key value is left `null`.

## Read {{ $.title("lastInsertId") }}

Call `.withId()` on the insert that needs the generated ID.

```kotlin group="Read Id" name="kotlin" icon="kotlin"
val result = User(name = "Kronos")
    .insert()
    .withId()
    .execute()

val affectedRows = result.affectedRows
val lastInsertId = result.lastInsertId
```

`execute()` returns `KronosOperationResult`. `affectedRows` is the number of inserted rows, and `lastInsertId` is the generated identity value returned by the wrapper or the internal dialect fallback.

## When no ID is returned

Kronos does not request a generated ID unless `.withId()` is present.

```kotlin group="No Id" name="kotlin" icon="kotlin"
val result = User(name = "Kronos")
    .insert()
    .execute()

val lastInsertId = result.lastInsertId // null
```

If the primary key value is already set, the insert uses that value and `lastInsertId` stays empty.

```kotlin group="Assigned Id" name="kotlin" icon="kotlin"
val result = User(id = 1001, name = "Kronos")
    .insert()
    .withId()
    .execute()

val lastInsertId = result.lastInsertId // null
```

## Dialect fallback SQL

The built-in `KronosJdbcWrapper` first reads JDBC generated keys during insert execution. If a wrapper cannot provide generated keys directly, Kronos uses the active dialect's follow-up SQL.

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
SELECT MAX("ID") FROM "USER"
```
