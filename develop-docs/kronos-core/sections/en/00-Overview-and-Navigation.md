# 0. Overview & Navigation (Top)

This is the entry of kronos-core developer docs. Structure follows a three-level “Top-Domain-Feature” hierarchy:

- Top (this page + 01~04)
  - Module overview, directory layout, architecture/flow diagrams.
- Domain sections (domains/*)
  - Runtime flow (flow)
  - Mechanisms & strategies (mechanisms)
  - ORM clause modeling (orm)
  - Functions (functions)
  - Database & transactions (db)
  - Plugins (plugins)
  - Logging (logging)
- Feature sections (features/*)
  - select/insert/upsert/update/delete/join/cascade etc.
  - Parser for named parameters, naming strategy, no-value strategy, annotations/index, exceptions/i18n, interfaces.

Suggested reading order:
1. 01-Module Overview
2. 04-Architecture
3. domains/flow and domains/mechanisms
4. features/*

Quick links:
- End-to-end runtime: domains/flow/README.md
- Action lifecycle: domains/mechanisms/action/README.md
- TaskEventPlugin: domains/mechanisms/task-event-plugin/README.md
- Common strategies (create/update/logic/optimistic): domains/mechanisms/common-strategies/README.md
- Named parameter parsing: features/parser/README.md
- Database wrapper & transactions: domains/db/README.md
- Functions & Transformer: domains/functions/README.md
- Logging: domains/logging/README.md
