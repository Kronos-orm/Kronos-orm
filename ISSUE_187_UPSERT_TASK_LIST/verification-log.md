# Recent Verification

Updated: 2026-07-12

## 2026-07-12 Baseline

- Scope: Current failing behavior before implementation.
- Evidence: Existing test reports and newly added transaction guard tests.
- Result: Failing as expected.
- Follow-up: Complete Tasks 1 and 2.

## 2026-07-12 Implementation Verification

- Scope: Core upsert SQL, conflict target inference, logic-delete restore, transaction hook boundary, last insert id behavior, and syntax renderer coverage.
- Evidence: `./gradlew :kronos-core:test --tests com.kotlinorm.orm.upsert.UpsertClauseBehaviorTest --tests com.kotlinorm.orm.upsert.UpsertSubquerySqlTest --tests com.kotlinorm.orm.sql.mysql.MysqlUpsertSqlTest --tests com.kotlinorm.orm.sql.dialects.CoreOrmDialectSqlTest --tests com.kotlinorm.beans.task.TaskAndSqlExecutorBehaviorTest --tests com.kotlinorm.plugins.LastInsertIdTest --no-daemon --console=plain`.
- Result: Pass.
- Follow-up: Run real database integration tests.

## 2026-07-12 Syntax Renderer Verification

- Scope: Syntax module tests after MERGE upsert target-column rendering changes.
- Evidence: `./gradlew :kronos-syntax:test --no-daemon --console=plain`.
- Result: Pass.
- Follow-up: None.

## 2026-07-12 Broad Core Verification

- Scope: Full `kronos-core` unit test suite after the upsert and transaction changes.
- Evidence: `./gradlew :kronos-core:test --no-daemon --console=plain`.
- Result: Pass.
- Follow-up: None.

## 2026-07-12 Real Database Verification

- Scope: Full `kronos-testing` integration suite against local Docker MySQL, PostgreSQL, SQL Server, Oracle, and SQLite.
- Evidence: `source envsetup.sh && ./gradlew :kronos-testing:test --no-daemon --console=plain --stacktrace`.
- Result: Pass.
- Follow-up: Commit, push, and create PR linked to issue #187.

## 2026-07-12 Compiler Plugin Verification

- Scope: Full compiler-plugin test suite after including the existing projection-related branch changes.
- Evidence: `./gradlew :kronos-compiler-plugin:test --no-daemon --console=plain`.
- Result: Pass.
- Follow-up: None.

## 2026-07-12 PR Creation

- Scope: Push branch and open pull request for issue #187.
- Evidence: https://github.com/Kronos-orm/Kronos-orm/pull/235.
- Result: Pass.
- Follow-up: Review PR checks.
