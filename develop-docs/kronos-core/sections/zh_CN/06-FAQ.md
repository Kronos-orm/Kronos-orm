# 6. 常见问题（FAQ）

- KPojo 的方法体在哪里实现？
  - 由 kronos-compiler-plugin 在编译期注入，运行时直接调用。
- 没有配置数据源时会怎样？
  - 使用 NoneDataSourceWrapper 时会抛出 NoDataSourceException，并携带 i18n 文案。
- 为什么我的 lastInsertId 为空？
  - 确认 LastInsertIdPlugin.enabled 已开启，或 InsertClause 显式调用 withId()；
  - 并且仅在 useIdentity==true（自增主键）时生效；
  - 某些数据库需要特定方言/事务范围，参考插件实现与 wrapper 行为。
- 命名策略对查询结果映射有影响吗？
  - 是。db2k 用于将列名转换回 Kotlin 属性名；请保证策略与建表/生成器一致。
- Null 值策略如何生效？
  - 执行层在组装参数时调用 NoValueStrategy；如需自定义，注入全局即可。
