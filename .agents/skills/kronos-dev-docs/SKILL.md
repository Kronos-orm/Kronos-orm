---
name: kronos-dev-docs
description: >
  Kronos-ORM documentation maintainer guide for kronos-docs and related user-facing
  docs. Use this skill when editing, reviewing, reorganizing, auditing, or verifying
  Kronos documentation, including kronos-docs Angular/ng-doc pages, docs routes,
  bilingual en/zh-CN content, README installation examples, blog/homepage copy,
  documentation task lists, code snippets, links, build checks, and syncing
  documentation changes back into .agents/skills/kronos-orm-guide.
---

# Kronos Dev Docs

Use this skill to keep Kronos user documentation accurate, bilingual, navigable, and aligned with the current repository implementation.

## Evolution Memory Protocol

For recurring docs-maintenance pitfalls, read `Evolution.index.md` first. Do not browse the full evolution history by default. If the index matches, open only the linked `evolution/*.md` entry file. If a new docs pitfall is verified, create one entry file under `evolution/`, add one concise row to `Evolution.index.md`, and remove, archive, or rewrite obsolete records when the guidance is no longer true.

## Source Of Truth

Read these before significant docs work:

1. `DOCS_REFACTOR_TASK_LIST/README.md`
2. `DOCS_REFACTOR_TASK_LIST/00-design-locks.md`
3. `DOCS_REFACTOR_TASK_LIST/style-baseline.md`
4. The numbered task file matching the requested work
5. Neighboring `kronos-docs` pages in the same language and module
6. Current source/tests/README files for any API, dependency, version, or behavior claim

Treat `DOCS_REFACTOR_TASK_LIST` as the active refactor plan and acceptance checklist. Update its task files, `verification-log.md`, and `verification-gaps.md` when docs work changes status or evidence.

## Repository Map

- `kronos-docs/`: Angular 21 + ng-doc user documentation app.
- `kronos-docs/src/app/docs/`: docs source pages.
- `kronos-docs/src/app/docs/en/`: English documentation tree.
- `kronos-docs/src/app/docs/zh-CN/`: Chinese documentation tree.
- Current docs section directories are numbered by IA order:
  `1.getting-started`, `2.mapping`, `3.query`, `4.mutation`, `5.database`,
  `6.configuration`, `7.advanced`, and `8.resources`.
- `kronos-docs/src/app/docs/macros/`: Nunjucks macros used by pages.
- `kronos-docs/src/assets/blogs/en/` and `kronos-docs/src/assets/blogs/zh-CN/`: blog markdown.
- `kronos-docs/src/assets/i18n/`: homepage/navigation translations.
- `kronos-docs/src/app/routes/home/`: homepage copy and install code snippets.
- `kronos-docs/src/app/routes/blog/`: blog listing metadata.
- `README.MD` and `README-zh_CN.MD`: top-level user-facing install and overview docs.
- `.agents/skills/kronos-orm-guide/`: user-facing Codex skill that must stay aligned with docs.

Do not treat these as source-maintained docs:

- `kronos-docs/docs/`
- `kronos-docs/ng-doc/`
- `kronos-docs/node_modules/`

They are generated output or dependencies unless the user explicitly asks otherwise.

## Current Documentation Locks

Use the current repository implementation as the source of truth.

- Use stable release-style versions such as `0.1.1` in recommended user-facing dependency snippets. Do not use `-SNAPSHOT` in copyable install examples unless the page is explicitly about snapshot/source development; merge/release flow will bump versions after docs changes land.
- In `kronos-docs/src/app/docs` Markdown, source current Kronos versions from the docs macros instead of hard-coding them. Use `{{ $.kronosVersion() }}`, `{{ $.kronosSnapshotVersion() }}`, and `{{ $.kronosSnapshotBadgeVersion() }}` after the page imports its language macro file.
- README files, homepage TypeScript snippets, module READMEs, and blog assets do not share the `kronos-docs` Markdown macro pipeline; keep their version text explicit or use local constants when the file already has that pattern.
- Verify the current repository development version from `build-logic/src/main/kotlin/publishing.gradle.kts` before changing source-development or snapshot-specific text.
- Current Kotlin catalog is in `gradle/libs.versions.toml`; verify before updating Kotlin requirements.
- Complete database dialects are MySQL, PostgreSQL, SQLite, SQL Server, and Oracle.
- `Kronos` global configuration is direct property assignment on the `Kronos` object; do not document `Kronos.init { ... }` unless it exists again.
- `KronosJdbcWrapper` lives in `com.kotlinorm.wrappers`, takes a `DataSource`, optional `DBType`, and `KronosJdbcConfig` block; database type is inferred from JDBC metadata when `databaseType` is not supplied.
- Public user docs should explain available APIs, configuration, observable behavior, and troubleshooting. Keep compiler FIR/IR internals, maintainer test infrastructure, and implementation pipeline details out of ordinary docs navigation.
- Multi-platform, KMP, mobile, and Android language may remain in homepage/blog/product direction areas, but runnable tutorials must focus on current working JVM/JDBC usage.

## NgDoc Structure Rules

Each docs page normally has:

```text
kronos-docs/src/app/docs/<lang>/<section>/<page>/
|-- index.md
`-- ng-doc.page.ts
```

Each section has `ng-doc.category.ts`.

When adding, moving, or renaming pages:

- Keep `en` and `zh-CN` structures mirrored unless there is a deliberate documented exception.
- Keep route semantics identical across languages. The category route carries the language prefix, such as `en/database` or `zh-CN/database`; page routes should match.
- Current category routes:
  - `en/getting-started` / `zh-CN/getting-started`
  - `en/mapping` / `zh-CN/mapping`
  - `en/query` / `zh-CN/query`
  - `en/mutation` / `zh-CN/mutation`
  - `en/database` / `zh-CN/database`
  - `en/configuration` / `zh-CN/configuration`
  - `en/advanced` / `zh-CN/advanced`
  - `en/resources` / `zh-CN/resources`
- Page link routes use the category route segment plus the page route, for example `query/select`, `mutation/insert`, `mutation/last-insert-id`, `database/dialect-support`, `configuration/global-config`, and `advanced/cascade-select`.
- Check `route` and `order` for duplicates inside each category.
- Keep `title` localized but technically equivalent.
- Preserve required imports and demos when a page uses `NgDocActions.demo("AnimateLogoComponent", {container: false})`.
- Update `$.keyword(...)` links after route changes.
- Check category order. Existing English category order values are large (`1001`, `1002`, etc.) while Chinese category orders are smaller; preserve existing navigation intent unless the task is information architecture work.

## Markdown And Macro Style

Before editing a page, read at least two neighboring pages in the same language and module. Match their title levels, paragraph density, code block metadata, callout style, and link syntax.

Common page header:

```njk
{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}
```

For Chinese pages, import:

```njk
{% import "../../../macros/macros-zh-CN.njk" as $ %}
```

Use existing macros:

- `{{ $.title("select") }}`
- `{{ $.annotation("Table") }}`
- `{{ $.code("KPojo") }}`
- `{{ $.kronosVersion() }}`
- `{{ $.kronosSnapshotVersion() }}`
- `{{ $.kronosSnapshotBadgeVersion() }}`
- `{{ $.params([...]) }}`
- `{{ $.members([...]) }}`
- `{{ $.keyword("query/select", ["Select"]) }}`

Keep `$.keyword(...)` calls on one logical line unless the existing formatter has already proven the split safe. Bad route strings can produce broken links.

Use existing callout style:

```markdown
> **Note**
> Short, specific note tied to the current API.

> **Warning**
> Short, specific warning about configuration or behavior.
```

Use code fence metadata consistently, for example ` ```kotlin group="Case 1" name="kotlin" icon="kotlin" {1}`.

Use `group="..."` only for code fences that should render as tabs for one example unit. A group is appropriate when the fenced blocks are alternative views of the same example, such as Kotlin plus MySQL/PostgreSQL/SQLite/SQLServer/Oracle SQL, or Kotlin plus the direct result/params for the same call. Do not reuse a page-level topic name as the group across unrelated sections. When a page has multiple independent examples under different headings, give each example its own group, for example `Table column model`, `Default value`, and `Timestamp fields` instead of one shared `TableColumn`.

Before finalizing pages with multiple fenced blocks, scan repeated `group` values. A repeated group that spans multiple `##` / `###` headings is usually a tab-merging bug unless the headings are deliberately part of one continuous example. Good groups typically have names like `kotlin`, `Mysql`, `PostgreSQL`, `SQLite`, `SQLServer`, `Oracle`, `result`, or `params` for the same scenario; risky groups have many unrelated names such as `model`, `default`, `timestamps`, `insert params`, and `update params` under one broad topic.

For SQL examples, preserve the existing dialect order when showing multiple outputs: MySQL, PostgreSQL, SQLite, SQLServer, Oracle.

## Writing Rules

- Keep docs API-first and example-driven.
- Explain what the user can do, how to configure it, and what SQL/result/behavior to expect.
- Write every substantive description as an example-backed unit: one user action, API, configuration item, or observable behavior per short section.
- Pair important claims with a nearby Kotlin snippet, configuration block, generated SQL, result example, command output, or a direct link to the page that contains that example.
- Use lists and tables as navigation, checklists, or compact indexes. Add the runnable or inspectable examples in the same section or immediately after the list.
- Keep English concise and natural for the existing English docs.
- Keep Chinese direct, compact, and consistent with current terminology.
- Keep facts aligned between English and Chinese, but do not force word-for-word translation.
- Prefer current recommended API only in ordinary docs.
- Put migration history, old syntax comparisons, or compatibility notes only in changelog or migration-style pages.
- Do not write "new in this module", "compared with the previous version", or similar release narration in ordinary docs body.
- Do not invent modules, integrations, annotations, config properties, or dialect support.
- Use examples from current tests, source, README, or verified snippets.

## High-Risk Checks

Use these scans after broad docs edits, adapting scope to the changed files:

```bash
rg -n "0\\.1\\.0|latest\\.release|kronos-jdbc-wrappere|com\\.kotlinorm\\.kronos-jdbc-wrapper|Kronos\\.init|lineHumpStrategy|dateFormat\\b|@updateTime|enabled = false" kronos-docs README.MD README-zh_CN.MD
rg -n "\\{\\{ [0-9]+kronos|0\\.1\\.1|0\\.1\\.1-SNAPSHOT|0\\.1\\.1--SNAPSHOT" kronos-docs/src/app/docs -g "*.md"
rg -n "class-definition|3\\.database|4\\.advanced|5\\.plugin|6\\.concept|7\\.ai|database/(insert-records|delete-records|update-records|upsert-records|select-records|select-join-tables|subqueries)|advanced/cascade-query|plugins/last-insert-id|\\$\\.keyword\\(\"$" kronos-docs/src/app/docs
rg -n "route:|order:|title:|@status" kronos-docs/src/app/docs/en kronos-docs/src/app/docs/zh-CN
```

These scans are not automatic proof of failure. Inspect matches and decide whether each is stale docs content, generated/dependency content, or acceptable historical text.

Known high-risk patterns:

- `0.1.0` or `-SNAPSHOT` in runnable install snippets when the recommended docs version should be stable-style `0.1.1`.
- Hard-coded current Kronos versions in `kronos-docs/src/app/docs` Markdown when the value should come from `$.kronosVersion()` or the snapshot macros.
- Broken version macro text such as `{{ 9kronosVersion() }}` after shell or Perl replacements.
- `latest.release` in copyable dependency examples.
- Wrong artifact coordinate separators, especially `com.kotlinorm.kronos-jdbc-wrapper` instead of `com.kotlinorm:kronos-jdbc-wrapper`.
- Misspelled `kronos-jdbc-wrappere`.
- Nonexistent `Kronos.init { ... }`.
- Old global config symbols such as `lineHumpStrategy` and `dateFormat`.
- Lowercase `@updateTime`.
- Annotation examples using nonexistent `enabled = false` if the current annotation does not support it.
- Links to stale routes from the old IA, including `class-definition/*`, `3.database/*`, `4.advanced/*`, `5.plugin/*`, `6.concept/*`, `7.ai/*`, `database/select-records`, `advanced/cascade-query`, and `plugins/last-insert-id`.
- Cross-language macro imports.
- Empty placeholder pages left in navigation.

## Verification

Prefer narrow verification first, then broad checks when the change is user-facing or structural.

For docs app build:

```bash
cd kronos-docs
pnpm build
```

For local preview:

```bash
cd kronos-docs
pnpm start
```

For unified docs deployment build:

```bash
./deploy-docs.sh
./deploy-docs.sh --skip-dokka
```

When touching snippets that claim to compile, verify against source/tests where possible. If runnable snippet verification is not available, record the gap in `DOCS_REFACTOR_TASK_LIST/verification-gaps.md`.

After each substantial docs task, update `DOCS_REFACTOR_TASK_LIST/verification-log.md` with:

- modified scope,
- source or test evidence checked,
- commands run,
- pass/fail result,
- remaining unverified pages or code blocks.

## Mandatory ORM Guide Sync

After changing user-facing Kronos docs, refresh:

```text
.agents/skills/kronos-orm-guide/
```

At minimum, check and update:

- `.agents/skills/kronos-orm-guide/SKILL.md`
- `.agents/skills/kronos-orm-guide/references/advanced.md`
- `.agents/skills/kronos-orm-guide/references/annotations.md`

The ORM guide must match the docs for:

- installation coordinates and versions,
- Kotlin/JDK requirements,
- `Kronos` global configuration,
- `KPojo` and annotation examples,
- CRUD/query/update/delete/upsert syntax,
- condition DSL, join, subquery, projection, and function examples,
- transactions, table operations, cascade, logic delete, optimistic lock,
- database dialect support and wrapper usage.

If the docs change invalidates guide examples, update the guide in the same task. If the guide cannot be fully refreshed, record the exact remaining sync gap in `DOCS_REFACTOR_TASK_LIST/verification-gaps.md`.

## Workflow

1. Read the relevant task list files and neighboring docs pages.
2. Inspect current source/tests for every API or version claim.
3. Check the worktree before editing; do not overwrite unrelated user changes.
4. Edit English and Chinese counterparts together when the technical fact changes.
5. Update routes/categories/links/i18n/blog metadata when moving or adding pages.
6. Refresh `.agents/skills/kronos-orm-guide/` after user-facing docs edits.
7. Run targeted scans and, when feasible, `pnpm build`.
8. Update task-list verification records and gaps.
9. Report which docs, guide files, and verification commands changed.

## When In Doubt

- Prefer current source and tests over old docs.
- Prefer preserving established docs style over introducing a new prose pattern.
- Prefer a short verified example over a broad unverified feature claim.
- Prefer linking to one authority page over duplicating long explanations across pages.
