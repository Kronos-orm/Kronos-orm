# Task 9: Align IDEA Projection Analysis And Presentation

Progress: 100%
Status: Done

## Goal

Make IDEA show the same generated projection names, types, diagnostics, Context resolution, nested-query shapes, and union shapes as compiler/runtime behavior.

## Completed

- A shared projection type resolver has been introduced so IDEA projection analysis can consume the same resolved property types instead of maintaining an independent type interpretation path.
- IDEA projection test changes covering the new resolver behavior are present in the worktree.
- The main agent completed a static review of the shared resolver and associated test changes.
- IDEA plugin production and test sources compile against the configured IDEA 2026.2 platform; the first complete test baseline executed 17 tests, with 15 passes and two stale source-text smoke failures.
- Read-only audits traced the remaining work into receiver carrier semantics, exact declaration/documentation rendering, bridge session/module lifecycle, and real editor highlighting/completion/navigation fixtures.
- Projection discovery now retains a carrier path across direct rows, selectable queries, supported collections, execution stages, and result envelopes; row-field completion is restricted to a direct row or an explicit supported row extraction.
- Declaration view and quick documentation now share one renderer. It preserves allocated names and bridge field types, emits `= null` only for top-level nullable fields, deduplicates identical Result/Context declarations, and reports deterministic same-name shape conflicts without creating redeclarations.
- Projection fallback paths now rethrow IDEA and Java cancellation exceptions instead of swallowing control flow.
- Main-agent review accepted the carrier boundary and shared-renderer direction, and identified two bounded follow-ups before verification: exact Analysis API star/variance rendering and empty-session module snapshot invalidation.
- Analysis API and FIR bridge rendering now use structured type projections and nullability APIs, preserving `*`, `in`/`out`, and nested/outer nullability without parsing rendered type strings.
- Bridge publication now starts every module session with an empty snapshot, retains other modules, and atomically rejects late publication from an older session so deleted projections cannot reappear.
- The formal IU-262.8665.258 IDEA 2026.2 fixture run passes the duplicate-JOIN projection highlighting check, and the complete IDEA plugin test gate passes 32/32 with no failures, errors, or skips.

## Remaining

None for Task 9. Signed artifact, Plugin Verifier, Marketplace approval, and Marketplace installation remain tracked in Task 10.

## Acceptance

- IDEA and command-line compilation expose identical Selected and Context properties/types for the same source.
- Duplicate JOIN projections show `id`, `id_1`, and later suffixes after opt-in.
- Explicit alias guidance is visible without suggesting a nonexistent API.
- Nested select and union completion use the materialized prior-layer names and first-union-branch contract.
- No completion item or documentation page shows a name that SQL/mapping will discard or rename differently.

## Verification Record

- Current IDEA projection surface and new runtime/FIR requirements: planning audit completed.
- Shared projection type resolver and IDEA test edits: static main-agent review completed.
- Current Task 9 implementation follow-ups: closed by the formal IU-262.8665.258 fixture run and complete 32/32 IDEA test gate.
- 2026-07-20 clean formal IU-262.8665.258 sandbox: failed during editor BODY_RESOLVE for `SelectClauseBehaviorTest.kt` at `{ [CarDetails::car] }`. The Kotlin resolver entered `transformArrayLiteralInAnnotation` because the IDEA analysis session did not support `LanguageFeature.CollectionLiterals`, then `FirCallCompletionResultsWriterTransformer` requested the unresolved `FirCallableReferenceAccessImpl` type and logged a SEVERE `KotlinIllegalArgumentExceptionWithAttachments`. The stack contains no Kronos FIR extension frame; the bounded plugin-side repair enables only `CollectionLiterals` in a copy of the imported IDE compiler configuration and remains unverified until the sandbox is rebuilt. The enabled projection probe also emitted no `Kronos probe:` line because its first log was deferred behind the smart-mode callback; it now logs scheduling before that wait so the next run can distinguish startup registration from delayed execution.
- 2026-07-20 baseline `./gradlew :kronos-idea-plugin:test --no-daemon --console=plain`: production/test compilation passed; 17 tests executed, 15 passed, and two `KronosIdeaPlatformSmokeTest` source-text assertions failed. The screenshot assertion contradicts the intentionally image-free final Marketplace description, while the documentation assertion searches for a literal now produced through KotlinPoet. No production behavior is inferred from those two failures.
- 2026-07-20 `./gradlew :kronos-idea-plugin:compileTestKotlin --no-daemon --console=plain`: passed. The real IDEA fixture and explicit compiler/core/syntax artifact wiring compile against IDEA 2026.2; only the deprecated `KotlinArtifacts` package warning remains.
- 2026-07-20 first real-fixture run: failed before test discovery because `com.intellij.psi.PsiClassOwner` was absent from the test runtime. The class was verified in the bundled Java plugin's `intellij.java.psi.jar`; adding the Java bundled plugin advanced execution past discovery.
- 2026-07-20 second real-fixture run: the single fixture was discovered and started, then failed in setup because the Kotlin facet type was unavailable. IDEA's plugin-resolution log shows Java excluded for missing `intellij.platform.structureView`, Database excluded for missing `intellij.grid`, Kotlin excluded through Java, and Kronos excluded through Kotlin. This is an open modular-test-platform wiring failure, not a passing editor-behavior result.
- 2026-07-20 formal IU-262.8665.258 fixture and IDEA test gate: the focused duplicate-JOIN fixture passed after the modular test-platform wiring was completed; the complete `kronos-idea-plugin:test` suite recorded 32/32 tests passing with no failures, errors, or skips.
- 2026-07-20 result: pass. Task 9 is complete; signed ZIP, Plugin Verifier, Marketplace approval, and Marketplace installation are not part of this task and remain Task 10 verification.
