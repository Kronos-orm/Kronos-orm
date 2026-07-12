# Task 1: Restore Upsert Conflict Target And Strategy Contracts

Progress: 100%
Status: Done

## Goal

Make native and match-field upsert generate legal conflict SQL and preserve strategy fields, including logic-delete restoration for issue #187.

## Completed

- `onConflict()` infers conflict fields from explicit `onFields`, valued primary/global primary key fields, or value-complete unique table indexes.
- Native conflict upsert initializes create time, update time, logic-delete, and optimistic version insert values.
- Native conflict update restores logic-delete fields, advances update time, and increments version with an AST expression.
- Match-field upsert restores logic-delete fields through `UpdatePlanner` without generating `deletedNewNew`.
- MERGE upsert renders target-row column references with the target alias so SQL Server and Oracle do not see ambiguous or invalid `version` references.

## Follow-Up Work

- None for this task.

## Acceptance

- Existing `UpsertClauseBehaviorTest`, `UpsertSubquerySqlTest`, `MysqlUpsertSqlTest`, and `CoreOrmDialectSqlTest` pass with complete SQL/parameter matches.
- Native conflict SQL never contains empty conflict targets.
- Issue #187 behavior is covered by a match-field upsert test that restores a logically deleted row.

## Verification Record

- `./gradlew :kronos-core:test --tests com.kotlinorm.orm.upsert.UpsertClauseBehaviorTest --tests com.kotlinorm.orm.upsert.UpsertSubquerySqlTest --tests com.kotlinorm.orm.sql.mysql.MysqlUpsertSqlTest --tests com.kotlinorm.orm.sql.dialects.CoreOrmDialectSqlTest --tests com.kotlinorm.beans.task.TaskAndSqlExecutorBehaviorTest --tests com.kotlinorm.plugins.LastInsertIdTest --no-daemon --console=plain`: Pass.
- `source envsetup.sh && ./gradlew :kronos-testing:test --tests "*UpsertIntegration*" --tests "*StrategyIntegration*" --tests "*TransactionIntegration*" --no-daemon --console=plain --stacktrace`: Pass against local Docker MySQL, PostgreSQL, SQL Server, Oracle, and SQLite.
- `./gradlew :kronos-core:test --no-daemon --console=plain`: Pass.
- `source envsetup.sh && ./gradlew :kronos-testing:test --no-daemon --console=plain --stacktrace`: Pass.
