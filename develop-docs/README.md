<p align="center">
    <a href="https://www.kotlinorm.com">
        <img src="https://cdn.leinbo.com/assets/images/kronos/logo_dark.png" alt="logo" height="160" width="160">
    </a>
</p>

<h1 align="center">Kronos-ORM Developer Docs</h1>

<div align="center">
English | <a href="./README-zh_CN.md">简体中文</a>

<br/>

<a href="http://kotlinlang.org"><img src="https://img.shields.io/badge/kotlin-2.2.0-%237f52ff.svg?logo=kotlin" alt="Kotlin"></a>
<a href="https://www.apache.org/licenses/LICENSE-2.0.html"><img src="https://img.shields.io/:license-apache_2.0-green.svg" alt="License"></a>
<br/>
<a href="https://www.kotlinorm.com">Official Website</a> | <a href="https://kotlinorm.com/#/documentation/en/getting-started/quick-start">Documentation</a>
</div>

---

This page is the English entry to all developer documents under develop-docs. It explains how modules fit together, where to start, and links to detailed guides. Rich diagrams are included for a quick mental model.

## Overview
Kronos-ORM is composed of several modules working together:
- kronos-core: core contracts (KPojo, tasks, strategies), execution engine, utilities, and logging contract;
- kronos-logging: adapters to external logging systems + bundled logger; logging DSL is defined in core;
- kronos-jdbc-wrapper: production-ready JDBC implementation of KronosDataSourceWrapper;
- kronos-codegen: generate Kotlin entities and annotations from DB metadata;
- kronos-compiler-plugin: compile-time transforms and injections for DSL ergonomics.

Quick navigation:
- Getting started: [Download, Build and Run Guide](./getting-started.md)
- Core: [Architecture diagrams (EN)](./kronos-core/sections/en/04-Architecture.md) and other sections in the module
- Logging: [README](./kronos-logging/README.md), EN sections and zh_CN sections
- JDBC Wrapper: [README](./kronos-jdbc-wrapper/README.md)
- Codegen: [README](./kronos-codegen/README.md)
- Compiler Plugin: [Layout & key classes (EN)](./kronos-compiler/sections/en/02-layout-and-key-classes.md) and other sections

Highlighted chapters:
- Logging DSL & Design: [kronos-logging/sections/en/04-dsl-and-design.md](./kronos-logging/sections/en/04-dsl-and-design.md)
- JDBC usage examples: [kronos-jdbc-wrapper/sections/en/03-usage.md](./kronos-jdbc-wrapper/sections/en/03-usage.md)
- Core architecture & flow: [kronos-core/sections/en/04-Architecture.md](./kronos-core/sections/en/04-Architecture.md)

### High-level Architecture
```mermaid
flowchart TD
  subgraph Core[kronos-core]
    C1[Contracts & Strategies\nKPojo/KLogger/Tasks]
    C2[Execution Engine\nQuery/Action/Batch]
    C3[Utils\nNamedParameter/Serialize]
  end

  subgraph Logging[kronos-logging]
    L1[Adapters\nSLF4J/JUL/Commons/Android]
    L2[Detector\nKronosLoggerApp]
    L3[Bundled Logger]
  end

  subgraph JDBC[kronos-jdbc-wrapper]
    J1[KronosBasicWrapper\nJDBC bridge]
  end

  subgraph Build[kronos-codegen + compiler]
    G1[Codegen\nfrom DB -> Kotlin]
    P1[Compiler Plugin\nIR transforms]
  end

  G1 --> C1
  P1 --> C1

  C2 --> C3
  C2 --> J1
  C1 --> C2

  C1 --> L1
  L2 --> L1
  L3 --> C1
```

### End-to-End Execution (Conceptual)
```mermaid
sequenceDiagram
  participant Dev as Developer App/DSL
  participant CP as Compiler Plugin
  participant CORE as kronos-core
  participant NP as NamedParameterUtils
  participant DS as JDBC Wrapper
  participant DB as Database
  participant LOG as Logging

  Dev->>CP: Define DSL/entities
  CP-->>Dev: Inject helpers/IR transforms
  Dev->>CORE: Build ClauseInfo -> AtomicTask
  CORE->>NP: Named SQL -> JDBC SQL + args
  CORE->>DS: Execute Query/Update/Batch
  DS->>DB: prepare/execute/read
  DB-->>DS: ResultSet / counts
  DS-->>CORE: List/Map/Object/Int/IntArray
  CORE->>LOG: Structured logs (KLogMessage[])
```

### How to read these docs
- Start with your task:
  - Need logs: read logging README and the DSL & Design chapter;
  - Need DB execution: check JDBC wrapper README and usage examples;
  - Need entities from DB: go to Codegen guide;
  - Need to understand runtime: core architecture diagrams in CN section;
  - Extending or integrating a logger: see kronos-logging adapters.
