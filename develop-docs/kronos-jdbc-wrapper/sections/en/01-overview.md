# 1. Module Overview

kronos-jdbc-wrapper ships a default JDBC-based implementation of KronosDataSourceWrapper: KronosBasicWrapper. It executes core tasks against a javax.sql.DataSource, handling parameter binding and result extraction.

Highlights:
- Minimal setup: wrap any DataSource instance.
- Comprehensive result modes: list of maps, single map, list of objects, single object, KPojo mapping.
- Batch update and simple transaction helper.
- Special handling for Oracle (LONG columns, scrollable result set), robust array/collection binding.
