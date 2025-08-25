# Database & Transactions

- Abstract interface: KronosDataSourceWrapper (com.kotlinorm.interfaces)
  - url/userName/dbType
  - forList/forMap/forObject
  - update/batchUpdate
  - transact(block)
- Default fallback: NoneDataSourceWrapper (com.kotlinorm.beans.parser)
  - Throws NoDataSourceException when DS is not configured, with i18n messages
- Transactions:
  - ActionTask.execute() uses wrapper.transact to group multiple atomic tasks;
  - Equal SQL atomic tasks are merged into KronosAtomicBatchTask for batching;
- Dialect:
  - dbType (com.kotlinorm.enums.DBType) is used across functions/plugins to branch behavior;
- Typical implementation:
  - Provided by modules like kronos-jdbc-wrapper; core only defines contracts.
