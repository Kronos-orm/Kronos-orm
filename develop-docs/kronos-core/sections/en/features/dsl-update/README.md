# Update Clause

- Core model: `com.kotlinorm.orm.update.UpdateClauseInfo` (implements KActionInfo)

Diagram:
```mermaid
flowchart LR
  A[DSL/Update] --> B[UpdateClauseInfo]
  B --> C[ActionTask]
  C --> D[NamedParameterUtils]
  D --> E[JDBC SQL + args]
  E --> F[DataSource.update]
```

What it does:
- Express target table, where clause, and source entity to build SET and args.
- Cooperates with common strategies (updateTime/logicDelete/optimisticLock).

Why this design:
- Keep complex SET/conditions in execution + strategies; model carries context.

Example (Patch-based):
```kotlin
val (sql, paramMap) = user.update { it.username + it.gender }
  .by { it.id }
  .build()
```
