# 非目标

更新日期：2026-07-22

- SQL 量化 `ANY`、`SOME`、`ALL` 的子查询语义。
- PostgreSQL 数组函数，例如 `f.any(...)`。
- Kotlin 无参 `any()`、`all`、`none`、`Sequence.any`、对象数组重载和基本类型数组重载。
- 嵌套集合量词，以及谓词内动态生成的任意 Kotlin 业务布尔逻辑。
- 使用 raw SQL、SQL 字符串构造、`asSql` 或 `patch` 作为实现方式。
- 修改通用 `andExpr` / `orExpr` 的空子节点行为。
- 查询优化器、数据库方言渲染或新增 syntax AST 节点类型。
