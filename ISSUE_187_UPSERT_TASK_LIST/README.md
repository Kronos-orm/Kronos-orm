# Issue 187 Upsert Fix Task List

Updated: 2026-07-12

This document is based on issue #187, current failing upsert tests, the added transaction-boundary guards, and the current `UpsertClause` / `KronosActionTask` implementation.

## Entrypoints

- [Design locks](00-design-locks.md)
- [Recommended implementation order](implementation-order.md)
- [Current verification gaps](verification-gaps.md)
- [Recent verification](verification-log.md)

## Task Documents

- [Task 1: Restore upsert conflict target and strategy contracts](01-upsert-conflict-target-and-strategies.md)
- [Task 2: Keep action task hooks inside one transaction](02-action-task-transaction-boundary.md)
- [Task 3: Verify, version, and PR](03-verification-version-and-pr.md)

## Current Judgment

- Issue #187 is a real logic-delete upsert regression: a match-field upsert must update the logically deleted row and restore the delete marker instead of inserting a duplicate key.
- Native `onConflict()` must never render `ON CONFLICT ()` or `MERGE ... ON ()`; an empty conflict target is a build-time failure, not legal output.
- `KronosActionTask` hooks are part of the action lifecycle and must run inside the same transaction as the grouped DML.

## Overview

| Progress | Task | Status | Notes |
|----------|------|--------|-------|
| 100% | Task 1: Restore upsert conflict target and strategy contracts | Done | Conflict targets, strategy fields, logic-delete restore, and MERGE target-column rendering are covered by core and integration tests. |
| 100% | Task 2: Keep action task hooks inside one transaction | Done | Core transaction probes and real integration rollback behavior pass. |
| 100% | Task 3: Verify, version, and PR | Done | Verification is complete and PR #235 is open. |
