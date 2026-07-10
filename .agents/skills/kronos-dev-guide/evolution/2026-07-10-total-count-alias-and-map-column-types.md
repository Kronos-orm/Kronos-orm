# 分页计数列命名与 Map 输出类型

## 症状

- SQL Server 执行 `SELECT COUNT(*) FROM (SELECT 1 FROM ...) AS total_count` 时提示派生表列没有名称。
- Oracle ORM 查询使用 `toMapList()` 时，`NUMBER` 列保留为 `BigDecimal`，与实体声明的 `Int` 等类型不一致。

## 根因

- total-count planner 使用了没有 alias 的常量投影。
- Map 映射将所有列按 `Any?` 读取，丢失了 ORM 最终投影字段已有的 `KType`。

## 已验证修复

- 使用 `SqlSelectItem.Expr(SqlExpr.NumberLiteral("1"), alias = "count_value")` 构造计数内层投影。
- `KAtomicQueryTask` 携带大小写兼容的输出列 `KType`，Select、Join、Union 在构建任务时填充；Map 映射按 ResultSet label 选择目标类型。
- 没有输出类型元数据的手写 SQL 继续使用 JDBC 原生值，不按数值大小猜测 `Int` 或 `Long`。

## 防回归

- 方言测试必须精确断言 SQL Server 的内层常量列 alias。
- JDBC 单测同时覆盖“有输出类型时转换”和“无输出类型时保留原值”。
- 真实数据库集成测试不能将 skipped 当作通过；本地无容器时由 CI 的 SQL Server、Oracle 服务完成验证。
