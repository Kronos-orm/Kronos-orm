# Task 5: Build The Compiler And Runtime Verification Matrix

Progress: 100%
Status: Done

## Goal

Prove the complete projection, JOIN, pagination, IDEA, and Marketplace compatibility contract with deterministic tests plus all non-Codacy PR gates.

## Completed

- Official diagnostics and box coverage use real FIR/IR testData; no kctfork path was added.
- Static registration audit found no missing or stale runner methods in the `projection`, `dslIntegration`, `select`, or `condition` testData groups. Projection diagnostics have 33 files and 33 registered methods.
- The current focused `ProjectionDiagnosticsTest` is 33/33, including compiler-wide opt-in and the two newest nested/derived/union diagnostics.
- Recorded focused compiler evidence is green: `ProjectionBoxTest` is 22/22 and `DslIntegrationBoxTest` is 23/23.
- The historical targeted core JOIN/pagination/composition batch failure at 104/112 was cleared. The expanded exact batch is 114/114, full core is 756/756, and the production legacy/debug scans plus `git diff --check` are green.
- The exact five-database pagination matrix is green across MySQL, PostgreSQL, SQL Server, Oracle, and SQLite at 30/30.
- The final-worktree compiler-plugin gate is green at 281/281. Its XML includes the expanded 33-test diagnostics,
  22-test projection box, 23-test DSL integration, and the repaired 38-test condition box with zero failures,
  errors, or skips.

## Remaining

- None for the Issue 266 compiler/runtime verification matrix. IDEA artifact, Plugin Verifier, Marketplace, and installation proof remain separately owned by Task 10.

## Acceptance

- Every acceptance branch in Tasks 1-4 and 7-10 has at least one testData, deterministic assertion, artifact inspection, or recorded IDE/Marketplace check.
- Diagnostics tests prove both rejection and all accepted opt-in scopes.
- Box tests prove generated property type/value, SQL, mapping, and derived-query output.
- Tests fail if the compiler plugin is disabled or if IR uses the source field type for a derived replacement.
- Existing projection diagnostics and box suites remain green.
- Verification commands/results are recorded in `verification-log.md`, coverage does not regress below the PR gate, and missing environmental proof is named in `verification-gaps.md`.

## Verification Record

- Static runner/testData registration audit: 33 projection diagnostic files / 33 runner methods, with no missing or stale registrations across projection diagnostics, projection box, DSL integration, select, and condition groups.
- Latest focused `ProjectionDiagnosticsTest`: 33 tests, zero failures, errors, or skips.
- Recorded focused compiler suites: `ProjectionBoxTest` 22/22 and `DslIntegrationBoxTest` 23/23.
- Exact core JOIN/pagination/composition batch: 114/114 after all eight historical failures were cleared.
- Recorded full core gate: 756/756; exact five-database pagination gate: 30/30.
- Focused `ConditionBoxTest`: 38/38 after aligning its captured `length` comparison with the intentional `Int?` return type.
- Final-worktree full compiler-plugin gate: 281/281, zero failures, errors, or skips.
- Final coverage and remaining non-Codacy PR gates: delegated to the repository CI workflow after PR creation; this task-list update does not claim Codacy results.
- IDEA artifact and Marketplace proof: unresolved under Tasks 9 and 10, not counted as completed Task 5 evidence.
- `ProjectionBoxTest.joinNonRootCascadeProjection`: pass; real SQL emits `profile_id AS profileId`, no `profile` physical
  column is emitted, hidden `profileId` is retained, and the cascade relation is backfilled exactly once.
