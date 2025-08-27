# 2. Directory Layout & Key Classes

Core packages (selected):
- com.kotlinorm.interfaces
  - KPojo: the core data-class contract. The compiler plugin injects bodies for its methods;
  - KActionInfo: generic DDL/DML action info (kClass/tableName/whereClause);
  - KronosNamingStrategy: naming conversion (db2k / k2db);
  - NoValueStrategy: missing-value handling (default: DefaultNoValueStrategy).
- com.kotlinorm.orm
  - select/insert/update/delete: SelectClauseInfo, InsertClauseInfo, UpdateClauseInfo, DeleteClauseInfo;
  - join: JoinClauseInfo;
  - cascade: CascadeInsertClause, NodeOfKPojo.
- com.kotlinorm.beans
  - dsl/KTableIndex: table index structure;
  - config/NamingStrategies: default naming strategies;
  - parser/NamedParameterUtils: parse named-parameter SQL and substitute args;
  - parser/NoneDataSourceWrapper: fallback when no data source configured;
  - serialize/NoneSerializeProcessor: default serialization fallback;
  - config/DefaultNoValueStrategy: default no-value strategy.
- com.kotlinorm.annotations
  - KronosInit, Necessary, TableIndex, etc.
- com.kotlinorm.plugins
  - LastInsertIdPlugin: read auto-increment id after insert.
- com.kotlinorm.enums / exceptions / i18n
  - Base enums, exception types, and literal messages.
