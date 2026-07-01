# Evolution Index

Use this index before opening `Evolution.md`. Match by module, symptom, command, or keyword, then read only the matching entry from `Evolution.md` with a targeted search such as:

```powershell
Select-String -Path .agents/skills/kronos-dev-guide/Evolution.md -Pattern "ENTRY TITLE OR UNIQUE ERROR" -Context 0,18
```

If no entry matches, do not open the full evolution log; continue normal investigation through references, code search, and focused tests. After adding a new `Evolution.md` entry, add one concise row here.

| Area | Keywords / Symptoms | Evolution Entry |
|------|---------------------|-----------------|
| Kotlin collection literals | `[]`, `-Xcollection-literals`, compile-testing, dynamic compile | `2026-06-27 - Collection literal tests require the explicit compiler flag` |
| Projection syntax migration | old `select { it.a + it.b }`, arithmetic misread as projection | `2026-06-27 - Field projection plus must not be reused for arithmetic` |
| Typed select API | `select<Source, Projection>`, receiver lost, overload ambiguity | `2026-06-27 - Typed select projection must preserve source DTO as lambda receiver` |
| IR generation | `Duplicate IR node`, generated invalid IR, reused `IrExpression` | `2026-06-27 - Generated IR nodes must not be reused across parents` |
| Set transformer | `setValue`, `setAssign`, RHS clone, duplicate IR node | `2026-06-27 - Set transformer must clone RHS expressions when adding setValue/setAssign calls` |
| Cascade projection | cascade-only select, source table metadata, local keys, recursion | `2026-06-30 - Cascade projection rows must keep source table metadata and local keys` |
| Reverse cascade | parent side lacks `@Cascade`, hidden local key missing | `2026-06-30 - Reverse cascade annotations must also provide projection local keys` |
| Test KPojo discovery | private nested KPojo, `IllegalAccessError`, `NoClassDefFoundError`, `KronosTestBase.ensureInitialized` | `2026-06-30 - Test-local private KPojo can poison global KClassCreator discovery` |
| Incremental test compilation | stale `kClassCreator`, `KClass ... instantiation failed`, missing new test KPojo | `2026-06-30 - Shared Kronos.init can hold a stale kClassCreator snapshot under incremental test compilation` |
| Delete EXISTS | `DELETE` drops `EXISTS`, fieldless SQL criteria | `2026-06-30 - DeleteClause must preserve fieldless SQL criteria such as EXISTS` |
| Insert select params | `INSERT SELECT`, parameter becomes null, source query params missing | `2026-06-30 - INSERT SELECT must share source query parameter values` |
| Upsert expression params | `upsert().patch`, expression assignment overwrites insert param, subquery param missing | `2026-06-30 - UPSERT expression assignments must not overwrite insert parameters` |
| CTAS params | `CREATE TABLE AS SELECT`, source query params missing | `2026-06-30 - CTAS must share source query parameter values` |
| Tuple IN handoff | `[field1, field2] in query`, `List<Field>`, row-value lower in core | `2026-06-30 - Tuple IN compiler handoff should lower List<Field> in core` |
| Delete tuple IN | `DELETE` drops tuple IN, fieldless structured criteria | `2026-06-30 - DeleteClause must preserve all fieldless structured subquery criteria` |
| Structured IN NOT | `CriteriaSubqueryValue.In.not`, `Criteria.not`, `!in` becomes `IN`, NOT flags | `2026-06-30 - CriteriaSubqueryValue.In NOT flags must be merged with OR, not XOR` |
| Syntax AST type declarations | `SqlType.Int`, `Argument type mismatch`, `kotlin.Int`, nested type name shadowing | `2026-07-02 - SqlType.Int shadows Kotlin Int inside nested type declarations` |
| Syntax dialect routing | `SqlDialect.PostgreSql`, `SqlDialect.SQLite`, data class equality, wrong renderer, `INSERT OR REPLACE`, `ON CONFLICT` | `2026-07-02 - SqlDialect data equality must not decide renderer family` |
