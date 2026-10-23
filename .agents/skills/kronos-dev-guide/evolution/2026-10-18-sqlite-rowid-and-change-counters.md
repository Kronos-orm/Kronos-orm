# SQLite generated IDs and affected rows need statement-specific checks

## Symptoms

A Native SQLite adapter can return a prior insert's ID for WITHOUT ROWID, compound, INT, or inline
INTEGER PRIMARY KEY DESC tables. Reading changes() after CREATE/ALTER/PRAGMA also reports an earlier
mutation. Merely comparing total_changes() is insufficient: DROP TABLE with foreign-key cascades can
increase that counter while still being DDL.

## Fix

Keep validation, stepping, counters, and ID capture inside one connection's reentrant serial lock.

- For requested keys, parse the actual INSERT target and validate table_list's WITHOUT ROWID flag,
  table_xinfo's single INTEGER primary key, and index_list's absence of a primary-key index. The indexed
  inline DESC form differs from table-constraint PRIMARY KEY(id DESC), which remains a rowid alias.
- Match schema/table/column identifiers using SQLite ASCII case folding. Unicode ignoreCase can confuse
  distinct Ä/ä tables and validate the wrong schema. Request the physical column name from ORM metadata.
- Reject conflict-update and explicit RETURNING combined with automatic key capture. Zero affected rows
  yield no key. Capture last_insert_rowid after the outer insert completes; trigger-local IDs are restored
  by SQLite when the trigger returns.
- Consume action/batch RETURNING rows through DONE before counters. Classify real SQL (including WITH
  DML), rather than the plan's default operationType. Schema/PRAGMA actions return zero; DML with no
  total_changes increment returns zero, otherwise use changes() for the outer statement's row count.

## Verification

The example's latest 38 Native connection/codec cases passed on macosX64 with both AndroidX bundled
SQLite 2.6.2 and the sample-owned SQLite 3.54.1 C API driver, zero failures/errors/skips in each backend,
plus real desktop ID1 write/read. Source was copied into an independent
/tmp project and compiled against HEAD e6487aaa's execution-contract snapshot, so this proves the adapter
contract and does not claim current-tree ORM integration. Logs: /tmp/kronos-sqlite-contract42-androidx.log and /tmp/kronos-sqlite-contract42-capi.log.
Regressions cover stale IDs, triggers (outer1 versus audit101), schema-qualified/quoted/temp names, Ä/ä,
inline/table-constraint DESC, DDL/PRAGMA after INSERT, foreign-key DROP cascades, CTE DML, and single/batch
RETURNING. Full current-tree ORM and other-host execution remain separate acceptance work.

## Primary sources

- [SQLite rowid aliases and DESC exception](https://www.sqlite.org/lang_createtable.html#rowid)
- [Last insert rowid, WITHOUT ROWID, triggers, ignored inserts, and connection races](https://www.sqlite.org/c3ref/last_insert_rowid.html)
- [changes counter scope](https://www.sqlite.org/c3ref/changes.html)
- [total_changes includes triggers and foreign keys](https://www.sqlite.org/c3ref/total_changes.html)
- [PRAGMA index_list origin](https://www.sqlite.org/pragma.html#pragma_index_list)
