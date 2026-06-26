{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 基准测试

`Kronos`在性能上已经经过了严格的基准测试，可以在所有高并发场景下稳定运行，并且性能表现处于第一梯队，这对于需要高性能的应用程序来说是一个重要的优势。

综合来说，`Kronos`实现了易用性和性能的完美平衡，既能让开发者快速上手，又能在高并发场景下稳定运行。

我们使用了`kotlinx-benchmark`和`JMH`（Java Microbenchmark Harness）创建了基准测试项目，测试了`Kronos`在不同场景下的性能表现以及与`JPA`、`MyBatis`、`Jimmer`、`Ktorm`等其他`Kotlin ORM`框架的性能对比。

请移步到[orm-benchmark-project](https://github.com/Kronos-orm/orm-benchmark-project)查看我们的基准测试框架、测试用例及测试方法。

以下是我们基准测试的最新结果（基准测试只反应SNAPSHOT版本，并非最新发布版本）：

{{ NgDocActions.demo("BenchmarkComponent", {container: false}) }}
