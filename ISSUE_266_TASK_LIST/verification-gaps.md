# Current Verification Gaps

Updated: 2026-07-20

- Compiler-wide `-opt-in` is covered by official testData and the focused projection diagnostics suite is green at 33/33.
- The generated JOIN API is reproducible across two successful generator runs, and traversal-order-independent generated Selected materialization is implemented. The expanded serialized `DslIntegrationBoxTest` completed with `BUILD SUCCESSFUL` in 1m23s; XML reports 22 tests with all 22 passing and no failures/errors.
- The compiler pipeline verifies generated terminals, recursive JOIN shapes, all 12 table / derived `KSelectable` / nested `JoinSource` operand-matrix shapes, derived-plus-union composition, pagination type stages, and `joinSelectedSubqueryComposition`. The new fixture proves generated Selected mapping/type and both scalar and `IN` predicate composition through the nested `Select -> Join` ON/WHERE AST. Focused `UnionClauseBehaviorTest` is also 7/7 after test-only `UserRelation` isolation.
- The original targeted core behavior batch recorded 104 passes and 8 failures. After unifying the source-aware qualifier rule across direct selectable and planner paths, targeted `JoinSelectableSqlTest` plus `SelectFromPlannerCoverageTest` passed in 1m48s and the exact filtered core batch passed all 114 tests in 26s; all eight historical failures are cleared.
- Generated JOIN arity, source-aware qualification, recursive planning, parameter ordering, pagination-package behavior, CTAS, insert-select, and MySQL JOIN/union behavior are green in the exact core batch. `ProjectionDiagnosticsTest` is 33/33, and `ProjectionBoxTest` is 22/22 according to its XML report.
- The production legacy/debug scan is complete: removed `SelectFrom2..16`, `PagedClause`, `OffsetPageable`, and `CursorClause` symbols, `/tmp` debug writes, and old pre-select forwarding APIs are absent. Historical release-note mentions and a legacy-named test class are non-production references.
- Focused `UnionClauseBehaviorTest` now proves both dialect-neutral limit planning under `NoneDataSourceWrapper` and explicit SQL Server rejection of `limit` without `orderBy`.
- The final-worktree compiler-plugin gate is green at 281/281. It includes the expanded 33-test projection diagnostics,
  22-test projection box, 23-test DSL integration, and repaired 38-test condition box; no compiler-plugin rerun gap remains.
- Task 7 has no active verification gap. The full core gate is green with 86 suites and 734 tests, and final diff plus production legacy/debug static sanity passed.
- Duplicate projection propagation through JOIN, nested/derived queries, and union is covered by focused compiler integration. Aggregate/window/scalar-subquery/raw-SQL projection items and broader result consumers such as nullable/single-row terminals and cursor extraction still need explicit coverage or a recorded out-of-scope disposition.
- Task 8 has no active verification gap. `OffsetPageQuery : KSelectable` derived-source behavior, stable-key propagation, non-root JOIN Selected typing, and multi-layer derived aliases are covered; full core is 756/756, full compiler-plugin is 270/270, the exact five-database pagination gate is 30/30, and final legacy/debug plus diff checks are green.
- Task 6 has no active verification gap. Bilingual source-maintained docs, both READMEs, release notes, and the ORM skill pass the stale JOIN/pagination/projection scans and final diff check. The docs build was intentionally not run per user instruction and is not required by Task 6 acceptance.
- Task 9 has no active verification gap: the formal IU-262.8665.258 fixture and complete IDEA plugin test gate pass 32/32 with no failures, errors, or skips. Remaining artifact and Marketplace checks belong to Task 10.
- The supplied screenshot shows Marketplace incompatibility with IDEA 2026.2 even though Marketplace update metadata reports IDEA Pro 2026.2 compatibility. Update `1110390` is still unapproved/unlisted, the new explicit range and CI checks have not completed serialized verification, and actual Marketplace installation must wait until an approved update is selectable by the IDE.

## Must-Run Verification

- Run the final-worktree non-Codacy workflow equivalents for core, compiler plugin, syntax, codegen, examples, five-database integration, and Detekt.
- Run the repository coverage workflow equivalents, including the configured `koverVerify` gates.
- Build and sign the exact `publishPlugin` artifact, then run `verifyPluginStructure`, `verifyPluginSignature`, and `verifyPlugin` against formal IDEA 2026.2.
- Run `.github/scripts/verify-idea-plugin-artifact.py` against the final signed ZIP and assert `262..262.*` in its nested `META-INF/plugin.xml`.
- Inspect `META-INF/plugin.xml` inside the final signed ZIP and assert build 262 is covered.
- Install the Marketplace-served plugin into IntelliJ IDEA 2026.2 and record the version/build shown by the IDE.
