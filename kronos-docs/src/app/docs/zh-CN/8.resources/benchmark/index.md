{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 阅读基准测试报告

本页嵌入 [`orm-benchmark-project`](https://github.com/Kronos-orm/orm-benchmark-project) 生成的报告。该报告使用 `kotlinx-benchmark` 和 `JMH` 记录 `Kronos`、`JPA`、`MyBatis`、`Jimmer`、`Ktorm` 的测量结果。

请将这些数字视为该 benchmark 项目的输出，不作为当前发布版本保证，也不代表所有生产负载。阅读时应同时参考查询、变更和数据库页面中的 API 行为说明。

做应用决策时，需要结合自己的业务负载：查询复杂度、写入量、数据库方言、连接池、事务边界和 SQL 日志设置都会影响运行表现。

相关文档：

- {{ $.keyword("query/select", ["查询"]) }}
- {{ $.keyword("mutation/insert", ["插入"]) }}
- {{ $.keyword("database/dialect-support", ["方言支持"]) }}
- {{ $.keyword("configuration/logging", ["日志"]) }}

下方内容从 benchmark 项目的 `result` 分支加载：

{{ NgDocActions.demo("BenchmarkComponent", {container: false}) }}
