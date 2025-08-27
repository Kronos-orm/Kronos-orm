# 3. Relationship to other modules

- With kronos-compiler-plugin
  - Plugin depends on core interfaces (KPojo/KActionInfo), annotations (KronosInit/TableIndex), and DSL structures (Field/KTableIndex);
  - Plugin injects method bodies for KPojo at compile time (e.g., toDataMap, kronos* strategy accessors).
- With kronos-jdbc-wrapper
  - Executes SQL based on core models (ClauseInfo/Task);
  - Uses NoneDataSourceWrapper to throw NoDataSourceException when DS is missing;
  - Compatible with TaskEventPlugin (e.g., LastInsertIdPlugin).
- With kronos-codegen
  - Reads naming strategy (KronosNamingStrategy) and index definitions to generate boilerplate;
  - Shares KTableIndex structure.
- With build plugins (gradle/maven)
  - Align versions and pass through required dependencies.
