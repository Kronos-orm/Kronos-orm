# 通用策略（create/update/logic/optimistic）

- 访问入口（由编译期插件注入到 KPojo）：
  - kronosCreateTime(): KronosCommonStrategy
  - kronosUpdateTime(): KronosCommonStrategy
  - kronosLogicDelete(): KronosCommonStrategy
  - kronosOptimisticLock(): KronosCommonStrategy
- 用途：
  - 在 INSERT/UPDATE 组装参数时，自动补齐创建/修改时间；
  - 在 DELETE/UPDATE 时应用逻辑删除与乐观锁条件；
- 协作：
  - 与执行层的 SQL 生成与参数构造配合生效（不同 wrapper 可有不同实现细节）；
- 定制：
  - 可通过全局配置替换策略实现，或由编译器插件为特定实体生成常量策略。
