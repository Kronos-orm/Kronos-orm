# 11. Sequence Diagram (Expanded)

Complementary to the architecture diagram, the sequence shows runtime interactions and includes a Mermaid diagram.

```mermaid
sequenceDiagram
  participant App as Caller Script
  participant CR as ConfigReader
  participant TC as TemplateConfig
  participant DB as Database
  participant KT as KronosTemplate
  participant FS as File System

  App->>CR: init configPath
  CR->>TC: Parse TOML build TemplateConfig
  TC->>DB: First access triggers lazy queries
  App->>TC: Call template render
  TC->>KT: Provide context fields indexes comments
  KT-->>App: Build content string
  App->>FS: write outputs Kotlin files
```

- App calls `init(configPath)` on ConfigReader.
- ConfigReader builds TemplateConfig and wires Kronos.
- First access to metadata triggers DB calls.
- App calls `TemplateConfig.template { ... }` and the template code assembles content.
- `write()` finalizes IO.

Design note: separating init and render allows you to cache metadata across multiple template runs if desired (by reusing `codeGenConfig`).