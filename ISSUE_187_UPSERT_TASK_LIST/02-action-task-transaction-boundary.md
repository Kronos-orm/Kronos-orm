# Task 2: Keep Action Task Hooks Inside One Transaction

Progress: 100%
Status: Done

## Goal

Make `KronosActionTask.execute()` run `beforeExecute`, grouped DML, and `afterExecute` inside one wrapper transaction.

## Completed

- `KronosActionTask.execute()` now runs `beforeExecute`, grouped DML, result aggregation, and `afterExecute` inside one wrapper transaction.
- Adjacent identical SQL batching still happens after `beforeExecute` expands the task list.
- Result stash propagation remains based on the last grouped task result.

## Follow-Up Work

- None for this task.

## Acceptance

- The core transaction probe test passes with every probe showing `inTransaction=true`.
- The real integration rollback test passes with no rows left after `afterExecute` throws.
- Last insert id and batch task behavior remain covered by existing targeted tests.

## Verification Record

- `./gradlew :kronos-core:test --tests com.kotlinorm.beans.task.TaskAndSqlExecutorBehaviorTest --tests com.kotlinorm.plugins.LastInsertIdTest --no-daemon --console=plain`: Covered as part of the targeted core run; pass.
- `source envsetup.sh && ./gradlew :kronos-testing:test --tests "*TransactionIntegration*" --no-daemon --console=plain --stacktrace`: Covered as part of the targeted integration run; pass.
