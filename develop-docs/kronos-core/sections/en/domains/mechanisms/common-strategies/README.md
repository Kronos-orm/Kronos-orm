# Common Strategies (create/update/logic/optimistic)

- Accessors (injected into KPojo by compiler plugin):
  - kronosCreateTime(): KronosCommonStrategy
  - kronosUpdateTime(): KronosCommonStrategy
  - kronosLogicDelete(): KronosCommonStrategy
  - kronosOptimisticLock(): KronosCommonStrategy
- Purpose:
  - Auto-fill created/updated time during INSERT/UPDATE parameter assembly;
  - Apply logical delete and optimistic lock conditions for DELETE/UPDATE.
- Collaboration:
  - Works with execution layer for SQL assembly and param construction (wrappers may differ in details);
- Customization:
  - Replace strategy implementations via global config, or have the compiler plugin emit constant strategies for specific entities.
