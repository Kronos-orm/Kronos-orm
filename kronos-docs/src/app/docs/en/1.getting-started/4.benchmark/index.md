{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## benchmark

`Kronos` has been rigorously benchmarked for performance and can run stably in all high concurrency scenarios with first tier performance, which is an important advantage for applications that require high performance.

Taken together, `Kronos` strikes the perfect balance between ease of use and performance, allowing developers to get up and running quickly while remaining stable in high concurrency scenarios.

We created a benchmarking project using `kotlinx-benchmark` and `JMH` (Java Microbenchmark Harness) to test the performance of `Kronos` in different scenarios as well as its performance in comparison to other `Kronos` frameworks such as `JPA`, `MyBatis`, `Jimmer`, `Ktorm`, and so on. Kotlin ORM` frameworks.

Please move to [orm-benchmark-project](https://github.com/Kronos-orm/orm-benchmark-project) to see our benchmarking framework, test cases and test methods.

Below are the latest results of our benchmarking (the benchmarking only reflects the SNAPSHOT version, not the latest release):

{{ NgDocActions.demo("BenchmarkComponent", {container: false}) }}
