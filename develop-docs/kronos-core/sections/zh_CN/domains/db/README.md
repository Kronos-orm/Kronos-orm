# 数据库与事务

- 抽象接口：KronosDataSourceWrapper（com.kotlinorm.interfaces）
  - url/userName/dbType
  - forList/forMap/forObject
  - update/batchUpdate
  - transact(block)
- 默认兜底：NoneDataSourceWrapper（com.kotlinorm.beans.parser）
  - 未配置数据源时抛出 NoDataSourceException，配合 i18n 提示
- 事务：
  - ActionTask.execute() 内部通过 wrapper.transact 聚合多个原子任务；
  - 相同 SQL 的原子任务会被批处理合并为 KronosAtomicBatchTask；
- 方言：
  - dbType（com.kotlinorm.enums.DBType）用以在函数/插件等环节分流行为；
- 典型实现：
  - 由 kronos-jdbc-wrapper 等模块提供；core 只定义契约。
