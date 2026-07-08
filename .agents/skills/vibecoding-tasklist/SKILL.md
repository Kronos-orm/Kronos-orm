---
name: vibecoding-tasklist
description: >
  Create structured, verifiable development task lists and task-document sets.
  Use when Codex needs to turn a development goal, refactor plan, feature spec,
  audit result, or vague engineering backlog into implementation tasks,
  acceptance criteria, verification logs, execution order, out-of-scope notes,
  or maintainable task-list documentation. Also use for Chinese or English
  development planning docs that emphasize how to split work and how to record
  evidence, regardless of business domain.
---

# Vibecoding Tasklist

## Core Principle

Create task documents that make future implementation boring: every task must have a bounded outcome, explicit current evidence, concrete next actions, and verifiable acceptance criteria.

Keep the skill domain-neutral. Do not copy product names, architecture facts, APIs, or business rules from examples unless the current user request provides them as the actual target context.

## Workflow

1. Gather the source basis.
   - Read the user's goal, related design docs, existing task lists, code, tests, bug reports, and verification output.
   - Separate facts already proven from assumptions, desired future state, and historical notes.
   - Prefer current repository evidence, executable tests, and command output over memory or stale docs.

2. Lock decisions before splitting tasks.
   - Write immutable decisions, terminology, boundaries, current facts, prohibited directions, and example rules into `00-design-locks.md`.
   - Move unresolved ideas into task follow-up or verification gaps; do not hide them as design locks.
   - Use design locks to prevent later tasks from re-litigating core direction.

3. Split by dependency and validation surface.
   - Start with baseline, cleanup, inventory, or contract stabilization tasks.
   - Then split implementation by stable boundaries such as public contract, model/type layer, pipeline stages, adapters, integrations, diagnostics, docs, and tests.
   - Put cross-cutting verification, legacy removal, and documentation sync near the end unless they unblock earlier tasks.
   - Create separate tasks for optional, risky, or environment-dependent work.
   - Keep each task small enough to review and verify independently, but large enough to produce a meaningful state change.

4. Write the document set.
   - Create a task-list directory with one overview, one design-lock file, numbered task files, and verification tracking files.
   - Make each numbered task self-contained enough that another agent can implement it without reading the whole plan.
   - Keep historical context in `archive.md`; keep explicit exclusions in `out-of-scope.md` when useful.

5. Record evidence continuously.
   - Update task progress only when backed by completed edits, tests, static scans, review notes, or clear investigation results.
   - Put commands, pass/fail results, and uncovered gaps in verification files.
   - Preserve failed verification when it teaches the next implementer what remains.

## Directory Shape

Use this shape unless the user asks for a different format:

```text
TASK_LIST_DIR/
|-- README.md
|-- 00-design-locks.md
|-- 01-first-task.md
|-- 02-next-task.md
|-- ...
|-- implementation-order.md
|-- verification-log.md
|-- verification-gaps.md
|-- out-of-scope.md      # optional
`-- archive.md           # optional
```

Use two-digit numeric prefixes for stable ordering. Do not renumber completed tasks unless the user explicitly asks.

## README.md

Write the overview as the entry point and status board:

```markdown
# <Project Or Workstream> Task List

Updated: <date>

This document is based on <source basis>. It is an implementation plan and acceptance checklist for <goal>.

## Entrypoints

- [Design locks](00-design-locks.md)
- [Recommended implementation order](implementation-order.md)
- [Current verification gaps](verification-gaps.md)
- [Recent verification](verification-log.md)
- [Out of scope](out-of-scope.md)
- [Archive](archive.md)

## Task Documents

- [Task 1: <name>](01-first-task.md)
- [Task 2: <name>](02-next-task.md)

## Current Judgment

- <Concrete fact or risk discovered from current evidence.>
- <Concrete boundary, dependency, or incomplete area.>

## Overview

| Progress | Task | Status | Notes |
|----------|------|--------|-------|
| 0% | Task 1: <name> | Pending | <Evidence-based note.> |
```

README rules:

- State the source basis and date.
- Link all required tracking documents.
- Keep `Current Judgment` factual, not aspirational.
- Use progress only when meaningful. If progress is unknown, use `TBD` or omit the percentage.
- Make the table notes explain why the status is what it is.

## 00-design-locks.md

Use this file for decisions that all tasks must obey:

```markdown
# Design Locks

Updated: <date>

## Terms And Boundaries

- `<term>` means <definition>.

## Current Facts

- <Fact verified from code, tests, docs, or command output.>

## Migration Or Implementation Principles

- <Principle that constrains implementation choices.>

## Do Not Add

- <Forbidden shortcut, unsupported future capability, stale API, or risky path.>

## Example Or Documentation Rules

- <Rules for examples, docs, generated output, or test fixtures.>
```

Design-lock rules:

- Put only high-confidence decisions here.
- Phrase locks as constraints, not vague preferences.
- Move speculative future work to task docs, verification gaps, or out-of-scope.

## Numbered Task Files

Use this template for each task:

```markdown
# Task <n>: <Name>

Progress: <0-100% or TBD>
Status: <Pending | In Progress | Blocked | Done>

## Goal

<One bounded outcome.>

## Current State

- <Evidence-backed fact.>
- <Known partial implementation, failing test, missing doc, or risk.>

## Follow-Up Work

- <Concrete edit, investigation, test, migration, or review step.>
- <Another step.>

## Acceptance

- <Observable condition that proves the task is complete.>
- <Command, test, static scan, review condition, or artifact check.>

## Verification Record

- `<command or check>`: <pass/fail/not run and why>.
```

Use `Completed` / `Remaining` sections instead of `Current State` / `Follow-Up Work` when a task is already partly implemented. Use `Blocked` only when a specific dependency prevents progress.

Task-writing rules:

- Give each task one primary outcome.
- Avoid file-by-file task splits unless files are the true ownership boundary.
- Include both positive acceptance and important negative acceptance, such as removed legacy paths or forbidden outputs.
- Make acceptance criteria testable by another agent.
- Do not mark a task done only because code was edited; require verification or a documented reason verification could not run.

## implementation-order.md

Write a topological execution sequence:

```markdown
# Recommended Implementation Order

1. Finish Task 1 because <dependency or risk>.
2. Then Task 2 to stabilize <contract or surface>.
3. Run Task N verification before broad docs or cleanup.
```

Ordering rules:

- Put cleanup and baseline before dependent work.
- Put shared contracts before consumers.
- Put diagnostics, compatibility, and migration aids before large user-facing examples when they affect correctness.
- Put broad verification after enough implementation exists, then loop back to fix gaps.

## verification-log.md

Use this file as a chronological evidence trail:

```markdown
# Recent Verification

Updated: <date>

## <date> <short label>

- Scope: <what changed or was audited>.
- Evidence: <files, tests, commands, review source, or static scan>.
- Result: <pass/fail/partial>.
- Follow-up: <linked task or gap if needed>.
```

Log enough detail that a later agent can reproduce the check. Keep command output summarized unless the exact output is the evidence.

## verification-gaps.md

Use this file for known unproven areas:

```markdown
# Current Verification Gaps

Updated: <date>

- <Gap, why it matters, and which task should close it.>

## Must-Run Verification

- `<command>`
```

Gap rules:

- A gap is not a vague concern; it names the missing proof.
- Include environment constraints when relevant.
- Remove or update gaps after verification lands.

## out-of-scope.md And archive.md

Create `out-of-scope.md` when the plan needs explicit exclusions:

- unsupported feature variants,
- future work intentionally deferred,
- risky shortcuts that should not be implemented under this task list.

Create `archive.md` when useful historical details would otherwise clutter active tasks:

- old designs,
- superseded task lists,
- migration notes,
- abandoned alternatives and why they were dropped.

## Quality Checklist

Before finishing, verify:

- `README.md` links every active task and tracking document.
- `00-design-locks.md` contains only current, high-confidence constraints.
- Every task has goal, current state or completed state, follow-up or remaining work, acceptance, and verification.
- Each acceptance criterion is observable.
- `implementation-order.md` respects dependencies.
- `verification-log.md` records what was actually checked.
- `verification-gaps.md` names missing proof rather than general uncertainty.
- Out-of-scope and archive material are not mixed into active task instructions.
- No domain-specific facts from unrelated examples leaked into the new task list.
- The output language matches the user's request and surrounding task documents.
