{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

The main use case of `DataGuard` is protecting dangerous operations from accidental data loss or incorrect mass updates. It checks database operations before execution and blocks unsafe actions according to your configuration.

Supported scenarios include:

- **Alter operations**: prevent accidental table structure changes and possible data loss.
- **Drop operations**: prevent tables from being deleted.
- **Truncate operations**: prevent tables from being emptied.
- **DeleteAll operations**: prevent full-table deletes.
- **UpdateAll operations**: prevent full-table updates.

The `DataGuard` plugin is disabled by default and must be enabled manually.

## Usage

Configure it in `Kronos.init`:

```kotlin
DataGuardPlugin.enable {
    ... // Configure DataGuard scenarios here
} // Enable DataGuard

DataGuardPlugin.disable() // Disable DataGuard
```

## Blacklist mode

In blacklist mode, `DataGuard` forbids configured operations. You can specify which operations should be denied.

```kotlin
DataGuardPlugin.enable {
    alter {
        allowAll()
        deny {
            tableName = "sensitive_%" // Forbid Alter on sensitive tables
            // Use % as a wildcard to match multiple table names
        }
        deny {
            databaseName = "sensitive_db"
            tableName = "sensitive_table"
        }
    }
}
```

## Whitelist mode

In whitelist mode, `DataGuard` only allows configured operations. You can specify which operations are permitted.

```kotlin
DataGuardPlugin.enable {
    updateAll {
        denyAll()
        allow {
            tableName = "allowed_table"
        }
    }
    deleteAll {
        denyAll()
        allow {
            tableName = "allowed_table"
        }
    }
    truncate {
        denyAll()
        allow {
            tableName = "allowed_table"
        }
    }
    drop {
        denyAll()
        allow {
            tableName = "allowed_table"
        }
    }
}
```

In practice, `DataGuard` uses whitelist behavior by default. If you write the following form, all matching operations are denied unless explicitly allowed, so `denyAll` is not required:

```kotlin
DataGuardPlugin.enable {
    drop {
        allow { tableName = "tmp_%" } // Only allow dropping tables whose names start with tmp_
    }
}
```

{{ $.hr() }}

Configure `DataGuard` according to your production safety requirements. With a reasonable policy, it can effectively prevent accidental data loss and incorrect mass modifications.
