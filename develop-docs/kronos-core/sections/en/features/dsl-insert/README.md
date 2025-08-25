# Insert Clause

- Core model: `com.kotlinorm.orm.insert.InsertClauseInfo` (implements KActionInfo)

Diagram:
```mermaid
flowchart LR
  A[DSL/Entity] --> B[InsertClauseInfo]
  B --> C[ActionTask]
  C --> D[NamedParameterUtils]
  D --> E[JDBC SQL + args]
  E --> F[DataSource.update]
  F --> G[afterAction Plugins]
  G --> H[lastInsertId?]
```

What it does:
- Describes target table and entity type (kClass), lets execution build columns/values.
- Works with TaskEventPlugin for post-insert actions (e.g., lastInsertId).

Why this design:
- Keep the model as pure data; post actions via plugins (decoupled).
- Support multiple DBs (identity/sequence) via plugins/wrappers.

Example (Patch-based):
```kotlin
val (sql, paramMap) = User(1).insert().build()
// Or: User(1).insert().withId() // require plugin & wrapper to actually query lastInsertId
```
