# 1. Module Overview (Expanded)

This chapter provides an in-depth overview of the kronos-codegen module: its purpose, boundaries, design philosophy, and how it fits in the larger Kronos ecosystem.

- Purpose: Automate Kotlin POJO generation from relational metadata with minimal configuration.
- Non-goals: Business-specific modeling or ORM runtime behavior (these belong to kronos-core).
- Design values:
  - Minimal magic: All automatic behavior is traceable to explicit strategies or metadata.
  - Deterministic generation: Same input (schema, config) produces the same output.
  - Seamless integration: Usable as a library (tests/scripts), and by build plugins.
  - Extensibility: Strategies decoupled via Kronos global context; templating via a small DSL.

## Key Flows

1. Configuration loading: ConfigReader.init(path) reads TOML and builds a TemplateConfig.
2. Strategy wiring: TemplateConfig initializes Kronos strategies (naming, timestamps, deletion, version, PK).
3. Metadata access: Lazy getters pull columns, indexes, and table comments via SqlManager and queryTableComment.
4. Template rendering: KronosTemplate exposes contextual fields and helpers; you render content with a small DSL.
5. Output: KronosConfig.write writes the generated content to disk.

## Why this architecture?

- Clear separation of concerns:
  - Config parsing is isolated in ConfigReader.
  - Runtime global behaviors (naming/strategies) centralized in Kronos but configured by TemplateConfig.
  - Data source creation is pluggable via reflection (DataSourceHelper), so no hard dependency.
  - Rendering context (KronosTemplate) is a pure data structure + helper algorithms, making templates testable.
- Performance and ergonomics:
  - Lazy loading avoids unnecessary DB calls if some parts of the template aren’t used.
  - Import set is auto-managed so templates remain clean.

## Edge considerations

- Re-entrancy: init should be called once per config within a process. Multiple calls overwrite codeGenConfig.
- Determinism: Ensure DB metadata queries are stable; avoid time-based content in the template unless desired.
- Safety: Reflection in DataSourceHelper is guarded with type checks and warnings when setters aren’t found.
