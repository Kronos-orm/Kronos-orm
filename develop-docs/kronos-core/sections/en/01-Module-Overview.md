# 1. Module Overview

kronos-core is the runtime core of Kronos ORM, providing:
- Unified contracts: KPojo, KActionInfo, KronosNamingStrategy, NoValueStrategy;
- ORM clause models: Select/Insert/Update/Delete, Join, and helpers like group/order/having fields;
- Runtime extension points: TaskEventPlugin (e.g., LastInsertId), naming strategy, no-value strategy;
- Utilities: Named parameter SQL parsing, DataSource wrapper and a None implementation, exceptions & i18n;
- DDL and table index descriptors (KTableIndex / @TableIndex).

Goal: let DSL, compiler plugin, and JDBC/Reactive wrappers work together under one contract.
