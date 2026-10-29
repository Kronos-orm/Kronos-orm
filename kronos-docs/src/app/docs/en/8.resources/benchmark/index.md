{% import "../../../macros/macros-en.njk" as $ %}

## Workloads

Compare Kronos, direct JDBC, Hibernate ORM (JPA), MyBatis-Plus, Jimmer, and Ktorm under the same JVM / MySQL configuration. GitHub Actions in [orm-benchmark-project](https://github.com/Kronos-orm/orm-benchmark-project) runs the experiment and updates the report automatically after validating the complete results.

Read scenarios measure entity, Map, and scalar projections across table sizes, result sizes, and access patterns. Write scenarios measure complete transactions including commit. The report links versions, environment settings, actual SQL validation, raw measurements, and source identities.

### Keeping the comparison fair

- **Shared environment**: every ORM uses the same MySQL, JDK, JDBC driver, connection pool, connection configuration, and transaction commit settings.
- **Equivalent work**: all implementations use the same fixture data, query parameters, and required results. Actual SQL, complete results, and connection/session lifetimes are checked before measurement. Writes use assigned keys and the same number of row-wise inserts per committed transaction; fixture resets happen outside timing.
- **Consistent measurement**: the six implementations in each scenario share one runner and database, run in a fixed randomized order, and warm up and measure in separate JVMs. Reads use five warmup and ten measurement iterations of two seconds each. Writes use 2000 warmup transactions and 300 measurements. Every scenario repeats across five independent blocks.

### Reading the report

- **Latency**: reads use `μs/query`; writes use `μs/transaction`. Lower values mean less elapsed time in that scenario.
- **Repeats and intervals**: measurements within a JVM are averaged first, then results from independent JVMs are combined. Figures show the repeat measurements, mean, and 95% confidence interval.
- **Scenario analysis**: start with the key-scene figures, then open the English analysis for the full matrix, paired comparisons, and environment records.

[View the complete performance report on GitHub](https://github.com/Kronos-orm/orm-benchmark-project/blob/result/readme.md)

{{ NgDocActions.demo("BenchmarkComponent", {container: false}) }}

## Related operations

- {{ $.keyword("query/select", ["Select"]) }}
- {{ $.keyword("mutation/insert", ["Insert"]) }}
- {{ $.keyword("database/dialect-support", ["Dialect Support"]) }}
- {{ $.keyword("configuration/logging", ["Logging"]) }}
