# 6. Frequently Asked Questions (FAQ)

- Where are KPojo method bodies implemented?
  - They are injected at compile time by kronos-compiler-plugin; at runtime they are simply called.
- What happens if no data source is configured?
  - When using NoneDataSourceWrapper, a NoDataSourceException will be thrown with i18n message text.
- Why is my lastInsertId null?
  - Ensure LastInsertIdPlugin.enabled is true, or call withId() on InsertClause explicitly;
  - It only applies when useIdentity == true (auto-increment PK);
  - Some databases require specific dialect/transaction scope; see the plugin implementation and wrapper behavior.
- Does the naming strategy affect result mapping?
  - Yes. db2k is used to convert column names back to Kotlin property names; keep the strategy consistent with DDL/codegen.
- How does the null/missing-value strategy take effect?
  - The execution layer applies NoValueStrategy during parameter assembly; inject a global strategy to customize.
