{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Enable DataGuard

`DataGuardPlugin` checks Kronos write and table operations before execution. Enable it during application startup when the application should reject full-table write operations and destructive table operations by default.

```kotlin group="Enable 1" name="kotlin" icon="kotlin"
import com.kotlinorm.plugins.DataGuardPlugin

DataGuardPlugin.enable()
```

Disable it when the same process needs to run without DataGuard checks.

```kotlin group="Enable 2" name="disable" icon="kotlin"
DataGuardPlugin.disable()
```

> **Note**
> DataGuard reads table and condition information from Kronos DSL and table operation APIs. Guard raw SQL at the boundary where your application creates and executes that SQL.

## Block full-table delete

With the default policy, a delete without `by` or `where` is rejected.

```kotlin group="DeleteAll 1 1" name="kotlin" icon="kotlin"
DataGuardPlugin.enable()

User()
    .delete()
    .execute()
```

The operation stops before it reaches the data source.

```text group="DeleteAll 1 2" name="result"
UnsupportedOperationException: Delete operation is not allowed.
```

Add an explicit condition for a delete that targets known rows.

```kotlin group="DeleteAll 1 3" name="with where" icon="kotlin"
User()
    .delete()
    .where { it.id == 1 }
    .execute()
```

## Block full-table update

With the default policy, an update that has no generated `WHERE` clause is rejected.

```kotlin group="UpdateAll 1" name="kotlin" icon="kotlin"
DataGuardPlugin.enable()

User()
    .update()
    .set { it.status = "LOCKED" }
    .execute()
```

The operation stops with the update guard message.

```text group="UpdateAll 2" name="result"
UnsupportedOperationException: Update operation is not allowed.
```

Allow a planned full-table update for one table by adding an `updateAll` allow rule.

```kotlin group="UpdateAll 3" name="allow table" icon="kotlin"
DataGuardPlugin.enable {
    updateAll {
        allow {
            tableName = "user_archive"
        }
    }
}

UserArchive()
    .update()
    .set { it.status = "EXPIRED" }
    .execute()
```

## Allow a maintenance delete

Use `deleteAll` when a planned maintenance job intentionally deletes every row from a known table.

```kotlin group="DeleteAll 2 1" name="allow table" icon="kotlin"
DataGuardPlugin.enable {
    deleteAll {
        allow {
            tableName = "tmp_import_error"
        }
    }
}

TmpImportError()
    .delete()
    .execute()
```

The allow rule applies only to `tmp_import_error`. Other full-table deletes are still rejected.

```text group="DeleteAll 2 2" name="other table"
UnsupportedOperationException: Delete operation is not allowed.
```

## Allow temporary table DDL

`truncate`, `drop`, and `alter` rules protect table operations. The `%` wildcard matches any sequence of characters in `databaseName` or `tableName`.

```kotlin group="DDL 1" name="tmp tables" icon="kotlin"
import com.kotlinorm.Kronos

DataGuardPlugin.enable {
    truncate {
        allow {
            tableName = "tmp_%"
        }
    }
    drop {
        allow {
            tableName = "tmp_%"
        }
    }
    alter {
        allow {
            tableName = "tmp_%"
        }
    }
}

val wrapper = Kronos.dataSource()

wrapper.table.truncateTable("tmp_session")
wrapper.table.dropTable("tmp_session")
wrapper.table.syncTable(TmpSession())
```

A table outside the temporary-table rule is rejected.

```text group="DDL 2" name="result"
UnsupportedOperationException: Drop operation is not allowed.
```

## Match one database

Set `databaseName` when the rule should apply only to the current database reported by the active data source.

```kotlin group="Database 1" name="database rule" icon="kotlin"
DataGuardPlugin.enable {
    deleteAll {
        allow {
            databaseName = "kronos"
            tableName = "tmp_%"
        }
    }
}
```

When the same table name is used from another database, the rule does not match.

```text group="Database 2" name="result"
UnsupportedOperationException: Delete operation is not allowed.
```

## Deny sensitive tables

Use `allowAll()` to allow an operation by default, then add `deny { ... }` rules for sensitive tables.

```kotlin group="Deny 1" name="drop" icon="kotlin"
import com.kotlinorm.Kronos

DataGuardPlugin.enable {
    drop {
        allowAll()
        deny {
            tableName = "sensitive_%"
        }
    }
}

val wrapper = Kronos.dataSource()

wrapper.table.dropTable("archive_2026")
wrapper.table.dropTable("sensitive_user")
```

The first drop is allowed. The second drop is rejected by the deny rule.

```text group="Deny 2" name="result"
UnsupportedOperationException: Drop operation is not allowed.
```

For explicit delete and update conditions, see {{ $.keyword("mutation/delete", ["Delete Records"]) }} and {{ $.keyword("mutation/update", ["Update Records"]) }}.
