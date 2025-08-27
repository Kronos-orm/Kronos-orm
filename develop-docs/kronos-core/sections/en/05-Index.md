# 5. Index & Sub-modules

- Domains:
  - flow/ — Full SQL generation & execution flow
  - mechanisms/
    - action/ — Action lifecycle
    - task-event-plugin/ — Plugin registration & hooks
    - common-strategies/ — create/update/logic/optimistic
  - orm/ — Overview for ClauseInfo (details in features/*)
  - functions/ — FunctionManager/FunctionBuilder & Transformer
  - db/ — KronosDataSourceWrapper, transactions, dialects
  - logging/ — KLogger/KLoggerFactory, default logging

- Features:
  - dsl-select/ — SelectClauseInfo
  - dsl-insert/ — InsertClauseInfo + LastInsertId
  - dsl-upsert/ — UpsertClauseInfo
  - dsl-update/ — UpdateClauseInfo
  - dsl-delete/ — DeleteClauseInfo
  - join/ — JoinClauseInfo
  - cascade/ — Cascade operations (Insert/Update/Delete)
  - parser/ — NamedParameterUtils
  - naming-strategy/ — KronosNamingStrategy
  - no-value-strategy/ — NoValueStrategy
  - plugins/last-insert-id/ — LastInsertIdPlugin
  - annotations/ — KronosInit/Necessary/TableIndex + KTableIndex
  - ddl-and-index/ — DDLInfo & index modeling
  - exceptions-and-i18n/ — Exceptions & i18n
  - interfaces/ — KPojo/KActionInfo/KronosDataSourceWrapper, etc.
