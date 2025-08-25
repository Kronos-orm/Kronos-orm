# 插件：LastInsertIdPlugin

- 位置：`com.kotlinorm.plugins.LastInsertIdPlugin`

简图：
```mermaid
flowchart LR
  A[INSERT 执行完成] --> B[afterAction 钩子]
  B --> C[按 DBType 构造 SQL]
  C --> D[查询 lastInsertId]
  D --> E[写入 OperationResult.stash]
```

主要功能：
- 注册为 TaskEventPlugin，监听 INSERT 结束事件；
- 根据 DBType 执行 lastInsertId 查询；
- 结果存入 KronosOperationResult.stash["lastInsertId"]。

为什么这样设计：
- 将“主键回查”与核心执行解耦，按需启用；
- 便于扩展不同数据库的获取语句与事务范围要求。

使用：
- 通过 `LastInsertIdPlugin.enabled = true` 开启；
- 或在具体 InsertClause 上调用 `withId()` 临时开启（配合 Patch：`entity.insert().withId()`）；
- 仅在 `stash["useIdentity"] == true` 时生效。

数据库兼容：
- MySQL/H2/OceanBase: SELECT LAST_INSERT_ID()
- Oracle: 需结合序列（示例：SELECT my_seq.CURRVAL FROM dual）
- MSSQL: SELECT SCOPE_IDENTITY()
- Postgres: SELECT LASTVAL()
- DB2: SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1
- Sybase: SELECT @@IDENTITY
- SQLite: SELECT last_insert_rowid()
