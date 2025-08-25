# DDL Modeling & Table Index

- DDLInfo: `com.kotlinorm.orm.ddl.DDLInfo`
- KTableIndex / @TableIndex

Diagram:
```mermaid
flowchart LR
  A[KPojo & annotations] --> B[KTableIndex / TableIndex]
  A --> C[Field/Column meta]
  B --> D[DDLInfo]
  C --> D
  D --> E[Executor / Generator]
```

What it does:
- Describe schema & index requirements in a structured way.

Why this design:
- Core focuses on description; execution may be done by wrappers or generators.

Example:
```
val idx = KTableIndex(name = "idx_user_name", columns = arrayOf("name"), method = "btree")
```
