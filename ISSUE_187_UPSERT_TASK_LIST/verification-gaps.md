# Current Verification Gaps

Updated: 2026-07-12

- No code verification gaps remain for issue #187 after the recorded core, syntax, compiler-plugin, and real database integration runs.
- PR #235 is open for review.

## Must-Run Verification

- Completed: `./gradlew :kronos-core:test --tests com.kotlinorm.orm.upsert.UpsertClauseBehaviorTest --tests com.kotlinorm.orm.upsert.UpsertSubquerySqlTest --tests com.kotlinorm.orm.sql.mysql.MysqlUpsertSqlTest --tests com.kotlinorm.orm.sql.dialects.CoreOrmDialectSqlTest --tests com.kotlinorm.beans.task.TaskAndSqlExecutorBehaviorTest --tests com.kotlinorm.plugins.LastInsertIdTest --no-daemon --console=plain`
- Completed: `./gradlew :kronos-syntax:test --no-daemon --console=plain`
- Completed: `./gradlew :kronos-core:test --no-daemon --console=plain`
- Completed: `./gradlew :kronos-compiler-plugin:test --no-daemon --console=plain`
- Completed: `source envsetup.sh && ./gradlew :kronos-testing:test --no-daemon --console=plain --stacktrace`
