# Recent Verification

Updated: 2026-07-20

## 2026-07-20 Non-root JOIN Cascade Materialization Regression

- Scope: close the remaining non-root JOIN cascade gap in the projection model, IR materialization, and verification matrix.
- Evidence: `ProjectionBoxTest.joinNonRootCascadeProjection` passed. Real SQL emits `profile_id AS profileId`; the
  `profile` relation is not emitted as a physical column, the mapped Selected row retains hidden `profileId`, and the cascade
  query runs once to backfill `profile`.
- Result: pass. Task 3, Task 4, and Task 5 are now 100% / Done; this regression closes the cascade metadata and hidden-local-key gap.
- Follow-up: none for Tasks 3-5. IDEA 2026.2 artifact signing, Verifier, Marketplace, and installation remain in Task 10.

## 2026-07-20 Task 9 Formal IDEA 2026.2 Gate

- Scope: verify the real IDEA fixture and complete IDEA plugin test suite against the installed stable IU-262.8665.258 platform after the test-runtime wiring and Analysis API thread/read-action fixes.
- Evidence: the focused duplicate-JOIN projection fixture passed, and the complete `kronos-idea-plugin:test` gate recorded 32/32 tests passing with no failures, errors, or skips.
- Result: pass. Task 9 advances to 100% and is Done; the fixture proves the real compiler-plugin diagnostic path under formal IDEA 2026.2.
- Follow-up: Task 10 still requires signed ZIP generation, nested descriptor inspection, Plugin Verifier, guarded publish, Marketplace approval/metadata, and installation in IDEA 2026.2.

## 2026-07-20 Task 3-5 Final Compiler Regression Gate

- Scope: verify the intentional `StringFunctions.length -> Int?` projection contract after the stale captured-condition fixture was corrected, then rerun the complete compiler-plugin gate on the final Task 2/3/4 worktree.
- Evidence: the first complete run exposed one `ConditionBoxTest.capturedNonKPojoValues` type error because the fixture compared a `String?` property with the newly accurate `Int?` function result. After preserving the production `Int?` contract and aligning only that fixture, focused `ConditionBoxTest` passed 38/38. The subsequent `./gradlew :kronos-compiler-plugin:test --no-daemon --max-workers=1 --console=plain` run completed with all 281 tests passing; XML reports zero failures, errors, or skips, including projection diagnostics 33/33, projection box 22/22, DSL integration 23/23, and condition box 38/38.
- Result: pass. The final compiler-plugin rerun gap is closed. Task 3 advances from 80% to 90%, Task 4 from 80% to 85%, and Task 5 from 80% to 90%; each remains In Progress because projection-form parity, broader runtime-consumer proof, coverage, and remaining non-Codacy gates are still open.
- Follow-up: finish the explicit projection-form/runtime-consumer dispositions, then run coverage and the remaining non-Codacy workflow equivalents. IDEA and Marketplace proof remain isolated in Tasks 9 and 10.

## 2026-07-20 Task 6 Canonical Documentation Sync

- Scope: synchronize bilingual projection, JOIN, pagination, result-method, select, subquery, condition, dialect, multi-tenant, IDEA, and release-note pages plus both READMEs and the ORM guide; do not change IDEA production or run a build.
- Evidence: source comparison against `UnsafeProjectionOverride`, `JoinSource`/`JoinedSelectQuery`, `OffsetPageQuery`/`TotalPageQuery`/`CursorPageQuery`, `PageResult`/`CursorResult`, and the green Task 7/8 fixtures. Repository scans found no legacy `withCursor`, cursor `offset`, operand-argument relation call, standalone JOIN `on { ... }`, `Triple` pagination result, or superseded alias/duplicate-property guidance in source-maintained docs, READMEs, or the ORM skill. The multiline old-order scan matched only separate examples in the bilingual result-method pages; lines 207-218 were inspected and each total-count example uses `page(...).withTotal()`. `git diff --check` passed on the final worktree.
- Result: pass. Bilingual docs, READMEs, release notes, and the maintainer skill match the implemented projection, JOIN, pagination, and IDEA contracts. Task 6 advances from 85% to 100% and is Done. No Gradle or docs build was run per user instruction.
- Follow-up: none for Task 6.

## 2026-07-20 Task 5 Static Verification-Matrix Audit

- Scope: reconcile compiler/runtime registration and recorded gate evidence without running Gradle or changing production, test, documentation-site, or IDEA files.
- Evidence: static comparison found 33 projection diagnostic testData files and 33 registered runner methods, with no missing or stale registrations across projection diagnostics, projection box, DSL integration, select, and condition groups. Existing records show focused diagnostics 33/33, projection box 22/22, DSL integration 23/23, exact core composition 114/114, full core 756/756, and five-database pagination 30/30.
- Result: partial/pass for the static audit. Compiler-wide opt-in and JOIN/nested/derived/union coverage are no longer gaps, and the historical 104/112 core failure is cleared. Task 5 advances to 80%, but the recorded 270/270 full compiler run predates the two newest diagnostics.
- Follow-up: the main agent must rerun the complete compiler-plugin suite, coverage, and all remaining non-Codacy PR gates on the final Task 2/3/4 worktree. IDEA and Marketplace proof remain owned by Tasks 9 and 10.

## 2026-07-20 Task 9 IDEA Baseline And Gap Audit

- Scope: compile and run the current IDEA plugin tests, then audit type resolution, completion, declaration view, quick documentation, FIR diagnostics delivery, bridge lifecycle, and nested/JOIN/union shapes.
- Evidence: `./gradlew :kronos-idea-plugin:test --no-daemon --console=plain` compiled production and tests against IDEA 2026.2, then ran 17 tests: 15 passed and two static smoke assertions failed. Independent source review found wrapper receiver over-completion, forced-nullable declaration text, duplicate shared Context emission, nested generic nullability loss, stale session/module models, empty navigation proof, and missing real highlighting/completion fixtures.
- Result: partial/fail. The two executed failures are stale tests, but the additional production and verification gaps are real; Task 9 advances only from 25% to 30% based on successful platform compilation and a concrete bounded repair plan.
- Follow-up: repair the three independent type/presentation/lifecycle boundaries through sub-agents, then run focused and complete IDEA tests serially.

## 2026-07-20 Task 8 Complete

- Scope: final pagination-stage verification after repairing current-source FIR bindings, non-root JOIN Selected typing, conservative stable-key propagation, and derived SQL aliases.
- Evidence: focused non-root JOIN and Union compiler boxes passed; complete `DslIntegrationBoxTest` passed 23/23; `StableKeyPropagationBehaviorTest` passed 5/5; complete compiler-plugin passed 270/270 in 4m35s; complete core passed 756/756 in 32s; the exact six-scenario filter passed across MySQL, PostgreSQL, SQL Server, Oracle, and SQLite for 30/30; legacy/debug scans returned no matches; `git diff --check` passed.
- Result: pass. `OffsetPageQuery` is the only selectable pagination stage, total/cursor stages remain execution-only, inner pagination is preserved through derived composition, and SQL labels agree with stable-key/projection metadata. Task 8 advances from 97% to 100% and is Done.
- Follow-up: continue with Task 9 only; keep every other unfinished task frozen until Task 9 reaches verified 100%.

## 2026-07-20 Stable-Key Alias Failure And Repair

- Scope: focused stable-key propagation across physical labels, composite UNIQUE keys, nullable-key rejection, JOIN/Union boundaries, and multiple derived aliases.
- Evidence: the first 5-case run had one exact SQL failure: metadata propagated `keyTwo`/`keyThree`, but the derived SQL omitted `AS keyTwo`/`AS keyThree`. Review traced the split to `SelectPlanner` comparing the current field name instead of the upstream derived output label. After the narrow planner fix, the unchanged exact SQL assertion and all five cases passed.
- Result: pass after one evidence-producing failure. SQL aliases and metadata now expose the same names at every derived layer; the failed run is retained because it identified a real invalid-SQL path rather than a stale expectation.
- Follow-up: none for Task 8.

## 2026-07-20 JOIN Cursor Context Normalization

- Scope: Task 8 generated Context ordering and JOIN cursor stable-key qualification.
- Evidence: the main agent ran focused `SelectFromPlannerCoverageTest` (`BUILD SUCCESSFUL` in 1m51s) and then the official `DslIntegrationBoxTest` (`BUILD SUCCESSFUL` in 1m26s, 22/22 tests).
- Result: pass. Unqualified generated Context ordering rebinds to the selected root physical field, and automatically appended child stable keys retain source-qualified expressions. The previous 21/22 compiler failure is cleared.
- Follow-up: implement and verify the newly locked `OffsetPageQuery : KSelectable` derived-source contract, then run Task 8 full and five-database gates.

## 2026-07-20 Task 8 Five-Database Gate Preparation

- Scope: locate the exact six pagination integration methods, concrete five-database subclasses, local services, credentials, and SQL Server prerequisites without executing tests.
- Evidence: repository suite/environment/compose inspection plus read-only `docker ps`; four external database containers are healthy and SQLite uses a JVM temporary file. The six methods expand to 30 concrete inherited test cases.
- Result: preparation pass only. `envsetup.sh` must be sourced because shell defaults do not match the MySQL/PostgreSQL containers, and SQL Server's `kronos_testing` database must be created idempotently before the focused Gradle command.
- Follow-up: after the offset-page source contract is green, prepare SQL Server without pulling images and run the exact 30-case filter.

## 2026-07-20 Offset Selectable First Focused Run

- Scope: compile and execute the new Offset `KSelectable` stage and count/source behavior tests.
- Evidence: `./gradlew :kronos-core:test --tests 'com.kotlinorm.orm.pagination.PaginationStageBehaviorTest' --tests 'com.kotlinorm.orm.pagination.PageQueriesBehaviorTest' --no-daemon --console=plain`; core main compiled, then test-source compilation failed at the test-only `page.alias("paged")` expression with the expected scalar-subquery `limit(1)` diagnostic.
- Result: fail before test execution. This does not contradict Offset inheritance; it shows that standalone alias invocation has scalar-subquery semantics and cannot serve as a multi-row method-visibility probe.
- Follow-up: keep inherited `alias` visibility under reflection, remove the semantically invalid invocation, and rerun the exact focused command.

## 2026-07-20 Offset Selectable Second Focused Run

- Scope: rerun the same two focused core suites after removing the invalid alias invocation.
- Evidence: compilation passed; Gradle executed 15 tests, with 14 passes and one `PaginationStageBehaviorTest` failure at outer cursor construction: `Cursor pagination requires a primary key or unique key tie-breaker.`
- Result: partial/fail. Offset derived select, independent outer offset page, stage reflection, insert inheritance, both total-count boundaries, and existing task behavior passed. The failure proves generated Selected currently drops the stable-key fact even when the derived output visibly retains the complete primary key.
- Follow-up: propagate only provable output stable-key candidates across the selectable/offset derived boundary, keep projections without a complete key rejected, then rerun the exact focused command.

## 2026-07-20 Offset Selectable Third Focused Run

- Scope: verify conservative stable-key propagation, missing-key rejection, and aliased-key propagation through an offset-page derived source.
- Evidence: compilation passed and 17 tests executed; 16 passed. Both the formerly failing ordinary outer cursor and the missing-key rejection passed. The sole failure was the new alias test's expected SQL omitting the existing page-1 `OFFSET 0`; actual SQL was otherwise exact.
- Result: partial/fail because the command is not green, but the production behavior under repair is now exercised successfully.
- Follow-up: correct only the stale expected string and rerun the same focused command.

## 2026-07-20 Offset Selectable Focused Gate

- Scope: final focused core verification of Offset `KSelectable`, derived pagination, total planning, wrapper reuse, and conservative stable-key propagation.
- Evidence: the exact two-suite Gradle command completed with `BUILD SUCCESSFUL` in 1m21s; XML records 17/17 tests passing with no failures, errors, or skips.
- Result: pass. A finite offset page composes as a derived source; its outer query can independently page or cursor when a complete stable key is provable, aliases propagate by output label, and incomplete-key projections remain rejected.
- Follow-up: rerun the official compiler integration box, the broader 57-case core batch, full module gates, and the focused 30-case five-database integration gate.

## 2026-07-20 Offset Selectable Compiler Gate

- Scope: official compiler integration coverage for JOIN generated Selected pagination followed by a derived `select`.
- Evidence: `DslIntegrationBoxTest` completed with `BUILD SUCCESSFUL` in 1m19s; XML records 22/22 tests with zero failures, errors, or skips.
- Result: pass. Generated Selected values and columns survive `page(...).select { ... }`, and the emitted outer SQL/AST retains the exact inner LIMIT/OFFSET query.
- Follow-up: rerun the broader focused core batch, then full core/compiler and five-database gates.

## 2026-07-20 Expanded Task 8 Focused Core Gate

- Scope: page tasks/stages, ordinary and JOIN cursor behavior, token encoding, and JOIN planner coverage after Offset/stable-key changes.
- Evidence: the exact five-suite command completed with `BUILD SUCCESSFUL` in 29s; XML totals 62/62 tests passing with zero failures, errors, or skips.
- Result: pass. All prior 57 cases remain green and the five new Offset/stable-key cases are included.
- Follow-up: run full core/compiler, the focused 30-case five-database gate, final legacy/debug scans, and diff sanity.

## 2026-07-20 Task 8 Full Core And Compiler Gates

- Scope: all core and compiler-plugin tests after final Offset/selectable/stable-key changes.
- Evidence: full core completed with `BUILD SUCCESSFUL` in 31s and XML totals 86 suites / 751 tests; full compiler completed with `BUILD SUCCESSFUL` in 4m15s and XML totals 25 suites / 269 tests. Both report zero failures, errors, and skips.
- Result: pass for both complete module gates.
- Follow-up: run the focused 30-case five-database integration gate and final static/diff sanity.

## 2026-07-20 Task 8 First Five-Database Attempt

- Scope: prepare the existing SQL Server container and run the six locked pagination methods across five databases.
- Evidence: SQL Server preparation returned `kronos_testing`; Gradle then failed in `:kronos-testing:compileTestKotlin` at `DslEdgeCaseIntegrationSuite.kt:256-257` before executing tests. The JOIN Selected exposed aliased root fields but omitted directly selected non-root `order.amount`, so derived `it.amount` references were unresolved.
- Result: fail before integration execution. No database behavior is inferred. The source is valid under the locked all-JOIN-fields projection rule, so aliasing `amount` merely to compile would hide a compiler defect.
- Follow-up: fix FIR generated declaration collection for non-root unaliased JOIN fields, add official testData, rerun compiler/full gates as needed, then rerun the exact 30-case integration command.

## 2026-07-20 Non-Root JOIN Field First Compiler Run

- Scope: run only the new official `joinNonRootUnaliasedProjection` compiler box after the FIR property-owner repair.
- Evidence: Gradle stopped in compiler-plugin production compilation at `KronosProjectionCallRefinementExtension.kt:471`; the current Kotlin API declares `FirPropertySymbol.callableId` nullable, but the new helper accessed `.classId` without a safe call.
- Result: fail before test execution. This is a compile-time API nullability correction, not evidence about the new projection behavior.
- Follow-up: preserve unknown-owner semantics with a null-safe ClassId read, then rerun the exact focused compiler method.

## 2026-07-20 Non-Root JOIN Field Second Compiler Run

- Scope: rerun the single official box after the nullable CallableId correction.
- Evidence: production and test compilation passed, then the box reported `INITIALIZER_TYPE_MISMATCH: expected Int?, actual String?` at `val amount: Int? = row.amount`. The generated property now exists but incorrectly uses the root table's same-name `amount: String?` instead of the captured non-root order field's `amount: Int?`.
- Result: fail. The deliberate same-name/different-type fixture proved that callable-symbol-first ownership is wrong for this JOIN fake-override FIR shape; no golden or expected source should be updated.
- Follow-up: make the concrete captured receiver authoritative when its owner conflicts with the fake/root callable symbol, then rerun the single box.

## 2026-07-20 Non-Root JOIN Field Third Compiler Run

- Scope: rerun the single official box after preferring a property resolved from `receiverCandidates()` over the JOIN root fake callable.
- Evidence: production and test compilation passed, but `DslIntegrationBoxTest.joinNonRootUnaliasedProjection` still failed with `INITIALIZER_TYPE_MISMATCH: expected Int?, actual String?` at `val amount: Int? = row.amount`.
- Result: fail. Merely reordering the existing receiver/property fallbacks did not prove that the surviving FIR projection node contains the captured `order` receiver or its `JoinNonRootOrder.amount: Int?` symbol.
- Follow-up: inspect the actual post-`projectionStatementOrNull()` FIR node, candidate receivers, resolved owner, and later registry/refinement writes before changing resolution again.

## 2026-07-20 Non-Root JOIN Field Fourth Compiler Run

- Scope: rerun the same official box after applying the follow-up receiver-owner implementation based on static FIR inspection.
- Evidence: `./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.DslIntegrationBoxTest.joinNonRootUnaliasedProjection' --no-daemon --console=plain` compiled the plugin and runner, then XML again reported `INITIALIZER_TYPE_MISMATCH: expected Int?, actual String?` at the typed `row.amount` assignment; one test executed and failed.
- Result: fail. The generated field still takes the root `JoinNonRootUser.amount: String?` type. The implementation needs evidence from the exact FIR node path rather than another fallback-order change. Task 8 remains 97%.
- Follow-up: the compiler subtask must report the concrete receiver/symbol/type evidence and locate any later overwrite before another production change; no database gate should run meanwhile.

## 2026-07-20 Non-Root JOIN Field FIR Trace

- Scope: identify the exact phase and owner/type loss for the failing `order.amount` projection without changing the fixture.
- Evidence: the main agent reran the focused official box with temporary amount-only FIR tracing and `--rerun-tasks --info`. At the original projection leaf, `order.amount` retained an `order` receiver node, but both its resolved symbol owner and type were null; `receiverTypes` was empty, so model construction fell back by name to root `JoinNonRootUser.amount: String?`. The registered model and declaration generator then consumed that same `String?` unchanged. The outer derived `it.amount` correctly inherited the already-wrong generated type.
- Result: diagnostic fail with a confirmed cause. There is no later registry/refinement overwrite; the wrong type originates when the unresolved captured JOIN receiver is interpreted as a root field during projection model construction.
- Follow-up: recover the captured receiver's declared type from the enclosing JOIN lambda/value-parameter context, retain correct root-field behavior, remove every temporary trace, and rerun the exact focused box.

## 2026-07-20 Non-Root JOIN Field Focused Pass And Broad Regression

- Scope: verify the narrow JoinSource receiver binding first, then run the complete official `DslIntegrationBoxTest` surface.
- Evidence: `DslIntegrationBoxTest.joinNonRootUnaliasedProjection` completed with `BUILD SUCCESSFUL` in 1m36s; the deliberate root `amount: String?` versus non-root `amount: Int?` fixture now compiles and validates generated Selected, derived Selected, mapping, output names, and order-table ownership. The subsequent complete suite executed 23 tests: 16 passed and 7 failed. Six selectable/union insert cases reported unresolved `it.id`, and `joinSelectableSource` reported unresolved `order.userId`.
- Result: partial/fail. The new focused contract is fixed, while the broad run exposes a shared generated/selectable receiver regression. The failure pattern is consistent with the current shared `resolvedConeType()` no longer consulting the resolved symbol return type when `coneTypeOrNull` is absent at this FIR phase.
- Follow-up: restore the shared resolved-symbol type fallback without weakening the JoinSource-only lexical binding, rerun the exact focused box, and then rerun all of `DslIntegrationBoxTest`. Task 8 remains 97%.

## 2026-07-20 Task 8 Post-Review Core Compilation

- Scope: compile the reviewed pagination-stage, cursor-safety, JOIN stable-key, token-type, and regression-test changes before running behavior tests.
- Evidence: the main agent ran `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain`; core main compilation completed, then test compilation reported `CursorEncodingTest.kt:107` nullable class-literal usage and `PageQueriesBehaviorTest.kt:62` missing expected-value generic inference.
- Result: partial/fail. No production compilation error was reported, but test sources did not compile and no behavior test ran.
- Follow-up: fix only the two test typing errors, rerun the same command serially, and keep Task 8 at 47% until a green compile provides new evidence.

## 2026-07-20 Task 8 Core Compilation Rerun

- Scope: rerun core test-source compilation after the two focused test-only type fixes.
- Evidence: `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain` completed with `BUILD SUCCESSFUL` in 1m23s; 24 actionable tasks, one executed and 23 up-to-date.
- Result: pass. Reviewed Task 8 production code and all core test sources compile; no behavior test is inferred from this command.
- Follow-up: run the focused pagination/cursor/JOIN behavior batch before broader core, compiler, and five-database gates.

## 2026-07-20 Task 8 Focused Behavior Batch

- Scope: page result/stages, ordinary and JOIN cursor behavior, cursor token encoding, and JOIN planner cursor coverage.
- Evidence: the main agent ran the five-class filtered `:kronos-core:test` command; 57 tests executed, 56 passed, and `SelectFromPlannerCoverageTest.self join cursor uses distinct hidden labels token keys and after parameters` failed before SQL because `JoinIdentityRow` declared no primary or non-null unique key.
- Result: partial/fail. All new page/cursor behavior tests passed, including inner/cross JOIN stable keys and unsafe-shape rejection; the historical self-join fixture lacks the stable-key metadata required by the new contract.
- Follow-up: annotate the fixture `id` as its primary key and rerun the exact focused batch serially; Task 8 remains 65% until the batch is green.

## 2026-07-20 Task 8 Focused Behavior Rerun

- Scope: rerun the exact five-suite page/cursor/JOIN batch after adding stable-key metadata to the self-join fixture.
- Evidence: the filtered `:kronos-core:test` command completed with `BUILD SUCCESSFUL` in 1m25s; XML reports `PageQueriesBehaviorTest` 6/6, `PaginationStageBehaviorTest` 7/7, `CursorClauseBehaviorTest` 27/27, `CursorEncodingTest` 6/6, and `SelectFromPlannerCoverageTest` 11/11. `git diff --check` passed.
- Result: pass. All 57 focused tests are green with zero failures, errors, or skipped tests.
- Follow-up: run compiler pagination/JOIN box coverage, then full core/compiler and focused five-database integration gates.

## 2026-07-20 Task 8 Compiler Pagination Box

- Scope: compiler-generated JOIN Selected and Context types across offset, total, and cursor stages.
- Evidence: `./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.DslIntegrationBoxTest' --no-daemon --console=plain` executed 22 tests; 21 passed. `joinPaginationTypeStages` returned three unqualified `id` order items instead of the expected root output plus child stable key.
- Result: fail with actionable production evidence. Generated Context `it.id` was not normalized to the selected root physical field, so root PK was appended twice; the child PK was also resolved through the same-name selected alias.
- Follow-up: normalize unqualified JOIN Context order fields through selected output metadata and force appended leaf tie-breakers to retain source-qualified expressions, then rerun focused core and compiler gates.

## 2026-07-20 Task 7 Complete

- Scope: Task 7 full core regression gate and final diff/production static sanity after the full compiler-plugin gate passed.
- Evidence: the main agent ran `./gradlew :kronos-core:test --no-daemon --console=plain`; Gradle reported `BUILD SUCCESSFUL` in 28s. Aggregated XML contains 86 suites and 734 tests with zero failures, zero errors, and zero skipped tests. Final `git diff --check` passed, and production scans for old `SelectFrom2..16`, `PagedClause`, `OffsetPageable`, and `CursorClause` symbols plus `/tmp` debug paths returned no matches.
- Result: pass. Task 7 advances from 99% to 100% and is Done. Task 8 remains unchanged at 47% / In Progress, and becomes the current focus.
- Follow-up: continue with Task 8 only; do not infer Task 8 progress from Task 7 evidence.

## 2026-07-20 Task 7 Full Compiler-Plugin Gate Green

- Scope: Task 7 full compiler-plugin regression gate after all targeted JOIN, projection, diagnostics, and subquery evidence became green.
- Evidence: the main agent ran `./gradlew :kronos-compiler-plugin:test --no-daemon --console=plain`; Gradle reported `BUILD SUCCESSFUL` in 4m5s. Aggregated XML contains 25 suites and 269 tests with zero failures, zero errors, and zero skipped tests.
- Result: pass. The full compiler-plugin gate advances Task 7 from 97% to 99%; every other task remains frozen and unchanged.
- Follow-up: run the full core gate, then final `git diff --check` and legacy/debug static sanity.

## 2026-07-20 Task 7 JOIN Selected Subquery Composition Green

- Scope: Task 7-only compiler-pipeline evidence for using a JOIN selected query in scalar and predicate subqueries.
- Evidence: after review of `joinSelectedSubqueryComposition`, the main agent ran `./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.DslIntegrationBoxTest' --no-daemon --console=plain`; Gradle reported `BUILD SUCCESSFUL` in 1m23s, and XML reports 22 tests with all 22 passing and zero failures/errors.
- Result: pass. The fixture verifies generated Selected mapping/type plus scalar-subquery and `IN` predicate composition, including the expected nested outer `Select` to inner `Join` ON/WHERE AST. Task 7 advances from 94% to 97%; every other task remains frozen and unchanged.
- Follow-up: run the full compiler-plugin gate, full core gate, and final `git diff --check`.

## 2026-07-20 Task 7 Focused Union Green

- Scope: Task 7-only focused Union guard regression after correcting the test's data-source isolation without changing production behavior.
- Evidence: after test-only `UserRelation` isolation, the main agent ran `./gradlew :kronos-core:test --tests 'com.kotlinorm.orm.union.UnionClauseBehaviorTest' --no-daemon --console=plain`; Gradle reported `BUILD SUCCESSFUL` in 1m21s and all 7 tests passed.
- Result: pass. Limit planning remains dialect-neutral under `NoneDataSourceWrapper`, and an explicit SQL Server wrapper still rejects `limit` without `orderBy`. Task 7 advances from 92% to 94%; every other task remains frozen and unchanged.
- Follow-up: verify JOIN selected scalar/predicate-subquery evidence, then run full compiler/core gates.

## 2026-07-20 Task 7 Legacy Scan Pass And Focused Union Isolation Failure

- Scope: Task 7-only production legacy/debug acceptance plus the focused Union guard regression excluded by the exact 114-test filter.
- Evidence: static scans found no production `SelectFrom2..16`, `PagedClause`, `OffsetPageable`, or `CursorClause` symbols, no `/tmp` debug writes, and no old pre-select forwarding APIs; historical release-note mentions and one legacy-named test class are non-production references. The main agent then ran `./gradlew :kronos-core:test --tests 'com.kotlinorm.orm.union.UnionClauseBehaviorTest' --no-daemon --console=plain`; 7 tests executed, 6 passed, and 1 failed.
- Result: partial. The static acceptance passes and advances Task 7 from 90% to 92%; every other task remains frozen and unchanged. The Union failure occurs before the guard because `TestUser` branches invoke logical-delete boolean rendering under `NoneDataSourceWrapper`, so it is a test-isolation defect and does not prove the guard passes or fails.
- Follow-up: a child is correcting only the focused test. Rerun it afterward, then verify JOIN selected scalar/predicate-subquery evidence before full compiler/core gates.

## 2026-07-20 Task 7 Projection Box Green

- Scope: Task 7-only official projection box verification after JOIN/compiler shapes and diagnostics stabilized.
- Evidence: the main agent ran `./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.ProjectionBoxTest' --no-daemon --console=plain`; Gradle reported `BUILD SUCCESSFUL` in 1m36s, and the XML report records 22 tests with all 22 passing.
- Result: pass. Official box-test projection behavior is green, advancing Task 7 from 88% to 90%; every other task remains frozen and unchanged.
- Follow-up: run focused `UnionClauseBehaviorTest`, perform the legacy API/debug static scan, verify JOIN selected scalar/predicate-subquery evidence, then run full compiler/core gates.

## 2026-07-20 Task 7 Projection Diagnostics Green

- Scope: Task 7-only projection diagnostics rerun after aligning the duplicate JOIN projection's standard opt-in diagnostic range to the property symbol.
- Evidence: the main agent ran `./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.ProjectionDiagnosticsTest' --no-daemon --console=plain`; Gradle reported `BUILD SUCCESSFUL` in 46s with all 31 tests passing.
- Result: pass. `joinDuplicateProjection` retains the required standard `OPT_IN_USAGE_ERROR`, and its golden now marks the `id` property symbol in `company.id`. Task 7 advances from 85% to 88%; every other task remains frozen and unchanged.
- Follow-up: run `ProjectionBoxTest` next.

## 2026-07-20 Task 7 Projection Diagnostics At 30/31

- Scope: Task 7 duplicate JOIN projection diagnostics after compiler integration and the exact core batch became green.
- Evidence: the main agent ran `./gradlew :kronos-compiler-plugin:test --tests "com.kotlinorm.compiler.ProjectionDiagnosticsTest" --no-daemon --console=plain`; compiler/test sources compiled and 31 tests executed: 30 passed and 1 failed. `joinDuplicateProjection` emitted the required standard `OPT_IN_USAGE_ERROR`; actual output marks the referenced property symbol as `company.<!OPT_IN_USAGE_ERROR!>id<!>`, consistent with existing property-access opt-in fixtures, while the golden incorrectly marked the entire qualified access.
- Result: fail overall due only to diagnostic marker range. The expected marker now wraps only `id`; production behavior, test input, and the duplicate JOIN projection opt-in requirement are unchanged. Static `diff -u` against the compiler-produced actual file and `git diff --check` both pass. No Gradle command was run in this golden-only edit pass, and Task 7 remains at 85%.
- Follow-up: rerun the exact 31-test diagnostics suite serially, then run `ProjectionBoxTest` if it passes.

## 2026-07-20 Task 7 Source-Aware Qualifiers And Exact Core Batch Green

- Scope: Task 7-only verification of the unified source-aware qualifier rule followed by the exact core JOIN, pagination, CTAS, insert-select, MySQL JOIN, and MySQL union batch.
- Evidence: targeted `JoinSelectableSqlTest` plus `SelectFromPlannerCoverageTest` completed with `BUILD SUCCESSFUL` in 1m48s. The main agent then ran `./gradlew :kronos-core:test --tests 'com.kotlinorm.orm.join.*' --tests 'com.kotlinorm.orm.pagination.*' --tests 'com.kotlinorm.orm.ddl.CreateTableAsSelectSqlTest' --tests 'com.kotlinorm.orm.insert.InsertSelectSqlTest' --tests 'com.kotlinorm.orm.sql.mysql.MysqlJoinSqlTest' --tests 'com.kotlinorm.orm.sql.mysql.MysqlUnionSqlTest' --no-daemon --console=plain`; Gradle reported `BUILD SUCCESSFUL` in 26s with all 114 tests passing.
- Result: pass. Qualification is determined by the `FromSource` that exposes a field, so derived aliases such as `q.rankStatus` are retained consistently by direct selectable and planner paths. All eight historical core failures are cleared, advancing Task 7 from 80% to 85%; every other task remains frozen and unchanged.
- Follow-up: run `ProjectionDiagnosticsTest`, then `ProjectionBoxTest`.

## 2026-07-20 Task 7 Official Compiler Integration Green

- Scope: Task 7-only serialized compiler-pipeline verification after stabilizing `joinOperandMatrix` labels.
- Evidence: the main agent ran `./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.DslIntegrationBoxTest' --no-daemon --console=plain`; compiler/test sources compiled and Gradle reported `BUILD SUCCESSFUL` in 1m16s with all 21 tests passing.
- Result: pass. Generated terminals, recursive JOIN shapes, all 12 operand-matrix shapes, derived-plus-union composition, and pagination type stages are verified through the compiler pipeline. Task 7 advances from 70% to 75%; every other task remains frozen and unchanged.
- Follow-up: run the exact 112-test core JOIN/pagination/composition batch next and re-evaluate its eight historical failures.

## 2026-07-20 Task 7 Operand-Matrix Fixture Label

- Scope: the sole remaining `joinOperandMatrix` fixture representation failure; no production behavior or expected AST shape changed.
- Evidence: `SqlTable.Join.shape()` now renders an exhaustive deterministic `SqlJoinType` label covering `Inner`, `Left`, `Right`, `Full`, `Cross`, and `UnsafeCustom`. All 12 concrete expected operand shapes are unchanged. `git diff --check` passed.
- Result: static review passed. No Gradle command was run in this edit pass so Task 7 remains at 70% based on the latest executed 20/21 result.
- Follow-up: rerun the exact 21-test `DslIntegrationBoxTest` command serially and confirm `joinOperandMatrix` plus the full suite pass.

## 2026-07-20 Task 7 Official Compiler Integration Rerun At 20/21

- Scope: Task 7-only rerun after the Union empty-data-source guard, covering the operand matrix and derived/union nested composition.
- Evidence: the main agent reran `./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.DslIntegrationBoxTest' --no-daemon --console=plain`. Compiler and test sources compiled, then 21 tests executed: 20 passed and 1 failed. `joinDerivedUnionComposition` passed. The only failure was `joinOperandMatrix`; its 12 actual AST shapes retained the expected topology but interpolated ordinary `SqlJoinType` objects as identity strings such as `SqlJoinType$Left@...`.
- Result: fail overall due only to test fixture representation, not planner behavior. Derived/union nested composition is now proven, advancing Task 7 from 65% to 70%; every other task remains frozen and unchanged.
- Follow-up: use an exhaustive deterministic `SqlJoinType` label in the fixture without changing the 12 expected shapes, then rerun the exact 21-test command serially.

## 2026-07-20 Task 7 Union Empty-Data-Source Guard

- Scope: the single remaining compiler-integration root cause in `UnionClause.validateSqlServerLimit` plus Task 7-only regression coverage and tracking.
- Evidence: `UnionClause` now returns from SQL Server-specific validation when the resolved wrapper is exactly `NoneDataSourceWrapper`; focused core tests assert dialect-neutral Union limit planning without a configured data source and assert that an explicit SQL Server wrapper still rejects `limit` without `orderBy`. `git diff --check` passed.
- Result: static review passed. No Gradle command was run in this edit pass so behavioral verification remains serialized with the main agent; Task 7 stays at 65% based on the latest executed 19/21 result.
- Follow-up: run the exact `DslIntegrationBoxTest` command, then the exact targeted core batch if all 21 compiler-integration tests pass.

## 2026-07-20 Task 7 Official Compiler Integration Rerun

- Scope: Task 7-only rerun after the JOIN planner empty-data-source guard and derived selectable qualification fixes.
- Evidence: the main agent reran `./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.DslIntegrationBoxTest' --no-daemon --console=plain`. Compiler and test sources compiled, then 21 tests executed: 19 passed and 2 failed. `joinSelectableSource` and `joinPaginationTypeStages` passed; `joinOperandMatrix` and `joinDerivedUnionComposition` both failed with `NoDataSourceException` from `UnionClause.validateSqlServerLimit` reading `NoneDataSourceWrapper.dbType`.
- Result: fail overall with two newly proven contracts. Generated terminal property access, pagination staging, and derived selectable qualification are now green, advancing Task 7 from 60% to 65%; every other task remains frozen and unchanged.
- Follow-up: add the Union-specific empty-data-source guard and focused core regression coverage, then rerun the exact 21-test command serially.

## 2026-07-20 Task 7 Official Compiler Integration Run

- Scope: Task 7-only official compiler integration coverage for JOIN operands, pagination stages, derived/union composition, derived-source qualification, and generated-Selected terminal property reads.
- Evidence: the main agent ran `./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.DslIntegrationBoxTest' --no-daemon --console=plain`. Compiler and test sources compiled, then 21 tests executed: 17 passed and 4 failed. `joinOperandMatrix`, `joinPaginationTypeStages`, and `joinDerivedUnionComposition` failed with `NoDataSourceException` from `JoinedSelectPlanner` SQL Server detection; `joinSelectableSource` returned unqualified `rankStatus` instead of `q.rankStatus`.
- Result: fail overall with focused runtime evidence. Traversal-order-independent generated Selected materialization is implemented; the prior fake-override backend crash did not recur; and `joinSelectGeneratedQueryReturn` passed generated property reads from helper-returned `toList`, `first`, and `firstOrNull` results. These completed surfaces advance Task 7 from 52% to 60%; every other task remains frozen and unchanged. The four failures remain under two planner root causes: SQL Server data-source detection and derived-source qualification.
- Follow-up: keep all tasks other than Task 7 frozen, fix the three data-source detection failures and one derived qualifier failure, then rerun the exact 21-test command.

## 2026-07-20 Task 7 Targeted Rerun Blocked During Test Compilation

- Scope: Task 7-only rerun of the targeted core JOIN, pagination, CTAS, insert-select, MySQL JOIN, and MySQL union command after updates to the generated-Selected assertions.
- Evidence: the main agent reran `./gradlew :kronos-core:test --tests 'com.kotlinorm.orm.join.*' --tests 'com.kotlinorm.orm.pagination.*' --tests 'com.kotlinorm.orm.ddl.CreateTableAsSelectSqlTest' --tests 'com.kotlinorm.orm.insert.InsertSelectSqlTest' --tests 'com.kotlinorm.orm.sql.mysql.MysqlJoinSqlTest' --tests 'com.kotlinorm.orm.sql.mysql.MysqlUnionSqlTest' --no-daemon --console=plain`. Gradle failed in `:kronos-core:compileTestKotlin` before executing any test because direct generated-Selected property reads in `SelectFromBehaviorTest` triggered Kotlin JVM backend error `Fake override should have at least one overridden descriptor: <get-id>`.
- Result: fail before test execution. This run provides no new runtime pass/fail evidence and does not increase any task percentage.
- Follow-up: keep Task 8 and all tasks other than Task 7 frozen, clear the generated-Selected JVM backend blocker, and rerun the exact command until Task 7 has verified acceptance evidence.

## 2026-07-20 Targeted Core JOIN, Pagination, And Composition Batch

- Scope: Task 5/7/8 behavior across all core JOIN and pagination tests plus CTAS, insert-select, MySQL JOIN, and MySQL union composition.
- Evidence: the main agent ran `./gradlew :kronos-core:test --tests 'com.kotlinorm.orm.join.*' --tests 'com.kotlinorm.orm.pagination.*' --tests 'com.kotlinorm.orm.ddl.CreateTableAsSelectSqlTest' --tests 'com.kotlinorm.orm.insert.InsertSelectSqlTest' --tests 'com.kotlinorm.orm.sql.mysql.MysqlJoinSqlTest' --tests 'com.kotlinorm.orm.sql.mysql.MysqlUnionSqlTest' --no-daemon --console=plain`. Gradle reported 112 tests completed, 8 failed, and therefore 104 passed.
- Result: fail overall. Failures are `JoinSelectableSqlTest` (1 `ComparisonFailure`), `SelectFromBehaviorTest` (1 `ClassCastException`), `SelectFromSourceIdentityTest` (1 `ComparisonFailure`), and `MysqlJoinSqlTest` (5 `ComparisonFailure`). Generated arity/syntax/planner, pagination-package, CTAS, insert-select, and MySQL union tests in the same batch reported no failures, but that does not turn the failed command into a passing gate.
- Follow-up: repair the eight JOIN/mapping/qualification failures and rerun the exact command before claiming targeted core behavior is green. Task 5 advances to 58%, Task 7 to 52%, and Task 8 only to 47%.

## 2026-07-20 Sixth Core Compilation

- Scope: Task 7 core main/test-source compilation after correcting the stale generated-Selected assertions; compilation coverage also includes the new shared JOIN/pagination source surface.
- Evidence: the main agent reran `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain`; it completed with `BUILD SUCCESSFUL` in 1m3s, and core main plus all test sources compiled. The earlier generic mismatches, JVM signature clash, test migration failures, and generated Selected assertion errors did not recur.
- Result: pass for compilation. The complete core source/test-source surface now compiles, but this command did not execute any behavioral test. Task 7 advances conservatively from 45% to 50%; Task 8 remains at 45%.
- Follow-up: run targeted core JOIN and pagination behavior tests serially before increasing either task based on runtime evidence.

## 2026-07-20 Fifth Core Compilation

- Scope: Task 7 core test compilation after the default JOIN terminal expectations were narrowed to verify generated Selected inference.
- Evidence: the main agent reran `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain`; default terminal calls inferred the compiler-generated Selected type successfully. The only remaining diagnostics are three `SelectFromBehaviorTest` `assertEquals<T>` calls that cannot unify a `TestUser` expected value with the generated Selected actual value.
- Result: partial/fail. Generated terminal type inference is proven, but the three stale generic assertions still prevent test-source compilation. No test was executed, and Task 7 remains at 45%.
- Follow-up: update those three expectations without forcing `TestUser`, then rerun the same command serially before any behavioral test.

## 2026-07-20 Fourth Core Compilation

- Scope: Task 7 core test compilation after the third run's JOIN test-migration failures were corrected.
- Evidence: the main agent reran `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain`; every prior DDL/insert, JOIN behavior, `PageQueries`, fixture, and duplicate-projection migration error disappeared. `compileTestKotlin` now reports only three `SelectFromBehaviorTest` expectations that require `TestUser` even though the new API correctly returns the generated `KronosSelectResult` Selected type.
- Result: partial/fail. The earlier migration blockers are cleared, but the three stale test expectations still prevent test-source compilation and no test ran.
- Follow-up: update those expectations to the generated Selected type, rerun the same command serially, and keep Task 7 at 45% until test compilation and behavioral tests pass.

## 2026-07-20 Third Core Compilation

- Scope: Task 7 core main/test compilation after resolving the `SelectFromContext.kt` `cursorKey` JVM signature clash.
- Evidence: the main agent reran `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain`; `kronos-core:compileKotlin` succeeded and the `cursorKey` clash did not recur, then `compileTestKotlin` reported migrated-test errors in DDL/insert JOIN blocks that do not end in `select`, JOIN behavior and `PageQueries` type inference, a missing `SampleMssqlJdbcWrapper`, and a duplicate self-join projection without opt-in.
- Result: partial/fail. Core main compilation is now proven, but core test-source compilation still fails and no test ran.
- Follow-up: finish the listed test migrations, rerun the same command serially, and keep Task 7 at 45% until test compilation and behavioral tests pass.

## 2026-07-20 Second Core Compilation

- Scope: Task 7 core main/test compilation after resolving the 12 generated JOIN `KPojo`-to-`Source` generic mismatches.
- Evidence: the main agent reran `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain`; none of the previous 12 mismatches appeared, but `kronos-core:compileKotlin` reported a JVM signature clash in `SelectFromContext.kt` between the ordinary `cursorKey(Field)` function and the `Field.cursorKey()` member extension.
- Result: partial/fail. The previous blocker is cleared, but the command still stopped during core main compilation, before core test compilation; no test ran.
- Follow-up: resolve the `cursorKey` signature clash, rerun the same command serially, and keep Task 7 at 40% until compilation and behavioral tests provide stronger evidence.

## 2026-07-19 Core Compilation After Compiler Fix

- Scope: Task 2 compiler-plugin main compilation and Task 7 core compilation against the generated JOIN API.
- Evidence: the main agent ran `./gradlew :kronos-core:compileTestKotlin --no-daemon --console=plain`; compiler-plugin `compileKotlin` completed successfully with only existing warnings, then `kronos-core:compileKotlin` reported 12 `SelectFrom.kt` `KPojo`-to-`Source` generic mismatches.
- Result: partial/fail. The prior compiler-plugin main compilation blocker is cleared, but the overall command failed before core test compilation and no test was executed.
- Follow-up: fix the 12 core generic mismatches, rerun the same compile command serially, and only then start targeted JOIN/pagination tests.

## 2026-07-19 JOIN Codegen Determinism

- Scope: Task 7 generated JOIN arities and operand overload surface.
- Evidence: `generateJoinClause` completed with `BUILD SUCCESSFUL` twice; SHA-256 values for generated `Patch.kt` and `JoinSources.kt` were identical across both runs; static output review found multi-generic `JoinSource2..16`, all nine table / `KSelectable` / nested `JoinSource` operand combinations, and no generated old `SelectFromN` family.
- Result: partial pass. Generation is reproducible and the intended generic/operand surface is present.
- Follow-up: core compilation, compiler integration, recursive planner/runtime behavior, pagination integration, and JOIN test migration have not run and remain Task 7 verification gaps.

## 2026-07-19 IDEA Static Implementation Review

- Scope: Task 9 shared projection type resolution and tests; Task 10 IDEA 2026.2 build/publication hardening.
- Evidence: main-agent static review of the shared IDEA projection type resolver and test edits; explicit `262..262.*` range, stable 2026.2 target, Temurin 25, signed-archive publication binding, fail-fast structure/signature/descriptor/Plugin Verifier dependencies, and final-artifact inspection script.
- Result: partial pass. The intended source/configuration changes are present, but no IDEA test, signed artifact build, descriptor assertion, or Plugin Verifier has run.
- Follow-up: run serialized IDEA tests and artifact verification after JOIN/compiler shapes stabilize; do not publish as part of verification.

## 2026-07-19 Ordinary Select Pagination Stages

- Scope: Task 8 ordinary `SelectClause` page, total-page, and cursor stages; named result/task holders; immutable pagination snapshots; typed cursor behavior; supported-dialect SQL rendering.
- Evidence: the main agent serially passed `:kronos-core:compileTestKotlin --rerun-tasks` and the targeted `PaginationStageBehaviorTest`; the full 741-test core run passed the existing cursor, ordinary pagination, and five-dialect SQL cases apart from one stale `id` versus `idMin` expected parameter name, which was corrected and then targeted verification passed.
- Result: partial pass. Ordinary select stages are verified, including stage-method exclusivity, `PageResult` / `CursorResult`, base and sibling query isolation, derived-source state, wrapper reuse, and typed cursor visibility requirements.
- Follow-up: Task 7 must connect JOIN selected queries to the same stages and remove `PagedClause` / `OffsetPageable`; Task 8 still needs five-database integration and complete non-Codacy gate evidence.

## 2026-07-19 Issue and source audit

- Scope: Issue 266 proposal and current projection implementation.
- Evidence: Issue body and comments fetched with `gh issue view 266 --comments`; current `main` updated to `2c774ebfe`; read-only inspection of `KronosProjectionCheckersExtension`, `KronosProjectionCallRefinementExtension`, `KronosProjectionModel`, `KronosProjectionIrTransformer`, `SelectClause`, `UnsafeCondition`, and projection testData.
- Result: pass for planning basis. The proposal is reasonable, with explicit requirements for standard opt-in reporting, source-minus effective shape, and selected-type-authoritative IR materialization.
- Follow-up: Tasks 1-6.

## 2026-07-19 Kotlin FIR opt-in API audit

- Scope: Confirm whether standard Kotlin opt-in scope acceptance can be reused.
- Evidence: Kotlin 2.4.10-RC compiler classes expose `FirOptInUsageBaseChecker.loadExperimentality...` and `reportNotAcceptedExperimentalities(...)`; `@OptIn` supports expression/function/class/file targets.
- Result: pass for design feasibility; implementation remains unverified.
- Follow-up: Task 2 must isolate compiler-version-sensitive API usage and add diagnostics coverage.

## 2026-07-19 Workspace safety check

- Scope: Ensure task-list creation starts from updated `main` without unrelated edits.
- Evidence: `git switch main`, `git pull --ff-only origin main`, `git status --short --branch`.
- Result: pass; workspace was clean on updated `main`.
- Follow-up: none for planning phase.

## 2026-07-19 Issue 266 implementation slice

- Scope: public opt-in marker, FIR collision checker, effective source-minus Context shape, selected-type-authoritative IR materialization, diagnostics/box fixtures, and bilingual docs.
- Evidence: `UnsafeProjectionOverride.kt`, projection FIR/IR files, `projectionAliasOptInScopes.kt`, `selectedAliasOverrideType.kt`, and compiler-plugin docs.
- Result: targeted compile, full `ProjectionDiagnosticsTest`, full `ProjectionBoxTest`, and `git diff --check` pass.
- Follow-up: compiler-wide `-opt-in`, same-layer `where`, derived outer-select, full compiler-plugin/core gates, and docs build.

## 2026-07-19 Expanded projection and query API design

- Scope: duplicate Selected names, Context shadowing, JOIN return-type propagation and nesting, page/cursor type stages, nested queries, union, Map mapping, and IDEA presentation.
- Evidence: user design decisions; current `SelectFrom`/`SelectClause`/pagination/union code; uncommitted FIR/runtime allocator changes; targeted failing JOIN projection fixture.
- Result: partial. Projection allocation is implemented in slices, but the old JOIN `Unit` lambda prevents outer generated-type propagation and the broader contract is unverified.
- Follow-up: Tasks 2-9.

## 2026-07-19 IDEA 2026.2 compatibility audit

- Scope: compare the reported Marketplace incompatibility with local build configuration and signed artifacts.
- Evidence: supplied screenshot says `Not compatible with the version of your running IDE (IntelliJ IDEA 2026.2)`; `kronos-idea-plugin/build.gradle.kts` targets IDEA `2026.2` / build `262.8665.258`; local signed `0.2.4` and `0.2.5-SNAPSHOT` ZIP plugin descriptors contain `since-build="262"` and no `until-build`.
- Result: mismatch remains. Source configuration/local ZIP alone does not prove the Marketplace artifact is the same or installable.
- Follow-up: Task 10 must make the range explicit, inspect the final signed ZIP, verify the uploaded Marketplace metadata, and install from Marketplace on IDEA 2026.2.

## 2026-07-19 Ordinary projection duplicate-name verification

- Scope: standard opt-in diagnostics, Context shadow reads, deterministic duplicate-name allocation, independent alias types, typed/Map mapping, derived select, union first-branch naming, and core SQL/result-column metadata.
- Evidence: `ProjectionDiagnosticsTest.duplicateProjectionProperty`, `ProjectionDiagnosticsTest.selectedAliasConflictsWithSource`, `ProjectionBoxTest.duplicateProjectionOutputNames`, plus the full 741-test `:kronos-core:test` run containing `ProjectionNamesTest` and `MysqlSelectSqlTest.testDuplicateProjectionNamesReserveExplicitSuffixes`.
- Result: pass. Ordinary select resolves `id, id, id_1` to `id, id_2, id_1`; opted-in same-name aliases retain independent selected types; typed, Map, derived, and union consumers agree; harmless alias shadowing reports only when same-layer Context reads the shadowed name.
- Follow-up: verify JOIN and nested-query propagation, IDEA presentation, coverage, five databases, and the remaining non-Codacy gates.

## 2026-07-19 Marketplace 0.2.4 state audit

- Scope: distinguish descriptor compatibility from Marketplace publication state without using credentials or publishing a new update.
- Evidence: public Marketplace plugin/update APIs for plugin `32985` and update `1110390`; exact-version download; build-compatible download for `IU-262.8665.258`; nested remote `META-INF/plugin.xml`; local and remote SHA-256 values.
- Result: partial. Marketplace metadata reports `since=262.0+` and `IDEA_PRO=2026.2+`, but the update is `approve=false` and `listed=false`; exact-version download works while the IDE-compatible endpoint does not. The remote descriptor has only `since-build="262"`, and its ZIP hash differs from the local candidate.
- Follow-up: verify the refreshed signed `262..262.*` artifact locally, publish only through the guarded release workflow, then repeat the Marketplace download/descriptor/install checks after approval.

## 2026-07-20 Allocated projection labels in JDBC Map mapping

- Scope: Task 4 runtime consumption of duplicate opted-in projection labels after SQL-name allocation.
- Evidence: `KronosResultMappingTest.map rows preserve allocated duplicate projection labels and target types` supplied `id` and `id_1` ResultSet labels with numeric JDBC values and a matching `resultColumnTypes` map; the test passed.
- Result: pass for the focused JDBC mapping boundary. The mapper retains both allocated Map keys and converts each value independently to its selected `Int` or `Long` type.
- Follow-up: JOIN, nested-query, cursor-only, cascade, IDEA, and broader/full runtime consumers remain open; no Task 4 completion claim is made from this focused test.
