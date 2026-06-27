{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Use cases

The `lastInsertId` plugin is used to retrieve the ID of the last row inserted into the database after an insert operation. This is useful when you need the unique identifier of a newly created record.

> **Note**
> This plugin only applies to tables that use auto-increment primary keys.

## Disable the {{ $.title("lastInsertId") }} plugin

By default, the `lastInsertId` plugin is enabled. You can configure it in `Kronos.init`:

```kotlin
Kronos.init {
    LastInsertId.enabled = false // Disable the lastInsertId plugin
}
```

After disabling it globally, database operations will no longer return the last inserted ID. You can still enable it temporarily by using `withId`:

```kotlin
KPojo().insert().withId().execute()
```
