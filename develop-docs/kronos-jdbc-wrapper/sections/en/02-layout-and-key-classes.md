# 2. Layout and Key Classes

Source root: `kronos-jdbc-wrapper/src/main/kotlin/com/kotlinorm`

- KronosBasicWrapper(DataSource)
  - Detects url/userName/dbType from JDBC metadata.
  - Query path: task.parsed() -> PreparedStatement -> executeQuery -> ResultSet -> toMapList()/toObjectList()/toKPojoList().
  - Update path: task.parsed() -> executeUpdate.
  - Batch path: task.parsedArr() -> addBatch() -> executeBatch().
  - Transaction: transact { ... } sets autoCommit=false and commits/rolls back.
- Internal helpers
  - PreparedStatement.setParameters(params): binds arrays/collections/JVM types safely.
  - Oracle-specific options and LONG column handling.
  - KPojo creation via cached fields mapping and type-safe value assignment.

Contract references (in kronos-core):
- interfaces/KronosDataSourceWrapper
- interfaces/KAtomicQueryTask / KAtomicActionTask
- beans/task/KronosAtomicBatchTask
- enums/DBType (and Oracle helper)
