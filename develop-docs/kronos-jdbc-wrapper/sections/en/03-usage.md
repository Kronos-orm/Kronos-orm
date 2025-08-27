# 3. Usage

1) Wrap a DataSource

```kotlin
val ds: javax.sql.DataSource = /* create via your pool */
val wrapper = KronosBasicWrapper(ds)
```

2) Execute with Kronos DSL or tasks (conceptually)

```kotlin
// SelectClause(...).queryList(wrapper)
// InsertClause(...).execute(wrapper)
// UpsertClause(...).execute(wrapper)
```

3) Transaction helper

```kotlin
val result = wrapper.transact {
    // execute multiple actions using the same connection, auto-commit disabled
    // return any value if needed
}
```

Notes
- NamedParameterUtils (from core) converts named SQL to JDBC SQL + args before wrapper execution.
- For arrays/collections in parameters, ensure the DB/driver supports JDBC Array for your types.
