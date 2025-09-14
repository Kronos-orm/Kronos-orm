<p align="center">
    <a href="https://www.kotlinorm.com">
        <img src="https://cdn.leinbo.com/assets/images/kronos/logo_dark.png" alt="logo" height="160" width="160">
    </a>
</p>

<h1 align="center">Kronos-ORM 开发者文档</h1>

<div align="center">
<a href="./README.md">English</a> | 简体中文

<br/>

<a href="http://kotlinlang.org"><img src="https://img.shields.io/badge/kotlin-2.2.0-%237f52ff.svg?logo=kotlin" alt="Kotlin"></a>
<a href="https://www.apache.org/licenses/LICENSE-2.0.html"><img src="https://img.shields.io/:license-apache_2.0-green.svg" alt="License"></a>
<br/>
<a href="https://www.kotlinorm.com">官网</a> | <a href="https://kotlinorm.com/#/documentation/zh-CN/getting-started/quick-start">中文文档</a>
</div>

---

本页为 develop-docs 的中文入口，帮助你建立整体心智模型并快速跳转到各模块的详细文档，同时附带多张示意图。

## 模块概览
- kronos-core：核心部分（注解、KPojo、任务、策略）、执行引擎、工具与内置日志工具；
- kronos-logging：日志适配器与自动探测，内置简单 Logger；日志 DSL 定义在 core；
- kronos-jdbc-wrapper：JDBC 生产可用的数据源包装实现；
- kronos-codegen：从数据库元数据生成 Kotlin 实体与注解；
- kronos-compiler-plugin：编译期增强与注入，实现 DSL 解析。

快速导航：
- 入门：[下载、构建与运行指南](getting-started-zh_CN.md)
- Core：[架构图](./kronos-core/sections/zh_CN/04-架构图.md)
- Logging：[README](./kronos-logging/README.md)
- JDBC Wrapper：[README](./kronos-jdbc-wrapper/README.md)
- Codegen：[README](./kronos-codegen/README.md)
- Compiler Plugin：[目录结构与关键类](./kronos-compiler/sections/zh_CN/02-目录结构与关键类.md)

重点章节直达：
- 日志 DSL 与设计：[kronos-logging/sections/zh-CN/04-dsl-and-design.md](./kronos-logging/sections/en/04-dsl-and-design.md)
- JDBC 用法示例：[kronos-jdbc-wrapper/sections/zh-CN/03-usage.md](./kronos-jdbc-wrapper/sections/en/03-usage.md)
- 核心架构与流程图：[kronos-core/sections/zh_CN/04-架构图.md](./kronos-core/sections/zh_CN/04-架构图.md)

### 顶层关系图
```mermaid
flowchart TD
  subgraph 核心[kronos-core]
    A1[契约/策略\nKPojo/KLogger/Task]
    A2[执行引擎\n查询/变更/批量]
    A3[工具\n命名参数/序列化]
  end

  subgraph 日志[kronos-logging]
    B1[适配器\nSLF4J/JUL/Commons/Android]
    B2[探测器\nKronosLoggerApp]
    B3[内置 Logger]
  end

  subgraph JDBC[kronos-jdbc-wrapper]
    C1[JDBC 包装\nKronosBasicWrapper]
  end

  subgraph 构建[kronos-codegen + compiler]
    D1[代码生成]
    D2[编译期插件]
  end

  D1 --> A1
  D2 --> A1

  A2 --> A3
  A2 --> C1
  A1 --> A2

  A1 --> B1
  B2 --> B1
  B3 --> A1
```

### 端到端流程（示意）
```mermaid
sequenceDiagram
  participant 开发 as 业务代码/DSL
  participant 插件 as 编译期插件
  participant 核心 as kronos-core
  participant 命名 as NamedParameterUtils
  participant 包装 as JDBC 包装
  participant 数据库 as DB
  participant 日志 as logging

  开发->>插件: 定义实体/DSL
  插件-->>开发: 注入/改写 IR
  开发->>核心: 组装 ClauseInfo -> AtomicTask
  核心->>命名: 命名参数 SQL -> JDBC SQL + args
  核心->>包装: 执行 查询/更新/批量
  包装->>数据库: prepare/execute/read
  数据库-->>包装: ResultSet / 更新计数
  包装-->>核心: List/Map/Object/Int/IntArray
  核心->>日志: 输出结构化日志（KLogMessage[]）
```

### 阅读建议
- 按需查阅：
  - 想接入日志：阅读 logging README 与 DSL 设计章节；
  - 想执行数据库：阅读 JDBC wrapper README 与用法；
  - 想从库生成实体：阅读 Codegen 指南；
  - 想理解运行机制：查看 core 中文架构与时序图；
  - 想扩展日志后端：参考 kronos-logging 适配器实现。
