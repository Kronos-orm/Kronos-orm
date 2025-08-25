# 9. Relationship with Other Modules (Expanded)

- kronos-core
  - Provides DSL types (Field, KTableIndex), annotations (@Table, @PrimaryKey, etc.), and enums (KColumnType, PrimaryKeyType).
  - Codegen must match these contracts to produce compilable outputs.
- kronos-jdbc-wrapper
  - Defines `KronosDataSourceWrapper` and default `KronosBasicWrapper`.
  - Codegen uses wrapper to query metadata through `SqlManager` and DDL helpers.
- kronos-gradle-plugin / kronos-maven-plugin
  - Build-time integration layers can call `init` + `template` + `write` under the hood.
  - This module stays independent so it can also be used from tests or standalone apps.
