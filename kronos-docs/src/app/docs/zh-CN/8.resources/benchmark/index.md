{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 测量内容

在同一套 JVM / MySQL 条件下，对比 Kronos、直接 JDBC、Hibernate ORM（JPA）、MyBatis-Plus、Jimmer 和 Ktorm。测试由 [orm-benchmark-project](https://github.com/Kronos-orm/orm-benchmark-project) 的 GitHub Actions 执行，完整结果验证通过后自动更新报告。

读取测试分别测量实体、Map 和标量投影，覆盖不同表规模、返回行数及访问方式。写入测试测量包含提交的完整事务。报告提供版本、环境、实际 SQL 校验、原始数据与源码的对应记录。

### 如何保证公平

- **相同环境**：各 ORM 使用同一版本的 MySQL、JDK、JDBC 驱动和连接池，采用一致的连接配置与事务提交设置。
- **相同工作量**：使用同一套测试数据、查询参数和返回结果要求；计时前校验实际 SQL、完整结果、连接与会话生命周期。写入统一使用应用赋值主键，按相同行数逐行插入并提交事务，数据重置在计时外执行。
- **相同测量方法**：每个场景中的六种实现共享同一台 runner 和数据库；按固定随机顺序运行，各自在独立 JVM 中预热、测量。读取采用 5 次预热、10 次测量，每次 2 秒；写入采用 2000 次事务预热、300 次测量。每个场景独立重复 5 轮。

### 如何阅读

- **时延**：读取以 `μs/query` 表示，写入以 `μs/transaction` 表示，数值越低表示该场景耗时越少。
- **重复与区间**：同一 JVM 内的测量先取均值，再汇总独立 JVM 的结果；图表展示重复测量点、均值和 95% 置信区间。
- **场景分析**：先看关键场景图表，再打开中文详细分析查看完整矩阵、成对比较和环境记录。

[在 GitHub 查看完整性能报告](https://github.com/Kronos-orm/orm-benchmark-project/blob/result/readme.md)

{{ NgDocActions.demo("BenchmarkComponent", {container: false}) }}

## 相关操作

- {{ $.keyword("query/select", ["查询"]) }}
- {{ $.keyword("mutation/insert", ["插入"]) }}
- {{ $.keyword("database/dialect-support", ["方言支持"]) }}
- {{ $.keyword("configuration/logging", ["日志"]) }}
