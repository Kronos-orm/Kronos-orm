# ORM 子句建模

本域汇总各类 ClauseInfo 的统一约定，具体字段详见 [features/*](../../features/)：
- SelectClauseInfo（[features/dsl-select](../../features/dsl-select/README.md)）
- InsertClauseInfo（[features/dsl-insert](../../features/dsl-insert/README.md)）
- UpdateClauseInfo（[features/dsl-update](../../features/dsl-update/README.md)）
- DeleteClauseInfo（[features/dsl-delete](../../features/dsl-delete/README.md)）
- JoinClauseInfo（[features/join](../../features/join/README.md)）
- CascadeInsertClause/NodeOfKPojo（[features/cascade](../../features/cascade/README.md)）

统一约定：
- ClauseInfo 仅承载数据，不直接生成最终 SQL；
- 最终 SQL 的方言拼装由执行层（数据源 wrapper + 函数 builder 等）完成；
- 命名、空值、通用策略在不同阶段生效（见 [domains/mechanisms/*](../mechanisms/)）。
